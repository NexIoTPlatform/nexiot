package cn.universal.admin.platform.service.customfield;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.persistence.entity.IoTProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IndustrialSystemFieldInjector 测试")
class IndustrialSystemFieldInjectorTest {

  private final IndustrialSystemFieldInjector injector = new IndustrialSystemFieldInjector();

  @Test
  @DisplayName("工业S7产品应只注入S7专用字段")
  void shouldInjectS7DeviceConnectionFields() {
    IoTProduct product =
        IoTProduct.builder().thirdPlatform("industrial").transportProtocol("S7").build();
    JSONArray fields = new JSONArray();

    assertTrue(injector.supports(product));
    injector.inject(product, fields);

    assertFalse(hasField(fields, "industrialProtocol"));
    assertTrue(hasField(fields, "host"));
    assertTrue(hasField(fields, "port"));
    assertTrue(hasField(fields, "rack"));
    assertTrue(hasField(fields, "slot"));
    assertFalse(hasField(fields, "transportType"));
    assertFalse(hasField(fields, "slaveAddress"));
    assertFalse(hasField(fields, "unitId"));
    assertEquals(102, findField(fields, "port").getInt("defaultValue"));
    assertTrue(findField(fields, "port").getBool("required"));
  }

  @Test
  @DisplayName("工业新Modbus产品应注入TCP和串口识别字段")
  void shouldInjectNewModbusConnectionFields() {
    IoTProduct product =
        IoTProduct.builder().thirdPlatform("industrial").transportProtocol("MODBUS").build();
    JSONArray fields = new JSONArray();

    assertTrue(injector.supports(product));
    injector.inject(product, fields);

    assertFalse(hasField(fields, "industrialProtocol"));
    assertTrue(hasField(fields, "transportType"));
    assertTrue(hasField(fields, "slaveAddress"));
    assertTrue(hasField(fields, "serialPort"));
    assertFalse(hasField(fields, "rack"));
    assertTrue(hasField(fields, "baudRate"));
    assertTrue(hasField(fields, "parity"));
    assertTrue(hasField(fields, "dataBits"));
    assertTrue(hasField(fields, "stopBits"));
    assertTrue(findField(fields, "host").getBool("required"));
    assertTrue(findField(fields, "port").getBool("required"));
    assertTrue(findField(fields, "transportType").getBool("required"));
    assertTrue(findField(fields, "slaveAddress").getBool("required"));
    assertTrue(findField(fields, "serialPort").getBool("required"));
    assertEquals("select", findField(fields, "serialPort").getStr("type"));
    assertFalse(findField(fields, "serialPort").containsKey("defaultValue"));
    assertEquals("/admin/v1/industrial/serial-ports/options", findField(fields, "serialPort").getStr("optionUrl"));
    assertTrue(findField(fields, "serialPort").getJSONArray("options").isEmpty());
    assertEquals("tcp-client", findField(fields, "transportType").getStr("defaultValue"));
    assertEquals(502, findField(fields, "port").getInt("defaultValue"));
    assertVisibleWhenIn(fields, "host", "transportType", "tcp-client", "rtu-tcp");
    assertVisibleWhenIn(fields, "port", "transportType", "tcp-client", "rtu-tcp");
    assertVisibleWhenIn(fields, "serialPort", "transportType", "rtu-serial", "ascii-serial");
    assertVisibleWhenIn(fields, "baudRate", "transportType", "rtu-serial", "ascii-serial");
    assertVisibleWhenIn(fields, "parity", "transportType", "rtu-serial", "ascii-serial");
    assertVisibleWhenIn(fields, "dataBits", "transportType", "rtu-serial", "ascii-serial");
    assertVisibleWhenIn(fields, "stopBits", "transportType", "rtu-serial", "ascii-serial");
    assertFalse(hasField(fields, "unitId"));
  }

