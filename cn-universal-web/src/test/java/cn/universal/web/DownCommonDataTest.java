package cn.universal.web;

import cn.universal.core.message.DownCommonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DownCommonData 单元测试（纯JDK21+Jackson实现，无Hutool依赖）
 * 测试覆盖：固定字段读写、动态字段读写、JSON序列化/反序列化、Map转换、业务场景、边界条件
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/02/06
 */
@DisplayName("DownCommonData 纯JDK21版单元测试")
class DownCommonDataTest {

  private DownCommonData data;
  private ObjectMapper objectMapper; // Jackson核心映射器，项目必带

  /**
   * 每个测试方法执行前初始化
   */
  @BeforeEach
  void setUp() {
    data = new DownCommonData();
    objectMapper = new ObjectMapper();
    // 可选：Jackson配置，支持空值序列化、日期格式化等
    objectMapper.findAndRegisterModules(); // 适配JDK21的模块
  }

  // ===================== 固定字段测试 =====================
  @Test
  @DisplayName("固定字段-setter/getter 正常读写")
  void testFixedFieldsBySetterGetter() {
    // 1. 用setter设置固定字段
    data.setDeviceName("温湿度传感器-001");
    data.setImei("123456789012345");
    data.setDeviceModel("TH-Sensor-V3");
    data.setLongitude("120.123456");
    data.setLatitude("30.654321");
    data.setSlaveAddress("01");

    // 2. 用getter验证
    assertEquals("温湿度传感器-001", data.getDeviceName());
    assertEquals("123456789012345", data.getImei());
    assertEquals("TH-Sensor-V3", data.getDeviceModel());
    assertEquals("120.123456", data.getLongitude());
    assertEquals("30.654321", data.getLatitude());
    assertEquals("01", data.getSlaveAddress());

    // 3. 测试嵌套配置字段（JDK Map）
    Map<String, Object> config = new HashMap<>();
    config.put("timeout", 5000);
    config.put("retry", 3);
    data.setConfiguration(config);
    assertEquals(5000, data.getConfiguration().get("timeout"));
    assertEquals(3, data.getConfiguration().get("retry"));
  }

  @Test
  @DisplayName("固定字段-反序列化时自动赋值（Jackson原生能力）")
  void testFixedFieldsByDeserialization() throws Exception {
    // 1. 构造包含固定字段的JSON
    String json = "{\"deviceName\":\"智能电表-008\",\"imei\":\"987654321054321\",\"companyNo\":\"NEXIOT-2025\"}";

    // 2. 反序列化
    DownCommonData desData = objectMapper.readValue(json, DownCommonData.class);

    // 3. 验证getter能拿到值（Jackson自动调用setter）
    assertEquals("智能电表-008", desData.getDeviceName());
    assertEquals("987654321054321", desData.getImei());
    assertEquals("NEXIOT-2025", desData.getCompanyNo());
  }

  // ===================== 动态字段测试 =====================
  @Test
  @DisplayName("动态字段-put/get 正常读写，类型安全方法可用")
  void testDynamicFields() {
    // 1. 设置不同类型的动态字段（和原Hutool用法一致）
    data.put("manufacturer", "华为");
    data.put("installTime", 1738723200000L);
    data.put("isActive", true);
    data.put("version", 2.1);
    data.put("signalStrength", 98);

    // 2. 原生get获取
    assertEquals("华为", data.get("manufacturer"));
    assertEquals(1738723200000L, data.get("installTime"));
    assertEquals(true, data.get("isActive"));

    // 3. 类型安全方法获取（推荐）
    assertEquals("华为", data.getStr("manufacturer"));
    assertEquals(98, data.getInt("signalStrength"));
    assertEquals(true, data.getBool("isActive"));

    // 4. 验证存在性
    assertTrue(data.containsKey("manufacturer"));
    assertFalse(data.containsKey("unExistField"));
  }

