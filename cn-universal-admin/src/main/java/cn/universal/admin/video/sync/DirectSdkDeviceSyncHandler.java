/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 厂商直连SDK设备同步处理器基类
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
import lombok.extern.slf4j.Slf4j;
import tk.mybatis.mapper.entity.Example;

/** 厂商直连SDK设备同步处理器基类。 */
@Slf4j
public abstract class DirectSdkDeviceSyncHandler extends AbstractDeviceSyncHandler {

  private final VideoPlatformAdapterRegistry adapterRegistry;

  protected DirectSdkDeviceSyncHandler(VideoPlatformAdapterRegistry adapterRegistry) {
    this.adapterRegistry = adapterRegistry;
  }

  protected abstract String getLogPrefix();

  protected abstract String getDefaultManufacturer();

  @Override
  public DeviceSyncResult syncDevices(VideoPlatformInstance instance, Map<String, Object> filters) {
    DeviceSyncResult result = new DeviceSyncResult();
    String logPrefix = getLogPrefix();

    try {
      if (shouldResetBeforeSync(filters)) {
        cleanService.markAllDevicesAsDisabled(instance.getInstanceKey());
      }

      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      List<Map<String, Object>> devices = adapter.listDevices(instance, filters);

      if (devices == null || devices.isEmpty()) {
        log.info("[{}同步] 无设备数据", logPrefix);
        return result;
      }

      log.info(
          "[{}同步] 开始同步设备: instanceKey={}, 设备数={}",
          logPrefix,
          instance.getInstanceKey(),
          devices.size());

      for (Map<String, Object> remoteDeviceData : devices) {
        Map<String, Object> deviceData =
            remoteDeviceData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(remoteDeviceData);
        String deviceId = resolveDirectSdkDeviceId(deviceData);
        try {
          if (deviceId == null || deviceId.isBlank() || "null".equalsIgnoreCase(deviceId)) {
            log.warn("[{}同步] 设备ID为空，跳过", logPrefix);
            continue;
          }

          normalizeDirectSdkDeviceData(deviceData);
          DeviceRecordSyncResult itemResult =
              syncSingleDeviceInNewTransaction(instance, deviceData, deviceId);
          applySyncResult(result, itemResult);
        } catch (Exception e) {
          log.error("[{}同步] 同步设备失败: deviceId={}, error={}", logPrefix, deviceId, e.getMessage(), e);
          addSyncError(result, deviceId, e);
        }
      }

      log.info(
          "[{}同步] 同步完成: 总设备={}, 新增={}, 更新={}, 总通道={}",
          logPrefix,
          result.getTotalDevices(),
          result.getInsertedDevices(),
          result.getUpdatedDevices(),
          result.getTotalChannels());
    } catch (Exception e) {
      log.error("[{}同步] 同步失败: {}", logPrefix, e.getMessage(), e);
      result.addError("同步失败: " + e.getMessage());
    }

    return result;
  }

  protected String resolveDirectSdkDeviceId(Map<String, Object> deviceData) {
    if (deviceData == null) {
      return null;
    }
    return fit(
        firstNonBlankString(
            deviceData.get("deviceId"),
            deviceData.get("id"),
            deviceData.get("serialNumber"),
            deviceData.get("deviceSn"),
            deviceData.get("sn"),
            deviceData.get("ip"),
            deviceData.get("deviceIp")),
        100);
  }

  protected void normalizeDirectSdkDeviceData(Map<String, Object> deviceData) {
    if (deviceData == null) {
      return;
    }
    deviceData.putIfAbsent("manufacturer", getDefaultManufacturer());
    deviceData.putIfAbsent("deviceStatus", firstNonBlankString(deviceData.get("status"), "online"));
  }

