package cn.universal.admin.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.security.support.TenantDomainResolver;
import cn.universal.security.support.TenantLoginResolver;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

@ExtendWith(MockitoExtension.class)
class TenantLoginResolverTest {

  @InjectMocks private TenantLoginResolver tenantLoginResolver;

  @Mock private IoTUserMapper ioTUserMapper;
  @Mock private IoTTenantMapper ioTTenantMapper;
  @Mock private TenantDomainResolver tenantDomainResolver;

  @BeforeAll
  static void initEntityTables() {
    TkMapperEntityTestSupport.registerEntityTables(IoTUser.class, IoTTenant.class);
  }

  @Test
  void resolveLoginUserShouldPreferCurrentDomainTenantWhenTenantCodeMissing() {
    IoTUser tenantAUser = tenantUser(1L, "tenant-a", "demo");
    IoTUser tenantBUser = tenantUser(2L, "tenant-b", "demo");

    when(ioTUserMapper.selectByExample(any())).thenReturn(List.of(tenantAUser, tenantBUser));
    when(tenantDomainResolver.resolveCurrentTenant())
        .thenReturn(IoTTenant.builder().tenantId("tenant-b").tenantCode("tenant-b").build());

    IoTUser resolvedUser = tenantLoginResolver.resolveLoginUser("demo", null);

    assertEquals("tenant-b", resolvedUser.getTenantId());
  }

  @Test
  void resolveLoginUserShouldAllowPlatformAdminWhenCurrentDomainHasNoMatchingTenantUser() {
    IoTUser platformAdmin =
        IoTUser.builder()
            .id(1L)
            .username("admin")
            .unionId("admin")
            .identity(0)
            .deleted(0)
            .build();

    when(ioTUserMapper.selectByExample(any())).thenReturn(List.of(platformAdmin));
    when(tenantDomainResolver.resolveCurrentTenant())
        .thenReturn(IoTTenant.builder().tenantId("tenant-a").tenantCode("tenant-a").build());

    IoTUser resolvedUser = tenantLoginResolver.resolveLoginUser("admin", null);

    assertEquals("admin", resolvedUser.getUnionId());
  }

  @Test
  void resolveLoginUserShouldRejectTenantUserOnWrongDomain() {
    IoTUser tenantUser = tenantUser(1L, "tenant-b", "demo");

    when(ioTUserMapper.selectByExample(any())).thenReturn(List.of(tenantUser));
    when(tenantDomainResolver.resolveCurrentTenant())
        .thenReturn(IoTTenant.builder().tenantId("tenant-a").tenantCode("tenant-a").build());

    OAuth2AuthenticationException exception =
        assertThrows(
            OAuth2AuthenticationException.class,
            () -> tenantLoginResolver.resolveLoginUser("demo", null));

    assertEquals("当前账号不属于当前域名对应租户", exception.getError().getDescription());
  }

  private static IoTUser tenantUser(Long id, String tenantId, String username) {
    return IoTUser.builder()
        .id(id)
        .tenantId(tenantId)
        .tenantRole("MEMBER")
        .username(username)
        .unionId(username + "-" + tenantId)
        .identity(1)
        .deleted(0)
        .build();
  }
}
