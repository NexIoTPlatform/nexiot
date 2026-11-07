/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台产品自动创建服务
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.service;

import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformInstance;

/** 视频平台产品创建策略服务 */
public interface VideoPlatformProductService {

  /**
   * 为WVP平台自动创建GB/级联两个产品（如果autoCreateProducts=1且尚未创建）
   *
   * @param instance 平台实例
   */
  void autoCreateWvpProducts(VideoPlatformInstance instance);

  /**
   * 懒创建平台产品（HIK/ICC/WVP通用）
   *
   * @param instance 平台实例
   * @param deviceNode 设备节点类型：VIDEO_GATEWAY/VIDEO_DEVICE等
   * @return 平台产品（用于后续设备落库时填充gwProductKey）
   */
  IoTProduct ensurePlatformProduct(VideoPlatformInstance instance, String deviceNode);

  /** 告警只读取已存在产品并补齐事件，禁止由匿名回调重新创建设备产品。 */
  IoTProduct prepareAlarmProduct(VideoPlatformInstance instance);

  /**
   * 根据instanceKey获取或创建平台产品
   *
   * @param instanceKey 平台实例Key
   * @param deviceNode 设备节点类型
   * @return 平台产品
   */
  IoTProduct getOrCreatePlatformProduct(String instanceKey, String deviceNode);
}
