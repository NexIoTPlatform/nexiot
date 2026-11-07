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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.stereotype.Component;

/**
 * RocketMQ推送策略实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class RocketMQPushStrategy implements PushStrategy<UPPushBO.RocketMQPushConfig> {

  private static final long SEND_TIMEOUT_MS = 1200L;
  private static final long FAILURE_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);
  private static final int MAX_CACHED_PRODUCERS = 64;

  private final AtomicBoolean destroying = new AtomicBoolean();
  private final AtomicLong nextFailureWarningNanos = new AtomicLong();
  private final Cache<String, DefaultMQProducer> producerCache =
      Caffeine.newBuilder()
          .maximumSize(MAX_CACHED_PRODUCERS)
          .expireAfterAccess(30, TimeUnit.MINUTES)
          .removalListener(
              (String key,
                      DefaultMQProducer producer,
                      com.github.benmanes.caffeine.cache.RemovalCause cause) ->
                  closeProducerLater(producer))
          .build();

  @Override
  public IoTPushResult execute(
      BaseUPRequest request, String messageJson, UPPushBO.RocketMQPushConfig config) {
    if (destroying.get()) {
      return buildFailedResult(request, messageJson, "推送组件正在关闭", "SHUTTING_DOWN");
    }
    if (config == null) {
      log.warn("[RocketMQ推送] 配置为空，跳过推送");
      return buildFailedResult(request, messageJson, "配置为空", "CONFIG_NULL");
    }

    if (StrUtil.isBlank(config.getNamesrvAddr())) {
      log.warn("[RocketMQ推送] namesrvAddr为空，跳过推送");
      return buildFailedResult(request, messageJson, "namesrvAddr为空", "NAMESRV_EMPTY");
    }

    if (StrUtil.isBlank(config.getTopic())) {
      log.warn("[RocketMQ推送] topic为空，跳过推送");
      return buildFailedResult(request, messageJson, "topic为空", "TOPIC_EMPTY");
    }

    if (StrUtil.isBlank(config.getGroup())) {
      log.warn("[RocketMQ推送] group为空，跳过推送");
      return buildFailedResult(request, messageJson, "group为空", "GROUP_EMPTY");
    }

    if (isIncompleteAuthConfig(config.getAccessKey(), config.getSecretKey())) {
      log.warn("[RocketMQ推送] AccessKey和SecretKey需同时配置");
      return buildFailedResult(request, messageJson, "AccessKey和SecretKey需同时配置", "AUTH_CONFIG_INVALID");
    }
    if (StrUtil.isNotBlank(config.getSecurityToken())
        && (StrUtil.isBlank(config.getAccessKey()) || StrUtil.isBlank(config.getSecretKey()))) {
      log.warn("[RocketMQ推送] SecurityToken需配合AccessKey和SecretKey使用");
      return buildFailedResult(
          request,
          messageJson,
          "SecurityToken需配合AccessKey和SecretKey使用",
          "AUTH_CONFIG_INVALID");
    }

    try {
      long startTime = System.currentTimeMillis();
      DefaultMQProducer producer = getOrCreateProducer(config);
      byte[] messageBody = messageJson.getBytes(StandardCharsets.UTF_8);
      Message message = new Message(config.getTopic(), messageBody);
      if (StrUtil.isNotBlank(request.getIotId())) {
        message.setKeys(request.getIotId());
      }

      log.debug(
          "[RocketMQ推送] 准备单向投递: namesrvAddr={}, topic={}, group={}, iotId={}, tls={}, bytes={}",
          config.getNamesrvAddr(),
          config.getTopic(),
          config.getGroup(),
          request.getIotId(),
          config.isUseTLS(),
          messageBody.length);
      producer.sendOneway(message);
      log.debug(
          "[RocketMQ推送] 单向投递已提交, topic={}, iotId={}, elapsedMs={}",
          config.getTopic(),
          request.getIotId(),
          System.currentTimeMillis() - startTime);

      return IoTPushResult.success(
          request.getIoTDeviceDTO().getThirdPlatform(),
          request.getProductKey(),
          request.getIotId(),
          "RocketMQ",
          messageJson,
          System.currentTimeMillis() - startTime);
    } catch (Exception e) {
      logFailure(request.getIotId(), config.getTopic(), e);
      return buildFailedResult(request, messageJson, "推送异常: " + e.getMessage(), "PUSH_EXCEPTION");
    }
  }

  @Override
  public boolean isSupported(UPPushBO.RocketMQPushConfig config) {
    return config != null && config.isSupport() && config.isEnable();
  }

  /**
   * Verifies NameServer reachability, ACL credentials, and the target topic route without sending
   * a message.
   */
  public void testConnection(UPPushBO.RocketMQPushConfig config) throws Exception {
    validateTestConfig(config);
    long startTime = System.currentTimeMillis();
    DefaultMQProducer producer = new DefaultMQProducer(config.getGroup(), buildRpcHook(config));
    try {
      configureProducer(producer, config);
      producer.start();
      producer.fetchPublishMessageQueues(config.getTopic());
      log.debug(
          "[RocketMQ推送] 测试连接成功: namesrvAddr={}, topic={}, elapsedMs={}",
          config.getNamesrvAddr(),
          config.getTopic(),
          System.currentTimeMillis() - startTime);
    } finally {
      producer.shutdown();
    }
  }

  private DefaultMQProducer getOrCreateProducer(UPPushBO.RocketMQPushConfig config) {
    String cacheKey =
        StrUtil.join(
            "|",
            config.getNamesrvAddr(),
            config.getGroup(),
            StrUtil.nullToEmpty(config.getAccessKey()),
            StrUtil.nullToEmpty(config.getSecretKey()),
            StrUtil.nullToEmpty(config.getSecurityToken()),
            StrUtil.nullToEmpty(config.getNamespace()),
            config.isUseTLS());
    DefaultMQProducer cached = producerCache.getIfPresent(cacheKey);
    if (cached != null) {
      log.debug(
          "[RocketMQ推送] 复用Producer: namesrvAddr={}, group={}",
          config.getNamesrvAddr(),
          config.getGroup());
      return cached;
    }
    return producerCache.get(
        cacheKey,
        key -> {
          try {
            log.debug(
                "[RocketMQ推送] 创建Producer: namesrvAddr={}, group={}, namespace={}, tls={}, acl={}",
                config.getNamesrvAddr(),
                config.getGroup(),
                config.getNamespace(),
                config.isUseTLS(),
                StrUtil.isNotBlank(config.getAccessKey()));
            DefaultMQProducer producer = new DefaultMQProducer(config.getGroup(), buildRpcHook(config));
            configureProducer(producer, config);
            producer.start();
            log.debug(
                "[RocketMQ推送] Producer启动成功: namesrvAddr={}, group={}",
                config.getNamesrvAddr(),
                config.getGroup());
            return producer;
          } catch (Exception e) {
            throw new IllegalStateException("RocketMQ Producer启动失败", e);
          }
        });
  }

  void configureProducer(DefaultMQProducer producer, UPPushBO.RocketMQPushConfig config) {
    producer.setNamesrvAddr(config.getNamesrvAddr());
    if (StrUtil.isNotBlank(config.getNamespace())) {
      producer.setNamespace(config.getNamespace());
    }
    producer.setUseTLS(config.isUseTLS());
    producer.setSendMsgTimeout((int) SEND_TIMEOUT_MS);
    producer.setRetryTimesWhenSendFailed(0);
  }

  private void closeProducerLater(DefaultMQProducer producer) {
    if (producer == null || destroying.get()) {
      return;
    }
    CompletableFuture.delayedExecutor(SEND_TIMEOUT_MS + 1000L, TimeUnit.MILLISECONDS)
        .execute(() -> closeProducer(producer));
  }

  private void closeProducer(DefaultMQProducer producer) {
    try {
      producer.shutdown();
      log.debug("[RocketMQ推送] Producer已关闭: group={}", producer.getProducerGroup());
    } catch (Exception e) {
      log.warn("[RocketMQ推送] 关闭Producer失败", e);
    }
  }

  private RPCHook buildRpcHook(UPPushBO.RocketMQPushConfig config) {
    if (StrUtil.isBlank(config.getAccessKey()) || StrUtil.isBlank(config.getSecretKey())) {
      return null;
    }

    SessionCredentials credentials =
        StrUtil.isNotBlank(config.getSecurityToken())
            ? new SessionCredentials(
                config.getAccessKey(), config.getSecretKey(), config.getSecurityToken())
            : new SessionCredentials(config.getAccessKey(), config.getSecretKey());
    return new AclClientRPCHook(credentials);
  }

  private boolean isIncompleteAuthConfig(String accessKey, String secretKey) {
    return StrUtil.isBlank(accessKey) != StrUtil.isBlank(secretKey);
  }

  private void validateTestConfig(UPPushBO.RocketMQPushConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("RocketMQ 推送配置不能为空");
    }
    if (StrUtil.isBlank(config.getNamesrvAddr())) {
      throw new IllegalArgumentException("RocketMQ namesrvAddr 不能为空");
    }
    if (StrUtil.isBlank(config.getTopic())) {
      throw new IllegalArgumentException("RocketMQ topic 不能为空");
    }
    if (StrUtil.isBlank(config.getGroup())) {
      throw new IllegalArgumentException("RocketMQ group 不能为空");
    }
    if (isIncompleteAuthConfig(config.getAccessKey(), config.getSecretKey())) {
      throw new IllegalArgumentException("RocketMQ AccessKey 和 SecretKey 必须同时配置");
    }
    if (StrUtil.isNotBlank(config.getSecurityToken())
        && (StrUtil.isBlank(config.getAccessKey()) || StrUtil.isBlank(config.getSecretKey()))) {
      throw new IllegalArgumentException("RocketMQ SecurityToken 必须配合 AccessKey 和 SecretKey 使用");
    }
  }

  private void logFailure(String iotId, String topic, Exception exception) {
    long now = System.nanoTime();
    long next = nextFailureWarningNanos.get();
    if (now >= next
        && nextFailureWarningNanos.compareAndSet(next, now + FAILURE_LOG_INTERVAL_NANOS)) {
      log.warn("[RocketMQ推送] 单向投递失败: iotId={}, topic={}", iotId, topic, exception);
      return;
    }
    log.debug(
        "[RocketMQ推送] 单向投递失败: iotId={}, topic={}, error={}",
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
        "RocketMQ",
        messageJson,
        errorMessage,
        errorCode);
  }

  @PreDestroy
  public void destroy() {
    destroying.set(true);
    log.debug(
        "[RocketMQ推送] 开始关闭Producer缓存: producerCount={}", producerCache.estimatedSize());
    producerCache.asMap().values().forEach(this::closeProducer);
    producerCache.invalidateAll();
    log.debug("[RocketMQ推送] Producer缓存已关闭");
  }
}
