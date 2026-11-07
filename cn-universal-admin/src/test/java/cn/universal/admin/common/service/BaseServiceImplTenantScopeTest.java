package cn.universal.admin.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.service.IoTUserService;
import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class BaseServiceImplTenantScopeTest {

  @Mock private IoTUserService ioTUserService;

  private TestService service;

  @BeforeEach
  void setUp() throws Exception {
    service = new TestService();
    Field field = BaseServiceImpl.class.getDeclaredField("ioTUserService");
    field.setAccessible(true);
    field.set(service, ioTUserService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void normalizeTenantIdShouldRejectCrossTenantRequestForTenantUser() {
    setJwt("user-1");
    when(ioTUserService.selectUserByUnionId("user-1"))
        .thenReturn(IoTUser.builder().identity(1).unionId("user-1").tenantId("tenant-a").build());

    assertThrows(IoTException.class, () -> service.normalize("tenant-b"));
  }

  @Test
  void normalizeTenantIdShouldUseCurrentTenantForBlankTenantUserRequest() {
    setJwt("user-1");
    when(ioTUserService.selectUserByUnionId("user-1"))
        .thenReturn(IoTUser.builder().identity(1).unionId("user-1").tenantId("tenant-a").build());

    assertEquals("tenant-a", service.normalize(null));
  }

  @Test
  void normalizeTenantIdShouldKeepRequestedTenantForPlatformAdmin() {
    setJwt("admin");
    when(ioTUserService.selectUserByUnionId("admin"))
        .thenReturn(IoTUser.builder().identity(0).unionId("admin").build());

    assertEquals("tenant-b", service.normalize("tenant-b"));
  }

  private void setJwt(String unionId) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(unionId)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  private static final class TestService extends BaseServiceImpl {

    private String normalize(String tenantId) {
      return normalizeTenantIdForCurrentUser(tenantId);
    }
  }
}
