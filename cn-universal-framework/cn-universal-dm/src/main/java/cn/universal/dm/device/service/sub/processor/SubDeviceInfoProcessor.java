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

package cn.universal.dm.device.service.sub.processor;

import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.sub.context.SubDeviceRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.IoTDeviceQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 子设备信息处理器
 *
 * @version 2.0
 * @since 2025/10/20
 */
@Slf4j
@Component
public class SubDeviceInfoProcessor extends AbstratIoTService implements SubDeviceMessageProcessor {

  @Autowired private IoTDeviceShadowService ioTDeviceShadowService;

  @Override
  public String getName() {
    return "子设备信息处理器";
  }

  @Override
  public String getDescription() {
    return "子设备信息处理器,解析出设备和产品的信息";
  }

  @Override
  public int getOrder() {
    return 1; // 编解码处理是第一步
  }

  @Override
  public boolean supports(SubDeviceRequest request) {
    return request.getGwDeviceId() != null
        && request.getGwProductKey() != null
        && request.getSubDevice() != null
        && request.getDeviceId() != null
        && request.getProductKey() != null;
  }

  @Override
  public ProcessorResult process(SubDeviceRequest request) {
    try {
      log.debug(
          "[{}] 开始处理子设备信息，网关: {}, 子设备: {}",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId());
      IoTProduct ioTProduct = getProduct(request.getProductKey());
      IoTDeviceDTO ioTDeviceDTO =
          lifeCycleDevInstance(
              IoTDeviceQuery.builder()
                  .deviceId(request.getDeviceId())
                  .productKey(request.getProductKey())
                  .build());
      if (ioTProduct == null) {
        log.warn("[{}] 产品不存在: {}", getName(), request.getProductKey());
        return ProcessorResult.ERROR;
      }
      if (ioTDeviceDTO == null) {
        log.warn("[{}] 设备不存在: {}", getName(), request.getProductKey());
        return ProcessorResult.ERROR;
      }
      request.setIoTProduct(ioTProduct);
      request.setIoTDeviceDTO(ioTDeviceDTO);
      return ProcessorResult.CONTINUE;
    } catch (Exception e) {
      log.error(
          "[{}] 子设备信息处理器异常，网关: {}, 子设备: {}, 异常: ",
          getName(),
          request.getGwDeviceId(),
          request.getDeviceId(),
          e);
      return ProcessorResult.ERROR;
    }
  }

  @Override
  public boolean preCheck(SubDeviceRequest request) {
    return true;
  }

  @Override
  public void postProcess(SubDeviceRequest request, ProcessorResult result) {
    return;
  }

  @Override
  public void onError(SubDeviceRequest request, Exception e) {
    log.error(
        "[{}] 子设备编解码处理异常，网关: {}, 子设备: {}, 异常: ",
        getName(),
        request.getGwDeviceId(),
        request.getDeviceId(),
        e);
    request.setErrorMessage("子设备编解码处理失败: " + e.getMessage());
  }

  @Override
  public int getPriority() {
    return 5; // 子设备编解码处理优先级中等
  }

  @Override
  public boolean isRequired() {
    return true; // 编解码处理是必需的
  }
}
