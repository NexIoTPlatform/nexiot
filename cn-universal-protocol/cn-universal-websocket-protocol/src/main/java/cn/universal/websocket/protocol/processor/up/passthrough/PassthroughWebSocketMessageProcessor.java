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

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket透传消息处理器
 *
 * <p>对应步骤FOUR：处理透传消息的业务逻辑 - 存储原始数据 - 触发业务规则 - 记录设备影子
 *
 * @version 2.0
 * @Author gitee.com/NexIoT
 * @since 2026/1/15
 */
@Slf4j(topic = "websocket")
@Component
public class PassthroughWebSocketMessageProcessor extends AbstratIoTService
    implements WebSocketUPProcessor {

  @Override
  public String getName() {
    return "WebSocket透传消息处理器";
  }

  @Override
  public String getDescription() {
    return "处理透传消息的业务逻辑";
  }

  @Override
  public int getOrder() {
    return 500; // 消息处理是第五步
  }

  @Override
  public boolean supports(WebSocketUPRequest request) {
    // 必须已经过编解码处理
    Object codecProcessed = request.getContextValue("codecProcessed");
    if (!(codecProcessed instanceof Boolean) || !(Boolean) codecProcessed) {
      return false;
    }

    // 检查是否有编解码后的请求列表
    List<BaseUPRequest> upRequestList = request.getUpRequestList();
    return CollUtil.isNotEmpty(upRequestList);
  }

  @Override
  public ProcessorResult process(WebSocketUPRequest request) {
    try {
      log.info(
          "[{}] [Step 5/5 消息处理] 开始处理透传消息，SessionID: {}, messageId: {}",
          getName(),
          request.getSessionId(),
          request.getMessageId());

      List<BaseUPRequest> upRequestList = request.getUpRequestList();
      if (CollUtil.isEmpty(upRequestList)) {
        log.warn(
            "[{}] 上行请求列表为空，检查编解码是否执行 | codecProcessed: {} | upRequestList: {}",
            getName(),
            request.getContextValue("codecProcessed"),
            upRequestList);
        return ProcessorResult.SKIP;
      }

      // 处理每个上行请求
      int processedCount = 0;
      int failedCount = 0;
      for (BaseUPRequest upRequest : upRequestList) {
        if (processUpRequest(request, upRequest)) {
          processedCount++;
        } else {
          failedCount++;
        }
      }

      request.setContextValue("processedRequestCount", processedCount);
      request.setContextValue("failedRequestCount", failedCount);
      request.setContextValue("messageProcessed", true);

      log.info(
          "[{}] [Step 5/5 SUCCESS] 透传消息处理完成，成功: {}, 失败: {}, 总数: {}",
          getName(),
          processedCount,
          failedCount,
          upRequestList.size());
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error(
          "[{}] [Step 5/5 EXCEPTION] 透传消息处理异常，SessionID: {}, 异常: ",
          getName(),
          request.getSessionId(),
          e);
      return ProcessorResult.ERROR;
    }
  }

  /** 处理单个上行请求 */
  private boolean processUpRequest(WebSocketUPRequest request, BaseUPRequest upRequest) {
    try {
      if (upRequest == null) {
        log.warn("[{}] 上行请求为空", getName());
        return false;
      }

      log.debug(
          "[{}] 处理上行请求 | messageType: {} | iotId: {} | 数据类型: {}",
          getName(),
          upRequest.getMessageType(),
          upRequest.getIotId(),
          upRequest.getData() != null ? upRequest.getData().getClass().getSimpleName() : "null");

      // 获取消息类型和数据
      Map<String, Object> data = upRequest.getData();
      Map<String, Object> properties = upRequest.getProperties();

      // 两个都为空则告警
      if (data == null && properties == null) {
        log.warn(
            "[{}] 数据和属性都为空，messageType: {} | payload: {}",
            getName(),
            upRequest.getMessageType(),
            upRequest.getPayload());
        return false;
      }

      // 优先处理 properties 数据（来自编解码）
      if (properties != null && !properties.isEmpty()) {
        log.debug("[{}] 处理属性数据，字段数: {}", getName(), properties.size());
        // TODO: 实际的业务处理逻辑
        // 1. 存储数据到时序数据库
        // 2. 触发规则引擎
        // 3. 更新设备影子
        // 4. 发送告警通知
      }

      // 其次处理 data 数据
      if (data != null && !data.isEmpty()) {
        log.debug("[{}] 处理数据字段，字段数: {}", getName(), data.size());
        // 处理业务逻辑
      }

      log.info(
          "[{}] 处理上行请求成功，iotId: {}, messageType: {}",
          getName(),
          upRequest.getIotId(),
          upRequest.getMessageType());
      return true;

    } catch (Exception e) {
      log.error("[{}] 处理上行请求失败: {}", getName(), e.getMessage(), e);
      return false;
    }
  }

  @Override
  public int getPriority() {
    return 5; // 透传消息处理优先级中等
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
