package cn.universal.admin.system.service;

import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.bo.TenantBrandingBO;
import java.util.List;

public interface IIoTTenantService {

  List<IoTTenant> selectTenantList(IoTTenant query);

  List<IoTTenant> selectLoginTenantList();

  IoTTenant resolveLoginTenantByHost(String host);

  IoTTenant selectTenantById(String tenantId);

  TenantBrandingBO selectTenantBranding(String tenantId);

  TenantBrandingBO updateTenantBranding(String tenantId, TenantBrandingBO branding);

  int insertTenant(IoTTenant tenant);

  int updateTenant(IoTTenant tenant);

  int updateTenant(IoTTenant tenant, boolean clearExpireTime);

  int updateTenantStatus(IoTTenant tenant);

  int deleteTenantByIds(String[] tenantIds);

  void assertTenantUserQuota(String tenantId, int increment);

  Long countActiveUsers(String tenantId);

  void ensureTenantAdminRoleBinding(String tenantId, String adminUnionId);

  void syncTenantRoleMenusByPackage(Long packageId);

  IoTTenant createBootstrapTenant(
      String tenantCode,
      String tenantName,
      String contactName,
      String contactMobile,
      String adminUnionId,
      String operatorUnionId);
}
