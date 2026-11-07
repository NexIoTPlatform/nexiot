package cn.universal.admin.system.web;

import cn.universal.admin.system.service.IIoTTenantService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.TenantBrandingBO;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system/tenant")
@Tag(name = "租户管理")
public class IoTTenantController extends BaseController {

  @Resource private IIoTTenantService tenantService;
  @Resource private ObjectMapper objectMapper;

  @GetMapping("/public/list")
  public AjaxResult<java.util.List<IoTTenant>> publicList() {
    return AjaxResult.success(tenantService.selectLoginTenantList());
  }

  @GetMapping("/public/resolve")
  public AjaxResult<IoTTenant> publicResolve(@RequestParam(required = false) String host) {
    return AjaxResult.success(tenantService.resolveLoginTenantByHost(host));
  }

  @GetMapping("/list")
  public TableDataInfo<IoTTenant> list(IoTTenant query) {
    ensurePlatformAdmin();
    startPage();
    return getDataTable(tenantService.selectTenantList(query));
  }

  @GetMapping("/{tenantId}")
  public AjaxResult<IoTTenant> getInfo(@PathVariable String tenantId) {
    ensurePlatformAdmin();
    return AjaxResult.success(tenantService.selectTenantById(tenantId));
  }

  @GetMapping("/current/branding")
  public AjaxResult<TenantBrandingBO> getCurrentBranding() {
    IoTUser currentUser = requireTenantUser();
    return AjaxResult.success(tenantService.selectTenantBranding(currentUser.getTenantId()));
  }

  @PutMapping("/current/branding")
  @Log(title = "租户平台设置", businessType = BusinessType.UPDATE)
  public AjaxResult<TenantBrandingBO> updateCurrentBranding(
      @RequestBody TenantBrandingBO branding) {
    IoTUser currentUser = requireTenantUser();
    if (!"ADMIN".equalsIgnoreCase(currentUser.getTenantRole())
        && !"TENANT_ADMIN".equalsIgnoreCase(currentUser.getTenantRole())) {
      throw new IoTException("仅租户管理员可修改平台设置");
    }
    return AjaxResult.success(
        tenantService.updateTenantBranding(currentUser.getTenantId(), branding));
  }

  @PostMapping
  @Log(title = "租户", businessType = BusinessType.INSERT)
  public AjaxResult<Void> add(@RequestBody IoTTenant tenant) {
    ensurePlatformAdmin();
    return toAjax(tenantService.insertTenant(tenant));
  }

  @PutMapping
  @Log(title = "租户", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> edit(@RequestBody Map<String, Object> body) {
    ensurePlatformAdmin();
    Object rawExpireTime = body.get("expireTime");
    boolean clearExpireTime =
        body.containsKey("expireTime")
            && (rawExpireTime == null || rawExpireTime.toString().trim().isEmpty());
    if (clearExpireTime) {
      body.put("expireTime", null);
    }
    IoTTenant tenant = objectMapper.convertValue(body, IoTTenant.class);
    return toAjax(tenantService.updateTenant(tenant, clearExpireTime));
  }

  @PutMapping("/changeStatus")
  @Log(title = "租户状态", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> changeStatus(@RequestBody IoTTenant tenant) {
    ensurePlatformAdmin();
    return toAjax(tenantService.updateTenantStatus(tenant));
  }

  @DeleteMapping("/{tenantIds}")
  @Log(title = "租户", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable String[] tenantIds) {
    ensurePlatformAdmin();
    return toAjax(tenantService.deleteTenantByIds(tenantIds));
  }

  private void ensurePlatformAdmin() {
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || !currentUser.isAdmin()) {
      throw new IoTException("仅平台管理员可操作租户");
    }
  }

  private IoTUser requireTenantUser() {
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || currentUser.isAdmin() || currentUser.getTenantId() == null) {
      throw new IoTException("当前账号未关联租户");
    }
    return currentUser;
  }
}
