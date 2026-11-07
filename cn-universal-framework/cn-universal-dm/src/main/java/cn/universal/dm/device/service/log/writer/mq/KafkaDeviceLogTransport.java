package cn.universal.dm.device.service.log.writer.mq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.log.writer.DeviceLogWritePublisher;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRecord;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaDeviceLogTransport implements DeviceLogWritePublisher, SmartLifecycle {

  private final DeviceLogMqMessageHandler messageHandler;
  private final boolean mqEnabled;
  private final String configuredProvider;
  private final boolean consumerEnabled;
  private final String bootstrapServers;
  private final String topic;
  private final String consumerGroup;
  private final String username;
  private final String password;
  private final String securityProtocol;
  private final String saslMechanism;
  private final long sendTimeoutMs;
  private final long pollTimeoutMs;
  private final long retryBackoffMs;
  private final int maxConsumeAttempts;
  private final String dlqTopic;
  private final String autoOffsetReset;
  private final AtomicBoolean running = new AtomicBoolean();
  private final Map<String, Integer> consumeAttempts = new ConcurrentHashMap<>();
  private final ExecutorService consumerExecutor =
      Executors.newSingleThreadExecutor(Thread.ofVirtual().name("device-log-kafka-consumer").factory());

  private volatile KafkaProducer<String, String> producer;
  private volatile KafkaConsumer<String, String> consumer;

  public KafkaDeviceLogTransport(
      DeviceLogMqMessageHandler messageHandler,
      @Value("${nexiot.device-log.mq.enabled:false}") boolean mqEnabled,
      @Value("${nexiot.device-log.mq.provider:kafka}") String configuredProvider,
      @Value("${nexiot.device-log.mq.consumer-enabled:true}") boolean consumerEnabled,
      @Value("${nexiot.device-log.mq.kafka.bootstrap-servers:}") String bootstrapServers,
      @Value("${nexiot.device-log.mq.topic:device-up-v1}") String topic,
      @Value("${nexiot.device-log.mq.consumer-group:nexiot-device-log-storage-v1}")
          String consumerGroup,
      @Value("${nexiot.device-log.mq.kafka.username:}") String username,
      @Value("${nexiot.device-log.mq.kafka.password:}") String password,
      @Value("${nexiot.device-log.mq.kafka.security-protocol:SASL_PLAINTEXT}")
          String securityProtocol,
      @Value("${nexiot.device-log.mq.kafka.sasl-mechanism:PLAIN}") String saslMechanism,
      @Value("${nexiot.device-log.mq.send-timeout-ms:5000}") long sendTimeoutMs,
      @Value("${nexiot.device-log.mq.kafka.poll-timeout-ms:1000}") long pollTimeoutMs,
      @Value("${nexiot.device-log.mq.retry-backoff-ms:1000}") long retryBackoffMs,
      @Value("${nexiot.device-log.mq.max-consume-attempts:5}") int maxConsumeAttempts,
      @Value("${nexiot.device-log.mq.dlq-topic:device-up-v1-dlq}") String dlqTopic,
      @Value("${nexiot.device-log.mq.kafka.auto-offset-reset:earliest}")
          String autoOffsetReset) {
    this.messageHandler = messageHandler;
    this.mqEnabled = mqEnabled;
    this.configuredProvider = configuredProvider;
    this.consumerEnabled = consumerEnabled;
    this.bootstrapServers = bootstrapServers;
    this.topic = topic;
    this.consumerGroup = consumerGroup;
    this.username = username;
    this.password = password;
    this.securityProtocol = securityProtocol;
    this.saslMechanism = saslMechanism;
    this.sendTimeoutMs = Math.max(1000L, sendTimeoutMs);
    this.pollTimeoutMs = Math.max(100L, pollTimeoutMs);
    this.retryBackoffMs = Math.max(100L, retryBackoffMs);
    this.maxConsumeAttempts = Math.max(1, maxConsumeAttempts);
    this.dlqTopic = dlqTopic;
    this.autoOffsetReset = StrUtil.blankToDefault(autoOffsetReset, "earliest");
  }

  @Override
  public String provider() {
    return "kafka";
  }

  @Override
  public void publish(List<DeviceLogWriteRecord> records) {
    if (!selected()) {
      throw new IllegalStateException("Kafka device-log MQ transport is not enabled");
    }
    if (records == null || records.isEmpty()) {
      return;
    }
    KafkaProducer<String, String> currentProducer = producer();
    try {
      List<Future<RecordMetadata>> futures =
          records.stream()
              .map(
                  record ->
                      currentProducer.send(
                          new ProducerRecord<>(
                              topic, record.partitionKey(), JSONUtil.toJsonStr(record))))
              .toList();
      long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(sendTimeoutMs);
      for (Future<RecordMetadata> future : futures) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0L) {
          throw new java.util.concurrent.TimeoutException("Kafka device-log batch send timed out");
        }
        future.get(remainingNanos, TimeUnit.NANOSECONDS);
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Kafka device-log publish failed", ex);
    }
  }

  @Override
  public synchronized void start() {
    if (!selected() || !consumerEnabled || !running.compareAndSet(false, true)) {
      return;
    }
    validateConfig();
    consumerExecutor.execute(this::consumeLoop);
    log.info("Device-log Kafka consumer started topic={}, group={}", topic, consumerGroup);
  }

  private void consumeLoop() {
    try (KafkaConsumer<String, String> currentConsumer = new KafkaConsumer<>(consumerProperties())) {
      consumer = currentConsumer;
      currentConsumer.subscribe(List.of(topic));
      while (running.get()) {
        ConsumerRecords<String, String> records =
            currentConsumer.poll(Duration.ofMillis(pollTimeoutMs));
        if (records.isEmpty()) {
          continue;
        }
        Map<TopicPartition, Long> firstOffsets = firstOffsets(records);
        try {
          consumeBatch(records);
          currentConsumer.commitSync();
        } catch (Exception ex) {
          log.warn("Device-log Kafka consume failed; batch will retry: {}", ex.getMessage());
          firstOffsets.forEach(currentConsumer::seek);
          sleepBackoff();
        }
      }
    } catch (WakeupException ex) {
      if (running.get()) {
        log.error("Device-log Kafka consumer woke unexpectedly", ex);
      }
    } catch (Exception ex) {
      if (running.get()) {
        log.error("Device-log Kafka consumer stopped unexpectedly", ex);
      }
    } finally {
      consumer = null;
      running.set(false);
    }
  }

  private void consumeBatch(ConsumerRecords<String, String> records) throws Exception {
    List<ConsumerRecord<String, String>> sourceRecords = new ArrayList<>(records.count());
    List<DeviceLogWriteRecord> events = new ArrayList<>(records.count());
    try {
      for (ConsumerRecord<String, String> record : records) {
        sourceRecords.add(record);
        events.add(JSONUtil.toBean(record.value(), DeviceLogWriteRecord.class));
      }
      messageHandler.handleBatch(events);
      sourceRecords.forEach(record -> consumeAttempts.remove(attemptKey(record)));
      return;
    } catch (Exception batchFailure) {
      log.warn(
          "Device-log Kafka batch persistence failed; isolating records: rows={}, err={}",
          records.count(),
          batchFailure.getMessage());
    }

    boolean retryRequired = false;
    for (ConsumerRecord<String, String> record : records) {
      try {
        consumeRecord(record);
      } catch (Exception ex) {
        retryRequired = true;
      }
    }
    if (retryRequired) {
      throw new IllegalStateException("Device-log Kafka records require retry");
    }
  }

  private void consumeRecord(ConsumerRecord<String, String> record) throws Exception {
    String attemptKey = attemptKey(record);
    try {
      DeviceLogWriteRecord event = JSONUtil.toBean(record.value(), DeviceLogWriteRecord.class);
      messageHandler.handle(event);
      consumeAttempts.remove(attemptKey);
    } catch (Exception ex) {
      int attempts = consumeAttempts.merge(attemptKey, 1, Integer::sum);
      if (attempts < maxConsumeAttempts) {
        throw ex;
      }
      publishDlq(record, ex, attempts);
      consumeAttempts.remove(attemptKey);
    }
  }

  private String attemptKey(ConsumerRecord<String, String> record) {
    return record.topic() + ':' + record.partition() + ':' + record.offset();
  }

  private void publishDlq(ConsumerRecord<String, String> record, Exception failure, int attempts)
      throws Exception {
    ProducerRecord<String, String> dlq = new ProducerRecord<>(dlqTopic, record.key(), record.value());
    dlq.headers().add("nexiot-error", StrUtil.nullToEmpty(failure.getMessage()).getBytes());
    dlq.headers().add("nexiot-attempts", String.valueOf(attempts).getBytes());
    producer().send(dlq).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
    log.error(
        "Device-log event moved to Kafka DLQ topic={}, sourcePartition={}, sourceOffset={}",
        dlqTopic,
        record.partition(),
        record.offset(),
        failure);
  }

  private Map<TopicPartition, Long> firstOffsets(ConsumerRecords<String, String> records) {
    Map<TopicPartition, Long> offsets = new HashMap<>();
    for (ConsumerRecord<String, String> record : records) {
      offsets.putIfAbsent(new TopicPartition(record.topic(), record.partition()), record.offset());
    }
    return offsets;
  }

  private synchronized KafkaProducer<String, String> producer() {
    if (producer == null) {
      validateConfig();
      producer = new KafkaProducer<>(producerProperties());
    }
    return producer;
  }

  private Properties producerProperties() {
    Properties properties = commonProperties();
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.ACKS_CONFIG, "all");
    properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
    properties.put(ProducerConfig.LINGER_MS_CONFIG, 5);
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 64 * 1024);
    properties.put(
        ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Math.toIntExact(sendTimeoutMs));
    properties.put(
        ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
        Math.toIntExact(Math.min(3000L, Math.max(500L, sendTimeoutMs - 100L))));
    return properties;
  }

  private Properties consumerProperties() {
    Properties properties = commonProperties();
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
    properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
    return properties;
  }

  private Properties commonProperties() {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    if (StrUtil.isNotBlank(username) && StrUtil.isNotBlank(password)) {
      String mechanism = saslMechanism.trim().toUpperCase(Locale.ROOT);
      properties.put("security.protocol", securityProtocol.trim().toUpperCase(Locale.ROOT));
      properties.put("sasl.mechanism", mechanism);
      properties.put(
          "sasl.jaas.config",
          loginModule(mechanism)
              + " required username=\""
              + username
              + "\" password=\""
              + password
              + "\";");
    }
    return properties;
  }

  private String loginModule(String mechanism) {
    return mechanism.startsWith("SCRAM-")
        ? "org.apache.kafka.common.security.scram.ScramLoginModule"
        : "org.apache.kafka.common.security.plain.PlainLoginModule";
  }

  private void validateConfig() {
    if (StrUtil.isBlank(bootstrapServers) || StrUtil.isBlank(topic)) {
      throw new IllegalStateException("Kafka bootstrap servers and device-log topic are required");
    }
    if (StrUtil.isBlank(username) != StrUtil.isBlank(password)) {
      throw new IllegalStateException("Kafka username and password must be configured together");
    }
  }

  private boolean selected() {
    return mqEnabled && "kafka".equalsIgnoreCase(configuredProvider);
  }

  private void sleepBackoff() {
    try {
      Thread.sleep(retryBackoffMs);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public synchronized void stop() {
    running.set(false);
    KafkaConsumer<String, String> currentConsumer = consumer;
    if (currentConsumer != null) {
      currentConsumer.wakeup();
    }
    KafkaProducer<String, String> currentProducer = producer;
    producer = null;
    if (currentProducer != null) {
      currentProducer.close(Duration.ofSeconds(3));
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
    consumerExecutor.shutdownNow();
  }
}
