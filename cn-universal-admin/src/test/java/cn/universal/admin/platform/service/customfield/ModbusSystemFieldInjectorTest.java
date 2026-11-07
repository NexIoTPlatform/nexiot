package cn.universal.admin.platform.service.customfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.persistence.entity.IoTProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModbusSystemFieldInjector 测试")
class ModbusSystemFieldInjectorTest {

  private final ModbusSystemFieldInjector injector = new ModbusSystemFieldInjector();

  @Test
  @DisplayName("Modbus网关产品应自动注入设备级连接参数字段")
  void shouldInjectGatewayConnectionFieldsForModbusAccessProduct() {
    IoTProduct product =
        IoTProduct.builder().deviceNode(DeviceNode.GATEWAY.name()).thirdPlatform("modbus").build();
    JSONArray fields = new JSONArray();

    assertTrue(injector.supports(product));
    injector.inject(product, fields);

    assertTrue(hasField(fields, "transportType"));
    assertTrue(hasField(fields, "host"));
    assertTrue(hasField(fields, "port"));
    assertTrue(hasField(fields, "serialPort"));
    assertTrue(hasField(fields, "baudRate"));
    assertTrue(hasField(fields, "idleTimeoutMs"));
    assertEquals("modbus_transport_type", findField(fields, "transportType").getStr("dictType"));
  }

  @Test
  @DisplayName("Modbus直连产品应自动注入连接参数和从站地址字段")
  void shouldInjectConnectionAndSlaveAddressForModbusDirectProduct() {
    IoTProduct product =
        IoTProduct.builder().deviceNode(DeviceNode.DEVICE.name()).thirdPlatform("modbus").build();
    JSONArray fields = new JSONArray();

    assertTrue(injector.supports(product));
    injector.inject(product, fields);

    assertTrue(hasField(fields, "transportType"));
    assertTrue(hasField(fields, "host"));
    assertTrue(hasField(fields, "port"));
    assertTrue(hasField(fields, "slaveAddress"));
    assertEquals(false, findField(fields, "slaveAddress").getBool("autoGenerate"));
  }

  @Test
  @DisplayName("Modbus网关子设备产品只注入从站地址字段")
  void shouldInjectSlaveAddressForModbusSubDeviceProduct() {
    IoTProduct product =
        IoTProduct.builder()
            .deviceNode(DeviceNode.GATEWAY_SUB_DEVICE.name())
            .transportProtocol("modbus")
            .build();
    JSONArray fields = new JSONArray();

    assertTrue(injector.supports(product));
    injector.inject(product, fields);

    assertTrue(hasField(fields, "slaveAddress"));
    assertFalse(hasField(fields, "host"));
    assertEquals(true, findField(fields, "slaveAddress").getBool("autoGenerate"));
  }

  private boolean hasField(JSONArray fields, String id) {
    return findField(fields, id) != null;
  }

  private JSONObject findField(JSONArray fields, String id) {
    for (int i = 0; i < fields.size(); i++) {
      JSONObject field = fields.getJSONObject(i);
      if (id.equals(field.getStr("id"))) {
        return field;
      }
    }
    return null;
  }
}