  @Test
  @DisplayName("工业IEC104产品应注入主动上报连接与命令配置字段")
  void shouldInjectIec104DeviceConnectionFields() {
    IoTProduct product =
        IoTProduct.builder().thirdPlatform("industrial").transportProtocol("IEC104").build();
    JSONArray fields = new JSONArray();

    assertTrue(injector.supports(product));
    injector.inject(product, fields);

    assertFalse(hasField(fields, "industrialProtocol"));
    assertTrue(findField(fields, "host").getBool("required"));
    assertTrue(findField(fields, "port").getBool("required"));
    assertEquals(2404, findField(fields, "port").getInt("defaultValue"));
    assertTrue(hasField(fields, "commonAddress"));
    assertTrue(hasField(fields, "originatorAddress"));
    assertTrue(hasField(fields, "commandTimeoutMs"));
    assertTrue(hasField(fields, "ackEveryNFrames"));
    assertTrue(hasField(fields, "codecMode"));
    assertTrue(hasField(fields, "qualityPushMode"));
    assertEquals(1, findField(fields, "commonAddress").getInt("defaultValue"));
    assertEquals(0, findField(fields, "originatorAddress").getInt("defaultValue"));
    assertEquals(3000, findField(fields, "commandTimeoutMs").getInt("defaultValue"));
    assertEquals(8, findField(fields, "ackEveryNFrames").getInt("defaultValue"));
    assertEquals("AUTO", findField(fields, "codecMode").getStr("defaultValue"));
    assertEquals("NONE", findField(fields, "qualityPushMode").getStr("defaultValue"));
  }

  @Test
  @DisplayName("通用TCP和UDP产品应只提供本协议连接字段")
  void shouldInjectGenericRequestResponseConnectionFields() {
    for (String protocol : new String[] {"GENERIC_TCP", "GENERIC_UDP"}) {
      IoTProduct product =
          IoTProduct.builder().thirdPlatform("industrial").transportProtocol(protocol).build();
      JSONArray fields = new JSONArray();

      assertTrue(injector.supports(product));
      injector.inject(product, fields);

      assertEquals(4, fields.size());
      assertFalse(hasField(fields, "industrialProtocol"));
      assertTrue(hasField(fields, "host"));
      assertEquals(9000, findField(fields, "port").getInt("defaultValue"));
      assertTrue(hasField(fields, "connectTimeoutMs"));
      assertTrue(hasField(fields, "timeoutMs"));
      assertFalse(hasField(fields, "rack"));
      assertFalse(hasField(fields, "transportType"));
    }
  }

  @Test
  @DisplayName("非工业接入不应根据数据协议反推工业采集")
  void shouldNotInferIndustrialAccessFromTransportProtocol() {
    String[] transportProtocols = {
      "modbus", "modbus-rtu-serial", "modbus-rtu-over-tcp", "iec-60870-5-104", "opcua"
    };

    for (String transportProtocol : transportProtocols) {
      IoTProduct product =
          IoTProduct.builder()
              .deviceNode(DeviceNode.GATEWAY.name())
              .thirdPlatform("mqtt")
              .transportProtocol(transportProtocol)
              .build();
      assertFalse(injector.supports(product), transportProtocol);
    }
  }

  @Test
  @DisplayName("旧工业接入方式仍应注入对应工业字段")
  void shouldKeepLegacyIndustrialAccessProtocol() {
    IoTProduct product =
        IoTProduct.builder().thirdPlatform("opcua").transportProtocol("opcua").build();
    JSONArray fields = new JSONArray();

    assertTrue(injector.supports(product));
    injector.inject(product, fields);

    assertTrue(hasField(fields, "host"));
    assertTrue(hasField(fields, "port"));
    assertTrue(hasField(fields, "username"));
    assertFalse(hasField(fields, "industrialProtocol"));
  }

  @Test
  @DisplayName("旧Modbus平台应继续由旧注入器处理")
  void shouldNotTakeOverLegacyModbusPlatform() {
    IoTProduct product =
        IoTProduct.builder().thirdPlatform("modbus").transportProtocol("modbus").build();

    assertFalse(injector.supports(product));
  }

  @Test
  @DisplayName("MC和FINS产品应分别只暴露原生驱动已实现的选项")
  void shouldExposeOnlyImplementedNativeOptionsInDeviceCustomFields() {
    IoTProduct mcProduct =
        IoTProduct.builder().thirdPlatform("industrial").transportProtocol("MC").build();
    JSONArray mcFields = new JSONArray();

    assertTrue(injector.supports(mcProduct));
    injector.inject(mcProduct, mcFields);

    assertTrue(hasOption(findField(mcFields, "frame"), "3e-binary"));
    assertFalse(hasOption(findField(mcFields, "frame"), "3e-ascii"));
    assertFalse(hasField(mcFields, "transport"));

    IoTProduct finsProduct =
        IoTProduct.builder().thirdPlatform("industrial").transportProtocol("FINS").build();
    JSONArray finsFields = new JSONArray();
    injector.inject(finsProduct, finsFields);

    assertTrue(hasOption(findField(finsFields, "transport"), "tcp"));
    assertFalse(hasOption(findField(finsFields, "transport"), "udp"));
    assertTrue(hasField(finsFields, "sourceNetwork"));
    assertTrue(hasField(finsFields, "targetNetwork"));
    assertFalse(hasField(finsFields, "frame"));
  }

