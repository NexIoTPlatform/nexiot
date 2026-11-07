package cn.universal.security.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.http.HttpStatus;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import org.junit.jupiter.api.Test;

class TenantAccessPolicyTest {

  @Test
  void platformAdminUsesGlobalScopeWhenTenantIsNotRequested() {
    IoTUser admin = IoTUser.builder().identity(0).unionId("admin").tenantId("system").build();

    assertNull(TenantAccessPolicy.normalizeTenantIdForCurrentUser(admin, "  "));
    assertEquals(
        "tenant-b", TenantAccessPolicy.normalizeTenantIdForCurrentUser(admin, " tenant-b "));
    assertTrue(
        TenantAccessPolicy.canAccessTenantResource(
            admin, "tenant-b", "creator-b", "tenant-b"));
  }

  @Test
  void tenantUserIsLockedToOwnTenant() {
    IoTUser user = IoTUser.builder().identity(1).unionId("user-a").tenantId("tenant-a").build();

    assertEquals("tenant-a", TenantAccessPolicy.normalizeTenantIdForCurrentUser(user, null));
    IoTException error =
        assertThrows(
            IoTException.class,
            () -> TenantAccessPolicy.normalizeTenantIdForCurrentUser(user, "tenant-b"));
    assertEquals(HttpStatus.HTTP_FORBIDDEN, error.getCode());
  }
}
