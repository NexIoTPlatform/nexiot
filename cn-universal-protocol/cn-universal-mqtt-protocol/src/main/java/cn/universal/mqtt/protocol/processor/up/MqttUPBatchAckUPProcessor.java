/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权,未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.mqtt.protocol.processor.up;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.utils.PayloadCodecUtils;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.mqtt.protocol.config.MqttConstant;
import cn.universal.mqtt.protocol.entity.MQTTPublishMessage;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.mqtt.protocol.support.MqttRouteResolver;
import cn.universal.mqtt.protocol.support.MqttRouteResolver.MqttRoute;
import cn.universal.mqtt.protocol.system.SysMQTTManager;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.mqtt.protocol.topic.MQTTTopicType;
import cn.universal.persistence.base.BaseUPRequest;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQTT批量回复处理器
 *
 * <p>处理upRequestList中每个包含replyPayload的请求的批量回复
 *
 * <p>在MqttPublishUPProcessor之后执行,确保数据已推送完成
 *
 * <p>为每条回复记录独立的日志,MessageType设置为ACK
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/12/12
 */
@Slf4j(topic = "mqtt")
@Component
public class MqttUPBatchAckUPProcessor extends AbstratIoTService implements MqttMessageProcessor {

  @Autowired protected ThirdMQTTServerManager thirdMqttServerManager;
  @Autowired protected SysMQTTManager sysMQTTManager;
  @Autowired protected MqttRouteResolver mqttRouteResolver;
  @Autowired protected MQTTTopicManager mqttTopicManager;
  @Resource private IoTProductDeviceService ioTProductDeviceService;

  @Override
  public String getName() {
    return "MQTT批量回复处理器";
  }

  @Override
  public String getDescription() {
    return "处理upRequestList中每个包含replyPayload的请求的批量回复";
  }

