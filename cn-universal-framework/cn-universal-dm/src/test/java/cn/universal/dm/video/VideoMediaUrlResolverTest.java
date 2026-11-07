package cn.universal.dm.video;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;

class VideoMediaUrlResolverTest {
  @Test void emptyConfigurationKeepsLegacyAddresses() {
    var resolver = new VideoMediaUrlResolver();
    var urls = Map.of("flv", "http://192.0.2.1:80/rtp/stream.live.flv", "rtsp", "rtsp://camera/live");
    assertEquals(urls, resolver.publicUrls(urls));
    assertEquals("http://node:8080", resolver.httpBase("http://node:8080"));
    assertEquals("ws://node:8080", resolver.websocketBase("http://node:8080"));
  }

  @Test void publicProxyPreservesAuthQueryAndDoesNotRewriteInternalTransport() {
    var resolver = new VideoMediaUrlResolver("https://video.example.com/zlm/", "");
    var urls = Map.of("flv", "http://192.0.2.1:8080/rtp/live.live.flv?callId=a%2Bb&token=xyz",
        "hls", "http://192.0.2.1:8080/rtp/live/hls.m3u8", "ws_flv", "ws://192.0.2.1:8080/rtp/live.live.flv",
        "rtsp", "rtsp://192.0.2.1:554/rtp/live", "webrtc", "http://192.0.2.1/index/api/webrtc?app=rtp&stream=live&type=play");
    var result = resolver.publicUrls(urls);
    assertEquals("https://video.example.com/zlm/rtp/live.live.flv?callId=a%2Bb&token=xyz", result.get("https_flv"));
    assertEquals("wss://video.example.com/zlm/rtp/live.live.flv", result.get("wss_flv"));
    assertEquals(urls.get("rtsp"), result.get("rtsp"));
    assertEquals("https://video.example.com/zlm/index/api/webrtc?app=rtp&stream=live&type=play", result.get("webrtc"));
    assertEquals(result, resolver.publicUrls(result), "Repeated normalization must preserve the proxy prefix once");
    var chosen = resolver.publicUrls(Map.of("preferredUrl", "ws://internal:8080/rtp/one.live.flv", "playbackUrl", "http://internal/rtp/one/hls.m3u8"));
    assertEquals("wss://video.example.com/zlm/rtp/one.live.flv", chosen.get("preferredUrl"));
    assertEquals("https://video.example.com/zlm/rtp/one/hls.m3u8", chosen.get("playbackUrl"));
  }

  @Test void independentWebsocketProxyAndInvalidConfiguration() {
    var resolver = new VideoMediaUrlResolver("https://video.example.com/zlm", "wss://ws.example.com:8443/media/");
    assertEquals("wss://ws.example.com:8443/media", resolver.websocketBase("https://video.example.com/zlm"));
    for (String value : List.of("/relative", "https://user:pass@example.com", "https://example.com?token=secret",
        "https://example.com/#fragment", "ftp://example.com", "https://example.com/a/../zlm", "https://example.com:99999"))
      assertThrows(IllegalArgumentException.class, () -> new VideoMediaUrlResolver(value, ""), value);
    assertThrows(IllegalArgumentException.class, () -> new VideoMediaUrlResolver("", "https://example.com"));
  }
}
