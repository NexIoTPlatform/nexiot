package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.persistence.entity.VideoPlatformInstance;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class OpenApiVideoScopeTest {
  private Authentication authentication;

  @BeforeEach
  void authenticate() {
    authentication = token("app-id", "tenant-a");
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void scopeGrantsOnlyExplicitOperationsAndPreservesOriginalAuthentication() {
    var permissions = mock(SysPermissionService.class);
    var access = new VideoOperationAccess(permissions);
    var instance = VideoPlatformInstance.builder().instanceKey("instance").tenantId("tenant-a").build();
    assertTrue(OpenApiVideoScope.call(scope("app-id", "tenant-a", Set.of("snapshot", "preview")), () -> {
      assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
      assertTrue(OpenApiVideoScope.assertInstanceIfPresent(instance));
      assertTrue(access.allowed("snapshot"));
      assertTrue(access.allowed("preview"));
      assertFalse(access.allowed("ptz"));
      assertThrows(AccessDeniedException.class, () -> access.assertUserOperation("deviceConfigApply"));
      verifyNoInteractions(permissions);
      return true;
    }));
    assertNull(OpenApiVideoScope.current());
    assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void cleanupOccursEvenWhenWorkFails() {
    assertThrows(IllegalStateException.class, () -> OpenApiVideoScope.call(scope(), () -> {
      throw new IllegalStateException("failed");
    }));
    assertNull(OpenApiVideoScope.current());
    assertNull(OpenApiVideoScope.operationAllowed("preview"));
  }

  @Test
  void scopeDoesNotPropagateToAsyncWorkers() {
    OpenApiVideoScope.call(scope(), () -> {
      assertNull(CompletableFuture.supplyAsync(OpenApiVideoScope::current).join());
      return null;
    });
  }

  @Test
  void untrustedOrChangedAuthenticationCannotUseScope() {
    assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.call(
        scope("another-app", "tenant-a", Set.of("preview")), () -> null));
    assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.call(
        scope("app-id", "tenant-b", Set.of("preview")), () -> null));
    authentication.setAuthenticated(false);
    assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.call(scope(), () -> null));
    assertNull(OpenApiVideoScope.current());
  }

  @Test
  void replacingOrClearingAuthenticationFailsClosedAndStillCleansUp() {
    for (Authentication replacement : new Authentication[] {token("app-id", "tenant-a"), null}) {
      SecurityContextHolder.getContext().setAuthentication(authentication);
      assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.call(scope(), () -> {
        SecurityContextHolder.getContext().setAuthentication(replacement);
        assertThrows(AccessDeniedException.class,
            () -> new VideoOperationAccess(mock(SysPermissionService.class)).assertUserOperation("preview"));
        return OpenApiVideoScope.operationAllowed("preview");
      }));
      assertNull(OpenApiVideoScope.current());
    }
  }

  @Test
  void exactInstanceTenantAndChannelAreRequired() {
    OpenApiVideoScope.call(scope(), () -> {
      assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.assertInstanceIfPresent(
          VideoPlatformInstance.builder().instanceKey("other").tenantId("tenant-a").build()));
      assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.assertInstanceIfPresent(
          VideoPlatformInstance.builder().instanceKey("instance").tenantId("tenant-b").build()));
      assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.assertChannelIfPresent("instance", "other", "channel"));
      assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.assertChannelIfPresent("instance", "device", "other"));
      assertDoesNotThrow(() -> OpenApiVideoScope.assertChannelIfPresent("instance", "device", "channel"));
      return null;
    });
  }

  @Test
  void nestedScopesAndManagementOperationsAreRejected() {
    assertThrows(AccessDeniedException.class,
        () -> scope("app-id", "tenant-a", Set.of("configureActiveRegistration")));
    assertThrows(AccessDeniedException.class,
        () -> scope("app-id", "tenant-a", Set.of()));
    OpenApiVideoScope.call(scope(), () -> {
      assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.call(scope(), () -> null));
      assertTrue(OpenApiVideoScope.operationAllowed("preview"));
      return null;
    });
  }

  @Test
  void ordinaryCallsRetainMenuPermissions() {
    var permissions = mock(SysPermissionService.class);
    when(permissions.getCurrentPermissions()).thenReturn(Set.of());
    var access = new VideoOperationAccess(permissions);
    assertFalse(access.allowed("preview"));
    assertThrows(AccessDeniedException.class, () -> access.assertUserOperation("preview"));
    verify(permissions, atLeastOnce()).getCurrentPermissions();
  }

  private static OpenApiVideoScope.Scope scope() {
    return scope("app-id", "tenant-a", Set.of("preview"));
  }

  private static OpenApiVideoScope.Scope scope(String subject, String tenant, Set<String> operations) {
    return new OpenApiVideoScope.Scope(subject, tenant, "application", "instance", "device", "channel", operations);
  }

  private static Authentication token(String subject, String tenant) {
    return new JwtAuthenticationToken(Jwt.withTokenValue("test").header("alg", "none")
        .subject(subject).claim("tenantId", tenant).build(), List.of());
  }
}
