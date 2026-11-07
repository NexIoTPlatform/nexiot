package nexiot.web.ide.debug.plugins.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.exception.MagicLoginException;
import org.springframework.stereotype.Component;

/** Opaque, IDE-only sessions. Platform verification remains in the host application. */
@Component
public class MagicSsoSessionService {
  public static final String PREFIX = "ide-sso.";
  private static final int MAX_SESSIONS = 4096;
  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  private final SecureRandom random = new SecureRandom();
  private final Clock clock;

  public MagicSsoSessionService() {
    this(Clock.systemUTC());
  }

  MagicSsoSessionService(Clock clock) {
    this.clock = clock;
  }

  public static boolean supports(String token) {
    return token != null && token.startsWith(PREFIX);
  }

  public synchronized MagicUser issue(
      String unionId,
      String tenantId,
      boolean admin,
      Instant platformExpiresAt,
      Runnable validatePlatformSession)
      throws MagicLoginException {
    Instant now = clock.instant();
    sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    if (sessions.size() >= MAX_SESSIONS) throw new MagicLoginException("调试会话数量过多，请稍后重试");
    if (unionId == null
        || unionId.isBlank()
        || tenantId == null
        || tenantId.isBlank()
        || platformExpiresAt == null
        || !platformExpiresAt.isAfter(now)
        || validatePlatformSession == null) throw new MagicLoginException("平台登录已失效");
    Instant expiresAt = now.plusSeconds(300);
    if (platformExpiresAt.isBefore(expiresAt)) expiresAt = platformExpiresAt;
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Session session = new Session(unionId, tenantId, admin, expiresAt, validatePlatformSession);
    sessions.put(token, session);
    return toUser(token, session);
  }

  public MagicUser getUser(String token) throws MagicLoginException {
    Session session = require(token);
    try {
      session.validatePlatformSession().run();
    } catch (RuntimeException exception) {
      sessions.remove(token);
      throw new MagicLoginException("平台登录已失效或没有调试权限");
    }
    // Validation can take time; never grant a session that expired during it.
    require(token);
    return toUser(token, session);
  }

  public boolean isAdmin(MagicUser user) {
    Session session = sessions.get(user.getToken());
    return session != null && session.expiresAt().isAfter(clock.instant()) && session.admin();
  }

  public void revoke(String token) {
    if (token != null) sessions.remove(token);
  }

  private Session require(String token) throws MagicLoginException {
    Session session = token == null ? null : sessions.get(token);
    if (session == null || !session.expiresAt().isAfter(clock.instant())) {
      revoke(token);
      throw new MagicLoginException("调试登录已过期，请重新登录平台");
    }
    return session;
  }

  private MagicUser toUser(String token, Session session) {
    // unionId, not the display username, is the globally unique resource owner.
    long remaining = Math.max(1, (session.expiresAt().toEpochMilli() - clock.millis()) / 1000);
    return new MagicUser(session.unionId(), session.unionId(), token, remaining);
  }

  public record Session(
      String unionId,
      String tenantId,
      boolean admin,
      Instant expiresAt,
      Runnable validatePlatformSession) {}
}
