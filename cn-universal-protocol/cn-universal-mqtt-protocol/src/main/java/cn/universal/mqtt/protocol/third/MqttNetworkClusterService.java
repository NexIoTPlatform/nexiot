package cn.universal.mqtt.protocol.third;

import cn.universal.common.event.EventTopics;
import cn.universal.common.event.processer.EventPublisher;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Coordinates MQTT network lifecycle changes across application nodes. */
@Component
public class MqttNetworkClusterService {

  static final String EVENT_TYPE = "mqttNetwork";
  static final String OP_START = "start";
  static final String OP_RELOAD = "reload";
  static final String OP_CLOSE = "close";

  private final ThirdMQTTServerManager mqttServerManager;
  private final EventPublisher eventPublisher;

  public MqttNetworkClusterService(
      ThirdMQTTServerManager mqttServerManager, EventPublisher eventPublisher) {
    this.mqttServerManager = mqttServerManager;
    this.eventPublisher = eventPublisher;
  }

  public boolean start(String unionId) {
    boolean success = startLocal(unionId);
    if (success) {
      broadcast(unionId, OP_START);
    }
    return success;
  }

  public boolean stop(String unionId) {
    boolean success = stopLocal(unionId);
    if (success) {
      broadcast(unionId, OP_CLOSE);
    }
    return success;
  }

  public boolean restart(String unionId) {
    boolean success = restartLocal(unionId);
    if (success) {
      broadcast(unionId, OP_RELOAD);
    }
    return success;
  }

  public boolean startLocal(String unionId) {
    return mqttServerManager.startMqttClient(unionId);
  }

  public boolean stopLocal(String unionId) {
    return mqttServerManager.stopMqttClient(unionId);
  }

  public boolean restartLocal(String unionId) {
    return mqttServerManager.restartMqttClient(unionId);
  }

  public boolean isRunning(String unionId) {
    return mqttServerManager.isConnected(unionId);
  }

  private void broadcast(String unionId, String operation) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("type", EVENT_TYPE);
    payload.put("networkUnionId", unionId);
    payload.put("customType", operation);
    eventPublisher.publishEvent(EventTopics.PRODUCT_CONFIG_UPDATED, payload);
  }
}
