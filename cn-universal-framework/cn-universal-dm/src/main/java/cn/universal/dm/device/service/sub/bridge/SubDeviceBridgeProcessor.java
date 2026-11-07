package cn.universal.dm.device.service.sub.bridge;

import cn.universal.core.message.SubDevice;
import cn.universal.dm.device.service.sub.context.SubDeviceRequest;
import cn.universal.dm.device.service.sub.processor.SubDeviceProcessor;
import cn.universal.dm.device.service.sub.strategy.SubDeviceIdGeneratorStrategy;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 子设备桥接处理器 负责从网关处理结果中识别子设备数据，并桥接到子设备处理器
 *
 * @author system
 * @date 2025-01-16
 */
@Slf4j
@Component
public class SubDeviceBridgeProcessor {

  @Autowired private SubDeviceProcessor subDeviceProcessor;
  @Autowired private SubDeviceIdGeneratorStrategy subDeviceIdGeneratorStrategy;
  @Autowired private IoTDeviceMapper ioTDeviceMapper;

  /**
   * 处理子设备数据 从网关处理结果中识别并处理子设备数据
   *
   * @param gwDeviceId 网关设备deviceId
   * @param gwProductKey 网关产品Key
   * @param payload 原始报文
   * @param subDevice 子设备数据
   */
  public void processSubDevices(
      String gwDeviceId, String gwProductKey, String payload, SubDevice subDevice) {
    if (gwDeviceId == null || gwProductKey == null || subDevice == null) {
      log.debug("网关信息或解析数据为空，跳过子设备处理");
      return;
    }

    try {
      log.debug("开始处理子设备数据，网关: {}, 数据: {}", gwDeviceId, payload);
      SubDeviceRequest subDeviceRequest = new SubDeviceRequest();
      subDeviceRequest.setGwDeviceId(gwDeviceId);
      subDeviceRequest.setGwProductKey(gwProductKey);
      subDeviceRequest.setPayload(payload);
      subDeviceRequest.setSubDevice(subDevice);
      subDeviceRequest.setDeviceId(getSubDeviceId(subDeviceRequest));
      subDeviceRequest.setProductKey(getSubDeviceProductKey(subDeviceRequest));
      // 3. 使用责任链处理子设备
      subDeviceProcessor.processBatch(List.of(subDeviceRequest));

    } catch (Exception e) {
      log.error("子设备数据处理失败，网关: {}", gwDeviceId, e);
    }
  }

  private String getSubDeviceId(SubDeviceRequest subDeviceRequest) {
    return subDeviceIdGeneratorStrategy.generateSubDeviceId(
        subDeviceRequest.getGwProductKey(),
        subDeviceRequest.getGwDeviceId(),
        subDeviceRequest.getSubDevice());
  }

  private String getSubDeviceProductKey(SubDeviceRequest subDeviceRequest) {
    return subDeviceIdGeneratorStrategy.getSubDeviceProductKey(
        subDeviceRequest.getGwProductKey(),
        subDeviceRequest.getGwDeviceId(),
        subDeviceRequest.getSubDevice());
  }
}
