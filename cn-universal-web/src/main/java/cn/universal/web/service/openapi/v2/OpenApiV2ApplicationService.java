package cn.universal.web.service.openapi.v2;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.push.transform.AppPushConfigChangeNotifier;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.web.dto.openapi.v2.OpenApiV2ApplicationModels.ApplicationInfo;
import cn.universal.web.dto.openapi.v2.OpenApiV2ApplicationModels.DeviceBinding;
import cn.universal.web.dto.openapi.v2.OpenApiV2ApplicationModels.HttpPushInfo;
import cn.universal.web.dto.openapi.v2.OpenApiV2ApplicationModels.HttpPushUpdate;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 对外应用能力。使用已有应用 cfg 和推送通知链路，不创建第二份推送配置。
 *
 * <p>绑定更新必须同时约束设备归属、租户和旧绑定值，避免先鉴权后无条件更新产生越权竞态。
 */
@Service
@RequiredArgsConstructor
public class OpenApiV2ApplicationService {

  private static final String DEFAULT_HTTP_HEADER = "X-Nexiot-Secret";
  private static final Pattern HTTP_HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");
  private static final Set<String> RESERVED_HEADERS =
      Set.of(
          "host",
          "content-length",
          "transfer-encoding",
          "connection",
          "upgrade",
          "authorization",
          "proxy-authorization",
          "cookie");

  private final OpenApiV2Access access;
  private final IoTUserApplicationMapper applicationMapper;
  private final IoTDeviceMapper deviceMapper;
  private final IoTDeviceService devices;
  private final JdbcTemplate jdbc;
  private final AppPushConfigChangeNotifier pushConfigNotifier;
  private final IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  public ApplicationInfo current() {
    IoTUserApplication app = access.application();
    JSONObject cfg = parseConfiguration(app);
    JSONObject mqtt = cfg.getJSONObject("mqtt");
    return new ApplicationInfo(
        app.getAppUniqueId(),
        app.getAppId(),
        app.getAppName(),
        app.getAppStatus(),
        app.getScope(),
        app.getValidEndDate(),
        app.getCreateDate(),
        app.getRemark(),
        describeHttp(cfg),
        mqtt != null && mqtt.getBool("support", true) && mqtt.getBool("enable", false));
  }

  public HttpPushInfo httpPush() {
    return describeHttp(parseConfiguration(access.application()));
  }

