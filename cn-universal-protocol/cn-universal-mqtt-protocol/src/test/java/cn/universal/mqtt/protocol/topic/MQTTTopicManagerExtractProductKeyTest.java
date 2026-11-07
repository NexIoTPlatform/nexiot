package cn.universal.mqtt.protocol.topic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * MQTTTopicManager.extractProductKeyFromTopic 方法单元测试
 *
 * <p>测试覆盖范围：
 *
 * <ul>
 *   <li>标准物模型主题（属性上报、事件上报、下行）
 *   <li>透传主题（上行、下行）
 *   <li>OTA主题（上报、更新）
 *   <li>历史兼容格式
 *   <li>异常场景（空值、格式错误、特殊字符）
 * </ul>
 *
 * @author gitee.com/NexIoT
 */
@DisplayName("extractProductKeyFromTopic 方法测试")
class MQTTTopicManagerExtractProductKeyTest {

  private MQTTTopicManager topicManager;

  @BeforeEach
  void setUp() {
    topicManager = new MQTTTopicManager();
  }

  @Nested
  @DisplayName("1. 标准物模型主题测试")
  class ThingModelTopicTests {

    @Test
    @DisplayName("1.1 属性上报主题 - 标准格式")
    void testPropertyUpTopic() {
      // $thing/up/property/productKey123/device456
      String topic = "$thing/up/property/HnDdSnj1kiQg/device001";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("HnDdSnj1kiQg", result, "应该提取出正确的 productKey");
    }

    @Test
    @DisplayName("1.2 事件上报主题 - 标准格式")
    void testEventUpTopic() {
      // $thing/up/event/productKey/deviceId
      String topic = "$thing/up/event/testProduct/testDevice";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("testProduct", result, "应该提取出正确的 productKey");
    }

    @Test
    @DisplayName("1.3 下行主题 - 标准格式")
    void testThingDownTopic() {
      // $thing/down/productKey/deviceId
      String topic = "$thing/down/myProduct/myDevice";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("myProduct", result, "应该提取出正确的 productKey");
    }

    @Test
    @DisplayName("1.4 包含特殊字符的 productKey")
    void testProductKeyWithSpecialChars() {
      String topic = "$thing/up/property/Product-Key_123/device001";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("Product-Key_123", result, "应该支持 productKey 中的特殊字符");
    }

    @Test
    @DisplayName("1.5 不同动态前缀")
    void testDifferentDynamicPrefix() {
      // 使用 $custom 前缀
      String topic = "$custom/up/property/testProd/dev123";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("testProd", result, "应该支持不同的动态前缀");
    }
  }

  @Nested
  @DisplayName("2. 透传主题测试")
  class PassthroughTopicTests {

    @Test
    @DisplayName("2.1 透传上行主题")
    void testPassthroughUpTopic() {
      // $passthrough/up/productKey/deviceId
      String topic = "$passthrough/up/pass001/device999";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("pass001", result, "应该提取透传上行的 productKey");
    }

    @Test
    @DisplayName("2.2 透传下行主题")
    void testPassthroughDownTopic() {
      // $passthrough/down/productKey/deviceId
      String topic = "$passthrough/down/passProduct/passDevice";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("passProduct", result, "应该提取透传下行的 productKey");
    }
  }

  @Nested
  @DisplayName("3. OTA 主题测试")
  class OTATopicTests {

    @Test
    @DisplayName("3.1 OTA 上报主题")
    void testOTAReportTopic() {
      // $ota/report/productKey/deviceId
      String topic = "$ota/report/otaProduct/otaDevice";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("otaProduct", result, "应该提取 OTA 上报的 productKey");
    }

    @Test
    @DisplayName("3.2 OTA 更新主题")
    void testOTAUpdateTopic() {
      // $ota/update/productKey/deviceId
      String topic = "$ota/update/updateProd/updateDev";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("updateProd", result, "应该提取 OTA 更新的 productKey");
    }
  }

  @Nested
  @DisplayName("4. 历史兼容格式测试")
  class LegacyFormatTests {

    @Test
    @DisplayName("4.1 历史格式 - 两层路径")
    void testLegacyTwoLevelPath() {
      // productKey/deviceId
      String topic = "legacyProduct/legacyDevice";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("legacyProduct", result, "应该从倒数第二段提取 productKey");
    }

    @Test
    @DisplayName("4.2 历史格式 - 三层路径")
    void testLegacyThreeLevelPath() {
      // some/productKey/deviceId
      String topic = "some/path/myProduct/myDevice";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("myProduct", result, "应该从倒数第二段提取 productKey");
    }

    @Test
    @DisplayName("4.3 历史格式 - 单层路径")
    void testLegacySingleLevelPath() {
      // 只有一层，返回第一层
      String topic = "singleProduct";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("singleProduct", result, "单层路径应返回该值");
    }

    @Test
    @DisplayName("4.4 历史格式 - 多层路径")
    void testLegacyMultiLevelPath() {
      // a/b/c/productKey/deviceId
      String topic = "a/b/c/d/prodKey/devId";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("prodKey", result, "应该从倒数第二段提取 productKey");
    }
  }

  @Nested
  @DisplayName("5. 边界条件和异常测试")
  class EdgeCaseTests {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("5.1 空值测试")
    void testNullAndEmptyTopic(String topic) {
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertNull(result, "空值或 null 应返回 null");
    }

    @Test
    @DisplayName("5.2 只有斜杠的主题")
    void testOnlySlashes() {
      String topic = "///";
      String result = topicManager.extractProductKeyFromTopic(topic);
      // 空路径层级不包含产品，不能将空字符串当作可路由的产品 Key。
      assertNull(result, "只有斜杠的主题不应识别出产品");
    }

