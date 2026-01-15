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
      return cn.hutool.core.util.StrUtil.isNotBlank(thingModel);
    }

    // 或者检查消息格式是否符合物模型规范
    String payload = request.getPayload();
    if (payload != null && payload.startsWith("{")) {
      try {
        com.fasterxml.jackson.databind.JsonNode node =
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
        // 物模型消息通常包含 method, params 等字段
        return node.has("method") || node.has("properties") || node.has("events");
      } catch (Exception e) {
        // JSON 解析失败，不是物模型格式
        return false;
      }
    }

    return false;
  }

  @Override
  public int getPriority() {
    return 10; // 物模型处理优先级最高
  }
}