  @Test
  @DisplayName("混合场景-固定字段+动态字段 共存且平级序列化")
  void testMixedFixedAndDynamicFields() throws Exception {
    // 1. 设置固定字段
    data.setDeviceName("混合测试设备");
    data.setGwDeviceId("GW-001-NEX");
    // 配置字段
    Map<String, Object> config = new HashMap<>();
    config.put("connectType", "MQTT");
    config.put("port", 1883);
    data.setConfiguration(config);

    // 2. 设置动态字段
    data.put("customExt", "设备扩展信息V1");
    data.put("signalStrength", 98);

    // 3. 序列化（固定+动态字段平级，无嵌套）
    String jsonStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    System.out.println(">>> 混合字段序列化结果：\n" + jsonStr);

    // 4. 验证序列化结果包含所有字段（平级）
    assertTrue(jsonStr.contains("deviceName"));
    assertTrue(jsonStr.contains("GW-001-NEX"));
    assertTrue(jsonStr.contains("customExt"));
    assertTrue(jsonStr.contains("signalStrength"));
    assertTrue(jsonStr.contains("connectType"));
    // 验证无动态字段嵌套（不是dynamicFields: { ... }）
    assertFalse(jsonStr.contains("dynamicFields"));

    // 5. 反序列化后验证所有字段可获取
    DownCommonData desData = objectMapper.readValue(jsonStr, DownCommonData.class);
    // 固定字段
    assertEquals("混合测试设备", desData.getDeviceName());
    assertEquals("GW-001-NEX", desData.getGwDeviceId());
    assertEquals("MQTT", desData.getConfiguration().get("connectType"));
    // 动态字段
    assertEquals("设备扩展信息V1", desData.getStr("customExt"));
    assertEquals(98, desData.getInt("signalStrength"));
  }

  // ===================== JSON序列化/反序列化核心测试（解决你之前的核心报错） =====================
  @Test
  @DisplayName("核心测试-序列化有字段，反序列化getter能拿到值（彻底解决原报错）")
  void testJsonSerializeAndDeserialize() throws Exception {
    // 1. 准备数据（固定字段+嵌套配置+动态字段）
    data.setDeviceName("Hutool测试设备");
    // 嵌套配置（JDK Map）
    Map<String, Object> config = new HashMap<>();
    config.put("timeout", 5000);
    config.put("retry", 3);
    data.setConfiguration(config);
    // 动态字段
    data.put("hutoolDynamic", "Hutool原生动态值");

    // 2. Jackson序列化（核心：和你原序列化结果格式完全一致）
    String jsonStr = objectMapper.writeValueAsString(data);
    System.out.println(">>> 核心测试序列化结果：" + jsonStr);
    // 序列化结果示例：{"deviceName":"Hutool测试设备","configuration":{"timeout":5000,"retry":3},"hutoolDynamic":"Hutool原生动态值"}

    // 3. Jackson反序列化（核心：解决原getDeviceName()为null的问题）
    DownCommonData desData = objectMapper.readValue(jsonStr, DownCommonData.class);

    // 4. 验证固定字段（100%能拿到值，彻底解决原报错）
    assertEquals("Hutool测试设备", desData.getDeviceName());
    assertNotNull(desData.getConfiguration());
    assertEquals(5000, desData.getConfiguration().get("timeout"));
    assertEquals(3, desData.getConfiguration().get("retry"));

    // 5. 验证动态字段
    assertEquals("Hutool原生动态值", desData.getStr("hutoolDynamic"));
    assertEquals("Hutool原生动态值", desData.get("hutoolDynamic"));
  }

  // ===================== Map转换测试 =====================
  @Test
  @DisplayName("toMap方法-转换后包含所有固定+动态字段（平级）")
  void testToMap() {
    // 1. 准备数据
    data.setDeviceName("Map转换测试设备");
    data.setImei("123123123123123");
    data.put("mapDynamic", "Map动态值");
    data.put("mapBool", false);

    // 2. 转换为Map
    Map<String, Object> map = data.toMap();
    System.out.println(">>> toMap转换结果：" + map);

    // 3. 验证Map中包含所有字段（平级）
    assertEquals("Map转换测试设备", map.get("deviceName"));
    assertEquals("123123123123123", map.get("imei"));
    assertEquals("Map动态值", map.get("mapDynamic"));
    assertEquals(false, map.get("mapBool"));
  }

  // ===================== 边界条件测试 =====================
  @Test
  @DisplayName("边界条件-null值处理（setter/put均支持）")
  void testNullValues() {
    // 1. setter设置null
    data.setDeviceName(null);
    // 2. put设置null
    data.put("customField", null);

    // 3. 验证null值获取
    assertNull(data.getDeviceName());
    assertNull(data.get("customField"));
    assertNull(data.getStr("customField"));
  }

