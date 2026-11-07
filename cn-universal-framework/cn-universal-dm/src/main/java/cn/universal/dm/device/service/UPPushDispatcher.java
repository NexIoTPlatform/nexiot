package cn.universal.dm.device.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Shared bounded dispatcher for best-effort application push. */
@Slf4j
@Component
public class UPPushDispatcher {

  private final int stripeCount;
  private final long shutdownTimeoutMs;
  private final long maxQueueDelayNanos;
  private final List<BlockingQueue<PushTask>> queues;
  private final ExecutorService workers;
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final Counter acceptedTasks;
  private final Counter droppedTasks;
  private final Counter droppedMessages;
  private final Counter failedTasks;
  private final Timer queueLag;
  private final Timer executionTime;

  public UPPushDispatcher(
      MeterRegistry meterRegistry,
      @Value("${nexiot.push.dispatcher.stripes:64}") int stripes,
      @Value("${nexiot.push.dispatcher.queue-capacity:4096}") int queueCapacity,
      @Value("${nexiot.push.dispatcher.max-queue-delay-ms:2000}") long maxQueueDelayMs,
      @Value("${nexiot.push.dispatcher.shutdown-timeout-ms:10000}") long shutdownTimeoutMs) {
    int normalizedQueueCapacity = Math.max(1, queueCapacity);
    this.stripeCount = Math.max(1, Math.min(stripes, normalizedQueueCapacity));
    this.shutdownTimeoutMs = Math.max(0L, shutdownTimeoutMs);
    this.maxQueueDelayNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, maxQueueDelayMs));
    int stripeCapacity =
        Math.max(1, (int) Math.ceil((double) normalizedQueueCapacity / stripeCount));
    this.queues = new ArrayList<>(stripeCount);
    for (int i = 0; i < stripeCount; i++) {
      queues.add(new ArrayBlockingQueue<>(stripeCapacity));
    }
    this.workers =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("up-push-", 0).factory());
    this.acceptedTasks =
        Counter.builder("nexiot.push.dispatcher.accepted.tasks").register(meterRegistry);
    this.droppedTasks =
        Counter.builder("nexiot.push.dispatcher.dropped.tasks").register(meterRegistry);
    this.droppedMessages =
        Counter.builder("nexiot.push.dispatcher.dropped.messages").register(meterRegistry);
    this.failedTasks =
        Counter.builder("nexiot.push.dispatcher.failed.tasks").register(meterRegistry);
    this.queueLag =
        Timer.builder("nexiot.push.dispatcher.queue.lag").register(meterRegistry);
    this.executionTime =
        Timer.builder("nexiot.push.dispatcher.execution").register(meterRegistry);
    Gauge.builder(
            "nexiot.push.dispatcher.queue.depth",
            queues,
            values -> values.stream().mapToInt(BlockingQueue::size).sum())
        .register(meterRegistry);
  }

  @PostConstruct
  public void start() {
    for (int stripe = 0; stripe < stripeCount; stripe++) {
      int index = stripe;
      workers.execute(() -> workerLoop(index));
    }
  }

  /**
   * Attempts a non-blocking enqueue. A full queue drops the task by design so device ingestion is
   * never back-pressured by an unavailable application endpoint.
   */
  public boolean dispatch(String orderingKey, int messageCount, Runnable runnable) {
    if (!running.get() || runnable == null) {
      recordDrop(messageCount);
      return false;
    }
    int stripe = stripeIndex(orderingKey);
    PushTask task =
        new PushTask(
            runnable, Math.max(1, messageCount), System.nanoTime(), MDC.getCopyOfContextMap());
    if (queues.get(stripe).offer(task)) {
      acceptedTasks.increment();
      return true;
    }
    recordDrop(messageCount);
    return false;
  }

  boolean canAccept(String orderingKey) {
    return running.get() && queues.get(stripeIndex(orderingKey)).remainingCapacity() > 0;
  }

  void recordRejected(int messageCount) {
    recordDrop(messageCount);
  }

  private int stripeIndex(String orderingKey) {
    return Math.floorMod(orderingKey == null ? 0 : orderingKey.hashCode(), stripeCount);
  }

  private void recordDrop(int messageCount) {
    droppedTasks.increment();
    droppedMessages.increment(Math.max(1, messageCount));
  }

  private void workerLoop(int stripe) {
    BlockingQueue<PushTask> queue = queues.get(stripe);
    while (running.get() || !queue.isEmpty()) {
      try {
        PushTask task = queue.poll(200, TimeUnit.MILLISECONDS);
        if (task == null) {
          continue;
        }
        long lagNanos = System.nanoTime() - task.enqueuedAtNanos();
        queueLag.record(Duration.ofNanos(lagNanos));
        if (maxQueueDelayNanos > 0L && lagNanos > maxQueueDelayNanos) {
          recordDrop(task.messageCount());
          continue;
        }
        executeTask(stripe, task);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception ex) {
        failedTasks.increment();
        log.error("Application push worker failed, stripe={}", stripe, ex);
      }
    }
  }

  private void executeTask(int stripe, PushTask task) {
    Map<String, String> previous = MDC.getCopyOfContextMap();
    try {
      if (task.mdc() == null || task.mdc().isEmpty()) {
        MDC.clear();
      } else {
        MDC.setContextMap(task.mdc());
      }
      executionTime.record(task.runnable());
    } catch (Exception ex) {
      failedTasks.increment();
      log.error("Application push task failed, stripe={}", stripe, ex);
    } finally {
      if (previous == null || previous.isEmpty()) {
        MDC.clear();
      } else {
        MDC.setContextMap(previous);
      }
    }
  }

  @PreDestroy
  public void shutdown() {
    running.set(false);
    workers.shutdown();
    try {
      if (!workers.awaitTermination(shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
        List<PushTask> discardedTasks = new ArrayList<>();
        queues.forEach(queue -> queue.drainTo(discardedTasks));
        workers.shutdownNow();
        if (!discardedTasks.isEmpty()) {
          droppedTasks.increment(discardedTasks.size());
          droppedMessages.increment(
              discardedTasks.stream().mapToInt(PushTask::messageCount).sum());
          log.warn("Discarded {} application push tasks during shutdown", discardedTasks.size());
        }
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      workers.shutdownNow();
    }
  }

  int queueDepth() {
    return queues.stream().mapToInt(BlockingQueue::size).sum();
  }

  private record PushTask(
      Runnable runnable, int messageCount, long enqueuedAtNanos, Map<String, String> mdc) {}
}
