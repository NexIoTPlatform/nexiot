package cn.universal.mqtt.protocol.third;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.event.EventMessage;
import cn.universal.common.event.processer.ProductConfigProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Applies MQTT network lifecycle events received from another application node. */
@Slf4j(topic = "mqtt")
@Component
public class MqttNetworkRedisEventHandler implements ProductConfigProcessor {

  private final MqttNetworkClusterService clusterService;

  public MqttNetworkRedisEventHandler(MqttNetworkClusterService clusterService) {
    this.clusterService = clusterService;
  }

  @Override
  public void handleProductConfigUpdated(EventMessage message) {
    if (message == null || message.getData() == null) {
      return;
    }
    JSONObject payload = JSONUtil.parseObj(JSONUtil.toJsonStr(message.getData()));
    if (!MqttNetworkClusterService.EVENT_TYPE.equals(payload.getStr("type"))) {
      return;
    }

    String unionId = payload.getStr("networkUnionId");
    String operation = payload.getStr("customType");
    if (StrUtil.hasBlank(unionId, operation)) {
      log.warn("[THIRD_MQTT] 忽略无效的集群生命周期事件: {}", payload);
      return;
    }

    log.info("[THIRD_MQTT] 执行集群生命周期事件: unionId={}, operation={}", unionId, operation);
    switch (operation) {
      case MqttNetworkClusterService.OP_START -> clusterService.startLocal(unionId);
      case MqttNetworkClusterService.OP_RELOAD -> clusterService.restartLocal(unionId);
      case MqttNetworkClusterService.OP_CLOSE -> clusterService.stopLocal(unionId);
      default -> log.warn("[THIRD_MQTT] 未知的集群生命周期操作: {}", operation);
    }
  }

  @Override
  public void handleProtocolUpdated(EventMessage message) {
    // MQTT network lifecycle is carried only by product configuration events.
  }
}
