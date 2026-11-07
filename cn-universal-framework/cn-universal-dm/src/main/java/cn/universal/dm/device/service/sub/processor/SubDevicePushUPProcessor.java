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

import cn.hutool.core.collection.CollUtil;
import cn.universal.dm.device.service.IoTUPPushAdapter;
import cn.universal.dm.device.service.sub.context.SubDeviceRequest;
import cn.universal.persistence.base.BaseUPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 子设备消息推送
 *
 * @version 2.0
 * @since 2025/10/20
 */
@Slf4j
@Component
public class SubDevicePushUPProcessor extends IoTUPPushAdapter<BaseUPRequest>
    implements SubDeviceMessageProcessor {

  @Override
  public String getName() {
    return "MQTT消息推送处理器";
  }

  @Override
  public String getDescription() {
    return "MQTT消息推送处理器";
  }

  @Override
  public int getOrder() {
    return 999;
  }

  @Override
  public boolean supports(SubDeviceRequest request) {
    // 子设备推送，暂时不判断，直接往上抛
    return true;
  }

  @Override
  public ProcessorResult process(SubDeviceRequest request) {
    try {
      log.debug("[{}] 开始子设备消息处理，设备: {}", getName(), request.getDeviceId());
      if (CollUtil.isNotEmpty(request.getUpRequestList())) {
        doUp(request.getUpRequestList());
      }
      // 4. 更新处理阶段
      request.setStage(SubDeviceRequest.ProcessingStage.COMPLETED);
      log.debug("[{}] 子设备消息推送处理完成，设备: {}", getName(), request.getDeviceId());
      return ProcessorResult.STOP; // 处理完成，停止后续处理
    } catch (Exception e) {
      log.error("[{}] 子设备消息推送处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
      return ProcessorResult.ERROR;
    }
  }
}
