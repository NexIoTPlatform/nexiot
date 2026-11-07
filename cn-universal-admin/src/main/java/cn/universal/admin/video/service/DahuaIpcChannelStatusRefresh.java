package cn.universal.admin.video.service;

import cn.universal.admin.video.alarm.SdkVideoAlarmContextCache;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

/** IPC 视频丢失快照只能刷新已确认身份的已有通道状态，不能修改通道目录。 */
@Service
@RequiredArgsConstructor
public class DahuaIpcChannelStatusRefresh {
  private final VideoPlatformDeviceMapper deviceMapper;
  private final VideoPlatformChannelMapper channelMapper;
  private final SdkVideoAlarmContextCache alarmContextCache;

  @Transactional(rollbackFor = Exception.class)
  public int refresh(VideoPlatformDevice expected, Map<String, Object> discovered) {
    if (!Boolean.FALSE.equals(discovered.get("channelCatalogAuthoritative"))) return 0;
    Map<String, Map<String, Object>> evidence = index(discovered.get("channelList"));
    evidence.entrySet().removeIf(entry -> !hasSignalEvidence(entry.getValue()));
    if (evidence.isEmpty()) return 0;

    Example deviceScope = new Example(VideoPlatformDevice.class);
    deviceScope.createCriteria().andEqualTo("id", expected.getId())
        .andEqualTo("instanceKey", expected.getInstanceKey())
        .andEqualTo("deviceId", expected.getDeviceId()).andEqualTo("tenantId", expected.getTenantId());
    deviceScope.setForUpdate(true);
    List<VideoPlatformDevice> devices = deviceMapper.selectByExample(deviceScope);
    if (devices.size() != 1) throw new BaseException("设备已变更，已停止状态刷新");
    VideoPlatformDevice device = devices.getFirst();
    Map<String, Object> config = VideoDeviceNames.configuration(device.getConfiguration());
    Map<String, Map<String, Object>> saved = index(config.get("channelList"));
    if (saved.isEmpty()) return 0;

    Example channelScope = new Example(VideoPlatformChannel.class);
    channelScope.createCriteria().andEqualTo("deviceId", device.getId())
        .andEqualTo("instanceKey", device.getInstanceKey()).andEqualTo("tenantId", device.getTenantId());
    List<VideoPlatformChannel> channels = channelMapper.selectByExample(channelScope);
    int updated = 0;
    for (VideoPlatformChannel channel : channels) {
      Map<String, Object> previous = saved.get(channel.getChannelId());
      Map<String, Object> current = evidence.get(channel.getChannelId());
      if (previous == null || current == null || !sameSdkIdentity(previous, current)) continue;
      boolean lost = (Boolean) current.get("videoLost");
      Map<String, Object> status = new LinkedHashMap<>();
      status.put("channelStatus", lost ? "OFF" : "ON");
      status.put("status", lost ? "OFF" : "ON");
      status.put("online", !lost);
      status.put("onlineState", lost ? "offline" : "online");
      status.put("channelStateSource", "NET_DEVSTATE_VIDEOLOST");
      status.put("connectionState", lost ? "VIDEO_LOST" : "VIDEO_PRESENT");
      status.put("videoLost", lost);
      previous.putAll(status);
      Map<String, Object> attributes = VideoDeviceNames.object(previous.get("attributes"));
      attributes.putAll(status);
      previous.put("attributes", attributes);
      Example rowScope = new Example(VideoPlatformChannel.class);
      rowScope.createCriteria().andEqualTo("id", channel.getId())
          .andEqualTo("deviceId", device.getId()).andEqualTo("instanceKey", device.getInstanceKey())
          .andEqualTo("tenantId", device.getTenantId());
      int changed = channelMapper.updateByExampleSelective(VideoPlatformChannel.builder()
          .channelStatus(lost ? "OFF" : "ON").updateTime(new Date()).build(), rowScope);
      if (changed != 1) throw new BaseException("通道已变更，已回滚本次状态刷新");
      updated++;
    }
    if (updated > 0) {
      config.put("channelList", new ArrayList<>(saved.values()));
      config.put("channelCatalogAuthoritative", false);
      int changed = deviceMapper.updateByExampleSelective(VideoPlatformDevice.builder()
          .configuration(VideoDeviceNames.encode(config)).updateTime(new Date()).build(), deviceScope);
      if (changed != 1) throw new BaseException("设备已变更，已回滚本次状态刷新");
      alarmContextCache.publishPlatformChange(device.getInstanceKey());
    }
    return updated;
  }

  private static Map<String, Map<String, Object>> index(Object value) {
    Map<String, Map<String, Object>> result = new LinkedHashMap<>();
    if (value instanceof List<?> list) for (Object item : list) {
      Map<String, Object> channel = VideoDeviceNames.object(item);
      String id = VideoDeviceNames.text(channel.get("channelId"));
      if (id.isEmpty() || result.putIfAbsent(id, channel) != null) return new LinkedHashMap<>();
    }
    return result;
  }

  private static boolean hasSignalEvidence(Map<String, Object> channel) {
    return "NET_DEVSTATE_VIDEOLOST".equals(channel.get("channelStateSource"))
        && channel.get("videoLost") instanceof Boolean && sdkChannel(channel) != null;
  }

  private static boolean sameSdkIdentity(Map<String, Object> first, Map<String, Object> second) {
    Integer sdk = sdkChannel(first);
    return sdk != null && sdk.equals(sdkChannel(second))
        && VideoDeviceNames.text(first.get("streamType")).equals(second.get("streamType"));
  }

  private static Integer sdkChannel(Map<String, Object> channel) {
    try {
      int sdk = Integer.parseInt(VideoDeviceNames.text(channel.get("sdkChannel")));
      String stream = VideoDeviceNames.text(channel.get("streamType"));
      String id = String.valueOf(sdk + 1) + ("sub".equals(stream) ? "_sub" : "");
      return sdk >= 0 && sdk < 32 && List.of("main", "sub").contains(stream)
          && id.equals(channel.get("channelId")) ? sdk : null;
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
