package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WvpConfigurationAccessTest {
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final WvpConfigurationAccess access = new WvpConfigurationAccess(devices, channels);
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder()
      .instanceKey("wvp-one").platformType("wvp").tenantId("tenant-a").enabled(1).build();
  private VideoPlatformDevice device;
  private VideoPlatformChannel channel;
  private final Map<String, Object> parameters = Map.of("deviceId", "nvr", "channelId", "camera", "resource", "osd");

  @BeforeEach void setup() {
    device = VideoPlatformDevice.builder().id(11L).instanceKey("wvp-one")
        .deviceId("nvr").tenantId("tenant-a").enabled(1).build();
    channel = VideoPlatformChannel.builder().instanceKey("wvp-one").deviceId(11L)
        .channelId("camera").tenantId("tenant-a").build();
    when(devices.selectOne(any(VideoPlatformDevice.class))).thenReturn(device);
    when(channels.selectOne(any(VideoPlatformChannel.class))).thenReturn(channel);
  }

  @Test void resolvesDeviceAndChannelByTheAuthorizedInstanceAndParent() {
    assertEquals(parameters, access.validate(instance, "wvpConfigRead", parameters));
    verify(devices).selectOne(argThat(selector -> "wvp-one".equals(selector.getInstanceKey()) && "nvr".equals(selector.getDeviceId())));
    verify(channels).selectOne(argThat(selector -> "wvp-one".equals(selector.getInstanceKey())
        && Long.valueOf(11L).equals(selector.getDeviceId()) && "camera".equals(selector.getChannelId())));
  }

  @Test void rejectsADeviceOrChannelFromAnotherTenantAndMissingResources() {
    device.setTenantId("tenant-b");
    assertThrows(BaseException.class, () -> access.validate(instance, "wvpConfigRead", parameters));
    device.setTenantId("tenant-a");
    channel.setTenantId("tenant-b");
    assertThrows(BaseException.class, () -> access.validate(instance, "wvpConfigRead", parameters));
    channel.setTenantId("tenant-a");
    when(channels.selectOne(any(VideoPlatformChannel.class))).thenReturn(null);
    assertThrows(BaseException.class, () -> access.validate(instance, "wvpConfigRead", parameters));
  }

  @Test void rejectsCredentialOverridesAndReadOnlyUsersCannotUseTheConfigPermission() {
    assertThrows(BaseException.class, () -> access.validate(instance, "wvpConfigCapabilities",
        Map.of("deviceId", "nvr", "endpoint", "http://untrusted")));
    assertThrows(BaseException.class, () -> access.validate(instance, "wvpConfigApply",
        Map.of("deviceId", "nvr", "resource", "osd")));
    var read = cn.universal.dm.video.VideoOperationRegistry.requirePublic("wvpConfigRead");
    var apply = cn.universal.dm.video.VideoOperationRegistry.requirePublic("wvpConfigApply");
    assertTrue(read.allowed(java.util.Set.of("video:platform:query")));
    assertFalse(apply.allowed(java.util.Set.of("video:platform:query")));
    assertTrue(apply.allowed(java.util.Set.of("video:device:config")));
  }

  @Test void disabledDevicesStillExposeCapabilitiesAndTaskStatusButRejectCommands() {
    device.setEnabled(0);
    assertThrows(BaseException.class, () -> access.validate(instance, "wvpConfigRead", parameters));
    access.validate(instance, "wvpConfigCapabilities", Map.of("deviceId", "nvr"));
    access.validate(instance, "wvpConfigStatus", Map.of("deviceId", "nvr", "operationId", "job-1"));
  }

  @Test void acceptsFullRecordPlanAndRetainsTheObjectDepthLimit() {
    Map<String, Object> segment = Map.of("StartHour", 8, "StartMin", 0, "StartSec", 0,
        "StopHour", 18, "StopMin", 0, "StopSec", 0);
    Map<String, Object> configuration = Map.of("RecordEnable", 1, "StreamNumber", 0,
        "RecordScheduleSumNum", 1, "RecordSchedule", List.of(Map.of("WeekDayNum", 1,
            "TimeSegmentSumNum", 1, "TimeSegment", List.of(segment))));
    Map<String, Object> request = Map.of("deviceId", "nvr", "channelId", "camera",
        "resource", "videoRecordPlan", "configuration", configuration);
    assertEquals(request, access.validate(instance, "wvpConfigApply", request));
    Object tooDeep = Map.of("a", Map.of("b", Map.of("c", Map.of("d", Map.of("e", Map.of("f", "x"))))));
    assertThrows(BaseException.class, () -> access.validate(instance, "wvpConfigApply",
        Map.of("deviceId", "nvr", "resource", "videoRecordPlan", "configuration", tooDeep)));
  }
}
