package cn.universal.dm.device.service.sub.strategy;

import cn.hutool.core.util.StrUtil;
import cn.universal.core.message.SubDevice;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Modbus协议子设备ID生成策略 规则：网关设备ID + "-" + 从站地址 例如：860048070262660-1, 860048070262660-2
 *
 * @author system
 * @date 2025-01-16
 */
@Slf4j
@Component
public class ModbusSubDeviceId implements SubDeviceIdGeneration {

  private static final String PROTOCOL_NAME = "modbus";
  @Resource private IoTDeviceMapper ioTDeviceMapper;

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
      throw new IllegalArgumentException("Slave address cannot be blank for Modbus protocol");
    }

    // 验证从站地址范围 (1-247)
    try {
      int address = Integer.parseInt(slaveAddress);
      if (address < 1 || address > 247) {
        throw new IllegalArgumentException(
            "Modbus slave address must be between 1 and 247, got: " + address);
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid Modbus slave address format: " + slaveAddress);
    }

    String subDeviceId = gwDeviceId + "-" + slaveAddress;
    log.debug(
        "Generated Modbus sub-device ID: {} for gateway: {} with slave address: {}",
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
    String subDeviceId = generateSubDeviceId(gwProductKey, gwDeviceId, subDevice);
    IoTDevice iotDevice =
        IoTDevice.builder()
            .gwProductKey(gwProductKey)
            .extDeviceId(gwDeviceId)
            .deviceId(subDeviceId)
            .build();
    IoTDevice db = ioTDeviceMapper.selectOne(iotDevice);
    if (db == null) {
      return null;
    }
    return db.getProductKey();
  }
}
