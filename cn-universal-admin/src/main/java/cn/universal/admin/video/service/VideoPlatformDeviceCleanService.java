/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台设备清理服务
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.service;

import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.common.domain.R;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceExtMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

/**
 * 视频平台设备清理服务 负责设备状态重置与级联删除逻辑
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/01/29
 */
@Service
@Slf4j
public class VideoPlatformDeviceCleanService extends BaseServiceImpl {

  @Autowired private VideoPlatformDeviceMapper deviceMapper;

  @Autowired private VideoPlatformChannelMapper channelMapper;

  @Autowired private VideoPlatformDeviceExtMapper deviceExtMapper;

  @Autowired private IIoTDeviceService iotDeviceService;

  @Autowired private IoTDeviceMapper iotDeviceMapper;

  @Autowired private IoTProductMapper iotProductMapper;

  @Autowired private VideoPlatformInstanceMapper instanceMapper;

  @Autowired private IoTDeviceService iotDeviceLifecycleService;

  @Autowired private cn.universal.admin.video.alarm.SdkVideoAlarmContextCache alarmContextCache;

  /**
   * 同步前置操作: 批量标记所有设备为禁用状态
   *
   * @param instanceKey 实例Key
   * @return 影响行数
   */
  public int markAllDevicesAsDisabled(String instanceKey) {
    log.info("[设备清理] 开始标记设备为禁用: instanceKey={}", instanceKey);
    int count = deviceMapper.batchUpdateEnabledByInstanceKey(instanceKey, 0);
    alarmContextCache.publishPlatformChange(instanceKey);
    log.info("[设备清理] 标记完成: instanceKey={}, 影响行数={}", instanceKey, count);
    return count;
  }

  /**
   * 查询云端已删除的设备(enabled=0)
   *
   * @param instanceKey 实例Key
   * @return 已删除的设备列表
   */
  public List<VideoPlatformDevice> findCloudDeletedDevices(String instanceKey) {
    Example example = new Example(VideoPlatformDevice.class);
    example.createCriteria().andEqualTo("instanceKey", instanceKey).andEqualTo("enabled", 0);
    return deviceMapper.selectByExample(example);
  }

  /**
   * 级联删除视频平台设备
   *
   * @param id 视频平台设备主键ID
   * @return 删除结果
   */
  @Transactional(rollbackFor = Exception.class)
  public R<java.util.Map<String, Object>> deleteVideoPlatformDevice(Long id) {
    log.info("[设备删除] 开始删除视频平台设备: id={}", id);

    try {
      // 1. 查询设备信息
      VideoPlatformDevice device = deviceMapper.selectByPrimaryKey(id);
      if (device == null) {
        log.warn("[设备删除] 设备不存在: id={}", id);
        return R.error("设备不存在", null);
      }
      // 视频平台设备的真实归属由平台实例决定；设备表中的租户字段可能来自旧版本数据。
      VideoPlatformInstance instance =
          instanceMapper.selectOne(
              VideoPlatformInstance.builder().instanceKey(device.getInstanceKey()).build());
      if (instance == null) {
        assertTenantResourceAccess(device.getTenantId(), device.getCreateId(), "无权删除该视频平台设备");
      } else {
        assertTenantResourceAccess(instance.getTenantId(), instance.getCreatorId(), "无权删除该视频平台设备");
      }

      String instanceKey = device.getInstanceKey();
      String devId = device.getDeviceId();
      log.info(
          "[设备删除] 设备信息: instanceKey={}, deviceId={}, name={}",
          instanceKey,
          devId,
          device.getDeviceName());

      // 2. 检查并删除关联的IoT设备
      R<Boolean> iotDeleteResult = deleteRelatedIotDevice(instanceKey, devId);
      if (!iotDeleteResult.isSuccess()) {
        log.error("[设备删除] IoT设备删除失败: {}", iotDeleteResult.getMsg());
        throw new RuntimeException("IoT设备删除失败: " + iotDeleteResult.getMsg());
      }

      // 3. 删除通道数据
      int channelCount = deleteChannels(id);
      log.info("[设备删除] 删除通道: deviceId={}, 数量={}", id, channelCount);

      // 4. 删除扩展数据
      int extCount = deleteDeviceExt(id);
      log.info("[设备删除] 删除扩展数据: deviceId={}, 数量={}", id, extCount);

      // 5. 删除主表数据
      int deleteCount = deviceMapper.deleteByPrimaryKey(id);
      alarmContextCache.publishPlatformChange(instanceKey);
      log.info("[设备删除] 删除主表: deviceId={}, 结果={}", id, deleteCount);

      log.info("[设备删除] 删除成功: deviceId={}, 通道={}, 扩展={}", id, channelCount, extCount);

      // 构建返回结果
      java.util.Map<String, Object> resultData = new java.util.HashMap<>();
      resultData.put("deletedChannels", channelCount);
      resultData.put("deletedExtensions", extCount);
      resultData.put("deletedIotDevice", iotDeleteResult.getData());

      return R.ok(resultData);

    } catch (Exception e) {
      log.error("[设备删除] 删除失败: deviceId={}, error={}", id, e.getMessage(), e);
      throw new RuntimeException("删除设备失败: " + e.getMessage(), e);
    }
  }

