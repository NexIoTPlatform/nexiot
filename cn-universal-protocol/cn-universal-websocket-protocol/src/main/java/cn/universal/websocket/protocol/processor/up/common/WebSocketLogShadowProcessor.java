/*
 *
 * Copyright (c) 2026, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.websocket.protocol.processor.up.common;

import java.util.List;

import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.websocket.protocol.entity.WebSocketUPRequest;
import cn.universal.websocket.protocol.processor.up.WebSocketUPProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 日志与影子处理器 - 对标 MQTT 的日志影子处理
 *
 * <p>职责：保存设备日志与更新设备影子，补齐 WebSocket 入库链路
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/1/17
 */
@Slf4j(topic = "websocket")
@Component
public class WebSocketLogShadowProcessor extends AbstratIoTService
    implements WebSocketUPProcessor {

  @Override
  public String getName() {
    return "WebSocket日志影子处理器";
  }

  @Override
  public String getDescription() {
    return "保存设备日志并更新设备影子";
  }

  @Override
  public int getOrder() {
    return 1100; // 在数据桥接(1000)之后、发布推送(2000)之前执行
  }

  @Override
  public int getPriority() {
    return 10;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean preCheck(WebSocketUPRequest request) {
    return request != null
        && request.getUpRequestList() != null
        && !request.getUpRequestList().isEmpty();
  }

  @Override
  public boolean supports(WebSocketUPRequest request) {
    return request.getIoTDeviceDTO() != null
        && request.getIoTProduct() != null
        && CollUtil.isNotEmpty(request.getUpRequestList());
  }

  @Override
  public ProcessorResult process(WebSocketUPRequest request) {
    try {
      IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
      IoTProduct ioTProduct = request.getIoTProduct();
      List<BaseUPRequest> upRequestList = request.getUpRequestList();

      if (deviceDTO == null || ioTProduct == null || CollUtil.isEmpty(upRequestList)) {
        return ProcessorResult.CONTINUE;
      }

      int logCount = 0;
      int shadowCount = 0;

      for (BaseUPRequest upRequest : upRequestList) {
        if (upRequest == null || upRequest.isDebug()) {
          continue;
        }

        try {
          iIoTDeviceDataService.saveDeviceLog(upRequest, deviceDTO, ioTProduct);
          logCount++;
        } catch (Exception e) {
          log.warn(
              "[{}] 保存设备日志异常 - iotId: {}, messageType: {}, error: {}",
              getName(),
              upRequest.getIotId(),
              upRequest.getMessageType(),
              e.getMessage());
        }

        try {
          iotDeviceShadowService.doShadow(upRequest, deviceDTO);
          shadowCount++;
        } catch (Exception e) {
          log.warn(
              "[{}] 更新设备影子异常 - iotId: {}, messageType: {}, error: {}",
              getName(),
              upRequest.getIotId(),
              upRequest.getMessageType(),
              e.getMessage());
        }
      }

      request.setContextValue("logShadowProcessed", true);
      request.setContextValue("logProcessedCount", logCount);
      request.setContextValue("shadowUpdatedCount", shadowCount);

      log.debug(
          "[{}] 日志影子处理完成，日志: {}, 影子: {}, SessionID: {}",
          getName(),
          logCount,
          shadowCount,
          request.getSessionId());

      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] 日志影子处理异常，SessionID: {}", getName(), request.getSessionId(), e);
      return ProcessorResult.CONTINUE;
    }
  }

  @Override
  public void postProcess(WebSocketUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      log.debug("[{}] 日志影子处理后置完成，SessionID: {}", getName(), request.getSessionId());
    }
  }

  @Override
  public void onError(WebSocketUPRequest request, Exception e) {
    log.error("[{}] 日志影子处理异常，SessionID: {}", getName(), request.getSessionId(), e);
  }
}