package cn.universal.mqtt.protocol.third;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.universal.common.event.EventTopics;
import cn.universal.common.event.processer.EventPublisher;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MqttNetworkClusterServiceTest {

  private final ThirdMQTTServerManager manager = mock(ThirdMQTTServerManager.class);
  private final EventPublisher publisher = mock(EventPublisher.class);
  private final MqttNetworkClusterService service =
      new MqttNetworkClusterService(manager, publisher);

  @Test
  void broadcastsStopToOtherNodes() {
    when(manager.stopMqttClient("network-1")).thenReturn(true);

    assertTrue(service.stop("network-1"));

    ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
    verify(publisher).publishEvent(eq(EventTopics.PRODUCT_CONFIG_UPDATED), event.capture());
    Map<?, ?> payload = (Map<?, ?>) event.getValue();
    assertTrue(MqttNetworkClusterService.EVENT_TYPE.equals(payload.get("type")));
    assertTrue("network-1".equals(payload.get("networkUnionId")));
    assertTrue(MqttNetworkClusterService.OP_CLOSE.equals(payload.get("customType")));
  }
}
