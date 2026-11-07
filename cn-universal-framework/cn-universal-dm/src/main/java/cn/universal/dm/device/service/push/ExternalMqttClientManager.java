package cn.universal.dm.device.service.push;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.config.InstanceIdProvider;
import cn.universal.persistence.entity.bo.UPPushBO;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExternalMqttClientManager {

  private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
  private static final int DEFAULT_PUBLISH_TIMEOUT_SECONDS = 10;
  private static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;

  @Resource private InstanceIdProvider instanceIdProvider;

  private final Map<String, ClientHolder> clients = new ConcurrentHashMap<>();
  private final Map<String, Object> locks = new ConcurrentHashMap<>();

  public void publish(
      String appUniqueId,
      UPPushBO.ExternalMqttPushConfig config,
      String topic,
      String payload)
      throws Exception {
    validate(config, topic);
    log.debug(
        "[外部MQTT] 准备发布: appUniqueId={}, brokerUrl={}, topic={}, qos={}, retained={}, bytes={}",
        appUniqueId,
        config.getBrokerUrl(),
        topic,
        normalizeQos(config.getQos()),
        config.isRetained(),
        payload.getBytes(StandardCharsets.UTF_8).length);
    ClientHolder holder = client(appUniqueId, config);
    synchronized (holder) {
      ensureConnected(holder, config);
      MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
      message.setQos(normalizeQos(config.getQos()));
      message.setRetained(config.isRetained());
      IMqttToken token = holder.client.publish(topic, message);
      token.waitForCompletion(
          seconds(config.getPublishTimeoutSeconds(), DEFAULT_PUBLISH_TIMEOUT_SECONDS) * 1000L);
    }
    log.debug("[外部MQTT] 发布完成: appUniqueId={}, topic={}", appUniqueId, topic);
  }

  public void testConnection(UPPushBO.ExternalMqttPushConfig config) throws Exception {
    validate(config, StrUtil.blankToDefault(config.getTopic(), "nexiot/connection/test"));
    String clientId = resolveClientId("test", config) + "-test-" + UUID.randomUUID().toString().substring(0, 8);
    long start = System.currentTimeMillis();
    log.debug(
        "[外部MQTT] 开始测试连接: brokerUrl={}, clientId={}, timeoutSeconds={}",
        config.getBrokerUrl(),
        clientId,
        seconds(config.getConnectTimeoutSeconds(), DEFAULT_CONNECT_TIMEOUT_SECONDS));
    MqttAsyncClient client =
        new MqttAsyncClient(config.getBrokerUrl(), clientId, new MemoryPersistence());
    try {
      IMqttToken token = client.connect(connectOptions(config));
      token.waitForCompletion(
          seconds(config.getConnectTimeoutSeconds(), DEFAULT_CONNECT_TIMEOUT_SECONDS) * 1000L);
      log.debug(
          "[外部MQTT] 测试连接成功: brokerUrl={}, elapsedMs={}",
          config.getBrokerUrl(),
          System.currentTimeMillis() - start);
    } finally {
      closeClient(client);
    }
  }

  public void invalidate(String appUniqueId) {
    ClientHolder holder = clients.remove(appUniqueId);
    locks.remove(appUniqueId);
    if (holder != null) {
      closeClient(holder.client);
      log.debug("[外部MQTT] 已失效应用客户端: appUniqueId={}", appUniqueId);
    }
  }

  @PreDestroy
  public void close() {
    log.debug("[外部MQTT] 开始关闭客户端管理器: clientCount={}", clients.size());
    clients.values().forEach(holder -> closeClient(holder.client));
    clients.clear();
    locks.clear();
    log.debug("[外部MQTT] 客户端管理器已关闭");
  }

  private ClientHolder client(String appUniqueId, UPPushBO.ExternalMqttPushConfig config)
      throws Exception {
    String fingerprint = fingerprint(config);
    ClientHolder current = clients.get(appUniqueId);
    if (current != null && current.fingerprint.equals(fingerprint)) {
      log.debug("[外部MQTT] 复用应用客户端: appUniqueId={}", appUniqueId);
      return current;
    }
    synchronized (locks.computeIfAbsent(appUniqueId, ignored -> new Object())) {
      current = clients.get(appUniqueId);
      if (current != null && current.fingerprint.equals(fingerprint)) {
        log.debug("[外部MQTT] 复用应用客户端: appUniqueId={}", appUniqueId);
        return current;
      }
      if (current != null) {
        log.debug("[外部MQTT] 配置已变化，关闭旧客户端: appUniqueId={}", appUniqueId);
        closeClient(current.client);
      }
      MqttAsyncClient client =
          new MqttAsyncClient(
              config.getBrokerUrl(), resolveClientId(appUniqueId, config), new MemoryPersistence());
      ClientHolder replacement = new ClientHolder(fingerprint, client);
      clients.put(appUniqueId, replacement);
      log.debug(
          "[外部MQTT] 已创建应用客户端: appUniqueId={}, brokerUrl={}, clientId={}",
          appUniqueId,
          config.getBrokerUrl(),
          client.getClientId());
      return replacement;
    }
  }

  private void ensureConnected(ClientHolder holder, UPPushBO.ExternalMqttPushConfig config)
      throws Exception {
    if (holder.client.isConnected()) {
      return;
    }
    long start = System.currentTimeMillis();
    log.debug(
        "[外部MQTT] 开始连接: brokerUrl={}, clientId={}",
        config.getBrokerUrl(),
        holder.client.getClientId());
    IMqttToken token = holder.client.connect(connectOptions(config));
    token.waitForCompletion(
        seconds(config.getConnectTimeoutSeconds(), DEFAULT_CONNECT_TIMEOUT_SECONDS) * 1000L);
    log.debug(
        "[外部MQTT] 连接成功: brokerUrl={}, clientId={}, elapsedMs={}",
        config.getBrokerUrl(),
        holder.client.getClientId(),
        System.currentTimeMillis() - start);
  }

  private MqttConnectionOptions connectOptions(UPPushBO.ExternalMqttPushConfig config) {
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setAutomaticReconnect(true);
    options.setCleanStart(true);
    options.setConnectionTimeout(
        seconds(config.getConnectTimeoutSeconds(), DEFAULT_CONNECT_TIMEOUT_SECONDS));
    options.setKeepAliveInterval(
        seconds(config.getKeepAliveSeconds(), DEFAULT_KEEP_ALIVE_SECONDS));
    if (StrUtil.isNotBlank(config.getUsername())) {
      options.setUserName(config.getUsername());
    }
    if (StrUtil.isNotBlank(config.getPassword())) {
      options.setPassword(config.getPassword().getBytes(StandardCharsets.UTF_8));
    }
    return options;
  }

  private String resolveClientId(
      String appUniqueId, UPPushBO.ExternalMqttPushConfig config) {
    String base =
        StrUtil.blankToDefault(
            config.getClientId(), "nexiot-app-" + Integer.toHexString(appUniqueId.hashCode()));
    if (!config.isClusterClientId()) {
      return base;
    }
    String instanceId = instanceIdProvider.getInstanceId();
    return base + "-" + Integer.toHexString(instanceId.hashCode());
  }

  private void validate(UPPushBO.ExternalMqttPushConfig config, String topic) {
    if (config == null || StrUtil.isBlank(config.getBrokerUrl())) {
      throw new IllegalArgumentException("MQTT Broker 地址不能为空");
    }
    String url = config.getBrokerUrl().toLowerCase();
    if (!(url.startsWith("tcp://")
        || url.startsWith("ssl://")
        || url.startsWith("ws://")
        || url.startsWith("wss://"))) {
      throw new IllegalArgumentException("MQTT Broker 地址协议必须为 tcp、ssl、ws 或 wss");
    }
    if (StrUtil.isBlank(topic)) {
      throw new IllegalArgumentException("MQTT Topic 不能为空");
    }
    normalizeQos(config.getQos());
  }

  private int normalizeQos(Integer qos) {
    int value = qos == null ? 1 : qos;
    if (value < 0 || value > 2) {
      throw new IllegalArgumentException("MQTT QoS 必须为 0、1 或 2");
    }
    return value;
  }

  private int seconds(Integer value, int defaultValue) {
    return value == null || value <= 0 ? defaultValue : value;
  }

  private String fingerprint(UPPushBO.ExternalMqttPushConfig config) {
    return Integer.toHexString(
        Objects.hash(
            config.getBrokerUrl(),
            config.getClientId(),
            config.getUsername(),
            config.getPassword(),
            config.getKeepAliveSeconds(),
            config.getConnectTimeoutSeconds(),
            config.isClusterClientId()));
  }

  private void closeClient(MqttAsyncClient client) {
    if (client == null) {
      return;
    }
    try {
      if (client.isConnected()) {
        client.disconnect().waitForCompletion(3000);
        log.debug("[外部MQTT] 客户端已断开: clientId={}", client.getClientId());
      }
    } catch (Exception e) {
      log.debug("[外部MQTT] 断开连接失败: {}", e.getMessage());
    }
    try {
      client.close();
      log.debug("[外部MQTT] 客户端已关闭: clientId={}", client.getClientId());
    } catch (Exception e) {
      log.debug("[外部MQTT] 关闭客户端失败: {}", e.getMessage());
    }
  }

  private record ClientHolder(String fingerprint, MqttAsyncClient client) {}
}
