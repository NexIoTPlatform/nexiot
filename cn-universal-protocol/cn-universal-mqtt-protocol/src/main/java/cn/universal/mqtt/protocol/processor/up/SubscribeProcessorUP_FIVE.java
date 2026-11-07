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

package cn.universal.mqtt.protocol.processor.up;

import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.up.common.BaseSubscribeProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MQTT解码处理器
 *
 * <p>MQTTUPRequest，负责将MQTT协议转换
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Component
public class SubscribeProcessorUP_FIVE extends BaseSubscribeProcessor {

  @Override
  protected String getTopicType() {
    return "消息订阅";
  }

  @Override
  protected boolean processTopicSpecificSubscribe(MQTTUPRequest request) {
    return true;
  }
}
