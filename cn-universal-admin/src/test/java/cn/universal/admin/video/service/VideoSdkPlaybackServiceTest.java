package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.zlm.VideoZlmStreamProxyService;
import cn.universal.persistence.entity.VideoPlatformInstance;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VideoSdkPlaybackServiceTest {
  private static final long FROM = 1791338400000L, TO = FROM + 30000;
  private static final Set<String> RTP_KEYS = Set.of("zlmHost", "zlmRtpPort", "ssrc",
      "psPayloadType", "h264PayloadType", "h265PayloadType");
  private final VideoPlatformAdapterRegistry adapters = mock(VideoPlatformAdapterRegistry.class);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final VideoZlmStreamProxyService zlm = mock(VideoZlmStreamProxyService.class);
  @SuppressWarnings("unchecked")
  private final ObjectProvider<VideoZlmStreamProxyService> media = mock(ObjectProvider.class);
  private final VideoSdkPlaybackService service = new VideoSdkPlaybackService(adapters, media);
  private final VideoPlatformInstance instance = instance("sdk", "tenant", "dahua_sdk");

  private static VideoPlatformInstance instance(String key, String tenant, String type) {
    return VideoPlatformInstance.builder().instanceKey(key).tenantId(tenant).platformType(type).enabled(1).build();
  }

  private static Map<String, Object> input() {
    return Map.of("deviceId", "camera", "channelId", "1", "startTime", FROM, "endTime", TO);
  }

  private static Map<String, Object> urls(String stream) {
    var result = new LinkedHashMap<String, Object>();
    result.put("ws_flv", "ws://media.example/rtp/" + stream + ".live.flv");
    result.put("hls", "http://media.example/rtp/" + stream + "/hls.m3u8");
    result.put("mediaServerPrimaryId", "2"); result.put("zlmApp", "rtp"); result.put("zlmStream", stream);
    result.put("zlmHost", "192.0.2.2"); result.put("zlmRtpPort", 30000); result.put("ssrc", "4294967290");
    result.put("psPayloadType", 96); result.put("h264PayloadType", 98); result.put("h265PayloadType", 99);
    return result;
  }

  @BeforeEach void setup() {
    when(media.getIfAvailable()).thenReturn(zlm);
    when(adapters.getAdapter(anyString())).thenReturn(adapter);
    when(zlm.openRtpPlayback(eq("tenant"), any(), anyString())).thenAnswer(call -> urls(call.getArgument(2)));
    when(adapter.executeOperation(any(), eq("playbackStreamStart"), any())).thenReturn(Map.of("status", "starting"));
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "streaming"));
    when(adapter.executeOperation(any(), eq("playbackStreamPause"), any())).thenAnswer(call -> {
      Map<String, Object> request = call.getArgument(2);
      return Map.of("status", Boolean.TRUE.equals(request.get("paused")) ? "paused" : "streaming");
    });
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any())).thenReturn(Map.of("status", "stopped"));
  }

  @AfterEach void close() { service.shutdown(); }
  @Test void isupAllControlsUseOwnedNativeStreamInsteadOfUiPlaybackIdentifier() {
    var isup=instance("isup","tenant","hik_isup");
    when(adapter.executeOperation(any(),anyString(),anyMap())).thenAnswer(call->{
      Map<String,Object> request=call.getArgument(2);
      assertEquals(request.get("zlmStream"),request.get("streamKey"));
      assertNotEquals(request.get("playbackId"),request.get("streamKey"));
      return Map.of("status","starting");
    });
    var first=service.start(isup,input(),"tenant","node-2");var id=first.get("playbackId");
    service.control(isup,"playbackStreamStatus",Map.of("playbackId",id,"streamKey","foreign-stream"));
    service.control(isup,"playbackStreamPause",Map.of("playbackId",id,"paused",true));
    var next=service.control(isup,"playbackStreamSeek",Map.of("playbackId",id,"seekTime",FROM+5000));
    service.control(isup,"playbackStreamStop",Map.of("playbackId",next.get("playbackId")));
  }
  @Test void seekCanReturnBeforePreviousSeekWhileKeepingOriginalInterval() {
    var started=service.start(instance,input(),"tenant",null);
    var later=service.control(instance,"playbackStreamSeek",Map.of("playbackId",started.get("playbackId"),"seekTime",FROM+20000));
    var earlier=service.control(instance,"playbackStreamSeek",Map.of("playbackId",later.get("playbackId"),"seekTime",FROM+5000));
    assertEquals(FROM+5000,earlier.get("startTime"));assertEquals(FROM,earlier.get("originalStartTime"));assertEquals(TO,earlier.get("originalEndTime"));
  }

  @Test void isupNormalizesConnectingAndPauseStateAndKeepsAuthorizedNodeUrls() {
    var isup = instance("isup", "tenant", "hik_isup");
    when(adapter.executeOperation(any(), eq("playbackStreamStart"), any()))
        .thenReturn(Map.of("status", "CONNECTING", "ws_flv", "ws://untrusted-node/live.flv"));
    var started = service.start(isup, input(), "tenant", "node-2");
    String id = started.get("playbackId").toString();
    assertEquals("starting", started.get("status"));
    assertEquals("ws://media.example/rtp/sdk-playback-" + id + ".live.flv", started.get("ws_flv"));
    when(adapter.executeOperation(any(), eq("playbackStreamPause"), any())).thenReturn(Map.of("paused", true));
    assertEquals("paused", service.control(isup, "playbackStreamPause", Map.of("playbackId", id, "paused", true)).get("status"));
    when(adapter.executeOperation(any(), eq("playbackStreamPause"), any())).thenReturn(Map.of("paused", false));
    when(zlm.isRtpPlaybackReady(eq("tenant"), eq("2"), anyString())).thenReturn(true);
    assertEquals("streaming", service.control(isup, "playbackStreamPause", Map.of("playbackId", id, "paused", false)).get("status"));
  }

  @Test void isupNativeBytesAndGatewayReadyClaimsCannotReplaceSelectedZlmVideoReadiness() {
    var isup = instance("isup", "tenant", "hik_isup");
    when(adapter.executeOperation(any(), eq("playbackStreamStart"), any()))
        .thenReturn(Map.of("status", "CONNECTING"));
    var started = service.start(isup, input(), "tenant", "node-2");
    String id = started.get("playbackId").toString();
    assertEquals("starting", started.get("status"));
    assertEquals(false, started.get("mediaReady"));
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any()))
        .thenReturn(Map.of("status", "streaming", "receivedBytes", 150_000,
            "nativeDataReceived", true, "mediaReady", true, "mediaReadinessOwner", "gateway"));
    var pending = service.control(isup, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("starting", pending.get("status"));
    assertEquals(false, pending.get("mediaReady"));
    assertEquals(true, pending.get("nativeDataReceived"));
    assertEquals(150_000, pending.get("receivedBytes"));
    assertEquals("platform", pending.get("mediaReadinessOwner"));

    when(zlm.isRtpPlaybackReady("tenant", "2", "sdk-playback-" + id)).thenReturn(true);
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any()))
        .thenReturn(Map.of("status", "connecting", "mediaReady", false, "mediaReadinessOwner", "platform"));
    var ready = service.control(isup, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("streaming", ready.get("status"));
    assertEquals(true, ready.get("mediaReady"));
    assertEquals("ws://media.example/rtp/sdk-playback-" + id + ".live.flv", ready.get("ws_flv"));
    verify(zlm, times(3)).isRtpPlaybackReady("tenant", "2", "sdk-playback-" + id);
  }

  @Test void isupNativeTerminalStateStaysTerminalEvenWhenZlmVideoWasReady() {
    var isup = instance("isup", "tenant", "hik_isup");
    when(zlm.isRtpPlaybackReady(eq("tenant"), eq("2"), anyString())).thenReturn(true);
    String id = service.start(isup, input(), "tenant", "node-2").get("playbackId").toString();
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any()))
        .thenReturn(Map.of("status", "failed", "message", "native link failed"));
    var failed = service.control(isup, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("failed", failed.get("status"));
    assertEquals(false, failed.get("mediaReady"));
    verify(zlm, times(1)).isRtpPlaybackReady(anyString(), anyString(), anyString());
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void isupNativeCompletionWaitsForReadyVideoBeforeDraining() {
    var isup = instance("isup", "tenant", "hik_isup");
    String id = service.start(isup, input(), "tenant", "node-2").get("playbackId").toString();
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    var waiting = service.control(isup, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("starting", waiting.get("status"));
    assertEquals(false, waiting.get("mediaReady"));
    assertEquals(true, waiting.get("nativeEnded"));
    verify(zlm, never()).closeRtpPlayback(anyString(), anyString(), anyString());
    when(zlm.isRtpPlaybackReady("tenant", "2", "sdk-playback-" + id)).thenReturn(true);
    var draining = service.control(isup, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("draining", draining.get("status"));
    assertEquals(true, draining.get("mediaReady"));
    assertEquals("ended", service.control(isup, "playbackStreamStop", Map.of("playbackId", id, "completed", true)).get("status"));
  }

  @Test void isupNativeCompletionWithoutVideoFailsAfterBoundedReadinessWait() {
    var isup = instance("isup", "tenant", "hik_isup");
    String id = service.start(isup, input(), "tenant", "node-2").get("playbackId").toString();
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    service.control(isup, "playbackStreamStatus", Map.of("playbackId", id));
    Map<?, ?> sessions = (Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(service, "sessions");
    org.springframework.test.util.ReflectionTestUtils.setField(sessions.get(id), "mediaEndedAt", System.currentTimeMillis() - 15001);
    var failed = service.control(isup, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("failed", failed.get("status"));
    assertEquals(false, failed.get("mediaReady"));
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void isupUnsupportedRateFailsBeforeAllocatingMedia() {
    var request = new LinkedHashMap<>(input()); request.put("playbackRate", 2);
    assertThrows(BaseException.class, () -> service.start(instance("isup", "tenant", "hik_isup"), request, "tenant", null));
    verifyNoInteractions(zlm);
  }

  @Test void speedForwardsOnlyRateWithOwnedDeviceAndClosesFailedNativeState() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    assertThrows(BaseException.class,()->service.control(instance,"playbackStreamSpeed",Map.of("playbackId",id,"deviceId","foreign","playbackRate",2)));
    assertThrows(BaseException.class,()->service.control(instance,"playbackStreamStop",Map.of("playbackId",id,"channelId","7")));
    when(adapter.executeOperation(any(), eq("playbackStreamSpeed"), any()))
        .thenReturn(Map.of("status", "streaming", "playbackRate", 2));
    assertEquals(2, service.control(instance, "playbackStreamSpeed", Map.of("playbackId", id,
        "playbackRate", 2, "deviceId", "camera", "zlmHost", "foreign")).get("playbackRate"));
    verify(adapter).executeOperation(eq(instance), eq("playbackStreamSpeed"), argThat(request ->
        "camera".equals(request.get("deviceId")) && "192.0.2.2".equals(request.get("zlmHost"))
            && Double.valueOf(2).equals(request.get("playbackRate"))));
    assertThrows(BaseException.class, () -> service.control(instance, "playbackStreamSpeed", Map.of("playbackId", id, "playbackRate", 3)));
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    assertThrows(BaseException.class, () -> service.control(instance, "playbackStreamSpeed", Map.of("playbackId", id, "playbackRate", 4)));
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", false));
    when(adapter.executeOperation(any(), eq("playbackStreamSpeed"), any()))
        .thenReturn(Map.of("status", "failed", "message", "restart required"));
    assertEquals("failed", service.control(instance, "playbackStreamSpeed", Map.of("playbackId", id, "playbackRate", 4)).get("status"));
    verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void injectsAllocatedRtpTargetAndReturnsOnlyPublicPlaybackFields() {
    var request = new LinkedHashMap<>(input());
    request.put("zlmHost", "attacker.example"); request.put("ssrc", "1"); request.put("zlmRtpPort", 1);
    var result = service.start(instance, request, "tenant", "node-2");
    String id = (String) result.get("playbackId");
    assertTrue(id.matches("[a-f0-9-]{36}"));
    assertEquals("starting", result.get("status"));
    assertEquals(true, result.get("audioSupported"));
    assertTrue(result.containsKey("ws_flv")); assertTrue(result.containsKey("hls"));
    assertTrue(RTP_KEYS.stream().noneMatch(result::containsKey));
    verify(zlm).openRtpPlayback("tenant", "node-2", "sdk-playback-" + id);
    verify(adapter).executeOperation(eq(instance), eq("playbackStreamStart"), argThat(nativeRequest ->
        id.equals(nativeRequest.get("playbackId")) && "camera".equals(nativeRequest.get("deviceId"))
            && "1".equals(nativeRequest.get("channelId")) && FROM == (long) nativeRequest.get("startTime")
            && "192.0.2.2".equals(nativeRequest.get("zlmHost")) && (int) nativeRequest.get("zlmRtpPort") == 30000
            && "4294967290".equals(nativeRequest.get("ssrc")) && (int) nativeRequest.get("h264PayloadType") == 98));
    assertEquals("attacker.example", request.get("zlmHost"));
    assertFalse(request.containsKey("playbackId"));
  }

  @Test void adapterStateCannotReintroduceInternalRtpParametersInAnyResponse() {
    var state = urls("echoed"); state.put("status", "streaming");
    when(adapter.executeOperation(any(), eq("playbackStreamStart"), any())).thenReturn(state);
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(state);
    var started = service.start(instance, input(), "tenant", null);
    assertTrue(RTP_KEYS.stream().noneMatch(started::containsKey));
    var status = service.control(instance, "playbackStreamStatus", Map.of("playbackId", started.get("playbackId")));
    assertTrue(RTP_KEYS.stream().noneMatch(status::containsKey));
  }

  @Test void deviceAndChannelOverridesCannotReplaceValidatedPlaybackTargets() {
    var request = new LinkedHashMap<>(input());
    request.put("device", Map.of("host", "foreign.example", "username", "foreign", "password", "foreign"));
    request.put("channel", Map.of("channelNo", 99, "channelId", "foreign"));
    for (String key : Set.of("host", "username", "password", "gatewayUrl", "deviceHost", "channelNo",
        "cameraIndexCode", "extDeviceId", "fileAttribute", "options", "config")) request.put(key, "foreign");
    request.put("streamType", "sub");
    for (String type : Set.of("dahua_sdk", "hik_sdk", "tiandy_sdk"))
      service.start(instance("sdk-" + type, "tenant", type), request, "tenant", null);
    var allowed = new java.util.HashSet<>(urls("allocated").keySet());
    allowed.addAll(Set.of("deviceId", "channelId", "startTime", "endTime", "playbackRate", "playbackId", "streamType"));
    verify(adapter, times(3)).executeOperation(any(), eq("playbackStreamStart"), argThat(nativeRequest ->
        "camera".equals(nativeRequest.get("deviceId")) && "1".equals(nativeRequest.get("channelId"))
            && "sub".equals(nativeRequest.get("streamType")) && nativeRequest.keySet().stream().allMatch(allowed::contains)));
  }

  @Test void invalidPlaybackStreamSelectionDoesNotAllocateMediaOrLogin() {
    for (Object selection : new Object[] {"", "other", 1, Map.of("host", "foreign")}) {
      var request = new LinkedHashMap<>(input()); request.put("streamType", selection);
      assertThrows(BaseException.class, () -> service.start(instance, request, "tenant", null));
    }
    verifyNoInteractions(zlm, adapter);
  }

  @Test void rejectsForeignInstanceOrTenantWithoutDispatchingAnyControl() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    for (var foreign : new VideoPlatformInstance[] {
        instance("other", "tenant", "dahua_sdk"), instance("sdk", "other", "dahua_sdk")}) {
      for (String command : new String[] {"playbackStreamStatus", "playbackStreamPause", "playbackStreamStop"})
        assertThrows(BaseException.class, () -> service.control(foreign, command, Map.of("playbackId", id)));
    }
    verify(adapter, never()).executeOperation(any(), eq("playbackStreamPause"), any());
    verify(adapter, never()).executeOperation(any(), eq("playbackStreamStatus"), any());
    verify(adapter, never()).executeOperation(any(), eq("playbackStreamStop"), any());
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
  }

  @Test void pauseResumeAndRepeatedStopUseOwnedRequestAndReleaseResourcesOnce() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    assertEquals("paused", service.control(instance, "playbackStreamPause",
        Map.of("playbackId", id, "paused", true, "deviceId", "camera")).get("status"));
    assertEquals("streaming", service.control(instance, "playbackStreamPause",
        Map.of("playbackId", id, "paused", false)).get("status"));
    verify(adapter).executeOperation(eq(instance), eq("playbackStreamPause"), argThat(request ->
        Boolean.TRUE.equals(request.get("paused")) && "camera".equals(request.get("deviceId"))));
    for (int i = 0; i < 2; i++)
      assertEquals("stopped", service.control(instance, "playbackStreamStop", Map.of("playbackId", id)).get("status"));
    reapAndAwait();
    verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void missingSessionStopIsIdempotentWhileStatusAndPauseStillReject() {
    String id = "052a0f9e-6e6e-49e6-a914-d336119d2f58";
    for (int i = 0; i < 2; i++) {
      var result = service.control(instance, "playbackStreamStop", Map.of("playbackId", id));
      assertEquals("stopped", result.get("status"));
      assertEquals(id, result.get("playbackId"));
    }
    for (String operation : new String[] {"playbackStreamStatus", "playbackStreamPause"})
      assertThrows(BaseException.class, () -> service.control(instance, operation, Map.of("playbackId", id)));
    verifyNoInteractions(adapter, zlm);
  }

  @Test void stoppingReapedEndedOrFailedSessionAllowsReplacementPlayback() throws Exception {
    var field = VideoSdkPlaybackService.class.getDeclaredField("sessions"); field.setAccessible(true);
    Map<?, ?> sessions = (Map<?, ?>) field.get(service);
    for (String status : new String[] {"ended", "failed"}) {
      String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
      when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", status));
      assertEquals("ended".equals(status) ? "draining" : "failed",
          service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
      if ("ended".equals(status)) expireDrain(id);
      Object playback = sessions.get(id);
      var touched = playback.getClass().getDeclaredField("touched"); touched.setAccessible(true);
      touched.setLong(playback, System.currentTimeMillis() - 61000);
      reapAndAwait();
      assertFalse(sessions.containsKey(id));
      clearInvocations(adapter, zlm);
      assertEquals("stopped", service.control(instance, "playbackStreamStop", Map.of("playbackId", id)).get("status"));
      verifyNoInteractions(adapter, zlm);
      var replacement = service.start(instance, input(), "tenant", null);
      assertEquals("starting", replacement.get("status"));
      assertNotEquals(id, replacement.get("playbackId"));
      service.control(instance, "playbackStreamStop", Map.of("playbackId", replacement.get("playbackId")));
    }
  }

  @Test void failedStartReleasesNativeAndMediaLeasesBeforeRethrowingTheOriginalError() {
    var failure = new IllegalStateException("device rejected range");
    when(adapter.executeOperation(any(), eq("playbackStreamStart"), any())).thenThrow(failure);
    assertSame(failure, assertThrows(IllegalStateException.class,
        () -> service.start(instance, input(), "tenant", null)));
    verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), argThat(request ->
        "camera".equals(request.get("deviceId")) && request.get("playbackId") != null));
    verify(zlm).closeRtpPlayback(eq("tenant"), eq("2"), startsWith("sdk-playback-"));
    reapAndAwait();
    verify(zlm, times(1)).closeRtpPlayback(any(), anyString(), anyString());
  }

  @Test void endedStatusKeepsMediaForBufferedTailThenClosesWithoutPollingNativeAgain() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any()))
        .thenReturn(Map.of("status", "ended", "positionMs", TO));
    for (int i = 0; i < 2; i++) {
      var result = service.control(instance, "playbackStreamStatus", Map.of("playbackId", id));
      assertEquals("draining", result.get("status")); assertEquals(TO, result.get("positionMs"));
      reapAndAwait();
    }
    verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStatus"), any());
    verify(adapter, never()).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
    expireDrain(id);
    assertEquals("ended", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void explicitStopDuringDrainImmediatelyReleasesMedia() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    service.control(instance, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("stopped", service.control(instance, "playbackStreamStop", Map.of("playbackId", id)).get("status"));
    verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void univiewNativeCompletionClosesProducerAndKeepsReceiverUntilRendererAcknowledges() {
    var uniview = instance("uniview", "tenant", "uniview_sdk");
    String id = (String) service.start(uniview, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any()))
        .thenReturn(Map.of("status", "ended", "positionMs", TO));
    clearInvocations(adapter, zlm);
    for (int i = 0; i < 2; i++) {
      var draining = service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id));
      assertEquals("draining", draining.get("status"));
      assertEquals(TO, draining.get("positionMs"));
      assertEquals("ws://media.example/rtp/sdk-playback-" + id + ".live.flv", draining.get("ws_flv"));
      reapAndAwait();
    }
    var order = inOrder(adapter, zlm);
    order.verify(adapter).executeOperation(eq(uniview), eq("playbackStreamStatus"), any());
    order.verify(adapter).executeOperation(eq(uniview), eq("playbackStreamStop"), any());
    order.verify(zlm, times(2)).renewRtpPlayback("tenant", "2", "sdk-playback-" + id);
    verify(adapter, times(1)).executeOperation(eq(uniview), eq("playbackStreamStatus"), any());
    verify(adapter, times(1)).executeOperation(eq(uniview), eq("playbackStreamStop"), any());
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
    for (int i = 0; i < 2; i++)
      assertEquals("ended", service.control(uniview, "playbackStreamStop",
          Map.of("playbackId", id, "completed", true)).get("status"));
    verify(adapter, times(1)).executeOperation(eq(uniview), eq("playbackStreamStop"), any());
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void univiewProducerCloseFailureKeepsDrainingAndPollRetriesWithoutRequeryingNative() {
    var uniview = instance("uniview", "tenant", "uniview_sdk");
    String id = (String) service.start(uniview, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any()))
        .thenThrow(new IllegalStateException("producer close unavailable"))
        .thenReturn(Map.of("status", "stopped"));
    assertEquals("draining", service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    assertFalse((boolean) field(sessions().get(id), "sdkClosed"));
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
    assertEquals("draining", service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    assertTrue((boolean) field(sessions().get(id), "sdkClosed"));
    reapAndAwait();
    verify(adapter, times(1)).executeOperation(eq(uniview), eq("playbackStreamStatus"), any());
    verify(adapter, times(2)).executeOperation(eq(uniview), eq("playbackStreamStop"), any());
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
  }

  @Test void univiewReaperRetriesProducerCloseWithoutCompletingRendererDrain() {
    var uniview = instance("uniview", "tenant", "uniview_sdk");
    String id = (String) service.start(uniview, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any()))
        .thenThrow(new IllegalStateException("producer close unavailable"))
        .thenReturn(Map.of("status", "stopped"));
    service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id));
    reapAndAwait(); reapAndAwait();
    assertEquals("draining", service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    verify(adapter, times(2)).executeOperation(eq(uniview), eq("playbackStreamStop"), any());
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
  }

  @Test void univiewRendererAcknowledgementCannotCompleteWhileProducerCloseStillFails() {
    var uniview = instance("uniview", "tenant", "uniview_sdk");
    String id = (String) service.start(uniview, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any()))
        .thenThrow(new IllegalStateException("producer close unavailable"));
    service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("draining", service.control(uniview, "playbackStreamStop",
        Map.of("playbackId", id, "completed", true)).get("status"));
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
    doReturn(Map.of("status", "stopped")).when(adapter).executeOperation(any(), eq("playbackStreamStop"), any());
    assertEquals("ended", service.control(uniview, "playbackStreamStop",
        Map.of("playbackId", id, "completed", true)).get("status"));
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void univiewDrainTimeoutFailsEvenWhenProducerWasReleased() throws Exception {
    var uniview = instance("uniview", "tenant", "uniview_sdk");
    String id = (String) service.start(uniview, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id));
    expireDrain(id);
    var failed = service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("failed", failed.get("status"));
    assertEquals("回放画面和声音未能确认播放完成，请重新开始回放", failed.get("message"));
    assertEquals("failed", service.control(uniview, "playbackStreamStop",
        Map.of("playbackId", id, "completed", true)).get("status"));
    verify(adapter, times(1)).executeOperation(eq(uniview), eq("playbackStreamStop"), any());
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void univiewReaperDrainTimeoutReportsProducerFailureAndKeepsRetryingCleanup() throws Exception {
    var uniview = instance("uniview", "tenant", "uniview_sdk");
    String id = (String) service.start(uniview, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any()))
        .thenThrow(new IllegalStateException("producer close unavailable"));
    service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id));
    expireDrain(id);
    reapAndAwait();
    var failed = service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id));
    assertEquals("failed", failed.get("status"));
    assertEquals("回放源连接释放失败，请重新开始回放", failed.get("message"));
    assertFalse((boolean) field(sessions().get(id), "sdkClosed"));
    doReturn(Map.of("status", "stopped")).when(adapter).executeOperation(any(), eq("playbackStreamStop"), any());
    reapAndAwait();
    assertTrue((boolean) field(sessions().get(id), "sdkClosed"));
    assertEquals("failed", service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void univiewPausedNativeCompletionResumesReceiverBeforeClosingProducer() {
    var uniview = instance("uniview", "tenant", "uniview_sdk");
    String id = (String) service.start(uniview, input(), "tenant", null).get("playbackId");
    service.control(uniview, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    clearInvocations(adapter, zlm);
    assertEquals("draining", service.control(uniview, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    var order = inOrder(adapter, zlm);
    order.verify(adapter).executeOperation(eq(uniview), eq("playbackStreamStatus"), any());
    order.verify(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    order.verify(adapter).executeOperation(eq(uniview), eq("playbackStreamStop"), any());
    order.verify(zlm).renewRtpPlayback("tenant", "2", "sdk-playback-" + id);
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
  }

  @Test void rendererCompletionAcknowledgementEndsDrainAndReleasesMediaOnce() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    assertEquals("draining", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    var field = VideoSdkPlaybackService.class.getDeclaredField("sessions"); field.setAccessible(true);
    Object playback = ((Map<?, ?>) field.get(service)).get(id);
    var deadline = playback.getClass().getDeclaredField("drainUntil"); deadline.setAccessible(true);
    assertTrue(deadline.getLong(playback) - System.currentTimeMillis() > 55000,
        "A delayed renderer must retain more than the former three-second tail window");
    reapAndAwait();
    verify(adapter, never()).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
    for (int i = 0; i < 2; i++)
      assertEquals("ended", service.control(instance, "playbackStreamStop",
          Map.of("playbackId", id, "completed", true)).get("status"));
    verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void nativeCompletionDuringPauseResumesReceiverBeforeEnteringDrain() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamPause"), any())).thenReturn(Map.of("status", "ended"));
    clearInvocations(adapter, zlm);
    assertEquals("draining", service.control(instance, "playbackStreamPause",
        Map.of("playbackId", id, "paused", true)).get("status"));
    var order = inOrder(adapter, zlm);
    order.verify(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, true, 90);
    order.verify(adapter).executeOperation(eq(instance), eq("playbackStreamPause"), any());
    order.verify(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    order.verify(zlm).renewRtpPlayback("tenant", "2", "sdk-playback-" + id);
    reapAndAwait();
    verify(adapter, never()).executeOperation(any(), eq("playbackStreamStop"), any());
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
  }

  @Test void failedReceiverResumeAfterNativeCompletionDuringPausePreservesOriginalFailure() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamPause"), any())).thenReturn(Map.of("status", "ended"));
    var failure = new IllegalStateException("ZLM resume unavailable");
    doThrow(failure).when(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    assertSame(failure, assertThrows(IllegalStateException.class, () -> service.control(instance,
        "playbackStreamPause", Map.of("playbackId", id, "paused", true))));
    verify(zlm, times(1)).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    assertEquals("failed", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
  }

  @Test void pausedStatusCompletionResumesReceiverAndFailedResumeClosesBothResources() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    var failure = new IllegalStateException("ZLM resume unavailable");
    doThrow(failure).when(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    clearInvocations(adapter, zlm);
    assertSame(failure, assertThrows(IllegalStateException.class,
        () -> service.control(instance, "playbackStreamStatus", Map.of("playbackId", id))));
    var order = inOrder(adapter, zlm);
    order.verify(adapter).executeOperation(eq(instance), eq("playbackStreamStatus"), any());
    order.verify(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    order.verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    order.verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    assertEquals("failed", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    verify(zlm, never()).renewRtpPlayback(any(), anyString(), anyString());
  }

  @Test void prematureRendererAcknowledgementDoesNotStopNativePlayback() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    assertEquals("starting", service.control(instance, "playbackStreamStop",
        Map.of("playbackId", id, "completed", true)).get("status"));
    verify(adapter, never()).executeOperation(any(), eq("playbackStreamStop"), any());
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
    assertEquals("stopped", service.control(instance, "playbackStreamStop", Map.of("playbackId", id)).get("status"));
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void reaperFinishesDrainAfterThePlaybackWindowDisconnects() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "ended"));
    service.control(instance, "playbackStreamStatus", Map.of("playbackId", id));
    reapAndAwait();
    verify(adapter, never()).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
    expireDrain(id);
    reapAndAwait();
    assertEquals("ended", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  private void expireDrain(String id) throws Exception {
    var field = VideoSdkPlaybackService.class.getDeclaredField("sessions"); field.setAccessible(true);
    Object playback = ((Map<?, ?>) field.get(service)).get(id);
    var drainUntil = playback.getClass().getDeclaredField("drainUntil"); drainUntil.setAccessible(true);
    drainUntil.setLong(playback, System.currentTimeMillis() - 1);
  }

  @Test void reapExpiresAbandonedSessionAndRemovesItAfterSuccessfulCleanup() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    var field = VideoSdkPlaybackService.class.getDeclaredField("sessions"); field.setAccessible(true);
    Map<?, ?> sessions = (Map<?, ?>) field.get(service);
    Object playback = sessions.get(id);
    var touched = playback.getClass().getDeclaredField("touched"); touched.setAccessible(true);
    touched.setLong(playback, System.currentTimeMillis() - 61000);
    reapAndAwait(); reapAndAwait();
    assertThrows(BaseException.class, () -> service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)));
    verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void cleanupFailuresAreRetriedWithoutRepeatingSuccessfulRelease() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any()))
        .thenThrow(new IllegalStateException("native stop unavailable")).thenReturn(Map.of("status", "stopped"));
    assertEquals("stopped", service.control(instance, "playbackStreamStop", Map.of("playbackId", id)).get("status"));
    reapAndAwait(); reapAndAwait();
    verify(adapter, times(2)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void blockedCleanupDoesNotDelayOtherSessionsOrDuplicateThePendingTask() throws Exception {
    String first = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    String second = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    expireLease(first); expireLease(second);
    var playbacks = List.copyOf(sessions().values());
    var blockedId = new AtomicReference<String>();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var otherMediaClosed = new CountDownLatch(1);
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any())).thenAnswer(call -> {
      Map<String, Object> request = call.getArgument(2);
      if (blockedId.compareAndSet(null, (String) request.get("playbackId"))) {
        entered.countDown();
        assertTrue(release.await(5, TimeUnit.SECONDS));
      }
      return Map.of("status", "stopped");
    });
    doAnswer(call -> {
      if (!("sdk-playback-" + blockedId.get()).equals(call.getArgument(2)))
        otherMediaClosed.countDown();
      return null;
    }).when(zlm).closeRtpPlayback(any(), anyString(), anyString());
    try {
      assertTimeout(Duration.ofMillis(500), service::reap);
      assertTrue(entered.await(2, TimeUnit.SECONDS));
      assertTrue(otherMediaClosed.await(2, TimeUnit.SECONDS), "Other sessions must close while one gateway stop is blocked");
      for (int i = 0; i < 3; i++) assertTimeout(Duration.ofMillis(500), service::reap);
      verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStop"),
          argThat(request -> blockedId.get().equals(request.get("playbackId"))));
    } finally {
      release.countDown();
      awaitCleanup(playbacks);
    }
    verify(adapter, times(2)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    for (String id : new String[] {first, second}) {
      verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
      assertFalse(sessions().containsKey(id));
    }
  }

  @Test void fullCleanupQueueResetsPendingFlagAndRetriesOnTheNextScan() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    expireLease(id);
    Object playback = sessions().get(id);
    var executor = cleanupExecutor();
    var entered = new CountDownLatch(executor.getCorePoolSize());
    var release = new CountDownLatch(1);
    occupyCleanupWorkers(executor, entered, release);
    try {
      assertTrue(entered.await(2, TimeUnit.SECONDS));
      int queueSize = executor.getQueue().remainingCapacity();
      for (int i = 0; i < queueSize; i++) executor.execute(() -> {});
      assertEquals(0, executor.getQueue().remainingCapacity());
      assertTimeout(Duration.ofMillis(500), service::reap);
      assertFalse(cleanupPending(playback), "A rejected task must be eligible for the next scan");
      verify(adapter, never()).executeOperation(any(), eq("playbackStreamStop"), any());
      verify(zlm, never()).closeRtpPlayback(any(), anyString(), anyString());
    } finally {
      release.countDown();
      awaitCondition(() -> executor.getActiveCount() == 0 && executor.getQueue().isEmpty());
    }
    reapAndAwait();
    verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    assertFalse(sessions().containsKey(id));
  }

  @Test void abandonedStreamingAndPausedSessionsReleaseSdkAndMediaWithoutStop() throws Exception {
    for (boolean paused : new boolean[] {false, true}) {
      String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
      assertEquals("streaming", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
      if (paused) service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
      expireLease(id);
      reapAndAwait();
      assertThrows(BaseException.class, () -> service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)));
      verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), argThat(request -> id.equals(request.get("playbackId"))));
      verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    }
  }

  @Test void abandonedPausedSessionRetriesSdkAndMediaCleanupUntilBothSucceed() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any()))
        .thenThrow(new IllegalStateException("gateway temporarily unavailable")).thenReturn(Map.of("status", "stopped"));
    doThrow(new IllegalStateException("ZLM temporarily unavailable")).doNothing().when(zlm)
        .closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    expireLease(id);
    reapAndAwait(); reapAndAwait(); reapAndAwait();
    assertThrows(BaseException.class, () -> service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)));
    verify(adapter, times(2)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(2)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    verify(zlm, times(1)).pauseRtpPlayback(eq("tenant"), eq("2"), eq("sdk-playback-" + id), eq(true), anyInt());
  }

  @Test void immediatelyTerminalStartReleasesSdkAndMediaBeforeReturning() {
    for (String status : new String[] {"failed", "stopped", "expired"}) {
      when(adapter.executeOperation(any(), eq("playbackStreamStart"), any())).thenReturn(Map.of("status", status));
      var result = service.start(instance, input(), "tenant", null);
      String id = (String) result.get("playbackId");
      assertEquals(status, result.get("status"));
      verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), argThat(request -> id.equals(request.get("playbackId"))));
      verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    }
  }

  @Test void shutdownAttemptsEveryLeaseAndRejectsNewStarts() {
    String first = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    String second = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    service.control(instance, "playbackStreamPause", Map.of("playbackId", second, "paused", true));
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any()))
        .thenThrow(new IllegalStateException("first stop failed")).thenReturn(Map.of("status", "stopped"));
    service.shutdown();
    for (String id : new String[] {first, second}) {
      assertEquals("stopped", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
      verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    }
    service.shutdown();
    service.reap();
    assertTrue(cleanupExecutor().isShutdown());
    assertThrows(BaseException.class, () -> service.start(instance, input(), "tenant", null));
    verify(adapter, times(3)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(2)).openRtpPlayback(eq("tenant"), any(), anyString());
    verify(zlm, times(2)).closeRtpPlayback(any(), anyString(), anyString());
  }

  @Test void shutdownDiscardsQueuedCleanupAndClosesItsSessionSynchronously() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    expireLease(id);
    Object playback = sessions().get(id);
    var executor = cleanupExecutor();
    var entered = new CountDownLatch(executor.getCorePoolSize());
    var release = new CountDownLatch(1);
    occupyCleanupWorkers(executor, entered, release);
    try {
      assertTrue(entered.await(2, TimeUnit.SECONDS));
      service.reap();
      assertTrue(cleanupPending(playback));
      verify(adapter, never()).executeOperation(any(), eq("playbackStreamStop"), any());
      service.shutdown();
      assertFalse(cleanupPending(playback));
      assertTrue(executor.isShutdown());
      service.reap();
      verify(adapter, times(1)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
      verify(zlm, times(1)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
      assertThrows(BaseException.class, () -> service.start(instance, input(), "tenant", null));
    } finally {
      release.countDown();
      awaitCondition(executor::isTerminated);
    }
  }

  @Test void shutdownWaitsForInFlightMediaAllocationThenClosesTheNewSession() throws Exception {
    var allocating = new CountDownLatch(1);
    var continueStart = new CountDownLatch(1);
    var closing = new CountDownLatch(1);
    when(zlm.openRtpPlayback(eq("tenant"), any(), anyString())).thenAnswer(call -> {
      allocating.countDown();
      assertTrue(continueStart.await(2, TimeUnit.SECONDS));
      return urls(call.getArgument(2));
    });
    try (var executor = Executors.newFixedThreadPool(2)) {
      var start = executor.submit(() -> service.start(instance, input(), "tenant", null));
      assertTrue(allocating.await(2, TimeUnit.SECONDS));
      var shutdown = executor.submit(() -> { closing.countDown(); service.shutdown(); });
      assertTrue(closing.await(2, TimeUnit.SECONDS));
      try {
        assertThrows(TimeoutException.class, () -> shutdown.get(50, TimeUnit.MILLISECONDS));
      } finally { continueStart.countDown(); }
      String id = (String) start.get(2, TimeUnit.SECONDS).get("playbackId");
      shutdown.get(2, TimeUnit.SECONDS);
      assertEquals("stopped", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
      verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), any());
      verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    } finally { continueStart.countDown(); }
  }

  private void expireLease(String id) throws Exception {
    var field = VideoSdkPlaybackService.class.getDeclaredField("sessions"); field.setAccessible(true);
    Object playback = ((Map<?, ?>) field.get(service)).get(id);
    var touched = playback.getClass().getDeclaredField("touched"); touched.setAccessible(true);
    touched.setLong(playback, System.currentTimeMillis() - 61000);
  }

  @Test void pauseProtectsReceiverBeforeNativePauseAndResumeReopensItAfterNativeResume() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    clearInvocations(adapter, zlm);
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", false));
    var order = inOrder(adapter, zlm);
    order.verify(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, true, 90);
    order.verify(adapter).executeOperation(eq(instance), eq("playbackStreamPause"),
        argThat(request -> Boolean.TRUE.equals(request.get("paused"))));
    order.verify(adapter).executeOperation(eq(instance), eq("playbackStreamPause"),
        argThat(request -> Boolean.FALSE.equals(request.get("paused"))));
    order.verify(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
  }

  @Test void failedNativePauseRestoresReceiverAndRetainsOriginalFailure() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    var failure = new IllegalStateException("native pause failed");
    when(adapter.executeOperation(any(), eq("playbackStreamPause"), any())).thenThrow(failure);
    clearInvocations(adapter, zlm);
    assertSame(failure, assertThrows(IllegalStateException.class, () -> service.control(instance,
        "playbackStreamPause", Map.of("playbackId", id, "paused", true))));
    var order = inOrder(adapter, zlm);
    order.verify(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, true, 90);
    order.verify(adapter).executeOperation(eq(instance), eq("playbackStreamPause"), any());
    order.verify(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
  }

  @Test void receiverPauseFailureNeverDispatchesNativePause() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    doThrow(new IllegalStateException("ZLM pause failed")).when(zlm)
        .pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, true, 90);
    assertThrows(IllegalStateException.class, () -> service.control(instance,
        "playbackStreamPause", Map.of("playbackId", id, "paused", true)));
    verify(adapter, never()).executeOperation(any(), eq("playbackStreamPause"), any());
  }

  @Test void failedReceiverResumeFailsTheSessionAndReleasesBothLeases() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    var failure = new IllegalStateException("ZLM resume failed");
    doThrow(failure).when(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    clearInvocations(adapter, zlm);
    assertSame(failure, assertThrows(IllegalStateException.class, () -> service.control(instance,
        "playbackStreamPause", Map.of("playbackId", id, "paused", false))));
    var order = inOrder(adapter, zlm);
    order.verify(adapter).executeOperation(eq(instance), eq("playbackStreamPause"),
        argThat(request -> Boolean.FALSE.equals(request.get("paused"))));
    order.verify(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    order.verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    order.verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    assertEquals("failed", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    verify(adapter, never()).executeOperation(any(), eq("playbackStreamPause"),
        argThat(request -> Boolean.TRUE.equals(request.get("paused"))));
  }

  @Test void failedReceiverResumeRetriesResourceCleanupThroughReaper() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    var failure = new IllegalStateException("ZLM resume failed");
    doThrow(failure).when(zlm).pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any()))
        .thenThrow(new IllegalStateException("gateway stop temporarily failed")).thenReturn(Map.of("status", "stopped"));
    doThrow(new IllegalStateException("ZLM close temporarily failed")).doNothing().when(zlm)
        .closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    assertSame(failure, assertThrows(IllegalStateException.class, () -> service.control(instance,
        "playbackStreamPause", Map.of("playbackId", id, "paused", false))));
    assertEquals("failed", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    reapAndAwait(); reapAndAwait();
    verify(adapter, times(2)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(2)).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
  }

  @Test void pausedStatusRenewsReceiverDeadlineEveryFifteenSecondsFromOriginalPause() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any())).thenReturn(Map.of("status", "paused"));
    setPauseTimes(id, System.currentTimeMillis() - 120000, System.currentTimeMillis() - 20000);
    clearInvocations(adapter, zlm);
    assertEquals("paused", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    verify(zlm).pauseRtpPlayback(eq("tenant"), eq("2"), eq("sdk-playback-" + id), eq(true),
        intThat(seconds -> seconds >= 210 && seconds <= 212));
    service.control(instance, "playbackStreamStatus", Map.of("playbackId", id));
    verify(zlm, times(1)).pauseRtpPlayback(any(), anyString(), anyString(), eq(true), anyInt());
    verify(adapter, never()).executeOperation(any(), eq("playbackStreamPause"), any());
  }

  @Test void failedReceiverResumeAfterLongPauseNeverGuessesANewReceiverDeadline() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    setPauseTimes(id, System.currentTimeMillis() - 3600000, System.currentTimeMillis() - 20000);
    doThrow(new IllegalStateException("ZLM resume failed")).when(zlm)
        .pauseRtpPlayback("tenant", "2", "sdk-playback-" + id, false, 0);
    clearInvocations(zlm);
    assertThrows(IllegalStateException.class, () -> service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", false)));
    verify(zlm, never()).pauseRtpPlayback(any(), anyString(), anyString(), eq(true), anyInt());
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    assertEquals("failed", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
  }

  private void setPauseTimes(String id, long started, long renewed) throws Exception {
    var field = VideoSdkPlaybackService.class.getDeclaredField("sessions"); field.setAccessible(true);
    Object playback = ((Map<?, ?>) field.get(service)).get(id);
    var pauseStarted = playback.getClass().getDeclaredField("pauseStarted"); pauseStarted.setAccessible(true);
    pauseStarted.setLong(playback, started);
    var lastRenewal = playback.getClass().getDeclaredField("lastPauseRenewal"); lastRenewal.setAccessible(true);
    lastRenewal.setLong(playback, renewed);
  }

  @Test void invalidRangeDisabledInstanceOrMissingMediaNeverDispatchesAStart() {
    for (long duration : new long[] {0, 999, 7200001}) {
      var data = new LinkedHashMap<>(input()); data.put("endTime", FROM + duration);
      assertThrows(BaseException.class, () -> service.start(instance, data, "tenant", null));
    }
    var disabled = instance("sdk", "tenant", "hik_sdk"); disabled.setEnabled(0);
    assertThrows(BaseException.class, () -> service.start(disabled, input(), "tenant", null));
    when(media.getIfAvailable()).thenReturn(null);
    assertThrows(BaseException.class, () -> service.start(instance, input(), "tenant", null));
    verifyNoInteractions(zlm, adapter);
  }

  @Test void successfulStatusRenewsCapacityAfterNativeStatusAndSuccessfulStopDoesNotRenew() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    clearInvocations(adapter, zlm);
    service.control(instance, "playbackStreamStatus", Map.of("playbackId", id));
    var order = inOrder(adapter, zlm);
    order.verify(adapter).executeOperation(eq(instance), eq("playbackStreamStatus"), any());
    order.verify(zlm).renewRtpPlayback("tenant", "2", "sdk-playback-" + id);
    service.control(instance, "playbackStreamStop", Map.of("playbackId", id));
    verify(zlm, times(1)).renewRtpPlayback(any(), anyString(), anyString());
  }

  @Test void repeatedNativeStatusFailuresDoNotRefreshHeartbeatOrCapacityAndReaperCloses() throws Exception {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    expireLease(id);
    var sessionsField = VideoSdkPlaybackService.class.getDeclaredField("sessions"); sessionsField.setAccessible(true);
    Map<?, ?> sessions = (Map<?, ?>) sessionsField.get(service);
    Object playback = sessions.get(id);
    var touched = playback.getClass().getDeclaredField("touched"); touched.setAccessible(true);
    long originalTouched = touched.getLong(playback);
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any()))
        .thenThrow(new IllegalStateException("gateway unavailable"));
    clearInvocations(adapter, zlm);
    for (int i = 0; i < 3; i++)
      assertThrows(IllegalStateException.class, () -> service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)));
    assertEquals(originalTouched, touched.getLong(playback));
    verify(zlm, never()).renewRtpPlayback(any(), anyString(), anyString());
    reapAndAwait();
    verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    assertFalse(sessions.containsKey(id));
  }

  @Test void failedCapacityRenewalInvalidatesPlaybackAndClosesBothResources() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    var failure = new IllegalStateException("expired Redis lease");
    doThrow(failure).when(zlm).renewRtpPlayback("tenant", "2", "sdk-playback-" + id);
    assertSame(failure, assertThrows(IllegalStateException.class,
        () -> service.control(instance, "playbackStreamStatus", Map.of("playbackId", id))));
    verify(adapter).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm).closeRtpPlayback("tenant", "2", "sdk-playback-" + id);
    assertEquals("failed", service.control(instance, "playbackStreamStatus", Map.of("playbackId", id)).get("status"));
    verify(zlm, times(1)).renewRtpPlayback(any(), anyString(), anyString());
  }

  @Test void reaperNeverRenewsStreamingOrPausedCapacityLeases() {
    String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
    reapAndAwait();
    service.control(instance, "playbackStreamPause", Map.of("playbackId", id, "paused", true));
    clearInvocations(zlm);
    reapAndAwait(); reapAndAwait();
    verify(zlm, never()).renewRtpPlayback(any(), anyString(), anyString());
  }

  @Test void concurrentLimitStopsFurtherMediaAllocationAndTiandyAllowsAudioDetection() {
    var tiandy = instance("tiandy", "tenant", "tiandy_sdk");
    assertEquals(true, service.start(tiandy, input(), "tenant", null).get("audioSupported"));
    for (int i = 1; i < 12; i++) service.start(instance, input(), "tenant", null);
    assertThrows(BaseException.class, () -> service.start(instance, input(), "tenant", null));
    verify(zlm, times(12)).openRtpPlayback(eq("tenant"), isNull(), anyString());
  }

  @Test void gatewayAudioDetectionOverridesTheDefaultForEveryVendor() {
    var tiandy = instance("tiandy", "tenant", "tiandy_sdk");
    when(adapter.executeOperation(any(), eq("playbackStreamStart"), any()))
        .thenReturn(Map.of("status", "starting", "audioSupported", false));
    var started = service.start(tiandy, input(), "tenant", null);
    assertEquals(false, started.get("audioSupported"));
    when(adapter.executeOperation(any(), eq("playbackStreamStatus"), any()))
        .thenReturn(Map.of("status", "streaming", "audioSupported", true, "audioCodec", "PCMA"));
    var detected = service.control(tiandy, "playbackStreamStatus", Map.of("playbackId", started.get("playbackId")));
    assertEquals(true, detected.get("audioSupported"));
    assertEquals("PCMA", detected.get("audioCodec"));
  }

  @Test void failedCleanupKeepsConcurrencySlotsUntilResourcesAreReleasedAndCachedSessionsSkipDispatch() {
    when(adapter.executeOperation(any(), eq("playbackStreamStop"), any()))
        .thenThrow(new IllegalStateException("gateway unavailable"));
    for (int i = 0; i < 12; i++) {
      String id = (String) service.start(instance, input(), "tenant", null).get("playbackId");
      assertEquals("stopped", service.control(instance, "playbackStreamStop", Map.of("playbackId", id)).get("status"));
    }
    assertThrows(BaseException.class, () -> service.start(instance, input(), "tenant", null));
    verify(zlm, times(12)).openRtpPlayback(eq("tenant"), isNull(), anyString());
    doReturn(Map.of("status", "stopped")).when(adapter).executeOperation(any(), eq("playbackStreamStop"), any());
    reapAndAwait();
    verify(adapter, times(24)).executeOperation(eq(instance), eq("playbackStreamStop"), any());
    verify(zlm, times(12)).closeRtpPlayback(any(), anyString(), anyString());
    assertEquals(12, sessions().size(), "Closed terminal sessions remain cached during their acknowledgement window");
    var executor = cleanupExecutor();
    awaitCondition(() -> executor.getActiveCount() == 0 && executor.getQueue().isEmpty());
    long submittedTasks = executor.getTaskCount();
    service.reap();
    assertEquals(submittedTasks, executor.getTaskCount(), "Closed cached sessions must not occupy the cleanup queue");
    assertEquals("starting", service.start(instance, input(), "tenant", null).get("status"));
    verify(zlm, times(13)).openRtpPlayback(eq("tenant"), isNull(), anyString());
  }

  private void reapAndAwait() {
    var playbacks = List.copyOf(sessions().values());
    service.reap();
    awaitCleanup(playbacks);
  }

  private void awaitCleanup(List<?> playbacks) {
    awaitCondition(() -> playbacks.stream().noneMatch(this::cleanupPending));
  }

  private void awaitCondition(BooleanSupplier complete) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
    while (!complete.getAsBoolean() && System.nanoTime() < deadline) {
      try { Thread.sleep(5); }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted while waiting for playback cleanup", ex);
      }
    }
    assertTrue(complete.getAsBoolean(), "Playback cleanup did not complete within two seconds");
  }

  private Map<?, ?> sessions() { return (Map<?, ?>) field(service, "sessions"); }

  private ThreadPoolExecutor cleanupExecutor() {
    return (ThreadPoolExecutor) field(service, "cleanupExecutor");
  }

  private boolean cleanupPending(Object playback) {
    return ((AtomicBoolean) field(playback, "cleanupPending")).get();
  }

  private Object field(Object target, String name) {
    try {
      var field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(target);
    } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
  }

  private void occupyCleanupWorkers(ThreadPoolExecutor executor, CountDownLatch entered, CountDownLatch release) {
    for (int i = 0; i < executor.getCorePoolSize(); i++) executor.execute(() -> {
      entered.countDown();
      try { release.await(); }
      catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    });
  }
}
