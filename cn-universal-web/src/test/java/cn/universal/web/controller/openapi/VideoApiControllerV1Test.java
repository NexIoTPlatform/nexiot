package cn.universal.web.controller.openapi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.universal.admin.system.service.impl.UserApplicationService;
import cn.universal.admin.video.service.*;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.video.VideoMediaUrlResolver;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.query.IoTAPIQuery;
import cn.universal.web.context.TtlAuthContextHolder;
import cn.universal.web.controller.openapi.video.VideoApiControllerV1;
import cn.universal.web.config.openapi.video.VideoApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VideoApiControllerV1Test {
  private static final String ROOT = "/api/openapi/v1/video/devices/product/camera/channels/1";
  private final VideoApiControllerV1 controller = new VideoApiControllerV1();
  private final IoTDeviceService devices = mock(IoTDeviceService.class);
  private final IoTProductDeviceService products = mock(IoTProductDeviceService.class);
  private final VideoPlatformInstanceService videos = mock(VideoPlatformInstanceService.class);
  private final VideoPlatformDeviceMapper videoDevices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final VideoOpenApiSessionService sessions = mock(VideoOpenApiSessionService.class);
  private final VideoPlaybackTicketService tickets = mock(VideoPlaybackTicketService.class);
  private final UserApplicationService applications = mock(UserApplicationService.class);
  private MockMvc mvc;
  private VideoPlatformInstance instance;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(controller, "iotDeviceService", devices);
    ReflectionTestUtils.setField(controller, "products", products);
    ReflectionTestUtils.setField(controller, "videos", videos);
    ReflectionTestUtils.setField(controller, "videoDevices", videoDevices);
    ReflectionTestUtils.setField(controller, "channels", channels);
    ReflectionTestUtils.setField(controller, "sessions", sessions);
    ReflectionTestUtils.setField(controller, "tickets", tickets);
    ReflectionTestUtils.setField(controller, "userApplicationService", applications);
    ReflectionTestUtils.setField(controller, "mediaUrls", new VideoMediaUrlResolver("https://media.example/zlm", ""));
    Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("app-client")
        .claim("tenantId", "tenant").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    TtlAuthContextHolder.getInstance().setContext("app-client");
    when(applications.getIotUserByAppid("app-client")).thenReturn(IoTUserApplication.builder()
        .appId("app-client").appUniqueId("app-owned").unionId("owner").tenantId("tenant").appStatus(0).build());
    when(devices.selectDevCount(any())).thenReturn(true);
    when(devices.selectDevInstanceBO("product", "camera")).thenReturn(IoTDeviceDTO.builder()
        .productKey("product").deviceId("camera").tenantId("tenant").build());
    when(products.getProduct("product")).thenReturn(IoTProduct.builder().productKey("product")
        .tenantId("tenant").thirdPlatform("hik_isup").thirdConfiguration("{\"instanceKey\":\"instance\"}").build());
    instance = VideoPlatformInstance.builder().instanceKey("instance").tenantId("tenant")
        .platformType("hik_isup").enabled(1).build();
    when(videos.getByInstanceKey("instance")).thenReturn(instance);
    when(videoDevices.selectOne(any(VideoPlatformDevice.class))).thenReturn(VideoPlatformDevice.builder()
        .id(12L).deviceId("camera").instanceKey("instance").tenantId("tenant").enabled(1).build());
    when(channels.selectOne(any(VideoPlatformChannel.class))).thenReturn(VideoPlatformChannel.builder()
        .channelId("1").deviceId(12L).instanceKey("instance").tenantId("tenant").enabled(1).build());
    when(sessions.create(anyString(), any(), eq("camera"), eq("1"), anyString(), anyMap())).thenReturn("session");
    when(tickets.create(anyMap(), anyString(), anyInt())).thenReturn(Map.of("token", "ticket", "playerUrl", "https://ui.example/#/video-player/ticket"));
    // These assertions exercise the JSON contract; XML remains available to existing V1 clients.
    mvc = MockMvcBuilders.standaloneSetup(controller)
        .defaultRequest(get("/").accept(MediaType.APPLICATION_JSON))
        .setControllerAdvice(new VideoApiExceptionHandler()).build();
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    TtlAuthContextHolder.getInstance().clear();
    assertNull(OpenApiVideoScope.operationAllowed("preview"));
  }

  @Test
  void previewUsesApplicationAndTenantGrantAndOnlyReturnsPublicPlaybackData() throws Exception {
    when(videos.getStreamUrl(eq("instance"), eq("camera"), eq("1"), eq("main"))).thenAnswer(call -> {
      assertTrue(OpenApiVideoScope.operationAllowed("preview"));
      OpenApiVideoScope.assertChannelIfPresent("instance", "camera", "1");
      assertFalse(OpenApiVideoScope.operationAllowed("deviceReboot"));
      return Map.of("ws_flv", "ws://192.168.0.1/live/owned.flv?sign=x", "originRtsp", "rtsp://admin:secret@camera/1",
          "gatewayToken", "management-secret", "ownerNode", "private-node");
    });
    String json = mvc.perform(post(ROOT + "/preview").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.sessionId").value("session"))
        .andExpect(jsonPath("$.data.urls.wss_flv").value("wss://media.example/zlm/live/owned.flv?sign=x"))
        .andExpect(jsonPath("$.data.playerUrl").exists()).andReturn().getResponse().getContentAsString();
    assertFalse(json.contains("secret")); assertFalse(json.contains("private-node"));
    ArgumentCaptor<IoTAPIQuery> query = ArgumentCaptor.forClass(IoTAPIQuery.class);
    verify(devices).selectDevCount(query.capture());
    assertEquals("app-owned", query.getValue().getApplicationId());
    assertEquals("owner", query.getValue().getIotUnionId());
    assertEquals("product", query.getValue().getProductKey());
    assertEquals("camera", query.getValue().getDeviceId());
  }

  @Test
  void productOwnerCannotBypassApplicationDeviceGrant() throws Exception {
    when(devices.selectDevCount(any())).thenReturn(false);
    mvc.perform(post(ROOT + "/preview")).andExpect(status().isForbidden());
    verify(videos, never()).getStreamUrl(anyString(), anyString(), anyString(), anyString());
    verifyNoInteractions(sessions, tickets);
  }

  @Test
  void crossTenantChannelNeverReachesMedia() throws Exception {
    when(channels.selectOne(any(VideoPlatformChannel.class))).thenReturn(VideoPlatformChannel.builder().tenantId("other").build());
    mvc.perform(post(ROOT + "/preview")).andExpect(status().isForbidden());
    verify(videos, never()).getStreamUrl(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void malformedRangeAndOversizedTicketFailBeforeCreatingMedia() throws Exception {
    mvc.perform(post(ROOT + "/playback").contentType(MediaType.APPLICATION_JSON)
        .content("{\"startTime\":3000,\"endTime\":1000}")).andExpect(status().isBadRequest());
    mvc.perform(post(ROOT + "/preview").contentType(MediaType.APPLICATION_JSON)
        .content("{\"expiresInSeconds\":999999}")).andExpect(status().isBadRequest());
    verifyNoInteractions(sessions, tickets);
  }

  @Test
  void sdkPlaybackUsesOwnedStreamingService() throws Exception {
    when(videos.executePlatformOperation(eq("instance"), eq("playbackStreamStart"), anyMap()))
        .thenReturn(Map.of("playbackId", "native-id", "wss_flv", "wss://media.example/zlm/playback/owned.flv"));
    mvc.perform(post(ROOT + "/playback").contentType(MediaType.APPLICATION_JSON)
        .content("{\"startTime\":1000,\"endTime\":61000}")).andExpect(status().isOk());
    verify(videos, never()).getPlaybackUrl(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyMap());
  }

  @Test
  void failedStreamDoesNotIssuePlayerTicketOrExposeNativeErrors() throws Exception {
    when(videos.getStreamUrl(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Map.of("available", "false", "originRtsp", "rtsp://admin:secret@camera/1", "message", "native secret"));
    mvc.perform(post(ROOT + "/preview")).andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.msg").value("设备暂未返回浏览器可播放的视频流"));
    verifyNoInteractions(sessions, tickets);
  }

  @Test
  void anotherApplicationCannotControlSession() throws Exception {
    when(sessions.require(eq("foreign-session"), anyString())).thenThrow(new org.springframework.security.access.AccessDeniedException("foreign"));
    mvc.perform(post(ROOT + "/sessions/foreign-session/stop")).andExpect(status().isForbidden());
    verify(videos, never()).executePlatformOperation(anyString(), anyString(), anyMap());
  }

  @Test
  void sessionControlUsesStoredNativeIdentity() throws Exception {
    when(sessions.require(eq("owned-session"), anyString())).thenReturn(Map.of("kind", "playback", "stream",
        Map.of("playbackId", "stored-native-id", "ownerNode", "stored-node")));
    when(videos.executePlatformOperation(eq("instance"), eq("playbackStreamStop"), anyMap())).thenAnswer(call -> {
      Map<?, ?> input = call.getArgument(2);
      assertEquals("stored-native-id", input.get("playbackId"));
      assertEquals("stored-node", input.get("ownerNode"));
      assertEquals("camera", input.get("deviceId")); assertEquals("1", input.get("channelId"));
      return Map.of("status", "stopped");
    });
    mvc.perform(post(ROOT + "/sessions/owned-session/stop")).andExpect(status().isOk());
    verify(sessions).remove(eq("owned-session"), anyString());
  }

  @Test
  void completedAsyncSnapshotProjectsImageButNotInternalMetadata() throws Exception {
    when(videos.executePlatformOperation(eq("instance"), eq("snapshotStatus"), anyMap())).thenReturn(Map.of(
        "status", "completed", "imageSource", "device_snapshot", "image", Map.of(
            "snapshotUrl", "https://images.example/shot.jpg", "gatewayToken", "secret")));
    mvc.perform(get(ROOT + "/snapshots/request-id")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.snapshotUrl").value("https://images.example/shot.jpg"))
        .andExpect(jsonPath("$.data.imageSource").value("device_snapshot"))
        .andExpect(jsonPath("$.data.gatewayToken").doesNotExist());
  }

  @Test
  void wvpPreviewStopDoesNotCloseSharedSipStream() throws Exception {
    instance.setPlatformType("wvp");
    when(products.getProduct("product")).thenReturn(IoTProduct.builder().productKey("product")
        .tenantId("tenant").thirdPlatform("wvp").thirdConfiguration("{\"instanceKey\":\"instance\"}").build());
    when(sessions.require(eq("owned-session"), anyString())).thenReturn(Map.of("kind", "preview", "stream", Map.of("stream", "shared-sip-stream")));
    mvc.perform(post(ROOT + "/sessions/owned-session/stop")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.releaseMode").value("viewer_only"));
    verify(videos, never()).stopPreview(anyString(), anyString(), anyString(), anyMap());
    verify(sessions).remove(eq("owned-session"), anyString());
  }

  @Test
  void failedStopPreservesHandleForRetry() throws Exception {
    when(sessions.require(eq("owned-session"), anyString())).thenReturn(Map.of("kind", "preview", "stream", Map.of("zlmStream", "owned")));
    when(videos.stopPreview(anyString(), anyString(), anyString(), anyMap())).thenReturn(Map.of("stopped", false, "skipped", true));
    mvc.perform(post(ROOT + "/sessions/owned-session/stop")).andExpect(status().isConflict());
    verify(sessions, never()).remove(anyString(), anyString());
  }

  @Test
  void publicationFailuresReleaseOnlyAcquiredPreviewWithOriginalIdentityAndLimitedScope() throws Exception {
    when(videos.getStreamUrl(eq("instance"), eq("camera"), eq("1"), eq("main")))
        .thenReturn(Map.of("ws_flv", "ws://media/rtp/new-preview.flv", "zlmStream", "new-preview",
            "ownerNode", "acquiring-node", "mediaServerPrimaryId", "owned-media-node"));
    when(videos.stopPreview(eq("instance"), eq("camera"), eq("1"), anyMap())).thenAnswer(call -> {
      assertTrue(OpenApiVideoScope.operationAllowed("stopPreview"));
      assertFalse(OpenApiVideoScope.operationAllowed("preview"));
      assertFalse(OpenApiVideoScope.operationAllowed("deviceReboot"));
      assertTrue(OpenApiVideoScope.assertInstanceIfPresent(instance));
      OpenApiVideoScope.assertChannelIfPresent("instance", "camera", "1");
      assertEquals("app-client", SecurityContextHolder.getContext().getAuthentication().getName());
      Map<?, ?> options = call.getArgument(3);
      assertEquals("new-preview", options.get("zlmStream"));
      assertEquals("acquiring-node", options.get("ownerNode"));
      assertEquals("owned-media-node", options.get("mediaServerPrimaryId"));
      return Map.of("stopped", true);
    });
    for (String phase : List.of("session-create", "ticket-create", "session-update")) {
      reset(sessions, tickets);
      clearInvocations(videos);
      var failure = new org.springframework.data.redis.RedisConnectionFailureException("Redis unavailable");
      when(sessions.create(anyString(), any(), eq("camera"), eq("1"), eq("preview"), anyMap()))
          .thenAnswer(call -> { if ("session-create".equals(phase)) throw failure; return "session"; });
      when(tickets.create(anyMap(), anyString(), anyInt()))
          .thenAnswer(call -> { if ("ticket-create".equals(phase)) throw failure;
            return Map.of("token", "new-ticket", "playerUrl", "https://ui.example/video-player.html#new-ticket"); });
      doAnswer(call -> { if ("session-update".equals(phase)) throw failure; return null; })
          .when(sessions).update(eq("session"), anyString(), anyMap());
      if ("session-update".equals(phase))
        doThrow(new org.springframework.data.redis.RedisConnectionFailureException("Cleanup unavailable"))
            .when(tickets).revoke(eq("new-ticket"), anyString());
      Exception resolved = mvc.perform(post(ROOT + "/preview")).andExpect(status().isBadGateway())
          .andReturn().getResolvedException();
      assertSame(failure, resolved, "Cleanup must preserve the original publication failure");
      assertEquals("session-update".equals(phase) ? 1 : 0, failure.getSuppressed().length);
      verify(videos).stopPreview(eq("instance"), eq("camera"), eq("1"), anyMap());
      if ("session-create".equals(phase)) verify(sessions, never()).remove(anyString(), anyString());
      else verify(sessions).remove(eq("session"), anyString());
      if ("session-update".equals(phase)) verify(tickets).revoke(eq("new-ticket"), anyString());
      else verify(tickets, never()).revoke(anyString(), anyString());
      assertNull(OpenApiVideoScope.operationAllowed("stopPreview"));
    }
  }

  @Test
  void failedWvpPublicationProtectsSharedPreviewAndReleasesOnlyAcquiredPlayback() throws Exception {
    instance.setPlatformType("wvp");
    when(products.getProduct("product")).thenReturn(IoTProduct.builder().productKey("product")
        .tenantId("tenant").thirdPlatform("wvp").thirdConfiguration("{\"instanceKey\":\"instance\"}").build());
    when(videos.getStreamUrl(eq("instance"), eq("camera"), eq("1"), eq("main")))
        .thenReturn(Map.of("ws_flv", "ws://media/rtp/shared.flv", "stream", "shared-sip-stream"));
    when(tickets.create(anyMap(), anyString(), anyInt()))
        .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Redis unavailable"));
    mvc.perform(post(ROOT + "/preview")).andExpect(status().isBadGateway());
    verify(videos, never()).stopPreview(anyString(), anyString(), anyString(), anyMap());
    verify(videos, never()).controlPlayback(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    verify(sessions).remove(eq("session"), anyString());
    when(videos.getPlaybackUrl(eq("instance"), eq("camera"), eq("1"), eq(1000L), eq(61000L), anyMap()))
        .thenReturn(Map.of("ws_flv", "ws://media/rtp/owned-replay.flv", "stream", "owned-replay"));
    doAnswer(call -> {
      assertTrue(OpenApiVideoScope.operationAllowed("playbackStreamStop"));
      assertTrue(OpenApiVideoScope.operationAllowed("playbackStreamStatus"));
      assertFalse(OpenApiVideoScope.operationAllowed("stopPreview"));
      assertTrue(OpenApiVideoScope.assertInstanceIfPresent(instance));
      OpenApiVideoScope.assertChannelIfPresent("instance", "camera", "1");
      return null;
    }).when(videos).controlPlayback(eq("instance"), eq("camera"), eq("1"), eq("owned-replay"), eq("stop"), isNull());
    mvc.perform(post(ROOT + "/playback").contentType(MediaType.APPLICATION_JSON)
        .content("{\"startTime\":1000,\"endTime\":61000}")).andExpect(status().isBadGateway());
    verify(videos).controlPlayback(eq("instance"), eq("camera"), eq("1"), eq("owned-replay"), eq("stop"), isNull());
    verify(videos, never()).stopPreview(anyString(), anyString(), anyString(), anyMap());
    verify(sessions, times(2)).remove(eq("session"), anyString());
  }

  @Test
  void seekPublicationFailureStopsOnlyReplacementPlaybackAndDiscardsItsTicket() throws Exception {
    when(sessions.require(eq("owned-session"), anyString())).thenReturn(Map.of("kind", "playback", "stream",
        Map.of("playbackId", "old-native-id", "ownerNode", "old-node", "openApiPlayerToken", "old-ticket")));
    when(videos.executePlatformOperation(eq("instance"), eq("playbackStreamSeek"), anyMap()))
        .thenReturn(Map.of("previousPlaybackId", "old-native-id", "playbackId", "new-native-id",
            "ownerNode", "new-node", "wss_flv", "wss://media/rtp/new-playback.flv"));
    doThrow(new org.springframework.data.redis.RedisConnectionFailureException("Redis unavailable"))
        .when(sessions).update(eq("owned-session"), anyString(), anyMap());
    when(videos.executePlatformOperation(eq("instance"), eq("playbackStreamStop"), anyMap())).thenAnswer(call -> {
      assertTrue(OpenApiVideoScope.operationAllowed("playbackStreamStop"));
      assertFalse(OpenApiVideoScope.operationAllowed("playbackStreamSeek"));
      assertTrue(OpenApiVideoScope.assertInstanceIfPresent(instance));
      OpenApiVideoScope.assertChannelIfPresent("instance", "camera", "1");
      Map<?, ?> input = call.getArgument(2);
      assertEquals("new-native-id", input.get("playbackId"));
      assertEquals("new-node", input.get("ownerNode"));
      assertEquals("camera", input.get("deviceId"));
      assertEquals("1", input.get("channelId"));
      return Map.of("status", "stopped");
    });
    mvc.perform(post(ROOT + "/sessions/owned-session/seek").contentType(MediaType.APPLICATION_JSON)
        .content("{\"seekTime\":10000}")).andExpect(status().isBadGateway());
    verify(videos).executePlatformOperation(eq("instance"), eq("playbackStreamStop"), anyMap());
    verify(tickets).revoke(eq("ticket"), anyString());
    verify(sessions).remove(eq("owned-session"), anyString());
  }
}
