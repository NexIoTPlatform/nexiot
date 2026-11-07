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
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 设备缓存后置处理器
 *
 * <p>负责在设备生命周期操作后清理相关缓存
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component
@Slf4j
public class DeviceCachePostProcessor implements IoTDevicePostProcessor {

  @Resource private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  @Override
  public String getName() {
    return "DeviceCachePostProcessor";
  }

  @Override
  public int getOrder() {
    return 200; // 较低优先级，在网关关系处理器之后执行
  }

  @Override
  public boolean supports(Operation operation) {
    return operation != Operation.COMMAND_RESP;
  }

  @Override
  public void process(Operation operation, IoTDeviceDTO deviceDTO, DownRequest downRequest) {
    try {
      cacheInvalidationPublisher.publishDeviceChange(operation, deviceDTO);

      log.info(
          "设备缓存异步清理已提交, operation={}, deviceId={}, productKey={}",
          operation,
          deviceDTO.getDeviceId(),
          deviceDTO.getProductKey());

    } catch (Exception e) {
      log.warn(
          "设备缓存清理失败, operation={}, deviceId={}: {}",
          operation,
          deviceDTO.getDeviceId(),
          e.getMessage(),
          e);
    }
  }
}
