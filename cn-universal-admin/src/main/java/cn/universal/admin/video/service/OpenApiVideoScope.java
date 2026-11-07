package cn.universal.admin.video.service;

import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.security.utils.SecurityUtils;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Request-local authority established after the OpenAPI controller has checked the application's
 * IoT device grant and the device/channel/tenant relationship. This does not authenticate callers or
 * grant access to an application by itself, and must never be populated from request JSON.
 *
 * <p>The original JWT authentication remains installed for audit, OSS and downstream tenant checks.
 * Authority is limited to one channel and an explicit set of playback operations, and does not flow
 * to asynchronous jobs or survive the call.
 */
public final class OpenApiVideoScope {
  private static final Set<String> PUBLIC_OPERATIONS = Set.of(
      "preview", "stopPreview", "previewHeartbeat", "snapshot", "snapshotStatus", "snapshotCancel",
      "recordingSearch", "ptz",
      "playbackStreamStart", "playbackStreamStatus", "playbackStreamStop", "playbackStreamPause",
      "playbackStreamResume", "playbackStreamSeek", "playbackStreamSpeed");
  private static final ThreadLocal<BoundScope> CURRENT = new ThreadLocal<>();

  private OpenApiVideoScope() {}

  public record Scope(
      String subject, String tenantId, String applicationId, String instanceKey, String deviceId,
      String channelId, Set<String> operations) {
    public Scope {
      if (blank(subject) || blank(tenantId) || blank(instanceKey) || blank(deviceId) || blank(channelId))
        throw denied("视频开放接口授权上下文不完整");
      if (operations == null || operations.isEmpty() || !PUBLIC_OPERATIONS.containsAll(operations))
        throw denied("视频开放接口操作超出授权范围");
      operations = Set.copyOf(operations);
    }
  }

  public static <T> T call(Scope scope, Supplier<T> action) {
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(action, "action");
    if (CURRENT.get() != null) throw denied("视频开放接口不允许嵌套授权");
    Authentication authentication = SecurityUtils.getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof Jwt jwt)
        || !Objects.equals(scope.subject(), jwt.getSubject()))
      throw denied("视频开放接口认证身份不匹配");
    String tokenTenant = jwt.getClaimAsString("tenantId");
    if (!blank(tokenTenant) && !Objects.equals(tokenTenant, scope.tenantId()))
      throw denied("视频开放接口租户身份不匹配");
    CURRENT.set(new BoundScope(scope, authentication));
    try {
      return action.get();
    } finally {
      CURRENT.remove();
    }
  }

  /** Returns null when this is an ordinary UI/worker call. Invalid bound identities fail closed. */
  static Scope current() {
    BoundScope bound = CURRENT.get();
    if (bound == null) return null;
    if (SecurityUtils.getAuthentication() != bound.authentication()
        || !bound.authentication().isAuthenticated())
      throw denied("视频开放接口认证上下文发生变化");
    return bound.scope();
  }

  public static boolean isPresent() {
    return current() != null;
  }

  /** Returns true only after checking the exact instance and tenant in an active OpenAPI scope. */
  public static boolean assertInstanceIfPresent(VideoPlatformInstance instance) {
    Scope scope = current();
    if (scope == null) return false;
    if (instance == null || !Objects.equals(scope.instanceKey(), instance.getInstanceKey())
        || !Objects.equals(scope.tenantId(), instance.getTenantId()))
      throw denied("视频实例超出开放接口授权范围");
    return true;
  }

  public static void assertChannelIfPresent(String instanceKey, String deviceId, String channelId) {
    Scope scope = current();
    if (scope != null && (!Objects.equals(scope.instanceKey(), instanceKey)
        || !Objects.equals(scope.deviceId(), deviceId) || !Objects.equals(scope.channelId(), channelId)))
      throw denied("视频通道超出开放接口授权范围");
  }

  public static void assertResourceTenantIfPresent(String tenantId) {
    Scope scope = current();
    if (scope != null && !Objects.equals(scope.tenantId(), tenantId))
      throw denied("视频资源超出开放接口租户范围");
  }

  /** Null means the caller must use the existing UI/menu policy. */
  public static Boolean operationAllowed(String operation) {
    Scope scope = current();
    return scope == null ? null : scope.operations().contains(operation);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static AccessDeniedException denied(String message) {
    return new AccessDeniedException(message);
  }

  private record BoundScope(Scope scope, Authentication authentication) {}
}