  /** 在同一事务中锁定并重新读取应用行，避免并发 HTTP 编辑覆盖其他配置。 只向 Mapper 传递 HTTP 配置白名单字段，绝不透传数据库实体或客户端应用标识。 */
  @Transactional(rollbackFor = Exception.class)
  public HttpPushInfo updateHttpPush(HttpPushUpdate request) {
    if (request == null || request.enable() == null) {
      throw bad("enable 不能为空");
    }
    IoTUserApplication authenticated = access.application();
    jdbc.queryForObject(
        "SELECT app_unique_id FROM iot_user_application WHERE app_unique_id = ? FOR UPDATE",
        String.class,
        authenticated.getAppUniqueId());
    IoTUserApplication app =
        applicationMapper.selectIotUserApplicationById(authenticated.getAppUniqueId());
    assertSameActiveApplication(authenticated, app);
    JSONObject cfg = parseConfiguration(app);
    JSONObject http = cfg.getJSONObject("http");
    if (http == null) http = new JSONObject();
    if (request.url() != null) {
      validateUrl(request.url());
      http.set("url", request.url());
    }
    if (request.header() != null) {
      validateHeader(request.header());
      http.set("header", request.header());
    }
    if (StrUtil.isBlank(http.getStr("header"))) http.set("header", DEFAULT_HTTP_HEADER);
    if (StrUtil.isNotBlank(request.secret())) {
      validateSecret(request.secret());
      http.set("secret", request.secret());
    }
    if (request.enable()) {
      if (!http.getBool("support", true)) throw bad("HTTP 推送当前不受支持");
      validateUrl(http.getStr("url"));
      validateHeader(http.getStr("header"));
      validateSecret(http.getStr("secret"));
    }
    http.set("support", http.getBool("support", true));
    http.set("enable", request.enable());
    cfg.set("http", http);
    IoTUserApplication patch = new IoTUserApplication();
    patch.setAppUniqueId(app.getAppUniqueId());
    patch.setCfg(cfg.toString());
    patch.setHttpEnable(request.enable() ? 1 : 0);
    patch.setNotifyUrl(http.getStr("url"));
    if (applicationMapper.updateIotUserApplication(patch) != 1) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "HTTP 推送配置更新失败，请重试");
    }
    pushConfigNotifier.notifyAppConfigChanged(app.getAppUniqueId(), "OPENAPI_V2_HTTP_PUSH_UPDATED");
    return describeHttp(cfg);
  }

  @Transactional(rollbackFor = Exception.class)
  public DeviceBinding bind(String productKey, String deviceId) {
    IoTUserApplication app = access.application();
    IoTDevice device = ownedDevice(app, productKey, deviceId);
    if (app.getAppUniqueId().equals(device.getApplication())) {
      return binding(app, device, true);
    }
    if (StrUtil.isNotBlank(device.getApplication())) throw noDevicePermission();
    int updated =
        jdbc.update(
            "UPDATE iot_device SET application = ? WHERE id = ? AND creator_id = ? "
                + "AND tenant_id = ? "
                + "AND (application IS NULL OR application = '')",
            app.getAppUniqueId(),
            device.getId(),
            app.getUnionId(),
            access.tenantId(app));
    if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "设备绑定状态已变化，请重新查询后重试");
    publishDeviceUpdate(device, app.getAppUniqueId());
    return binding(app, device, true);
  }

  @Transactional(rollbackFor = Exception.class)
  public DeviceBinding unbind(String productKey, String deviceId) {
    IoTUserApplication app = access.application();
    IoTDevice device = ownedDevice(app, productKey, deviceId);
    if (StrUtil.isBlank(device.getApplication())) return binding(app, device, false);
    if (!app.getAppUniqueId().equals(device.getApplication())) throw noDevicePermission();
    int updated =
        jdbc.update(
            "UPDATE iot_device SET application = NULL WHERE id = ? AND creator_id = ? "
                + "AND tenant_id = ? "
                + "AND application = ?",
            device.getId(),
            app.getUnionId(),
            access.tenantId(app),
            app.getAppUniqueId());
    if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "设备绑定状态已变化，请重新查询后重试");
    publishDeviceUpdate(device, null);
    return binding(app, device, false);
  }

  /** 读取设备原始归属，避免 LEFT JOIN 已删除应用后把非空绑定误判为未绑定。 */
  private IoTDevice ownedDevice(IoTUserApplication app, String productKey, String deviceId) {
    OpenApiV2Access.identifier(productKey, "productKey");
    OpenApiV2Access.identifier(deviceId, "deviceId");
    IoTDevice device =
        deviceMapper.selectOne(
            IoTDevice.builder().productKey(productKey).deviceId(deviceId).build());
    if (device == null
        || StrUtil.isBlank(app.getUnionId())
        || !app.getUnionId().equals(device.getCreatorId())) throw noDevicePermission();
    if (!Objects.equals(access.tenantId(app), device.getTenantId())) throw noDevicePermission();
    return device;
  }

  private void publishDeviceUpdate(IoTDevice device, String applicationId) {
    IoTDeviceDTO dto =
        devices.selectDevInstanceBONoCache(
            Map.of("productKey", device.getProductKey(), "deviceId", device.getDeviceId()));
    if (dto == null) {
      dto =
          IoTDeviceDTO.builder()
              .id(device.getId())
              .iotId(device.getIotId())
              .productKey(device.getProductKey())
              .deviceId(device.getDeviceId())
              .userUnionId(device.getCreatorId())
              .tenantId(device.getTenantId())
              .build();
    }
    dto.setApplicationId(applicationId);
    cacheInvalidationPublisher.publishDeviceChange(IoTDevicePostProcessor.Operation.UPDATE, dto);
  }

  private DeviceBinding binding(IoTUserApplication app, IoTDevice device, boolean bound) {
    return new DeviceBinding(
        app.getAppUniqueId(),
        device.getProductKey(),
        device.getDeviceId(),
        device.getIotId(),
        bound);
  }

  private void assertSameActiveApplication(
      IoTUserApplication authenticated, IoTUserApplication stored) {
    if (stored == null
        || !Objects.equals(authenticated.getAppId(), stored.getAppId())
        || !Objects.equals(authenticated.getUnionId(), stored.getUnionId())
        || !Objects.equals(access.tenantId(authenticated), access.tenantId(stored))
        || !Integer.valueOf(0).equals(stored.getDeleted())
        || !Integer.valueOf(0).equals(stored.getAppStatus())
        || (stored.getValidEndDate() != null && !stored.getValidEndDate().after(new Date()))) {
      throw new AccessDeniedException("当前应用已失效或归属发生变化");
    }
  }

  private static JSONObject parseConfiguration(IoTUserApplication app) {
    if (StrUtil.isBlank(app.getCfg())) return new JSONObject();
    if (!JSONUtil.isTypeJSONObject(app.getCfg())) throw new IoTException("应用推送配置格式错误，请联系管理员修复");
    return JSONUtil.parseObj(app.getCfg());
  }

  private static HttpPushInfo describeHttp(JSONObject cfg) {
    JSONObject http = cfg.getJSONObject("http");
    if (http == null) return new HttpPushInfo(true, false, null, DEFAULT_HTTP_HEADER, false);
    boolean supported = http.getBool("support", true);
    return new HttpPushInfo(
        supported,
        supported && http.getBool("enable", false),
        http.getStr("url"),
        StrUtil.blankToDefault(http.getStr("header"), DEFAULT_HTTP_HEADER),
        StrUtil.isNotBlank(http.getStr("secret")));
  }

  private static void validateUrl(String value) {
    if (StrUtil.isBlank(value) || value.length() > 2048) throw bad("HTTP 推送地址不能为空且长度不能超过 2048");
    try {
      URI uri = URI.create(value);
      String scheme = uri.getScheme();
      if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          || StrUtil.isBlank(uri.getHost())
          || uri.getUserInfo() != null
          || uri.getFragment() != null) {
        throw new IllegalArgumentException();
      }
    } catch (IllegalArgumentException e) {
      throw bad("HTTP 推送地址必须是有效的 HTTP/HTTPS URL，且不能包含用户名密码或片段");
    }
  }

  private static void validateHeader(String value) {
    if (value == null
        || value.length() > 128
        || !HTTP_HEADER_NAME.matcher(value).matches()
        || RESERVED_HEADERS.contains(value.toLowerCase(Locale.ROOT))) {
      throw bad("推送密钥请求头名无效，请使用 X-Nexiot-Secret 等自定义请求头");
    }
  }

  private static void validateSecret(String value) {
    if (StrUtil.isBlank(value)
        || value.length() > 2048
        || value.chars().anyMatch(c -> c < 32 || c == 127)) {
      throw bad("HTTP 推送密钥不能为空、不能包含控制字符且长度不能超过 2048");
    }
  }

  private static AccessDeniedException noDevicePermission() {
    return new AccessDeniedException("设备不存在、无权限或已绑定其他应用");
  }

  private static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
