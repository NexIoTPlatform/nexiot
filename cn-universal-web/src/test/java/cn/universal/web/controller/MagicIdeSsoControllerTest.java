package cn.universal.web.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.TenantDomainResolver;
import java.time.Instant;
import java.util.Set;
import nexiot.web.ide.debug.plugins.auth.MagicSsoSessionService;
import nexiot.web.ide.debug.plugins.core.exception.MagicLoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;

class MagicIdeSsoControllerTest {
  final JwtDecoder decoder = mock(JwtDecoder.class);
  final IoTUserService users = mock(IoTUserService.class);
  final SysPermissionService permissions = mock(SysPermissionService.class);
  final TenantDomainResolver domains = mock(TenantDomainResolver.class);
  final MagicSsoSessionService sessions = new MagicSsoSessionService();
  final MagicIdeSsoController controller =
      new MagicIdeSsoController(decoder, users, permissions, domains, sessions);
  final IoTUser user = new IoTUser();
  Jwt jwt;

  @BeforeEach
  void setup() {
    user.setUnionId("user-a");
    user.setTenantId("tenant-a");
    user.setStatus(0);
    user.setDeleted(0);
    jwt = token("tenant-a");
    when(users.selectUserByUnionId("user-a")).thenReturn(user);
    when(permissions.getMenuPermission(user)).thenReturn(Set.of("protocol:protocol:codec"));
    when(decoder.decode("platform.jwt")).thenReturn(jwt);
  }

  Jwt token(String tenant) {
    return Jwt.withTokenValue("platform.jwt")
        .header("alg", "RS256")
        .subject("user-a")
        .claim("tenantId", tenant)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(900))
        .build();
  }

  String exchange() throws Exception {
    return (String) controller.exchange(jwt).getBody().get("token");
  }

  @Test
  void exchangesWithoutExposingPlatformJwtAndDisablesCaching() throws Exception {
    var response = controller.exchange(jwt);
    assertEquals("no-store", response.getHeaders().getCacheControl());
    assertEquals("user-a", response.getBody().get("userId"));
    assertEquals("tenant-a", response.getBody().get("tenantId"));
    assertFalse(response.getBody().toString().contains("platform.jwt"));
    String token = (String) response.getBody().get("token");
    assertEquals("user-a", sessions.getUser(token).getId());
    verify(decoder).decode("platform.jwt");
  }

  @Test
  void unauthenticatedIsRejected() {
    var error = assertThrows(ResponseStatusException.class, () -> controller.exchange(null));
    assertEquals(401, error.getStatusCode().value());
  }

  @Test
  void permissionRequiredEvenWhenLoggedIn() {
    when(permissions.getMenuPermission(user)).thenReturn(Set.of());
    assertEquals(
        403, assertThrows(ResponseStatusException.class, () -> exchange()).getStatusCode().value());
  }

  @Test
  void accountDisabledOrDeletedIsRejected() {
    user.setStatus(1);
    assertThrows(ResponseStatusException.class, () -> exchange());
    user.setStatus(0);
    user.setDeleted(1);
    assertThrows(ResponseStatusException.class, () -> exchange());
  }

  @Test
  void tokenTenantMismatchRejected() {
    jwt = token("tenant-b");
    assertThrows(ResponseStatusException.class, () -> exchange());
  }

  @Test
  void hostTenantMismatchRejected() {
    IoTTenant host = new IoTTenant();
    host.setTenantId("tenant-b");
    when(domains.resolveCurrentTenant()).thenReturn(host);
    assertThrows(ResponseStatusException.class, () -> exchange());
  }

  @Test
  void permissionRemovalRevokesExistingIdeSession() throws Exception {
    String token = exchange();
    when(permissions.getMenuPermission(user)).thenReturn(Set.of());
    assertThrows(MagicLoginException.class, () -> sessions.getUser(token));
  }

  @Test
  void platformDecoderRejectionRevokesExistingSession() throws Exception {
    String token = exchange();
    when(decoder.decode("platform.jwt")).thenThrow(new IllegalArgumentException("expired"));
    assertThrows(MagicLoginException.class, () -> sessions.getUser(token));
  }

  @Test
  void tenantChangeRevokesExistingSession() throws Exception {
    String token = exchange();
    user.setTenantId("tenant-b");
    assertThrows(MagicLoginException.class, () -> sessions.getUser(token));
  }

  @Test
  void applicationTokenIsNotAnInteractiveUser() {
    jwt =
        Jwt.withTokenValue("app.jwt")
            .header("alg", "RS256")
            .subject("user-a")
            .claim("clientId", "user-a")
            .expiresAt(Instant.now().plusSeconds(900))
            .build();
    assertThrows(ResponseStatusException.class, () -> exchange());
  }
}
