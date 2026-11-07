/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: MQTT OTA上报处理器
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.mqtt.protocol.processor.up;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.dm.device.service.ota.support.OtaReportExtractor;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MQTT OTA 上报处理器。
 *
 * <p>职责边界很明确：协议链在完成主题识别、物模型转换或编解码之后，
 * 若已经得到了标准化的 {@link BaseUPRequest} 列表，则在 MQTT 模块内部识别 OTA 语义并交给 OTA 运行时处理。
 *
 * <p>这样可以避免把 OTA 识别逻辑塞到全局推送链，也更符合 MQTT 协议自身的消息分类方式。
 */
@Slf4j(topic = "mqtt")
@Component
@RequiredArgsConstructor
public class MqttUPOtaProcessor implements MqttMessageProcessor {

  private final IoTOtaRuntimeService otaRuntimeService;
  private final OtaReportExtractor otaReportExtractor;

  @Override
  public String getName() {
    return "mqtt OTA上报处理器";
  }

  @Override
  public String getDescription() {
    return "在MQTT协议链中识别并处理OTA属性/事件上报";
  }

  @Override
  public int getOrder() {
    return 450;
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    if (request == null || CollUtil.isEmpty(request.getUpRequestList())) {
      return false;
    }
    return request.getUpRequestList().stream()
        .filter(upRequest -> upRequest != null)
        .anyMatch(otaReportExtractor::supports);
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    List<BaseUPRequest> upRequests = request.getUpRequestList();
    if (CollUtil.isEmpty(upRequests)) {
      return ProcessorResult.CONTINUE;
    }

    String protocol = resolveProtocol(request);
    int handledCount = 0;
    for (BaseUPRequest upRequest : upRequests) {
      if (upRequest == null || !otaReportExtractor.supports(upRequest)) {
        continue;
      }
      try {
        otaRuntimeService.handleDecodedReport(
            upRequest, upRequest.getProductKey(), upRequest.getDeviceId(), protocol);
        handledCount++;
      } catch (Exception e) {
        log.warn(
            "[{}] OTA处理失败, productKey={}, deviceId={}, error={}",
            getName(),
            upRequest.getProductKey(),
            upRequest.getDeviceId(),
            e.getMessage());
      }
    }

    if (handledCount > 0) {
      request.setContextValue("otaHandled", true);
      request.setContextValue("otaHandledCount", handledCount);
      log.debug("[{}] MQTT OTA上报处理完成, handledCount={}", getName(), handledCount);
    }
    return ProcessorResult.CONTINUE;
  }

  private String resolveProtocol(MQTTUPRequest request) {
    IoTProduct product = request.getIoTProduct();
    if (product != null) {
      if (StrUtil.isNotBlank(product.getThirdPlatform())) {
        return product.getThirdPlatform();
      }
      if (StrUtil.isNotBlank(product.getTransportProtocol())) {
        return product.getTransportProtocol();
      }
    }
    IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
    if (deviceDTO != null && StrUtil.isNotBlank(deviceDTO.getThirdPlatform())) {
      return deviceDTO.getThirdPlatform();
    }
    return "mqtt";
  }
}
