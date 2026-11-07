package cn.universal.mqtt.protocol.third;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.universal.common.event.EventMessage;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MqttNetworkRedisEventHandlerTest {

  private final MqttNetworkClusterService clusterService = mock(MqttNetworkClusterService.class);
  private final MqttNetworkRedisEventHandler handler =
      new MqttNetworkRedisEventHandler(clusterService);

  @Test
  void stopsLocalClientForClusterCloseEvent() {
    EventMessage message =
        EventMessage.builder()
            .data(
                Map.of(
                    "type",
                    MqttNetworkClusterService.EVENT_TYPE,
                    "networkUnionId",
                    "network-1",
                    "customType",
                    MqttNetworkClusterService.OP_CLOSE))
            .build();

    handler.handleProductConfigUpdated(message);

    verify(clusterService).stopLocal("network-1");
  }

  @Test
  void ignoresUnrelatedProductEvents() {
    EventMessage message = EventMessage.builder().data(Map.of("type", "server")).build();

    handler.handleProductConfigUpdated(message);

    verifyNoInteractions(clusterService);
  }
}
