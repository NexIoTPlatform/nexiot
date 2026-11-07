package cn.universal.dm.device.service.log.writer;

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.service.log.IIoTDeviceLogService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Shared bounded writer for device logs.
 *
 * <p>The writer batches all log policies through one controlled path, while preserving an SPI for
 * future Kafka/RocketMQ fan-out. It tries hard to persist logs: flush on full, wait briefly, then
 * fallback to a direct write before dropping.
 */
@Slf4j
@Component
public class DeviceLogWriteBuffer implements SmartLifecycle {

  private final boolean enabled;
  private final DeviceLogWriteMode mode;
  private final String mqProvider;
  private final boolean fallbackDirectEnabled;
  private final int queueCapacity;
  private final int batchSize;
  private final int workerCount;
  private final int maxWorkerCount;
  private final long flushIntervalMs;
  private final long enqueueWaitMs;
  private final long shutdownTimeoutMs;
  private final List<DeviceLogWritePublisher> publishers;
  private final DeviceLogWritePublisher mqPublisher;
  private final BlockingQueue<QueuedWrite> queue;
  private final ExecutorService executor;
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final AtomicLong activeWorkers = new AtomicLong();
  private final WriterMetrics globalMetrics = new WriterMetrics();
  private final Map<String, WriterMetrics> policyMetrics =
      new java.util.concurrent.ConcurrentHashMap<>();

  public DeviceLogWriteBuffer(
      @Value("${nexiot.device-log.mode:buffer}") String mode,
      @Value("${nexiot.device-log.mq.provider:kafka}") String mqProvider,
      @Value("${nexiot.device-log.mq.enabled:false}") boolean mqEnabled,
      @Value("${nexiot.device-log.writer.enabled:true}") boolean enabled,
      @Value("${nexiot.device-log.writer.fallback-direct-enabled:true}")
          boolean fallbackDirectEnabled,
      @Value("${nexiot.device-log.writer.queue-capacity:100000}") int queueCapacity,
      @Value("${nexiot.device-log.writer.batch-size:1000}") int batchSize,
      @Value("${nexiot.device-log.writer.worker-count:2}") int workerCount,
      @Value("${nexiot.device-log.writer.max-worker-count:8}") int maxWorkerCount,
      @Value("${nexiot.device-log.writer.flush-interval-ms:200}") long flushIntervalMs,
      @Value("${nexiot.device-log.writer.enqueue-wait-ms:1000}") long enqueueWaitMs,
      @Value("${nexiot.device-log.writer.shutdown-timeout-ms:10000}") long shutdownTimeoutMs,
      List<DeviceLogWritePublisher> publishers) {
    this.enabled = enabled;
    this.mode =
        mqEnabled
            ? DeviceLogWriteMode.MQ
            : (enabled ? DeviceLogWriteMode.parse(mode) : DeviceLogWriteMode.DIRECT);
    this.mqProvider = StrUtil.blankToDefault(mqProvider, "kafka").trim().toLowerCase(Locale.ROOT);
    this.fallbackDirectEnabled = fallbackDirectEnabled;
    this.queueCapacity = Math.max(1, queueCapacity);
    this.batchSize = Math.max(1, batchSize);
    this.maxWorkerCount = Math.max(1, maxWorkerCount);
    this.workerCount = Math.min(Math.max(1, workerCount), this.maxWorkerCount);
    this.flushIntervalMs = Math.max(10L, flushIntervalMs);
    this.enqueueWaitMs = Math.max(0L, enqueueWaitMs);
    this.shutdownTimeoutMs = Math.max(1_000L, shutdownTimeoutMs);
    this.publishers = publishers == null ? Collections.emptyList() : List.copyOf(publishers);
    this.mqPublisher =
        this.publishers.stream()
            .filter(publisher -> this.mqProvider.equalsIgnoreCase(publisher.provider()))
            .findFirst()
            .orElse(null);
    this.queue = new ArrayBlockingQueue<>(this.queueCapacity);
    this.executor =
        Executors.newFixedThreadPool(
            this.workerCount,
            task -> {
              Thread thread = new Thread(task, "device-log-writer");
              thread.setDaemon(true);
              return thread;
            });
    if (this.mode != DeviceLogWriteMode.DIRECT) {
      for (int i = 0; i < this.workerCount; i++) {
        executor.execute(this::workerLoop);
      }
      log.info(
          "DeviceLogWriteBuffer enabled mode={}, queueCapacity={}, batchSize={}, workers={}, maxWorkers={}, flushIntervalMs={}, enqueueWaitMs={}, mqProvider={}, mqPublisher={}",
          this.mode,
          this.queueCapacity,
          this.batchSize,
          this.workerCount,
          this.maxWorkerCount,
          this.flushIntervalMs,
          this.enqueueWaitMs,
          this.mqProvider,
          this.mqPublisher == null ? "none" : this.mqPublisher.getClass().getSimpleName());
    } else {
      log.info("DeviceLogWriteBuffer disabled; device logs use direct service writes");
    }
  }

