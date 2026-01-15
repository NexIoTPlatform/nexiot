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

package cn.universal.websocket.protocol.processor.up;

import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.service.IoTUPPushAdapter;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 消息推送处理器
 *
 * <p>对标MQTT的MqttPublishUPProcessor，负责将处理完成的消息推送到应用层
 * 支持数据入库、规则引擎触发、Webhook回调等后续处理
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/15
 */
@Slf4j
@Component
public class WebSocketPublishUPProcessor extends IoTUPPushAdapter<BaseUPRequest>
    implements WebSocketUPProcessor {

  @Override
  public String getName() {
    return "WebSocket消息推送处理器";
  }

  @Override
  public String getDescription() {
    return "将WebSocket处理完成的消息推送到应用层（数据入库、规则引擎等）";
  }

  @Override
  public ProcessorResult process(WebSocketUPRequest request) {
    try {
      log.debug(
          "[{}] [最后一步] 开始推送消息到应用层，SessionID: {}, Device: {}/{}",
          getName(), request.getSessionId(), request.getProductKey(), request.getIotId());

      // 检查是否有处理完成的请求列表
      if (CollUtil.isEmpty(request.getUpRequestList())) {
        log.warn("[{}] 请求列表为空，无法推送，SessionID: {}", getName(), request.getSessionId());
        return ProcessorResult.SKIP;
      }

      int messageCount = request.getUpRequestList().size();
      String deviceInfo = "";
      if (request.getIoTDeviceDTO() != null) {
        deviceInfo = String.format(
            "Product: %s, Device: %s, App: %s",
            request.getIoTDeviceDTO().getProductKey(),
            request.getIoTDeviceDTO().getDeviceId(),
            StrUtil.isNotBlank(request.getIoTDeviceDTO().getApplicationId())
                ? request.getIoTDeviceDTO().getApplicationId()
                : "默认应用");
      }

      log.debug("[{}] 准备推送 {} 条消息到应用层，{}", 
          getName(), messageCount, deviceInfo);

      // 核心步骤：调用 doUp() 推送消息
      // 这会触发以下处理链：
      // 1. 数据入库（时序数据库或关系型数据库）
      // 2. 设备影子更新
      // 3. 规则引擎触发
      // 4. 事件通知（Webhook、MQ等）
      // 5. WebSocket推送（如果客户端订阅了）
      long startTime = System.currentTimeMillis();
      doUp(request.getUpRequestList());
      long elapsedTime = System.currentTimeMillis() - startTime;

      // 对标MQTT的最终输出日志格式
      log.info("[数据输出处理完成] 处理数量: {}/{}, 耗时: {}ms",
          messageCount, messageCount, elapsedTime);

      log.info("[{}] [最后一步 SUCCESS] 消息推送完成，SessionID: {}，消息数: {}, 耗时: {}ms, {}",
          getName(), request.getSessionId(), messageCount, elapsedTime, deviceInfo);

      // 标记阶段
      request.setStage(WebSocketUPRequest.ProcessingStage.COMPLETED);

      return ProcessorResult.STOP; // 处理完成，停止后续处理器
    } catch (Exception e) {
      log.error("[{}] [最后一步 ERROR] 消息推送异常，SessionID: {}，异常: ",
          getName(), request.getSessionId(), e);
      request.setError("推送失败: " + e.getMessage());
      return ProcessorResult.ERROR;
    }
  }

  @Override
  public boolean supports(WebSocketUPRequest request) {
    // 仅支持已完成业务处理、有设备信息且有上行请求列表的消息
    return request != null
        && request.getIoTDeviceDTO() != null
        && CollUtil.isNotEmpty(request.getUpRequestList());
  }

  @Override
  public int getOrder() {
    return 2000; // 在所有其他处理器之后执行（最后一步）
  }

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public boolean preCheck(WebSocketUPRequest request) {
    return true;
  }

  @Override
  public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.STOP) {
      log.debug("[{}] 消息推送处理完成", getName());
    }
  }

  @Override
  public void onError(WebSocketUPRequest request, Exception e) {
    log.error("[{}] 消息推送错误，SessionID: {}",
        getName(), request.getSessionId(), e);
  }
}
