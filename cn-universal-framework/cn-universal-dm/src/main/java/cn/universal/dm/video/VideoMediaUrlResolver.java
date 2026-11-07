package cn.universal.dm.video;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Public playback addresses are independent of internal ZLM API/RTP addresses. */
@Component
public class VideoMediaUrlResolver {
  private static final Set<String> HTTP_KEYS = Set.of("flv", "http_flv", "httpFlv", "https_flv",
      "hls", "https_hls", "fmp4", "https_fmp4", "ts", "https_ts", "rtc", "rtcs", "webrtc");
  private static final Set<String> WS_KEYS = Set.of("ws_flv", "wsFlv", "wss_flv", "wssFlv",
      "ws_hls", "wss_hls", "ws_fmp4", "wss_fmp4", "ws_ts", "wss_ts",
      "originWsFlvUrl", "origin_ws_flv_url");
  private static final Set<String> PLAY_KEYS = Set.of("preferredUrl", "playbackUrl", "playUrl", "url");

  @Value("${nexiot.video.media.public-base-url:}")
  private String publicBaseUrl = "";
  @Value("${nexiot.video.media.public-websocket-base-url:}")
  private String publicWebsocketBaseUrl = "";

  public VideoMediaUrlResolver() {}

  public VideoMediaUrlResolver(String publicBaseUrl, String publicWebsocketBaseUrl) {
    this.publicBaseUrl = publicBaseUrl;
    this.publicWebsocketBaseUrl = publicWebsocketBaseUrl;
    validate();
  }

  @PostConstruct
  public void validate() {
    normalizeBase(publicBaseUrl, false);
    normalizeBase(publicWebsocketBaseUrl, true);
  }

  public String httpBase(String fallback) {
    String configured = normalizeBase(publicBaseUrl, false);
    return configured == null ? fallback : configured;
  }

  public String websocketBase(String httpBase) {
    String configured = normalizeBase(publicWebsocketBaseUrl, true);
    return configured != null ? configured : httpBase == null ? null
        : httpBase.replaceFirst("^https://", "wss://").replaceFirst("^http://", "ws://");
  }

  /** Rewrites known playback URL fields, keeping authentication queries and transport URLs intact. */
  public Map<String, String> publicUrls(Map<String, String> urls) {
    Map<String, String> result = new LinkedHashMap<>(urls == null ? Map.of() : urls);
    String http = normalizeBase(publicBaseUrl, false);
    String ws = normalizeBase(publicWebsocketBaseUrl, true);
    if (http == null && ws == null) return result;
    for (Map.Entry<String, String> entry : result.entrySet()) {
      String value = entry.getValue();
      if (value == null) continue;
      boolean websocket = WS_KEYS.contains(entry.getKey())
          || (PLAY_KEYS.contains(entry.getKey()) && (value.startsWith("ws://") || value.startsWith("wss://")));
      String base = websocket ? websocketBase(http) : HTTP_KEYS.contains(entry.getKey())
          || PLAY_KEYS.contains(entry.getKey()) ? http : null;
      if (base == null) continue;
      try {
        URI source = URI.create(value);
        if (source.getScheme() == null || !Set.of("http", "https", "ws", "wss").contains(source.getScheme())) continue;
        // Already public URLs must not acquire the reverse proxy prefix twice.
        if (value.equals(base) || value.startsWith(base + "/") || value.startsWith(base + "?")) continue;
        String path = source.getRawPath() == null ? "" : source.getRawPath();
        entry.setValue(base + (path.startsWith("/") ? path : "/" + path)
            + (source.getRawQuery() == null ? "" : "?" + source.getRawQuery()));
      } catch (IllegalArgumentException ignored) {
        // An unrelated non-URL response value does not break the existing operation.
      }
    }
    alias(result, "flv", "https_flv", "https://");
    alias(result, "hls", "https_hls", "https://");
    alias(result, "ws_flv", "wss_flv", "wss://");
    return result;
  }

  private static void alias(Map<String, String> urls, String from, String to, String scheme) {
    String value = urls.get(from);
    if (value != null && value.startsWith(scheme)) urls.put(to, value);
  }

  public static String normalizeBase(String value, boolean websocket) {
    if (value == null || value.isBlank()) return null;
    URI uri;
    try { uri = URI.create(value.trim()); }
    catch (IllegalArgumentException ex) { throw new IllegalArgumentException("公共媒体地址不是有效的绝对 URL", ex); }
    Set<String> schemes = websocket ? Set.of("ws", "wss") : Set.of("http", "https");
    if (uri.getScheme() == null || !schemes.contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
        || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getPort() == 0
        || uri.getPort() > 65535 || !uri.normalize().equals(uri)) {
      throw new IllegalArgumentException("公共媒体地址必须使用 " + schemes
          + "，可带路径前缀，不得包含账号、查询参数、片段或父级路径");
    }
    return uri.toString().replaceAll("/+$", "");
  }
}
