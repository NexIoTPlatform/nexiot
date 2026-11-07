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

package cn.universal.mqtt.protocol.processor.up.thingmodel;

import cn.universal.mqtt.protocol.config.MqttConstant;
import cn.universal.mqtt.protocol.config.MqttConstant.TopicCategory;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.up.common.BaseDeviceInfoProcessor;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 物模型主题设备信息处理器
 *
 * <p>处理物模型主题的设备信息提取和回填： - $thing/up/property/${productKey}/${deviceId} (属性上报) -
 * $thing/up/event/${productKey}/${deviceId} (事件上报) - $thing/down/${productKey}/${deviceId} (下行控制)
 *
 * <p>物模型特点： - 消息格式标准化，直接可用不需要编解码 - 支持属性和事件两种消息类型 - 消息体已经是JSON格式的物模型数据
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Component
public class ThingModelDeviceInfoProcessor extends BaseDeviceInfoProcessor {

  @Autowired private MQTTTopicManager topicManager;

  @Override
  protected String getTopicType() {
    return "物模型";
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    if (request.getUpTopic() == null
        || request.getPayload() == null
        || !request.isSysMQTTBroker()) {
      return false;
    }
    TopicCategory topicCategory = request.getTopicCategory();
    if (topicCategory != null) {
      return MqttConstant.TopicCategory.THING_MODEL.equals(topicCategory);
    }
    topicCategory = MQTTTopicManager.matchCategory(request.getUpTopic());
    request.setTopicCategory(topicCategory);
    return MqttConstant.TopicCategory.THING_MODEL.equals(topicCategory);
  }

  @Override
  public int getPriority() {
    return 10; // 物模型处理优先级最高
  }
}