  @Test
  @DisplayName("产品创建字段应覆盖PLC4J EIP、AB-ETH与ADS连接参数")
  void shouldInjectCompletePlc4jConnectionFields() {
    JSONArray eipFields = injectFields("EIP");
    assertEquals(44818, findField(eipFields, "port").getInt("defaultValue"));
    assertTrue(hasField(eipFields, "backplane"));
    assertTrue(hasField(eipFields, "slot"));
    assertTrue(hasField(eipFields, "bigEndian"));
    assertTrue(hasField(eipFields, "forceUnconnectedOperation"));
    assertTrue(hasField(eipFields, "communicationPath"));

    JSONArray abEthFields = injectFields("AB_ETH");
    assertEquals(2222, findField(abEthFields, "port").getInt("defaultValue"));
    assertTrue(hasField(abEthFields, "station"));
    assertFalse(hasField(abEthFields, "backplane"));

    JSONArray adsFields = injectFields("ADS");
    assertTrue(findField(adsFields, "targetAmsNetId").getBool("required"));
    assertTrue(hasField(adsFields, "targetAmsPort"));
    assertTrue(hasField(adsFields, "sourceAmsNetId"));
    assertTrue(hasField(adsFields, "sourceAmsPort"));
    assertTrue(hasField(adsFields, "loadSymbolAndDataTypeTables"));
  }

  @Test
  @DisplayName("CUSTOM产品应只注入模拟器与请求超时字段")
  void shouldInjectCustomSimulatorFieldsWithoutNetworkTarget() {
    JSONArray fields = injectFields("CUSTOM");

    assertEquals(2, fields.size());
    assertTrue(hasField(fields, "simulator"));
    assertTrue(hasOption(findField(fields, "simulator"), "true"));
    assertTrue(hasField(fields, "timeoutMs"));
    assertFalse(hasField(fields, "host"));
    assertFalse(hasField(fields, "port"));
    assertFalse(hasField(fields, "connectTimeoutMs"));
  }

  private JSONArray injectFields(String protocol) {
    IoTProduct product =
        IoTProduct.builder().thirdPlatform("industrial").transportProtocol(protocol).build();
    JSONArray fields = new JSONArray();
    assertTrue(injector.supports(product));
    injector.inject(product, fields);
    return fields;
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

  private boolean hasOption(JSONObject field, String value) {
    JSONArray options = field == null ? null : field.getJSONArray("options");
    if (options == null) {
      return false;
    }
    for (int i = 0; i < options.size(); i++) {
      JSONObject option = options.getJSONObject(i);
      if (value.equals(option.getStr("value"))) {
        return true;
      }
    }
    return false;
  }

  private void assertVisibleWhenIn(
      JSONArray fields, String fieldId, String dependsOn, String... expectedValues) {
    JSONObject visibleWhen = findVisibleWhenCondition(fields, fieldId, dependsOn);
    JSONArray values = visibleWhen.getJSONArray("in");
    assertEquals(expectedValues.length, values.size());
    for (String expectedValue : expectedValues) {
      assertTrue(values.contains(expectedValue));
    }
  }

  private JSONObject findVisibleWhenCondition(JSONArray fields, String fieldId, String dependsOn) {
    Object visibleWhen = findField(fields, fieldId).get("visibleWhen");
    JSONObject matched = findCondition(visibleWhen, dependsOn);
    if (matched != null) {
      return matched;
    }
    throw new AssertionError("未找到字段 " + fieldId + " 的 visibleWhen 条件: " + dependsOn);
  }

  private JSONObject findCondition(Object visibleWhen, String dependsOn) {
    if (visibleWhen instanceof JSONObject condition) {
      if (condition.containsKey("anyOf")) {
        return findCondition(condition.getJSONArray("anyOf"), dependsOn);
      }
      if (condition.containsKey("allOf")) {
        return findCondition(condition.getJSONArray("allOf"), dependsOn);
      }
      if (dependsOn.equals(condition.getStr("field"))) {
        return condition;
      }
      return null;
    }
    JSONArray conditions = (JSONArray) visibleWhen;
    for (int i = 0; i < conditions.size(); i++) {
      JSONObject condition = findCondition(conditions.get(i), dependsOn);
      if (condition != null) {
        return condition;
      }
    }
    return null;
  }
}
