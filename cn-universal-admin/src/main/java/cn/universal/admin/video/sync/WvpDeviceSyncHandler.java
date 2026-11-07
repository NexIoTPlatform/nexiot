/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: WVP平台设备同步处理器
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

/** WVP平台设备同步处理器 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WvpDeviceSyncHandler extends AbstractDeviceSyncHandler {

  private final VideoPlatformAdapterRegistry adapterRegistry;

  @Override
  public String getPlatformType() {
    return "wvp";
  }

  @Override
  public DeviceSyncResult syncDevices(VideoPlatformInstance instance, Map<String, Object> filters) {
    DeviceSyncResult result = new DeviceSyncResult();
    try {
      if (shouldResetBeforeSync(filters)) {
        cleanService.markAllDevicesAsDisabled(instance.getInstanceKey());
      }

      // 获取适配器并拉取远程设备列表
      VideoPlatformAdapter adapter = adapterRegistry.getAdapter(instance.getPlatformType());
      List<Map<String, Object>> remoteDevices = adapter.listDevices(instance, filters);

      log.info(
          "[WVP同步] 开始同步设备: instanceKey={}, 设备数={}",
          instance.getInstanceKey(),
          remoteDevices.size());

      for (Map<String, Object> deviceData : remoteDevices) {
        String deviceId = resolvePlatformDeviceId(deviceData);
        try {
          if (deviceId == null || deviceId.equals("null")) continue;

          DeviceRecordSyncResult itemResult =
              syncSingleDeviceInNewTransaction(instance, deviceData, deviceId);
          applySyncResult(result, itemResult);
        } catch (Exception e) {
          log.error("[WVP同步] 同步设备失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
          addSyncError(result, deviceId, e);
        }
      }

      log.info(
          "[WVP同步] 同步完成: 总设备={}, 新增={}, 更新={}, 通道={}",
          result.getTotalDevices(),
          result.getInsertedDevices(),
          result.getUpdatedDevices(),
          result.getTotalChannels());

    } catch (Exception e) {
      log.error("[WVP同步] 同步失败: {}", e.getMessage(), e);
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

    // WVP特有字段
    builder.charset(fit(firstNonBlankString(deviceData.get("charset")), 20));
    builder.transport(fit(firstNonBlankString(deviceData.get("transport")), 20));
    builder.streamMode(fit(firstNonBlankString(deviceData.get("streamMode")), 50));
    builder.hostAddress(fit(firstNonBlankString(deviceData.get("hostAddress")), 100));
    builder.mediaServerId(fit(firstNonBlankString(deviceData.get("mediaServerId")), 100));
    try {
      Object expires = deviceData.get("expires");
      if (expires != null) builder.expires(Integer.parseInt(String.valueOf(expires)));
    } catch (Exception ignore) {
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
    // WVP通道特有字段
    builder.streamId(fit(firstNonBlankString(channelData.get("streamId")), 100));
    builder.gbStreamId(fit(firstNonBlankString(channelData.get("gbStreamId")), 100));
    builder.hasAudio(parseIntOrNull(channelData.get("hasAudio")));
  }
}
