/*
 *
 * Copyright (c) 2025, IoT-Universal. All Rights Reserved.
 *
 * @Description: 本文件由 Aleo 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: Aleo
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.platform.scheduler;

import cn.universal.admin.platform.service.IGatewayPollingService;
import cn.universal.common.constant.IoTConstant;
import cn.universal.persistence.entity.GatewayPollingConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 网关轮询定时任务调度器
 *
 * <p>使用固定轮询间隔(30s, 60s, 120s, 300s, 600s)，通过分布式锁保证集群环境下只有一个节点执行
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "gateway.polling",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class GatewayPollingScheduler {

  private static final String TASK_TYPE_CLOUD_POLLING = "CLOUD_POLLING";
  private static final String TASK_TYPE_MODBUS_COLLECT = "MODBUS_COLLECT";

  @Autowired private IGatewayPollingService gatewayPollingService;

  @Autowired private RedissonClient redissonClient;

  @Autowired(required = false)
  @Qualifier("namedVirtualThreadExecutor")
  private ExecutorService pollingExecutor;

  @Value("${gateway.polling.max-concurrency:64}")
  private int maxConcurrency;

  @Value("${gateway.polling.shutdown-timeout-ms:10000}")
  private long shutdownTimeoutMs;

  private final AtomicBoolean shutdownStarted = new AtomicBoolean();
  private final AtomicInteger activeRuns = new AtomicInteger();
  private final Set<Future<?>> inFlightTasks = ConcurrentHashMap.newKeySet();
  private final Object shutdownMonitor = new Object();
  private volatile boolean ownsPollingExecutor;

  /** 30秒轮询任务 */
  @Scheduled(cron = "0/30 * * * * ?")
  public void polling30s() {
    String traceId = generateTraceId("polling-30s");
    MDC.put(IoTConstant.TRACE_ID, traceId);
    try {
      executePollingWithLock(30);
    } finally {
      MDC.remove(IoTConstant.TRACE_ID);
    }
  }

  /** 60秒轮询任务 */
  @Scheduled(cron = "0 0/1 * * * ?")
  public void polling60s() {
    String traceId = generateTraceId("polling-60s");
    MDC.put(IoTConstant.TRACE_ID, traceId);
    try {
      executePollingWithLock(60);
    } finally {
      MDC.remove(IoTConstant.TRACE_ID);
    }
  }

  /** 120秒轮询任务 */
  @Scheduled(cron = "0 0/2 * * * ?")
  public void polling120s() {
    String traceId = generateTraceId("polling-120s");
    MDC.put(IoTConstant.TRACE_ID, traceId);
    try {
      executePollingWithLock(120);
    } finally {
      MDC.remove(IoTConstant.TRACE_ID);
    }
  }

  /** 300秒轮询任务 (5分钟) */
  @Scheduled(cron = "0 0/5 * * * ?")
  public void polling300s() {
    String traceId = generateTraceId("polling-300s");
    MDC.put(IoTConstant.TRACE_ID, traceId);
    try {
      executePollingWithLock(300);
    } finally {
      MDC.remove(IoTConstant.TRACE_ID);
    }
  }

  /** 600秒轮询任务 (10分钟) */
  @Scheduled(cron = "0 0/10 * * * ?")
  public void polling600s() {
    String traceId = generateTraceId("polling-600s");
    MDC.put(IoTConstant.TRACE_ID, traceId);
    try {
      executePollingWithLock(600);
    } finally {
      MDC.remove(IoTConstant.TRACE_ID);
    }
  }

  /** 生成 traceId 格式: {prefix}-{timestamp}-{random} 示例: polling-30s-1699344000-a1b2c3 */
  private String generateTraceId(String prefix) {
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    String random = UUID.randomUUID().toString().substring(0, 6);
    return String.format("%s-%s-%s", prefix, timestamp, random);
  }

  /**
   * 使用分布式锁执行轮询任务
   *
   * @param intervalSeconds 轮询间隔(秒)
   */
  private void executePollingWithLock(int intervalSeconds) {
    executePollingWithLock(intervalSeconds, TASK_TYPE_CLOUD_POLLING);
    executePollingWithLock(intervalSeconds, TASK_TYPE_MODBUS_COLLECT);
  }

  private void executePollingWithLock(int intervalSeconds, String taskType) {
    if (!beginRun()) {
      return;
    }
    String lockKey = "gateway:polling:lock:" + taskType + ":" + intervalSeconds;
    RLock lock = null;

    // 打印线程信息用于验证虚拟线程
    Thread currentThread = Thread.currentThread();
    log.debug(
        "当前线程: name={}, isVirtual={}, id={}",
        currentThread.getName(),
        currentThread.isVirtual(),
        currentThread.threadId());

    try {
      lock = redissonClient.getLock(lockKey);
      // 使用 Redisson watchdog 自动续期，避免高频采集耗时超过固定租约后被下一轮重复执行。
      boolean acquired = lock.tryLock(0, TimeUnit.SECONDS);

      if (acquired) {
        log.debug("✓ 节点获得 {}s {} 轮询任务锁，开始执行", intervalSeconds, taskType);
        executePollingTask(intervalSeconds, taskType);
      } else {
        log.debug("✗ 节点未获得 {}s {} 轮询任务锁，跳过执行", intervalSeconds, taskType);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("获取轮询任务锁被中断: intervalSeconds={}, taskType={}", intervalSeconds, taskType);
    } catch (Exception e) {
      log.error("执行轮询任务异常: intervalSeconds={}, taskType={}", intervalSeconds, taskType, e);
    } finally {
      try {
        if (lock != null && lock.isHeldByCurrentThread()) {
          lock.unlock();
        }
      } catch (RuntimeException ex) {
        if (!shutdownStarted.get()) {
          log.warn("释放网关轮询锁失败: lockKey={}, err={}", lockKey, ex.getMessage());
        }
      } finally {
        endRun();
      }
    }
  }

  /**
   * 执行轮询任务
   *
   * @param intervalSeconds 轮询间隔(秒)
   */
  private void executePollingTask(int intervalSeconds, String taskType) {
    long startTime = System.currentTimeMillis();

    // 1. 查询待轮询的网关设备
    List<GatewayPollingConfig> configs =
        gatewayPollingService.getDuePollingDevices(intervalSeconds, taskType);

    if (configs == null || configs.isEmpty()) {
      log.debug("本次{}s {}轮询无待执行设备", intervalSeconds, taskType);
      return;
    }

    log.debug("本次{}s {}轮询共 {} 个网关设备", intervalSeconds, taskType, configs.size());

    // 2. 并发执行轮询。单个网关内部仍由 GatewayPollingService 保持指令顺序。
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failCount = new AtomicInteger();
    Semaphore semaphore = new Semaphore(Math.max(1, maxConcurrency));
    ExecutorService executor = resolvePollingExecutor();
    List<Future<?>> futures = new ArrayList<>(configs.size());

    for (GatewayPollingConfig config : configs) {
      if (shutdownStarted.get()) {
        break;
      }
      try {
        Future<?> future =
            executor.submit(() -> pollOneGateway(config, semaphore, successCount, failCount));
        inFlightTasks.add(future);
        if (shutdownStarted.get()) {
          future.cancel(true);
        }
        futures.add(future);
      } catch (RejectedExecutionException ex) {
        if (!shutdownStarted.get()) {
          log.warn("提交网关轮询任务失败: deviceId={}, err={}", config.getDeviceId(), ex.getMessage());
        }
        break;
      }
    }

    waitForPollingTasks(futures);

    long costTime = System.currentTimeMillis() - startTime;
    log.debug(
        "{}s {}轮询任务完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
        intervalSeconds,
        taskType,
        configs.size(),
        successCount.get(),
        failCount.get(),
        costTime);
  }

  private void pollOneGateway(
      GatewayPollingConfig config,
      Semaphore semaphore,
      AtomicInteger successCount,
      AtomicInteger failCount) {
    boolean acquired = false;
    try {
      semaphore.acquire();
      acquired = true;
      gatewayPollingService.pollGatewayDevice(config);
      successCount.incrementAndGet();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      failCount.incrementAndGet();
      log.warn("轮询网关设备被中断: deviceId={}", config.getDeviceId());
    } catch (Exception e) {
      failCount.incrementAndGet();
      log.error("轮询网关设备失败: deviceId={}", config.getDeviceId(), e);
    } finally {
      if (acquired) {
        semaphore.release();
      }
    }
  }

  private void waitForPollingTasks(List<Future<?>> futures) {
    try {
      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (CancellationException ignored) {
          // Context shutdown cancels outstanding polling tasks.
        } catch (InterruptedException ex) {
          futures.forEach(task -> task.cancel(true));
          Thread.currentThread().interrupt();
          return;
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause();
          log.warn("等待网关轮询任务失败: err={}", cause == null ? ex.getMessage() : cause.getMessage());
        }
      }
    } finally {
      inFlightTasks.removeAll(futures);
    }
  }

  private synchronized ExecutorService resolvePollingExecutor() {
    if (pollingExecutor != null) {
      return pollingExecutor;
    }
    pollingExecutor =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("GatewayPolling-", 0).factory());
    ownsPollingExecutor = true;
    return pollingExecutor;
  }

  private boolean beginRun() {
    if (shutdownStarted.get()) {
      return false;
    }
    activeRuns.incrementAndGet();
    if (shutdownStarted.get()) {
      endRun();
      return false;
    }
    return true;
  }

  private void endRun() {
    if (activeRuns.decrementAndGet() == 0) {
      synchronized (shutdownMonitor) {
        shutdownMonitor.notifyAll();
      }
    }
  }

  @EventListener(ContextClosedEvent.class)
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public void onContextClosed(ContextClosedEvent event) {
    if (!shutdownStarted.compareAndSet(false, true)) {
      return;
    }
    inFlightTasks.forEach(task -> task.cancel(true));
    ExecutorService executor = pollingExecutor;
    if (ownsPollingExecutor && executor != null) {
      executor.shutdownNow();
    }
    awaitActiveRuns();
  }

  private void awaitActiveRuns() {
    long timeoutMs = Math.max(1000L, shutdownTimeoutMs);
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
    synchronized (shutdownMonitor) {
      while (activeRuns.get() > 0) {
        long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0L) {
          log.warn("等待网关轮询任务关闭超时 activeRuns={}", activeRuns.get());
          return;
        }
        try {
          TimeUnit.NANOSECONDS.timedWait(shutdownMonitor, remainingNanos);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }
}
