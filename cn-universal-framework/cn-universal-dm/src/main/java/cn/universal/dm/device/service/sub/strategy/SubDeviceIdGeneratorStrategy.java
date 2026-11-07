package cn.universal.dm.device.service.sub.strategy;

import cn.hutool.core.util.StrUtil;
import cn.universal.core.message.SubDevice;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 子设备ID生成器管理器 根据协议类型选择合适的ID生成策略
 *
 * @author system
 * @date 2025-01-16
 */
@Slf4j
@Service
public class SubDeviceIdGeneratorStrategy {

  @Autowired private List<SubDeviceIdGeneration> strategies;
  @Resource private IoTProductDeviceService ioTProductDeviceService;

  /**
   * 生成子设备ID
   *
   * @throws IllegalArgumentException 如果协议不支持或参数无效
   */
  public String generateSubDeviceId(String gwProductKey, String gwDeviceId, SubDevice subDevice) {
    if (subDevice == null) {
      throw new IllegalArgumentException("subDevice cannot be null");
    }

    String transportProtocol = ioTProductDeviceService.selectTransportProtocol(gwProductKey);
    if (StrUtil.isBlank(transportProtocol)) {
      throw new IllegalArgumentException("Transport protocol cannot be blank");
    }

    // 查找支持该协议的策略
    SubDeviceIdGeneration strategy = findStrategy(transportProtocol);
    if (strategy == null) {
      log.warn("No strategy found for protocol: {}, using default strategy", transportProtocol);
      strategy =
          strategies.stream()
              .filter(s -> s instanceof DefaultSubDeviceId)
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("DefaultSubDeviceId strategy not found"));
    }

    log.debug(
        "Using strategy: {} for protocol: {}",
        strategy.getClass().getSimpleName(),
        transportProtocol);

    return strategy.generateSubDeviceId(gwProductKey, gwDeviceId, subDevice);
  }

  /**
   * 生成子设备ID
   *
   * @throws IllegalArgumentException 如果协议不支持或参数无效
   */
  public String getSubDeviceProductKey(
      String gwProductKey, String gwDeviceId, SubDevice subDevice) {
    if (subDevice == null) {
      throw new IllegalArgumentException("subDevice cannot be null");
    }

    String transportProtocol = ioTProductDeviceService.selectTransportProtocol(gwProductKey);
    if (StrUtil.isBlank(transportProtocol)) {
      throw new IllegalArgumentException("Transport protocol cannot be blank");
    }

    // 查找支持该协议的策略
    SubDeviceIdGeneration strategy = findStrategy(transportProtocol);
    if (strategy == null) {
      log.warn("No strategy found for protocol: {}, using default strategy", transportProtocol);
      strategy =
          strategies.stream()
              .filter(s -> s instanceof DefaultSubDeviceId)
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("DefaultSubDeviceId strategy not found"));
    }

    log.debug(
        "Using strategy: {} for protocol: {}",
        strategy.getClass().getSimpleName(),
        transportProtocol);

    return strategy.getSubDeviceProductKey(gwProductKey, gwDeviceId, subDevice);
  }

  /**
   * 查找支持指定协议的策略
   *
   * @param transportProtocol 传输协议
   * @return 策略实例，如果未找到返回null
   */
  private SubDeviceIdGeneration findStrategy(String transportProtocol) {
    if (strategies == null || strategies.isEmpty()) {
      log.warn("No strategies available");
      return null;
    }

    for (SubDeviceIdGeneration strategy : strategies) {
      if (strategy.supports(transportProtocol)) {
        return strategy;
      }
    }

    log.warn(
        "No strategy found for transport protocol: {}, using default strategy", transportProtocol);
    return strategies.stream()
        .filter(s -> s instanceof DefaultSubDeviceId)
        .findFirst()
        .orElse(null);
  }

  /**
   * 获取所有支持的协议列表
   *
   * @return 支持的协议列表
   */
  public List<String> getSupportedProtocols() {
    return strategies.stream()
        .map(
            strategy -> {
              // 通过反射获取协议名称，或者添加getProtocolName方法到接口
              if (strategy instanceof ModbusSubDeviceId) {
                return "modbus";
              }
              //              else if (strategy instanceof OpcUaSubDeviceIdStrategy) {
              //                return "opcua";
              //              } else if (strategy instanceof S7SubDeviceIdStrategy) {
              //                return "s7";
              //              }
              return "modbus";
            })
        .distinct()
        .collect(java.util.stream.Collectors.toList());
  }
}
