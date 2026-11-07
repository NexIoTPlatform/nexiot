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

package cn.universal.dm.device.service.push;

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.bo.UPPushBO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

/**
 * Kafka推送策略实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class KafkaPushStrategy implements PushStrategy<UPPushBO.KafkaPushConfig> {

  private static final int SEND_TIMEOUT_MS = 1200;
  private static final long FAILURE_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);
  private static final String DEFAULT_SECURITY_PROTOCOL = "PLAINTEXT";
  private static final String DEFAULT_SASL_SECURITY_PROTOCOL = "SASL_PLAINTEXT";
  private static final String DEFAULT_SASL_MECHANISM = "PLAIN";
  private static final int MAX_CACHED_PRODUCERS = 64;

  private final AtomicBoolean destroying = new AtomicBoolean();
  private final AtomicLong nextFailureWarningNanos = new AtomicLong();
  private final Cache<String, KafkaProducer<String, String>> producerCache =
      Caffeine.newBuilder()
          .maximumSize(MAX_CACHED_PRODUCERS)
          .expireAfterAccess(30, TimeUnit.MINUTES)
          .removalListener(
              (String key,
                      KafkaProducer<String, String> producer,
                      com.github.benmanes.caffeine.cache.RemovalCause cause) ->
                  closeProducerLater(producer))
          .build();

  @Override
  public IoTPushResult execute(
      BaseUPRequest request, String messageJson, UPPushBO.KafkaPushConfig config) {
    if (destroying.get()) {
      return buildFailedResult(request, messageJson, "推送组件正在关闭", "SHUTTING_DOWN");
    }
    if (config == null) {
      log.warn("[Kafka推送] 配置为空，跳过推送");
      return buildFailedResult(request, messageJson, "配置为空", "CONFIG_NULL");
    }

    if (StrUtil.isBlank(config.getBootstrapServers())) {
      log.warn("[Kafka推送] bootstrapServers为空，跳过推送");
      return buildFailedResult(request, messageJson, "bootstrapServers为空", "BOOTSTRAP_EMPTY");
    }

    if (StrUtil.isBlank(config.getTopic())) {
      log.warn("[Kafka推送] topic为空，跳过推送");
      return buildFailedResult(request, messageJson, "topic为空", "TOPIC_EMPTY");
    }

    String securityError = validateSecurityConfig(config);
    if (securityError != null) {
      log.warn("[Kafka推送] {}", securityError);
      return buildFailedResult(request, messageJson, securityError, "AUTH_CONFIG_INVALID");
    }

    try {
      long startTime = System.currentTimeMillis();
      String messageKey = StrUtil.blankToDefault(request.getIotId(), request.getDeviceId());
      KafkaProducer<String, String> producer = getOrCreateProducer(config);
      RecordMetadata metadata =
          producer
              .send(new ProducerRecord<>(config.getTopic(), messageKey, messageJson))
              .get(SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);

      log.debug(
          "[Kafka推送] 推送成功, topic={}, partition={}, offset={}, key={},elapsedMs={},payload={}",
          metadata.topic(),
          metadata.partition(),
          metadata.offset(),
          messageKey,
          System.currentTimeMillis() - startTime,messageJson);

      return IoTPushResult.success(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "Kafka",
          messageJson,
          System.currentTimeMillis() - startTime);
    } catch (Exception e) {
      logFailure(request.getIotId(), config.getTopic(), e);
      return buildFailedResult(request, messageJson, "推送异常: " + e.getMessage(), "PUSH_EXCEPTION");
    }
  }

  @Override
  public boolean isSupported(UPPushBO.KafkaPushConfig config) {
    return config != null && config.isSupport() && config.isEnable();
  }

  /**
   * Verifies the configured broker, TLS certificate chain, SASL credentials, and topic metadata
   * without producing an application message.
   */
  public void testConnection(UPPushBO.KafkaPushConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Kafka 推送配置不能为空");
    }
    if (StrUtil.isBlank(config.getBootstrapServers())) {
      throw new IllegalArgumentException("Kafka bootstrapServers 不能为空");
    }
    if (StrUtil.isBlank(config.getTopic())) {
      throw new IllegalArgumentException("Kafka topic 不能为空");
    }
    String securityError = validateSecurityConfig(config);
    if (securityError != null) {
      throw new IllegalArgumentException(securityError);
    }

    long startTime = System.currentTimeMillis();
    try (KafkaProducer<String, String> producer = new KafkaProducer<>(buildProperties(config))) {
      producer.partitionsFor(config.getTopic());
      log.debug(
          "[Kafka推送] 测试连接成功: bootstrapServers={}, topic={}, elapsedMs={}",
          config.getBootstrapServers(),
          config.getTopic(),
          System.currentTimeMillis() - startTime);
    }
  }

  private KafkaProducer<String, String> getOrCreateProducer(UPPushBO.KafkaPushConfig config) {
    String cacheKey =
        StrUtil.join(
            "|",
            config.getBootstrapServers(),
            resolveSecurityProtocol(config),
            resolveSaslMechanism(config),
            StrUtil.nullToEmpty(config.getUsername()),
            StrUtil.nullToEmpty(config.getPassword()));
    KafkaProducer<String, String> cached = producerCache.getIfPresent(cacheKey);
    if (cached != null) {
      log.debug(
          "[Kafka推送] 复用Producer: bootstrapServers={}, securityProtocol={}",
          config.getBootstrapServers(),
          resolveSecurityProtocol(config));
      return cached;
    }
    return producerCache.get(
        cacheKey,
        key -> {
          log.debug(
              "[Kafka推送] 创建Producer: bootstrapServers={}, securityProtocol={}, saslMechanism={}",
              config.getBootstrapServers(),
              resolveSecurityProtocol(config),
              resolveSaslMechanism(config));
          return new KafkaProducer<>(buildProperties(config));
        });
  }

  private void closeProducerLater(KafkaProducer<String, String> producer) {
    if (producer == null || destroying.get()) {
      return;
    }
    CompletableFuture.delayedExecutor(SEND_TIMEOUT_MS + 1000L, TimeUnit.MILLISECONDS)
        .execute(() -> closeProducer(producer));
  }

  private void closeProducer(KafkaProducer<String, String> producer) {
    try {
      producer.close(Duration.ofSeconds(3));
      log.debug("[Kafka推送] Producer已关闭");
    } catch (Exception e) {
      log.warn("[Kafka推送] 关闭Producer失败", e);
    }
  }

  Properties buildProperties(UPPushBO.KafkaPushConfig config) {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.ACKS_CONFIG, "0");
    properties.put(ProducerConfig.RETRIES_CONFIG, 0);
    properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, SEND_TIMEOUT_MS);
    properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
    properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, SEND_TIMEOUT_MS);

    String securityProtocol = resolveSecurityProtocol(config);
    properties.put("security.protocol", securityProtocol);
    if (securityProtocol.startsWith("SASL_")) {
      String saslMechanism = resolveSaslMechanism(config);
      properties.put("sasl.mechanism", saslMechanism);
      properties.put(
          "sasl.jaas.config",
          String.format(
              "%s required username=\"%s\" password=\"%s\";",
              resolveLoginModule(saslMechanism),
              escapeJaasValue(config.getUsername()),
              escapeJaasValue(config.getPassword())));
    }

    return properties;
  }

  private String resolveLoginModule(String saslMechanism) {
    if (StrUtil.isBlank(saslMechanism)) {
      return "org.apache.kafka.common.security.plain.PlainLoginModule";
    }

    String normalized = saslMechanism.trim().toUpperCase(Locale.ROOT);
    if (normalized.startsWith("SCRAM-")) {
      return "org.apache.kafka.common.security.scram.ScramLoginModule";
    }
    return "org.apache.kafka.common.security.plain.PlainLoginModule";
  }

  private String resolveSecurityProtocol(UPPushBO.KafkaPushConfig config) {
    String defaultProtocol =
        StrUtil.isNotBlank(config.getUsername()) || StrUtil.isNotBlank(config.getPassword())
            ? DEFAULT_SASL_SECURITY_PROTOCOL
            : DEFAULT_SECURITY_PROTOCOL;
    return StrUtil.blankToDefault(config.getSecurityProtocol(), defaultProtocol)
        .trim()
        .toUpperCase(Locale.ROOT);
  }

  private String resolveSaslMechanism(UPPushBO.KafkaPushConfig config) {
    return StrUtil.blankToDefault(config.getSaslMechanism(), DEFAULT_SASL_MECHANISM)
        .trim()
        .toUpperCase(Locale.ROOT);
  }

  private String validateSecurityConfig(UPPushBO.KafkaPushConfig config) {
    String protocol = resolveSecurityProtocol(config);
    if (!("PLAINTEXT".equals(protocol)
        || "SSL".equals(protocol)
        || "SASL_PLAINTEXT".equals(protocol)
        || "SASL_SSL".equals(protocol))) {
      return "不支持的安全协议: " + protocol;
    }
    if (!protocol.startsWith("SASL_")) {
      return null;
    }
    if (StrUtil.isBlank(config.getUsername()) || StrUtil.isBlank(config.getPassword())) {
      return "SASL 鉴权必须同时配置用户名和密码";
    }
    String mechanism = resolveSaslMechanism(config);
    if (!("PLAIN".equals(mechanism)
        || "SCRAM-SHA-256".equals(mechanism)
        || "SCRAM-SHA-512".equals(mechanism))) {
      return "不支持的 SASL 机制: " + mechanism;
    }
    return null;
  }

  private String escapeJaasValue(String value) {
    return StrUtil.nullToEmpty(value).replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private void logFailure(String iotId, String topic, Exception exception) {
    long now = System.nanoTime();
    long next = nextFailureWarningNanos.get();
    if (now >= next
        && nextFailureWarningNanos.compareAndSet(next, now + FAILURE_LOG_INTERVAL_NANOS)) {
      log.warn("[Kafka推送] 单次投递失败: iotId={}, topic={}", iotId, topic, exception);
      return;
    }
    log.debug(
        "[Kafka推送] 单次投递失败: iotId={}, topic={}, error={}",
        iotId,
        topic,
        exception.getMessage());
  }

  private IoTPushResult buildFailedResult(
      BaseUPRequest request, String messageJson, String errorMessage, String errorCode) {
    return IoTPushResult.failed(
        request.getIoTDeviceDTO().getThirdPlatform(),
        request.getProductKey(),
        request.getIotId(),
        "Kafka",
        messageJson,
        errorMessage,
        errorCode);
  }

  @PreDestroy
  public void destroy() {
    destroying.set(true);
    log.debug("[Kafka推送] 开始关闭Producer缓存: producerCount={}", producerCache.estimatedSize());
    producerCache.asMap().values().forEach(this::closeProducer);
    producerCache.invalidateAll();
    log.debug("[Kafka推送] Producer缓存已关闭");
  }
}
