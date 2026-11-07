package cn.universal.dm.device.service.sub.strategy;

import cn.universal.core.message.SubDevice;

/**
 * 子设备ID生成策略接口 支持不同协议的子设备ID生成规则
 *
 * @author system
 * @date 2025-01-16
 */
public interface SubDeviceIdGeneration {

  /**
   * 判断是否支持该协议
   *
   * @param transportProtocol 传输协议 (modbus, opcua, s7, etc.)
   * @return true if supported
   */
  boolean supports(String transportProtocol);

  /** 生成子设备ID */
  String generateSubDeviceId(String gwProductKey, String gwDeviceId, SubDevice subDevice);

  String getSubDeviceProductKey(String gwProductKey, String gwDeviceId, SubDevice subDevice);
}
