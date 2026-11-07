package cn.universal.mqtt.protocol.third;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.mqtt.protocol.entity.MQTTProductConfig;
import cn.universal.mqtt.protocol.metrics.MqttMetricsMananer;
import java.lang.reflect.Field;
import java.util.Map;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("ThirdMQTTServerManager 发布消息测试")
class ThirdMQTTServerManagerPublishTest {

  @Test
  @DisplayName("原始字节消息应保持内容并使用网络默认 QoS")
  void shouldPublishRawBytesWithConfiguredQos() throws Exception {
    ThirdMQTTServerManager manager = new ThirdMQTTServerManager();
    MqttAsyncClient client = mock(MqttAsyncClient.class);
    MqttMetricsMananer metricsCollector = mock(MqttMetricsMananer.class);
    when(client.isConnected()).thenReturn(true);

    getMap(manager, "networkClients").put("network-1", client);
    getMap(manager, "networkConfigs")
        .put("network-1", MQTTProductConfig.builder().defaultQos(2).build());
    setField(manager, "metricsCollector", metricsCollector);

    byte[] payload = new byte[] {0x01, (byte) 0xFF, 0x00};

    assertTrue(manager.publishMessage("network-1", "device/down", payload));

    ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
    verify(client)
        .publish(
            eq("device/down"),
            messageCaptor.capture(),
            any(),
            any(MqttActionListener.class));
    assertArrayEquals(payload, messageCaptor.getValue().getPayload());
    assertEquals(2, messageCaptor.getValue().getQos());
    verify(metricsCollector).incrementPublishMessageCount();
  }

  @SuppressWarnings("unchecked")
  private <T> Map<String, T> getMap(ThirdMQTTServerManager manager, String fieldName)
      throws Exception {
    Field field = ThirdMQTTServerManager.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return (Map<String, T>) field.get(manager);
  }

  private void setField(ThirdMQTTServerManager manager, String fieldName, Object value)
      throws Exception {
    Field field = ThirdMQTTServerManager.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(manager, value);
  }
}