  public boolean submitRequest(
      String policy,
      IIoTDeviceLogService service,
      BaseUPRequest upRequest,
      IoTDeviceDTO ioTDeviceDTO,
      IoTProduct ioTProduct) {
    if (service == null || upRequest == null || ioTProduct == null) {
      return true;
    }
    QueuedWrite write =
        QueuedWrite.request(policyKey(policy), service, upRequest, ioTDeviceDTO, ioTProduct);
    return submit(write);
  }

  public boolean submitEntity(
      String policy,
      IIoTDeviceLogService service,
      IoTDeviceLog ioTDeviceLog,
      IoTDeviceDTO ioTDeviceDTO,
      IoTProduct ioTProduct) {
    if (service == null || ioTDeviceLog == null || ioTProduct == null) {
      return true;
    }
    QueuedWrite write =
        QueuedWrite.entity(policyKey(policy), service, ioTDeviceLog, ioTDeviceDTO, ioTProduct);
    return submit(write);
  }

  private boolean submit(QueuedWrite write) {
    if (!running.get()) {
      return false;
    }
    if (mode == DeviceLogWriteMode.DIRECT) {
      return writeDirect(write);
    }
    WriterMetrics metrics = metricsFor(write.policy());
    metrics.acceptedCount.incrementAndGet();
    globalMetrics.acceptedCount.incrementAndGet();
    if (queue.offer(write)) {
      onEnqueued(write, metrics);
      return true;
    }
    metrics.queueFullCount.incrementAndGet();
    globalMetrics.queueFullCount.incrementAndGet();
    flushSome(false);
    if (offerWithWait(write)) {
      onEnqueued(write, metrics);
      return true;
    }
    if (fallbackDirectEnabled) {
      metrics.queueFullFallbackCount.incrementAndGet();
      globalMetrics.queueFullFallbackCount.incrementAndGet();
      metrics.fallbackDirectCount.incrementAndGet();
      globalMetrics.fallbackDirectCount.incrementAndGet();
      boolean written = writeDirect(write);
      return written;
    }
    metrics.droppedCount.incrementAndGet();
    globalMetrics.droppedCount.incrementAndGet();
    log.warn(
        "Device log writer queue full, dropped log policy={}, queueCapacity={}, dropped={}",
        write.policy(),
        queueCapacity,
        metrics.droppedCount.get());
    return false;
  }

