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

package cn.universal.mqtt.protocol.processor.up.passthrough;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.core.message.UPRequest;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.mqtt.protocol.config.MqttConstant.TopicCategory;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.IoTDeviceQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 第三方自定义MQTT设备信息处理器
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/11/26
 */
@Slf4j(topic = "mqtt")
@Component
public class ThirdMQTTDeviceInfoProcessor extends AbstratIoTService
    implements MqttMessageProcessor {

  @Autowired private ThirdMQTTServerManager thirdMQTTServerManager;

  @Override
  public String getName() {
    return "第三方自定义MQTT设备解析";
  }

  @Override
  public String getDescription() {
    return "针对无法修改的主题做适配";
  }

  @Override
  public int getOrder() {
    return 50; // 第三方透传设备
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    if (request.getUpTopic() == null || request.getPayload() == null || request.isSysMQTTBroker()) {
      return false;
    }
    return true;
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      log.debug("[{}] 开始处理，设备topic: {}", getName(), request.getUpTopic());

      // 0. 设置主题分类（在回填产品信息之前，因为需要networkUnionId）
      if (!setTopicCategory(request)) {
        log.error(
            "[{}] 设置主题分类失败: networkUnionId={}, productKey={}, topic={}, category={}",
            getName(),
            request.getNetworkUnionId(),
            request.getProductKey(),
            request.getUpTopic(),
            request.getTopicCategory());
        return ProcessorResult.ERROR;
      }

      // 1. 查询和回填产品信息
      if (!fillProductInfo(request)) {
        log.error("[{}] 产品信息回填失败", getName());
        return ProcessorResult.ERROR;
      }
      // 执行预解码获取设备ID
      UPRequest upRequest =
          preDecode(request.getProductKey(), request.getPayload(), request.getUpTopic());
      log.info("[{}] 开始deviceId识别,执行[preDecode],返回={}", getName(), JSONUtil.toJsonStr(upRequest));
      // 如果开启了主动注册，但是没有写编解码或者返回为null，那么把消息丢给设备默认设备Id,便于调试
      if (upRequest == null && request.isAllowInsert()) {
        request.setDeviceId(IoTConstant.NEXIOT_DEBUG_DEVICE_ID);
      } else if (upRequest != null && StrUtil.isNotBlank(upRequest.getDeviceId())) {
        request.setDeviceId(upRequest.getDeviceId());
      } else {
        log.info("[{}] deviceId preDecode 编解码不存在，暂停解析", getName());
        return ProcessorResult.STOP;
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
      return ProcessorResult.CONTINUE;
    } catch (Exception e) {
      log.error("[{}] 透传编解码处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
      return ProcessorResult.ERROR;
    }
  }

  /** 设置主题分类（物模型/透传） */
  private boolean setTopicCategory(MQTTUPRequest request) {
    try {
      String productKey = request.getProductKey();

      if (StrUtil.isBlank(productKey)) {
        log.debug("[{}] 无法设置主题分类：productKey为空", getName());
        return false;
      }

      // 接收端已按实际 Topic 确定分类。保留请求快照，避免重连或配置更新影响在途消息。
      TopicCategory topicCategory = request.getTopicCategory();
      if (topicCategory == null || topicCategory == TopicCategory.UNKNOWN) {
        topicCategory =
            thirdMQTTServerManager.resolveTopicCategory(
                request.getNetworkUnionId(), request.getUpTopic());
      }
      if (topicCategory == null || topicCategory == TopicCategory.UNKNOWN) {
        return false;
      }
      request.setContextValue("topicCategory", topicCategory);
      request.setTopicCategory(topicCategory);
      return true;
    } catch (Exception e) {
      log.warn("[{}] 设置主题分类异常: ", getName(), e);
    }
    return false;
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
    if (StrUtil.isBlank(request.getProductKey()) && StrUtil.isBlank(request.getProductKey())) {
      log.warn(
          "[{}] 查询设备失败，ProductKey或deviceId为空，deviceId={},ProductKey={}",
          request.getDeviceId(),
          request.getProductKey());
      return false;
    }
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
      if (TopicCategory.THING_MODEL.equals(request.getTopicCategory())) {
        request.setContextValue("topicCategory", request.getTopicCategory());
        JSONObject messageJson = JSONUtil.parseObj(request.getPayload());
        request.setContextValue("messageJson", messageJson);
      }
      return true;
    } catch (Exception e) {
      log.error("[{}] 消息解码异常: ", getName(), e);
      return false;
    }
  }

  /** 统计编解码结果 */
  private void collectCodecStatistics(MQTTUPRequest request) {
    try {
      Boolean codecSuccess = (Boolean) request.getContextValue("codecSuccess");
      String codecType = (String) request.getContextValue("codecType");
      Integer processedCount = (Integer) request.getContextValue("codecProcessedCount");

      request.setContextValue(
          "codecStatistics",
          "成功: " + codecSuccess + ", 类型: " + codecType + ", 数量: " + processedCount);

      log.debug(
          "[{}] 编解码统计 - 成功: {}, 类型: {}, 数量: {}",
          getName(),
          codecSuccess,
          codecType,
          processedCount != null ? processedCount : 0);

    } catch (Exception e) {
      log.warn("[{}] 编解码统计异常: ", getName(), e);
    }
  }

  @Override
  public boolean preCheck(MQTTUPRequest request) {
    // 检查必要的数据
    return !request.isSysMQTTBroker();
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      // 收集编解码统计信息
      collectCodecStatistics(request);

      Integer processedCount = (Integer) request.getContextValue("codecProcessedCount");
      Boolean codecSuccess = (Boolean) request.getContextValue("codecSuccess");

      log.debug(
          "[{}] 透传编解码处理成功 - 设备: {}, 成功: {}, 生成请求: {}",
          getName(),
          request.getDeviceId(),
          codecSuccess,
          processedCount != null ? processedCount : 0);
    } else {
      log.warn("[{}] 透传编解码处理失败 - 设备: {}, 结果: {}", getName(), request.getDeviceId(), result);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    log.error("[{}] 透传编解码处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
    request.setError("透传编解码处理失败: " + e.getMessage());
  }

  @Override
  public int getPriority() {
    return 1; // 优先级极高
  }
}