  @Test
  @DisplayName("边界条件-不存在的字段返回null")
  void testNonExistentField() {
    assertNull(data.get("notExistField"));
    assertNull(data.getStr("notExistField"));
    assertThrows(NullPointerException.class, () -> data.getInt("notExistField"));
    assertFalse(data.containsKey("notExistField"));
  }

  // ===================== 实际业务场景测试 =====================
  @Test
  @DisplayName("业务场景-设备注册数据完整流程（设置→序列化→反序列化）")
  void testBusinessScenario_DeviceRegister() throws Exception {
    // 1. 模拟设备注册时设置全量数据
    data.setDeviceName("烟感传感器-YG001");
    data.setImei("567890123456789");
    data.setDeviceModel("YG-Sensor-V1");
    data.setLongitude("121.473701");
    data.setLatitude("31.230416");
    data.setSlaveAddress("05");
    data.setGwDeviceId("GW-Shanghai-002");

    // 2. 模拟添加业务自定义动态字段
    data.put("manufacturer", "海康威视");
    data.put("installDate", "2025-02-06");
    data.put("warrantyPeriod", 2);
    data.put("projectName", "上海某园区智慧消防");

    // 3. 模拟发送到服务器：序列化
    String registerJson = objectMapper.writeValueAsString(data);
    System.out.println(">>> 设备注册上报数据：" + registerJson);

    // 4. 模拟服务器接收：反序列化
    DownCommonData receivedDevice = objectMapper.readValue(registerJson, DownCommonData.class);

    // 5. 验证核心业务字段（固定+动态）
    assertEquals("烟感传感器-YG001", receivedDevice.getDeviceName());
    assertEquals("567890123456789", receivedDevice.getImei());
    assertEquals("GW-Shanghai-002", receivedDevice.getGwDeviceId());
    assertEquals("海康威视", receivedDevice.getStr("manufacturer"));
    assertEquals("上海某园区智慧消防", receivedDevice.getStr("projectName"));
    assertEquals(2, receivedDevice.getInt("warrantyPeriod"));
  }

  @Test
  @DisplayName("业务场景-参数校验（验证必填字段非空）")
  void testBusinessScenario_ParamValidate() {
    // 1. 模拟设置必填字段（固定+动态）
    data.setDeviceName("门禁设备-MJ001");
    data.setImei("111222333444555");
    data.put("deviceSn", "MJ20250206001"); // 动态必填字段

    // 2. 模拟参数校验逻辑
    String[] requiredFields = {"deviceName", "imei", "deviceSn"};
    for (String field : requiredFields) {
      Object value = "deviceSn".equals(field) ? data.get(field) : getFieldValue(data, field);
      assertNotNull(value, "业务校验失败：必填字段【" + field + "】不能为空");
    }

    // 3. 验证非必填字段默认null
    assertNull(data.getCompanyNo());
    assertNull(data.get("unRequiredDynamic"));
  }

  @Test
  @DisplayName("业务场景-字段重名覆盖（动态字段覆盖固定字段）")
  void testBusinessScenario_FieldNameOverwrite() throws Exception {
    // 1. 初始设置固定字段
    data.setDeviceName("初始设备名称");
    assertEquals("初始设备名称", data.getDeviceName());

    // 2. 业务中通过put设置同名字段（动态字段）
    data.put("deviceName", "更新后的设备名称");

    // 3. 序列化后反序列化，验证动态字段覆盖固定字段
    String jsonStr = objectMapper.writeValueAsString(data);
    DownCommonData desData = objectMapper.readValue(jsonStr, DownCommonData.class);

    // 4. 验证覆盖成功（动态字段优先级更高）
    assertEquals("更新后的设备名称", desData.getStr("deviceName"));
    assertNull(desData.getDeviceName()); // 固定字段为null，动态字段生效
  }

  // ===================== 辅助方法：反射获取固定字段值（参数校验用） =====================
  private Object getFieldValue(DownCommonData obj, String fieldName) {
    try {
      return obj.getClass().getMethod("get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1)).invoke(obj);
    } catch (Exception e) {
      return null;
    }
  }
}