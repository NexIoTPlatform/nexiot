package cn.universal.http.access.runtime;

import cn.universal.http.access.client.HttpInteractionExecutor;
import cn.universal.http.access.client.HttpInteractionResult;
import cn.universal.http.access.config.HttpConfigParser;
import cn.universal.http.access.model.HttpDriverContext;
import cn.universal.http.access.model.HttpNormalizedBatch;
import cn.universal.http.access.pipeline.HttpUpstreamService;
import cn.universal.persistence.entity.IoTProduct;
import java.time.ZoneId;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * Schedules optional driver polling. The schedule lives with product configuration; request details
 * still come exclusively from the driver contract.
 */
@Slf4j
@Component
public class HttpDriverPollingScheduler {

  private final ThreadPoolTaskScheduler scheduler;
  private final ThreadPoolTaskExecutor worker;
  private final HttpConfigParser parser;
  private final HttpInteractionExecutor interactionExecutor;
  private final HttpUpstreamService upstreamService;
  private final RedissonClient redissonClient;
  private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
  private final Map<Long, PollTarget> targets = new ConcurrentHashMap<>();
  private final Map<Long, AtomicBoolean> localRunning = new ConcurrentHashMap<>();

  public HttpDriverPollingScheduler(
      @Qualifier("httpAccessTaskScheduler") ThreadPoolTaskScheduler scheduler,
      @Qualifier("httpAccessWorkerExecutor") ThreadPoolTaskExecutor worker,
      HttpConfigParser parser,
      HttpInteractionExecutor interactionExecutor,
      HttpUpstreamService upstreamService,
      ObjectProvider<RedissonClient> redissonClient) {
    this.scheduler = scheduler;
    this.worker = worker;
    this.parser = parser;
    this.interactionExecutor = interactionExecutor;
    this.upstreamService = upstreamService;
    this.redissonClient = redissonClient.getIfAvailable();
  }

  public synchronized void reschedule(HttpRouteSnapshot snapshot, IoTProduct product) {
    cancel(snapshot.definition().id());
    HttpDriverPollingConfig config = HttpDriverPollingConfig.from(product.getConfiguration(), parser);
    if (!config.enabled()) {
      return;
    }
    long networkId = snapshot.definition().id();
    targets.put(networkId, new PollTarget(snapshot, product.getProductKey(), config));
    Runnable trigger = () -> worker.execute(() -> run(networkId));
    ScheduledFuture<?> future = config.cron() == null
        ? scheduler.scheduleWithFixedDelay(trigger,
            Instant.now().plusMillis(config.initialDelayMs()),
            Duration.ofMillis(config.fixedDelayMs()))
        : scheduler.schedule(trigger, new CronTrigger(config.cron(), config.zone()));
    futures.put(networkId, future);
    log.info("HTTP driver polling scheduled: networkId={}, productKey={}, schedule={}", networkId,
        product.getProductKey(), config.cron() == null ? config.fixedDelayMs() + "ms" : config.cron());
  }

  public synchronized void cancel(Long networkId) {
    ScheduledFuture<?> future = futures.remove(networkId);
    if (future != null) {
      future.cancel(false);
    }
    targets.remove(networkId);
    localRunning.remove(networkId);
  }

  public boolean isConfigured(Long networkId) {
    return targets.containsKey(networkId);
  }

  public void runNow(Long networkId) {
    if (!targets.containsKey(networkId)) {
      throw new IllegalArgumentException("The bound product has not enabled httpAccess.poll");
    }
    worker.execute(() -> run(networkId));
  }

  private void run(Long networkId) {
    PollTarget target = targets.get(networkId);
    if (target == null) {
      return;
    }
    AtomicBoolean running = localRunning.computeIfAbsent(networkId, ignored -> new AtomicBoolean());
    if (!running.compareAndSet(false, true)) {
      return;
    }
    RLock lock = redissonClient == null ? null : redissonClient.getLock("http:driver-poll:" + networkId);
    boolean locked = false;
    try {
      if (lock != null) {
        locked = lock.tryLock(0, Math.max(target.config.fixedDelayMs() * 2, 60000), TimeUnit.MILLISECONDS);
        if (!locked) {
          return;
        }
      }
      HttpDriverContext context = HttpDriverContext.builder()
          .networkId(networkId)
          .networkUnionId(target.snapshot.definition().unionId())
          .accessCode(target.snapshot.config().getAccessCode())
          .operationType("POLL")
          .source("POLL")
          .productKey(target.productKey)
          .attributes(new LinkedHashMap<>(Map.of("trigger", "SCHEDULED")))
          .build();
      HttpInteractionResult interaction = interactionExecutor.execute(
          target.snapshot, target.productKey, "{}", context);
      HttpNormalizedBatch batch = parseBatch(interaction.normalizedPayload());
      upstreamService.processPoll(target.snapshot, batch.getRecords(), context);
    } catch (Exception e) {
      log.error("HTTP driver poll failed: networkId={}, productKey={}", networkId, target.productKey, e);
    } finally {
      if (locked && lock != null && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
      running.set(false);
    }
  }

  private HttpNormalizedBatch parseBatch(String value) throws Exception {
    if (value == null || value.isBlank()) {
      return new HttpNormalizedBatch();
    }
    if (value.trim().startsWith("[")) {
      HttpNormalizedBatch batch = new HttpNormalizedBatch();
      batch.setRecords(parser.mapper().readerForListOf(HttpNormalizedBatch.HttpNormalizedRecord.class)
          .readValue(value));
      return batch;
    }
    return parser.mapper().readValue(value, HttpNormalizedBatch.class);
  }

  private record PollTarget(
      HttpRouteSnapshot snapshot, String productKey, HttpDriverPollingConfig config) {}
}
