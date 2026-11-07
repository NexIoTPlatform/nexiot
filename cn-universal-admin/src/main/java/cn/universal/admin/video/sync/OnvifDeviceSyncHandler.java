package cn.universal.admin.video.sync;

import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.security.utils.SecurityUtils;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

@Slf4j
@Component
@RequiredArgsConstructor
public class OnvifDeviceSyncHandler extends AbstractDeviceSyncHandler {

  private final VideoPlatformAdapterRegistry adapterRegistry;

  @Override
  public String getPlatformType() {
    return "onvif";
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
      log.info("[ONVIF同步] 开始同步设备: instanceKey={}, 设备数={}", instance.getInstanceKey(), devices.size());
      for (Map<String, Object> deviceData : devices) {
        String deviceId = resolvePlatformDeviceId(deviceData);
        try {
          if (shouldSkipDevice(deviceData)) {
            String errorMessage = firstNonBlankString(deviceData.get("__syncError"), "ONVIF设备认证失败，未入库");
            log.warn("[ONVIF同步] 跳过设备: deviceId={}, reason={}", deviceId, errorMessage);
            result.addError(errorMessage);
            continue;
          }
          if (deviceId == null || deviceId.equals("null") || deviceId.isBlank()) {
            continue;
          }
          DeviceRecordSyncResult itemResult =
              syncSingleDeviceInNewTransaction(instance, deviceData, deviceId);
          applySyncResult(result, itemResult);
        } catch (Exception e) {
          log.error("[ONVIF同步] 同步设备失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
          addSyncError(result, deviceId, e);
        }
      }
      log.info(
          "[ONVIF同步] 同步完成: 总设备={}, 新增={}, 更新={}, 通道={}",
          result.getTotalDevices(),
          result.getInsertedDevices(),
          result.getUpdatedDevices(),
          result.getTotalChannels());
    } catch (Exception e) {
      log.error("[ONVIF同步] 同步失败: {}", e.getMessage(), e);
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

    builder
        .deviceSn(fit(firstNonBlankString(deviceData.get("serialNumber")), 100))
        .deviceType("onvif")
        .deviceCapabilitySet(fit(firstNonBlankString(deviceData.get("firmwareVersion")), 100))
        .extField1(fit(firstNonBlankString(deviceData.get("xAddr")), 500))
        .extField2(fit(firstNonBlankString(deviceData.get("mediaServiceUrl")), 500))
        .extField3(buildOnvifExtPayload(deviceData));

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

  private boolean shouldSkipDevice(Map<String, Object> deviceData) {
    if (deviceData == null) {
      return false;
    }
    Object flag = deviceData.get("__skipSync");
    return flag instanceof Boolean
        ? (Boolean) flag
        : flag != null && Boolean.parseBoolean(String.valueOf(flag));
  }

  @Override
  protected void setPlatformSpecificChannelFields(
      VideoPlatformChannel.VideoPlatformChannelBuilder builder, Map<String, Object> channelData) {
    builder
        .channelCode(fit(firstNonBlankString(channelData.get("channelId")), 100))
        .streamId(fit(firstNonBlankString(channelData.get("channelId")), 100))
        .ptz(parseIntOrNull(channelData.get("ptzType")))
        .extField1(fit(firstNonBlankString(channelData.get("xAddr")), 500))
        .extField2(fit(firstNonBlankString(channelData.get("platformDeviceId")), 500));
  }

  private String buildOnvifExtPayload(Map<String, Object> deviceData) {
    try {
      Map<String, Object> payload = new java.util.LinkedHashMap<>();
      copyIfPresent(payload, "xAddr", deviceData.get("xAddr"));
      copyIfPresent(payload, "deviceServiceUrl", deviceData.get("deviceServiceUrl"));
      copyIfPresent(payload, "mediaServiceUrl", deviceData.get("mediaServiceUrl"));
      copyIfPresent(payload, "ptzServiceUrl", deviceData.get("ptzServiceUrl"));
      copyIfPresent(payload, "recordingServiceUrl", deviceData.get("recordingServiceUrl"));
      copyIfPresent(payload, "searchServiceUrl", deviceData.get("searchServiceUrl"));
      copyIfPresent(payload, "replayServiceUrl", deviceData.get("replayServiceUrl"));
      copyIfPresent(payload, "serialNumber", deviceData.get("serialNumber"));
      copyIfPresent(payload, "hardwareId", deviceData.get("hardwareId"));
      copyIfPresent(payload, "firmwareVersion", deviceData.get("firmwareVersion"));
      copyIfPresent(payload, "username", deviceData.get("username"));
      copyIfPresent(payload, "password", deviceData.get("password"));
      copyIfPresent(payload, "authType", deviceData.get("authType"));
      copyIfPresent(payload, "deviceType", deviceData.get("deviceType"));
      copyIfPresent(payload, "deviceKind", deviceData.get("deviceKind"));
      copyIfPresent(payload, "createMethod", deviceData.get("createMethod"));
      copyIfPresent(payload, "vendor", deviceData.get("vendor"));
      copyIfPresent(payload, "remoteChannel", deviceData.get("remoteChannel"));
      copyIfPresent(payload, "channelTotal", deviceData.get("channelTotal"));
      copyIfPresent(payload, "rtspPort", deviceData.get("rtspPort"));
      copyIfPresent(payload, "decodePolicy", deviceData.get("decodePolicy"));
      copyIfPresent(payload, "encrypted", deviceData.get("encrypted"));
      copyIfPresent(payload, "raw", deviceData.get("raw"));
      return payload.isEmpty() ? null : objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.warn("[ONVIF同步] 构建设备扩展快照失败: {}", e.getMessage());
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
