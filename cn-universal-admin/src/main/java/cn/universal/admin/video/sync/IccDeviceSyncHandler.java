/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 大华ICC平台设备同步处理器
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
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/**
 * 大华ICC平台设备同步处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IccDeviceSyncHandler extends AbstractDeviceSyncHandler {

  private final VideoPlatformAdapterRegistry adapterRegistry;

  @Override
  public String getPlatformType() {
    return "icc";
  }

  @Override
  public DeviceSyncResult syncDevices(VideoPlatformInstance instance, Map<String, Object> filters) {
    DeviceSyncResult result = new DeviceSyncResult();

    try {
      if (shouldResetBeforeSync(filters)) {
        cleanService.markAllDevicesAsDisabled(instance.getInstanceKey());
      }

      // 通过适配器获取设备列表
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      if (adapter == null) {
        log.error("[ICC同步] 未找到适配器: platformType={}", instance.getPlatformType());
        result.addError("未找到平台适配器");
        return result;
      }

      // 调用适配器获取设备列表
      List<Map<String, Object>> devices = adapter.listDevices(instance, filters);

      if (devices == null || devices.isEmpty()) {
        log.info("[ICC同步] 无设备数据");
        return result;
      }

      log.info(
          "[ICC同步] 开始同步设备: instanceKey={}, 设备数={}",
          instance.getInstanceKey(),
          devices.size());

      for (Map<String, Object> deviceData : devices) {
        String deviceId = resolvePlatformDeviceId(deviceData);
        try {
          if (deviceId == null || deviceId.equals("null") || deviceId.isEmpty()) {
            log.warn("[ICC同步] 设备ID为空，跳过");
            continue;
          }

          DeviceRecordSyncResult itemResult =
              syncSingleDeviceInNewTransaction(instance, deviceData, deviceId);
          applySyncResult(result, itemResult);
          log.debug(
              "[ICC同步] 设备同步完成: deviceId={}, isNew={}, channelCount={}",
              itemResult.getDeviceId(),
              itemResult.isNewDevice(),
              itemResult.getChannelCount());
        } catch (Exception e) {
          log.error("[ICC同步] 同步设备失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
          addSyncError(result, deviceId, e);
        }
      }

      log.info(
          "[ICC同步] 同步完成: 总设备={}, 新增={}, 更新={}, 总通道={}",
          result.getTotalDevices(),
          result.getInsertedDevices(),
          result.getUpdatedDevices(),
          result.getTotalChannels());

    } catch (Exception e) {
      log.error("[ICC同步] 同步失败: {}", e.getMessage(), e);
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

    // 查询是否存在
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

    // ICC特有字段
    builder.deviceSn(fit(firstNonBlankString(deviceData.get("deviceSn")), 100));
    builder.deviceType(fit(firstNonBlankString(deviceData.get("deviceType")), 50));
    builder.ownerCode(fit(firstNonBlankString(deviceData.get("ownerCode")), 100));
    builder.isOnline(fit(firstNonBlankString(deviceData.get("isOnline")), 10));
    builder.subSystem(fit(firstNonBlankString(deviceData.get("subSystem")), 100));
    
    // 保存unitsInfo（如果存在）
    try {
      Object unitsInfo = deviceData.get("unitsInfo");
      if (unitsInfo != null) {
        builder.unitsInfo(objectMapper.writeValueAsString(unitsInfo));
      }
    } catch (Exception e) {
      log.warn("[ICC同步] 序列化unitsInfo失败: {}", e.getMessage());
    }

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
    // ICC通道特有字段
    builder.channelCode(fit(firstNonBlankString(channelData.get("channelCode")), 100));
    builder.channelSeq(parseIntOrNull(channelData.get("channelSeq")));
    builder.encodeFormat(fit(firstNonBlankString(channelData.get("encodeFormat")), 50));
    builder.resolution(fit(firstNonBlankString(channelData.get("resolution")), 50));

    // 其他可能的ICC字段
    builder.ptz(parseIntOrNull(channelData.get("cameraType")));
  }
}
