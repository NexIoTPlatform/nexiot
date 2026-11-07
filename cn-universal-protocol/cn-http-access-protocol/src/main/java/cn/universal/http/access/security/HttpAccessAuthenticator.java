package cn.universal.http.access.security;

import cn.universal.http.access.config.HttpIntegrationConfig.AuthType;
import cn.universal.http.access.config.HttpIntegrationConfig.HttpInboundAuth;
import cn.universal.http.access.runtime.HttpRouteSnapshot;
import cn.universal.http.access.web.HttpAccessException;
import cn.universal.http.access.web.HttpIngressRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class HttpAccessAuthenticator {

  private final StringRedisTemplate redisTemplate;

  public HttpAccessAuthenticator(ObjectProvider<StringRedisTemplate> redisTemplate) {
    this.redisTemplate = redisTemplate.getIfAvailable();
  }

  public void authenticate(HttpRouteSnapshot snapshot, HttpIngressRequest request) {
    HttpInboundAuth auth = snapshot.config().getInbound().getAuth();
    if (AuthType.NONE.equals(auth.getType())) {
      return;
    }
    if (AuthType.API_KEY.equals(auth.getType())) {
      requireEquals(auth.getValue(), request.firstHeader(auth.getHeaderName()));
      return;
    }
    if (AuthType.HMAC_SHA256.equals(auth.getType())) {
      authenticateHmac(snapshot, auth, request);
      return;
    }
    throw unauthorized("UNSUPPORTED_AUTH_TYPE");
  }

  private void authenticateHmac(
      HttpRouteSnapshot snapshot, HttpInboundAuth auth, HttpIngressRequest request) {
    String timestampValue = request.firstHeader(auth.getTimestampHeader());
    String nonce = request.firstHeader(auth.getNonceHeader());
    String signature = request.firstHeader(auth.getSignatureHeader());
    if (blank(timestampValue) || blank(nonce) || blank(signature) || nonce.length() > 128) {
      throw unauthorized("INVALID_HMAC_HEADERS");
    }
    long rawTimestamp;
    try {
      rawTimestamp = Long.parseLong(timestampValue);
    } catch (NumberFormatException e) {
      throw unauthorized("INVALID_HMAC_TIMESTAMP");
    }
    long timestampSeconds = rawTimestamp > 10_000_000_000L ? rawTimestamp / 1000 : rawTimestamp;
    if (Math.abs(Instant.now().getEpochSecond() - timestampSeconds) > auth.getClockSkewSeconds()) {
      throw unauthorized("HMAC_TIMESTAMP_EXPIRED");
    }
    String canonical = request.method().toUpperCase() + '\n' + request.routePath() + '\n'
        + canonicalQuery(request.query()) + '\n' + sha256(request.body()) + '\n'
        + timestampValue + '\n' + nonce;
    requireEquals(hmac(auth.getSecret(), canonical), signature.toLowerCase());
    claimNonce(snapshot, nonce, auth.getClockSkewSeconds());
  }

  private void claimNonce(HttpRouteSnapshot snapshot, String nonce, int skewSeconds) {
    if (redisTemplate == null) {
      throw new HttpAccessException(503, "REPLAY_GUARD_UNAVAILABLE", "Authentication service unavailable");
    }
    try {
      Boolean accepted = redisTemplate.opsForValue().setIfAbsent(
          "http:nonce:" + snapshot.definition().id() + ':' + sha256(nonce), "1",
          Duration.ofSeconds(skewSeconds * 2L));
      if (!Boolean.TRUE.equals(accepted)) {
        throw unauthorized("HMAC_REPLAY_DETECTED");
      }
    } catch (HttpAccessException e) {
      throw e;
    } catch (Exception e) {
      throw new HttpAccessException(503, "REPLAY_GUARD_UNAVAILABLE", "Authentication service unavailable");
    }
  }

  private String canonicalQuery(Map<String, List<String>> query) {
    if (query == null || query.isEmpty()) return "";
    List<String> pairs = new ArrayList<>();
    query.forEach((key, values) -> (values == null || values.isEmpty() ? List.of("") : values)
        .forEach(value -> pairs.add(encode(key) + '=' + encode(value))));
    pairs.sort(Comparator.naturalOrder());
    return String.join("&", pairs);
  }

  private String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private String hmac(String secret, String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to calculate HMAC", e);
    }
  }

  private String sha256(String value) { return sha256(value.getBytes(StandardCharsets.UTF_8)); }
  private String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value == null ? new byte[0] : value));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to calculate SHA-256", e);
    }
  }

  private void requireEquals(String expected, String actual) {
    byte[] left = expected == null ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
    byte[] right = actual == null ? new byte[0] : actual.getBytes(StandardCharsets.UTF_8);
    if (expected == null || actual == null || !MessageDigest.isEqual(left, right)) {
      throw unauthorized("AUTHENTICATION_FAILED");
    }
  }

  private HttpAccessException unauthorized(String code) {
    return new HttpAccessException(401, code, "Authentication failed");
  }
  private boolean blank(String value) { return value == null || value.isBlank(); }
}
