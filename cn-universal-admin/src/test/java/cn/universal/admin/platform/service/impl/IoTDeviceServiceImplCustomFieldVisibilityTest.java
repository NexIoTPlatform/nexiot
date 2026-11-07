package cn.universal.admin.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("IoTDeviceServiceImpl 自定义字段可见性测试")
class IoTDeviceServiceImplCustomFieldVisibilityTest {

  private final IoTDeviceServiceImpl service = new IoTDeviceServiceImpl();

  @Test
  @DisplayName("导入模板应区分普通必填、条件必填和非必填")
  void shouldResolveConditionalRequiredLabel() throws Exception {
    assertEquals("是", resolveCustomFieldRequired(field("slaveAddress", true, null)));
    assertEquals(
        "条件",
        resolveCustomFieldRequired(
            field("serialPort", true, visibleWhenIn("transportType", "rtu-serial"))));
    assertEquals("否", resolveCustomFieldRequired(field("baudRate", false, null)));
  }

  @Test
  @DisplayName("新Modbus TCP和串口字段应按visibleWhen参与导入必填校验")
  void shouldEvaluateModbusTransportVisibility() throws Exception {
    JSONArray fields = new JSONArray();
    fields.add(selectField("transportType", "tcp-client"));
    fields.add(field("host", true, visibleWhenIn("transportType", "tcp-client", "rtu-tcp")));
    fields.add(field("serialPort", true, visibleWhenIn("transportType", "rtu-serial", "ascii-serial")));

    JSONObject host = fields.getJSONObject(1);
    JSONObject serialPort = fields.getJSONObject(2);

    assertTrue(isCustomFieldVisible(host, Map.of(), fields));
    assertFalse(isCustomFieldVisible(serialPort, Map.of(), fields));

    Map<String, Object> serialValues = Map.of("transportType", "rtu-serial");
    assertFalse(isCustomFieldVisible(host, serialValues, fields));
    assertTrue(isCustomFieldVisible(serialPort, serialValues, fields));
  }

  @Test
  @DisplayName("设备新增应从configuration抽取产品自定义字段进入下行data")
  void shouldExtractCustomFieldsFromConfigurationWhenCreatingDevice() throws Exception {
    IoTDeviceBO request =
        IoTDeviceBO.builder()
            .configuration(
                "{\"transportType\":\"tcp-client\",\"host\":\"43.143.97.93\","
                    + "\"port\":26504,\"slaveAddress\":1,\"ignored\":\"x\"}")
            .build();
    JSONObject data = new JSONObject();

    mergeCreateDeviceConfiguration(data, request, productWithCustomFields());

    assertEquals("tcp-client", data.getStr("transportType"));
    assertEquals("43.143.97.93", data.getStr("host"));
    assertEquals(26504, data.getInt("port"));
    assertEquals(1, data.getInt("slaveAddress"));
    assertFalse(data.containsKey("ignored"));
  }

  @Test
  @DisplayName("设备更新切回Modbus TCP时应清理旧串口字段")
  void shouldPruneStaleSerialFieldsWhenDeviceConfigSwitchesToTcp() throws Exception {
    IoTDeviceServiceImpl service = serviceWithProductCustomFields();
    IoTDevice existingDevice =
        IoTDevice.builder()
            .productKey("pk1")
            .configuration(
                "{\"transportType\":\"rtu-serial\",\"serialPort\":\"/dev/ttyUSB0\","
                    + "\"baudRate\":9600,\"host\":\"10.0.0.8\",\"port\":502,"
                    + "\"slaveAddress\":7,\"keepMe\":\"x\"}")
            .build();
    IoTDeviceBO request =
        IoTDeviceBO.builder()
            .configuration(
                "{\"transportType\":\"tcp-client\",\"host\":\"43.143.97.93\","
                    + "\"port\":26504,\"slaveAddress\":1}")
            .build();

    JSONObject merged = mergeDeviceConfiguration(service, request, existingDevice);

    assertEquals("tcp-client", merged.getStr("transportType"));
    assertEquals("43.143.97.93", merged.getStr("host"));
    assertEquals(26504, merged.getInt("port"));
    assertEquals(1, merged.getInt("slaveAddress"));
    assertEquals("x", merged.getStr("keepMe"));
    assertFalse(merged.containsKey("serialPort"));
    assertFalse(merged.containsKey("baudRate"));
  }

