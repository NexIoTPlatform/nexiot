package cn.universal.mqtt.protocol.processor.down;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.common.constant.IoTConstant;
import cn.universal.mqtt.protocol.support.MqttRouteResolver.MqttRoute;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MQTTDownFunction payload 编码测试")
class MQTTDownFunctionPayloadEncodeTest {

  private MQTTDownFunction downFunction;
  private Method encodePayloadMethod;

  @BeforeEach
  void setUp() throws Exception {
    downFunction = new MQTTDownFunction();
    encodePayloadMethod =
        MQTTDownFunction.class.getDeclaredMethod(
            "encodePayload", String.class, String.class, String.class);
    encodePayloadMethod.setAccessible(true);
  }

  @Test
  @DisplayName("HEX 编码应支持大小写不敏感的 encoderType")
  void shouldEncodeHexPayloadWithCaseInsensitiveEncoderType() throws Exception {
    Object result = encodePayloadMethod.invoke(downFunction, "hex", "AA55", null);

    assertTrue(invokeBoolean(result, "isSuccess"));
    assertArrayEquals(new byte[] {(byte) 0xAA, 0x55}, (byte[]) invoke(result, "getPayloadBytes"));
  }

  @Test
  @DisplayName("HEX 配置遇到 raw fallback JSON 时应返回明确错误")
  void shouldReturnClearErrorForHexRawFallbackPayload() throws Exception {
    Object result =
        encodePayloadMethod.invoke(
            downFunction,
            "HEX",
            "{\"function\":\"set\"}",
            IoTConstant.PAYLOAD_SOURCE_RAW_FALLBACK);

    assertFalse(invokeBoolean(result, "isSuccess"));
    assertEquals(
        "产品配置的下发编码为 HEX，但当前 encode 驱动未返回有效 HEX 报文，平台已回退为原始 JSON，无法继续下发，请完善 encode 驱动或将产品 encoderType 调整为 STRING",
        invoke(result, "getErrorMessage"));
  }

  @Test
  @DisplayName("第三方 MQTT 应直接发送 HEX 编码后的原始字节")
  void shouldPublishEncodedBytesDirectlyToThirdMqtt() throws Exception {
    ThirdMQTTServerManager thirdMqttServerManager = mock(ThirdMQTTServerManager.class);
    Field managerField = MQTTDownFunction.class.getDeclaredField("thirdMqttServerManager");
    managerField.setAccessible(true);
    managerField.set(downFunction, thirdMqttServerManager);

    byte[] payload = new byte[] {0x01, (byte) 0xFF, 0x00};
    when(thirdMqttServerManager.publishMessage("network-1", "device/down", payload))
        .thenReturn(true);

    Method pushMessageMethod =
        MQTTDownFunction.class.getDeclaredMethod(
            "pushMessage", MqttRoute.class, String.class, byte[].class);
    pushMessageMethod.setAccessible(true);

    boolean success =
        (Boolean)
            pushMessageMethod.invoke(
                downFunction, MqttRoute.third("network-1"), "device/down", payload);

    assertTrue(success);
    verify(thirdMqttServerManager)
        .publishMessage(eq("network-1"), eq("device/down"), eq(payload));
  }

  private Object invoke(Object target, String methodName) throws Exception {
    Method method = target.getClass().getDeclaredMethod(methodName);
    method.setAccessible(true);
    return method.invoke(target);
  }

  private boolean invokeBoolean(Object target, String methodName) throws Exception {
    return (Boolean) invoke(target, methodName);
  }
}
