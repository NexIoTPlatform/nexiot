package cn.universal.mqtt.protocol.processor.up.common;

import cn.hutool.core.util.StrUtil;
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
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 公共消息回复处理器基类
 *
 * <p>统一处理所有需要自动回复的MQTT消息（如物模型/透传下行等）
 */
@Slf4j(topic = "mqtt")
public abstract class BaseReplyProcessor extends AbstratIoTService implements MqttMessageProcessor {

  @Autowired protected ThirdMQTTServerManager thirdMqttServerManager;
  @Autowired protected SysMQTTManager sysMQTTManager;
  @Autowired protected MqttRouteResolver mqttRouteResolver;

  @Autowired protected MQTTTopicManager mqttTopicManager;
  @Resource private IoTProductDeviceService ioTProductDeviceService;

  @Override
  public String getName() {
    return "消息回复处理器-" + getTopicType();
  }

  @Override
  public String getDescription() {
    return "处理" + getTopicType() + "主题的自动消息回复";
  }

  @Override
  public int getOrder() {
    return 800; // 回复处理一般在业务处理后
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      // 已禁用：统一由 MqttUPBatchAckUPProcessor 处理批量回复，避免重复发送
      log.debug("[{}] 已禁用，由批量回复处理器统一处理", getName());
      return ProcessorResult.CONTINUE;

      /* 原逻辑已禁用
      String replyPayload = request.getReplyPayload();
      if (StrUtil.isBlank(replyPayload)) {
        log.debug("[{}] 无需回复，跳过", getName());
        return ProcessorResult.CONTINUE;
      }
      String downTopic = buildDownTopic(request);
      // 根据产品配置的 encoderType 编码 payload
      String productKey = request.getProductKey();
      String encoderType = ioTProductDeviceService.getProductEncoderType(productKey);
      byte[] payloadBytes = PayloadCodecUtils.encode(encoderType, replyPayload);

      MQTTPublishMessage build =
          MQTTPublishMessage.builder()
              .topic(downTopic)
              .qos(request.getQos())
              .payload(payloadBytes)
              .build();
      boolean success = publishWithSystemOrThirdMqtt(request, build);
      if (success) {
        log.info(
            "[{}] 回复消息发布成功 - 主题: {} encoderType: {} 内容：{}",
            getName(),
            downTopic,
            encoderType,
            replyPayload);
        request.setContextValue("replySuccess", true);
        iIoTDeviceDataService.saveDeviceLog(
            request, request.getIoTDeviceDTO(), request.getIoTProduct());
      } else {
        log.warn("[{}] 回复消息发布失败 - 主题: {}", getName(), downTopic);
        request.setContextValue("replySuccess", false);
      }
      // 主题类型特定的后续处理
      if (!processTopicSpecificReply(request, build)) {
        log.warn("[{}] 主题特定回复处理失败", getName());
        return ProcessorResult.ERROR;
      }

      return ProcessorResult.CONTINUE;
      */

    } catch (Exception e) {
      log.error("[{}] 回复处理异常: ", getName(), e);
      return ProcessorResult.ERROR;
    }
  }

  private String buildDownTopic(MQTTUPRequest request) {
    String downTopic = request.getDownTopic();
    if (StrUtil.isBlank(downTopic)) {
      downTopic = buildReplyTopic(request);
    } else {
      // 对已有的下发主题做占位符填充，以支持模板与通配符
      downTopic =
          mqttTopicManager.fillTopicPattern(
              downTopic, request.getProductKey(), request.getDeviceId());
    }
    return downTopic;
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    // 已禁用：统一由 MqttUPBatchAckUPProcessor 处理批量回复
    return false;
  }

  /** 构建回复topic */
  protected String buildReplyTopic(MQTTUPRequest request) {
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
  protected boolean publishWithSystemOrThirdMqtt(MQTTUPRequest request, MQTTPublishMessage reply) {
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

  /** 获取主题类型名称 */
  protected abstract String getTopicType();

  /** 主题类型特定的回复处理（可选扩展） */
  protected boolean processTopicSpecificReply(MQTTUPRequest request, MQTTPublishMessage reply) {
    return true;
  }
}
