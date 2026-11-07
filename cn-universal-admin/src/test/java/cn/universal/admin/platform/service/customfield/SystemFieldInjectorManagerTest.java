package cn.universal.admin.platform.service.customfield;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.persistence.entity.IoTProduct;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SystemFieldInjectorManager 测试")
class SystemFieldInjectorManagerTest {

  private final SystemFieldInjectorManager manager = manager();

  @Test
  @DisplayName("工业Modbus产品写入前应生成设备新增所需customField")
  void shouldPersistIndustrialModbusCustomFieldsOnProductWrite() {
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk-industrial")
            .deviceNode(DeviceNode.DEVICE.name())
            .thirdPlatform("industrial")
            .transportProtocol("MODBUS")
            .thirdConfiguration("{\"customField\":[]}")
            .build();

    manager.injectSystemFields(product);

    assertFalse(hasField(product, "industrialProtocol"));
    assertTrue(hasField(product, "transportType"));
    assertTrue(hasField(product, "slaveAddress"));
    assertTrue(hasField(product, "serialPort"));
    assertFalse(hasField(product, "rack"));
  }

  @Test
  @DisplayName("通用TCP产品重新注入时应清理其他协议字段并保留用户字段")
  void shouldPersistOnlyGenericTcpAndUserCustomFields() {
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk-generic-tcp")
            .deviceNode(DeviceNode.DEVICE.name())
            .thirdPlatform("industrial")
            .transportProtocol("GENERIC_TCP")
            .thirdConfiguration(
                "{\"customField\":["
                    + "{\"id\":\"rack\",\"name\":\"S7 Rack\",\"system\":true},"
                    + "{\"id\":\"siteCode\",\"name\":\"站点编号\",\"type\":\"text\",\"system\":false}"
                    + "]}")
            .build();

    manager.injectSystemFields(product);

    assertEquals(5, fields(product).size());
    assertTrue(hasField(product, "host"));
    assertTrue(hasField(product, "port"));
    assertTrue(hasField(product, "connectTimeoutMs"));
    assertTrue(hasField(product, "timeoutMs"));
    assertTrue(hasField(product, "siteCode"));
    assertFalse(hasField(product, "industrialProtocol"));
    assertFalse(hasField(product, "rack"));
    assertFalse(hasField(product, "transportType"));
  }

  @Test
  @DisplayName("云接入Modbus网关不应生成工业customField")
  void shouldNotPersistIndustrialFieldsForCloudAccessModbusGateway() {
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk-cloud-modbus")
            .deviceNode(DeviceNode.GATEWAY.name())
            .thirdPlatform("mqtt")
            .transportProtocol("modbus")
            .thirdConfiguration("{\"customField\":[]}")
            .build();

    manager.injectSystemFields(product);

    assertEquals(0, fields(product).size());
  }

  @Test
  @DisplayName("云接入的旧Modbus网关子设备应只生成从站地址字段")
  void shouldOnlyPersistSlaveAddressForLegacyModbusGatewaySubDevice() {
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk-cloud-modbus-sub-device")
            .deviceNode(DeviceNode.GATEWAY_SUB_DEVICE.name())
            .thirdPlatform("mqtt")
            .transportProtocol("modbus")
            .thirdConfiguration("{\"customField\":[]}")
            .build();

    manager.injectSystemFields(product);

    assertEquals(1, fields(product).size());
    assertTrue(hasField(product, "slaveAddress"));
    assertFalse(hasField(product, "host"));
    assertFalse(hasField(product, "transportType"));
    assertFalse(hasField(product, "serialPort"));
  }

  @Test
  @DisplayName("旧Modbus平台仍只走旧Modbus系统字段")
  void shouldKeepLegacyModbusOnLegacyInjector() {
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk-legacy-modbus")
            .deviceNode(DeviceNode.DEVICE.name())
            .thirdPlatform("modbus")
            .transportProtocol("modbus")
            .thirdConfiguration("{\"customField\":[]}")
            .build();

    manager.injectSystemFields(product);

    assertFalse(hasField(product, "industrialProtocol"));
    assertTrue(hasField(product, "transportType"));
    assertTrue(hasField(product, "slaveAddress"));
    assertTrue(hasField(product, "idleTimeoutMs"));
  }

  @Test
  @DisplayName("产品不再匹配规则时应移除旧系统字段并保留用户字段")
  void shouldRemoveStaleSystemFieldsWhenProductNoLongerMatchesRules() {
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk-normal")
            .deviceNode(DeviceNode.DEVICE.name())
            .thirdPlatform("mqtt")
            .transportProtocol("json")
            .thirdConfiguration(
                "{\"customField\":["
                    + "{\"id\":\"slaveAddress\",\"name\":\"从站地址\",\"system\":true},"
                    + "{\"id\":\"siteCode\",\"name\":\"站点编号\",\"type\":\"text\",\"system\":false}"
                    + "]}")
            .build();

    manager.injectSystemFields(product);

    assertFalse(hasField(product, "slaveAddress"));
    assertTrue(hasField(product, "siteCode"));
    assertEquals(1, fields(product).size());
  }

  @Test
  @DisplayName("非工业接入不应从S7数据协议重建工业字段")
  void shouldNotRebuildIndustrialFieldsFromCloudTransportProtocol() {
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk-industrial-s7")
            .deviceNode(DeviceNode.DEVICE.name())
            .thirdPlatform("mqtt")
            .transportProtocol("S7")
            .thirdConfiguration(
                "{\"customField\":["
                    + "{\"id\":\"endpointUrl\",\"name\":\"OPC UA地址\",\"system\":true},"
                    + "{\"id\":\"siteCode\",\"name\":\"站点编号\",\"type\":\"text\",\"system\":false}"
                    + "]}")
            .build();

    manager.injectSystemFields(product);

    assertFalse(hasField(product, "endpointUrl"));
    assertFalse(hasField(product, "industrialProtocol"));
    assertFalse(hasField(product, "rack"));
    assertFalse(hasField(product, "slot"));
    assertTrue(hasField(product, "siteCode"));
    assertEquals(1, fields(product).size());
  }

  private SystemFieldInjectorManager manager() {
    SystemFieldInjectorManager manager = new SystemFieldInjectorManager();
    ReflectionTestUtils.setField(
        manager,
        "injectors",
        List.of(new ModbusSystemFieldInjector(), new IndustrialSystemFieldInjector()));
    return manager;
  }

  private boolean hasField(IoTProduct product, String id) {
    JSONArray fields = fields(product);
    for (int i = 0; i < fields.size(); i++) {
      if (id.equals(fields.getJSONObject(i).getStr("id"))) {
        return true;
      }
    }
    return false;
  }

  private JSONArray fields(IoTProduct product) {
    return JSONUtil.parseObj(product.getThirdConfiguration()).getJSONArray("customField");
  }
}