  @Test
  @DisplayName("设备更新切到Modbus串口时应清理旧TCP字段")
  void shouldPruneStaleTcpFieldsWhenDeviceConfigSwitchesToSerial() throws Exception {
    IoTDeviceServiceImpl service = serviceWithProductCustomFields();
    IoTDevice existingDevice =
        IoTDevice.builder()
            .productKey("pk1")
            .configuration(
                "{\"transportType\":\"tcp-client\",\"host\":\"43.143.97.93\","
                    + "\"port\":26504,\"slaveAddress\":1,\"keepMe\":\"x\"}")
            .build();
    IoTDeviceBO request =
        IoTDeviceBO.builder()
            .configuration(
                "{\"transportType\":\"ascii-serial\",\"serialPort\":\"COM3\","
                    + "\"baudRate\":19200,\"slaveAddress\":5}")
            .build();

    JSONObject merged = mergeDeviceConfiguration(service, request, existingDevice);

    assertEquals("ascii-serial", merged.getStr("transportType"));
    assertEquals("COM3", merged.getStr("serialPort"));
    assertEquals(19200, merged.getInt("baudRate"));
    assertEquals(5, merged.getInt("slaveAddress"));
    assertEquals("x", merged.getStr("keepMe"));
    assertFalse(merged.containsKey("host"));
    assertFalse(merged.containsKey("port"));
  }

  @Test
  @DisplayName("普通产品和设备服务应识别工业协议别名")
  void shouldRecognizeShortModbusTransportAliasesInAdminServices() throws Exception {
    assertEquals("MODBUS", normalizeDeviceIndustrialProtocol("rtu-serial"));
    assertEquals("MODBUS", normalizeDeviceIndustrialProtocol("rtu-over-tcp"));
    assertEquals("MODBUS", normalizeDeviceIndustrialProtocol("ascii-serial"));
    assertEquals("MODBUS", normalizeProductIndustrialProtocol("rtu-serial"));
    assertEquals("MODBUS", normalizeProductIndustrialProtocol("rtu-over-tcp"));
    assertEquals("MODBUS", normalizeProductIndustrialProtocol("ascii-serial"));
    assertEquals("IEC104", normalizeDeviceIndustrialProtocol("iec-60870-5-104"));
    assertEquals("IEC104", normalizeProductIndustrialProtocol("iec-60870-5-104"));
    assertEquals("NATIVE", defaultProductIndustrialDriver("IEC104"));
    assertEquals("GENERIC_TCP", normalizeDeviceIndustrialProtocol("tcp-client"));
    assertEquals("GENERIC_UDP", normalizeDeviceIndustrialProtocol("generic_udp"));
    assertEquals("GENERIC_TCP", normalizeProductIndustrialProtocol("generic-tcp"));
    assertEquals("GENERIC_UDP", normalizeProductIndustrialProtocol("udp-client"));
    assertEquals("NATIVE", defaultProductIndustrialDriver("GENERIC_TCP"));
  }

  @Test
  @DisplayName("通用请求响应产品应固定使用原生驱动和对应连接模式")
  void shouldPrepareGenericRequestResponseProductDefaults() throws Exception {
    IoTProductServiceImpl productService = new IoTProductServiceImpl();
    Method method =
        IoTProductServiceImpl.class.getDeclaredMethod(
            "prepareIndustrialProductConfig", IoTProduct.class, IoTProduct.class);
    method.setAccessible(true);

    IoTProduct tcp = genericProduct("GENERIC_TCP");
    method.invoke(productService, tcp, null);
    JSONObject tcpIndustrial = JSONUtil.parseObj(tcp.getConfiguration()).getJSONObject("industrial");
    assertEquals("GENERIC_TCP", tcp.getTransportProtocol());
    assertEquals("NATIVE", tcpIndustrial.getStr("driverType"));
    assertEquals("TCP_CLIENT", tcpIndustrial.getStr("connectionMode"));

    IoTProduct udp = genericProduct("GENERIC_UDP");
    method.invoke(productService, udp, null);
    JSONObject udpIndustrial = JSONUtil.parseObj(udp.getConfiguration()).getJSONObject("industrial");
    assertEquals("GENERIC_UDP", udp.getTransportProtocol());
    assertEquals("NATIVE", udpIndustrial.getStr("driverType"));
    assertEquals("UDP", udpIndustrial.getStr("connectionMode"));
  }

  @Test
  @DisplayName("新增设备应拒绝中文或异常字符设备序列号")
  void shouldRejectInvalidDeviceIdWhenCreatingDevice() throws Exception {
    IoTDeviceBO validDevice = IoTDeviceBO.builder().deviceId(" DL299NEXIOT-01 ").build();
    assertNull(validateNewDeviceId(validDevice));
    assertEquals("DL299NEXIOT-01", validDevice.getDeviceId());

    assertEquals(
        "设备序列号格式不合法，仅支持1-64位英文字母、数字、下划线、点、冒号或中横线",
        validateNewDeviceId(IoTDeviceBO.builder().deviceId("设备001").build()));
    assertEquals(
        "设备序列号格式不合法，仅支持1-64位英文字母、数字、下划线、点、冒号或中横线",
        validateNewDeviceId(IoTDeviceBO.builder().deviceId("DL299NEXIOT�").build()));
  }

