package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.alarm.SdkVideoAlarmContextCache;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class VideoDeviceNameServiceTest {
  final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  final IoTDeviceMapper iot = mock(IoTDeviceMapper.class);
  final SdkVideoAlarmContextCache cache = mock(SdkVideoAlarmContextCache.class);
  final VideoDeviceNameService service = new VideoDeviceNameService(devices, channels, iot, cache);
  final VideoPlatformInstance instance =
      VideoPlatformInstance.builder().instanceKey("dh").tenantId("tenant").build();
  VideoPlatformDevice device;
  List<VideoPlatformChannel> rows;
  IoTDevice linked;
  IoTDevice foreign;

  @BeforeEach
  void setup() {
    for (Class<?> entity :
        List.of(VideoPlatformDevice.class, VideoPlatformChannel.class, IoTDevice.class))
      EntityHelper.initEntityNameMap(entity, new Config());
    device =
        VideoPlatformDevice.builder()
            .id(10L)
            .deviceId("camera")
            .instanceKey("dh")
            .tenantId("tenant")
            .deviceName("大华球机")
            .configuration(
                """
                {"fusion":{"keep":true},"sourceChannelNames":{"1":"露台","1_sub":"露台 Sub"},
                 "channelList":[{"channelId":"1","channelName":"露台","rtspUrl":"keep-url"},
                 {"channelId":"1_sub","channelName":"露台 Sub"}]}
                """)
            .build();
    rows =
        List.of(
            VideoPlatformChannel.builder()
                .id(21L)
                .deviceId(10L)
                .channelId("1")
                .channelName("露台")
                .build(),
            VideoPlatformChannel.builder()
                .id(22L)
                .deviceId(10L)
                .channelId("1_sub")
                .channelName("露台 Sub")
                .build());
    linked = new IoTDevice();
    linked.setId(30L);
    linked.setProductKey("custom-product");
    linked.setConfiguration(
        "{\"instanceKey\":\"dh\",\"local\":true,\"channelList\":[{\"channelId\":\"1\",\"channelName\":\"露台\",\"custom\":9}]}");
    foreign = new IoTDevice();
    foreign.setId(31L);
    foreign.setProductKey("dh");
    foreign.setConfiguration("{\"instanceKey\":\"another-instance\"}");
    when(devices.selectByExample(any())).thenReturn(List.of(device));
    when(channels.selectByExample(any())).thenReturn(rows);
    when(iot.selectByExample(any())).thenReturn(List.of(linked, foreign));
    doAnswer(
            call -> {
              IoTDevice update = call.getArgument(0);
              if (update.getConfiguration() != null)
                linked.setConfiguration(update.getConfiguration());
              return 1;
            })
        .when(iot)
        .updateByPrimaryKeySelective(any());
  }

  @Test
  void renameAndResetUpdateAllSnapshotsWithoutChangingIdentityOrFusion() {
    service.renameChannel(instance, "camera", "1_sub", Map.of("channelName", "平台门口"));
    assertEquals("平台门口", rows.getFirst().getChannelName());
    assertEquals("平台门口 Sub", rows.getLast().getChannelName());
    var config = VideoDeviceNames.configuration(device.getConfiguration());
    assertEquals(Map.of("keep", true), config.get("fusion"));
    assertTrue(device.getConfiguration().contains("keep-url"));
    assertTrue(linked.getConfiguration().contains("平台门口"));
    assertTrue(linked.getConfiguration().contains("\"custom\":9"));
    assertTrue(linked.getConfiguration().contains("\"local\":true"));
    var writes = ArgumentCaptor.forClass(VideoPlatformChannel.class);
    verify(channels, times(2)).updateByPrimaryKeySelective(writes.capture());
    assertEquals(
        List.of(21L, 22L),
        writes.getAllValues().stream().map(VideoPlatformChannel::getId).toList());
    assertTrue(
        writes.getAllValues().stream()
            .allMatch(row -> row.getStreamConfig() == null && row.getChannelId() == null));
    var iotWrite = ArgumentCaptor.forClass(IoTDevice.class);
    verify(iot).updateByPrimaryKeySelective(iotWrite.capture());
    assertEquals(30L, iotWrite.getValue().getId());
    var lock = ArgumentCaptor.forClass(Example.class);
    verify(devices).selectByExample(lock.capture());
    assertTrue(lock.getValue().isForUpdate());
    assertTrue(
        lock.getValue().getOredCriteria().getFirst().getAllCriteria().stream()
            .anyMatch(
                c -> c.getCondition().startsWith("tenant_id") && "tenant".equals(c.getValue())));
    service.renameChannel(instance, "camera", "1", Map.of("resetToDeviceName", true));
    assertEquals("露台", rows.getFirst().getChannelName());
    assertEquals("露台 Sub", rows.getLast().getChannelName());
    assertTrue(
        VideoDeviceNames.object(
                VideoDeviceNames.configuration(device.getConfiguration())
                    .get(VideoDeviceNames.CHANNEL_OVERRIDES))
            .isEmpty());
    verify(cache, times(2)).publishPlatformChange("dh");
  }

  @Test
  void deviceRenameAlsoUpdatesCustomProductLinkAndStoresAlias() {
    service.renameDevice(instance, device, "大华新名称");
    assertEquals("大华新名称", device.getDeviceName());
    assertEquals(
        "大华新名称",
        VideoDeviceNames.configuration(device.getConfiguration())
            .get(VideoDeviceNames.DEVICE_OVERRIDE));
    var write = ArgumentCaptor.forClass(IoTDevice.class);
    verify(iot).updateByPrimaryKeySelective(write.capture());
    assertEquals(30L, write.getValue().getId());
    assertEquals("大华新名称", write.getValue().getDeviceName());
  }

  @Test
  void rejectsWrongTenantMissingChannelAndResetWithoutObservedName() {
    device.setTenantId("other-tenant");
    assertThrows(
        BaseException.class,
        () -> service.renameChannel(instance, "camera", "1", Map.of("channelName", "test")));
    device.setTenantId("tenant");
    assertThrows(
        BaseException.class,
        () -> service.renameChannel(instance, "camera", "missing", Map.of("channelName", "test")));
    device.setConfiguration("{}");
    assertThrows(
        BaseException.class,
        () -> service.renameChannel(instance, "camera", "1", Map.of("resetToDeviceName", true)));
    verify(channels, never()).updateByPrimaryKeySelective(any());
    verify(iot, never()).updateByPrimaryKeySelective(any());
    verifyNoInteractions(cache);
  }
}
