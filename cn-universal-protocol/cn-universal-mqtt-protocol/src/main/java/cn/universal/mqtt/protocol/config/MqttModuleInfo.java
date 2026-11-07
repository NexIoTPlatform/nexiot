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

package cn.universal.mqtt.protocol.config;

import cn.universal.core.protocol.ProtocolModuleInfo;
import org.springframework.stereotype.Component;

/**
 * MQTT 协议模块信息
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/2
 */
@Component
public class MqttModuleInfo implements ProtocolModuleInfo {

  @Override
  public String getCode() {
    return "mqtt";
  }

  @Override
  public String getName() {
    return "MQTT";
  }

  @Override
  public String getDescription() {
    return "使用EMQX作为内置MQTT，轻量级的发布/订阅消息传输";
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  @Override
  public String getVendor() {
    return "Universal IoT";
  }

  @Override
  public boolean isCore() {
    return true; // MQTT是核心协议
  }

  @Override
  public ProtocolCategory getCategory() {
    return ProtocolCategory.MESSAGING;
  }
}
