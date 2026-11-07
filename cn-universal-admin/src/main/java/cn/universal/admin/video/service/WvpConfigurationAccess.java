package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoOperationRegistry;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves every GB configuration selector inside the already-authorized instance. */
@Service
@RequiredArgsConstructor
public class WvpConfigurationAccess {
  private static final Set<String> OPERATIONS =
      Set.of("wvpConfigCapabilities", "wvpConfigRead", "wvpConfigApply", "wvpConfigStatus");
  private final VideoPlatformDeviceMapper devices;
  private final VideoPlatformChannelMapper channels;

  public static boolean handles(String operation) {
    return OPERATIONS.contains(operation);
  }

  public Map<String, Object> validate(
      VideoPlatformInstance instance, String operation, Map<String, Object> parameters) {
    if (instance == null || !"wvp".equals(instance.getPlatformType()))
      throw new BaseException("此接口仅适用于 GB28181 平台");
    Map<String, Object> params = VideoOperationRegistry.validate(
        VideoOperationRegistry.requirePublic(operation), parameters);
    String deviceId = Objects.toString(params.get("deviceId"), "");
    String channelId = Objects.toString(params.get("channelId"), "");
    VideoPlatformDevice device = devices.selectOne(VideoPlatformDevice.builder()
        .instanceKey(instance.getInstanceKey()).deviceId(deviceId).build());
    if (device == null || !Objects.equals(instance.getTenantId(), device.getTenantId()))
      throw new BaseException("设备不存在或无权访问");
    OpenApiVideoScope.assertResourceTenantIfPresent(device.getTenantId());
    if (!channelId.isBlank()) {
      VideoPlatformChannel channel = channels.selectOne(VideoPlatformChannel.builder()
          .instanceKey(instance.getInstanceKey()).deviceId(device.getId()).channelId(channelId).build());
      if (channel == null || !Objects.equals(instance.getTenantId(), channel.getTenantId()))
        throw new BaseException("通道不存在或无权访问");
      OpenApiVideoScope.assertResourceTenantIfPresent(channel.getTenantId());
    }
    if (Set.of("wvpConfigRead", "wvpConfigApply").contains(operation)
        && (!Integer.valueOf(1).equals(instance.getEnabled())
            || Integer.valueOf(0).equals(device.getEnabled())))
      throw new BaseException("实例或设备已停用");
    return params;
  }
}
