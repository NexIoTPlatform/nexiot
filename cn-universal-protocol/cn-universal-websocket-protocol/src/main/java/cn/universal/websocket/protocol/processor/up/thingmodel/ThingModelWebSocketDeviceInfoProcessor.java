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

package cn.universal.websocket.protocol.processor.up.thingmodel;

import org.springframework.stereotype.Component;

import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.processor.up.common.BaseWebSocketDeviceInfoProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket物模型消息设备信息处理器
 *
 * <p>处理物模型消息的设备信息提取和回填
 *
 * <p>物模型特点： - 消息格式标准化，直接可用不需要编解码 - 支持属性和事件两种消息类型 - 消息体已经是JSON格式的物模型数据
 *
 * @version 2.0
 * @Author gitee.com/NexIoT
 * @since 2026/1/15
 */
@Slf4j(topic = "websocket")
@Component
public class ThingModelWebSocketDeviceInfoProcessor extends BaseWebSocketDeviceInfoProcessor {

  @Override
  protected String getMessageType() {
    return "物模型";
  }

  @Override
  public boolean supports(WebSocketUPRequest request) {
    if (request == null || request.getPayload() == null) {
      return false;
    }

    // 检查产品配置 - 物模型模式：产品配置了物模型
    Object productInfo = request.getContextValue("productInfo");
    if (productInfo instanceof java.util.Map) {
      java.util.Map<String, Object> info = (java.util.Map<String, Object>) productInfo;
      String thingModel = (String) info.get("thingModel");
      // 如果产品配置了物模型，支持物模型处理
      if (cn.hutool.core.util.StrUtil.isNotBlank(thingModel)) {
        return true;
      }
      // 即使物模型未定义，也应该尝试作为降级方案处理消息
      // 这样可以支持无物模型定义场景的消息入库
      log.debug("[{}] 物模型未定义，但将作为降级方案处理消息，productKey: {}", 
          getMessageType(), request.getProductKey());
    }

    // 或者检查消息格式是否符合物模型规范
    String payload = request.getPayload();
    if (payload != null && payload.startsWith("{")) {
      try {
        com.fasterxml.jackson.databind.JsonNode node =
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
        // 物模型消息通常包含 method, params 等字段，但即使没有这些字段
        // 也应该作为通用消息处理以支持入库
        boolean isStandardThingModel = node.has("method") || node.has("properties") || node.has("events");
        if (isStandardThingModel) {
          return true;
        }
        // 降级处理：即使不是标准物模型格式，如果是JSON格式也尝试处理
        // 这样可以支持灵活的消息格式
        log.debug("[{}] 消息非标准物模型格式，但将作为通用消息处理，productKey: {}", 
            getMessageType(), request.getProductKey());
        return true;
      } catch (Exception e) {
        // JSON 解析失败，仍然作为文本消息尝试处理
        log.debug("[{}] 消息解析异常，但将继续尝试处理，productKey: {}", 
            getMessageType(), request.getProductKey());
        return true;
      }
    }

    // 即使是非JSON格式，也应该尝试处理（作为透传消息）
    return true;
  }

  @Override
  public int getPriority() {
    return 10; // 物模型处理优先级最高
  }
}