  @Override
  public int getOrder() {
    return 2000; // 在MqttPublishUPProcessor(1999)之后执行
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    // 必须有upRequestList
    if (CollUtil.isEmpty(request.getUpRequestList())) {
      return false;
    }
    // 至少有一个请求包含replyPayload
    return request.getUpRequestList().stream()
        .anyMatch(upRequest -> ObjectUtil.isNotNull(upRequest.getReplyPayload()));
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      log.debug("[{}] 开始批量回复处理，设备: {}", getName(), request.getDeviceId());

      int replyCount = 0;
      int successCount = 0;

      for (BaseUPRequest upRequest : request.getUpRequestList()) {
        Object replyPayload = upRequest.getReplyPayload();
        if (ObjectUtil.isNull(replyPayload)) {
          continue;
        }
        // 这里如果是字符串和JSON自动兼容
        String realReplyPayload = JSONUtil.toJsonStr(replyPayload);
        replyCount++;
        boolean success = processReply(request, upRequest, realReplyPayload);
        if (success) {
          successCount++;
        }
      }

      log.info(
          "[{}] 批量回复处理完成 - 设备: {}, 总回复数: {}, 成功: {}",
          getName(),
          request.getDeviceId(),
          replyCount,
          successCount);

      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] 批量回复处理异常: ", getName(), e);
      return ProcessorResult.ERROR;
    }
  }

  /** 处理单个回复 */
  private boolean processReply(
      MQTTUPRequest request, BaseUPRequest upRequest, String replyPayload) {
    try {
      // 1. 构建下行主题
      String downTopic = buildDownTopic(request, upRequest);

      // 2. 根据产品配置的encoderType编码payload
      String productKey = request.getProductKey();
      String encoderType = ioTProductDeviceService.getProductEncoderType(productKey);
      byte[] payloadBytes = PayloadCodecUtils.encode(encoderType, replyPayload);

      // 3. 构建发布消息
      MQTTPublishMessage publishMessage =
          MQTTPublishMessage.builder()
              .topic(downTopic)
              .qos(request.getQos())
              .payload(payloadBytes)
              .build();

      // 4. 发布消息
      boolean success = publishWithSystemOrThirdMqtt(request, publishMessage);

      if (success) {
        log.info(
            "[{}] 回复消息发布成功 - 主题: {}, encoderType: {}, 内容: {}",
            getName(),
            downTopic,
            encoderType,
            replyPayload);

        // 5. 保存设备日志(MessageType为REPLY)
        saveReplyLog(request, upRequest, replyPayload);
      } else {
        log.warn("[{}] 回复消息发布失败 - 主题: {}", getName(), downTopic);
      }

      return success;

    } catch (Exception e) {
      log.error("[{}] 处理单个回复异常: ", getName(), e);
      return false;
    }
  }

  /** 构建下行主题 */
  private String buildDownTopic(MQTTUPRequest request, BaseUPRequest upRequest) {
    // 1. 优先使用upRequest中指定的downTopic
    String downTopic = upRequest.getDownTopic();
    if (StrUtil.isNotBlank(downTopic)) {
      return mqttTopicManager.fillTopicPattern(
          downTopic, request.getProductKey(), request.getDeviceId());
    }

    // 2. 优先使用request中指定的downTopic
    downTopic = request.getDownTopic();
    if (StrUtil.isNotBlank(downTopic)) {
      return mqttTopicManager.fillTopicPattern(
          downTopic, request.getProductKey(), request.getDeviceId());
    }

    // 3. 构建回复主题
    return buildReplyTopic(request);
  }

  /** 构建回复topic */
  private String buildReplyTopic(MQTTUPRequest request) {
    String productKey = request.getProductKey();
    String deviceId = request.getDeviceId();
    MqttRoute route = mqttRouteResolver.resolveReplyRoute(request);

    if (route.isSystem()) {
      return MQTTTopicType.THING_DOWN.buildTopic(productKey, deviceId);
    }

    // 1. 优先查第三方MQTT自定义下发topic
    String thirdPartyDownPattern =
        mqttTopicManager.getThirdPartyDownTopicPattern(
            route.getNetworkUnionId(), MqttConstant.TYPE_DOWN);
    if (thirdPartyDownPattern != null) {
      // 2. 使用统一的pattern填充工具生成最终下发topic
      return mqttTopicManager.fillTopicPattern(thirdPartyDownPattern, productKey, deviceId);
    }

    // 3. 兜底：仍然走默认物模型下行topic
    return MQTTTopicType.THING_DOWN.buildTopic(productKey, deviceId);
  }

  /** 根据产品类型决定推送到内置MQTT还是第三方MQTT */
  private boolean publishWithSystemOrThirdMqtt(MQTTUPRequest request, MQTTPublishMessage reply) {
    MqttRoute route = mqttRouteResolver.resolveReplyRoute(request);

    if (route.isSystem()) {
      if (!sysMQTTManager.isEnabled()) {
        log.warn("[{}] 系统MQTT未启用，无法回复，topic={}", getName(), reply.getTopic());
        return false;
      }
      return sysMQTTManager.publishMessage(
          reply.getTopic(), reply.getPayloadAsBytes(), reply.getQos(), reply.isRetained());
    }
    return thirdMqttServerManager.publishMessage(route.getNetworkUnionId(), reply);
  }

  /** 保存回复日志(MessageType设置为REPLY) */
  private void saveReplyLog(MQTTUPRequest request, BaseUPRequest upRequest, String replyPayload) {
    try {
      Map<String, Object> repayBody = new HashMap<>();
      repayBody.put("payload", JSONUtil.toJsonStr(replyPayload));
      // 创建临时请求用于记录日志
      MQTTUPRequest logRequest =
          MQTTUPRequest.builder()
              .productKey(request.getProductKey())
              .deviceId(request.getDeviceId())
              .replyPayload(JSONUtil.toJsonStr(repayBody))
              .function("decode(IDE调试器)") // 设置回复的原因
              .messageType(IoTConstant.MessageType.ACK) // 设置MessageType为REPLY
              .build();

      // 保存设备日志
      iIoTDeviceDataService.saveDeviceLog(
          logRequest, request.getIoTDeviceDTO(), request.getIoTProduct());

      log.debug("[{}] 回复日志保存成功 - 设备: {}, MessageType: ACK", getName(), request.getDeviceId());

    } catch (Exception e) {
      log.error("[{}] 保存回复日志异常: ", getName(), e);
    }
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      log.debug("[{}] 批量回复处理成功 - 设备: {}", getName(), request.getDeviceId());
    } else {
      log.warn("[{}] 批量回复处理失败 - 设备: {}, 结果: {}", getName(), request.getDeviceId(), result);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    log.error("[{}] 批量回复处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
    request.setError("批量回复处理失败: " + e.getMessage());
  }
}
