package cn.universal.mqtt.protocol.processor.down;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.mqtt.protocol.entity.MQTTDownRequest;
import cn.universal.mqtt.protocol.support.MqttRouteResolver.MqttRoute;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MQTT 子设备主题选择测试")
class MQTTDownFunctionTopicSelectionTest {

  private MQTTDownFunction downFunction;
  private Method buildDownTopicMethod;

  @BeforeEach
  void setUp() throws Exception {
    downFunction = new MQTTDownFunction();

    Field mqttTopicManagerField = MQTTDownFunction.class.getDeclaredField("mqttTopicManager");
    mqttTopicManagerField.setAccessible(true);
    mqttTopicManagerField.set(downFunction, new MQTTTopicManager());

    buildDownTopicMethod =
        MQTTDownFunction.class.getDeclaredMethod("buildDownTopic", MQTTDownRequest.class, MqttRoute.class);
    buildDownTopicMethod.setAccessible(true);
  }

  @Test
  @DisplayName("子设备专用主题模板应优先于老 downTopic")
  void shouldPreferSubDeviceDownTopic() throws Exception {
    MQTTDownRequest request = new MQTTDownRequest();
    request.setProductKey("gatewayPk");
    request.setDeviceId("gatewayDk");
    request.setGwProductKey("gatewayPk");
    request.setGwDeviceId("gatewayDk");
    request.setSubProductKey("subPk");
    request.setSubDeviceId("subDk");
    request.setIsGatewayProxy(true);
    request.setDownTopic("$thing/down/${productKey}/${deviceId}");
    request.setSubDeviceDownTopic("$thing/down/${gatewayPk}/${gatewayDk}/sub/${subPk}/${subDk}");

    String topic = (String) buildDownTopicMethod.invoke(downFunction, request, MqttRoute.system());

    assertEquals("$thing/down/gatewayPk/gatewayDk/sub/subPk/subDk", topic);
  }

  @Test
  @DisplayName("未配置子设备专用主题时应回退老 downTopic")
  void shouldFallbackToLegacyDownTopic() throws Exception {
    MQTTDownRequest request = new MQTTDownRequest();
    request.setProductKey("gatewayPk");
    request.setDeviceId("gatewayDk");
    request.setSubProductKey("subPk");
    request.setSubDeviceId("subDk");
    request.setIsGatewayProxy(true);
    request.setDownTopic("$thing/down/${productKey}/${deviceId}");

    String topic = (String) buildDownTopicMethod.invoke(downFunction, request, MqttRoute.system());

    assertEquals("$thing/down/gatewayPk/gatewayDk", topic);
  }
}
