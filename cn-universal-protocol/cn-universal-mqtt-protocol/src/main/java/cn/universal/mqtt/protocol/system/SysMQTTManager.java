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

package cn.universal.mqtt.protocol.system;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.utils.PayloadCodecUtils;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.push.MQTTPushService;
import cn.universal.mqtt.protocol.entity.MQTTProductConfig;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.metrics.MqttMetricsMananer;
import cn.universal.mqtt.protocol.processor.MqttUPProcessorChain;
import cn.universal.mqtt.protocol.ssl.MqttTlsSupport;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
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
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Slf4j(topic = "mqtt")
@Service("sysMQTTManager")
public class SysMQTTManager
    implements SysMQTTStatusProvider, ApplicationListener<ApplicationReadyEvent>, MQTTPushService {

  private static final long PUBLISH_FAILURE_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);

  private final AtomicLong nextPublishFailureWarningNanos = new AtomicLong();

  @Value("${mqtt.cfg.maxInflight:500}")
  private int maxInflight;

  @Autowired private cn.universal.common.util.WebInterfaceReadyChecker webInterfaceReadyChecker;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    // 使用Web接口就绪检查器，等待所有Controller和Webhook接口完全就绪
    // 这对于EMQX webhook回调特别重要
    webInterfaceReadyChecker.executeAfterWebInterfaceReady(
        () -> {
          try {
            log.info("开始延迟初始化SysMQTTManager...");
            initialize();
            log.info("SysMQTTManager初始化完成");
          } catch (Exception e) {
            log.error("SysMQTTManager初始化失败", e);
          }
        },
        scheduledExecutor);
  }

  @Autowired private MqttUPProcessorChain processorChain;

  @Autowired private MqttMetricsMananer metricsCollector;

  @Autowired private MQTTTopicManager mqttTopicManager;
  @Autowired private MqttTlsSupport mqttTlsSupport;

  @Autowired private IoTProductDeviceService iotProductDeviceService;

  @Autowired
  @Qualifier("virtualScheduledExecutor")
  private ScheduledExecutorService scheduledExecutor;

  // === 系统MQTT配置 ===
  @Value("${mqtt.cfg.enable:true}")
  private boolean enabled;

  @Value("${mqtt.cfg.host:tcp://localhost:1883}")
  private String host;

  @Value("${mqtt.cfg.client.username:}")
  private String username;

  @Value("${mqtt.cfg.client.password:}")
  private String password;

  @Value("${mqtt.cfg.clientIdPrefix:univ_iot}")
  private String clientIdPrefix;

  @Value("${mqtt.cfg.keepAliveInterval:60}")
  private int keepAliveInterval;

  @Value("${mqtt.cfg.cleanSession:true}")
  private boolean cleanSession;

  @Value("${mqtt.cfg.autoReconnect:true}")
  private boolean autoReconnect;

  @Value("${mqtt.cfg.defaultQos:1}")
  private int defaultQos;

  @Value("${mqtt.cfg.ssl:false}")
  private boolean ssl;

  @Value("${mqtt.cfg.ssl.mode:}")
  private String sslMode;

  @Value("${mqtt.cfg.ssl.ca-cert-key:}")
  private String sslCaCertKey;

  @Value("${mqtt.cfg.ssl.client-cert-key:}")
  private String sslClientCertKey;

  @Value("${mqtt.cfg.ssl.hostname-verify:true}")
  private boolean sslHostnameVerify;

  @Value("${mqtt.cfg.ssl.ca-path:}")
  private String sslCaPath;

  @Value("${mqtt.cfg.ssl.cert-path:}")
  private String sslCertPath;

  @Value("${mqtt.cfg.ssl.key-path:}")
  private String sslKeyPath;

  // === 运行时状态 ===
  private MqttAsyncClient systemMqttClient;
  private MQTTProductConfig systemMqttConfig;
  private volatile boolean initialized = false;

  /** 系统MQTT回调处理器 */
  private class SystemMqttCallbackHandler implements MqttCallback {

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
      log.warn("[MQTT] 连接丢失: {}", mqttDisconnectResponse.getReasonString());
      reconnect();
    }

    @Override
    public void mqttErrorOccurred(MqttException e) {
      log.error("[MQTT] 连接异常", e.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
      try {
        MDC.put(IoTConstant.TRACE_ID, IdUtil.objectId());
        String senderClientId = null;
        // 获取 MQTTv5 消息属性
        MqttProperties properties = message.getProperties();
        // 解析产品Key
        String productKey = mqttTopicManager.extractProductKeyFromTopic(topic);
        if (productKey != null) {
          // 根据产品配置获取解码类型并解码 payload
          String decoderType = iotProductDeviceService.getProductDecoderType(productKey);
          String payload = PayloadCodecUtils.decode(decoderType, message.getPayload());

          log.info("[MQTT] 收到消息 - 主题: {}, decoderType: {}, 消息: {}", topic, decoderType, payload);
          // 构建UP请求并处理
          MQTTUPRequest request = buildMqttUPRequest(topic, payload, productKey);
          processorChain.process(request);
          metricsCollector.incrementActiveClientCount();
        } else {
          log.warn("[MQTT] 无法从主题中提取产品Key: {}", topic);
        }

      } catch (Exception e) {
        log.error("[MQTT] 消息处理异常 - 主题: {}, 异常: ", topic, e);
        metricsCollector.incrementErrorCount();
      } finally {
        MDC.clear();
      }
    }

    @Override
    public void deliveryComplete(IMqttToken iMqttToken) {}

    @Override
    public void connectComplete(boolean b, String s) {}

    @Override
    public void authPacketArrived(int i, MqttProperties mqttProperties) {}
  }

  /** 初始化系统MQTT */
  public void initialize() {
    // 验证配置是否正确注入
    if (host == null || host.isEmpty()) {
      log.warn("[MQTT] 系统MQTT配置未正确注入，使用默认配置");
    }
    log.info("[MQTT] 系统MQTT配置 - enabled: {}, host: {}, username: {}", enabled, host, username);
    if (!enabled) {
      log.info("[MQTT] 系统MQTT已禁用");
      return;
    }

    try {
      // 构建配置
      buildSystemMqttConfig();

      // 启动客户端（异步连接，通过回调处理后续操作）
      initializeSystemMqttClient();

      // 设置重连调度
      setupReconnectScheduler(scheduledExecutor);

      log.info("[MQTT] 系统MQTT初始化流程启动完成（实际连接状态待回调确认）");

    } catch (Exception e) {
      log.error("[MQTT] 系统MQTT初始化失败: ", e);
    }
  }

  /** 构建系统MQTT配置 */
  private void buildSystemMqttConfig() {
    systemMqttConfig =
        MQTTProductConfig.builder()
            .networkUnionId("SYSTEM_MQTT_BROKER")
            .networkName("系统MQTT Broker")
            .networkType("SYSTEM_MQTT")
            .host(host)
            .username(username.isEmpty() ? null : username)
            .password(password.isEmpty() ? null : password)
            .keepAliveInterval(keepAliveInterval)
            .cleanSession(cleanSession)
            .autoReconnect(autoReconnect)
            .defaultQos(defaultQos)
            .ssl(ssl)
            .sslMode(sslMode)
            .sslCaCertKey(sslCaCertKey)
            .sslClientCertKey(sslClientCertKey)
            .sslHostnameVerify(sslHostnameVerify)
            .sslCaPath(sslCaPath)
            .sslCertPath(sslCertPath)
            .sslKeyPath(sslKeyPath)
            .enabled(true)
            .subscribeTopics(parseSystemMqttTopics())
            .createdTime(LocalDateTime.now())
            .updatedTime(LocalDateTime.now())
            .build();
    log.info("[MQTT] 系统MQTT配置构建完成: {}", host);
  }

  /** 解析系统MQTT订阅主题 */
  private List<MQTTProductConfig.MqttTopicConfig> parseSystemMqttTopics() {
    return mqttTopicManager.getAllSubscriptionTopics().stream()
        .map(String::trim)
        .filter(topic -> !topic.isEmpty())
        .map(
            topic ->
                MQTTProductConfig.MqttTopicConfig.builder()
                    .topic(topic)
                    .qos(defaultQos)
                    .enabled(true)
                    .build())
        .collect(Collectors.toList());
  }

  /** 初始化系统MQTT客户端（异步连接，带回调） */
  private void initializeSystemMqttClient() throws MqttException {
    String clientId = generateSystemClientId();
    String brokerUri = mqttTlsSupport.resolveBrokerUri(host, systemMqttConfig);
    systemMqttConfig.setHost(brokerUri);
    systemMqttClient = new MqttAsyncClient(brokerUri, clientId, new MemoryPersistence());

    // 设置回调
    systemMqttClient.setCallback(new SystemMqttCallbackHandler());

    // 配置连接选项
    MqttConnectionOptions options = buildConnectOptions();

    // 异步连接，并通过回调处理连接结果（关键修改）
    systemMqttClient.connect(
        options,
        null,
        new MqttActionListener() {
          @Override
          public void onSuccess(IMqttToken asyncActionToken) {
            try {
              // 连接成功后执行订阅
              subscribeSystemTopics();
              initialized = true; // 仅在连接+订阅成功后标记为初始化完成
              log.info("[MQTT] 系统MQTT客户端连接并订阅成功 - Broker: {}, ClientId: {}", brokerUri, clientId);
            } catch (MqttException e) {
              log.error("[MQTT] 连接成功后订阅主题失败", e);
              initialized = false;
            }
          }

          @Override
          public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            log.error("[MQTT] 系统MQTT客户端连接失败 - Broker: {}, ClientId: {}", brokerUri, clientId, exception);
            initialized = false;
          }
        });
  }

  /** 生成系统客户端ID */
  private String generateSystemClientId() {
    String instanceId = System.getProperty("server.port", "9092");
    long timestamp = System.currentTimeMillis();
    return String.format("%s-%s-%d", clientIdPrefix, instanceId, timestamp);
  }

  /** 构建连接选项 */
  private MqttConnectionOptions buildConnectOptions() {
    MqttConnectionOptions options = new MqttConnectionOptions();
    options.setReceiveMaximum(maxInflight); // 可根据实际并发需求调整
    options.setCleanStart(cleanSession);
    options.setKeepAliveInterval(keepAliveInterval);
    options.setConnectionTimeout(30); // 增加连接超时时间（秒）

    if (username != null && !username.isEmpty()) {
      options.setUserName(username);
    }
    if (password != null && !password.isEmpty()) {
      options.setPassword(password.getBytes());
    }

    options.setAutomaticReconnect(autoReconnect);
    options.setMaxReconnectDelay(5000);
    mqttTlsSupport.applyTls(options, systemMqttConfig, "SYSTEM_MQTT_BROKER");
    return options;
  }

  /** 订阅系统主题（仅在连接成功后调用） */
  private void subscribeSystemTopics() throws MqttException {
    List<MQTTProductConfig.MqttTopicConfig> topicConfigs = systemMqttConfig.getSubscribeTopics();
    if (topicConfigs.isEmpty()) {
      log.warn("[MQTT] 系统MQTT无订阅主题配置，跳过订阅");
      return;
    }

    String[] topics =
        topicConfigs.stream()
            .map(MQTTProductConfig.MqttTopicConfig::getTopic)
            .toArray(String[]::new);
    int[] qosArray =
        topicConfigs.stream().mapToInt(MQTTProductConfig.MqttTopicConfig::getQos).toArray();

    // 异步订阅，添加订阅结果回调（可选，增强可观测性）
    systemMqttClient.subscribe(
        topics,
        qosArray,
        null,
        new MqttActionListener() {
          @Override
          public void onSuccess(IMqttToken asyncActionToken) {
            log.info("[MQTT] 订阅主题成功，主题={} 总数: {}", topics, topics.length);
          }

          @Override
          public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            log.info("[MQTT] 订阅主题失败: {}", topics, exception);
          }
        });
  }

  /** 设置重连调度器 */
  private void setupReconnectScheduler(ScheduledExecutorService scheduler) {
    scheduler.scheduleWithFixedDelay(
        () -> {
          try {
            if (!initialized || (systemMqttClient != null && !systemMqttClient.isConnected())) {
              // initialized和状态不一致，有假死风险，直接断开
              systemMqttClient.disconnect();
              log.warn("[MQTT] 检测到连接断开，尝试重连...");
              reconnect();
            }
          } catch (Exception e) {
            log.error("[MQTT] 健康检查异常: ", e);
            reconnect();
          }
        },
        30,
        30,
        TimeUnit.SECONDS);
  }

  /** 重新连接（带回调的异步重连） */
  private void reconnect() {
    try {
      log.info("SysMQTT开始进入重连");
      if (systemMqttClient == null) {
        log.warn("[MQTT] 客户端未初始化，跳过重连");
        return;
      }

      if (systemMqttClient.isConnected()) {
        log.debug("[MQTT] 客户端已连接，无需重连");
        return;
      }

      MqttConnectionOptions options = buildConnectOptions();
      // 异步重连，连接成功后重新订阅
      systemMqttClient.connect(
          options,
          null,
          new MqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
              try {
                subscribeSystemTopics(); // 重连成功后重新订阅
                log.info("[MQTT] 重连成功");
                initialized = true;
              } catch (MqttException e) {
                log.error("[MQTT] 重连成功后订阅失败", e);
                initialized = false;
              }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
              log.error("[MQTT] 重连失败", exception);
              initialized = false;
            }
          });
    } catch (Exception e) {
      log.error("[MQTT] 重连操作触发异常", e);
    }
  }

  /** 发布消息（添加连接状态检查） */
  @Override
  public boolean publishMessage(String topic, byte[] payload, int qos, boolean retained) {
    try {
      if (systemMqttClient == null || !systemMqttClient.isConnected()) {
        logPublishFailure(topic, "系统MQTT未连接", null);
        return false;
      }

      MqttMessage mqttMessage = new MqttMessage();
      mqttMessage.setPayload(payload);
      mqttMessage.setQos(qos);
      mqttMessage.setRetained(retained);

      // 异步发布，添加发布结果回调（可选）
      systemMqttClient.publish(
          topic,
          mqttMessage,
          new UserProperty("networkId", "sys"),
          new MqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
              log.debug("[MQTT] 消息投递已完成 - 主题={},qos={},retained={}", topic, qos, retained);
              metricsCollector.incrementPublishMessageCount();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
              logPublishFailure(topic, "异步投递失败", exception);
              metricsCollector.incrementErrorCount();
            }
          });

      return true;

    } catch (Exception e) {
      logPublishFailure(topic, "投递调用失败", e);
      metricsCollector.incrementErrorCount();
      return false;
    }
  }

  private void logPublishFailure(String topic, String reason, Throwable exception) {
    long now = System.nanoTime();
    long next = nextPublishFailureWarningNanos.get();
    if (now >= next
        && nextPublishFailureWarningNanos.compareAndSet(
            next, now + PUBLISH_FAILURE_LOG_INTERVAL_NANOS)) {
      if (exception == null) {
        log.warn("[MQTT] 消息投递失败 - 主题={}, reason={}", topic, reason);
      } else {
        log.warn("[MQTT] 消息投递失败 - 主题={}, reason={}", topic, reason, exception);
      }
      return;
    }
    log.debug(
        "[MQTT] 消息投递失败 - 主题={}, reason={}, error={}",
        topic,
        reason,
        exception == null ? null : exception.getMessage());
  }

  /** 检查产品是否被系统MQTT覆盖 */
