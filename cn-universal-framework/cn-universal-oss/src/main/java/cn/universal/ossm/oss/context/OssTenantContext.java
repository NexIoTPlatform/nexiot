/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OSS租户上下文
 *
 */

package cn.universal.ossm.oss.context;

import java.util.function.Supplier;

public final class OssTenantContext {

  private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

  private OssTenantContext() {}

  public static String getTenantId() {
    return TENANT_ID.get();
  }

  public static <T> T run(String tenantId, Supplier<T> supplier) {
    String oldTenantId = TENANT_ID.get();
    TENANT_ID.set(tenantId);
    try {
      return supplier.get();
    } finally {
      if (oldTenantId == null) {
        TENANT_ID.remove();
      } else {
        TENANT_ID.set(oldTenantId);
      }
    }
  }

  public static void run(String tenantId, Runnable runnable) {
    run(
        tenantId,
        () -> {
          runnable.run();
          return null;
        });
  }
}
