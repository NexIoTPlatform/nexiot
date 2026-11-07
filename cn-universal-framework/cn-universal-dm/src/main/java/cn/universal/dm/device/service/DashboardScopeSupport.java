package cn.universal.dm.device.service;

import cn.hutool.core.util.StrUtil;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.security.support.DeptDataPermissionSupport;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DashboardScopeSupport {

  @Resource private DeptDataPermissionSupport deptDataPermissionSupport;
  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Resource private IoTProductMapper ioTProductMapper;
  @Resource private IoTUserApplicationMapper ioTUserApplicationMapper;

  public DeptDataPermissionScope resolveScope(String unionId, boolean isAdmin) {
    if (isAdmin || StrUtil.isBlank(unionId)) {
      return null;
    }
    return deptDataPermissionSupport.resolveScope(unionId);
  }

  public void applyScope(Object target, DeptDataPermissionScope scope) {
    if (target == null || scope == null) {
      return;
    }
    deptDataPermissionSupport.applyScope(target, scope);
  }

  public boolean canAccessResource(
      DeptDataPermissionScope scope, String tenantId, Long deptId, String creatorUnionId) {
    return deptDataPermissionSupport.canAccessResource(scope, tenantId, deptId, creatorUnionId);
  }

  public String scopeTenantId(DeptDataPermissionScope scope) {
    return scope == null ? null : scope.getTenantId();
  }

  public List<Long> scopeDeptIds(DeptDataPermissionScope scope) {
    return scope == null ? new ArrayList<>() : new ArrayList<>(scope.getScopeDeptIds());
  }

  public List<String> scopeUnionIds(DeptDataPermissionScope scope) {
    return scope == null ? new ArrayList<>() : new ArrayList<>(scope.getScopeUnionIds());
  }

  public boolean scopeDenied(DeptDataPermissionScope scope, boolean isAdmin) {
    return !isAdmin && (scope == null || scope.isDenied());
  }

  public boolean selfVisible(DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    return isAdmin
        || (scope != null
            && (scope.isAll() || scope.getScopeUnionIds().contains(StrUtil.blankToDefault(unionId, ""))));
  }

  public long countDevices(DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    if (isAdmin) {
      return ioTDeviceMapper.countAllDevices();
    }
    if (scopeDenied(scope, false)) {
      return 0L;
    }
    if (scope != null && scope.isAll()) {
      return ioTDeviceMapper.countDevicesByCreator(unionId);
    }
    return ioTDeviceMapper.countDevicesByScope(
        scopeTenantId(scope), scopeDeptIds(scope), scopeUnionIds(scope), false);
  }

  public long countOnlineDevices(DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    if (isAdmin) {
      return ioTDeviceMapper.countOnlineDevices();
    }
    if (scopeDenied(scope, false)) {
      return 0L;
    }
    if (scope != null && scope.isAll()) {
      return ioTDeviceMapper.countOnlineDevicesByCreator(unionId);
    }
    return ioTDeviceMapper.countOnlineDevicesByScope(
        scopeTenantId(scope), scopeDeptIds(scope), scopeUnionIds(scope), false);
  }

  public long countProducts(DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    if (isAdmin) {
      return ioTProductMapper.countAllProducts();
    }
    if (scopeDenied(scope, false)) {
      return 0L;
    }
    if (scope != null && scope.isAll()) {
      return ioTProductMapper.countProductsByCreator(unionId);
    }
    return ioTProductMapper.countProductsByScope(scopeTenantId(scope), scopeUnionIds(scope), false);
  }

  public long countSelfProducts(DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    if (StrUtil.isBlank(unionId) || !selfVisible(scope, unionId, isAdmin)) {
      return 0L;
    }
    return ioTProductMapper.countProductsCreatedByUser(unionId);
  }

  public long countApplications(DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    if (isAdmin) {
      return ioTUserApplicationMapper.countAllApplications();
    }
    if (scopeDenied(scope, false)) {
      return 0L;
    }
    if (scope != null && scope.isAll()) {
      return ioTUserApplicationMapper.countApplicationsByUnionId(unionId);
    }
    return ioTUserApplicationMapper.countApplicationsByScope(
        scopeTenantId(scope), scopeUnionIds(scope), false);
  }

  public List<Map<String, Object>> getDeviceTypeDistribution(
      DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    if (isAdmin) {
      return ioTDeviceMapper.getDeviceTypeDistribution();
    }
    if (scopeDenied(scope, false)) {
      return Collections.emptyList();
    }
    if (scope != null && scope.isAll()) {
      return ioTDeviceMapper.getDeviceTypeDistributionByCreator(unionId);
    }
    return ioTDeviceMapper.getDeviceTypeDistributionByScope(
        scopeTenantId(scope), scopeDeptIds(scope), scopeUnionIds(scope), false);
  }

  public List<Map<String, Object>> getProductPlatformDistribution(
      DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    if (isAdmin) {
      return ioTDeviceMapper.getProductPlatformDistribution();
    }
    if (scopeDenied(scope, false)) {
      return Collections.emptyList();
    }
    if (scope != null && scope.isAll()) {
      return ioTDeviceMapper.getProductPlatformDistributionByCreator(unionId);
    }
    return ioTDeviceMapper.getProductPlatformDistributionByScope(
        scopeTenantId(scope), scopeDeptIds(scope), scopeUnionIds(scope), false);
  }

  public List<Map<String, Object>> getTransportProtocolDistribution(
      DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    if (isAdmin) {
      return ioTDeviceMapper.getTransportProtocolDistribution();
    }
    if (scopeDenied(scope, false)) {
      return Collections.emptyList();
    }
    if (scope != null && scope.isAll()) {
      return ioTDeviceMapper.getTransportProtocolDistributionByCreator(unionId);
    }
    return ioTDeviceMapper.getTransportProtocolDistributionByScope(
        scopeTenantId(scope), scopeDeptIds(scope), scopeUnionIds(scope), false);
  }

  public List<Map<String, Object>> getMessageProtocolDistribution(
      DeptDataPermissionScope scope, String unionId, boolean isAdmin) {
    if (isAdmin) {
      return ioTDeviceMapper.getMessageProtocolDistribution();
    }
    if (scopeDenied(scope, false)) {
      return Collections.emptyList();
    }
    if (scope != null && scope.isAll()) {
      return ioTDeviceMapper.getMessageProtocolDistributionByCreator(unionId);
    }
    return ioTDeviceMapper.getMessageProtocolDistributionByScope(
        scopeTenantId(scope), scopeDeptIds(scope), scopeUnionIds(scope), false);
  }

  public List<Map<String, Object>> getProductRanking(
      DeptDataPermissionScope scope, Integer limit, String unionId, boolean isAdmin) {
    if (isAdmin) {
      return ioTDeviceMapper.getProductRanking(limit);
    }
    if (scopeDenied(scope, false)) {
      return Collections.emptyList();
    }
    if (scope != null && scope.isAll()) {
      return ioTDeviceMapper.getProductRankingByCreator(unionId, limit);
    }
    return ioTDeviceMapper.getProductRankingByScope(
        limit, scopeTenantId(scope), scopeDeptIds(scope), scopeUnionIds(scope), false);
  }
}
