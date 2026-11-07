/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 大华直连SDK设备同步处理器
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
import cn.universal.persistence.entity.VideoPlatformInstance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/** 大华直连SDK设备同步处理器。 */
@Component
public class DahuaSdkDeviceSyncHandler extends DirectSdkDeviceSyncHandler {

  @jakarta.annotation.Resource private VideoDeviceImportService importService;
  private final ThreadLocal<Boolean> offlineCatalogSync = ThreadLocal.withInitial(() -> false);

  public DahuaSdkDeviceSyncHandler(VideoPlatformAdapterRegistry adapterRegistry) {
    super(adapterRegistry);
  }

  @Override
  public String getPlatformType() {
    return "dahua_sdk";
  }

  @Override
  protected String getLogPrefix() {
    return "大华SDK";
  }

  @Override
  protected String getDefaultManufacturer() {
    return "Dahua";
  }

  @Override protected boolean preserveLocalConfiguration() { return true; }

  @Override protected boolean hasAuthoritativeChannelCatalog(Map<String, Object> deviceData) {
    return !explicitlyOffline(deviceData)
        && Boolean.TRUE.equals(deviceData.get("channelCatalogAuthoritative"));
  }

  @Override protected boolean shouldResetBeforeSync(Map<String, Object> filters) { return false; }

  public void syncRegisteredDevice(VideoPlatformInstance instance, Map<String, Object> registered) throws Exception {
    if (instance.getTenantId() == null || instance.getTenantId().isBlank())
      throw new IllegalStateException("大华主动注册实例缺少可信租户");
    Map<String, Object> data = new LinkedHashMap<>(registered);
    String id = firstNonBlankString(data.get("deviceId"), data.get("registrationId"), data.get("registerCode"), data.get("gatewayDeviceId"));
    if (id == null || id.isBlank() || id.length() > 100)
      throw new IllegalStateException("大华主动注册设备标识无效");
    data.put("deviceId", id); data.put("gatewayDeviceId", id);
    data.put("accessMode", "active_registration"); data.put("loginMode", "active_registration");
    data.put("streamMode", "sdk_gateway");
    normalizeDirectSdkDeviceData(data);
    if (explicitlyOffline(data)) { data.put("deviceStatus", "OFF"); data.put("status", "OFF"); }
    syncSingleDeviceInNewTransaction(instance, data, id);
  }

  @Override protected DeviceRecordSyncResult syncSingleDevice(
      String instanceKey, String deviceId, Map<String, Object> deviceData) {
    offlineCatalogSync.set(explicitlyOffline(deviceData));
    try {
      return syncDeviceCatalog(instanceKey, deviceId, deviceData);
    } finally {
      offlineCatalogSync.remove();
    }
  }

  private DeviceRecordSyncResult syncDeviceCatalog(
      String instanceKey, String deviceId, Map<String, Object> deviceData) {
    boolean registration = "active_registration".equals(deviceData.get("loginMode"))
        || "active_registration".equals(deviceData.get("accessMode"));
    if (!registration) {
      DeviceRecordSyncResult result = super.syncSingleDevice(instanceKey, deviceId, deviceData);
      VideoPlatformDevice saved = deviceMapper.selectOne(VideoPlatformDevice.builder()
          .instanceKey(instanceKey).deviceId(deviceId).build());
      if (saved != null) importService.updateDeviceStatusAndConfig(instanceKey, effectiveSnapshot(saved, deviceData), "dahua_sdk");
      return result;
    }
    if (explicitlyOffline(deviceData)) {
      Example existing = new Example(VideoPlatformDevice.class);
      existing.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("deviceId", deviceId);
      existing.setForUpdate(true);
      if (deviceMapper.selectByExample(existing).isEmpty()) return null;
    }
    DeviceRecordSyncResult result = super.syncSingleDevice(instanceKey, deviceId, deviceData);
    VideoPlatformDevice saved = deviceMapper.selectOne(VideoPlatformDevice.builder()
        .instanceKey(instanceKey).deviceId(deviceId).build());
    if (saved != null && Integer.valueOf(1).equals(saved.getEnabled())) {
      Map<String, Object> effective = effectiveSnapshot(saved, deviceData);
      importService.importDevice(instanceKey, effective, null);
      if (!importService.updateDeviceStatusAndConfig(instanceKey, effective, "dahua_sdk"))
        throw new IllegalStateException("大华主动注册 IoT 同步未完成");
    }
    return result;
  }

  @Override
  protected Map<String, Object> applyDeviceNames(VideoPlatformDevice stored, Map<String, Object> incoming) {
    if (stored == null) return super.applyDeviceNames(null, incoming);
    Map<String, Object> config = VideoDeviceNames.configuration(stored.getConfiguration());
    // 首次升级时沿用历史人工设备名；已有原名记录后，未设别名的设备继续跟随来源。
    String savedName = VideoDeviceNames.text(stored.getDeviceName());
    String incomingName = firstNonBlankString(incoming.get("deviceName"), incoming.get("name"));
    if (!config.containsKey(VideoDeviceNames.SOURCE_DEVICE)
        && !config.containsKey(VideoDeviceNames.DEVICE_OVERRIDE)
        && !savedName.isEmpty() && incomingName != null && !savedName.equals(incomingName))
      config.put(VideoDeviceNames.DEVICE_OVERRIDE, savedName);
    Map<String, Object> result = VideoDeviceNames.apply(VideoDeviceNames.encode(config), incoming);
    if (!hasAuthoritativeChannelCatalog(incoming)) {
      Map<String, Map<String, Object>> retained = new LinkedHashMap<>();
      Object previous = config.get("channelList");
      boolean hasPreviousDirectory = previous instanceof List<?>;
      if (previous instanceof List<?> list) for (Object item : list) {
        Map<String, Object> channel = VideoDeviceNames.object(item);
        channel.put("onlineState", explicitlyOffline(incoming) ? "offline" : "unknown");
        channel.put("channelStatus", explicitlyOffline(incoming) ? "OFF" : "UNKNOWN");
        channel.put("status", channel.get("channelStatus"));
        retained.put(VideoDeviceNames.text(channel.get("channelId")), channel);
      }
      if (!explicitlyOffline(incoming) && result.get("channelList") instanceof List<?> list) for (Object item : list) {
        Map<String, Object> channel = VideoDeviceNames.object(item);
        String id = VideoDeviceNames.text(channel.get("channelId"));
        if (!hasPreviousDirectory || retained.containsKey(id)) retained.put(id, channel);
      }
      if (hasPreviousDirectory || !retained.isEmpty()) result.put("channelList", new ArrayList<>(retained.values()));
      result.put("channelCatalogAuthoritative", false);
    }
    return result;
  }

  private Map<String, Object> effectiveSnapshot(VideoPlatformDevice saved, Map<String, Object> incoming) {
    Map<String, Object> result = new LinkedHashMap<>(incoming);
    Map<String, Object> config = VideoDeviceNames.configuration(saved.getConfiguration());
    result.put("deviceName", saved.getDeviceName());
    if (config.containsKey("channelList")) result.put("channelList", config.get("channelList"));
    for (String key : VideoDeviceNames.METADATA)
      if (config.containsKey(key)) result.put(key, config.get(key));
    return result;
  }

  @Override protected boolean preserveChannelConfiguration() { return true; }

  @Override protected int batchInsertChannels(Long deviceId, String instanceKey,
      String platformDeviceId, List<Map<String, Object>> channels) {
    // 离线快照只更新已有通道的状态；过时配置中的通道不得重新创建。
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
