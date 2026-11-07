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

package cn.universal.dm.device.service.processor;

import cn.universal.core.message.DownRequest;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 设备后置处理器管理器
 *
 * <p>负责管理和执行所有设备后置处理器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component
@Slf4j
public class DevicePostProcessorManager {

  @Autowired private List<IoTDevicePostProcessor> postProcessors;

  /**
   * 执行设备后置处理
   *
   * @param operation 操作类型
   * @param deviceDTO 设备信息
   * @param downRequest 下行请求（可能为null）
   */
  public void executePostProcessors(
      IoTDevicePostProcessor.Operation operation, IoTDeviceDTO deviceDTO, DownRequest downRequest) {
    if (deviceDTO == null) {
      log.warn("设备信息为空，跳过后置处理, operation={}", operation);
      return;
    }

    // 获取支持该操作类型的处理器，并按优先级排序
    List<IoTDevicePostProcessor> supportedProcessors =
        postProcessors.stream()
            .filter(processor -> processor.supports(operation))
            .sorted((p1, p2) -> Integer.compare(p1.getOrder(), p2.getOrder()))
            .collect(Collectors.toList());

    if (supportedProcessors.isEmpty()) {
      log.debug("没有找到支持操作 {} 的后置处理器", operation);
      return;
    }

    log.debug(
        "开始执行设备后置处理, operation={}, deviceId={}, 处理器数量={}",
        operation,
        deviceDTO.getDeviceId(),
        supportedProcessors.size());

    // 依次执行所有支持的处理器
    for (IoTDevicePostProcessor processor : supportedProcessors) {
      try {
        log.debug(
            "执行后置处理器: {}, operation={}, deviceId={}",
            processor.getName(),
            operation,
            deviceDTO.getDeviceId());

        processor.process(operation, deviceDTO, downRequest);

      } catch (Exception e) {
        log.error(
            "后置处理器执行失败: {}, operation={}, deviceId={}, error={}",
            processor.getName(),
            operation,
            deviceDTO.getDeviceId(),
            e.getMessage(),
            e);
        // 继续执行其他处理器，不因为一个处理器失败而中断整个流程
      }
    }

    log.debug("设备后置处理完成, operation={}, deviceId={}", operation, deviceDTO.getDeviceId());
  }
}
