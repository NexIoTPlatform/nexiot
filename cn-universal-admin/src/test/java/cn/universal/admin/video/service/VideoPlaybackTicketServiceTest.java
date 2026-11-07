package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoMediaUrlResolver;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class VideoPlaybackTicketServiceTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> values = mock(ValueOperations.class);
  private final Map<String, String> records = new ConcurrentHashMap<>();
  private final VideoPlaybackTicketService service =
      new VideoPlaybackTicketService(redis, new VideoMediaUrlResolver(), "https://iot.example.com/console/");

  @BeforeEach
  void setUp() {
    when(redis.opsForValue()).thenReturn(values);
    when(values.get(anyString())).thenAnswer(call -> records.get(call.getArgument(0)));
    doAnswer(call -> {
      records.put(call.getArgument(0), call.getArgument(1));
      return null;
    }).when(values).set(anyString(), anyString(), any(Duration.class));
    when(redis.delete(anyString())).thenAnswer(call -> records.remove(call.getArgument(0)) != null);
  }

  @Test
  void returnsConfiguredEmbeddableUrlAndDefaultsToFiveMinutes() {
    Map<String, Object> ticket = service.create(Map.of("wss_flv", "wss://media.example/live.flv"), "tenant:app", 0);
    String token = ticket.get("token").toString();
    assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
    assertEquals("https://iot.example.com/console/video-player.html#/video-player/" + token, ticket.get("playerUrl"));
    assertEquals(ticket.get("playerUrl"), ticket.get("embedUrl"));
    verify(values).set(anyString(), anyString(), eq(Duration.ofSeconds(300)));
    assertFalse(records.keySet().iterator().next().contains(token));
    assertEquals(300, ticket.get("ttlSeconds"));
  }

  @Test
  void publicContextOnlyContainsFixedBrowserMediaAndNeverOwnershipOrCredentials() {
    Map<String, Object> stream = new LinkedHashMap<>();
    stream.put("urls", Map.of("ws_flv", "ws://media.example/live.flv", "wss_flv", "wss://media.example/live.flv"));
    stream.put("rtsp", "rtsp://admin:secret@camera.example/stream");
    stream.put("gatewayToken", "management-secret");
    stream.put("token", "vendor-secret");
    stream.put("tenantId", "tenant");
    stream.put("deviceId", "camera");
    stream.put("openApiSessionId", "trusted-session");
    stream.put("playbackId", "internal-playback");
    String token = service.create(stream, "tenant:app", 60).get("token").toString();
    stream.put("wss_flv", "wss://changed.example/live.flv");

    Map<String, Object> context = service.context(token);
    Map<?, ?> browser = (Map<?, ?>) context.get("stream");
    assertEquals("wss://media.example/live.flv", browser.get("preferredUrl"));
    assertEquals("jessibuca", browser.get("playerType"));
    assertEquals(true, context.get("playback"));
    assertEquals("trusted-session", service.getSessionId(token));
    String publicJson = JSONUtil.toJsonStr(context);
    for (String forbidden : new String[]{"management-secret", "vendor-secret", "tenantId", "deviceId", "rtsp", "ownerScope", "trusted-session", "internal-playback"}) {
      assertFalse(publicJson.contains(forbidden), forbidden);
    }
  }

  @Test
  void capsTicketLifetimeAndAllowsOwnerToRevokeWithoutStoppingSharedMedia() {
    String token = service.create(Map.of("hls", "https://media.example/live.m3u8"), "tenant:app", 7200).get("token").toString();
    verify(values).set(anyString(), anyString(), eq(Duration.ofSeconds(3600)));
    assertThrows(BaseException.class, () -> service.revoke(token, "other:app"));
    assertNotNull(service.context(token));
    service.revoke(token, "tenant:app");
    assertThrows(BaseException.class, () -> service.context(token));
    assertDoesNotThrow(() -> service.revoke(token, "tenant:app"));
  }

  @Test
  void rejectsExpiredRecordEvenWhenRedisHasNotEvictedItYet() {
    String token = service.create(Map.of("flv", "https://media.example/live.flv"), "owner", 60).get("token").toString();
    String key = records.keySet().iterator().next();
    Map<String, Object> record = JSONUtil.parseObj(records.get(key));
    record.put("expiresAt", System.currentTimeMillis() - 1);
    records.put(key, JSONUtil.toJsonStr(record));
    assertThrows(BaseException.class, () -> service.context(token));
    assertThrows(BaseException.class, () -> service.getSessionId(token));
  }

  @Test
  void rejectsUnplayableAndCredentialBearingAddressesInsteadOfExposingThem() {
    for (String value : new String[]{"rtsp://camera.example/live", "https://admin:secret@media.example/live.flv", "javascript:alert(1)", "//media.example/live.flv"}) {
      assertThrows(BaseException.class, () -> service.create(Map.of("preferredUrl", value), "owner", 60));
    }
    assertThrows(BaseException.class, () -> service.create(Map.of("url", "https://media.example/live.m3u8"), "", 60));
    verify(values, never()).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  void invalidTokenNeverReadsRedisAndBaseUrlCannotContainSecretsOrQuery() {
    assertThrows(BaseException.class, () -> service.context("invalid"));
    verify(values, never()).get(anyString());
    for (String base : new String[]{"https://admin:secret@iot.example", "https://iot.example/?token=secret", "https://iot.example/#/old", "//iot.example"}) {
      assertThrows(IllegalArgumentException.class, () -> new VideoPlaybackTicketService(redis, new VideoMediaUrlResolver(), base));
    }
  }

  @Test
  void ticketMediaUsesPublicReverseProxyAddressBeforeItIsSaved() {
    var publicService = new VideoPlaybackTicketService(redis,
        new VideoMediaUrlResolver("https://media.example/proxy", ""), "https://iot.example");
    String token = publicService.create(Map.of("ws_flv", "ws://192.0.2.2:8080/rtp/live.flv?sign=playback"), "owner", 60).get("token").toString();
    Map<?, ?> stream = (Map<?, ?>) publicService.context(token).get("stream");
    assertEquals("wss://media.example/proxy/rtp/live.flv?sign=playback", stream.get("preferredUrl"));
    assertFalse(JSONUtil.toJsonStr(publicService.context(token)).contains("192.0.2.2"));
  }
}
