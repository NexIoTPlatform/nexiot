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

package cn.universal.mqtt.protocol.processor;

import cn.universal.common.domain.R;
import cn.universal.dm.device.service.plugin.BaseMessageProcessor;
import cn.universal.mqtt.protocol.entity.MQTTDownRequest;

/**
 * MQTT消息下行处理器接口
 *
 * <p>继承通用的BaseMessageProcessor，定义MQTT模块特有的处理方法 各MQTT处理器实现此接口，提供具体的处理逻辑
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
public interface MQTTDownMessageProcessor extends BaseMessageProcessor {

  /**
   * 处理MQTT消息
   *
   * @param request MQTT上行请求对象
   * @return 处理结果
   */
  R<?> process(MQTTDownRequest request);

  /**
   * 是否支持处理该消息
   *
   * @param request MQTT上行请求对象
   * @return true表示支持，false表示不支持
   */
  boolean supports(MQTTDownRequest request);

  /** 处理前的预检查（可选） */
  default boolean preCheck(MQTTDownRequest request) {
    return true;
  }

  /** 处理后的后置操作（可选） */
  default void postProcess(MQTTDownRequest request, R<?> result) {
    // 默认不做任何操作
  }
}
