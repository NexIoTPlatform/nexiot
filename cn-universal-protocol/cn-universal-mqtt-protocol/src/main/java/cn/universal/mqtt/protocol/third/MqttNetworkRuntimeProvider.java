package cn.universal.mqtt.protocol.third;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.enums.NetworkType;
import cn.universal.core.network.NetworkRuntimeDefinition;
import cn.universal.core.network.NetworkRuntimeProvider;
import cn.universal.persistence.entity.Network;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Bridges network-component lifecycle operations to the third-party MQTT runtime. */
@Component
public class MqttNetworkRuntimeProvider implements NetworkRuntimeProvider {

  private static final Set<String> SUPPORTED_TYPES =
      Set.of(NetworkType.MQTT_CLIENT.getId(), NetworkType.MQTT_SERVER.getId());

  private final MqttNetworkClusterService clusterService;

  public MqttNetworkRuntimeProvider(MqttNetworkClusterService clusterService) {
    this.clusterService = clusterService;
  }

  @Override
  public Set<String> supportedTypes() {
    return SUPPORTED_TYPES;
  }

  @Override
  public boolean start(NetworkRuntimeDefinition definition) {
    if (!isEnabled(definition)) {
      return false;
    }
    return clusterService.start(definition.unionId());
  }

  @Override
  public boolean stop(NetworkRuntimeDefinition definition) {
    return hasUnionId(definition) && clusterService.stop(definition.unionId());
  }

  @Override
  public boolean restart(NetworkRuntimeDefinition definition) {
    if (!isEnabled(definition)) {
      return stop(definition);
    }
    return clusterService.restart(definition.unionId());
  }

  @Override
  public boolean isRunning(NetworkRuntimeDefinition definition) {
    return hasUnionId(definition) && clusterService.isRunning(definition.unionId());
  }

  private boolean isEnabled(NetworkRuntimeDefinition definition) {
    return hasUnionId(definition)
        && definition.state() != null
        && definition.state() == Network.STATE_ENABLED;
  }

  private boolean hasUnionId(NetworkRuntimeDefinition definition) {
    return definition != null && StrUtil.isNotBlank(definition.unionId());
  }
}
