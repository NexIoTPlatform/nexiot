/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OSS租户归属解析
 *
 */

package cn.universal.ossm.oss.context;

import cn.hutool.core.util.StrUtil;
import cn.universal.persistence.entity.IoTUser;

/** 统一 OSS 配置归属规则，避免把超级管理员 unionId 当成租户ID。 */
public final class OssTenantResolver {

  public static final String GLOBAL_TENANT_ID = "";
  public static final String SYSTEM_RESOURCE_TENANT_ID = "system";
  public static final String PUBLIC_RESOURCE_TENANT_ID = "public";
  public static final String SCADA_SYSTEM_RESOURCE_TENANT_ID = "scada-system";
  /** @deprecated sys_oss_config uses {@link #GLOBAL_TENANT_ID}; legacy "system" is data to clean. */
  @Deprecated
  public static final String SYSTEM_TENANT_ID = GLOBAL_TENANT_ID;
  public static final String LEGACY_SYSTEM_TENANT_ID = SYSTEM_RESOURCE_TENANT_ID;
  private static final String SYSTEM_OPERATOR = "system";

  private OssTenantResolver() {}

  public static String resolveOperator(IoTUser user) {
    if (user == null) {
      return SYSTEM_OPERATOR;
    }
    return StrUtil.blankToDefault(user.getUnionId(), SYSTEM_OPERATOR);
  }

  /**
   * sys_oss_config 的全局兜底配置使用空租户ID，只允许真实租户ID或空租户。
   *
   * <p>"system"、"public"、"scada-system" 是历史或资源归属保留值，不是租户配置归属。
   * 对配置读写统一归一化到空租户ID，并由数据库迁移脚本禁止新的保留值记录进入。
   */
  public static String normalizeConfigTenantId(String tenantId) {
    String value = StrUtil.trim(tenantId);
    if (StrUtil.isBlank(value) || isReservedResourceTenantId(value)) {
      return GLOBAL_TENANT_ID;
    }
    return value;
  }

  public static boolean isReservedResourceTenantId(String tenantId) {
    String value = StrUtil.trim(tenantId);
    return LEGACY_SYSTEM_TENANT_ID.equalsIgnoreCase(value)
        || PUBLIC_RESOURCE_TENANT_ID.equalsIgnoreCase(value)
        || SCADA_SYSTEM_RESOURCE_TENANT_ID.equalsIgnoreCase(value);
  }

  public static boolean isGlobalConfigTenantId(String tenantId) {
    return StrUtil.isBlank(normalizeConfigTenantId(tenantId));
  }

  public static String resolveConfigTenantId(IoTUser user) {
    if (user == null || user.isAdmin()) {
      return GLOBAL_TENANT_ID;
    }
    return normalizeConfigTenantId(StrUtil.blankToDefault(user.getTenantId(), user.getUnionId()));
  }

  public static String resolveResourceTenantId(IoTUser user) {
    if (user == null) {
      return null;
    }
    if (user.isAdmin()) {
      return SYSTEM_RESOURCE_TENANT_ID;
    }
    return StrUtil.blankToDefault(user.getTenantId(), user.getUnionId());
  }

  /** @deprecated Use {@link #resolveResourceTenantId(IoTUser)} for sys_oss resource ownership. */
  @Deprecated
  public static String resolveTenantId(IoTUser user) {
    return resolveResourceTenantId(user);
  }
}
