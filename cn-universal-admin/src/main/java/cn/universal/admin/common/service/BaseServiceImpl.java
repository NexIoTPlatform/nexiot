/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.common.service;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.page.PageContextRunner;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.DeptDataPermissionSupport;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import cn.universal.security.support.TenantAccessPolicy;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;

public class BaseServiceImpl implements BaseService {

  @Resource private IoTUserService ioTUserService;
  @Resource private DeptDataPermissionSupport deptDataPermissionSupport;

  @Override
  @Cacheable(cacheNames = "user_parent_entity", key = "''+#unionId", unless = "#result == null")
  public IoTUser queryIotUser(String unionId) {
    return ioTUserService.selectUserByUnionId(unionId);
  }

  protected IoTUser currentTenantUser() {
    String unionId = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    return queryIotUser(unionId);
  }

  protected String resolveTenantId(IoTUser iotUser) {
    return TenantAccessPolicy.resolveTenantId(iotUser);
  }

  protected String normalizeTenantIdForCurrentUser(String requestedTenantId) {
    return TenantAccessPolicy.normalizeTenantIdForCurrentUser(
        currentTenantUser(), requestedTenantId);
  }

  protected String resolveTenantId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    return resolveTenantId(ioTUserService.selectUserByUnionId(unionId));
  }

  protected String resolveTenantOwnerUnionId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    IoTUser currentUser = ioTUserService.selectUserByUnionId(unionId);
    if (currentUser == null) {
      return unionId;
    }
    if (currentUser.isAdmin() || StrUtil.equalsIgnoreCase(currentUser.getTenantRole(), "ADMIN")) {
      return currentUser.getUnionId();
    }
    String tenantId = resolveTenantId(currentUser);
    if (StrUtil.isBlank(tenantId)) {
      return currentUser.getUnionId();
    }
    java.util.List<IoTUser> adminUsers =
        PageContextRunner.runWithoutPaging(
            () ->
                ioTUserService.selectUserList(
                    IoTUser.builder().tenantId(tenantId).tenantRole("ADMIN").build()));
    return adminUsers == null || adminUsers.isEmpty()
        ? currentUser.getUnionId()
        : adminUsers.get(0).getUnionId();
  }

  protected boolean canAccessTenantResource(String tenantId, String creatorId) {
    IoTUser currentUser = currentTenantUser();
    return TenantAccessPolicy.canAccessTenantResource(
        currentUser, tenantId, creatorId, resolveTenantId(creatorId));
  }

  protected void assertTenantResourceAccess(String tenantId, String creatorId, String message) {
    if (!canAccessTenantResource(tenantId, creatorId)) {
      throw new IoTException(message);
    }
  }

  protected DeptDataPermissionScope resolveDataPermissionScope(IoTUser fallbackUser) {
    DeptDataPermissionScope scope = deptDataPermissionSupport.resolveCurrentScope();
    if (scope != null || fallbackUser == null || StrUtil.isBlank(fallbackUser.getUnionId())) {
      return scope;
    }
    return deptDataPermissionSupport.resolveScope(fallbackUser.getUnionId());
  }

  protected void applyDataPermissionScope(Object target, IoTUser fallbackUser) {
    DeptDataPermissionScope scope = resolveDataPermissionScope(fallbackUser);
    if (scope == null) {
      return;
    }
    deptDataPermissionSupport.applyScope(target, scope);
  }

  protected boolean canAccessDeptResource(
      String tenantId, Long deptId, String creatorId, IoTUser fallbackUser) {
    DeptDataPermissionScope scope = resolveDataPermissionScope(fallbackUser);
    return deptDataPermissionSupport.canAccessResource(scope, tenantId, deptId, creatorId);
  }

  protected void assertDeptResourceAccess(
      String tenantId, Long deptId, String creatorId, IoTUser fallbackUser, String message) {
    if (!canAccessDeptResource(tenantId, deptId, creatorId, fallbackUser)) {
      throw new IoTException(message);
    }
  }
}