  /**
   * 删除关联的IoT设备
   *
   * @param instanceKey 视频平台实例Key（对应IoT设备的productKey）
   * @param deviceId 设备ID（对应IoT设备的deviceId）
   * @return 删除结果
   */
  private R<Boolean> deleteRelatedIotDevice(String instanceKey, String deviceId) {
    // 查询关联的IoT设备
    Example example = new Example(IoTDevice.class);
    example.createCriteria().andEqualTo("productKey", instanceKey).andEqualTo("deviceId", deviceId);

    List<IoTDevice> iotDevices = iotDeviceMapper.selectByExample(example);

    if (iotDevices == null || iotDevices.isEmpty()) {
      log.info("[设备删除] 未找到关联的IoT设备: productKey={}, deviceId={}", instanceKey, deviceId);
      return R.ok(false);
    }

    // 删除IoT设备(会级联删除影子表)
    String[] iotDeviceIds =
        iotDevices.stream().map(d -> d.getId().toString()).toArray(String[]::new);

    log.info(
        "[设备删除] 删除关联IoT设备: productKey={}, deviceId={}, iotDeviceIds={}",
        instanceKey,
        deviceId,
        Arrays.toString(iotDeviceIds));

    IoTProduct product = iotProductMapper.getProductByProductKey(instanceKey);
    if (product != null && isDirectSdkPlatform(product.getThirdPlatform())) {
      boolean deleted = false;
      for (IoTDevice device : iotDevices) {
        deleted |= iotDeviceLifecycleService.delDevInstance(device.getIotId()) > 0;
      }
      return R.ok(deleted);
    }

    R result = iotDeviceService.deleteDevInstanceByIds(iotDeviceIds);
    log.info("[设备删除] IoT设备删除结果: {}", result.isSuccess() ? "成功" : "失败");

    if (result.isSuccess()) {
      return R.ok(true);
    } else {
      return R.error(result.getMsg(), false);
    }
  }

  /**
   * 删除通道数据
   *
   * @param deviceId 设备主键ID
   * @return 删除数量
   */
  private int deleteChannels(Long deviceId) {
    Example example = new Example(VideoPlatformChannel.class);
    example.createCriteria().andEqualTo("deviceId", deviceId);
    return channelMapper.deleteByExample(example);
  }

  /**
   * 删除扩展数据
   *
   * @param deviceId 设备主键ID
   * @return 删除数量
   */
  private int deleteDeviceExt(Long deviceId) {
    Example example = new Example(VideoPlatformDeviceExt.class);
    example.createCriteria().andEqualTo("deviceId", deviceId);
    return deviceExtMapper.deleteByExample(example);
  }

  private boolean isDirectSdkPlatform(String platformType) {
    return "hik_sdk".equalsIgnoreCase(platformType) || "dahua_sdk".equalsIgnoreCase(platformType) || "tiandy_sdk".equalsIgnoreCase(platformType) || "uniview_sdk".equalsIgnoreCase(platformType);
  }
}
