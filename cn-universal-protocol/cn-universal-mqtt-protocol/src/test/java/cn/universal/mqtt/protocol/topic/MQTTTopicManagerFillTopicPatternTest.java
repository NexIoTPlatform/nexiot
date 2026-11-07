package cn.universal.mqtt.protocol.topic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("fillTopicPattern 方法测试")
class MQTTTopicManagerFillTopicPatternTest {

  private MQTTTopicManager topicManager;

  @BeforeEach
  void setUp() {
    topicManager = new MQTTTopicManager();
  }

  @Test
  @DisplayName("支持传统 productKey/deviceId 占位符")
  void shouldFillLegacyPlaceholders() {
    String topic =
        topicManager.fillTopicPattern("$thing/down/#{productKey}/#{deviceId}", "prodA", "devA");
    assertEquals("$thing/down/prodA/devA", topic);
  }

  @Test
  @DisplayName("支持网关子设备占位符和别名")
  void shouldFillGatewayAndSubDevicePlaceholders() {
    Map<String, String> variables = new HashMap<>();
    variables.put("productKey", "gwPk");
    variables.put("deviceId", "gwDk");
    variables.put("gatewayProductKey", "gwPk");
    variables.put("gatewayDeviceId", "gwDk");
    variables.put("gatewayPk", "gwPk");
    variables.put("gatewayDk", "gwDk");
    variables.put("subProductKey", "subPk");
    variables.put("subDeviceId", "subDk");
    variables.put("subPk", "subPk");
    variables.put("subDk", "subDk");

    String topic =
        topicManager.fillTopicPattern(
            "$thing/down/${gatewayPk}/${gatewayDk}/sub/#{subProductKey}/{{subDeviceId}}",
            variables);

    assertEquals("$thing/down/gwPk/gwDk/sub/subPk/subDk", topic);
  }

  @Test
  @DisplayName("支持四段加号通配符顺序替换")
  void shouldFillWildcardSubDevicePattern() {
    Map<String, String> variables = new HashMap<>();
    variables.put("productKey", "gwPk");
    variables.put("deviceId", "gwDk");
    variables.put("subProductKey", "subPk");
    variables.put("subDeviceId", "subDk");

    String topic = topicManager.fillTopicPattern("$thing/down/+/+/sub/+/+", variables);

    assertEquals("$thing/down/gwPk/gwDk/sub/subPk/subDk", topic);
  }

  @Test
  @DisplayName("THING_DOWN 支持构建标准子设备主题")
  void shouldBuildStandardSubDeviceTopic() {
    String topic = MQTTTopicType.THING_DOWN.buildSubDeviceTopic("gwPk", "gwDk", "subPk", "subDk");
    assertEquals("$thing/down/gwPk/gwDk/sub/subPk/subDk", topic);
  }
}
