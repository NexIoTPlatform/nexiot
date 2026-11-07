/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 天地伟业直连SDK设备同步处理器
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.sync;

import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.admin.video.service.VideoDeviceImportService;
import cn.universal.admin.video.service.VideoDeviceNames;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 天地伟业直连SDK设备同步处理器。 */
@Component
public class TiandySdkDeviceSyncHandler extends DirectSdkDeviceSyncHandler {

  @jakarta.annotation.Resource private VideoDeviceImportService importService;
  private final ThreadLocal<Boolean> offlineCatalogSync = ThreadLocal.withInitial(() -> false);

  public TiandySdkDeviceSyncHandler(VideoPlatformAdapterRegistry adapterRegistry) {
    super(adapterRegistry);
  }

  @Override
  public String getPlatformType() {
    return "tiandy_sdk";
  }

  @Override
  protected String getLogPrefix() {
    return "天地伟业SDK";
  }

  @Override
  protected String getDefaultManufacturer() {
    return "Tiandy";
  }

  @Override protected boolean preserveLocalConfiguration() { return true; }

  @Override protected boolean shouldResetBeforeSync(Map<String, Object> filters) { return false; }

  @Override protected boolean hasAuthoritativeChannelCatalog(Map<String, Object> deviceData) {
    return !explicitlyOffline(deviceData)
        && Boolean.TRUE.equals(deviceData.get("channelCatalogAuthoritative"));
  }

  @Override protected DeviceRecordSyncResult syncSingleDevice(
      String instanceKey, String deviceId, Map<String, Object> deviceData) {
    offlineCatalogSync.set(explicitlyOffline(deviceData));
    try {
      DeviceRecordSyncResult result = super.syncSingleDevice(instanceKey, deviceId, deviceData);
      VideoPlatformDevice saved = deviceMapper.selectOne(VideoPlatformDevice.builder()
          .instanceKey(instanceKey).deviceId(deviceId).build());
      if (saved != null) {
        Map<String, Object> effective = new LinkedHashMap<>(deviceData);
        Map<String, Object> config = VideoDeviceNames.configuration(saved.getConfiguration());
        effective.put("deviceName", saved.getDeviceName());
        for (String key : List.of("channelList", "channelCatalogAuthoritative", "loginChannelCount", "channelCount"))
          if (config.containsKey(key)) effective.put(key, config.get(key));
        for (String key : VideoDeviceNames.METADATA)
          if (config.containsKey(key)) effective.put(key, config.get(key));
        importService.updateDeviceStatusAndConfig(instanceKey, effective, getPlatformType());
      }
      return result;
    } finally {
      offlineCatalogSync.remove();
    }
  }

  @Override protected Map<String, Object> applyDeviceNames(
      VideoPlatformDevice stored, Map<String, Object> incoming) {
    Map<String, Object> result = super.applyDeviceNames(stored, incoming);
    boolean offline = explicitlyOffline(incoming);
    boolean authoritative = hasAuthoritativeChannelCatalog(incoming);
    result.put("channelCatalogAuthoritative", authoritative);
    if (!authoritative && stored != null) {
      Map<String, Map<String, Object>> retained = new LinkedHashMap<>();
      Object previous = VideoDeviceNames.configuration(stored.getConfiguration()).get("channelList");
      boolean hasPreviousDirectory = previous instanceof List<?>;
      if (previous instanceof List<?> list) for (Object item : list) {
        Map<String, Object> channel = VideoDeviceNames.object(item);
        channel.put("onlineState", offline ? "offline" : "unknown");
        channel.put("channelStatus", offline ? "OFF" : "UNKNOWN");
        channel.put("status", channel.get("channelStatus"));
        retained.put(VideoDeviceNames.text(channel.get("channelId")), channel);
      }
      // SDK 登录通道容量不能重新引入已经确认不存在的空槽。
      if (!offline && result.get("channelList") instanceof List<?> list) for (Object item : list) {
        Map<String, Object> channel = VideoDeviceNames.object(item);
        String id = VideoDeviceNames.text(channel.get("channelId"));
        if (!hasPreviousDirectory || retained.containsKey(id)) retained.put(id, channel);
      }
      if (hasPreviousDirectory || !retained.isEmpty())
        result.put("channelList", new ArrayList<>(retained.values()));
    }
    if (offline && result.get("channelList") instanceof List<?> list) {
      List<Map<String, Object>> channels = new ArrayList<>();
      for (Object item : list) {
        Map<String, Object> channel = VideoDeviceNames.object(item);
        channel.put("onlineState", "offline");
        channel.put("channelStatus", "OFF");
        channel.put("status", "OFF");
        channels.add(channel);
      }
      result.put("channelList", channels);
    }
    if (result.get("channelList") instanceof List<?> list) {
      result.put("channelCount", (int) list.stream().map(VideoDeviceNames::object)
          .map(channel -> VideoDeviceNames.physicalId(VideoDeviceNames.text(channel.get("channelId"))))
          .distinct().count());
    }
    return result;
  }

  @Override protected int batchInsertChannels(Long deviceId, String instanceKey,
      String platformDeviceId, List<Map<String, Object>> channels) {
    if (offlineCatalogSync.get()) return 0;
    return super.batchInsertChannels(deviceId, instanceKey, platformDeviceId, channels);
  }

  private boolean explicitlyOffline(Map<String, Object> data) {
    return isOffline(firstNonBlankString(data.get("deviceStatus"), data.get("status")))
        || isOffline(firstNonBlankString(data.get("online")));
  }

  @Override
  protected void setVendorSpecificDeviceExtFields(
      VideoPlatformDeviceExt.VideoPlatformDeviceExtBuilder builder, Map<String, Object> deviceData) {
    builder
        .ownerCode(fit(firstNonBlankString(deviceData.get("ownerCode")), 100))
        .subSystem(fit(firstNonBlankString(deviceData.get("subSystem")), 100));
    try {
      Object unitsInfo = deviceData.get("unitsInfo");
      if (unitsInfo != null) {
        builder.unitsInfo(objectMapper.writeValueAsString(unitsInfo));
      }
    } catch (Exception ignore) {
    }
  }

  @Override
  protected void setVendorSpecificChannelFields(
      VideoPlatformChannel.VideoPlatformChannelBuilder builder, Map<String, Object> channelData) {
    builder
        .channelCode(fit(firstNonBlankString(channelData.get("channelCode"), channelData.get("channelId")), 100))
        .channelSeq(
            parseIntOrNull(
                firstNonBlankString(
                    channelData.get("channelSeq"), channelData.get("channelNo"), channelData.get("channelIndex"))));
  }
}
