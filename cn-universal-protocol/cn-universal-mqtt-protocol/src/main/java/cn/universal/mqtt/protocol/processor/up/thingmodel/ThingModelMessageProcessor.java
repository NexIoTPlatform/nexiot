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

import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.mqtt.protocol.config.MqttConstant;
import cn.universal.mqtt.protocol.config.MqttConstant.TopicCategory;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 物模型消息转换处理器
 *
 * <p>替代编解码步骤THREE，专门处理物模型格式的消息转换： - 将物模型JSON消息转换为BaseUPRequest列表 - 设置正确的消息类型和数据结构 - 处理属性和事件的不同格式
 *
 * <p>物模型特点： - 无需编解码，直接解析JSON - 消息格式标准化且结构清晰 - 支持属性批量上报和单一事件上报
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Component
public class ThingModelMessageProcessor extends AbstratIoTService implements MqttMessageProcessor {

  @Autowired private MQTTTopicManager topicManager;

  @Override
  public String getName() {
    return "物模型消息转换处理器";
  }

  @Override
  public String getDescription() {
    return "将物模型JSON消息转换为BaseUPRequest列表";
  }

  @Override
  public int getOrder() {
    return 300; // 消息转换处理是第三步
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    if (request.getUpTopic() == null || request.getPayload() == null) {
      return false;
    }

    // 优先检查第三方MQTT配置的主题分类
    MqttConstant.TopicCategory configuredCategory = request.getContextValue("topicCategory");
    if (configuredCategory != null) {
      boolean supported = configuredCategory == MqttConstant.TopicCategory.THING_MODEL;
      if (supported) {
        log.debug("[{}] 通过配置的主题分类匹配: category={}", getName(), configuredCategory);
      }
      return supported;
    }

    // 回退到系统内置的主题匹配逻辑
    boolean match =
        TopicCategory.THING_MODEL.equals(MQTTTopicManager.matchCategory(request.getUpTopic()));
    return match;
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      log.debug("[{}] 开始处理物模型消息转换，设备: {}", getName(), request.getDeviceId());

      // 1. 获取消息JSON
      JSONObject messageJson = (JSONObject) request.getContextValue("messageJson");
      if (messageJson == null) {
        log.error("[{}] 消息JSON为空", getName());
        return ProcessorResult.ERROR;
      }

      // 2. 根据主题类型转换消息
      List<BaseUPRequest> upRequestList = convertThingModelMessage(request, messageJson);
      if (upRequestList == null || upRequestList.isEmpty()) {
        log.error("[{}] 消息转换失败", getName());
        return ProcessorResult.ERROR;
      }

      // 3. 设置转换结果
      request.setUpRequestList(upRequestList);
      request.setContextValue("convertedRequestCount", upRequestList.size());
      request.setContextValue("messageConverted", true);

      log.debug("[{}] 物模型消息转换完成，生成请求数量: {}", getName(), upRequestList.size());
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] 物模型消息转换异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
      return ProcessorResult.ERROR;
    }
  }

  /** 转换物模型消息 */
  private List<BaseUPRequest> convertThingModelMessage(
      MQTTUPRequest request, JSONObject messageJson) {
    try {

      // 对于物模型消息，根据消息内容判断是属性还是事件
      // 如果包含"event"字段，则为事件消息；否则为属性消息
      if (messageJson.containsKey("event")) {
        return convertEventMessage(request, messageJson);
      } else {
        return convertPropertyMessage(request, messageJson);
      }

    } catch (Exception e) {
      log.error("[{}] 物模型消息转换异常: ", getName(), e);
      return null;
    }
  }

  /** 转换属性消息 格式：{"battery": "99", "ecl": "30", "switchStatus": 0} */
  private List<BaseUPRequest> convertPropertyMessage(
      MQTTUPRequest request, JSONObject messageJson) {
    try {
      if (request.getUpRequestList() != null && request.getUpRequestList().size() > 0) {
        return request.getUpRequestList();
      }
      List<BaseUPRequest> upRequestList = new ArrayList<>();

      if (messageJson == null || messageJson.isEmpty()) {
        log.warn("[{}] 属性消息为空", getName());
        return upRequestList;
      }
      // 直接解析messageJson的所有键值对作为属性
      BaseUPRequest upRequest = getBaseUPRequest(request.getIoTDeviceDTO()).build();
      upRequest.setMessageType(MessageType.PROPERTIES);
      upRequest.setProperties(messageJson);
      upRequestList.add(upRequest);

      log.debug("[{}] 属性消息转换完成，属性数量: {}", getName(), upRequestList.size());
      return upRequestList;

    } catch (Exception e) {
      log.error("[{}] 属性消息转换异常: ", getName(), e);
      return null;
    }
  }

  /** 转换事件消息 格式：{"messageType": "EVENT", "event": "online"} */
  private List<BaseUPRequest> convertEventMessage(MQTTUPRequest request, JSONObject messageJson) {
    try {
      if (request.getUpRequestList() != null && request.getUpRequestList().size() > 0) {
        return request.getUpRequestList();
      }
      List<BaseUPRequest> upRequestList = new ArrayList<>();

      String eventType = messageJson.getStr("event");
      if (eventType == null || eventType.trim().isEmpty()) {
        log.warn("[{}] 事件消息无有效事件类型", getName());
        return upRequestList;
      }

      BaseUPRequest upRequest = getBaseUPRequest(request.getIoTDeviceDTO()).build();
      upRequest.setMessageType(MessageType.EVENT);
      upRequest.setEvent(eventType);

      // 设置事件数据（如果存在）
      JSONObject eventData = messageJson.getJSONObject("data");
      if (eventData != null) {
        upRequest.setData(eventData);
      }

      // 设置事件级别（如果存在）

      upRequestList.add(upRequest);

      log.debug("[{}] 事件消息转换完成，事件类型: {}", getName(), eventType);
      return upRequestList;

    } catch (Exception e) {
      log.error("[{}] 事件消息转换异常: ", getName(), e);
      return null;
    }
  }

  @Override
  public boolean preCheck(MQTTUPRequest request) {
    // 检查必要的数据
    return request.getIoTDeviceDTO() != null
        && request.getIoTProduct() != null
        && request.getContextValue("messageJson") != null;
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      Integer convertedCount = (Integer) request.getContextValue("convertedRequestCount");
      log.debug(
          "[{}] 物模型消息转换成功 - 设备: {}, 生成请求: {}",
          getName(),
          request.getDeviceId(),
          convertedCount != null ? convertedCount : 0);
    } else {
      log.warn("[{}] 物模型消息转换失败 - 设备: {}, 结果: {}", getName(), request.getDeviceId(), result);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    log.error("[{}] 物模型消息转换异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
    request.setError("物模型消息转换失败: " + e.getMessage());
  }

  @Override
  public int getPriority() {
    return 10; // 物模型处理优先级最高
  }
}
