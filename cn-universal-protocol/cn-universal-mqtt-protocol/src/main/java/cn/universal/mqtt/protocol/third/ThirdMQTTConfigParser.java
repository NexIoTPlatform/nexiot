package cn.universal.mqtt.protocol.third;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.mqtt.protocol.entity.MQTTProductConfig;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.persistence.entity.Network;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQTT配置解析器
 *
 * <p>负责将Network对象解析为MqttProductConfig，包含所有字段映射、类型转换、主题解析等。 工具方法集中，便于维护和单元测试。 @Author
 * gitee.com/NexIoT
 *
 * @since 2025/1/20
 */
@Component
public class ThirdMQTTConfigParser {

  @Autowired private MQTTTopicManager mqttTopicManager;

  /** 解析Network为MqttProductConfig */
  public MQTTProductConfig parse(Network network) {
    return parse(network, true);
  }

  /**
   * 解析Network为MqttProductConfig
   *
   * @param network Network对象
   * @param setTimestamps 是否设置时间戳
   * @return MQTTProductConfig
   */
  public MQTTProductConfig parse(Network network, boolean setTimestamps) {
    try {
      String networkUnionId = network.getUnionId();
      String configuration = network.getConfiguration();
      if (configuration == null || configuration.trim().isEmpty()) {
        return null;
      }

      Map<String, Object> configMap = JSONUtil.parseObj(configuration).toBean(Map.class);
      String brokerHost = getStringValue(configMap, "host", getStringValue(configMap, "url", null));
      // 如果brokerHost为空，则之间忽略
      if (StrUtil.isNullOrUndefined(brokerHost)) {
        return null;
      }
      MQTTProductConfig.MQTTProductConfigBuilder builder =
          MQTTProductConfig.builder()
              .networkUnionId(networkUnionId)
              .networkName(network.getName())
              .networkType(network.getType())
              .host(brokerHost)
              .username(getStringValue(configMap, "username", null))
              .password(getStringValue(configMap, "password", null))
              .clientId(getStringValue(configMap, "clientId", null))
              .clusterEnabled(getBooleanValue(configMap, "clusterEnabled", false))
              .connectTimeout(
                  getIntValue(
                      configMap, "connectionTimeout", getIntValue(configMap, "connectTimeout", 30)))
              .keepAliveInterval(getIntValue(configMap, "keepAliveInterval", 60))
              .cleanSession(getBooleanValue(configMap, "cleanSession", true))
              .autoReconnect(getBooleanValue(configMap, "autoReconnect", true))
              .defaultQos(getIntValue(configMap, "defaultQos", 1))
              .ssl(
                  getBooleanValue(
                      configMap, "ssl", getBooleanValue(configMap, "sslEnabled", false)))
              .sslMode(getStringValue(configMap, "sslMode", null))
              .sslCaCertKey(
                  getStringValue(
                      configMap, "sslCaCertKey", getStringValue(configMap, "sslCaKey", null)))
              .sslClientCertKey(
                  getStringValue(
                      configMap,
                      "sslClientCertKey",
                      getStringValue(configMap, "sslKey", null)))
              .sslHostnameVerify(getBooleanValue(configMap, "sslHostnameVerify", true))
              .sslCaPath(getStringValue(configMap, "sslCaPath", null))
              .sslCertPath(
                  getStringValue(
                      configMap,
                      "sslCertPath",
                      getStringValue(configMap, "SSL证书路径", null)))
              .sslKeyPath(getStringValue(configMap, "sslKeyPath", null))
              .enabled(network.isStateEnabled())
              .subscribeTopics(parseSubscribeTopics(networkUnionId, configMap));
      if (setTimestamps) {
        builder.createdTime(LocalDateTime.now()).updatedTime(LocalDateTime.now());
      }
      return builder.build();
    } catch (Exception e) {
      return null;
    }
  }

  /** 解析订阅主题 */
  public List<MQTTProductConfig.MqttTopicConfig> parseSubscribeTopics(
      String networkUnionId, Map<String, Object> configMap) {
    int defaultQos = getIntValue(configMap, "defaultQos", 1);
    return mqttTopicManager.parseThirdMQTTSubscribeTopicsFromConfig(
        networkUnionId, configMap, defaultQos);
  }

  // 工具方法
  private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
    Object value = map.get(key);
    return value != null ? String.valueOf(value) : defaultValue;
  }

  private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  private Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
    Object value = map.get(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    }
    return defaultValue;
  }
}