    @Test
    @DisplayName("5.3 不匹配任何模式的主题")
    void testNoPatternMatch() {
      String topic = "invalid/format/topic";
      String result = topicManager.extractProductKeyFromTopic(topic);
      // 会走历史兼容逻辑，返回倒数第二段
      assertEquals("format", result, "不匹配标准模式时走历史兼容逻辑");
    }

    @Test
    @DisplayName("5.4 包含中文的主题")
    void testChineseCharacters() {
      String topic = "$thing/up/property/产品Key/设备Id";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("产品Key", result, "应该支持中文字符");
    }

    @Test
    @DisplayName("5.5 超长主题")
    void testVeryLongTopic() {
      String productKey = "a".repeat(100);
      String topic = "$thing/up/property/" + productKey + "/device123";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals(productKey, result, "应该支持超长 productKey");
    }

    @Test
    @DisplayName("5.6 包含特殊字符的主题")
    void testSpecialCharactersInTopic() {
      String topic = "$thing/up/property/prod@#$%/dev!@#";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("prod@#$%", result, "应该能提取包含特殊字符的 productKey");
    }

    @Test
    @DisplayName("5.7 末尾有斜杠的主题")
    void testTopicWithTrailingSlash() {
      String topic = "$thing/up/property/myProd/myDev/";
      String result = topicManager.extractProductKeyFromTopic(topic);
      // 可能会匹配或走历史逻辑
      assertNotNull(result, "应该能处理末尾斜杠");
    }
  }

  @Nested
  @DisplayName("6. 参数化测试 - 批量验证")
  class ParameterizedTests {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
      "$thing/up/property/prod1/dev1, prod1",
      "$thing/up/event/prod2/dev2, prod2",
      "$thing/down/prod3/dev3, prod3",
      "$passthrough/up/prod4/dev4, prod4",
      "$passthrough/down/prod5/dev5, prod5",
      "$ota/report/prod6/dev6, prod6",
      "$ota/update/prod7/dev7, prod7",
      "legacy/prod8/dev8, prod8",
      "a/b/prod9/dev9, prod9",
      "singleProd, singleProd"
    })
    @DisplayName("6.1 批量主题格式验证")
    void testVariousTopicFormats(String topic, String expectedProductKey) {
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals(
          expectedProductKey, result, String.format("主题 %s 应提取出 %s", topic, expectedProductKey));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "$thing/up/property/Product-123/device",
          "$thing/up/property/Product_456/device",
          "$thing/up/property/Product.789/device",
          "$thing/up/property/Product:abc/device",
          "$thing/up/property/123Product/device",
          "$thing/up/property/PRODUCT/device",
          "$thing/up/property/product/device"
        })
    @DisplayName("6.2 各种 productKey 命名格式")
    void testVariousProductKeyFormats(String topic) {
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertNotNull(result, "应该能提取任何格式的 productKey");
      assertFalse(result.isEmpty(), "提取的 productKey 不应为空");
    }
  }

  @Nested
  @DisplayName("7. 真实场景测试")
  class RealWorldScenarioTests {

    @Test
    @DisplayName("7.1 智能电表场景")
    void testSmartMeterScenario() {
      String topic = "$thing/up/property/SmartMeter_V2/meter_001";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("SmartMeter_V2", result);
    }

    @Test
    @DisplayName("7.2 温湿度传感器场景")
    void testTempHumiditySensorScenario() {
      String topic = "$thing/up/event/TempSensor-Pro/sensor_hall_1";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("TempSensor-Pro", result);
    }

    @Test
    @DisplayName("7.3 工业网关场景")
    void testIndustrialGatewayScenario() {
      String topic = "$passthrough/up/Gateway_HJ212/gw_factory_01";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("Gateway_HJ212", result);
    }

    @Test
    @DisplayName("7.4 OTA 升级场景")
    void testOTAUpgradeScenario() {
      String topic = "$ota/update/FirmwareV3.0/device_12345";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("FirmwareV3.0", result);
    }

    @Test
    @DisplayName("7.5 第三方平台集成场景")
    void testThirdPartyIntegrationScenario() {
      String topic = "thirdparty/integration/ThirdProd_001/ThirdDev_001";
      String result = topicManager.extractProductKeyFromTopic(topic);
      assertEquals("ThirdProd_001", result, "第三方主题应走历史兼容逻辑");
    }
  }

  @Nested
  @DisplayName("8. 性能和稳定性测试")
  class PerformanceAndStabilityTests {

    @Test
    @DisplayName("8.1 重复调用稳定性")
    void testRepeatedCalls() {
      String topic = "$thing/up/property/testProd/testDev";

      // 多次调用应该返回相同结果
      for (int i = 0; i < 100; i++) {
        String result = topicManager.extractProductKeyFromTopic(topic);
        assertEquals("testProd", result, "重复调用应返回一致结果");
      }
    }

    @Test
    @DisplayName("8.2 不同主题混合测试")
    void testMixedTopics() {
      String[] topics = {
        "$thing/up/property/prod1/dev1",
        "$passthrough/up/prod2/dev2",
        "$ota/report/prod3/dev3",
        "legacy/prod4/dev4"
      };
      String[] expectedKeys = {"prod1", "prod2", "prod3", "prod4"};

      for (int i = 0; i < topics.length; i++) {
        String result = topicManager.extractProductKeyFromTopic(topics[i]);
        assertEquals(expectedKeys[i], result);
      }
    }
  }
}