  @Override
  protected VideoPlatformDeviceExt upsertDeviceExt(
      Long deviceId, String instanceKey, String platformDeviceId, Map<String, Object> deviceData) {
    String currentUserId = resolveSyncActor(instanceKey);
    String tenantId = resolveInstanceTenantId(instanceKey);
    Date now = new Date();

    Example ex = new Example(VideoPlatformDeviceExt.class);
    ex.createCriteria().andEqualTo("deviceId", deviceId);
    List<VideoPlatformDeviceExt> existList = deviceExtMapper.selectByExample(ex);
    VideoPlatformDeviceExt extEntity = existList.isEmpty() ? null : existList.get(0);
    // 扫描结果可能不带凭据，页面也可能回传掩码；这两种情况都应保留数据库中的真实凭据。
    if (extEntity != null && extEntity.getExtField3() != null) {
      try {
        Map<String, Object> stored = objectMapper.readValue(extEntity.getExtField3(), Map.class);
        deviceData = new LinkedHashMap<>(deviceData);
        for (String key : java.util.List.of("username", "password", "credentialMode")) {
          Object value = deviceData.get(key);
          if (value == null || String.valueOf(value).isBlank() || String.valueOf(value).matches("\\*+")) {
            if (stored.get(key) != null) deviceData.put(key, stored.get(key));
          }
        }
      } catch (Exception e) {
        throw new IllegalStateException("读取设备凭据失败，已停止同步以保留原配置", e);
      }
    }

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

    builder
        .deviceSn(fit(firstNonBlankString(deviceData.get("serialNumber"), deviceData.get("deviceSn"), deviceData.get("sn")), 100))
        .deviceType(getPlatformType())
        .deviceCapabilitySet(fit(firstNonBlankString(deviceData.get("deviceCapabilitySet"), deviceData.get("capabilitySet")), 100))
        .hostAddress(fit(firstNonBlankString(deviceData.get("deviceIp"), deviceData.get("ip"), deviceData.get("hostAddress")), 100))
        .mediaServerId(fit(firstNonBlankString(deviceData.get("mediaServerId"), deviceData.get("zlmMediaServerId")), 100))
        .isOnline(resolveOnlineFlag(deviceData))
        .extField1(fit(firstNonBlankString(deviceData.get("gatewayDeviceId"), deviceData.get("registerCode")), 500))
        .extField2(fit(firstNonBlankString(deviceData.get("streamMode"), deviceData.get("accessMode")), 500))
        .extField3(buildDeviceExtPayload(deviceData));

    setVendorSpecificDeviceExtFields(builder, deviceData);

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

  protected void setVendorSpecificDeviceExtFields(
      VideoPlatformDeviceExt.VideoPlatformDeviceExtBuilder builder, Map<String, Object> deviceData) {}

  @Override
  protected void setPlatformSpecificChannelFields(
      VideoPlatformChannel.VideoPlatformChannelBuilder builder, Map<String, Object> channelData) {
    builder
        .streamId(fit(firstNonBlankString(channelData.get("streamId"), channelData.get("channelId")), 100))
        .channelNo(
            fit(
                firstNonBlankString(
                    channelData.get("channelNo"), channelData.get("channelNum"), channelData.get("channelIndex")),
                20))
        .channelCode(fit(firstNonBlankString(channelData.get("channelCode"), channelData.get("channelId")), 100))
        .channelSeq(
            parseIntOrNull(
                firstNonBlankString(
                    channelData.get("channelSeq"), channelData.get("channelNo"), channelData.get("channelIndex"))))
        .cameraIndexCode(
            fit(firstNonBlankString(channelData.get("cameraIndexCode"), channelData.get("channelId")), 100))
        .cameraType(parseIntOrNull(channelData.get("cameraType")))
        .ptz(parseIntOrNull(firstNonBlankString(channelData.get("ptz"), channelData.get("ptzType"))))
        .ptzType(parseIntOrNull(firstNonBlankString(channelData.get("ptzType"), channelData.get("ptz"))))
        .capabilitySet(fit(firstNonBlankString(channelData.get("capabilitySet")), 100))
        .encodeFormat(fit(firstNonBlankString(channelData.get("encodeFormat"), channelData.get("codec")), 50))
        .resolution(fit(firstNonBlankString(channelData.get("resolution")), 50))
        .installLocation(fit(firstNonBlankString(channelData.get("installLocation"), channelData.get("address")), 200))
        .extField1(fit(firstNonBlankString(channelData.get("rtspUrl"), channelData.get("mainRtspUrl")), 500))
        .extField2(fit(firstNonBlankString(channelData.get("subRtspUrl"), channelData.get("playUrl")), 500))
        .extField3(buildChannelExtPayload(channelData));

    setVendorSpecificChannelFields(builder, channelData);
  }

  protected void setVendorSpecificChannelFields(
      VideoPlatformChannel.VideoPlatformChannelBuilder builder, Map<String, Object> channelData) {}

  private String resolveOnlineFlag(Map<String, Object> deviceData) {
    String value =
        firstNonBlankString(deviceData.get("isOnline"), deviceData.get("online"), deviceData.get("deviceStatus"), deviceData.get("status"));
    if (value == null) {
      return null;
    }
    if ("true".equalsIgnoreCase(value) || "online".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
      return "1";
    }
    if ("false".equalsIgnoreCase(value) || "offline".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
      return "0";
    }
    return fit(value, 10);
  }

  private String buildDeviceExtPayload(Map<String, Object> deviceData) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      copyIfPresent(payload, "platformType", getPlatformType());
      for (String key : java.util.List.of("username", "password", "credentialMode")) {
        Object value = deviceData.get(key);
        if (value != null && !String.valueOf(value).matches("\\*+")) {
          copyIfPresent(payload, key, value);
        }
      }
      copyIfPresent(payload, "gatewayDeviceId", deviceData.get("gatewayDeviceId"));
      copyIfPresent(payload, "registerCode", deviceData.get("registerCode"));
      copyIfPresent(payload, "serialNumber", deviceData.get("serialNumber"));
      copyIfPresent(payload, "deviceSn", deviceData.get("deviceSn"));
      copyIfPresent(payload, "sdkPort", firstNonNull(deviceData.get("sdkPort"), deviceData.get("port"), deviceData.get("devicePort")));
      copyIfPresent(payload, "rtspPort", deviceData.get("rtspPort"));
      copyIfPresent(payload, "streamMode", deviceData.get("streamMode"));
      copyIfPresent(payload, "loginMode", deviceData.get("loginMode"));
      copyIfPresent(payload, "accessMode", deviceData.get("accessMode"));
      copyIfPresent(payload, "registrationId", deviceData.get("registrationId"));
      copyIfPresent(payload, "authenticated", deviceData.get("authenticated"));
      copyIfPresent(payload, "lastSeen", deviceData.get("lastSeen"));
      copyIfPresent(payload, "channelTotal", deviceData.get("channelTotal"));
      copyIfPresent(payload, "firmwareVersion", deviceData.get("firmwareVersion"));
      copyIfPresent(payload, "gatewayEndpoint", deviceData.get("gatewayEndpoint"));
      copyIfPresent(payload, "raw", deviceData.get("raw"));
      return payload.isEmpty() ? null : objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.warn("[{}同步] 构建设备扩展快照失败: {}", getLogPrefix(), e.getMessage());
      return null;
    }
  }

  private String buildChannelExtPayload(Map<String, Object> channelData) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      copyIfPresent(payload, "platformType", getPlatformType());
      copyIfPresent(payload, "channelIndex", channelData.get("channelIndex"));
      copyIfPresent(payload, "channelNum", channelData.get("channelNum"));
      copyIfPresent(payload, "streamToken", channelData.get("streamToken"));
      copyIfPresent(payload, "mainRtspUrl", channelData.get("mainRtspUrl"));
      copyIfPresent(payload, "subRtspUrl", channelData.get("subRtspUrl"));
      copyIfPresent(payload, "rtspUrl", channelData.get("rtspUrl"));
      copyIfPresent(payload, "transport", channelData.get("transport"));
      if (List.of("hik_sdk", "tiandy_sdk", "uniview_sdk").contains(getPlatformType())) {
        for (String key : java.util.List.of("sdkChannel", "nativeChannelNo", "rtspChannel", "streamType", "onlineState",
            "configured", "enabled", "channelStateSource"))
          copyIfPresent(payload, key, channelData.get(key));
      }
      copyIfPresent(payload, "raw", channelData.get("raw"));
      return payload.isEmpty() ? null : objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.warn("[{}同步] 构建通道扩展快照失败: {}", getLogPrefix(), e.getMessage());
      return null;
    }
  }

  private Object firstNonNull(Object... values) {
    if (values == null) {
      return null;
    }
    for (Object value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  protected void copyIfPresent(Map<String, Object> target, String key, Object value) {
    if (value == null) {
      return;
    }
    if (value instanceof String && ((String) value).trim().isEmpty()) {
      return;
    }
    target.put(key, value);
  }
}
