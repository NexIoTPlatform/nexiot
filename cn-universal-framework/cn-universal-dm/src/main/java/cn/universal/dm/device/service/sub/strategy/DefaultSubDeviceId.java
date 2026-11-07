package cn.universal.dm.device.service.sub.strategy;

import cn.hutool.core.util.StrUtil;
import cn.universal.core.message.SubDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认子设备生成器
 *
 * @author system
 * @date 2025-01-16
 */
@Slf4j
@Component
public class DefaultSubDeviceId implements SubDeviceIdGeneration {

  private static final String PROTOCOL_NAME = "default";

  @Override
  public boolean supports(String transportProtocol) {
    return PROTOCOL_NAME.equalsIgnoreCase(transportProtocol);
  }

  @Override
  public String generateSubDeviceId(String gwProductKey, String gwDeviceId, SubDevice subDevice) {
    if (subDevice == null) {
      throw new IllegalArgumentException("DownRequest cannot be null");
    }

    String slaveAddress = subDevice.getSlaveAddress();

    if (StrUtil.isBlank(gwDeviceId)) {
      throw new IllegalArgumentException("Gateway device ID cannot be blank");
    }

    if (StrUtil.isBlank(slaveAddress)) {
      throw new IllegalArgumentException("Slave address cannot be blank for default protocol");
    }

    String subDeviceId = gwDeviceId + "-" + slaveAddress;
    log.debug(
        "Generated default sub-device ID: {} for gateway: {} with slave address: {}",
        subDeviceId,
        gwDeviceId,
        slaveAddress);

    return subDeviceId;
  }

  @Override
  public String getSubDeviceProductKey(
      String gwProductKey, String gwDeviceId, SubDevice subDevice) {
    // 如果编解码有，直接返回
    if (subDevice != null && subDevice.getProductKey() != null) {
      return subDevice.getProductKey();
    }
    return "";
  }
}
