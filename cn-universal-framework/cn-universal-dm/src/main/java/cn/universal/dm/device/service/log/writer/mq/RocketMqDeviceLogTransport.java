package cn.universal.dm.device.service.log.writer.mq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.log.writer.DeviceLogWritePublisher;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRecord;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RocketMqDeviceLogTransport
    implements DeviceLogWritePublisher, SmartLifecycle, MessageListenerOrderly {

  private final DeviceLogMqMessageHandler messageHandler;
  private final boolean mqEnabled;
  private final String configuredProvider;
  private final boolean consumerEnabled;
  private final String namesrvAddr;
  private final String topic;
  private final String producerGroup;
  private final String consumerGroup;
  private final String accessKey;
  private final String secretKey;
  private final String securityToken;
  private final long sendTimeoutMs;
  private final long retryBackoffMs;
  private final int maxConsumeAttempts;
  private final AtomicBoolean running = new AtomicBoolean();

  private volatile DefaultMQProducer producer;
  private volatile DefaultMQPushConsumer consumer;

  public RocketMqDeviceLogTransport(
      DeviceLogMqMessageHandler messageHandler,
      @Value("${nexiot.device-log.mq.enabled:false}") boolean mqEnabled,
      @Value("${nexiot.device-log.mq.provider:kafka}") String configuredProvider,
      @Value("${nexiot.device-log.mq.consumer-enabled:true}") boolean consumerEnabled,
      @Value("${nexiot.device-log.mq.rocketmq.namesrv-addr:}") String namesrvAddr,
      @Value("${nexiot.device-log.mq.topic:device-up-v1}") String topic,
      @Value("${nexiot.device-log.mq.rocketmq.producer-group:nexiot-device-log-producer-v1}")
          String producerGroup,
      @Value("${nexiot.device-log.mq.consumer-group:nexiot-device-log-storage-v1}")
          String consumerGroup,
      @Value("${nexiot.device-log.mq.rocketmq.access-key:}") String accessKey,
      @Value("${nexiot.device-log.mq.rocketmq.secret-key:}") String secretKey,
      @Value("${nexiot.device-log.mq.rocketmq.security-token:}") String securityToken,
      @Value("${nexiot.device-log.mq.send-timeout-ms:5000}") long sendTimeoutMs,
      @Value("${nexiot.device-log.mq.retry-backoff-ms:1000}") long retryBackoffMs,
      @Value("${nexiot.device-log.mq.max-consume-attempts:5}") int maxConsumeAttempts) {
    this.messageHandler = messageHandler;
    this.mqEnabled = mqEnabled;
    this.configuredProvider = configuredProvider;
    this.consumerEnabled = consumerEnabled;
    this.namesrvAddr = namesrvAddr;
    this.topic = topic;
    this.producerGroup = producerGroup;
    this.consumerGroup = consumerGroup;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.securityToken = securityToken;
    this.sendTimeoutMs = Math.max(1000L, sendTimeoutMs);
    this.retryBackoffMs = Math.max(100L, retryBackoffMs);
    this.maxConsumeAttempts = Math.max(1, maxConsumeAttempts);
  }

  @Override
  public String provider() {
    return "rocketmq";
  }

  @Override
  public void publish(List<DeviceLogWriteRecord> records) {
    if (!selected()) {
      throw new IllegalStateException("RocketMQ device-log transport is not enabled");
    }
    if (records == null || records.isEmpty()) {
      return;
    }
    DefaultMQProducer currentProducer = producer();
    try {
      for (DeviceLogWriteRecord record : records) {
        Message message =
            new Message(topic, JSONUtil.toJsonStr(record).getBytes(StandardCharsets.UTF_8));
        message.setKeys(record.eventId());
        SendResult result =
            currentProducer.send(
                message,
                (queues, ignored, key) ->
                    queues.get(Math.floorMod(String.valueOf(key).hashCode(), queues.size())),
                record.partitionKey(),
                sendTimeoutMs);
        if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
          throw new IllegalStateException(
              "RocketMQ send status is " + (result == null ? "null" : result.getSendStatus()));
        }
      }
    } catch (Exception ex) {
      throw new IllegalStateException("RocketMQ device-log publish failed", ex);
    }
  }

  @Override
  public synchronized void start() {
    if (!selected() || !consumerEnabled || !running.compareAndSet(false, true)) {
      return;
    }
    try {
      validateConfig();
      DefaultMQPushConsumer currentConsumer =
          new DefaultMQPushConsumer(consumerGroup, rpcHook(), null);
      currentConsumer.setNamesrvAddr(namesrvAddr);
      currentConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
      currentConsumer.setMaxReconsumeTimes(maxConsumeAttempts);
      currentConsumer.setSuspendCurrentQueueTimeMillis(retryBackoffMs);
      currentConsumer.subscribe(topic, "*");
      currentConsumer.registerMessageListener(this);
      currentConsumer.start();
      consumer = currentConsumer;
      log.info("Device-log RocketMQ consumer started topic={}, group={}", topic, consumerGroup);
    } catch (Exception ex) {
      running.set(false);
      throw new IllegalStateException("RocketMQ device-log consumer start failed", ex);
    }
  }

  @Override
  public ConsumeOrderlyStatus consumeMessage(
      List<MessageExt> messages, ConsumeOrderlyContext context) {
    try {
      List<DeviceLogWriteRecord> records = new ArrayList<>(messages.size());
      for (MessageExt message : messages) {
        records.add(toRecord(message));
      }
      messageHandler.handleBatch(records);
      return ConsumeOrderlyStatus.SUCCESS;
    } catch (Exception ex) {
      log.warn(
          "Device-log RocketMQ batch persistence failed; isolating records: rows={}, err={}",
          messages.size(),
          ex.getMessage());
      boolean retryRequired = false;
      for (MessageExt message : messages) {
        try {
          messageHandler.handle(toRecord(message));
        } catch (Exception recordFailure) {
          retryRequired = true;
        }
      }
      if (!retryRequired) {
        return ConsumeOrderlyStatus.SUCCESS;
      }
      context.setSuspendCurrentQueueTimeMillis(retryBackoffMs);
      log.warn("Device-log RocketMQ consume failed; message will retry: {}", ex.getMessage());
      return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
    }
  }

  private DeviceLogWriteRecord toRecord(MessageExt message) {
    return JSONUtil.toBean(
        new String(message.getBody(), StandardCharsets.UTF_8), DeviceLogWriteRecord.class);
  }

  private synchronized DefaultMQProducer producer() {
    if (producer != null) {
      return producer;
    }
    validateConfig();
    try {
      DefaultMQProducer created = new DefaultMQProducer(producerGroup, rpcHook());
      created.setNamesrvAddr(namesrvAddr);
      created.setRetryTimesWhenSendFailed(2);
      created.start();
      producer = created;
      return created;
    } catch (Exception ex) {
      throw new IllegalStateException("RocketMQ device-log producer start failed", ex);
    }
  }

  private RPCHook rpcHook() {
    if (StrUtil.isBlank(accessKey) || StrUtil.isBlank(secretKey)) {
      return null;
    }
    SessionCredentials credentials =
        StrUtil.isBlank(securityToken)
            ? new SessionCredentials(accessKey, secretKey)
            : new SessionCredentials(accessKey, secretKey, securityToken);
    return new AclClientRPCHook(credentials);
  }

  private void validateConfig() {
    if (StrUtil.isBlank(namesrvAddr) || StrUtil.isBlank(topic)) {
      throw new IllegalStateException("RocketMQ nameserver and device-log topic are required");
    }
    if (StrUtil.isBlank(accessKey) != StrUtil.isBlank(secretKey)) {
      throw new IllegalStateException("RocketMQ accessKey and secretKey must be configured together");
    }
  }

  private boolean selected() {
    return mqEnabled && "rocketmq".equalsIgnoreCase(configuredProvider);
  }

  @Override
  public synchronized void stop() {
    running.set(false);
    DefaultMQPushConsumer currentConsumer = consumer;
    consumer = null;
    if (currentConsumer != null) {
      currentConsumer.shutdown();
    }
    DefaultMQProducer currentProducer = producer;
    producer = null;
    if (currentProducer != null) {
      currentProducer.shutdown();
    }
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE - 100;
  }

  @PreDestroy
  public void destroy() {
    stop();
  }
}
