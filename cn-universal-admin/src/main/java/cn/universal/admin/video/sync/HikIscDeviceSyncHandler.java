/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 海康ISC平台设备同步处理器
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.sync;

import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.security.utils.SecurityUtils;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/** 海康ISC平台设备同步处理器 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HikIscDeviceSyncHandler extends AbstractDeviceSyncHandler {

  private final VideoPlatformAdapterRegistry adapterRegistry;

  @Override
  public String getPlatformType() {
    return "isc";
  }

  @Override
  public DeviceSyncResult syncDevices(VideoPlatformInstance instance, Map<String, Object> filters) {
    DeviceSyncResult result = new DeviceSyncResult();

    try {
      if (shouldResetBeforeSync(filters)) {
        cleanService.markAllDevicesAsDisabled(instance.getInstanceKey());
      }

      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      List<Map<String, Object>> devices = adapter.listDevices(instance, filters);

      if (devices == null || devices.isEmpty()) {
        log.info("[ISC同步] 无设备数据");
        return result;
      }

      log.info(
          "[ISC同步] 开始同步设备: instanceKey={}, 设备数={}",
          instance.getInstanceKey(),
          devices.size());

      for (Map<String, Object> deviceData : devices) {
        String deviceId = resolvePlatformDeviceId(deviceData);
        try {
          if (deviceId == null) {
            log.warn("[ISC同步] 设备ID为空，跳过");
            continue;
          }

          DeviceRecordSyncResult itemResult =
              syncSingleDeviceInNewTransaction(instance, deviceData, deviceId);
          applySyncResult(result, itemResult);
        } catch (Exception e) {
          log.error("[ISC同步] 同步设备失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
          addSyncError(result, deviceId, e);
        }
      }

      log.info(
          "[ISC同步] 同步完成: 总设备={}, 新增={}, 更新={}, 总通道={}",
          result.getTotalDevices(),
          result.getInsertedDevices(),
          result.getUpdatedDevices(),
          result.getTotalChannels());
    } catch (Exception e) {
      log.error("[ISC同步] 同步失败: {}", e.getMessage(), e);
      result.addError("同步失败: " + e.getMessage());
    }

    return result;
  }

  @Override
  protected VideoPlatformDeviceExt upsertDeviceExt(
      Long deviceId, String instanceKey, String platformDeviceId, Map<String, Object> deviceData) {
    String currentUserId = SecurityUtils.getUnionId();
    String tenantId = resolveInstanceTenantId(instanceKey);
    Date now = new Date();

    Example ex = new Example(VideoPlatformDeviceExt.class);
    ex.createCriteria().andEqualTo("deviceId", deviceId);
    List<VideoPlatformDeviceExt> existList = deviceExtMapper.selectByExample(ex);
    VideoPlatformDeviceExt extEntity = existList.isEmpty() ? null : existList.get(0);

    VideoPlatformDeviceExt.VideoPlatformDeviceExtBuilder builder =
        extEntity == null
            ? VideoPlatformDeviceExt.builder()
                .deviceId(deviceId)
                .instanceKey(instanceKey)
                .platformDeviceId(platformDeviceId)
            : VideoPlatformDeviceExt.builder()
                .id(extEntity.getId())
                .deviceId(deviceId)
                .instanceKey(instanceKey)
                .platformDeviceId(platformDeviceId);

    builder.encodeDevIndexCode(
        fit(firstNonBlankString(deviceData.get("encodeDevIndexCode")), 100));
    builder.deviceCapabilitySet(
        fit(firstNonBlankString(deviceData.get("deviceCapabilitySet"), deviceData.get("capabilitySet")), 100));
    builder.extField1(fit(firstNonBlankString(deviceData.get("regionPath")), 500));
    builder.extField2(
        fit(
            firstNonBlankString(deviceData.get("recordLocation"), deviceData.get("recordLocationName")),
            500));
    builder.extField3(buildDeviceExtPayload(deviceData));

    VideoPlatformDeviceExt finalExt =
        builder
            .createId(extEntity == null ? currentUserId : extEntity.getCreateId())
            .tenantId(extEntity == null ? tenantId : extEntity.getTenantId())
            .updateId(currentUserId)
            .createTime(extEntity == null ? now : extEntity.getCreateTime())
            .updateTime(now)
            .build();

    if (extEntity == null) {
      deviceExtMapper.insertSelective(finalExt);
    } else {
      deviceExtMapper.updateByPrimaryKeySelective(finalExt);
    }

    return finalExt;
  }

  @Override
  protected void setPlatformSpecificChannelFields(
      VideoPlatformChannel.VideoPlatformChannelBuilder builder, Map<String, Object> channelData) {
    builder.cameraIndexCode(
        fit(firstNonBlankString(channelData.get("cameraIndexCode"), channelData.get("channelId")), 100));
    builder.channelNo(fit(firstNonBlankString(channelData.get("channelNo")), 20));
    builder.cameraType(parseIntOrNull(channelData.get("cameraType")));
    builder.ptz(parseIntOrNull(channelData.get("ptz")));
    builder.capabilitySet(fit(firstNonBlankString(channelData.get("capabilitySet")), 100));
    builder.installLocation(fit(firstNonBlankString(channelData.get("installLocation")), 200));
    builder.extField1(fit(firstNonBlankString(channelData.get("encodeDevIndexCode")), 500));
    builder.extField2(
        fit(
            firstNonBlankString(
                channelData.get("regionIndexCode"), channelData.get("orgId"), channelData.get("parentId")),
            500));
  }

  private String buildDeviceExtPayload(Map<String, Object> deviceData) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      copyIfPresent(payload, "cameraIndexCode", deviceData.get("cameraIndexCode"));
      copyIfPresent(payload, "cameraName", deviceData.get("cameraName"));
      copyIfPresent(payload, "cameraType", deviceData.get("cameraType"));
      copyIfPresent(payload, "cameraTypeName", deviceData.get("cameraTypeName"));
      copyIfPresent(payload, "channelNo", deviceData.get("channelNo"));
      copyIfPresent(payload, "channelType", deviceData.get("channelType"));
      copyIfPresent(payload, "channelTypeName", deviceData.get("channelTypeName"));
      copyIfPresent(payload, "regionIndexCode", deviceData.get("regionIndexCode"));
      copyIfPresent(payload, "regionPath", deviceData.get("regionPath"));
      copyIfPresent(payload, "recordLocation", deviceData.get("recordLocation"));
      copyIfPresent(payload, "recordLocationName", deviceData.get("recordLocationName"));
      copyIfPresent(payload, "transType", deviceData.get("transType"));
      copyIfPresent(payload, "transTypeName", deviceData.get("transTypeName"));
      copyIfPresent(payload, "capabilitySet", deviceData.get("capabilitySet"));
      copyIfPresent(payload, "capabilitySetName", deviceData.get("capabilitySetName"));
      copyIfPresent(payload, "installLocation", deviceData.get("installLocation"));
      copyIfPresent(payload, "longitude", deviceData.get("longitude"));
      copyIfPresent(payload, "latitude", deviceData.get("latitude"));
      copyIfPresent(payload, "altitude", deviceData.get("altitude"));
      copyIfPresent(payload, "raw", deviceData.get("raw"));
      return payload.isEmpty() ? null : objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.warn("[ISC同步] 构建设备扩展快照失败: {}", e.getMessage());
      return null;
    }
  }

  private void copyIfPresent(Map<String, Object> target, String key, Object value) {
    if (value == null) {
      return;
    }
    if (value instanceof String && ((String) value).trim().isEmpty()) {
      return;
    }
    target.put(key, value);
  }
}
