package nexiot.web.ide.debug.plugins.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.concurrent.atomic.AtomicBoolean;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.exception.MagicLoginException;
import org.junit.jupiter.api.Test;

class MagicSsoSessionServiceTest {
  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");
  private final MutableClock clock = new MutableClock();
  private final MagicSsoSessionService sessions = new MagicSsoSessionService(clock);

  private MagicUser issue(Instant expiry, Runnable validator) throws Exception {
    return sessions.issue("user-a", "tenant-a", false, expiry, validator);
  }

  @Test
  void randomOpaqueTokensAndUniqueOwners() throws Exception {
    MagicUser a = issue(NOW.plusSeconds(3600), () -> {});
    MagicUser b = issue(NOW.plusSeconds(3600), () -> {});
    assertNotEquals(a.getToken(), b.getToken());
    assertTrue(a.getToken().startsWith("ide-sso."));
    assertFalse(a.getToken().contains("user-a"));
    assertEquals("user-a", sessions.getUser(a.getToken()).getUsername());
    assertEquals(300, a.getTimeout());
  }

  @Test
  void capsAtPlatformExpiryAndNeverSlides() throws Exception {
    MagicUser a = issue(NOW.plusSeconds(30), () -> {});
    assertEquals(30, a.getTimeout());
    clock.now = NOW.plusSeconds(25);
    assertEquals(5, sessions.getUser(a.getToken()).getTimeout());
    clock.now = NOW.plusSeconds(30);
    assertThrows(MagicLoginException.class, () -> sessions.getUser(a.getToken()));
  }

  @Test
  void rejectsExpiredMissingAndForgedCredentials() throws Exception {
    assertThrows(MagicLoginException.class, () -> issue(NOW, () -> {}));
    assertThrows(MagicLoginException.class, () -> sessions.getUser(null));
    assertThrows(MagicLoginException.class, () -> sessions.getUser("ide-sso.forged"));
  }

  @Test
  void logoutRevokesImmediately() throws Exception {
    MagicUser a = issue(NOW.plusSeconds(3600), () -> {});
    sessions.revoke(a.getToken());
    assertThrows(MagicLoginException.class, () -> sessions.getUser(a.getToken()));
  }

  @Test
  void rechecksPlatformStateAndPermanentlyRevokesFailures() throws Exception {
    AtomicBoolean revoked = new AtomicBoolean();
    MagicUser a =
        issue(
            NOW.plusSeconds(3600),
            () -> {
              if (revoked.get()) throw new IllegalStateException("permission removed");
            });
    sessions.getUser(a.getToken());
    revoked.set(true);
    assertThrows(MagicLoginException.class, () -> sessions.getUser(a.getToken()));
    revoked.set(false);
    assertThrows(MagicLoginException.class, () -> sessions.getUser(a.getToken()));
  }

  @Test
  void cannotOutliveExpiryDuringValidation() throws Exception {
    MagicUser a =
        issue(
            NOW.plusSeconds(30),
            () -> {
              clock.now = NOW.plusSeconds(31);
            });
    assertThrows(MagicLoginException.class, () -> sessions.getUser(a.getToken()));
  }

  private static class MutableClock extends Clock {
    Instant now = NOW;

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId zone) {
      return this;
    }

    public Instant instant() {
      return now;
    }
  }
}
