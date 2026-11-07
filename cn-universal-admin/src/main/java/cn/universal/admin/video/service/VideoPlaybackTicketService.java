package cn.universal.admin.video.service;

import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoMediaUrlResolver;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Short lived capability for an already authorized stream; public reads never start a new stream. */
@Service
public class VideoPlaybackTicketService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String KEY_PREFIX = "video:openapi:player:";
  private static final String INVALID_MESSAGE = "播放链接无效、已过期或已撤销";
  private static final int DEFAULT_TTL = 300;
  private static final int MAX_TTL = 3600;
  private static final List<String> URL_FIELDS =
      List.of("preferredUrl", "wss_flv", "https_flv", "https_hls", "ws_flv", "wsFlv",
          "flv", "http_flv", "httpFlv", "hls", "playbackUrl", "playUrl", "url");
  private static final Set<String> BROWSER_SCHEMES = Set.of("http", "https", "ws", "wss");

  private final StringRedisTemplate redis;
  private final VideoMediaUrlResolver mediaUrls;
  private final String playerBaseUrl;

  public VideoPlaybackTicketService(
      StringRedisTemplate redis,
      VideoMediaUrlResolver mediaUrls,
      @Value("${nexiot.video.openapi.player-base-url:}") String playerBaseUrl) {
    this.redis = redis;
    this.mediaUrls = mediaUrls;
    this.playerBaseUrl = normalizePlayerBaseUrl(playerBaseUrl);
  }

  /** Call only after the business API has authorized the channel and acquired its stream. */
  public Map<String, Object> create(
      Map<String, Object> stream, String ownerScope, int ttlSeconds) {
    if (ownerScope == null || ownerScope.isBlank()) {
      throw new BaseException("播放链接缺少授权范围");
    }
    Map<String, Object> browserStream = browserStream(stream);
    if (browserStream.isEmpty()) {
      throw new BaseException("当前视频流未提供浏览器可播放的地址");
    }
    int ttl = ttlSeconds <= 0 ? DEFAULT_TTL : Math.min(ttlSeconds, MAX_TTL);
    long expiresAt = System.currentTimeMillis() + ttl * 1000L;
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    Map<String, Object> record = new LinkedHashMap<>();
    record.put("ownerScope", ownerScope);
    record.put("stream", browserStream);
    Object sessionId = stream.get("openApiSessionId");
    if (sessionId instanceof String id && !id.isBlank()) record.put("openApiSessionId", id);
    record.put("playback", Boolean.TRUE.equals(stream.get("playback")) || stream.containsKey("playbackId"));
    record.put("expiresAt", expiresAt);
    redis.opsForValue().set(redisKey(token), JSONUtil.toJsonStr(record), Duration.ofSeconds(ttl));

    String url = playerBaseUrl + "/video-player.html#/video-player/" + token;
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("token", token);
    result.put("playerUrl", url);
    result.put("embedUrl", url);
    result.put("expiresAt", expiresAt);
    result.put("ttlSeconds", ttl);
    return result;
  }

  public Map<String, Object> context(String token) {
    Map<String, Object> record = requireRecord(token);
    Map<String, Object> result = new LinkedHashMap<>();
    // Project again so changes to the internal record never expose ownership or management data.
    result.put("stream", browserStream(asMap(record.get("stream"))));
    result.put("playback", Boolean.TRUE.equals(record.get("playback")));
    result.put("expiresAt", record.get("expiresAt"));
    result.put("serverTime", System.currentTimeMillis());
    return result;
  }

  /** Internal session identity for the trusted lifecycle service; never included in public context. */
  public String getSessionId(String token) {
    Object value = requireRecord(token).get("openApiSessionId");
    return value instanceof String ? (String) value : null;
  }

  /** Idempotent for an expired ticket, but an existing ticket must belong to this caller. */
  public void revoke(String token, String ownerScope) {
    if (ownerScope == null || ownerScope.isBlank()) {
      throw new BaseException("播放链接缺少授权范围");
    }
    validateToken(token);
    String recordJson = redis.opsForValue().get(redisKey(token));
    if (recordJson == null) return;
    Map<String, Object> record = readRecord(recordJson);
    if (!ownerScope.equals(record.get("ownerScope"))) {
      throw new BaseException("播放链接不属于当前调用方");
    }
    redis.delete(redisKey(token));
  }

  private Map<String, Object> requireRecord(String token) {
    validateToken(token);
    String json = redis.opsForValue().get(redisKey(token));
    if (json == null) throw new BaseException(INVALID_MESSAGE);
    Map<String, Object> record = readRecord(json);
    Object expiry = record.get("expiresAt");
    if (!(expiry instanceof Number) || ((Number) expiry).longValue() <= System.currentTimeMillis()) {
      throw new BaseException(INVALID_MESSAGE);
    }
    return record;
  }

  private Map<String, Object> readRecord(String json) {
    try {
      return JSONUtil.parseObj(json);
    } catch (RuntimeException e) {
      throw new BaseException(INVALID_MESSAGE);
    }
  }

  private void validateToken(String token) {
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
      throw new BaseException(INVALID_MESSAGE);
    }
  }

  private String redisKey(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
      return KEY_PREFIX + HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }

  private Map<String, Object> browserStream(Map<String, Object> source) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (source == null) return result;
    Map<String, String> candidateUrls = new LinkedHashMap<>();
    for (String field : URL_FIELDS) {
      Object value = source.get(field);
      for (String nested : List.of("urls", "streamUrls", "stream")) {
        if (value == null) value = asMap(source.get(nested)).get(field);
      }
      if (value instanceof String url && isBrowserUrl(url)) candidateUrls.put(field, url);
    }
    mediaUrls.publicUrls(candidateUrls).forEach((field, url) -> {
      if (URL_FIELDS.contains(field) && isBrowserUrl(url)) result.put(field, url);
    });
    if (!result.isEmpty()) {
      // Prefer secure media even when the API consumer itself is using plain HTTP.
      Object preferred = null;
      for (String field : List.of("wss_flv", "https_flv", "https_hls", "preferredUrl",
          "ws_flv", "wsFlv", "flv", "http_flv", "httpFlv", "hls", "playbackUrl", "playUrl", "url")) {
        if (result.containsKey(field)) {
          preferred = result.get(field);
          break;
        }
      }
      result.put("preferredUrl", preferred);
      String url = String.valueOf(preferred).toLowerCase(java.util.Locale.ROOT);
      result.put("playerType", url.startsWith("ws") || url.matches(".*\\.flv(?:[?#].*)?$")
          ? "jessibuca" : "standard");
    }
    return result;
  }

  private boolean isBrowserUrl(String url) {
    try {
      URI uri = URI.create(url);
      return uri.getScheme() != null
          && BROWSER_SCHEMES.contains(uri.getScheme().toLowerCase(java.util.Locale.ROOT))
          && uri.getHost() != null
          && uri.getRawUserInfo() == null;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object value) {
    return value instanceof Map ? (Map<String, Object>) value : Map.of();
  }

  private String normalizePlayerBaseUrl(String value) {
    String base = value == null ? "" : value.trim().replaceAll("/+$", "");
    if (base.isEmpty()) return "";
    try {
      URI uri = URI.create(base);
      if (uri.getScheme() == null || !Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
          || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
        throw new IllegalArgumentException();
      }
      return base;
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("nexiot.video.openapi.player-base-url 必须是前端站点的 HTTP(S) 基础地址", e);
    }
  }
}
