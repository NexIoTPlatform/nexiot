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

package cn.universal.persistence.base;

import java.util.List;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/8/10
 */
public interface IoTUPWrapper<T> {

  /**
   * 推送前置处理，规则引擎，场景联动
   *
   * @param downRequests 消息原文
   */
  default void beforePush(List<T> downRequests) {}

  /**
   * mqtt 推送
   *
   * @param topic 主题
   * @param message 消息
   */
  default void mqttPush(String topic, String message) {}

  default void tcpPush(String applicationId, String productKey, String message) {}
}
