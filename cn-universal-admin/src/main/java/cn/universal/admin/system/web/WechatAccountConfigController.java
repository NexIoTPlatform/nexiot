package cn.universal.admin.system.web;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.wechat.WechatAccountConfigService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 公众号登录管理。请求含凭据，禁止通过通用操作日志记录请求体。 */
@RestController
@RequestMapping("/admin/system/wechat")
public class WechatAccountConfigController extends BaseController {
  private final WechatAccountConfigService service;

  public WechatAccountConfigController(WechatAccountConfigService service) { this.service = service; }

  @GetMapping("/config")
  public AjaxResult<Map<String, Object>> config(@RequestParam(required = false) String tenantId) {
    return AjaxResult.success(service.safeConfig(managedTenant(tenantId)));
  }

  public record ConfigRequest(String accountName, String appId, String appSecret,
      String callbackToken, String encodingAESKey, String accountQrUrl, String capabilityMode,
      String loginMode, Boolean enabled, Boolean bindingEnabled, String demoUserUnionId,
      Integer expireSeconds, Long configVersion, Boolean clearEncodingAESKey) {
    @Override public String toString() { return "WechatConfigRequest[credentials redacted]"; }
  }

  @PutMapping("/config")
  public AjaxResult<Map<String, Object>> save(@RequestParam(required = false) String tenantId,
      @RequestBody ConfigRequest request) {
    String currentTenant = managedTenant(tenantId);
    WechatLoginConfig value = new WechatLoginConfig();
    value.setAccountName(request.accountName());
    value.setAppId(request.appId());
    value.setAppSecret(request.appSecret());
    value.setCallbackToken(request.callbackToken());
    value.setEncodingAESKey(request.encodingAESKey());
    value.setAccountQrUrl(request.accountQrUrl());
    value.setCapabilityMode(request.capabilityMode());
    value.setLoginMode(request.loginMode());
    value.setEnabled(request.enabled());
    value.setBindingEnabled(request.bindingEnabled());
    value.setDemoUserUnionId(request.demoUserUnionId());
    value.setExpireSeconds(request.expireSeconds());
    value.setConfigVersion(request.configVersion());
    service.save(currentTenant, value, Boolean.TRUE.equals(request.clearEncodingAESKey()));
    return AjaxResult.success(service.safeConfig(currentTenant));
  }

  @GetMapping("/users")
  public AjaxResult<List<Map<String, String>>> users(@RequestParam(required = false) String tenantId,
      @RequestParam(required = false) String keyword) {
    return AjaxResult.success(service.availableDemoUsers(managedTenant(tenantId), keyword));
  }

  private String managedTenant(String requestedTenantId) {
    IoTUser current = currentTenantUser();
    if (current == null || Integer.valueOf(1).equals(current.getStatus())
        || Integer.valueOf(1).equals(current.getDeleted())) throw new IoTException("当前登录账号不可用");
    if (current.isAdmin()) {
      if (requestedTenantId == null || requestedTenantId.isBlank())
        throw new IoTException("请先指定需要管理的租户");
      return requestedTenantId;
    }
    if (current.getTenantId() == null || current.getTenantId().isBlank())
      throw new IoTException("当前账号未关联租户");
    if (!"ADMIN".equalsIgnoreCase(current.getTenantRole())
        && !"TENANT_ADMIN".equalsIgnoreCase(current.getTenantRole()))
      throw new IoTException("仅租户管理员可管理公众号登录设置");
    if (requestedTenantId != null && !current.getTenantId().equals(requestedTenantId))
      throw new IoTException("不能管理其他租户的公众号设置");
    return current.getTenantId();
  }
}
