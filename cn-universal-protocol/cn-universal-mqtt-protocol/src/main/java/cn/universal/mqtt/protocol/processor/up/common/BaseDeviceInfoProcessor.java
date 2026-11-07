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

package cn.universal.mqtt.protocol.processor.up.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.mqtt.protocol.config.MqttConstant.TopicCategory;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.entity.ProcessingStage;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.IoTDeviceQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 公共设备信息处理器基类
 *
 * <p>步骤ONE：实现设备 IoTDeviceDTO 和产品IoTProduct的信息回填，确保上报的消息产品或设备是存在的
 *
 * <p>三种主题类型的公共处理逻辑： - 设备和产品信息查询和回填 - 消息基础信息提取 - 主题解析和设备标识提取
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
public abstract class BaseDeviceInfoProcessor extends AbstratIoTService
    implements MqttMessageProcessor {

  @Autowired private MQTTTopicManager topicManager;

  @Override
  public String getName() {
    return "设备信息处理器-" + getTopicType();
  }

  @Override
  public String getDescription() {
    return "处理" + getTopicType() + "主题的设备和产品信息回填";
  }

  @Override
  public int getOrder() {
    return 100; // 设备信息处理是第一步
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      log.debug(
          "[{}] 开始处理设备信息，主题: {}, 消息ID: {}",
          getName(),
          request.getUpTopic(),
          request.getMessageId());

      // 1. 解析主题和提取设备信息
      if (!parseTopicAndExtractDevice(request)) {
        log.error("[{}] 主题解析失败", getName());
        return ProcessorResult.ERROR;
      }

      // 2. 查询和回填产品信息
      if (!fillProductInfo(request)) {
        log.error("[{}] 产品信息回填失败", getName());
        return ProcessorResult.ERROR;
      }

      // 3. 查询和回填设备信息
      if (!fillDeviceInfo(request)) {
        log.error("[{}] 设备信息回填失败", getName());
        return ProcessorResult.ERROR;
      }

      // 4. 解码payload
      if (!decodePayload(request)) {
        log.error("[{}] 消息解码失败", getName());
        return ProcessorResult.ERROR;
      }
      // 物模型
      processTopicSpecificInfo(request);

      // 5. 提取基础信息
      extractBasicInfo(request);

      // 7. 更新处理阶段
      request.setStage(ProcessingStage.DEVICE_EXTRACTED);

      log.debug(
          "[{}] 设备信息处理完成，产品: {}, 设备: {}",
          getName(),
          request.getProductKey(),
          request.getDeviceId());
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] 设备信息处理异常，主题: {}, 异常: ", getName(), request.getUpTopic(), e);
      return ProcessorResult.ERROR;
    }
  }

  /** 解析主题并提取设备信息 */
  protected boolean parseTopicAndExtractDevice(MQTTUPRequest request) {
    try {
      String topic = request.getUpTopic();
      if (StrUtil.isBlank(topic)) {
        log.warn("[{}] 主题为空", getName());
        return false;
      }
      // 调用子类的主题解析逻辑
      if (!parseTopicForThisType(request, topic)) {
        log.warn("[{}] 主题解析失败: {}", getName(), topic);
        return false;
      }
      // 验证提取的设备信息
      if (StrUtil.isBlank(request.getProductKey()) || StrUtil.isBlank(request.getDeviceId())) {
        log.warn(
            "[{}] 设备信息提取不完整，产品: {}, 设备: {}",
            getName(),
            request.getProductKey(),
            request.getDeviceId());
        return false;
      }

      // 设置设备唯一标识
      String deviceUniqueId = request.getProductKey() + ":" + request.getDeviceId();
      request.setDeviceUniqueId(deviceUniqueId);

      log.debug(
          "[{}] 主题解析成功，产品: {}, 设备: {}", getName(), request.getProductKey(), request.getDeviceId());
      return true;

    } catch (Exception e) {
      log.error("[{}] 主题解析异常: ", getName(), e);
      return false;
    }
  }

  /** 回填产品信息 */
  protected boolean fillProductInfo(MQTTUPRequest request) {
    try {
      String productKey = request.getProductKey();
      IoTProduct ioTProduct = getProduct(productKey);

      if (ioTProduct == null) {
        log.warn("[{}] 产品不存在: {}", getName(), productKey);
        return false;
      }
      if (StrUtil.isNotBlank(ioTProduct.getConfiguration())
          && JSONUtil.isTypeJSON(ioTProduct.getConfiguration())) {
        JSONObject config = JSONUtil.parseObj(ioTProduct.getConfiguration());
        request.setAllowInsert(config.getBool(IoTConstant.ALLOW_INSERT, true));
      }
      request.setIoTProduct(ioTProduct);
      request.setContextValue("productInfo", ioTProduct);

      log.debug("[{}] 产品信息回填成功: {}", getName(), productKey);
      return true;

    } catch (Exception e) {
      log.error("[{}] 产品信息回填异常: ", getName(), e);
      return false;
    }
  }

  /** 回填设备信息 */
  protected boolean fillDeviceInfo(MQTTUPRequest request) {
    try {
      IoTDeviceDTO ioTDeviceDTO =
          lifeCycleDevInstance(
              IoTDeviceQuery.builder()
                  .deviceId(request.getDeviceId())
                  .productKey(request.getProductKey())
                  .build());

      request.setIoTDeviceDTO(ioTDeviceDTO);
      request.setContextValue("deviceInfo", ioTDeviceDTO);

      if (ioTDeviceDTO != null) {
        log.debug("[{}] 设备信息回填成功: {}", getName(), request.getDeviceId());
      } else {
        log.debug("[{}] 设备不存在，可能需要自动注册: {}", getName(), request.getDeviceId());
      }

      return true;

    } catch (Exception e) {
      log.error("[{}] 设备信息回填异常: ", getName(), e);
      return false;
    }
  }

  /** 解码payload */
  protected boolean decodePayload(MQTTUPRequest request) {
    try {
      // 转换为字符串
      request.setPayload(request.getPayload());
      return true;
    } catch (Exception e) {
      log.error("[{}] 消息解码异常: ", getName(), e);
      return false;
    }
  }

  /** 提取基础信息 */
  protected void extractBasicInfo(MQTTUPRequest request) {
    try {

      // 设置基础上下文信息
      request.setContextValue("protocol", "MQTT");
      request.setContextValue("version", "2.0");
      request.setContextValue("topicType", getTopicType());
      request.setContextValue("messageId", request.getMessageId());
      request.setContextValue("qos", request.getQos());
      request.setContextValue("retained", request.isRetained());

      log.debug("[{}] 基础信息提取完成", getName());

    } catch (Exception e) {
      log.warn("[{}] 基础信息提取异常: ", getName(), e);
    }
  }

  private boolean processTopicSpecificInfo(MQTTUPRequest request) {
    try {

      String topic = request.getUpTopic();
      if (StrUtil.isBlank(topic)) {
        log.warn("[{}] 主题为空", getName());
        return false;
      }
      // 1. 解析消息内容
      if (parseThingModelMessage(request)) {
        // 3. 提取物模型信息
        extractThingModelInfo(request);
      }

      log.debug("[{}] 物模型特定信息处理完成", getName());
      return true;

    } catch (Exception e) {
      log.error("[{}] 物模型特定信息处理异常: ", getName(), e);
      return false;
    }
  }

  /** 解析物模型消息内容 */
  private boolean parseThingModelMessage(MQTTUPRequest request) {
    try {
      String payload = request.getPayload();
      if (StrUtil.isBlank(payload)) {
        return false;
      }
      if (!TopicCategory.THING_MODEL.equals(request.getTopicCategory())) {
        // 不是物模型，直接返回
        return false;
      }
      // 解析JSON消息
      JSONObject messageJson;
      try {
        messageJson = JSONUtil.parseObj(payload);
      } catch (Exception e) {
        log.warn("[{}] 物模型消息不是有效的JSON格式: {}", getName(), payload);
        return false;
      }
      request.setContextValue("messageJson", messageJson);
      request.setContextValue("messageSize", payload.length());

      log.debug("[{}] 物模型消息解析成功，大小: {}字节", getName(), payload.length());

      return true;

    } catch (Exception e) {
      log.error("[{}] 物模型消息解析异常: ", getName(), e);
      return false;
    }
  }

  protected boolean parseTopicForThisType(MQTTUPRequest request, String topic) {
    try {
      // 使用TopicManager解析物模型主题
      MQTTTopicManager.TopicInfo topicInfo = topicManager.extractTopicInfo(topic);

      if (!topicInfo.isValid()) {
        log.warn("[{}] 物模型主题格式无效: {}", getName(), topic);
        return false;
      }
      // 设置设备信息
      request.setProductKey(topicInfo.getProductKey());
      request.setDeviceId(topicInfo.getDeviceId());
      // 设置物模型特定上下文
      request.setContextValue("topicInfo", topicInfo);
      request.setContextValue("topicType", topicInfo.getTopicType());

      log.debug(
          "[{}] 物模型主题解析成功 - 类型: {}, 产品: {}, 设备: {}",
          getName(),
          topicInfo.getTopicType(),
          topicInfo.getProductKey(),
          topicInfo.getDeviceId());

      return true;

    } catch (Exception e) {
      log.error("[{}] 物模型主题解析异常: ", getName(), e);
      return false;
    }
  }

  /** 提取物模型信息 */
  private void extractThingModelInfo(MQTTUPRequest request) {
    try {
      JSONObject messageJson = (JSONObject) request.getContextValue("messageJson");
      MQTTTopicManager.TopicInfo topicInfo =
          (MQTTTopicManager.TopicInfo) request.getContextValue("topicInfo");
      // 根据主题类型提取特定信息
      switch (topicInfo.getTopicType()) {
        case THING_PROPERTY_UP:
          extractPropertyInfo(request, messageJson);
          break;
        case THING_EVENT_UP:
          extractEventInfo(request, messageJson);
          break;
        case THING_DOWN:
          extractDownstreamInfo(request, messageJson);
          break;
      }

      // 设置处理标识
      request.setContextValue("isThingModel", true);
      request.setContextValue("needDecode", false); // 物模型不需要编解码

      log.debug("[{}] 物模型信息提取完成 - 消息类型: {}", getName(), topicInfo.getTopicType());

    } catch (Exception e) {
      log.warn("[{}] 物模型信息提取异常: ", getName(), e);
    }
  }

  /** 提取属性信息 */
  private void extractPropertyInfo(MQTTUPRequest request, JSONObject messageJson) {
    request.setProperties(messageJson);
  }

  /** 提取事件信息 */
  private void extractEventInfo(MQTTUPRequest request, JSONObject messageJson) {
    String event = messageJson.getStr("event");
    if (event != null) {
      request.setEvent(event);
    }
    JSONObject data = messageJson.getJSONObject("data");
    if (data != null) {
      request.setData(data);
    }
  }

  /** 提取下行信息 */
  private void extractDownstreamInfo(MQTTUPRequest request, JSONObject messageJson) {
    // 提取下行命令信息
    log.debug("[{}] 下行信息提取完成 - 命令: {}", getName());
  }

  // ==================== 抽象方法，由子类实现 ====================

  /** 获取主题类型名称 */
  protected abstract String getTopicType();

  // ==================== 生命周期方法 ====================

  @Override
  public boolean preCheck(MQTTUPRequest request) {
    // 检查必要的数据
    return request.getUpTopic() != null && request.getPayload() != null;
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      log.debug(
          "[{}] 设备信息处理成功 - 产品: {}, 设备: {}",
          getName(),
          request.getProductKey(),
          request.getDeviceId());
    } else {
      log.warn("[{}] 设备信息处理失败 - 主题: {}, 结果: {}", getName(), request.getUpTopic(), result);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    log.error("[{}] 设备信息处理异常，主题: {}, 异常: ", getName(), request.getUpTopic(), e);
    request.setError("设备信息处理失败: " + e.getMessage());
  }
}
