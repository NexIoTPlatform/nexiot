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

package cn.universal.websocket.protocol.processor.up.passthrough;

import org.springframework.stereotype.Component;

import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.processor.up.common.BaseWebSocketDeviceInfoProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket透传消息设备信息处理器
 *
 * <p>处理透传消息的设备信息提取和回填
 *
 * <p>透传特点： - 消息格式不固定，需要编解码 - 支持各种自定义协议格式 - 原始数据需要通过编解码器转换
 *
 * @version 2.0
 * @Author gitee.com/NexIoT
 * @since 2026/1/15
 */
@Slf4j(topic = "websocket")
@Component
public class PassthroughWebSocketDeviceInfoProcessor extends BaseWebSocketDeviceInfoProcessor {

  @Override
  protected String getMessageType() {
    return "透传";
  }

  @Override
  public boolean supports(WebSocketUPRequest request) {
    if (request == null || request.getPayload() == null) {
      return false;
    }

    // 检查产品配置 - 透传模式：产品没有配置物模型
    Object productInfo = request.getContextValue("productInfo");
    if (productInfo instanceof java.util.Map) {
      java.util.Map<String, Object> info = (java.util.Map<String, Object>) productInfo;
      String thingModel = (String) info.get("thingModel");
      // 如果产品没有物模型配置，支持透传
      return cn.hutool.core.util.StrUtil.isBlank(thingModel);
    }

    // 默认支持透传（如果没有产品信息配置）
    return true;
  }

  @Override
  public int getPriority() {
    return 5; // 透传处理优先级中等
  }
}
