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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class VideoUniviewPreviewServiceTest {
  private final VideoPlatformAdapterRegistry adapters = mock(VideoPlatformAdapterRegistry.class);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final ObjectProvider<VideoZlmStreamProxyService> provider = mock(ObjectProvider.class);
  private final VideoZlmStreamProxyService media = mock(VideoZlmStreamProxyService.class);
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoUniviewPreviewService preview = new VideoUniviewPreviewService(adapters, provider, instances, devices);
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder().id(1L).instanceKey("uv-a")
      .platformType("uniview_sdk").tenantId("tenant-a").auth("private").enabled(1)
      .options("{}").build();
  @BeforeEach void setup() {
    when(provider.getIfAvailable()).thenReturn(media); when(adapters.getAdapter("uniview_sdk")).thenReturn(adapter);
    when(instances.selectByPrimaryKey(1L)).thenReturn(instance);
    when(devices.selectOne(any())).thenReturn(VideoPlatformDevice.builder().enabled(1).tenantId("tenant-a").build());
    when(media.openRtpPlayback(eq("tenant-a"), eq("node-a"), anyString())).thenAnswer(call -> Map.of(
        "zlmHost", "192.0.2.1", "zlmRtpPort", 19000, "ssrc", "1", "zlmTransport", "tcp",
        "mediaServerPrimaryId", "node-a", "zlmStream", call.getArgument(2), "ws_flv", "ws://node/live.flv", "secret", "private-secret"));
    when(adapter.executeOperation(any(), eq("previewStreamStart"), anyMap())).thenReturn(Map.of("status", "connecting"));
  }
  private Map<String, String> start() {
    return preview.start(instance, "nvr-a", "1", "sub", Map.of("zlmHost", "untrusted"), "tenant-a", "node-a");
  }
  @Test void nativePreviewUsesTrustedTenantMediaTargetAndHidesInternalRtpFields() {
    var result = start(); var request = org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(adapter).executeOperation(eq(instance), eq("previewStreamStart"), request.capture());
    assertEquals("192.0.2.1", request.getValue().get("zlmHost")); assertEquals("sub", request.getValue().get("streamType"));
    assertEquals("gateway_uniview_sdk", result.get("streamSource"));
    assertFalse(result.containsKey("zlmHost")); assertFalse(result.containsKey("ssrc"));
    assertFalse(result.containsKey("secret")); assertFalse(result.containsValue("private-secret"));
    assertEquals("node-a", result.get("mediaServerId"));
    assertEquals(result.get("previewId"), request.getValue().get("previewId"));
    preview.shutdown();
  }
  @Test void stopRequiresExactTenantDeviceStreamAndReleasesOnlyOneViewer() {
    var first = start(); var second = start(); assertNotEquals(first.get("stream"), second.get("stream"));
    var other = VideoPlatformInstance.builder().instanceKey("uv-a").tenantId("tenant-b").build();
    assertEquals(0, preview.stop(other, "nvr-a", "1", Map.of("stream", first.get("stream"))).get("releasedSessions"));
    preview.stop(instance, "nvr-a", "1", Map.of("stream", first.get("stream"), "previewId", first.get("previewId")));
    verify(media).closeRtpPlayback("tenant-a", "node-a", first.get("stream"));
    verify(media, never()).closeRtpPlayback("tenant-a", "node-a", second.get("stream")); preview.shutdown();
  }
  @Test void failedNativeStartReleasesBothSdkAndMediaResources() {
    when(adapter.executeOperation(any(), eq("previewStreamStart"), anyMap())).thenThrow(new IllegalStateException("failure"));
    assertThrows(IllegalStateException.class, this::start);
    verify(adapter).executeOperation(eq(instance), eq("previewStreamStop"), anyMap());
    verify(media).closeRtpPlayback(eq("tenant-a"), eq("node-a"), anyString());
  }
  @Test void disabledOrForeignDeviceNeverAllocatesMedia() {
    when(devices.selectOne(any())).thenReturn(VideoPlatformDevice.builder().enabled(1).tenantId("tenant-b").build());
    assertThrows(RuntimeException.class, this::start);
    when(devices.selectOne(any())).thenReturn(VideoPlatformDevice.builder().enabled(0).tenantId("tenant-a").build());
    assertThrows(RuntimeException.class, this::start); verifyNoInteractions(media, adapter);
  }
  @Test void heartbeatBindsViewerAndNativeStatusRenewsBothLeases() {
    var result = start(); String stream = result.get("stream"), id = result.get("previewId");
    assertEquals(false, preview.heartbeat(instance, "wrong-device", "1", stream, id).get("active"));
    assertEquals(false, preview.heartbeat(instance, "nvr-a", "1", stream, "wrong-id").get("active"));
    assertEquals(true, preview.heartbeat(instance, "nvr-a", "1", stream, id).get("active"));
    when(adapter.executeOperation(eq(instance), eq("previewStreamStatus"), anyMap())).thenReturn(Map.of("status", "streaming", "bytes", 100));
    preview.renewAndCleanup(); verify(media).renewRtpPlayback("tenant-a", "node-a", stream);
    instance.setEnabled(0); preview.renewAndCleanup(); verify(media).closeRtpPlayback("tenant-a", "node-a", stream);
    assertEquals("expired", preview.heartbeat(instance, "nvr-a", "1", stream, id).get("status"));
  }
  @Test void failedNativeReleaseRetriesWithoutReleasingMediaTwice() {
    var result = start(); when(adapter.executeOperation(eq(instance), eq("previewStreamStop"), anyMap()))
        .thenThrow(new IllegalStateException("temporary")).thenReturn(Map.of("stopped", true));
    preview.stop(instance, "nvr-a", "1", Map.of("stream", result.get("stream"))); preview.renewAndCleanup();
    verify(adapter, times(2)).executeOperation(eq(instance), eq("previewStreamStop"), anyMap());
    verify(media, times(1)).closeRtpPlayback("tenant-a", "node-a", result.get("stream"));
  }

  @Test void missingStopIdentityCannotCloseOtherViewers() {
    var result = start();
    assertThrows(RuntimeException.class, () -> preview.stop(instance, "nvr-a", "1", Map.of()));
    assertThrows(RuntimeException.class, () -> preview.stop(instance, "nvr-a", "1", null));
    verify(media, never()).closeRtpPlayback(any(), any(), any());
    preview.stop(instance, "nvr-a", "1", Map.of("previewId", result.get("previewId")));
    verify(media).closeRtpPlayback("tenant-a", "node-a", result.get("stream"));
  }
  @Test void wrongOwnerDeviceOrChannelCannotTouchAViewer() {
    var result = start();
    assertThrows(RuntimeException.class, () -> preview.stop(instance, "nvr-a", "1", Map.of(
        "previewId", result.get("previewId"), "ownerNode", "wrong-node")));
    assertEquals(0, preview.stop(instance, "other", "1", Map.of("previewId", result.get("previewId"))).get("releasedSessions"));
    assertEquals(0, preview.stop(instance, "nvr-a", "2", Map.of("previewId", result.get("previewId"))).get("releasedSessions"));
    verify(media, never()).closeRtpPlayback(any(), any(), any());
    preview.assertOwner(result.get("ownerNode"));
    preview.shutdown();
  }
  @Test void mainPreviewNeedsNoActiveRegistrationAndKeepsItsOwnSdkIdentity() {
    var result = preview.start(instance, "nvr-a", "5", "main", null, "tenant-a", "node-a");
    var request = org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(adapter).executeOperation(eq(instance), eq("previewStreamStart"), request.capture());
    assertEquals("main", request.getValue().get("streamType"));
    assertEquals("5", request.getValue().get("channelId"));
    assertTrue(result.get("stream").startsWith("uniview-live-"));
    assertEquals(result.get("previewId"), request.getValue().get("previewId"));
    preview.shutdown();
  }
  @Test void nativeTerminalStartReleasesResourcesAndReturnsNoPlayableSession() {
    when(adapter.executeOperation(any(), eq("previewStreamStart"), anyMap())).thenReturn(Map.of("status", "failed"));
    assertThrows(RuntimeException.class, this::start);
    verify(adapter).executeOperation(eq(instance), eq("previewStreamStop"), anyMap());
    verify(media).closeRtpPlayback(eq("tenant-a"), eq("node-a"), anyString());
  }
  @Test void nativeTerminalStatusReleasesResourcesAndDoesNotRenewTheMediaLease() {
    var result = start();
    when(adapter.executeOperation(any(), eq("previewStreamStatus"), anyMap())).thenReturn(Map.of("status", "ended"));
    preview.renewAndCleanup();
    verify(media).closeRtpPlayback("tenant-a", "node-a", result.get("stream"));
    verify(media, never()).renewRtpPlayback(any(), any(), any());
  }
  @Test void changedDeviceTenantReleasesSessionBeforeStatusOrRenewal() {
    var result = start();
    when(devices.selectOne(any())).thenReturn(VideoPlatformDevice.builder().enabled(1).tenantId("tenant-b").build());
    preview.renewAndCleanup();
    verify(media).closeRtpPlayback("tenant-a", "node-a", result.get("stream"));
    verify(adapter, never()).executeOperation(any(), eq("previewStreamStatus"), anyMap());
  }
  @Test void nativeStatusDoesNotExposeGatewayConnectionSecretsInBrowserHeartbeat() {
    var result = start();
    when(adapter.executeOperation(any(), eq("previewStreamStatus"), anyMap())).thenReturn(Map.of(
        "status", "streaming", "bytes", 100L, "zlmHost", "private-host", "password", "private-password"));
    when(media.isRtpPlaybackReady(any(), any(), any())).thenReturn(true);
    preview.renewAndCleanup();
    var state = preview.heartbeat(instance, "nvr-a", "1", result.get("stream"), result.get("previewId"));
    assertEquals("streaming", state.get("status"));
    assertEquals(true, state.get("mediaReady"));
    assertFalse(state.containsKey("zlmHost")); assertFalse(state.containsKey("password"));
    preview.shutdown();
  }
}
