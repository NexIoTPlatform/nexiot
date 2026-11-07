/// *
// *
// * Copyright (c) 2025, NexIoT. All Rights Reserved.
// *
// * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权,未经授权严禁擅自商用、复制或传播。
// * @Author: gitee.com/NexIoT
// * @Email: wo8335224@gmail.com
// * @Wechat: outlookFil
// *
// *
// */
//
// package cn.universal.core.message;
//
// import cn.hutool.json.JSONObject;
// import cn.hutool.json.JSONUtil;
// import cn.universal.common.constant.IoTConstant.DownCmd;
// import org.junit.Test;
//
// import java.util.HashMap;
// import java.util.Map;
//
// import static org.junit.Assert.*;
//
/// **
// * UnifiedDownlinkCommand 单元测试
// *
// * @version 1.0
// * @since 2025/10/25
// */
// public class UnifiedDownlinkCommandTest {
//
//  @Test
//  public void testFromJson() {
//    // 准备测试数据
//    JSONObject json = new JSONObject();
//    json.set("productKey", "test-product-001");
//    json.set("deviceId", "test-device-001");
//    json.set("cmd", "DEV_ADD");
//    json.set("appUnionId", "user-001");
//
//    // 执行转换
//    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(json);
//
//    // 验证结果
//    assertNotNull(command);
//    assertEquals("test-product-001", command.getProductKey());
//    assertEquals("test-device-001", command.getDeviceId());
//    assertEquals(DownCmd.DEV_ADD, command.getCmd());
//    assertEquals("user-001", command.getAppUnionId());
//  }
//
//  @Test
//  public void testFromString() {
//    // 准备JSON字符串
//    String jsonString =
//
// "{\"productKey\":\"test-product-002\",\"deviceId\":\"test-device-002\",\"cmd\":\"DEV_FUNCTION\",\"function\":{\"messageType\":\"FUNCTIONS\",\"function\":\"turnOn\",\"data\":{\"switch\":true}}}";
//
//    // 执行转换
//    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromString(jsonString);
//
//    // 验证结果
//    assertNotNull(command);
//    assertEquals("test-product-002", command.getProductKey());
//    assertEquals("test-device-002", command.getDeviceId());
//    assertEquals(DownCmd.DEV_FUNCTION, command.getCmd());
//    assertNotNull(command.getFunction());
//    assertTrue(command.getFunction().containsKey("function"));
//  }
//
//  @Test
//  public void testFromMap() {
//    // 准备Map数据
//    Map<String, Object> map = new HashMap<>();
//    map.put("productKey", "test-product-003");
//    map.put("deviceId", "test-device-003");
//    map.put("cmd", "DEV_DEL");
//    map.put("applicationId", "app-001");
//
//    // 执行转换
//    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromMap(map);
//
//    // 验证结果
//    assertNotNull(command);
//    assertEquals("test-product-003", command.getProductKey());
//    assertEquals("test-device-003", command.getDeviceId());
//    assertEquals(DownCmd.DEV_DEL, command.getCmd());
//    assertEquals("app-001", command.getApplicationId());
//  }
//
//  @Test
//  public void testBuilder() {
//    // 使用Builder构建
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .productKey("test-product-004")
//            .deviceId("test-device-004")
//            .cmd(DownCmd.DEV_FUNCTION)
//            .appUnionId("user-004")
//            .build();
//
//    // 验证结果
//    assertNotNull(command);
//    assertEquals("test-product-004", command.getProductKey());
//    assertEquals("test-device-004", command.getDeviceId());
//    assertEquals(DownCmd.DEV_FUNCTION, command.getCmd());
//  }
//
//  @Test
//  public void testChainMethods() {
//    // 测试链式调用
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .build()
//            .withProductKey("test-product-005")
//            .withDeviceId("test-device-005")
//            .withCmd(DownCmd.DEV_ADD)
//            .withAppUnionId("user-005")
//            .withApplicationId("app-005")
//            .addExtension("customField", "customValue")
//            .withSource("web-api");
//
//    // 验证结果
//    assertEquals("test-product-005", command.getProductKey());
//    assertEquals("test-device-005", command.getDeviceId());
//    assertEquals(DownCmd.DEV_ADD, command.getCmd());
//    assertEquals("customValue", command.getExtension("customField"));
//    assertEquals("web-api", command.getMetadata().getSource());
//  }
//
//  @Test
//  public void testValidate_Success() {
//    // 准备有效数据
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .productKey("test-product-006")
//            .deviceId("test-device-006")
//            .cmd(DownCmd.DEV_ADD)
//            .build();
//
//    // 验证应该通过
//    assertDoesNotThrow(() -> command.validate());
//  }
//
//  @Test
//  public void testValidate_MissingProductKey() {
//    // 缺少productKey
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder().deviceId("test-device-007").cmd(DownCmd.DEV_ADD).build();
//
//    // 验证应该失败
//    IllegalArgumentException exception =
//        assertThrows(IllegalArgumentException.class, () -> command.validate());
//    assertTrue(exception.getMessage().contains("productKey"));
//  }
//
//  @Test
//  public void testValidate_MissingCmd() {
//    // 缺少cmd
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .productKey("test-product-008")
//            .deviceId("test-device-008")
//            .build();
//
//    // 验证应该失败
//    IllegalArgumentException exception =
//        assertThrows(IllegalArgumentException.class, () -> command.validate());
//    assertTrue(exception.getMessage().contains("cmd"));
//  }
//
//  @Test
//  public void testValidate_MissingDeviceId() {
//    // DEV_ADD指令缺少deviceId
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .productKey("test-product-009")
//            .cmd(DownCmd.DEV_ADD)
//            .build();
//
//    // 验证应该失败
//    IllegalArgumentException exception =
//        assertThrows(IllegalArgumentException.class, () -> command.validate());
//    assertTrue(
//        exception.getMessage().contains("deviceId") || exception.getMessage().contains("iotId"));
//  }
//
//  @Test
//  public void testToJson() {
//    // 准备命令对象
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .productKey("test-product-010")
//            .deviceId("test-device-010")
//            .cmd(DownCmd.DEV_FUNCTION)
//            .appUnionId("user-010")
//            .build();
//
//    Map<String, Object> functionMap = new HashMap<>();
//    functionMap.put("function", "turnOn");
//    functionMap.put("data", Map.of("switch", true));
//    command.setFunction(functionMap);
//
//    // 转换为JSON
//    JSONObject json = command.toJson();
//
//    // 验证结果
//    assertNotNull(json);
//    assertEquals("test-product-010", json.getStr("productKey"));
//    assertEquals("test-device-010", json.getStr("deviceId"));
//    assertEquals(DownCmd.DEV_FUNCTION.getValue(), json.getStr("cmd"));
//    assertTrue(json.containsKey("function"));
//  }
//
//  @Test
//  public void testExtensions() {
//    // 测试扩展字段
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .productKey("test-product-011")
//            .cmd(DownCmd.DEV_ADD)
//            .build();
//
//    // 添加扩展字段
//    command.addExtension("customField1", "value1");
//    command.addExtension("customField2", 123);
//    command.addExtension("customField3", true);
//
//    // 验证扩展字段
//    assertEquals("value1", command.getExtension("customField1"));
//    assertEquals(123, command.getExtension("customField2"));
//    assertEquals(true, command.getExtension("customField3"));
//    assertNull(command.getExtension("nonExistentField"));
//
//    // 测试带默认值的获取
//    assertEquals("default", command.getExtension("nonExistentField", "default"));
//  }
//
//  @Test
//  public void testMetadata() {
//    // 测试元数据
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .productKey("test-product-012")
//            .cmd(DownCmd.DEV_FUNCTION)
//            .build()
//            .withSource("web-api");
//
//    UnifiedDownlinkCommand.CommandMetadata metadata = command.getMetadata();
//    assertNotNull(metadata);
//    assertEquals("web-api", metadata.getSource());
//
//    // 手动设置元数据
//    metadata.setPriority(5);
//    metadata.setTimeout(5000L);
//    metadata.setRetryCount(3);
//    metadata.setRequireResponse(true);
//
//    assertEquals(5, metadata.getPriority());
//    assertEquals(5000L, metadata.getTimeout());
//    assertEquals(3, metadata.getRetryCount());
//    assertTrue(metadata.getRequireResponse());
//  }
//
//  @Test
//  public void testFromJson_WithExtensions() {
//    // 测试包含扩展字段的JSON转换
//    String jsonString =
//
// "{\"productKey\":\"test-product-013\",\"deviceId\":\"test-device-013\",\"cmd\":\"DEV_ADD\",\"data\":{\"imei\":\"123456789\",\"location\":\"Beijing\"}}";
//
//    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromString(jsonString);
//
//    // 验证扩展字段
//    assertNotNull(command.getExtensions());
//    assertEquals("123456789", command.getExtension("imei"));
//    assertEquals("Beijing", command.getExtension("location"));
//  }
//
//  @Test
//  public void testToDownRequest() {
//    // 测试转换为DownRequest
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .productKey("test-product-014")
//            .deviceId("test-device-014")
//            .cmd(DownCmd.DEV_FUNCTION)
//            .gwProductKey("gateway-001")
//            .gwDeviceId("gateway-device-001")
//            .build();
//
//    DownRequest downRequest = command.toDownRequest();
//
//    // 验证转换结果
//    assertNotNull(downRequest);
//    assertEquals("test-product-014", downRequest.getProductKey());
//    assertEquals("test-device-014", downRequest.getDeviceId());
//    assertEquals(DownCmd.DEV_FUNCTION, downRequest.getCmd());
//    assertEquals("gateway-001", downRequest.getGwProductKey());
//    assertEquals("gateway-device-001", downRequest.getGwDeviceId());
//  }
//
//  @Test
//  public void testFromJson_EmptyOrNull() {
//    // 测试空JSON
//    assertThrows(IllegalArgumentException.class, () -> UnifiedDownlinkCommand.fromJson(null));
//
//    assertThrows(
//        IllegalArgumentException.class, () -> UnifiedDownlinkCommand.fromJson(new JSONObject()));
//  }
//
//  @Test
//  public void testFromString_EmptyOrNull() {
//    // 测试空字符串
//    assertThrows(IllegalArgumentException.class, () -> UnifiedDownlinkCommand.fromString(null));
//
//    assertThrows(IllegalArgumentException.class, () -> UnifiedDownlinkCommand.fromString(""));
//
//    assertThrows(IllegalArgumentException.class, () -> UnifiedDownlinkCommand.fromString("   "));
//  }
//
//  @Test
//  public void testFromMap_EmptyOrNull() {
//    // 测试空Map
//    assertThrows(IllegalArgumentException.class, () -> UnifiedDownlinkCommand.fromMap(null));
//
//    assertThrows(
//        IllegalArgumentException.class, () -> UnifiedDownlinkCommand.fromMap(new HashMap<>()));
//  }
//
//  @Test
//  public void testToString() {
//    // 测试toString
//    UnifiedDownlinkCommand command =
//        UnifiedDownlinkCommand.builder()
//            .productKey("test-product-015")
//            .deviceId("test-device-015")
//            .cmd(DownCmd.DEV_ADD)
//            .msgId("msg-001")
//            .build();
//
//    String str = command.toString();
//
//    // 验证包含关键信息
//    assertNotNull(str);
//    assertTrue(str.contains("test-product-015"));
//    assertTrue(str.contains("test-device-015"));
//    assertTrue(str.contains("DEV_ADD"));
//    assertTrue(str.contains("msg-001"));
//  }
// }
