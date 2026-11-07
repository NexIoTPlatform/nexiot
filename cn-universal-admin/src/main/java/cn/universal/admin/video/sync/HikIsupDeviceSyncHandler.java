/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 海康ISUP设备同步处理器
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.sync;

import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.admin.video.service.VideoDeviceImportService;
import cn.universal.admin.video.service.VideoDeviceNames;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/** 海康ISUP主动注册设备同步处理器。 */
@Component
public class HikIsupDeviceSyncHandler extends DirectSdkDeviceSyncHandler {

  @Autowired private VideoDeviceImportService importService;
  private final ThreadLocal<Boolean> offlineCatalogSync = ThreadLocal.withInitial(() -> false);

  public HikIsupDeviceSyncHandler(VideoPlatformAdapterRegistry adapterRegistry) {
    super(adapterRegistry);
  }

  @Override
  public String getPlatformType() {
    return "hik_isup";
  }

  @Override
  protected String getLogPrefix() {
    return "海康ISUP";
  }

  @Override
  protected String getDefaultManufacturer() {
    return "Hikvision";
  }

  @Override protected boolean preserveLocalConfiguration() { return true; }

  @Override protected boolean shouldResetBeforeSync(Map<String, Object> filters) { return false; }

  @Override protected boolean hasAuthoritativeChannelCatalog(Map<String, Object> deviceData) {
    return !explicitlyOffline(deviceData)
        && Boolean.TRUE.equals(deviceData.get("channelCatalogAuthoritative"));
  }

  @Override protected String resolveDirectSdkDeviceId(Map<String, Object> deviceData) {
    return firstNonBlankString(deviceData.get("deviceId"), deviceData.get("registerCode"), deviceData.get("gatewayDeviceId"));
  }

  @Override protected void normalizeDirectSdkDeviceData(Map<String, Object> deviceData) {
    super.normalizeDirectSdkDeviceData(deviceData);
    if (explicitlyOffline(deviceData)) {
      deviceData.put("deviceStatus", "OFF");
      deviceData.put("status", "OFF");
    }
  }

  public void syncRegisteredDevice(VideoPlatformInstance instance, Map<String, Object> registered) throws Exception {
    if (instance.getTenantId() == null || instance.getTenantId().isBlank())
      throw new IllegalStateException("ISUP 实例缺少可信租户");
    Map<String, Object> data = new LinkedHashMap<>(registered);
    String id = resolveDirectSdkDeviceId(data);
    if (id == null || id.isBlank() || id.length() > 100)
      throw new IllegalStateException("ISUP 注册设备标识无效");
    normalizeDirectSdkDeviceData(data);
    syncSingleDeviceInNewTransaction(instance, data, id);
  }

  @Override protected DeviceRecordSyncResult syncSingleDevice(
      String instanceKey, String deviceId, Map<String, Object> deviceData) {
    if (explicitlyOffline(deviceData)) {
      // 网关保留离线目录；只能更新已有记录，不能把用户已删除的设备重新建回。
      // 此处已在单设备事务中，锁住现存行，避免后续 upsert 与物理删除交叉后再次插入。
      Example existing = new Example(cn.universal.persistence.entity.VideoPlatformDevice.class);
      existing.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("deviceId", deviceId);
      existing.setForUpdate(true);
      if (deviceMapper.selectByExample(existing).isEmpty()) return null;
    }
    offlineCatalogSync.set(explicitlyOffline(deviceData));
    try {
      DeviceRecordSyncResult result = super.syncSingleDevice(instanceKey, deviceId, deviceData);
      var saved = deviceMapper.selectOne(VideoPlatformDevice.builder()
          .instanceKey(instanceKey).deviceId(deviceId).build());
      if (saved != null && Integer.valueOf(1).equals(saved.getEnabled())) {
        Map<String, Object> effective = new LinkedHashMap<>(deviceData);
        Map<String, Object> config = VideoDeviceNames.configuration(saved.getConfiguration());
        effective.put("deviceName", saved.getDeviceName());
        for (String key : List.of("channelList", "channelCatalogAuthoritative", "channelCount"))
          if (config.containsKey(key)) effective.put(key, config.get(key));
        for (String key : VideoDeviceNames.METADATA)
          if (config.containsKey(key)) effective.put(key, config.get(key));
        importService.importDevice(instanceKey, effective, null);
        if (!importService.updateDeviceStatusAndConfig(instanceKey, effective, getPlatformType()))
          throw new IllegalStateException("ISUP IoT 设备同步未完成");
      }
      return result;
    } finally {
      offlineCatalogSync.remove();
    }
  }

