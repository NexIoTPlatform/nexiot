package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.OpenApiVideoScope;
import cn.universal.admin.video.service.VideoOperationAccess;
import cn.universal.admin.video.service.VideoUniviewPreviewService;
import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.zlm.VideoZlmStreamProxyService;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class VideoUniviewPreviewRoutingTest {
  private final VideoPlatformInstanceServiceImpl service = mock(VideoPlatformInstanceServiceImpl.class, CALLS_REAL_METHODS);
  private final VideoUniviewPreviewService preview = mock(VideoUniviewPreviewService.class);
  private final VideoOperationAccess access = mock(VideoOperationAccess.class);
  private final VideoZlmStreamProxyService media = mock(VideoZlmStreamProxyService.class);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformDevice device = VideoPlatformDevice.builder().id(11L)
      .instanceKey("uv-test").deviceId("nvr").tenantId("tenant-a").enabled(1).build();
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder().id(1L)
      .instanceKey("uv-test").platformType("uniview_sdk").tenantId("tenant-a").enabled(1).build();

  @BeforeEach @SuppressWarnings("unchecked") void setup() {
    SecurityContextHolder.clearContext();
    for (Class<?> entity : List.of(VideoPlatformDevice.class, VideoPlatformChannel.class))
      EntityHelper.initEntityNameMap(entity, new Config());
    var registry = mock(VideoPlatformAdapterRegistry.class);
    ObjectProvider<VideoZlmStreamProxyService> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(media);
    when(registry.getAdapter("uniview_sdk")).thenReturn(adapter);
    when(devices.selectByExample(any())).thenReturn(List.of(device));
    when(devices.selectByPrimaryKey(11L)).thenReturn(device);
    when(channels.selectByExample(any())).thenReturn(List.of(VideoPlatformChannel.builder()
        .id(12L).deviceId(11L).instanceKey("uv-test").channelId("5").tenantId("tenant-a").build()));
    ReflectionTestUtils.setField(service, "deviceMapper", devices);
    ReflectionTestUtils.setField(service, "channelMapper", channels);
    ReflectionTestUtils.setField(service, "adapterRegistry", registry);
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    ReflectionTestUtils.setField(service, "zlmStreamProxyServiceProvider", provider);
    ReflectionTestUtils.setField(service, "univiewPreviewService", preview);
    ReflectionTestUtils.setField(service, "videoOperationAccess", access);
    doReturn(instance).when(service).getByInstanceKey("uv-test");
    doReturn(Map.of()).when(service).getDeviceDetail("uv-test", "nvr");
  }

  @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

  @Test void platformPreviewAlwaysUsesNativeSdkAndRespectsSelectedMediaNode() {
    when(preview.start(eq(instance), eq("nvr"), eq("5"), eq("sub"), anyMap(), eq("tenant-a"), eq("selected-node")))
        .thenReturn(Map.of("ws_flv", "ws://media.invalid/rtp/preview.live.flv", "rtsp", "rtsp://media.invalid/rtp/preview",
            "previewId", "preview-1", "stream", "preview", "streamSource", "gateway_uniview_sdk", "streamMode", "sdk_zlm_rtp"));
    var result = service.getStreamUrl("uv-test", "nvr", "5", "sub", Map.of("mediaServerId", "selected-node"));
    assertEquals("ws://media.invalid/rtp/preview.live.flv", result.get("preferredUrl"));
    assertEquals("preview-1", result.get("previewId"));
    verify(access).assertUserOperation("preview");
    verifyNoInteractions(adapter, media);
  }

  @ParameterizedTest @ValueSource(strings = {"main", "sub"})
  void desktopOriginUsesSavedDeviceWithoutAllocatingNativeOrMediaSession(String streamType) {
    String authorized = "rtsp://camera.invalid/unicast/c5/s0/live";
    when(adapter.getStreamUrl(eq(instance), eq("nvr"), eq("5"), eq(streamType), anyMap()))
        .thenReturn(Map.of("rtsp", authorized, "streamSource", "origin", "streamMode", "rtsp_first"));
    Map<String, String> result = service.getStreamUrl("uv-test", "nvr", "5", streamType,
        Map.of("playMode", "origin", "preferProtocol", "rtsp", "disableZlmProxy", "true",
            "proxyRtsp", "false", "mediaServerId", "not-required", "rtsp", "rtsp://untrusted.invalid/other",
            "devicePassword", "untrusted", "rtspChannel", "999"));

    assertEquals(authorized, result.get("preferredUrl"));
    assertEquals("origin", result.get("streamSource"));
    assertFalse(result.containsKey("previewId"));
    verify(adapter).getStreamUrl(eq(instance), eq("nvr"), eq("5"), eq(streamType), argThat(options ->
        "rtsp_first".equals(options.get("streamMode")) && Boolean.FALSE.equals(options.get("forceSdkFallback"))
            && !options.containsKey("rtsp") && !options.containsKey("devicePassword")
            && !options.containsKey("rtspChannel") && !options.containsKey("mediaServerId")));
    verify(access).assertUserOperation("preview");
    verifyNoInteractions(preview, media);
  }

  @Test void disabledOrForeignDeviceCannotIssueADirectEndpoint() {
    device.setEnabled(0);
    assertThrows(BaseException.class, () -> service.getStreamUrl("uv-test", "nvr", "5", "main",
        Map.of("disableZlmProxy", "true")));
    device.setEnabled(1);
    device.setTenantId("tenant-b");
    assertThrows(BaseException.class, () -> service.getStreamUrl("uv-test", "nvr", "5", "main",
        Map.of("disableZlmProxy", "true")));
    device.setTenantId("tenant-a");
    instance.setEnabled(0);
    assertThrows(BaseException.class, () -> service.getStreamUrl("uv-test", "nvr", "5", "main",
        Map.of("disableZlmProxy", "true")));
    verifyNoInteractions(adapter, preview, media);
  }

  @Test void deniedDirectPreviewCannotReadTheDeviceEndpoint() {
    doThrow(new BaseException("denied")).when(access).assertUserOperation("preview");
    assertThrows(BaseException.class, () -> service.getStreamUrl("uv-test", "nvr", "5", "main",
        Map.of("disableZlmProxy", "true")));
    verifyNoInteractions(adapter, preview, media);
  }

  @Test void missingDirectEndpointNeverFallsBackToAllocatingANativeSession() {
    when(adapter.getStreamUrl(eq(instance), eq("nvr"), eq("5"), eq("main"), anyMap()))
        .thenThrow(new IllegalStateException("device RTSP unavailable"));
    assertThrows(IllegalStateException.class, () -> service.getStreamUrl("uv-test", "nvr", "5", "main",
        Map.of("disableZlmProxy", "true")));
    verifyNoInteractions(preview, media);
  }

  @ParameterizedTest @ValueSource(strings = {"stopPreview", "previewStop", "stopStream"})
  void stopAliasesReleaseTheNativeSessionEvenWithoutModeMarkers(String operation) {
    when(preview.stop(eq(instance), eq("nvr"), eq("5"), anyMap())).thenReturn(Map.of("stopped", true));
    assertEquals(true, service.executePlatformOperation("uv-test", operation,
        Map.of("deviceId", "nvr", "channelId", "5", "stream", "uniview-live-1")).get("stopped"));
    verify(preview).stop(eq(instance), eq("nvr"), eq("5"), argThat(options -> "uniview-live-1".equals(options.get("stream"))));
    verifyNoInteractions(adapter, media);
  }

  @ParameterizedTest @ValueSource(strings = {"previewStreamStart", "previewStreamStatus", "previewStreamStop"})
  void browserCannotDirectlyAllocateOrControlGatewayRtpSessions(String operation) {
    assertThrows(BaseException.class, () -> service.executePlatformOperation("uv-test", operation,
        Map.of("deviceId", "nvr", "channelId", "5", "zlmHost", "untrusted")));
    verifyNoInteractions(preview, adapter, media);
  }

  @Test void heartbeatChecksChannelAndOwnerAndCarriesTheExactPreviewId() {
    when(preview.heartbeat(instance, "nvr", "5", "uniview-live-1", "preview-1")).thenReturn(Map.of("active", true));
    assertEquals(true, service.executePlatformOperation("uv-test", "previewHeartbeat", Map.of(
        "deviceId", "nvr", "channelId", "5", "stream", "uniview-live-1", "previewId", "preview-1", "ownerNode", "backend-a")).get("active"));
    verify(preview).assertOwner("backend-a");
    verify(access).assertUserOperation("previewHeartbeat");
    verifyNoInteractions(adapter, media);
  }

  @ParameterizedTest @ValueSource(strings = {"previewHeartbeat", "stopPreview"})
  void missingChannelCannotReachNativeSessionControl(String operation) {
    when(channels.selectByExample(any())).thenReturn(List.of());
    assertThrows(BaseException.class, () -> service.executePlatformOperation("uv-test", operation,
        Map.of("deviceId", "nvr", "channelId", "missing", "stream", "uniview-live-1")));
    verifyNoInteractions(preview, adapter, media);
  }

  @Test void deniedPreviewPermissionCannotAllocateNativeResources() {
    doThrow(new BaseException("denied")).when(access).assertUserOperation("preview");
    assertThrows(BaseException.class, () -> service.getStreamUrl("uv-test", "nvr", "5", "main", Map.of()));
    verifyNoInteractions(preview, adapter, media);
  }

  @Test void openApiChannelScopeIsAppliedBeforeNativeSessionControl() {
    var jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("app-a")
        .claim("tenantId", "tenant-a").build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    var scope = new OpenApiVideoScope.Scope("app-a", "tenant-a", "application", "uv-test", "nvr", "5", Set.of("preview"));
    var error = assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.call(scope, () -> service.executePlatformOperation(
        "uv-test", "previewHeartbeat", Map.of("deviceId", "nvr", "channelId", "6", "stream", "uniview-live-1"))));
    assertTrue(error.getMessage().contains("视频通道"));
    verifyNoInteractions(preview, adapter, media);
  }

  @Test void snapshotWaitsForMediaAndReleasesOnlyItsOwnNativePreviewInTheInstanceTenant() {
    snapshotPreview();
    when(media.isRtpPlaybackReady("tenant-a", "selected-node", "snapshot-stream"))
        .thenReturn(false, true);
    when(media.captureStreamSnapshot("tenant-a", "selected-node",
        "rtsp://media.invalid/rtp/snapshot-stream", "rtp", "snapshot-stream"))
        .thenReturn(Map.of("snapshotUrl", "https://media.invalid/snapshot.jpg"));

    Map<String, Object> result = snapshot();

    assertEquals("https://media.invalid/snapshot.jpg", result.get("snapshotUrl"));
    var order = inOrder(preview, media);
    order.verify(preview).start(eq(instance), eq("nvr"), eq("5"), eq("sub"), anyMap(), eq("tenant-a"), eq("selected-node"));
    order.verify(media, times(2)).isRtpPlaybackReady("tenant-a", "selected-node", "snapshot-stream");
    order.verify(media).captureStreamSnapshot("tenant-a", "selected-node",
        "rtsp://media.invalid/rtp/snapshot-stream", "rtp", "snapshot-stream");
    order.verify(preview).stop(instance, "nvr", "5", snapshotStopOptions());
    verifyNoInteractions(adapter);
  }

  @Test void snapshotCaptureFailureStillReleasesTheExactPreviewAndKeepsTheCaptureError() {
    snapshotPreview();
    when(media.isRtpPlaybackReady("tenant-a", "selected-node", "snapshot-stream")).thenReturn(true);
    when(media.captureStreamSnapshot(anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new BaseException("capture failed"));
    when(preview.stop(instance, "nvr", "5", snapshotStopOptions()))
        .thenThrow(new BaseException("cleanup pending"));

    assertEquals("capture failed", assertThrows(BaseException.class, this::snapshot).getMessage());

    verify(preview).stop(instance, "nvr", "5", snapshotStopOptions());
  }

  @Test void snapshotReadinessFailureReleasesItsPreviewWithoutAttemptingCapture() {
    snapshotPreview();
    when(media.isRtpPlaybackReady("tenant-a", "selected-node", "snapshot-stream"))
        .thenThrow(new BaseException("media unavailable"));

    assertEquals("media unavailable", assertThrows(BaseException.class, this::snapshot).getMessage());

    verify(preview).stop(instance, "nvr", "5", snapshotStopOptions());
    verify(media, never()).captureStreamSnapshot(anyString(), anyString(), anyString(), anyString(), anyString());
  }

  @Test @Timeout(15)
  void snapshotReadinessTimeoutReleasesItsPreviewWithoutAttemptingCapture() {
    snapshotPreview();
    when(media.isRtpPlaybackReady("tenant-a", "selected-node", "snapshot-stream")).thenReturn(false);

    assertTrue(assertThrows(BaseException.class, this::snapshot).getMessage().contains("尚未就绪"));

    verify(preview).stop(instance, "nvr", "5", snapshotStopOptions());
    verify(media, never()).captureStreamSnapshot(anyString(), anyString(), anyString(), anyString(), anyString());
  }

  @Test void snapshotOfAnExistingFusionStreamDoesNotWaitForOrReleaseNativePreviewResources() {
    doReturn(Map.of("rtsp", "rtsp://media.invalid/fusion/existing", "stream", "fusion-stream",
        "app", "fusion", "mediaServerPrimaryId", "selected-node", "streamSource", "fusion"))
        .when(service).getStreamUrl(eq("uv-test"), eq("nvr"), eq("5"), eq("sub"), anyMap());
    when(media.captureStreamSnapshot("tenant-a", "selected-node", "rtsp://media.invalid/fusion/existing", "fusion", "fusion-stream"))
        .thenReturn(Map.of("snapshotUrl", "https://media.invalid/snapshot.jpg"));

    assertEquals("https://media.invalid/snapshot.jpg", snapshot().get("snapshotUrl"));

    verifyNoInteractions(preview);
    verify(media, never()).isRtpPlaybackReady(anyString(), anyString(), anyString());
  }

  private void snapshotPreview() {
    when(preview.start(eq(instance), eq("nvr"), eq("5"), eq("sub"), anyMap(), eq("tenant-a"), eq("selected-node")))
        .thenReturn(Map.of("rtsp", "rtsp://media.invalid/rtp/snapshot-stream", "previewId", "snapshot-preview",
            "ownerNode", "backend-a", "stream", "snapshot-stream", "app", "rtp", "streamType", "sub",
            "mediaServerPrimaryId", "selected-node", "streamSource", "gateway_uniview_sdk", "streamMode", "sdk_zlm_rtp"));
  }

  private Map<String, Object> snapshot() {
    return ReflectionTestUtils.invokeMethod(service, "captureSnapshotViaZlm", instance,
        "uv-test", "nvr", "5", "sub", Map.of("mediaServerId", "selected-node", "tenantId", "other-tenant",
            "ownerNode", "untrusted-owner", "previewId", "untrusted-preview", "stream", "untrusted-stream"));
  }

  private Map<String, String> snapshotStopOptions() {
    return Map.of("stream", "snapshot-stream", "streamType", "sub", "previewId", "snapshot-preview", "ownerNode", "backend-a");
  }
}
