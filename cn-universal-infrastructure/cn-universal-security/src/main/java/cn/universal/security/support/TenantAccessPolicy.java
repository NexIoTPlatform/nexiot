package cn.universal.security.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;

/** Shared tenant-scope and tenant-resource access policy. */
public final class TenantAccessPolicy {

  private TenantAccessPolicy() {}

  public static String resolveTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    return normalize(StrUtil.blankToDefault(user.getTenantId(), user.getUnionId()));
  }

  /** Platform administrators keep the requested scope; tenant users are locked to their tenant. */
  public static String normalizeTenantIdForCurrentUser(
      IoTUser currentUser, String requestedTenantId) {
    String requested = normalize(requestedTenantId);
    if (currentUser == null || currentUser.isAdmin()) {
      return requested;
    }
    String currentTenantId = resolveTenantId(currentUser);
    if (StrUtil.isNotBlank(requested) && !StrUtil.equals(currentTenantId, requested)) {
      throw new IoTException("无权操作其他租户数据", HttpStatus.HTTP_FORBIDDEN);
    }
    return currentTenantId;
  }

  public static boolean canAccessTenantResource(
      IoTUser currentUser,
      String resourceTenantId,
      String creatorId,
      String creatorTenantId) {
    if (currentUser == null || currentUser.isAdmin()) {
      return true;
    }
    String currentTenantId = resolveTenantId(currentUser);
    String effectiveResourceTenantId =
        StrUtil.blankToDefault(normalize(resourceTenantId), normalize(creatorTenantId));
    if (StrUtil.isBlank(effectiveResourceTenantId)) {
      return StrUtil.equals(currentUser.getUnionId(), creatorId);
    }
    return StrUtil.equals(currentTenantId, effectiveResourceTenantId);
  }

  private static String normalize(String value) {
    return StrUtil.trimToNull(value);
  }
}
