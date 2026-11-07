/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频设备导入服务
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.service;

import java.util.List;
import java.util.Map;

/** 视频设备导入服务 从三方平台拉取设备后，勾选使用时落库到IoTDevice */
public interface VideoDeviceImportService {

  /**
   * 批量导入设备（勾选使用流程）
   *
   * @param instanceKey 平台实例Key
   * @param devices 设备列表（从适配器返回的原始设备数据）
   * @param productKey 指定产品Key（可选，为空则自动创建/选择平台产品）
   * @return 导入结果 {success: 成功数, failed: 失败数, exists: 已存在数}
   */
  Map<String, Integer> importDevices(
      String instanceKey, List<Map<String, Object>> devices, String productKey);

  /**
   * 单个设备导入
   *
   * @param instanceKey 平台实例Key
   * @param device 设备数据
   * @param productKey 指定产品Key
   * @return 导入成功的设备ID
   */
  String importDevice(String instanceKey, Map<String, Object> device, String productKey);

  /**
   * 更新已存在设备的在线状态和配置（同步时调用）
   *
   * @param instanceKey 平台实例Key
   * @param device 设备数据
   * @param platformType 平台类型
   * @return 更新成功返回true
   */
  boolean updateDeviceStatusAndConfig(String instanceKey, Map<String, Object> device, String platformType);
}