//  public boolean isProductCovered(String productKey) {
//    return isUseSysMQTT(productKey);
//  }

  /** 获取连接状态 */
  public boolean isConnected() {
    return systemMqttClient != null && systemMqttClient.isConnected();
  }

  /** 获取配置 */
  public MQTTProductConfig getConfig() {
    return systemMqttConfig;
  }

  /** 是否启用 */
  public boolean isEnabled() {
    return enabled;
  }

  /** 关闭系统MQTT */
  public void shutdown() {
    if (systemMqttClient != null) {
      try {
        if (systemMqttClient.isConnected()) {
          // 异步断开连接，带回调
          systemMqttClient.disconnect(
              null,
              new MqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                  try {
                    systemMqttClient.close();
                    log.info("[MQTT] 系统MQTT客户端已关闭");
                  } catch (MqttException e) {
                    log.error("[MQTT] 关闭客户端失败", e);
                  }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                  log.error("[MQTT] 断开连接失败", exception);
                }
              });
        } else {
          systemMqttClient.close();
          log.info("[MQTT] 系统MQTT客户端已关闭");
        }
      } catch (Exception e) {
        log.error("[MQTT] 关闭系统MQTT客户端异常: ", e);
      }
    }
  }

  /** 构建MQTT UP请求 */
  private MQTTUPRequest buildMqttUPRequest(String topic, String payload, String productKey) {
    MQTTUPRequest request =
        MQTTUPRequest.builder()
            .upTopic(topic)
            .productKey(productKey)
            .messageId(IdUtil.simpleUUID())
            .isSysMQTTBroker(true)
            .deviceId(mqttTopicManager.extractDeviceIdFromTopic(topic))
            .build();
    request.setPayload(payload);
    return request;
  }

  @Override
  public boolean isUseSysMQTT(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return false;
    }
    return StrUtil.isBlank(iotProductDeviceService.selectNetworkUnionId(productKey));
  }
}
