package cn.universal.admin.video.service;

import cn.universal.admin.video.alarm.SdkVideoAlarmContextCache;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.security.utils.SecurityUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

/** 名称修改与视频/IoT 快照在同一事务中提交。 */
@Service
@RequiredArgsConstructor
public class VideoDeviceNameService {
  private final VideoPlatformDeviceMapper devices;
  private final VideoPlatformChannelMapper channels;
  private final IoTDeviceMapper iotDevices;
  private final SdkVideoAlarmContextCache alarmCache;

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void renameDevice(
      VideoPlatformInstance instance, VideoPlatformDevice device, Object name) {
    String validated = VideoDeviceNames.validateName(name);
    assertTenant(instance, device);
    Map<String, Object> config = VideoDeviceNames.configuration(device.getConfiguration());
    config.putIfAbsent(VideoDeviceNames.SOURCE_DEVICE, device.getDeviceName());
    config.put(VideoDeviceNames.DEVICE_OVERRIDE, validated);
    device.setDeviceName(validated);
    device.setConfiguration(VideoDeviceNames.encode(config));
    syncLinkedIot(instance, device, true, Map.of());
  }

  @Transactional
  public void renameChannel(
      VideoPlatformInstance instance,
      String deviceId,
      String channelId,
      Map<String, Object> updates) {
    Example query = new Example(VideoPlatformDevice.class);
    query
        .createCriteria()
        .andEqualTo("instanceKey", instance.getInstanceKey())
        .andEqualTo("deviceId", deviceId)
        .andEqualTo("tenantId", instance.getTenantId());
    query.setForUpdate(true);
    List<VideoPlatformDevice> found = devices.selectByExample(query);
    if (found.isEmpty()) throw new BaseException("设备不存在：" + deviceId);
    VideoPlatformDevice device = found.get(0);
    assertTenant(instance, device);
    Example channelQuery = new Example(VideoPlatformChannel.class);
    channelQuery
        .createCriteria()
        .andEqualTo("deviceId", device.getId())
        .andEqualTo("instanceKey", instance.getInstanceKey())
        .andEqualTo("tenantId", instance.getTenantId());
    List<VideoPlatformChannel> rows = channels.selectByExample(channelQuery);
    if (rows.stream().noneMatch(row -> Objects.equals(channelId, row.getChannelId())))
      throw new BaseException("通道不存在：" + channelId);

    if (updates == null) throw new BaseException("请填写通道名称");
    boolean reset = Boolean.TRUE.equals(updates.get("resetToDeviceName"));
    String name = reset ? "" : VideoDeviceNames.validateName(updates.get("channelName"));
    Map<String, Object> config = VideoDeviceNames.configuration(device.getConfiguration());
    Map<String, Object> overrides =
        VideoDeviceNames.object(config.get(VideoDeviceNames.CHANNEL_OVERRIDES));
    Map<String, Object> sources =
        VideoDeviceNames.object(config.get(VideoDeviceNames.SOURCE_CHANNELS));
    String physicalId = VideoDeviceNames.physicalId(channelId);
    if (reset) overrides.remove(physicalId);
    else overrides.put(physicalId, name);
    Map<String, String> renamed = new LinkedHashMap<>();
    for (VideoPlatformChannel row : rows) {
      if (!VideoDeviceNames.physicalId(row.getChannelId()).equals(physicalId)) continue;
      String id = row.getChannelId();
      String actual = VideoDeviceNames.text(sources.get(id));
      if (reset && actual.isEmpty()) throw new BaseException("尚未读取设备原名，请先同步设备通道");
      String display =
          VideoDeviceNames.validateName(reset ? actual : VideoDeviceNames.displayName(id, name));
      renamed.put(id, display);
      VideoPlatformChannel update = new VideoPlatformChannel();
      update.setId(row.getId());
      update.setChannelName(display);
      update.setUpdateId(SecurityUtils.getUnionIdQuietly());
      update.setUpdateTime(new Date());
      channels.updateByPrimaryKeySelective(update);
      row.setChannelName(display);
    }
    config.put(VideoDeviceNames.CHANNEL_OVERRIDES, overrides);
    config.put(VideoDeviceNames.SOURCE_CHANNELS, sources);
    List<Map<String, Object>> snapshot = channelList(config.get("channelList"));
    Map<String, Map<String, Object>> indexed = new LinkedHashMap<>();
    for (Map<String, Object> channel : snapshot)
      indexed.put(VideoDeviceNames.text(channel.get("channelId")), channel);
    for (VideoPlatformChannel row : rows) {
      if (!renamed.containsKey(row.getChannelId())) continue;
      Map<String, Object> channel =
          indexed.computeIfAbsent(
              row.getChannelId(),
              id -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("channelId", id);
                value.put("channelStatus", row.getChannelStatus());
                return value;
              });
      channel.put("channelName", row.getChannelName());
      channel.put("name", row.getChannelName());
    }
    config.put("channelList", new ArrayList<>(indexed.values()));
    device.setConfiguration(VideoDeviceNames.encode(config));
    device.setUpdateTime(new Date());
    device.setUpdateId(SecurityUtils.getUnionIdQuietly());
    devices.updateByPrimaryKeySelective(device);
    syncLinkedIot(instance, device, false, renamed);
    alarmCache.publishPlatformChange(instance.getInstanceKey());
  }

  private void assertTenant(VideoPlatformInstance instance, VideoPlatformDevice device) {
    if (instance.getTenantId() == null
        || instance.getTenantId().isBlank()
        || !Objects.equals(instance.getTenantId(), device.getTenantId())
        || !Objects.equals(instance.getInstanceKey(), device.getInstanceKey()))
      throw new BaseException("无权修改该视频设备");
  }

  private void syncLinkedIot(
      VideoPlatformInstance instance,
      VideoPlatformDevice device,
      boolean renameDevice,
      Map<String, String> renamed) {
    Example query = new Example(IoTDevice.class);
    query
        .createCriteria()
        .andEqualTo("deviceId", device.getDeviceId())
        .andEqualTo("tenantId", instance.getTenantId());
    query.setForUpdate(true);
    for (IoTDevice iot : iotDevices.selectByExample(query)) {
      Map<String, Object> config = VideoDeviceNames.configuration(iot.getConfiguration());
      String linkedInstance = VideoDeviceNames.text(config.get("instanceKey"));
      if (!instance.getInstanceKey().equals(linkedInstance)
          && !(linkedInstance.isEmpty() && instance.getInstanceKey().equals(iot.getProductKey())))
        continue;
      IoTDevice update = new IoTDevice();
      update.setId(iot.getId());
      if (renameDevice) update.setDeviceName(device.getDeviceName());
      if (!renamed.isEmpty()) {
        List<Map<String, Object>> snapshot = channelList(config.get("channelList"));
        Map<String, Map<String, Object>> indexed = new LinkedHashMap<>();
        for (Map<String, Object> channel : snapshot)
          indexed.put(VideoDeviceNames.text(channel.get("channelId")), channel);
        renamed.forEach(
            (id, name) -> {
              Map<String, Object> channel =
                  indexed.computeIfAbsent(id, key -> new LinkedHashMap<>(Map.of("channelId", key)));
              channel.put("channelName", name);
              if (channel.containsKey("name")) channel.put("name", name);
            });
        config.put("channelList", new ArrayList<>(indexed.values()));
        update.setConfiguration(VideoDeviceNames.encode(config));
      }
      iotDevices.updateByPrimaryKeySelective(update);
    }
  }

  private static List<Map<String, Object>> channelList(Object value) {
    List<Map<String, Object>> result = new ArrayList<>();
    if (value instanceof List<?> list)
      for (Object item : list) result.add(VideoDeviceNames.object(item));
    return result;
  }
}
