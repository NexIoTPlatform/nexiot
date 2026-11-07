/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.mqtt.protocol.third;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.config.InstanceIdProvider;
import cn.universal.common.utils.PayloadCodecUtils;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.mqtt.protocol.config.MqttConstant;
import cn.universal.mqtt.protocol.entity.MQTTProductConfig;
import cn.universal.mqtt.protocol.entity.MQTTPublishMessage;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.metrics.MqttMetricsMananer;
import cn.universal.mqtt.protocol.processor.MqttUPProcessorChain;
import cn.universal.mqtt.protocol.ssl.MqttTlsSupport;
import cn.universal.mqtt.protocol.system.SysMQTTManager;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * MQTT服务器管理器 - 重构版
 *
 * <p>专注于MQTT客户端的连接管理和消息处理 配置管理交由MqttConfigService统一处理 系统MQTT交由SystemMqttManager专门处理
 *
 * @version 3.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Service
@DependsOn("sysMQTTManager")
public class ThirdMQTTServerManager implements ApplicationListener<ApplicationReadyEvent> {

  @Autowired private cn.universal.common.util.DelayedStartupUtil delayedStartupUtil;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    // 等待SysMQTTManager初始化完成后再启动
    delayedStartupUtil.executeAfterBeanReady(
        () -> {
          try {
            log.info("开始延迟初始化ThirdMQTTServerManager...");
            initialize();
            log.info("ThirdMQTTServerManager初始化完成");
          } catch (Exception e) {
            log.error("ThirdMQTTServerManager初始化失败", e);
          }
        },
        "sysMQTTManager",
        10,
        executorService);
  }

  @Autowired private ThirdMQTTConfigService configService;

  @Autowired private SysMQTTManager sysMQTTManager;

  @Autowired private IoTProductDeviceService iotProductDeviceService;

  /** 命名虚拟线程执行器 - 用于需要特定命名的任务 */
  @Resource(name = "namedVirtualThreadExecutor")
  private ExecutorService executorService;

  @Autowired private MqttUPProcessorChain processorChain;

  @Autowired private MqttMetricsMananer metricsCollector;
  @Autowired private MQTTTopicManager mqttTopicManager;
  @Autowired private MqttTlsSupport mqttTlsSupport;

  @Resource private InstanceIdProvider instanceIdProvider;

  @Resource private StringRedisTemplate stringRedisTemplate;

  // Database-backed MQTT network components should recover after a restart by default.
  // Deployments that do not use third-party MQTT can still disable this explicitly.
  @Value("${mqtt.cfg.third.enabled:true}")
  private boolean enabled;

  @Value("${mqtt.cfg.third.maxInflight:500}")
  private int maxInflight;

  /** Lease used when clusterEnabled=false so only one application node connects. */
  @Value("${mqtt.cfg.third.single.ownerLeaseSeconds:90}")
  private long singleOwnerLeaseSeconds;

  /** Extra time kept on the owner lease while an asynchronous broker connection is in progress. */
  @Value("${mqtt.cfg.third.single.startupLeaseMarginSeconds:5}")
  private long singleOwnerStartupLeaseMarginSeconds;

  // === 客户端管理 ===
  private final Map<String, MqttAsyncClient> networkClients = new ConcurrentHashMap<>();
  /** Clients whose asynchronous connect request has not completed yet. */
  private final Map<String, MqttAsyncClient> connectingClients = new ConcurrentHashMap<>();
  private final Map<String, MQTTProductConfig> networkConfigs = new ConcurrentHashMap<>();

  // === 连接状态管理 ===
  private final Map<String, Boolean> connectionStatus = new ConcurrentHashMap<>();
  private final Map<String, Long> lastConnectTime = new ConcurrentHashMap<>();
  /** Prevent startup/reconnect races from creating more than one client per network. */
  private final Map<String, Object> lifecycleLocks = new ConcurrentHashMap<>();
  private final java.util.Set<String> startingClients = ConcurrentHashMap.newKeySet();
  private final java.util.Set<String> stoppingClients = ConcurrentHashMap.newKeySet();
  /** Invalidates callbacks that arrive after a stop or a newer start. */
  private final Map<String, Long> lifecycleGenerations = new ConcurrentHashMap<>();

  // === 线程池 ===
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
  private final ExecutorService startupExecutor =
      Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("MQTT-Startup-", 0).factory());

  // === 重连管理 ===
  private final Map<String, Integer> reconnectRetryMap = new ConcurrentHashMap<>();
  private final Map<String, java.util.concurrent.ScheduledFuture<?>> reconnectFutures =
      new ConcurrentHashMap<>();
  private final Map<String, String> ownershipTokens = new ConcurrentHashMap<>();
  private final Map<String, java.util.concurrent.ScheduledFuture<?>> ownershipRenewFutures =
      new ConcurrentHashMap<>();
  private final int maxReconnectRetry = 15; // 最大重试次数
  private final long maxReconnectDelayMillis = 30000; // 最大重试间隔30秒

  private volatile boolean started = false;

  private static final DefaultRedisScript<Long> RENEW_OWNER_SCRIPT =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "return redis.call('expire', KEYS[1], ARGV[2]) "
              + "else return 0 end",
          Long.class);

  private static final DefaultRedisScript<Long> RELEASE_OWNER_SCRIPT =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "return redis.call('del', KEYS[1]) "
              + "else return 0 end",
          Long.class);

  public void initialize() {
    if (!enabled) {
      log.info("[THIRD_MQTT] MQTT模块已禁用，跳过初始化");
      return;
    }

    log.info("[THIRD_MQTT] 开始初始化MQTT服务器...");

    // 延迟执行，确保配置注入完成
    scheduler.schedule(
        () -> {
          try {
            //        // 1. 初始化系统MQTT
            //        sysMQTTManager.initialize(scheduler);

            // 2. 加载并启动产品客户端
            loadAndStartAllClients();

            // 3. 启动健康检查
            startHealthCheck();

            started = true;
            log.info("[THIRD_MQTT] MQTT服务器初始化完成");

          } catch (Exception e) {
            log.error("[THIRD_MQTT] MQTT服务器初始化失败: ", e);
          }
        },
        2,
        TimeUnit.SECONDS);
  }

  /** MQTT回调处理器 */
  private class MqttCallbackHandler implements MqttCallback {

    private final String networkUnionId;

    private final MQTTProductConfig mqttProductConfig;

    private final MqttAsyncClient client;

    public MqttCallbackHandler(
        String networkUnionId, MQTTProductConfig mqttProductConfig, MqttAsyncClient client) {
      this.networkUnionId = networkUnionId;
      this.mqttProductConfig = mqttProductConfig;
      this.client = client;
    }

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
      if (stoppingClients.contains(networkUnionId)
          || networkClients.get(networkUnionId) != client) {
        return;
      }
      log.warn(
          "[THIRD_MQTT] 产品连接丢失: {}, 原因: {}",
          networkUnionId,
          mqttDisconnectResponse.getReasonString());
      connectionStatus.put(networkUnionId, false);
      reconnectClient(networkUnionId, networkConfigs.get(networkUnionId));
    }

    @Override
    public void mqttErrorOccurred(MqttException e) {
      log.error("[MQTT] 连接异常", e.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
      try {
        byte[] payloadRaw = message.getPayload();
        // 接受当前连接（包括订阅后立即收到的保留消息），拒绝停止/替换后的旧连接回调。
        if (networkConfigs.get(networkUnionId) != mqttProductConfig
            || (networkClients.get(networkUnionId) != client
                && connectingClients.get(networkUnionId) != client)) {
          return;
        }
        // 产品与分类来自同一份连接配置、同一条匹配规则，随请求进入异步处理链。
        MQTTTopicManager.TopicRoute route =
            mqttTopicManager.resolveRoute(
                topic,
                mqttProductConfig.getSubscribeTopics(),
                key -> configService.supportMQTTNetwork(key, networkUnionId));
        if (route == null || StrUtil.isBlank(route.productKey())) {
          log.warn(
              "[THIRD_MQTT] 无法识别产品，请检查订阅规则和关联产品: networkUnionId={}, topic={}, pattern={}",
              networkUnionId,
              topic,
              route == null ? null : route.topicPattern());
          return;
        }
        String productKey = route.productKey();
        MqttConstant.TopicCategory topicCategory = route.topicCategory();
        // 根据产品配置获取解码类型并解码 payload
        String decoderType = iotProductDeviceService.getProductDecoderType(productKey);
        String payload = PayloadCodecUtils.decode(decoderType, payloadRaw);

        log.info(
            "[THIRD_MQTT] 收到消息: network标识={},产品={}, 主题={}, decoderType={}, 消息={}",
            networkUnionId,
            productKey,
            topic,
            decoderType,
            payload);
        boolean isSupport = configService.supportMQTTNetwork(productKey, networkUnionId);
        if (!isSupport) {
          log.warn(
              "[THIRD_MQTT] 消息不支持，产品={},networkUnionId={},topic={},payload={} }",
              productKey,
              networkUnionId,
              topic,
              payload);
          return;
        }
        // 构建UP请求并处理
        MQTTUPRequest request = buildMqttUPRequest(topic, payload, payloadRaw, productKey);
        if (topicCategory != null) {
          request.setTopicCategory(topicCategory);
          request.setContextValue("topicCategory", topicCategory);
        }
        // 通过处理链处理
        executorService.submit(() -> processorChain.process(request));
        metricsCollector.incrementActiveClientCount();

      } catch (Exception e) {
        log.error("[THIRD_MQTT] 消息处理异常: 产品={}, 主题={}, 异常=", networkUnionId, topic, e);
        metricsCollector.incrementErrorCount();
      }
    }

    @Override
    public void deliveryComplete(IMqttToken iMqttToken) {
      log.debug(
          "[THIRD_MQTT] 消息发送完成: 产品={}, topics={} , 消息ID={}",
          networkUnionId,
          iMqttToken.getTopics(),
          iMqttToken.getMessageId());
    }

    @Override
    public void connectComplete(boolean b, String s) {}

    @Override
    public void authPacketArrived(int i, MqttProperties mqttProperties) {}

    /** 构建MQTT UP请求 */
    private MQTTUPRequest buildMqttUPRequest(
        String topic, String payload, byte[] payloadRaw, String productKey) {
      return MQTTUPRequest.builder()
          .upTopic(topic)
          .productKey(productKey)
          .networkUnionId(networkUnionId)
          .messageId(IdUtil.simpleUUID())
          .payloadRaw(payloadRaw)
          .isSysMQTTBroker(false)
          .payload(payload)
          .build();
    }
  }

  /** 为未携带分类的内部请求按网络与实际 Topic 解析，不按产品推断分类。 */
  public MqttConstant.TopicCategory resolveTopicCategory(String networkUnionId, String topic) {
    MQTTProductConfig config =
        StrUtil.isBlank(networkUnionId) ? null : networkConfigs.get(networkUnionId);
    return config == null
        ? MqttConstant.TopicCategory.UNKNOWN
        : mqttTopicManager.getTopicCategoryFromConfig(topic, config.getSubscribeTopics());
  }

  /** 加载并启动所有产品客户端 */
  public void loadAndStartAllClients() {
    try {
      // 从配置服务加载所有MQTT配置
      Map<String, MQTTProductConfig> allConfigs = configService.loadAllConfigs();
      networkConfigs.putAll(allConfigs);

      // 启动所有产品客户端
      for (Map.Entry<String, MQTTProductConfig> entry : networkConfigs.entrySet()) {
        String unionId = entry.getKey();
        MQTTProductConfig config = entry.getValue();

        try {
          // 启动产品客户端
          initializeClient(unionId, config);

        } catch (Exception e) {
          log.error("[THIRD_MQTT] 启动产品客户端失败: unionId={}, error={}", unionId, e.getMessage(), e);
        }
      }

      log.info("[THIRD_MQTT] 产品客户端启动完成，数量: {}", networkClients.size());

    } catch (Exception e) {
      log.error("[THIRD_MQTT] 加载和启动客户端失败: ", e);
    }
  }

  /** 构建客户端ID */
  private String buildClientId(String networkUnionId) {
    String instanceId = instanceIdProvider.getInstanceId();
    return String.format("%s-%s", networkUnionId, instanceId);
  }

  /** 解析实际连接Broker使用的客户端ID */
  private String resolveClientId(String networkUnionId, MQTTProductConfig config) {
    String clientId = config.getClientId();
    if (StrUtil.isBlank(clientId)) {
      return buildClientId(networkUnionId);
    }
    if (!config.isClusterEnabled()) {
      return clientId;
    }
    String instanceId = instanceIdProvider.getInstanceId();
    String nodeSuffix = Long.toHexString(Integer.toUnsignedLong(instanceId.hashCode()));
    return String.format("%s-%s", clientId, nodeSuffix);
  }

  /** 构建连接选项 */
  private MqttConnectionOptions buildConnectOptions(MQTTProductConfig config) {
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setReceiveMaximum(maxInflight);
    options.setCleanStart(config.isCleanSession());
    options.setKeepAliveInterval(config.getKeepAliveInterval());
    options.setConnectionTimeout(config.getConnectTimeout());

    if (config.getUsername() != null && !config.getUsername().isEmpty()) {
      options.setUserName(config.getUsername());
    }
    if (config.getPassword() != null && !config.getPassword().isEmpty()) {
      options.setPassword(config.getPassword().getBytes());
    }

    options.setAutomaticReconnect(config.isAutoReconnect());
    mqttTlsSupport.applyTls(options, config, config.getNetworkUnionId());

    return options;
  }

  /** 初始化单个客户端 */
  private boolean initializeClient(String unionId, MQTTProductConfig config) {
    if (StrUtil.isBlank(unionId) || config == null) {
      return false;
    }
    Object lifecycleLock = lifecycleLocks.computeIfAbsent(unionId, key -> new Object());
    final long generation;
    synchronized (lifecycleLock) {
      if (stoppingClients.contains(unionId)
          || networkConfigs.get(unionId) != config
          || isConnected(unionId)
          || !startingClients.add(unionId)) {
        log.debug("[THIRD_MQTT] 客户端已连接或正在启动，跳过重复启动: unionId={}", unionId);
        return isConnected(unionId) || startingClients.contains(unionId);
      }
      generation = lifecycleGenerations.merge(unionId, 1L, Long::sum);
    }
    MqttAsyncClient createdClient = null;
    String ownershipToken = null;
    try {
      if (!config.isClusterEnabled()) {
        ownershipToken = acquireSingleNodeOwnership(unionId, startupLeaseSeconds(config));
        if (ownershipToken == null) {
          synchronized (lifecycleLock) {
            startingClients.remove(unionId);
          }
          logOwnerLeaseBusy(unionId);
          return false;
        }
      }
      String clientId = resolveClientId(unionId, config);
      String brokerUri = mqttTlsSupport.resolveBrokerUri(config.getHost(), config);
      config.setHost(brokerUri);
      final MqttAsyncClient client =
          new MqttAsyncClient(brokerUri, clientId, new MemoryPersistence());
      createdClient = client;

      // 设置回调
      client.setCallback(new MqttCallbackHandler(unionId, config, client));
      synchronized (lifecycleLock) {
        if (stoppingClients.contains(unionId)
            || lifecycleGenerations.getOrDefault(unionId, 0L) != generation
            || networkConfigs.get(unionId) != config
            || !startingClients.contains(unionId)) {
          if (lifecycleGenerations.getOrDefault(unionId, 0L) == generation) {
            startingClients.remove(unionId);
          }
          releaseSingleNodeOwnership(unionId, ownershipToken);
          closeClientQuietly(unionId, client);
          return false;
        }
        connectingClients.put(unionId, client);
      }

      // 配置连接选项
      MqttConnectionOptions options = buildConnectOptions(config);
      final String acquiredOwnershipToken = ownershipToken;

      // 异步连接，并添加连接结果回调（关键修改）
      client.connect(
          options,
          null,
          new MqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
              if (!isCurrentAttempt(unionId, config, client, generation)) {
                releaseSingleNodeOwnership(unionId, acquiredOwnershipToken);
                closeClientQuietly(unionId, client);
                return;
              }
              // 连接成功后执行订阅
              try {
                subscribeTopics(client, config);
                if (!promoteConnectedClient(unionId, config, client, generation)) {
                  releaseSingleNodeOwnership(unionId, acquiredOwnershipToken);
                  closeClientQuietly(unionId, client);
                  return;
                }
                if (!startOwnershipRenewal(unionId, acquiredOwnershipToken)) {
                  return;
                }
                log.info(
                    "[THIRD_MQTT] MQTT客户端连接并订阅成功: {} -> {}, clientId={}, clusterEnabled={}",
                    unionId,
                    brokerUri,
                    clientId,
                    config.isClusterEnabled());
              } catch (MqttException e) {
                log.error("[THIRD_MQTT] 连接成功后订阅主题失败: unionId={}", unionId, e);
                connectionStatus.put(unionId, false);
                startingClients.remove(unionId);
                releaseSingleNodeOwnership(unionId, acquiredOwnershipToken);
                closeClientQuietly(unionId, client);
              }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
              if (!isCurrentAttempt(unionId, config, client, generation)) {
                releaseSingleNodeOwnership(unionId, acquiredOwnershipToken);
                closeClientQuietly(unionId, client);
                return;
              }
              // 连接失败处理
              log.error(
                  "[THIRD_MQTT] 客户端连接失败: unionId={}, 原因: {}",
                  unionId,
                  exception.getMessage(),
                  exception);
              connectionStatus.put(unionId, false);
              connectingClients.remove(unionId, client);
              startingClients.remove(unionId);
              closeClientQuietly(unionId, client);
              releaseSingleNodeOwnership(unionId, acquiredOwnershipToken);
              metricsCollector.incrementErrorCount();

              // 自动重连
              int retryCount = reconnectRetryMap.getOrDefault(unionId, 0) + 1;
              MQTTProductConfig config = networkConfigs.get(unionId);
              if (config != null) {
                scheduleReconnect(unionId, config, retryCount, generation);
              }
            }
          });

      return true; // 此处仅表示“发起连接成功”，实际连接结果在回调中处理

    } catch (Exception e) {
      if (createdClient != null) {
        connectingClients.remove(unionId, createdClient);
        closeClientQuietly(unionId, createdClient);
      }
      releaseSingleNodeOwnership(unionId, ownershipToken);
      log.error("[THIRD_MQTT] 初始化MQTT客户端失败: unionId={}", unionId, e);
      connectionStatus.put(unionId, false);
      startingClients.remove(unionId);
      metricsCollector.incrementErrorCount();
      return false;
    }
  }

  private boolean isCurrentAttempt(
      String unionId, MQTTProductConfig config, MqttAsyncClient client, long generation) {
    return !stoppingClients.contains(unionId)
        && lifecycleGenerations.getOrDefault(unionId, 0L) == generation
        && networkConfigs.get(unionId) == config
        && connectingClients.get(unionId) == client;
  }

  private boolean promoteConnectedClient(
      String unionId, MQTTProductConfig config, MqttAsyncClient client, long generation) {
    Object lifecycleLock = lifecycleLocks.computeIfAbsent(unionId, key -> new Object());
    synchronized (lifecycleLock) {
      if (!isCurrentAttempt(unionId, config, client, generation)) {
        return false;
      }
      networkClients.put(unionId, client);
      connectionStatus.put(unionId, true);
      lastConnectTime.put(unionId, System.currentTimeMillis());
      connectingClients.remove(unionId, client);
      startingClients.remove(unionId);
      metricsCollector.incrementActiveClientCount();
      reconnectRetryMap.remove(unionId);
      reconnectFutures.remove(unionId);
      return true;
    }
  }

  private void closeClientQuietly(String unionId, MqttAsyncClient client) {
    connectingClients.remove(unionId, client);
    try {
      if (client != null && client.isConnected()) {
        client.disconnect();
      }
      if (client != null) {
        client.close();
      }
    } catch (Exception e) {
      log.debug("[THIRD_MQTT] 关闭过期客户端失败: unionId={}", unionId, e);
    }
  }

  private String ownerKey(String unionId) {
    return "mqtt:network:owner:" + unionId;
  }

  /**
   * Calculates the short lease used only while Paho is establishing a connection.
   *
   * <p>The connected client is promoted to the normal, longer lease in
   * {@link #startOwnershipRenewal(String, String)}. Keeping this lease tied to the configured
   * broker timeout prevents an unreachable node from reserving a network for the full connected
   * owner lease.
   */
  private long startupLeaseSeconds(MQTTProductConfig config) {
    long connectTimeoutSeconds = config == null ? 30L : config.getConnectTimeout();
    return Math.max(
        10L,
        Math.max(1L, connectTimeoutSeconds)
            + Math.max(0L, singleOwnerStartupLeaseMarginSeconds));
  }

  /** Acquire a Redis lease for single-node MQTT mode. */
  private String acquireSingleNodeOwnership(String unionId, long requestedLeaseSeconds) {
    if (StrUtil.isBlank(unionId) || stringRedisTemplate == null || instanceIdProvider == null) {
      return null;
    }
    long leaseSeconds = Math.max(requestedLeaseSeconds, 1L);
    String token = instanceIdProvider.getInstanceId() + ":" + IdUtil.fastSimpleUUID();
    try {
      Boolean acquired =
          stringRedisTemplate
              .opsForValue()
              .setIfAbsent(ownerKey(unionId), token, leaseSeconds, TimeUnit.SECONDS);
      if (Boolean.TRUE.equals(acquired)) {
        ownershipTokens.put(unionId, token);
        return token;
      }
    } catch (Exception e) {
      log.warn("[THIRD_MQTT] 获取单机owner租约失败: unionId={}", unionId, e);
    }
    return null;
  }

  private void logOwnerLeaseBusy(String unionId) {
    long ttlSeconds = -2L;
    try {
      ttlSeconds = stringRedisTemplate.getExpire(ownerKey(unionId), TimeUnit.SECONDS);
    } catch (Exception e) {
      log.debug("[THIRD_MQTT] 查询owner租约TTL失败: unionId={}", unionId, e);
    }
    String instanceId =
        instanceIdProvider == null ? "unknown" : instanceIdProvider.getInstanceId();
    log.warn(
        "[THIRD_MQTT] 单机模式未获得owner租约，当前节点进入待机: unionId={}, ownerKey={}, "
            + "ttlSeconds={}, instanceId={}",
        unionId,
        ownerKey(unionId),
        ttlSeconds,
        instanceId);
  }

  private boolean startOwnershipRenewal(String unionId, String token) {
    if (StrUtil.isBlank(token)) {
      return true;
    }
    if (!token.equals(ownershipTokens.get(unionId)) || !isConnected(unionId)) {
      return false;
    }
    java.util.concurrent.ScheduledFuture<?> previous = ownershipRenewFutures.remove(unionId);
    if (previous != null) {
      previous.cancel(false);
    }
    long leaseSeconds = Math.max(singleOwnerLeaseSeconds, 30L);
    try {
      Long promoted =
          stringRedisTemplate.execute(
              RENEW_OWNER_SCRIPT,
              Collections.singletonList(ownerKey(unionId)),
              token,
              String.valueOf(leaseSeconds));
      if (!Long.valueOf(1L).equals(promoted)) {
        log.error("[THIRD_MQTT] 连接成功后提升owner租约失败，停止本地客户端: unionId={}", unionId);
        stopMqttClient(unionId, true);
        return false;
      }
    } catch (Exception e) {
      log.error("[THIRD_MQTT] 连接成功后提升owner租约异常，停止本地客户端: unionId={}", unionId, e);
      stopMqttClient(unionId, true);
      return false;
    }
    java.util.concurrent.ScheduledFuture<?> future =
        scheduler.scheduleWithFixedDelay(
            () -> {
              try {
                Long renewed =
                    stringRedisTemplate.execute(
                        RENEW_OWNER_SCRIPT,
                        Collections.singletonList(ownerKey(unionId)),
                        token,
                        String.valueOf(leaseSeconds));
                if (!Long.valueOf(1L).equals(renewed)) {
                  log.error("[THIRD_MQTT] 单机owner租约已丢失，停止本地客户端: unionId={}", unionId);
                  // Keep the database-backed config so this node can compete for ownership again.
                  stopMqttClient(unionId, true);
                }
              } catch (Exception e) {
                log.warn("[THIRD_MQTT] 续期单机owner租约失败: unionId={}", unionId, e);
              }
            },
            Math.max(5L, leaseSeconds / 3L),
            Math.max(5L, leaseSeconds / 3L),
            TimeUnit.SECONDS);
    ownershipRenewFutures.put(unionId, future);
    return true;
  }

  private void releaseSingleNodeOwnership(String unionId, String token) {
    if (StrUtil.isBlank(unionId)) {
      return;
    }
    String currentToken = ownershipTokens.get(unionId);
    if (StrUtil.isNotBlank(token) && !token.equals(currentToken)) {
      // A late callback from an obsolete connection attempt must not cancel the active owner.
      return;
    }
    java.util.concurrent.ScheduledFuture<?> renewFuture = ownershipRenewFutures.remove(unionId);
    if (renewFuture != null) {
      renewFuture.cancel(false);
    }
    String tokenToRelease = StrUtil.isBlank(token) ? currentToken : token;
    if (StrUtil.isBlank(tokenToRelease)) {
      return;
    }
    try {
      stringRedisTemplate.execute(
          RELEASE_OWNER_SCRIPT,
          Collections.singletonList(ownerKey(unionId)),
          tokenToRelease);
    } catch (Exception e) {
      log.warn("[THIRD_MQTT] 释放单机owner租约失败: unionId={}", unionId, e);
    } finally {
      ownershipTokens.remove(unionId, tokenToRelease);
    }
  }

  private String resolveSubscribeTopic(String unionId, MQTTProductConfig config, String topic) {
    if (!config.isClusterEnabled() || StrUtil.isBlank(topic) || topic.startsWith("$share/")) {
      return topic;
    }
    return "$share/mqtt-network-" + unionId + "/" + topic;
  }

  /** 订阅主题（保持不变，但确保仅在连接成功后调用） */
  private void subscribeTopics(MqttAsyncClient client, MQTTProductConfig config)
      throws MqttException {
    List<MQTTProductConfig.MqttTopicConfig> topicConfigs =
        config.getSubscribeTopics().stream()
            .filter(MQTTProductConfig.MqttTopicConfig::isEnabled)
            .toList();
    if (topicConfigs.isEmpty()) {
      return;
    }

    String[] topics =
        topicConfigs.stream()
            .map(topic -> resolveSubscribeTopic(config.getNetworkUnionId(), config, topic.getTopic()))
            .toArray(String[]::new);
    int[] qosArray =
        topicConfigs.stream().mapToInt(MQTTProductConfig.MqttTopicConfig::getQos).toArray();

    // 异步订阅（可选：添加订阅结果回调，更细致地处理订阅成功/失败）
    client.subscribe(
        topics,
        qosArray,
        null,
        new MqttActionListener() {
          @Override
          public void onSuccess(IMqttToken asyncActionToken) {
            log.info("[THIRD_MQTT] 订阅主题成功，主题={} 总数: {}", topics, topics.length);
          }

          @Override
          public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            log.warn("[THIRD_MQTT] 订阅主题失败: {}", Arrays.toString(topics), exception);
          }
        });
  }

  /** 启动健康检查 */
  private void startHealthCheck() {
    scheduler.scheduleWithFixedDelay(
        () -> {
          try {
            performHealthCheck();
          } catch (Exception e) {
            log.error("[THIRD_MQTT] 健康检查异常: ", e);
          }
        },
        30,
        30,
        TimeUnit.SECONDS);
  }

  /** 执行健康检查 */
  private void performHealthCheck() {
    stopClientsWithInvalidConfig();
    // A non-owner node retries periodically so it can take over after the owner lease expires.
    for (Map.Entry<String, MQTTProductConfig> entry : networkConfigs.entrySet()) {
      MQTTProductConfig config = entry.getValue();
      if (config != null
          && !config.isClusterEnabled()
          && !isConnected(entry.getKey())
          && !startingClients.contains(entry.getKey())
          && !stoppingClients.contains(entry.getKey())) {
        initializeClient(entry.getKey(), config);
      }
    }
    for (Map.Entry<String, MqttAsyncClient> entry : networkClients.entrySet()) {
      String unionId = entry.getKey();
      MqttAsyncClient client = entry.getValue();

      try {
        boolean isConnected = client != null && client.isConnected();
        connectionStatus.put(unionId, isConnected);

        if (!isConnected) {
          log.warn("[THIRD_MQTT] 检测到连接断开: {}", unionId);

          // 尝试重连
          MQTTProductConfig config = networkConfigs.get(unionId);
          if (config != null) {
            reconnectClient(unionId, config);
          }
        }
      } catch (Exception e) {
        log.error("[THIRD_MQTT] 健康检查失败: unionId={}, error={}", unionId, e.getMessage());
        connectionStatus.put(unionId, false);
      }
    }
  }

  /** Redis集群事件丢失时，以数据库启用状态为准停止本节点的过期客户端。 */
  private void stopClientsWithInvalidConfig() {
    Map<String, MQTTProductConfig> enabledConfigs = configService.loadAllConfigs();
    for (String unionId : getManagedUnionIds()) {
      if (!enabledConfigs.containsKey(unionId)) {
        log.warn("[THIRD_MQTT] 健康检查发现配置已删除或停用，停止本地客户端: {}", unionId);
        stopMqttClient(unionId);
      }
    }
  }

  private Set<String> getManagedUnionIds() {
    Set<String> unionIds = new HashSet<>(networkConfigs.keySet());
    unionIds.addAll(networkClients.keySet());
    unionIds.addAll(connectingClients.keySet());
    unionIds.addAll(reconnectFutures.keySet());
    return unionIds;
  }

  /** 重连客户端 */
  private void reconnectClient(String unionId, MQTTProductConfig config) {
    if (StrUtil.isBlank(unionId) || config == null) {
      log.debug("[THIRD_MQTT] 重连跳过，配置不存在: unionId={}", unionId);
      return;
    }
    Object lifecycleLock = lifecycleLocks.computeIfAbsent(unionId, key -> new Object());
    synchronized (lifecycleLock) {
      try {
        log.info("[THIRD_MQTT] 尝试重连客户端: {}", unionId);

        // 先关闭旧连接
        stopMqttClient(unionId);

        // stopMqttClient 会清理运行时缓存，重连前恢复配置，保证失败回调仍能按上限重试。
        networkConfigs.put(unionId, config);

        // 等待片刻
        Thread.sleep(1000);

        // 重新初始化
        if (initializeClient(unionId, config)) {
          log.info("[THIRD_MQTT] 客户端重连成功: {}", unionId);
        }

      } catch (Exception e) {
        log.error("[THIRD_MQTT] 客户端重连失败: unionId={}, error={}", unionId, e.getMessage());
      }
    }
  }

  /** 封装自动重连调度方法 */
  private void scheduleReconnect(
      String unionId, MQTTProductConfig config, int retryCount, long expectedGeneration) {
    Object lifecycleLock = lifecycleLocks.computeIfAbsent(unionId, key -> new Object());
    synchronized (lifecycleLock) {
      if (stoppingClients.contains(unionId)
          || lifecycleGenerations.getOrDefault(unionId, 0L) != expectedGeneration
          || networkConfigs.get(unionId) != config) {
        return;
      }
      if (retryCount > maxReconnectRetry) {
        log.error("[THIRD_MQTT] unionId={} 超过最大重连次数({})，不再重连", unionId, maxReconnectRetry);
        reconnectRetryMap.remove(unionId);
        reconnectFutures.remove(unionId);
        return;
      }

      // 取消之前的重连任务（如果存在）
      cancelReconnectTasks(unionId);

      long delay =
          Math.min(
              (1L << (retryCount - 1)) * 2000,
              maxReconnectDelayMillis); // 2s, 4s, 8s, 16s, 30s
      log.warn("[THIRD_MQTT] unionId={} 第{}次重连，{}ms后重试", unionId, retryCount, delay);

      reconnectRetryMap.put(unionId, retryCount);

      // 保存future以便后续取消
      java.util.concurrent.ScheduledFuture<?> future =
          scheduler.schedule(
              () -> {
                try {
                  if (stoppingClients.contains(unionId)
                      || lifecycleGenerations.getOrDefault(unionId, 0L) != expectedGeneration) {
                    return;
                  }
                  // 每次重连前都从数据库刷新，确保删除或停用后不会继续使用旧缓存。
                  Map<String, MQTTProductConfig> allConfigs = configService.loadAllConfigs();
                  MQTTProductConfig latestConfig = allConfigs.get(unionId);
                  if (latestConfig == null) {
                    log.warn("[THIRD_MQTT] 重连时配置已删除或停用，停止重连: {}", unionId);
                    stopMqttClient(unionId);
                    return;
                  }
                  synchronized (lifecycleLock) {
                    if (stoppingClients.contains(unionId)
                        || lifecycleGenerations.getOrDefault(unionId, 0L)
                            != expectedGeneration) {
                      return;
                    }
                    networkConfigs.put(unionId, latestConfig);
                  }

                  boolean success = initializeClient(unionId, latestConfig);
                  if (!success) {
                    // 递归调度下一次重连
                    scheduleReconnect(
                        unionId,
                        latestConfig,
                        retryCount + 1,
                        lifecycleGenerations.getOrDefault(unionId, 0L));
                  } else {
                    // initializeClient() 只表示异步连接请求已发起，状态必须等 onSuccess() 清理。
                    reconnectFutures.remove(unionId);
                  }
                } catch (Exception e) {
                  log.error("[THIRD_MQTT] unionId={} 重连异常", unionId, e);
                  // 检查是否应该继续重连
                  if (networkConfigs.containsKey(unionId)) {
                    scheduleReconnect(unionId, config, retryCount + 1, expectedGeneration);
                  } else {
                    log.warn("[THIRD_MQTT] unionId={} 配置已移除，停止重连", unionId);
                    reconnectRetryMap.remove(unionId);
                    reconnectFutures.remove(unionId);
                  }
                }
              },
              delay,
              TimeUnit.MILLISECONDS);

      reconnectFutures.put(unionId, future);
    }
  }

  // ==================== 管理API ====================

  /** 启动MQTT客户端 */
  public boolean startMqttClient(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return false;
    }
    Object lifecycleLock = lifecycleLocks.computeIfAbsent(unionId, key -> new Object());
    synchronized (lifecycleLock) {
      if (isConnected(unionId) || startingClients.contains(unionId)) {
        log.info("[THIRD_MQTT] 客户端已运行或正在启动，跳过重复启动: {}", unionId);
        return true;
      }
    try {
      log.info("[THIRD_MQTT] 开始启动客户端: {}", unionId);

      // 1. 先完全停止现有客户端（包括重连任务），确保清理干净
      stopMqttClient(unionId);

      // 2. 等待片刻，确保资源释放
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[THIRD_MQTT] 等待中断: {}", unionId);
      }

      // 3. 从数据库重新加载最新配置（不使用缓存）
      log.info("[THIRD_MQTT] 从数据库重新加载配置: {}", unionId);
      Map<String, MQTTProductConfig> allConfigs = configService.loadAllConfigs();

      // 4. 获取指定unionId的配置
      MQTTProductConfig config = allConfigs.get(unionId);
      if (config == null) {
        log.warn("[THIRD_MQTT] 数据库中未找到产品配置: {}", unionId);
        return false;
      }

      // 5. 更新缓存配置（使用最新配置）
      networkConfigs.put(unionId, config);

      log.info("[THIRD_MQTT] 配置已重新加载: unionId={}, host={}", unionId, config.getHost());

        // 6. 初始化并启动客户端
        boolean success = initializeClient(unionId, config);
      if (success) {
        log.info("[THIRD_MQTT] 客户端启动成功: {}", unionId);
      } else {
        log.error("[THIRD_MQTT] 客户端启动失败: {}", unionId);
      }
      return success;

    } catch (Exception e) {
      log.error("[THIRD_MQTT] 启动客户端失败: unionId={}, error={}", unionId, e.getMessage(), e);
      // 清理状态
      networkClients.remove(unionId);
      connectionStatus.put(unionId, false);
      startingClients.remove(unionId);
      return false;
    }
    }
  }

  /** 停止MQTT客户端 */
  public boolean stopMqttClient(String unionId) {
    return stopMqttClient(unionId, false);
  }

  /**
   * Stops a client and optionally keeps its cached configuration for lease takeover.
   *
   * <p>Manual/API stops remove the config as before. Lease-loss stops keep it so the health check
   * can retry ownership acquisition on this node.
   */
  private boolean stopMqttClient(String unionId, boolean retainConfig) {
    if (StrUtil.isBlank(unionId)) {
      return false;
    }
    Object lifecycleLock = lifecycleLocks.computeIfAbsent(unionId, key -> new Object());
    synchronized (lifecycleLock) {
      stoppingClients.add(unionId);
      lifecycleGenerations.merge(unionId, 1L, Long::sum);
      if (!retainConfig) {
        networkConfigs.remove(unionId);
      }
      startingClients.remove(unionId);
    }
    try {
      log.info("[THIRD_MQTT] 开始停止客户端: {}", unionId);

      // 1. 取消所有重连任务
      cancelReconnectTasks(unionId);
      releaseSingleNodeOwnership(unionId, null);

      // 2. 停止客户端连接
      MqttAsyncClient client = networkClients.get(unionId);
      if (client != null) {
        closeClientQuietly(unionId, client);
        networkClients.remove(unionId);
        metricsCollector.decrementActiveClientCount();
      }
      MqttAsyncClient connectingClient = connectingClients.remove(unionId);
      if (connectingClient != null && connectingClient != client) {
        closeClientQuietly(unionId, connectingClient);
      }

      // 3. 清理所有状态
      connectionStatus.put(unionId, false);
      reconnectRetryMap.remove(unionId);
      reconnectFutures.remove(unionId);
      lastConnectTime.remove(unionId);

      log.info("[THIRD_MQTT] 客户端已完全停止: {}", unionId);
      return true;

    } catch (Exception e) {
      log.error("[THIRD_MQTT] 停止客户端失败: unionId={}, error={}", unionId, e.getMessage(), e);
      // 即使出错也清理状态
      networkClients.remove(unionId);
      MqttAsyncClient connectingClient = connectingClients.remove(unionId);
      closeClientQuietly(unionId, connectingClient);
      connectionStatus.put(unionId, false);
      networkConfigs.remove(unionId);
      startingClients.remove(unionId);
      reconnectRetryMap.remove(unionId);
      reconnectFutures.remove(unionId);

      return false;
    } finally {
      stoppingClients.remove(unionId);
    }
  }

  /** 取消指定unionId的所有重连任务 */
  private void cancelReconnectTasks(String unionId) {
    java.util.concurrent.ScheduledFuture<?> future = reconnectFutures.remove(unionId);
    if (future != null && !future.isDone()) {
      boolean cancelled = future.cancel(false);
      if (cancelled) {
        log.info("[THIRD_MQTT] 已取消重连任务: {}", unionId);
      }
    }
    reconnectRetryMap.remove(unionId);
  }

  /** 重启MQTT客户端 */
  public boolean restartMqttClient(String unionId) {
    log.info("[THIRD_MQTT] 重启客户端: {}", unionId);

    // 先停止客户端
    stopMqttClient(unionId);

    try {
      Thread.sleep(1000); // 等待片刻
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // 重新启动客户端（会自动从数据库加载配置）
    return startMqttClient(unionId);
  }

  /** 发布消息 */
  public boolean publishMessage(String unionId, String topic, String payload) {
    if (payload == null) {
      log.warn("[THIRD_MQTT] 消息载荷为空，无法发布: unionId={}, topic={}", unionId, topic);
      return false;
    }
    return publishMessage(unionId, topic, payload.getBytes(StandardCharsets.UTF_8));
  }

  /** 发布原始字节消息 */
  public boolean publishMessage(String unionId, String topic, byte[] payload) {
    try {
      if (StrUtil.isBlank(unionId)) {
        return false;
      }
      if (payload == null) {
        log.warn("[THIRD_MQTT] 消息载荷为空，无法发布: unionId={}, topic={}", unionId, topic);
        return false;
      }
      // 使用产品客户端发布
      MqttAsyncClient client = networkClients.get(unionId);
      MQTTProductConfig mqttProductConfig = networkConfigs.get(unionId);
      if (client == null || !client.isConnected() || mqttProductConfig == null) {
        log.debug("[THIRD_MQTT] 客户端未连接，无法发布消息: {}", unionId);
        return false;
      }

      MqttMessage mqttMessage = new MqttMessage();
      mqttMessage.setPayload(payload);
      mqttMessage.setQos(mqttProductConfig.getDefaultQos());
      mqttMessage.setRetained(false);
      // 自定义 User Properties
      client.publish(
          topic,
          mqttMessage,
          new UserProperty("networkId",unionId),
          new MqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
              log.info(
                  "[THIRD_MQTT] 消息发布成功 - unionId={}, 主题={},qos={},retained={}",
                  unionId,
                  topic,
                  mqttProductConfig.getDefaultQos(),
                  false);
              metricsCollector.incrementPublishMessageCount();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
              log.error("[THIRD_MQTT] 消息发布失败 - 主题: {}", topic, exception);
              metricsCollector.incrementErrorCount();
            }
          });
      metricsCollector.incrementPublishMessageCount();
      return true;

    } catch (Exception e) {
      log.error("[THIRD_MQTT] 消息发布失败 - 主题: {}, 异常: ", topic, e);
      metricsCollector.incrementErrorCount();
      return false;
    }
  }

  /** 发布消息 */
  public boolean publishMessage(String unionId, MQTTPublishMessage message) {
    try {
      if (StrUtil.isBlank(unionId)) {
        log.warn("publishMessage unionId is empty");
        return false;
      }
      // 使用产品客户端发布
      MqttAsyncClient client = networkClients.get(unionId);
      if (client == null || !client.isConnected()) {
        log.debug("[THIRD_MQTT] 客户端未连接，无法发布消息: {}", unionId);
        return false;
      }

      MqttMessage mqttMessage = new MqttMessage();
      mqttMessage.setPayload(message.getPayloadAsBytes());
      mqttMessage.setQos(message.getQos());
      mqttMessage.setRetained(message.isRetained());

      client.publish(
          message.getTopic(),
          mqttMessage,
          null,
          new MqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
              log.info(
                  "[THIRD_MQTT] 消息发布成功 - unionId={},主题={},qos={},retained={}",
                  unionId,
                  message.getTopic(),
                  mqttMessage.getQos(),
                  false);
              metricsCollector.incrementPublishMessageCount();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
              log.error("[THIRD_MQTT] 消息发布失败 - 主题: {}", message.getTopic(), exception);
              metricsCollector.incrementErrorCount();
            }
          });
      metricsCollector.incrementPublishMessageCount();
      return true;

    } catch (Exception e) {
      log.error("[THIRD_MQTT] 消息发布失败: unionId={}, topic={}", unionId, message.getTopic(), e);
      metricsCollector.incrementErrorCount();
      return false;
    }
  }

  /** 获取连接状态 */
  public boolean isConnected(String unionId) {
    MqttAsyncClient client = networkClients.get(unionId);
    return client != null && client.isConnected();
  }

  /** 获取所有客户端状态 */
  public Map<String, String> getAllClientStatus() {
    Map<String, String> status = new HashMap<>();

    // 产品客户端状态
    for (String unionId : networkConfigs.keySet()) {
      status.put(unionId, isConnected(unionId) ? "CONNECTED" : "DISCONNECTED");
    }

    // 系统MQTT状态
    if (sysMQTTManager.isEnabled()) {
      status.put("SYSTEM_MQTT_BROKER", sysMQTTManager.isConnected() ? "CONNECTED" : "DISCONNECTED");
    }

    return status;
  }

  /** 重新加载配置 */
  public void reloadAllConfigs() {
    log.info("[THIRD_MQTT] 重新加载所有配置...");

    try {
      // 停止所有客户端
      for (String unionId : getManagedUnionIds()) {
        stopMqttClient(unionId);
      }

      // 清空配置和缓存
      networkConfigs.clear();

      // 重新加载和启动
      loadAndStartAllClients();

      log.info("[THIRD_MQTT] 配置重新加载完成");

    } catch (Exception e) {
      log.error("[THIRD_MQTT] 重新加载配置失败: ", e);
    }
  }

  /** 获取统计信息 */
  public String getStatistics() {
    StringBuilder stats = new StringBuilder();
    stats.append("MQTT服务器状态:\n");
    stats.append("服务状态: ").append(started ? "已启动" : "未启动").append("\n");
    stats.append("产品客户端数: ").append(networkClients.size()).append("\n");
    stats.append("活跃连接数: ").append(getActiveConnectionCount()).append("\n");
    stats
        .append("系统MQTT: ")
        .append(sysMQTTManager.isEnabled() ? (sysMQTTManager.isConnected() ? "已连接" : "已断开") : "已禁用")
        .append("\n");

    return stats.toString();
  }

  /** 获取活跃连接数 */
  public int getActiveConnectionCount() {
    int count = 0;
    for (String unionId : networkConfigs.keySet()) {
      if (isConnected(unionId)) {
        count++;
      }
    }
    if (sysMQTTManager.isConnected()) {
      count++;
    }
    return count;
  }

  // ==================== 生命周期管理 ====================

  @PreDestroy
  public void destroy() {
    log.info("[THIRD_MQTT] 开始关闭MQTT服务器...");

    try {
      // 关闭所有产品客户端
      for (String unionId : getManagedUnionIds()) {
        stopMqttClient(unionId);
      }

      // 关闭系统MQTT
      sysMQTTManager.shutdown();

      // 关闭线程池
      scheduler.shutdown();
      startupExecutor.shutdown();

      started = false;
      log.info("[THIRD_MQTT] MQTT服务器已关闭");

    } catch (Exception e) {
      log.error("[THIRD_MQTT] 关闭MQTT服务器异常: ", e);
    }
  }
}
