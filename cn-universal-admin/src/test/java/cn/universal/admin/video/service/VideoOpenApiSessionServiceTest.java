package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.access.AccessDeniedException;

class VideoOpenApiSessionServiceTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  @SuppressWarnings("unchecked") private final ValueOperations<String, String> values = mock(ValueOperations.class);
  private final VideoPlatformInstanceService instances = mock(VideoPlatformInstanceService.class);
  private final VideoIsupPreviewService isup = mock(VideoIsupPreviewService.class);
  private final VideoDahuaPreviewService dahua = mock(VideoDahuaPreviewService.class);
  private final VideoUniviewPreviewService uniview = mock(VideoUniviewPreviewService.class);
  private final VideoSdkPlaybackService playback = mock(VideoSdkPlaybackService.class);
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final Map<String, String> cache = new HashMap<>();
  private VideoOpenApiSessionService service;
  private VideoPlatformInstance instance;

  @BeforeEach void setup() {
    when(redis.opsForValue()).thenReturn(values);
    when(values.get(anyString())).thenAnswer(call -> cache.get(call.getArgument(0)));
    doAnswer(call -> { cache.put(call.getArgument(0), call.getArgument(1)); return null; })
        .when(values).set(anyString(), anyString(), any(Duration.class));
    service = new VideoOpenApiSessionService(redis, new ObjectMapper(), instances, isup, dahua, uniview, playback, devices, channels);
    instance = VideoPlatformInstance.builder().instanceKey("instance").tenantId("tenant").platformType("hik_isup").enabled(1).build();
    when(instances.getByInstanceKey("instance")).thenReturn(instance);
    when(devices.selectOne(any(VideoPlatformDevice.class))).thenReturn(VideoPlatformDevice.builder()
        .id(12L).tenantId("tenant").deviceId("camera").enabled(1).build());
    when(channels.selectOne(any(VideoPlatformChannel.class))).thenReturn(VideoPlatformChannel.builder()
        .tenantId("tenant").channelId("1").enabled(1).build());
    when(dahua.heartbeat(any(), anyString(), anyString(), anyString(), any())).thenReturn(Map.of("active", true, "status", "streaming"));
    when(uniview.heartbeat(any(), anyString(), anyString(), anyString(), any())).thenReturn(Map.of("active", true, "status", "streaming"));
  }

  @Test void handleCannotControlAnotherApplicationOrChannel() {
    String id = service.create("application-and-channel-a", instance, "camera", "1", "preview", Map.of("zlmStream", "owned"));
    assertThrows(AccessDeniedException.class, () -> service.require(id, "application-and-channel-b"));
    verifyNoInteractions(isup, dahua, uniview, playback);
  }

  @Test void previewHeartbeatUsesOnlyStoredIdentity() {
    String id = service.create("owner", instance, "camera", "1", "preview", Map.of("zlmStream", "owned-stream", "ownerNode", "node"));
    service.heartbeat(id);
    verify(isup).assertOwner("node");
    verify(isup).heartbeat(instance, "camera", "1", "owned-stream");
  }

  @Test void univiewPreviewHeartbeatUsesStoredIdentityAndExactSession() {
    instance.setPlatformType("uniview_sdk");
    String id = service.create("owner", instance, "camera", "1", "preview",
        Map.of("zlmStream", "owned-stream", "previewId", "native-preview", "ownerNode", "node"));
    service.heartbeat(id);
    var order = inOrder(uniview);
    order.verify(uniview).assertOwner("node");
    order.verify(uniview).heartbeat(instance, "camera", "1", "owned-stream", "native-preview");
    verifyNoInteractions(isup, dahua, playback);
  }

  @Test void univiewPreviewRejectsAnotherBackendOwnerBeforeRenewing() {
    instance.setPlatformType("uniview_sdk");
    String id = service.create("owner", instance, "camera", "1", "preview",
        Map.of("stream", "owned-stream", "previewId", "native-preview", "ownerNode", "other-node"));
    doThrow(new BaseException("SESSION_OWNER_MISMATCH")).when(uniview).assertOwner("other-node");
    assertThrows(BaseException.class, () -> service.heartbeat(id));
    verify(uniview, never()).heartbeat(any(), anyString(), anyString(), anyString(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"dahua_sdk", "uniview_sdk"})
  void nativePreviewHeartbeatRejectsInactiveAndTerminalSessions(String platformType) {
    instance.setPlatformType(platformType);
    String id = service.create("owner", instance, "camera", "1", "preview",
        Map.of("zlmStream", "owned-stream", "previewId", "native-preview", "ownerNode", "node"));
    for (Map<String, Object> state : java.util.List.<Map<String, Object>>of(
        Map.of("active", false, "status", "streaming"),
        Map.of("active", true, "status", "ended"),
        Map.of("active", true, "status", "failed"),
        Map.of("active", true, "status", "stopped"),
        Map.of("active", true, "status", "expired"),
        Map.of("active", true, "status", "EXPIRED"))) {
      when(dahua.heartbeat(any(), anyString(), anyString(), anyString(), any())).thenReturn(state);
      when(uniview.heartbeat(any(), anyString(), anyString(), anyString(), any())).thenReturn(state);
      assertThrows(BaseException.class, () -> service.heartbeat(id), state.toString());
    }
    verifyNoInteractions(isup, playback);
  }

  @ParameterizedTest
  @ValueSource(strings = {"dahua_sdk", "uniview_sdk"})
  void nativePreviewHeartbeatPreservesActiveSessions(String platformType) {
    instance.setPlatformType(platformType);
    String id = service.create("owner", instance, "camera", "1", "preview",
        Map.of("zlmStream", "owned-stream", "previewId", "native-preview", "ownerNode", "node"));
    assertDoesNotThrow(() -> service.heartbeat(id));
    verifyNoInteractions(isup, playback);
  }

  @ParameterizedTest
  @ValueSource(strings = {"hik_isup", "uniview_sdk"})
  void disabledOrReassignedResourcesCannotRenewPublicTicketSession(String platformType) {
    instance.setPlatformType(platformType);
    String id = service.create("owner", instance, "camera", "1", "preview",
        Map.of("zlmStream", "owned", "previewId", "native-preview", "ownerNode", "node"));
    when(channels.selectOne(any(VideoPlatformChannel.class))).thenReturn(VideoPlatformChannel.builder().tenantId("other").build());
    assertThrows(BaseException.class, () -> service.heartbeat(id));
    verifyNoInteractions(isup, dahua, uniview, playback);
    instance.setTenantId("other");
    assertThrows(BaseException.class, () -> service.heartbeat(id));
    instance.setTenantId("tenant");
    instance.setEnabled(0);
    assertThrows(BaseException.class, () -> service.heartbeat(id));
    verifyNoInteractions(isup, dahua, uniview, playback);
  }

  @Test void playbackHeartbeatKeepsExactNativeSessionAndEndsWithIt() {
    String id = service.create("owner", instance, "camera", "1", "playback", Map.of("playbackId", "native-id", "ownerNode", "node"));
    when(playback.control(eq(instance), eq("playbackStreamStatus"), anyMap())).thenAnswer(call -> {
      assertEquals(Map.of("playbackId", "native-id", "ownerNode", "node", "deviceId", "camera", "channelId", "1"), call.getArgument(2));
      return Map.of("status", "ended");
    });
    assertThrows(BaseException.class, () -> service.heartbeat(id));
  }
}