  private boolean offerWithWait(QueuedWrite write) {
    if (enqueueWaitMs <= 0L) {
      return queue.offer(write);
    }
    try {
      return queue.offer(write, enqueueWaitMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private void onEnqueued(QueuedWrite write, WriterMetrics metrics) {
    metrics.enqueuedDeviceLogCount.incrementAndGet();
    metrics.pendingCount.incrementAndGet();
    globalMetrics.enqueuedDeviceLogCount.incrementAndGet();
    globalMetrics.pendingCount.incrementAndGet();
  }

  private void workerLoop() {
    while (running.get() || !queue.isEmpty()) {
      flushSome(true);
    }
  }

  private void flushSome(boolean waitForFirst) {
    List<QueuedWrite> batch = drainBatch(waitForFirst);
    if (batch.isEmpty()) {
      return;
    }
    activeWorkers.incrementAndGet();
    try {
      flushBatch(batch);
    } finally {
      activeWorkers.decrementAndGet();
    }
  }

  private List<QueuedWrite> drainBatch(boolean waitForFirst) {
    List<QueuedWrite> batch = new ArrayList<>(batchSize);
    QueuedWrite first = pollFirst(waitForFirst);
    if (first == null) {
      return batch;
    }
    batch.add(first);
    queue.drainTo(batch, batchSize - 1);
    if (waitForFirst && batch.size() < batchSize) {
      long deadline = System.currentTimeMillis() + flushIntervalMs;
      while (batch.size() < batchSize) {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0L) {
          break;
        }
        try {
          QueuedWrite next = queue.poll(remaining, TimeUnit.MILLISECONDS);
          if (next == null) {
            break;
          }
          batch.add(next);
          queue.drainTo(batch, batchSize - batch.size());
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
    for (QueuedWrite write : batch) {
      metricsFor(write.policy()).pendingCount.decrementAndGet();
      globalMetrics.pendingCount.decrementAndGet();
    }
    return batch;
  }

  private QueuedWrite pollFirst(boolean waitForFirst) {
    try {
      return waitForFirst ? queue.poll(flushIntervalMs, TimeUnit.MILLISECONDS) : queue.poll();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private void flushBatch(List<QueuedWrite> batch) {
    Map<ServiceKey, List<QueuedWrite>> grouped = new LinkedHashMap<>();
    for (QueuedWrite write : batch) {
      grouped
          .computeIfAbsent(new ServiceKey(write.policy(), write.service()), ignored -> new ArrayList<>())
          .add(write);
    }
    grouped.forEach((key, writes) -> flushServiceBatch(key, writes));
  }

  private void flushServiceBatch(ServiceKey key, List<QueuedWrite> writes) {
    if (writes == null || writes.isEmpty()) {
      return;
    }
    long startedAt = System.currentTimeMillis();
    long lagMs = maxLagMs(writes, startedAt);
    WriterMetrics metrics = metricsFor(key.policy());
    if (mode == DeviceLogWriteMode.MQ) {
      publishMqBatch(key, writes, startedAt, lagMs, metrics);
      return;
    }
    try {
      List<DeviceLogWriteRequest> requests = new ArrayList<>();
      List<DeviceLogEntityWrite> entities = new ArrayList<>();
      for (QueuedWrite write : writes) {
        DeviceLogWriteRecord record = write.record();
        if (record.requestWrite()) {
          requests.add(
              new DeviceLogWriteRequest(
                  record.toRequest(), record.toDevice(), record.toProduct()));
        } else if (record.entityWrite()) {
          entities.add(
              new DeviceLogEntityWrite(
                  record.toDeviceLog(), record.toDevice(), record.toProduct()));
        }
      }
      if (!requests.isEmpty()) {
        key.service().flushDeviceLogRequests(requests);
      }
      if (!entities.isEmpty()) {
        key.service().flushDeviceLogEntities(entities);
      }
      recordFlushSuccess(metrics, writes.size(), startedAt, lagMs);
      recordFlushSuccess(globalMetrics, writes.size(), startedAt, lagMs);
    } catch (Exception ex) {
      recordFlushFailure(metrics, writes.size(), startedAt, lagMs);
      recordFlushFailure(globalMetrics, writes.size(), startedAt, lagMs);
      log.warn(
          "Device log writer batch failed policy={}, rows={}, err={}",
          key.policy(),
          writes.size(),
          ex.getMessage());
      retryDirect(writes);
    }
  }

  private void publishMqBatch(
      ServiceKey key,
      List<QueuedWrite> writes,
      long startedAt,
      long lagMs,
      WriterMetrics metrics) {
    try {
      if (mqPublisher == null) {
        throw new IllegalStateException("No device-log MQ publisher for provider " + mqProvider);
      }
      mqPublisher.publish(writes.stream().map(QueuedWrite::record).toList());
      metrics.publisherSuccessCount.addAndGet(writes.size());
      globalMetrics.publisherSuccessCount.addAndGet(writes.size());
      recordFlushCommon(
          metrics,
          writes.size(),
          Math.max(0L, System.currentTimeMillis() - startedAt),
          lagMs);
      recordFlushCommon(
          globalMetrics,
          writes.size(),
          Math.max(0L, System.currentTimeMillis() - startedAt),
          lagMs);
    } catch (Exception ex) {
      metrics.publisherFailureCount.addAndGet(writes.size());
      globalMetrics.publisherFailureCount.addAndGet(writes.size());
      log.warn(
          "Device log MQ publish failed policy={}, provider={}, rows={}, err={}",
          key.policy(),
          mqProvider,
          writes.size(),
          ex.getMessage());
      if (fallbackDirectEnabled) {
        metrics.fallbackDirectCount.addAndGet(writes.size());
        globalMetrics.fallbackDirectCount.addAndGet(writes.size());
        retryDirect(writes);
      } else {
        metrics.droppedCount.addAndGet(writes.size());
        globalMetrics.droppedCount.addAndGet(writes.size());
      }
    }
  }

  private void retryDirect(List<QueuedWrite> writes) {
    for (QueuedWrite write : writes) {
      if (!writeDirect(write)) {
        metricsFor(write.policy()).droppedCount.incrementAndGet();
        globalMetrics.droppedCount.incrementAndGet();
      }
    }
  }

  private boolean writeDirect(QueuedWrite write) {
    long startedAt = System.currentTimeMillis();
    long lagMs = Math.max(0L, startedAt - write.createdAtMs());
    WriterMetrics metrics = metricsFor(write.policy());
    try {
      DeviceLogWriteRecord record = write.record();
      if (record.requestWrite()) {
        write.service()
            .flushDeviceLogRequests(
                List.of(
                    new DeviceLogWriteRequest(
                        record.toRequest(), record.toDevice(), record.toProduct())));
      } else if (record.entityWrite()) {
        write.service()
            .flushDeviceLogEntities(
                List.of(
                    new DeviceLogEntityWrite(
                        record.toDeviceLog(), record.toDevice(), record.toProduct())));
      }
      recordFlushSuccess(metrics, 1, startedAt, lagMs);
      recordFlushSuccess(globalMetrics, 1, startedAt, lagMs);
      return true;
    } catch (Exception ex) {
      recordFlushFailure(metrics, 1, startedAt, lagMs);
      recordFlushFailure(globalMetrics, 1, startedAt, lagMs);
      log.warn(
          "Device log direct fallback failed policy={}, err={}", write.policy(), ex.getMessage());
      return false;
    }
  }

  private long maxLagMs(List<QueuedWrite> writes, long now) {
    long max = 0L;
    for (QueuedWrite write : writes) {
      max = Math.max(max, now - write.createdAtMs());
    }
    return Math.max(0L, max);
  }

  private void recordFlushSuccess(WriterMetrics metrics, int rows, long startedAt, long lagMs) {
    long durationMs = Math.max(0L, System.currentTimeMillis() - startedAt);
    metrics.insertedDeviceLogCount.addAndGet(rows);
    recordFlushCommon(metrics, rows, durationMs, lagMs);
  }

  private void recordFlushFailure(WriterMetrics metrics, int rows, long startedAt, long lagMs) {
    long durationMs = Math.max(0L, System.currentTimeMillis() - startedAt);
    metrics.failedDeviceLogCount.addAndGet(rows);
    recordFlushCommon(metrics, rows, durationMs, lagMs);
  }

  private void recordFlushCommon(WriterMetrics metrics, int rows, long durationMs, long lagMs) {
    metrics.lastDeviceLogFlushRows.set(rows);
    metrics.lastDeviceLogFlushDurationMs.set(durationMs);
    metrics.maxDeviceLogFlushDurationMs.updateAndGet(current -> Math.max(current, durationMs));
    metrics.lastDeviceLogQueueLagMs.set(lagMs);
    metrics.maxDeviceLogQueueLagMs.updateAndGet(current -> Math.max(current, lagMs));
    metrics.lastDeviceLogFlushAt.set(System.currentTimeMillis());
  }

  public Map<String, Object> metrics() {
    Map<String, Object> result = globalMetrics.snapshot();
    result.put("enabled", enabled);
    result.put("mode", mode.name().toLowerCase(Locale.ROOT));
    result.put("queueCapacity", queueCapacity);
    result.put("batchQueueDepth", queue.size());
    result.put("oldestDeviceLogQueueLagMs", currentQueueLagMs(null));
    result.put("batchSize", batchSize);
    result.put("batchFlushIntervalMs", flushIntervalMs);
    result.put("workerCount", workerCount);
    result.put("maxWorkerCount", maxWorkerCount);
    result.put("activeWorkers", activeWorkers.get());
    result.put("fallbackDirectEnabled", fallbackDirectEnabled);
    result.put("mqProvider", mqProvider);
    result.put("mqPublisherAvailable", mqPublisher != null);
    Map<String, Object> policies = new LinkedHashMap<>();
    policyMetrics.forEach((policy, metrics) -> policies.put(policy, policyMetrics(policy)));
    result.put("policies", policies);
    return result;
  }

  public Map<String, Object> policyMetrics(String policy) {
    String policyKey = policyKey(policy);
    Map<String, Object> result = metricsFor(policyKey).snapshot();
    result.put("enabled", enabled);
    result.put("mode", mode.name().toLowerCase(Locale.ROOT));
    result.put("batchQueueCapacity", queueCapacity);
    result.put("oldestDeviceLogQueueLagMs", currentQueueLagMs(policyKey));
    result.put("batchSize", batchSize);
    result.put("batchFlushIntervalMs", flushIntervalMs);
    result.put("workerCount", workerCount);
    result.put("maxWorkerCount", maxWorkerCount);
    result.put("activeWorkers", activeWorkers.get());
    result.put("fallbackDirectEnabled", fallbackDirectEnabled);
    result.put("mqProvider", mqProvider);
    result.put("mqPublisherAvailable", mqPublisher != null);
    return result;
  }

  private long currentQueueLagMs(String policy) {
    long now = System.currentTimeMillis();
    for (QueuedWrite write : queue) {
      if (policy == null || Objects.equals(policy, write.policy())) {
        return Math.max(0L, now - write.createdAtMs());
      }
    }
    return 0L;
  }

  @PreDestroy
  public void shutdown() {
    if (!running.compareAndSet(true, false)) {
      return;
    }
    executor.shutdown();
    if (!awaitExecutorTermination()) {
      log.warn(
          "Device log writer did not drain before timeout; forcing shutdown queueDepth={}, activeWorkers={}",
          queue.size(),
          activeWorkers.get());
      executor.shutdownNow();
    }
    while (!queue.isEmpty()) {
      flushSome(false);
    }
    if (!executor.isTerminated() && !awaitExecutorTermination()) {
      log.error(
          "Device log writer workers are still active after forced shutdown activeWorkers={}",
          activeWorkers.get());
    }
  }

  private boolean awaitExecutorTermination() {
    try {
      return executor.awaitTermination(shutdownTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  @Override
  public void start() {
    // Workers are started during construction so producers can submit before lifecycle callbacks.
  }

  @Override
  public void stop() {
    shutdown();
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE - 50;
  }

  private WriterMetrics metricsFor(String policy) {
    return policyMetrics.computeIfAbsent(policyKey(policy), ignored -> new WriterMetrics());
  }

  private String policyKey(String policy) {
    return StrUtil.blankToDefault(policy, "none").trim().toLowerCase(Locale.ROOT);
  }

  private record ServiceKey(String policy, IIoTDeviceLogService service) {
    private ServiceKey {
      policy = policy == null ? "none" : policy;
      Objects.requireNonNull(service, "service");
    }
  }

  private record QueuedWrite(
      String policy,
      IIoTDeviceLogService service,
      DeviceLogWriteRecord record,
      long createdAtMs) {

    static QueuedWrite request(
        String policy,
        IIoTDeviceLogService service,
        BaseUPRequest upRequest,
        IoTDeviceDTO ioTDeviceDTO,
        IoTProduct ioTProduct) {
      long now = System.currentTimeMillis();
      return new QueuedWrite(
          policy,
          service,
          DeviceLogWriteRecord.request(policy, upRequest, ioTDeviceDTO, ioTProduct, now),
          now);
    }

    static QueuedWrite entity(
        String policy,
        IIoTDeviceLogService service,
        IoTDeviceLog ioTDeviceLog,
        IoTDeviceDTO ioTDeviceDTO,
        IoTProduct ioTProduct) {
      long now = System.currentTimeMillis();
      return new QueuedWrite(
          policy,
          service,
          DeviceLogWriteRecord.entity(policy, ioTDeviceLog, ioTDeviceDTO, ioTProduct, now),
          now);
    }
  }

  private static final class WriterMetrics {
    private final AtomicLong acceptedCount = new AtomicLong();
    private final AtomicLong enqueuedDeviceLogCount = new AtomicLong();
    private final AtomicLong insertedDeviceLogCount = new AtomicLong();
    private final AtomicLong failedDeviceLogCount = new AtomicLong();
    private final AtomicLong queueFullCount = new AtomicLong();
    private final AtomicLong queueFullFallbackCount = new AtomicLong();
    private final AtomicLong fallbackDirectCount = new AtomicLong();
    private final AtomicLong droppedCount = new AtomicLong();
    private final AtomicLong pendingCount = new AtomicLong();
    private final AtomicLong publisherSuccessCount = new AtomicLong();
    private final AtomicLong publisherFailureCount = new AtomicLong();
    private final AtomicLong lastDeviceLogFlushRows = new AtomicLong();
    private final AtomicLong lastDeviceLogFlushDurationMs = new AtomicLong();
    private final AtomicLong maxDeviceLogFlushDurationMs = new AtomicLong();
    private final AtomicLong lastDeviceLogQueueLagMs = new AtomicLong();
    private final AtomicLong maxDeviceLogQueueLagMs = new AtomicLong();
    private final AtomicLong lastDeviceLogFlushAt = new AtomicLong();

    Map<String, Object> snapshot() {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("acceptedCount", acceptedCount.get());
      result.put("batchQueueDepth", Math.max(0L, pendingCount.get()));
      result.put("metadataBatchQueueDepth", 0L);
      result.put("oldestDeviceLogQueueLagMs", 0L);
      result.put("oldestMetadataQueueLagMs", 0L);
      result.put("enqueuedDeviceLogCount", enqueuedDeviceLogCount.get());
      result.put("insertedDeviceLogCount", insertedDeviceLogCount.get());
      result.put("failedDeviceLogCount", failedDeviceLogCount.get());
      result.put("queueFullCount", queueFullCount.get());
      result.put("queueFullFallbackCount", queueFullFallbackCount.get());
      result.put("fallbackDirectCount", fallbackDirectCount.get());
      result.put("droppedCount", droppedCount.get());
      result.put("enqueuedDeviceLogMetadataCount", 0L);
      result.put("insertedDeviceLogMetadataCount", 0L);
      result.put("failedDeviceLogMetadataCount", 0L);
      result.put("metadataQueueFullFallbackCount", 0L);
      result.put("publisherSuccessCount", publisherSuccessCount.get());
      result.put("publisherFailureCount", publisherFailureCount.get());
      result.put("lastDeviceLogFlushRows", lastDeviceLogFlushRows.get());
      result.put("lastDeviceLogFlushDurationMs", lastDeviceLogFlushDurationMs.get());
      result.put("maxDeviceLogFlushDurationMs", maxDeviceLogFlushDurationMs.get());
      result.put("lastDeviceLogQueueDepthBeforeFlush", 0L);
      result.put("lastDeviceLogQueueLagMs", lastDeviceLogQueueLagMs.get());
      result.put("maxDeviceLogQueueLagMs", maxDeviceLogQueueLagMs.get());
      result.put("lastDeviceLogFlushAt", lastDeviceLogFlushAt.get());
      result.put("lastMetadataFlushRows", 0L);
      result.put("lastMetadataFlushDurationMs", 0L);
      result.put("maxMetadataFlushDurationMs", 0L);
      result.put("lastMetadataQueueDepthBeforeFlush", 0L);
      result.put("lastMetadataQueueLagMs", 0L);
      result.put("maxMetadataQueueLagMs", 0L);
      result.put("lastMetadataFlushAt", 0L);
      return result;
    }
  }
}
