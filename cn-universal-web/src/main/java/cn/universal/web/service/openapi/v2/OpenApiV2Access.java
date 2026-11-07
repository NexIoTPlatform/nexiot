package cn.universal.web.service.openapi.v2;

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.security.service.IoTUserService;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * V2 开放接口的统一资源边界。所有身份取自已验证的 JWT 和数据库，禁止由请求体选择租户、账号或应用。
 *
 * <p>每次请求重新读取应用和设备绑定，应用停用、过期及解绑立即生效；不复用 V1 的宽松产品拥有者授权。
 */
@Service
@RequiredArgsConstructor
public class OpenApiV2Access {
  private final IoTUserApplicationMapper applications;
  private final IoTDeviceMapper devices;
  private final IoTDeviceService deviceService;
  private final IoTProductDeviceService products;
  private final IoTUserService users;

  /** 只接受 client_credentials 的应用令牌；普通用户令牌不能借用其名下任意应用。 */
  public IoTUserApplication application() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof Jwt jwt)
        || StrUtil.isBlank(jwt.getSubject())
        || !jwt.getSubject().equals(jwt.getClaimAsString("clientId"))) {
      throw new AccessDeniedException("请使用应用的 client_credentials 令牌");
    }
    IoTUserApplication app = applications.selectIotUserApplicationByAppId(jwt.getSubject());
    if (app == null
        || StrUtil.hasBlank(app.getAppUniqueId(), app.getUnionId())
        || !Integer.valueOf(0).equals(app.getAppStatus())
        || !Integer.valueOf(0).equals(app.getDeleted())
        || app.getValidEndDate() != null
            && app.getValidEndDate().getTime() <= System.currentTimeMillis()) {
      throw new AccessDeniedException("应用不存在、已停用或授权已过期");
    }
    String tenant = tenantId(app);
    String tokenTenant = jwt.getClaimAsString("tenantId");
    if (StrUtil.isNotBlank(tokenTenant) && !tenant.equals(tokenTenant)) {
      throw new AccessDeniedException("令牌租户与应用归属不一致，请重新获取令牌");
    }
    return app;
  }

  /** 兼容历史应用缺少 tenant_id 的记录，仅使用其数据库所有者推导，不信任调用参数。 */
  public String tenantId(IoTUserApplication app) {
    if (StrUtil.isNotBlank(app.getTenantId())) return app.getTenantId();
    IoTUser owner = users.selectUserByUnionId(app.getUnionId());
    if (owner == null) throw new AccessDeniedException("无法确认应用所属租户");
    String tenant = StrUtil.blankToDefault(owner.getTenantId(), owner.getUnionId());
    if (StrUtil.isBlank(tenant)) throw new AccessDeniedException("无法确认应用所属租户");
    return tenant;
  }

  /** 已绑定当前应用、属于当前账号和租户的设备；不存在和无权访问统一返回 403，避免资源枚举。 */
  public IoTDeviceDTO device(String productKey, String deviceId) {
    identifier(productKey, "productKey");
    identifier(deviceId, "deviceId");
    IoTUserApplication app = application();
    String tenant = tenantId(app);
    IoTDevice record =
        devices.selectOne(
            IoTDevice.builder()
                .productKey(productKey)
                .deviceId(deviceId)
                .creatorId(app.getUnionId())
                .tenantId(tenant)
                .application(app.getAppUniqueId())
                .build());
    if (record == null) throw new AccessDeniedException("应用未绑定此设备或设备租户不匹配");
    IoTProduct product = products.getProduct(productKey);
    if (product == null
        || !tenant.equals(product.getTenantId())
        || Integer.valueOf(1).equals(product.getIsDeleted())) {
      throw new AccessDeniedException("设备产品不可访问");
    }
    IoTDeviceDTO dto =
        deviceService.selectDevInstanceBONoCache(
            Map.of("productKey", productKey, "deviceId", deviceId));
    if (dto == null
        || !Objects.equals(record.getIotId(), dto.getIotId())
        || !tenant.equals(dto.getTenantId())
        || !app.getAppUniqueId().equals(dto.getApplicationId())) {
      throw new AccessDeniedException("设备归属已变化，请重新查询");
    }
    return dto;
  }

  /** 创建设备必须拥有目标产品；已有设备的访问始终另行检查应用绑定。 */
  public IoTProduct product(String productKey) {
    identifier(productKey, "productKey");
    IoTUserApplication app = application();
    IoTProduct product = products.getProduct(productKey);
    if (product == null
        || !tenantId(app).equals(product.getTenantId())
        || !app.getUnionId().equals(product.getCreatorId())
        || Integer.valueOf(1).equals(product.getIsDeleted())) {
      throw new AccessDeniedException("应用所有者没有此产品权限");
    }
    return product;
  }

  public Long department(IoTUserApplication app) {
    IoTUser user = users.selectUserByUnionId(app.getUnionId());
    return user == null ? null : user.getDeptId();
  }

  /** 所有资源标识保持原值，不静默 trim 或推导 iotId。 */
  public static void identifier(String value, String name) {
    if (StrUtil.isBlank(value)
        || value.length() > 100
        || !value.equals(value.trim())
        || value.chars().anyMatch(Character::isISOControl)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + "不能为空、包含控制字符或超过100个字符");
    }
  }
}
