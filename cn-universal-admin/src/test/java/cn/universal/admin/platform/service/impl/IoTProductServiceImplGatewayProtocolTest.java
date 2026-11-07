package cn.universal.admin.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("产品网关数据协议校验")
class IoTProductServiceImplGatewayProtocolTest {

  private final IoTProductServiceImpl service = new IoTProductServiceImpl();

  @Test
  @DisplayName("非工业网关和网关子设备只允许Modbus")
  void shouldOnlyAllowModbusForNonIndustrialGatewayNodes() {
    assertDoesNotThrow(
        () ->
            service.validateGatewayTransportProtocol(
                product(DeviceNode.GATEWAY, "mqtt", "modbus"), null));
    assertDoesNotThrow(
        () ->
            service.validateGatewayTransportProtocol(
                product(DeviceNode.GATEWAY_SUB_DEVICE, "tcp", "modbus"), null));
    assertThrows(
        IoTException.class,
        () ->
            service.validateGatewayTransportProtocol(
                product(DeviceNode.GATEWAY, "mqtt", "custom"), null));
    assertThrows(
        IoTException.class,
        () ->
            service.validateGatewayTransportProtocol(
                product(DeviceNode.GATEWAY_SUB_DEVICE, "http", "opcua"), null));
  }

  @Test
  @DisplayName("工业采集和传统Modbus接入保持原有协议能力")
  void shouldKeepIndustrialAndLegacyModbusAccess() {
    assertDoesNotThrow(
        () ->
            service.validateGatewayTransportProtocol(
                product(DeviceNode.GATEWAY, "industrial", "opcua"), null));
    assertDoesNotThrow(
        () ->
            service.validateGatewayTransportProtocol(
                product(DeviceNode.GATEWAY, "modbus", "modbus"), null));
  }

  @Test
  @DisplayName("历史非法组合未变更时允许保存其他字段")
  void shouldAllowUnchangedLegacyInvalidCombination() {
    IoTProduct current = product(DeviceNode.GATEWAY, "mqtt", "custom");
    IoTProduct update = product(DeviceNode.GATEWAY, "mqtt", "custom");

    assertDoesNotThrow(() -> service.validateGatewayTransportProtocol(update, current));
    assertThrows(
        IoTException.class,
        () ->
            service.validateGatewayTransportProtocol(
                product(DeviceNode.GATEWAY, "mqtt", "opcua"), current));
  }

  private IoTProduct product(DeviceNode deviceNode, String thirdPlatform, String transportProtocol) {
    return IoTProduct.builder()
        .deviceNode(deviceNode.name())
        .thirdPlatform(thirdPlatform)
        .transportProtocol(transportProtocol)
        .build();
  }
}
