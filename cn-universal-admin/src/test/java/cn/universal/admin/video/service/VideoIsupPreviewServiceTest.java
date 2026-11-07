package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.zlm.VideoZlmStreamProxyService;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class VideoIsupPreviewServiceTest {
  private final VideoPlatformAdapterRegistry adapters = mock(VideoPlatformAdapterRegistry.class);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final ObjectProvider<VideoZlmStreamProxyService> provider = mock(ObjectProvider.class);
  private final VideoZlmStreamProxyService media = mock(VideoZlmStreamProxyService.class);
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoIsupPreviewService preview = new VideoIsupPreviewService(adapters, provider, instances, devices);
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder().id(1L).instanceKey("isup-a")
      .platformType("hik_isup").tenantId("tenant-a").auth("private").enabled(1).build();

  @BeforeEach void setup() {
    when(provider.getIfAvailable()).thenReturn(media); when(adapters.getAdapter("hik_isup")).thenReturn(adapter);
    when(instances.selectByPrimaryKey(1L)).thenReturn(instance);
    when(devices.selectOne(any())).thenReturn(VideoPlatformDevice.builder().enabled(1).build());
    when(media.openRtpPlayback(eq("tenant-a"), eq("node-a"), anyString())).thenAnswer(call -> Map.of(
        "zlmHost", "192.0.2.1", "zlmRtpPort", 19000, "ssrc", "1", "zlmTransport", "tcp",
        "mediaServerPrimaryId", "node-a", "zlmStream", call.getArgument(2), "ws_flv", "ws://node/live.flv"));
    when(adapter.getStreamUrl(any(), anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(Map.of("status", "CONNECTING"));
  }

  private Map<String, String> start() {
    return preview.start(instance, "nvr-a", "33", "sub", Map.of("zlmHost", "untrusted"), "tenant-a", "node-a");
  }

  @Test void usesSelectedTenantNodeAndTrustedRtpTargetWithoutLeakingInternalFields() {
    Map<String, String> result = start();
    var request = org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(adapter).getStreamUrl(eq(instance), eq("nvr-a"), eq("33"), eq("sub"), request.capture());
    assertEquals("192.0.2.1", request.getValue().get("zlmHost"));
    assertEquals(19000, request.getValue().get("zlmRtpPort"));
    assertEquals("node-a", result.get("mediaServerId"));
    assertFalse(result.containsKey("zlmHost")); assertFalse(result.containsKey("ssrc"));
    preview.stop(instance, "nvr-a", "33", Map.of("stream", result.get("stream")));
  }

  @Test void closingOneViewerDoesNotStopAnotherOrAnotherTenant() {
    var first = start(); var second = start();
    assertNotEquals(first.get("stream"), second.get("stream"));
    var other = VideoPlatformInstance.builder().instanceKey("isup-a").tenantId("tenant-b").build();
    assertEquals(0, preview.stop(other, "nvr-a", "33", Map.of("stream", first.get("stream"))).get("releasedSessions"));
    preview.stop(instance, "nvr-a", "33", Map.of("stream", first.get("stream")));
    verify(media).closeRtpPlayback("tenant-a", "node-a", first.get("stream"));
    verify(media, never()).closeRtpPlayback("tenant-a", "node-a", second.get("stream"));
    preview.shutdown();
  }

  @Test void failedStartClosesNativeAndMediaResources() {
    when(adapter.getStreamUrl(any(), anyString(), anyString(), anyString(), anyMap()))
        .thenThrow(new IllegalStateException("failure"));
    assertThrows(IllegalStateException.class, this::start);
    verify(adapter).executeOperation(eq(instance), eq("stopPreview"), anyMap());
    verify(media).closeRtpPlayback(eq("tenant-a"), eq("node-a"), anyString());
  }

  @Test void disabledOrMissingDeviceCannotAllocateMediaOrStartNativePreview() {
    when(devices.selectOne(any())).thenReturn(VideoPlatformDevice.builder().enabled(0).build());
    assertThrows(RuntimeException.class, this::start);
    when(devices.selectOne(any())).thenReturn(null);
    assertThrows(RuntimeException.class, this::start);
    verifyNoInteractions(media, adapter);
  }

  @Test void stopFailureIsRetriedWhileDisabledPlatformIsCleaned() {
    var result = start();
    when(adapter.executeOperation(eq(instance), eq("stopPreview"), anyMap()))
        .thenThrow(new IllegalStateException("temporary")).thenReturn(Map.of("stopped", true));
    preview.stop(instance, "nvr-a", "33", Map.of("stream", result.get("stream")));
    preview.renewAndCleanup();
    verify(adapter, times(2)).executeOperation(eq(instance), eq("stopPreview"), anyMap());
    verify(media, times(1)).closeRtpPlayback("tenant-a", "node-a", result.get("stream"));
  }

  @Test void activeViewerRenewsBothNativeAndMediaLeaseAndDisabledInstanceStopsIt() {
    var result = start();
    when(adapter.executeOperation(eq(instance), eq("renewStream"), anyMap())).thenReturn(Map.of("status", "streaming"));
    preview.renewAndCleanup();
    verify(adapter).executeOperation(instance, "renewStream", Map.of("deviceId", "nvr-a", "channelId", "33", "streamKey", result.get("stream")));
    verify(media).renewRtpPlayback("tenant-a", "node-a", result.get("stream"));
    instance.setEnabled(0);
    preview.renewAndCleanup();
    verify(media).closeRtpPlayback("tenant-a", "node-a", result.get("stream"));
  }

  @Test void heartbeatRequiresExactTenantDeviceAndStreamAndExpiredViewerIsReleased() {
    var result = start(); String stream = result.get("stream");
    assertEquals(false, preview.heartbeat(instance, "other-device", "33", stream).get("active"));
    assertEquals(true, preview.heartbeat(instance, "nvr-a", "33", stream).get("active"));
    var sessions = (Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(preview, "sessions");
    org.springframework.test.util.ReflectionTestUtils.setField(sessions.values().iterator().next(), "touched", System.currentTimeMillis() - 61000);
    preview.renewAndCleanup();
    verify(media).closeRtpPlayback("tenant-a", "node-a", stream);
    assertEquals("expired", preview.heartbeat(instance, "nvr-a", "33", stream).get("status"));
  }
}