  @Override protected Map<String, Object> applyDeviceNames(
      VideoPlatformDevice stored, Map<String, Object> incoming) {
    Map<String, Object> result = super.applyDeviceNames(stored, incoming);
    boolean authoritative = hasAuthoritativeChannelCatalog(incoming);
    boolean offline = explicitlyOffline(incoming);
    result.put("channelCatalogAuthoritative", authoritative);
    if (!authoritative && stored != null) {
      Map<String, Map<String, Object>> retained = new LinkedHashMap<>();
      Object previous = VideoDeviceNames.configuration(stored.getConfiguration()).get("channelList");
      boolean hasPrevious = previous instanceof List<?>;
      if (previous instanceof List<?> list) for (Object item : list) {
        Map<String, Object> channel = VideoDeviceNames.object(item);
        channel.put("onlineState", offline ? "offline" : "unknown");
        channel.put("channelStatus", offline ? "OFF" : "UNKNOWN");
        channel.put("status", channel.get("channelStatus"));
        retained.put(VideoDeviceNames.text(channel.get("channelId")), channel);
      }
      if (!offline && result.get("channelList") instanceof List<?> list) for (Object item : list) {
        Map<String, Object> channel = VideoDeviceNames.object(item);
        String id = VideoDeviceNames.text(channel.get("channelId"));
        // Partial ISAPI discoveries may add real channels; registration capacity is only a fallback.
        if (!hasPrevious || retained.containsKey(id) || "isapi".equals(channel.get("directorySource")))
          retained.put(id, channel);
      }
      if (hasPrevious || !retained.isEmpty())
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
    if (result.get("channelList") instanceof List<?> list) result.put("channelCount", list.size());
    return result;
  }

  @Override protected int batchInsertChannels(Long deviceId, String instanceKey,
      String platformDeviceId, List<Map<String, Object>> channels) {
    if (offlineCatalogSync.get()) return 0;
    return super.batchInsertChannels(deviceId, instanceKey, platformDeviceId, channels);
  }

  private boolean explicitlyOffline(Map<String, Object> deviceData) {
    return isOffline(firstNonBlankString(deviceData.get("deviceStatus"), deviceData.get("status")))
        || isOffline(firstNonBlankString(deviceData.get("online")));
  }

  @Override
  protected void setVendorSpecificDeviceExtFields(
      VideoPlatformDeviceExt.VideoPlatformDeviceExtBuilder builder, Map<String, Object> deviceData) {
    builder
        .encodeDevIndexCode(
            fit(
                firstNonBlankString(
                    deviceData.get("encodeDevIndexCode"),
                    deviceData.get("gatewayDeviceId"),
                    deviceData.get("registerCode"),
                    deviceData.get("deviceId")),
                100))
        .ownerCode(fit(firstNonBlankString(deviceData.get("registerCode"), deviceData.get("gatewayDeviceId")), 100));
  }

  @Override
  protected void setVendorSpecificChannelFields(
      VideoPlatformChannel.VideoPlatformChannelBuilder builder, Map<String, Object> channelData) {
    builder
        .cameraIndexCode(
            fit(firstNonBlankString(channelData.get("cameraIndexCode"), channelData.get("channelId")), 100))
        .channelNo(
            fit(
                firstNonBlankString(
                    channelData.get("channelNo"), channelData.get("channelNum"), channelData.get("channelIndex")),
                20))
        .channelType(fit(firstNonBlankString(channelData.get("channelType"), "isup"), 50));
  }
}
