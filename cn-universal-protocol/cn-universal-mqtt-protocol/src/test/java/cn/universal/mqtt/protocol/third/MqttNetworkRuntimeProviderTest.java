package cn.universal.mqtt.protocol.third;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.universal.common.enums.NetworkType;
import cn.universal.core.network.NetworkRuntimeDefinition;
import cn.universal.persistence.entity.Network;
import org.junit.jupiter.api.Test;

class MqttNetworkRuntimeProviderTest {

  private final MqttNetworkClusterService clusterService = mock(MqttNetworkClusterService.class);
  private final MqttNetworkRuntimeProvider provider =
      new MqttNetworkRuntimeProvider(clusterService);

  @Test
  void supportsBothMqttNetworkTypes() {
    assertTrue(provider.supports(NetworkType.MQTT_CLIENT.getId()));
    assertTrue(provider.supports(NetworkType.MQTT_SERVER.getId()));
    assertFalse(provider.supports(NetworkType.TCP_CLIENT.getId()));
  }

  @Test
  void doesNotStartDisabledNetwork() {
    NetworkRuntimeDefinition definition = definition(Network.STATE_DISABLED);

    assertFalse(provider.start(definition));

    verifyNoInteractions(clusterService);
  }

  @Test
  void delegatesEnabledNetworkLifecycle() {
    NetworkRuntimeDefinition definition = definition(Network.STATE_ENABLED);
    when(clusterService.start("network-1")).thenReturn(true);
    when(clusterService.stop("network-1")).thenReturn(true);
    when(clusterService.restart("network-1")).thenReturn(true);
    when(clusterService.isRunning("network-1")).thenReturn(true);

    assertTrue(provider.start(definition));
    assertTrue(provider.stop(definition));
    assertTrue(provider.restart(definition));
    assertTrue(provider.isRunning(definition));

    verify(clusterService).start("network-1");
    verify(clusterService).stop("network-1");
    verify(clusterService).restart("network-1");
    verify(clusterService).isRunning("network-1");
  }

  private NetworkRuntimeDefinition definition(int state) {
    return new NetworkRuntimeDefinition(
        1L,
        NetworkType.MQTT_CLIENT.getId(),
        "product-1",
        "network-1",
        "MQTT",
        "{}",
        state);
  }
}