  private String resolveCustomFieldRequired(JSONObject field) throws Exception {
    Method method = IoTDeviceServiceImpl.class.getDeclaredMethod("resolveCustomFieldRequired", JSONObject.class);
    method.setAccessible(true);
    return (String) method.invoke(service, field);
  }

  private boolean isCustomFieldVisible(JSONObject field, Map<String, Object> values, JSONArray fields)
      throws Exception {
    Method method =
        IoTDeviceServiceImpl.class.getDeclaredMethod(
            "isCustomFieldVisible", JSONObject.class, Map.class, JSONArray.class);
    method.setAccessible(true);
    return (Boolean) method.invoke(service, field, values, fields);
  }

  private JSONObject mergeDeviceConfiguration(
      IoTDeviceServiceImpl service, IoTDeviceBO request, IoTDevice existingDevice) throws Exception {
    Method method =
        IoTDeviceServiceImpl.class.getDeclaredMethod(
            "mergeDeviceConfiguration", IoTDeviceBO.class, IoTDevice.class);
    method.setAccessible(true);
    return (JSONObject) method.invoke(service, request, existingDevice);
  }

  private void mergeCreateDeviceConfiguration(
      JSONObject target, IoTDeviceBO request, IoTProduct product) throws Exception {
    Method method =
        IoTDeviceServiceImpl.class.getDeclaredMethod(
            "mergeDeviceConfigurationFromRequest", JSONObject.class, IoTDeviceBO.class, IoTProduct.class);
    method.setAccessible(true);
    method.invoke(service, target, request, product);
  }

  private String normalizeDeviceIndustrialProtocol(String value) throws Exception {
    Method method = IoTDeviceServiceImpl.class.getDeclaredMethod("normalizeIndustrialProtocol", String.class);
    method.setAccessible(true);
    return (String) method.invoke(service, value);
  }

  private String normalizeProductIndustrialProtocol(String value) throws Exception {
    IoTProductServiceImpl productService = new IoTProductServiceImpl();
    Method method = IoTProductServiceImpl.class.getDeclaredMethod("normalizeIndustrialProtocol", String.class);
    method.setAccessible(true);
    return (String) method.invoke(productService, value);
  }

  private String defaultProductIndustrialDriver(String protocol) throws Exception {
    IoTProductServiceImpl productService = new IoTProductServiceImpl();
    Method method = IoTProductServiceImpl.class.getDeclaredMethod("defaultIndustrialDriver", String.class);
    method.setAccessible(true);
    return (String) method.invoke(productService, protocol);
  }

  private String validateNewDeviceId(IoTDeviceBO request) throws Exception {
    Method method = IoTDeviceServiceImpl.class.getDeclaredMethod("validateNewDeviceId", IoTDeviceBO.class);
    method.setAccessible(true);
    return (String) method.invoke(service, request);
  }

  private IoTProduct genericProduct(String protocol) {
    return IoTProduct.builder()
        .thirdPlatform("industrial")
        .transportProtocol(protocol)
        .configuration(
            "{\"industrial\":{\"protocol\":\""
                + protocol
                + "\",\"driverType\":\"PLC4X\",\"connectionMode\":\"TCP_CLIENT\"}}")
        .build();
  }

  private IoTDeviceServiceImpl serviceWithProductCustomFields() {
    IoTDeviceServiceImpl configuredService = new IoTDeviceServiceImpl();
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    when(productMapper.getProductByProductKey("pk1")).thenReturn(productWithCustomFields());
    ReflectionTestUtils.setField(configuredService, "ioTProductMapper", productMapper);
    return configuredService;
  }

  private IoTProduct productWithCustomFields() {
    return IoTProduct.builder()
        .productKey("pk1")
        .thirdPlatform("mqtt")
        .thirdConfiguration(
            "{\"customField\":["
                + "{\"id\":\"transportType\"},{\"id\":\"host\"},{\"id\":\"port\"},"
                + "{\"id\":\"slaveAddress\"},{\"id\":\"serialPort\"},{\"id\":\"baudRate\"}"
                + "]}")
        .build();
  }

  private JSONObject selectField(String id, String defaultValue) {
    JSONObject field = field(id, true, null);
    field.set("type", "select");
    field.set("defaultValue", defaultValue);
    return field;
  }

  private JSONObject field(String id, boolean required, JSONObject visibleWhen) {
    JSONObject field = new JSONObject();
    field.set("id", id);
    field.set("name", id);
    field.set("type", "text");
    field.set("required", required);
    if (visibleWhen != null) {
      field.set("visibleWhen", visibleWhen);
    }
    return field;
  }

  private JSONObject visibleWhenIn(String dependsOn, String... values) {
    JSONObject condition = new JSONObject();
    condition.set("field", dependsOn);
    JSONArray acceptedValues = new JSONArray();
    for (String value : values) {
      acceptedValues.add(value);
    }
    condition.set("in", acceptedValues);
    return condition;
  }
}
