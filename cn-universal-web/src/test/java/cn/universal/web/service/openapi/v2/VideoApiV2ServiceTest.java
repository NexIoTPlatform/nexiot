package cn.universal.web.service.openapi.v2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.universal.admin.video.service.OpenApiVideoScope;
import cn.universal.admin.video.service.VideoOpenApiSessionService;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.web.config.openapi.v2.OpenApiV2ExceptionHandler;
import cn.universal.web.context.TtlAuthContextHolder;
import cn.universal.web.controller.openapi.v2.VideoApiControllerV2;
import cn.universal.web.dto.openapi.v2.VideoApiV2Models;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

class VideoApiV2ServiceTest {
  private static final String HANDLE = "0123456789abcdef0123456789abcdef";
  private final OpenApiV2Access access = mock(OpenApiV2Access.class);
  private final IoTProductDeviceService products = mock(IoTProductDeviceService.class);
  private final VideoPlatformInstanceService videos = mock(VideoPlatformInstanceService.class);
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final VideoOpenApiSessionService sessions = mock(VideoOpenApiSessionService.class);
  private final VideoApiV2Service service =
      new VideoApiV2Service(access, products, videos, devices, channels, sessions);
  private final AtomicReference<String> owner = new AtomicReference<>();
  private VideoPlatformInstance instance;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    when(access.application())
        .thenReturn(
            IoTUserApplication.builder()
                .appId("client")
                .appUniqueId("app")
                .unionId("owner")
                .tenantId("tenant")
                .build());
    when(access.tenantId(any())).thenReturn("tenant");
    when(products.getProduct("product"))
        .thenReturn(
            IoTProduct.builder()
                .productKey("product")
                .tenantId("tenant")
                .thirdPlatform("hik_isup")
                .thirdConfiguration("{\"instanceKey\":\"video\"}")
                .build());
    instance =
        VideoPlatformInstance.builder()
            .instanceKey("video")
            .tenantId("tenant")
            .platformType("hik_isup")
            .enabled(1)
            .build();
    when(videos.getByInstanceKey("video")).thenReturn(instance);
    when(devices.selectOne(any(VideoPlatformDevice.class)))
        .thenReturn(VideoPlatformDevice.builder().id(1L).tenantId("tenant").enabled(1).build());
    when(channels.selectOne(any(VideoPlatformChannel.class)))
        .thenReturn(
            VideoPlatformChannel.builder().tenantId("tenant").channelId("1").enabled(1).build());
    when(sessions.create(anyString(), any(), eq("camera"), eq("1"), eq("snapshot"), anyMap()))
        .thenAnswer(
            call -> {
              owner.set(call.getArgument(0));
              return HANDLE;
            });
    Jwt jwt =
        Jwt.withTokenValue("test")
            .header("alg", "none")
            .subject("client")
            .claim("tenantId", "tenant")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    TtlAuthContextHolder.getInstance().setContext("client");
    mvc =
        MockMvcBuilders.standaloneSetup(new VideoApiControllerV2(service))
            .setControllerAdvice(new OpenApiV2ExceptionHandler())
            .build();
  }

  @AfterEach
  void cleanup() {
    assertFalse(OpenApiVideoScope.isPresent());
    TtlAuthContextHolder.getInstance().clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void boundDeviceGrantAndExactChannelScopeRequiredBeforeCapture() {
    when(access.device("product", "camera")).thenThrow(new AccessDeniedException("unbound"));
    assertThrows(
        AccessDeniedException.class, () -> service.snapshot("product", "camera", "1", null));
    verifyNoInteractions(videos, sessions);
  }

  @Test
  void tenantAndEnabledGuardsFailBeforeMedia() {
    when(channels.selectOne(any(VideoPlatformChannel.class)))
        .thenReturn(VideoPlatformChannel.builder().tenantId("other").enabled(1).build());
    assertThrows(
        AccessDeniedException.class, () -> service.snapshot("product", "camera", "1", null));
    when(channels.selectOne(any(VideoPlatformChannel.class)))
        .thenReturn(VideoPlatformChannel.builder().tenantId("tenant").enabled(0).build());
    assertThrows(
        ResponseStatusException.class, () -> service.snapshot("product", "camera", "1", null));
    verify(videos, never()).captureSnapshot(anyString(), anyString(), anyString(), anyMap());
  }

  @Test
  void synchronousCaptureProjectsOnlyCompletedPublicImage() {
    when(videos.captureSnapshot(eq("video"), eq("camera"), eq("1"), anyMap()))
        .thenAnswer(
            call -> {
              assertTrue(OpenApiVideoScope.operationAllowed("snapshot"));
              assertTrue(OpenApiVideoScope.operationAllowed("preview"));
              assertFalse(OpenApiVideoScope.operationAllowed("deviceReboot"));
              OpenApiVideoScope.assertChannelIfPresent("video", "camera", "1");
              assertEquals(
                  Map.of("source", "platform_frame", "streamType", "sub", "failOnOssUpload", true),
                  call.getArgument(3));
              return Map.of(
                  "snapshotUrl",
                  "https://images.example/shot.jpg",
                  "gatewayToken",
                  "private",
                  "imageSource",
                  "platform_frame",
                  "fallback",
                  true,
                  "capturedAt",
                  "2026-10-17T00:00:00Z");
            });
    var response =
        service.snapshot(
            "product",
            "camera",
            "1",
            new VideoApiV2Models.SnapshotRequest("platform_frame", "sub"));
    assertEquals("completed", response.status());
    assertFalse(response.pending());
    assertNull(response.requestId());
    assertEquals("https://images.example/shot.jpg", response.imageUrl());
    assertEquals(Instant.parse("2026-10-17T00:00:00Z").toEpochMilli(), response.capturedAt());
    verifyNoInteractions(sessions);
  }

  @Test
  void asyncRequestPublishesOpaqueAppOwnedHandleAndUsesStoredNativeId() {
    when(videos.captureSnapshot(anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(
            Map.of(
                "status",
                "waiting_upload",
                "requestId",
                "native-private",
                "imageSource",
                "device_snapshot"));
    var response = service.snapshot("product", "camera", "1", null);
    assertEquals("pending", response.status());
    assertTrue(response.pending());
    assertEquals(HANDLE, response.requestId());
    assertNull(response.imageUrl());
    when(sessions.require(eq(HANDLE), anyString()))
        .thenAnswer(
            call -> {
              assertEquals(owner.get(), call.getArgument(1));
              return snapshotRecord("tenant", "camera", "1", "snapshot");
            });
    when(videos.executePlatformOperation(eq("video"), eq("snapshotStatus"), anyMap()))
        .thenAnswer(
            call -> {
              assertTrue(OpenApiVideoScope.operationAllowed("snapshotStatus"));
              assertFalse(OpenApiVideoScope.operationAllowed("snapshotCancel"));
              assertEquals(
                  Map.of("deviceId", "camera", "channelId", "1", "requestId", "native-private"),
                  call.getArgument(2));
              return Map.of(
                  "status",
                  "completed",
                  "image",
                  Map.of(
                      "url", "https://images.example/a.jpg",
                      "contentType", "image/jpeg",
                      "gatewayToken", "secret"));
            });
    instance.setEnabled(0);
    var completed = service.snapshotStatus("product", "camera", "1", HANDLE, false);
    assertEquals("completed", completed.status());
    assertEquals("device_snapshot", completed.imageSource());
    assertEquals(HANDLE, completed.requestId());
    assertEquals("image/jpeg", completed.mimeType());
  }

  @Test
  void foreignApplicationOrWrongRecordKindCannotQueryNativeTask() {
    when(sessions.require(eq(HANDLE), anyString()))
        .thenThrow(new AccessDeniedException("foreign app"));
    assertThrows(
        AccessDeniedException.class,
        () -> service.snapshotStatus("product", "camera", "1", HANDLE, false));
    when(sessions.require(eq(HANDLE), anyString()))
        .thenReturn(snapshotRecord("tenant", "camera", "1", "preview"));
    assertThrows(
        AccessDeniedException.class,
        () -> service.snapshotStatus("product", "camera", "1", HANDLE, true));
    when(sessions.require(eq(HANDLE), anyString()))
        .thenReturn(snapshotRecord("other", "camera", "1", "snapshot"));
    assertThrows(
        AccessDeniedException.class,
        () -> service.snapshotStatus("product", "camera", "1", HANDLE, false));
    verify(videos, never()).executePlatformOperation(anyString(), anyString(), anyMap());
  }

  @Test
  void cancelUsesOnlySnapshotCancelScopeAndPreservesTerminalHandle() {
    when(sessions.require(eq(HANDLE), anyString()))
        .thenReturn(snapshotRecord("tenant", "camera", "1", "snapshot"));
    when(videos.executePlatformOperation(eq("video"), eq("snapshotCancel"), anyMap()))
        .thenAnswer(
            call -> {
              assertTrue(OpenApiVideoScope.operationAllowed("snapshotCancel"));
              assertFalse(OpenApiVideoScope.operationAllowed("snapshot"));
              return Map.of("status", "cancelled");
            });
    assertEquals(
        "cancelled", service.snapshotStatus("product", "camera", "1", HANDLE, true).status());
    verify(sessions, never()).remove(anyString(), anyString());
  }

  @Test
  void failedHandlePublicationCancelsAcquiredNativeTaskAndClearsScope() {
    when(videos.captureSnapshot(anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(Map.of("status", "pending", "requestId", "native-private"));
    var failure = new IllegalStateException("redis unavailable");
    when(sessions.create(anyString(), any(), anyString(), anyString(), anyString(), anyMap()))
        .thenThrow(failure);
    when(videos.executePlatformOperation(eq("video"), eq("snapshotCancel"), anyMap()))
        .thenAnswer(
            call -> {
              assertTrue(OpenApiVideoScope.operationAllowed("snapshotCancel"));
              assertFalse(OpenApiVideoScope.operationAllowed("snapshot"));
              return Map.of("status", "cancelled");
            });
    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class, () -> service.snapshot("product", "camera", "1", null)));
    verify(videos).executePlatformOperation(eq("video"), eq("snapshotCancel"), anyMap());
  }

  @Test
  void pendingOrFailedTaskNeverReturnsAnImageAndMissingImageNeverBecomesCompleted() {
    for (String state : List.of("pending", "waiting_upload", "failed", "cancelled", "expired")) {
      var result =
          VideoApiV2Service.project(
              Map.of("status", state, "snapshotUrl", "https://images.example/old.jpg"), HANDLE);
      assertNull(result.imageUrl());
    }
    assertEquals(
        "failed", VideoApiV2Service.project(Map.of("status", "completed"), HANDLE).status());
    assertEquals(
        "failed",
        VideoApiV2Service.project(Map.of("snapshotUrl", "http://admin:secret@camera/shot"), null)
            .status());
    assertEquals(
        "failed",
        VideoApiV2Service.project(Map.of("snapshotUrl", "file:///tmp/image.jpg"), null).status());
    assertEquals("failed", VideoApiV2Service.project(Map.of("status", "pending"), null).status());
  }

  @Test
  void malformedHandleFailsBeforeAuthorizationAndChannelsExcludeForeignTenantAndSecrets() {
    assertThrows(
        ResponseStatusException.class,
        () -> service.snapshotStatus("product", "camera", "1", "native-id", false));
    verifyNoInteractions(access);
    when(channels.select(any(VideoPlatformChannel.class)))
        .thenReturn(
            List.of(
                VideoPlatformChannel.builder()
                    .tenantId("tenant")
                    .channelId("1")
                    .enabled(1)
                    .capabilities(
                        "{\"snapshot\":true,\"gatewayToken\":\"secret\",\"preview\":{\"support\":\"unknown\",\"token\":\"secret\"}}")
                    .build(),
                VideoPlatformChannel.builder().tenantId("other").channelId("2").build()));
    var rows = service.channels("product", "camera");
    assertEquals(1, rows.size());
    assertEquals(
        Map.of("snapshot", true, "preview", Map.of("support", "unknown")),
        rows.getFirst().capabilities());
  }

  @Test
  void controllerBindsVersionedRoutesAndValidatesSnapshotRequestBeforeCapture() throws Exception {
    String root = "/api/openapi/v2/video/devices/product/camera/channels/1/snapshots";
    mvc.perform(
            post(root)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cn.hutool.json.JSONUtil.toJsonStr(
                        Map.of("source", "arbitrary-url", "streamType", "main"))))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post(root)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cn.hutool.json.JSONUtil.toJsonStr(Map.of("streamType", "third"))))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(videos, sessions, access);
    when(videos.captureSnapshot(anyString(), anyString(), anyString(), anyMap()))
        .thenReturn(Map.of("snapshotUrl", "https://images.example/shot.jpg"));
    mvc.perform(post(root).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("completed"))
        .andExpect(jsonPath("$.data.imageUrl").value("https://images.example/shot.jpg"))
        .andExpect(jsonPath("$.data.pending").value(false));
  }

  @Test
  void controllerMapsUnauthorizedAndCancelStatus() throws Exception {
    String root = "/api/openapi/v2/video/devices/product/camera/channels/1/snapshots/" + HANDLE;
    when(sessions.require(eq(HANDLE), anyString()))
        .thenThrow(new AccessDeniedException("private owner"));
    mvc.perform(get(root).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.msg").value("应用、设备或通道无访问权限，或应用已停用、过期"));
    when(sessions.require(eq(HANDLE), anyString()))
        .thenReturn(snapshotRecord("tenant", "camera", "1", "snapshot"));
    when(videos.executePlatformOperation(eq("video"), eq("snapshotCancel"), anyMap()))
        .thenReturn(Map.of("status", "cancelled"));
    mvc.perform(delete(root).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("cancelled"))
        .andExpect(jsonPath("$.data.requestId").value(HANDLE));
  }

  private static Map<String, Object> snapshotRecord(
      String tenant, String device, String channel, String kind) {
    return Map.of(
        "kind",
        kind,
        "instanceKey",
        "video",
        "tenantId",
        tenant,
        "deviceId",
        device,
        "channelId",
        channel,
        "stream",
        Map.of("nativeRequestId", "native-private", "imageSource", "device_snapshot"));
  }
}
