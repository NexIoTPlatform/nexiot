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

package cn.universal.mqtt.protocol.processor.up;

import cn.universal.common.constant.IoTConstant;
import cn.universal.dm.device.service.sub.bridge.SubDeviceBridgeProcessor;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TCP子设备处理器
 *
 * <p>负责在TCP处理链的末尾处理子设备数据
 *
 * <p>特点： - 在TCP编解码完成后执行 - 检测网关设备并处理子设备数据 - 不影响TCP主流程 - 支持多种网关设备识别方式
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/16
 */
@Slf4j(topic = "mqtt")
@Component
public class MQTTSubDeviceProcessor implements MqttMessageProcessor {

  @Autowired private SubDeviceBridgeProcessor subDeviceBridgeProcessor;

  @Override
  public String getName() {
    return "tcp子设备处理器";
  }

  @Override
  public String getDescription() {
    return "tcp子设备处理器 - 在TCP处理链末尾处理子设备数据";
  }

  @Override
  public int getOrder() {
    return 9999; // 在编解码处理器之后执行
  }

  @Override
  public boolean supports(MQTTUPRequest request) {
    if (request.getIoTDeviceDTO() == null
        || request.getSubDevice() == null
        || request.getIoTProduct() == null) {
      return false;
    }
    return request.getIoTDeviceDTO() != null
        && IoTConstant.DeviceNode.GATEWAY.equals(request.getIoTDeviceDTO().getDeviceNode());
  }

  @Override
  public ProcessorResult process(MQTTUPRequest request) {
    try {
      log.debug("[{}] 开始处理MQTT子设备数据，设备: {}", getName(), request.getDeviceId());
      // 4. 将BaseUPRequest转换为Map格式，供子设备桥接器使用

      // 5. 调用子设备桥接器处理
      subDeviceBridgeProcessor.processSubDevices(
          request.getDeviceId(), // 网关设备ID
          request.getProductKey(), // 网关产品Key
          request.getPayload(), // 原始报文
          request.getSubDevice() // 解析后的数据
          );

      log.debug("[{}] MQTT子设备数据处理完成: {}", getName(), request.getDeviceId());
      return ProcessorResult.CONTINUE;

    } catch (Exception e) {
      log.error("[{}] MQTT子设备数据处理异常: {}", getName(), request.getDeviceId(), e);
      // 子设备处理异常不影响MQTT主流程，继续处理
      return ProcessorResult.CONTINUE;
    }
  }

  @Override
  public boolean preCheck(MQTTUPRequest request) {
    // 检查必要的数据
    return request.getIoTDeviceDTO() != null
        && request.getIoTProduct() != null
        && request.getPayload() != null
        && request.getSubDevice() != null;
  }

  @Override
  public void postProcess(MQTTUPRequest request, ProcessorResult result) {
    if (result == ProcessorResult.CONTINUE) {
      log.debug("[{}] MQTT子设备处理完成 - 设备: {}, 结果: {}", getName(), request.getDeviceId(), result);
    } else {
      log.warn("[{}] MQTT子设备处理失败 - 设备: {}, 结果: {}", getName(), request.getDeviceId(), result);
    }
  }

  @Override
  public void onError(MQTTUPRequest request, Exception e) {
    log.error("[{}] MQTT子设备处理异常，设备: {}, 异常: ", getName(), request.getDeviceId(), e);
    // 子设备处理异常不影响MQTT主流程，不设置错误
  }

  @Override
  public int getPriority() {
    return 9999; // 子设备处理优先级较低
  }

  @Override
  public boolean isRequired() {
    return false; // 子设备处理不是必需的
  }
}
