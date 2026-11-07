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

class VideoDahuaPreviewServiceTest {
  private final VideoPlatformAdapterRegistry adapters = mock(VideoPlatformAdapterRegistry.class);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final ObjectProvider<VideoZlmStreamProxyService> provider = mock(ObjectProvider.class);
  private final VideoZlmStreamProxyService media = mock(VideoZlmStreamProxyService.class);
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoDahuaPreviewService preview = new VideoDahuaPreviewService(adapters, provider, instances, devices);
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder().id(1L).instanceKey("dh-a")
      .platformType("dahua_sdk").tenantId("tenant-a").auth("private").enabled(1)
      .options("{\"activeRegistration\":{\"enabled\":true}}").build();
  @BeforeEach void setup() {
    when(provider.getIfAvailable()).thenReturn(media); when(adapters.getAdapter("dahua_sdk")).thenReturn(adapter);
    when(instances.selectByPrimaryKey(1L)).thenReturn(instance);
    when(devices.selectOne(any())).thenReturn(VideoPlatformDevice.builder().enabled(1).tenantId("tenant-a").build());
    when(media.openRtpPlayback(eq("tenant-a"), eq("node-a"), anyString())).thenAnswer(call -> Map.of(
        "zlmHost", "192.0.2.1", "zlmRtpPort", 19000, "ssrc", "1", "zlmTransport", "tcp",
        "mediaServerPrimaryId", "node-a", "zlmStream", call.getArgument(2), "ws_flv", "ws://node/live.flv"));
    when(adapter.executeOperation(any(), eq("previewStreamStart"), anyMap())).thenReturn(Map.of("status", "connecting"));
  }
  private Map<String, String> start() {
    return preview.start(instance, "register-a", "1", "sub", Map.of("zlmHost", "untrusted"), "tenant-a", "node-a");
  }
  @Test void nativePreviewUsesTrustedTenantMediaTargetAndHidesInternalRtpFields() {
    var result = start(); var request = org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(adapter).executeOperation(eq(instance), eq("previewStreamStart"), request.capture());
    assertEquals("192.0.2.1", request.getValue().get("zlmHost")); assertEquals("sub", request.getValue().get("streamType"));
    assertEquals("gateway_dahua_registration", result.get("streamSource"));
    assertFalse(result.containsKey("zlmHost")); assertFalse(result.containsKey("ssrc"));
    preview.shutdown();
  }
  @Test void stopRequiresExactTenantDeviceStreamAndReleasesOnlyOneViewer() {
    var first = start(); var second = start(); assertNotEquals(first.get("stream"), second.get("stream"));
    var other = VideoPlatformInstance.builder().instanceKey("dh-a").tenantId("tenant-b").build();
    assertEquals(0, preview.stop(other, "register-a", "1", Map.of("stream", first.get("stream"))).get("releasedSessions"));
    preview.stop(instance, "register-a", "1", Map.of("stream", first.get("stream"), "previewId", first.get("previewId")));
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
    assertEquals(false, preview.heartbeat(instance, "register-a", "1", stream, "wrong-id").get("active"));
    assertEquals(true, preview.heartbeat(instance, "register-a", "1", stream, id).get("active"));
    when(adapter.executeOperation(eq(instance), eq("previewStreamStatus"), anyMap())).thenReturn(Map.of("status", "streaming", "bytes", 100));
    preview.renewAndCleanup(); verify(media).renewRtpPlayback("tenant-a", "node-a", stream);
    instance.setEnabled(0); preview.renewAndCleanup(); verify(media).closeRtpPlayback("tenant-a", "node-a", stream);
    assertEquals("expired", preview.heartbeat(instance, "register-a", "1", stream, id).get("status"));
  }
  @Test void failedNativeReleaseRetriesWithoutReleasingMediaTwice() {
    var result = start(); when(adapter.executeOperation(eq(instance), eq("previewStreamStop"), anyMap()))
        .thenThrow(new IllegalStateException("temporary")).thenReturn(Map.of("stopped", true));
    preview.stop(instance, "register-a", "1", Map.of("stream", result.get("stream"))); preview.renewAndCleanup();
    verify(adapter, times(2)).executeOperation(eq(instance), eq("previewStreamStop"), anyMap());
    verify(media, times(1)).closeRtpPlayback("tenant-a", "node-a", result.get("stream"));
  }
}
