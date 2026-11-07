package cn.universal.http.access.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.core.network.NetworkRuntimeDefinition;
import cn.universal.http.access.config.HttpIntegrationConfig;
import cn.universal.http.access.config.HttpIntegrationConfig.AuthType;
import cn.universal.http.access.config.HttpIntegrationConfig.HttpInboundAuth;
import cn.universal.http.access.runtime.HttpRouteSnapshot;
import cn.universal.http.access.web.HttpAccessException;
import cn.universal.http.access.web.HttpIngressRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class HttpAccessAuthenticatorTest {

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private HttpAccessAuthenticator authenticator;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(redisTemplate);
    authenticator = new HttpAccessAuthenticator(provider);
  }

  @Test
  void invalidSignatureCannotConsumeNonce() {
    Fixture fixture = fixture("bad-signature");

    HttpAccessException error =
        assertThrows(
            HttpAccessException.class,
            () ->
                authenticator.authenticate(
                    fixture.snapshot(), fixture.request()));

    assertEquals("AUTHENTICATION_FAILED", error.getCode());
    verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
  }

  @Test
  void validSignatureClaimsNonceAndReplayIsRejected() {
    Fixture fixture = fixture(null);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(true, false);

    assertDoesNotThrow(
        () ->
            authenticator.authenticate(
                fixture.snapshot(), fixture.request()));
    HttpAccessException replay =
        assertThrows(
            HttpAccessException.class,
            () ->
                authenticator.authenticate(
                    fixture.snapshot(), fixture.request()));

    assertEquals("HMAC_REPLAY_DETECTED", replay.getCode());
  }

  private Fixture fixture(String forcedSignature) {
    String timestamp = Long.toString(Instant.now().getEpochSecond());
    String nonce = "nonce-123";
    byte[] body = "{\"temperature\":21}".getBytes(StandardCharsets.UTF_8);
    Map<String, List<String>> query = new LinkedHashMap<>();
    query.put("z", List.of("last"));
    query.put("a", List.of("first"));
    String canonical =
        "POST\n/devices/D1/report\na=first&z=last\n"
            + sha256(body)
            + '\n'
            + timestamp
            + '\n'
            + nonce;
    String signature = forcedSignature == null ? hmac("hmac-secret", canonical) : forcedSignature;

    HttpInboundAuth profile = new HttpInboundAuth();
    profile.setType(AuthType.HMAC_SHA256);
    profile.setSecret("hmac-secret");
    HttpIntegrationConfig config = new HttpIntegrationConfig();
    config.setAccessCode("factory-east");
    config.getInbound().setAuth(profile);
    NetworkRuntimeDefinition definition =
        new NetworkRuntimeDefinition(99L, "HTTP_SERVER", null, "union", "test", "{}", 1);
    HttpRouteSnapshot snapshot = new HttpRouteSnapshot(definition, config, null);
    HttpIngressRequest request =
        new HttpIngressRequest(
            "POST",
            "/iot/http/v2/factory-east/devices/D1/report",
            "/devices/D1/report",
            "127.0.0.1",
            Map.of(
                profile.getTimestampHeader(), List.of(timestamp),
                profile.getNonceHeader(), List.of(nonce),
                profile.getSignatureHeader(), List.of(signature)),
            query,
            body);
    return new Fixture(snapshot, request);
  }

  private String hmac(String secret, String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private record Fixture(
      HttpRouteSnapshot snapshot,
      HttpIngressRequest request) {}
}
