/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.mqtt.protocol.system;

import cn.universal.mqtt.protocol.entity.MQTTProductConfig;

/**
 * 系统MQTT状态提供者接口
 *
 * <p>用于解决SystemMqttManager和MqttConfigService之间的循环依赖问题 只暴露必要的状态查询方法，避免直接依赖具体实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
public interface SysMQTTStatusProvider {

  /**
   * 系统MQTT是否启用
   *
   * @return true-启用, false-未启用
   */
  boolean isEnabled();

  /**
   * 系统MQTT是否已连接
   *
   * @return true-已连接, false-未连接
   */
  boolean isConnected();

  /**
   * 获取系统MQTT配置信息
   *
   * @return 系统MQTT配置，如果未启用则返回null
   */
  MQTTProductConfig getConfig();

  /**
   * 产品是否使用系统内置MQTT
   *
   * @param productKey
   * @return
   */
  boolean isUseSysMQTT(String productKey);
}
