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

package cn.universal.web.config;

import cn.universal.common.constant.IoTConstant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 虚拟线程配置 - Spring Boot 3.5 + JDK 21 新特性
 *
 * <p>虚拟线程优势：
 *
 * <ul>
 *   <li>轻量级：可创建数百万个虚拟线程
 *   <li>高性能：I/O密集型任务性能大幅提升
 *   <li>简化编程：无需手动管理线程池
 *   <li>适合IoT场景：大量并发连接处理
 * </ul>
 *
 * <p><b>注意：</b>@EnableAsync 已在主启动类中全局启用，此处无需重复配置
 *
 * <p><b>定时任务配置：</b>实现 SchedulingConfigurer 接口，全局配置 @Scheduled 注解的线程池
 */
@Slf4j
@Configuration
public class VirtualThreadConfig implements SchedulingConfigurer {

  /** 主要虚拟线程执行器 - 用于一般异步任务 */
  @Bean("virtualThreadExecutor")
  @Primary
  public ExecutorService virtualThreadExecutor(
      @Value("${nexiot.async.virtual-thread.background-max-concurrency:16}") int maxConcurrency) {
    ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    return new VirtualThreadContextExecutor(
        virtualExecutor, maxConcurrency, "virtualThreadExecutor", true);
  }

  /** DB 密集型后台执行器 - 规则/场景/公共属性/桥接等共享小预算，避免挤占 Web/Quartz 连接 */
  @Bean("dbBoundTaskExecutor")
  public ExecutorService dbBoundTaskExecutor(
      @Value("${nexiot.async.db-bound.max-concurrency:6}") int maxConcurrency) {
    ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    return new VirtualThreadContextExecutor(
        virtualExecutor, maxConcurrency, "dbBoundTaskExecutor", true);
  }

  /** Uplink side effects have independent bounded queues; saturation never runs on the caller. */
  @Bean("sceneTaskExecutor")
  public ExecutorService sceneTaskExecutor(
      @Value("${nexiot.scene.max-concurrency:8}") int maxConcurrency) {
    return uplinkExecutor("scene", maxConcurrency, 256);
  }

  @Bean("legacyRuleTaskExecutor")
  public ExecutorService legacyRuleTaskExecutor(
      @Value("${nexiot.rule.max-concurrency:6}") int maxConcurrency) {
    return uplinkExecutor("legacy-rule", maxConcurrency, 256);
  }

  @Bean("deviceAttributeTaskExecutor")
  public ExecutorService deviceAttributeTaskExecutor(
      @Value("${nexiot.uplink.attributes.max-concurrency:4}") int maxConcurrency) {
    return uplinkExecutor("device-attribute", maxConcurrency, 256);
  }

  static ExecutorService uplinkExecutor(String name, int concurrency, int capacity) {
    int size = Math.max(1, concurrency);
    return new java.util.concurrent.ThreadPoolExecutor(
        size,
        size,
        0,
        java.util.concurrent.TimeUnit.MILLISECONDS,
        new java.util.concurrent.ArrayBlockingQueue<>(capacity),
        Thread.ofVirtual().name(name + "-", 0).factory(),
        new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
  }

  /** 命名虚拟线程执行器 - 用于需要特定命名的任务 */
  @Bean("namedVirtualThreadExecutor")
  public ExecutorService namedVirtualThreadExecutor() {
    ThreadFactory factory = Thread.ofVirtual().name("IoT-vt-", 0).factory();
    return Executors.newThreadPerTaskExecutor(factory);
  }

  /** 虚拟线程调度器 - 用于定时任务 */
  @Bean("virtualScheduledExecutor")
  public ScheduledExecutorService virtualScheduledExecutor() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(10);
    scheduler.setThreadNamePrefix("IoT-scheduled-vt-");
    scheduler.setVirtualThreads(true); // 启用虚拟线程
    scheduler.setTaskDecorator(new VirtualThreadTaskDecorator());
    scheduler.initialize();
    return scheduler.getScheduledExecutor();
  }

  /** 默认异步执行器 - 支持上下文传递 */
  @Bean("taskExecutor")
  public Executor taskExecutor(
      @Value("${nexiot.async.virtual-thread.max-concurrency:32}") int maxConcurrency) {
    ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    return new VirtualThreadContextExecutor(virtualExecutor, maxConcurrency, "taskExecutor", true);
  }

  /**
   * 全局配置 @Scheduled 定时任务的线程池
   *
   * <p>重要：这个配置会覆盖 Spring 默认的单线程调度器， 让所有 @Scheduled 注解的任务都使用虚拟线程执行
   *
   * <p>优势：
   *
   * <ul>
   *   <li>并发执行：多个定时任务不会互相阻塞
   *   <li>虚拟线程：轻量级，可支持大量并发任务
   *   <li>上下文传递：自动传递 MDC
   * </ul>
   */
  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(20); // 核心线程数，根据定时任务数量调整
    scheduler.setThreadNamePrefix("Scheduled-VT-");
    scheduler.setVirtualThreads(true); // 启用虚拟线程
    scheduler.setTaskDecorator(new VirtualThreadTaskDecorator());
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(60);
    scheduler.initialize();

    taskRegistrar.setTaskScheduler(scheduler);
    log.info("✓ 已配置 @Scheduled 全局虚拟线程调度器: poolSize=20, virtualThreads=true");
  }

  /** 虚拟线程任务装饰器 - 仅传递MDC，避免后台任务误用失效的请求上下文 */
  public static class VirtualThreadTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
      String traceId = MDC.get("traceId");

      return () -> {
        try {
          // 传递MDC上下文
          if (traceId != null) {
            MDC.put("traceId", traceId);
          }
          runnable.run();
        } finally {
          MDC.clear();
        }
      };
    }
  }

  /** 虚拟线程上下文执行器 - 包装虚拟线程执行器 */
  public static class VirtualThreadContextExecutor extends AbstractExecutorService {

    private final ExecutorService delegate;
    private final Semaphore permits;
    private final int maxConcurrency;
    private final String executorName;
    private final boolean waitForPermitOnSaturation;
    private final ThreadLocal<Boolean> runningInExecutor =
        ThreadLocal.withInitial(() -> Boolean.FALSE);
    private final AtomicLong callerRunsCount = new AtomicLong();
    private volatile boolean shutdown;

    public VirtualThreadContextExecutor(ExecutorService delegate) {
      this(delegate, 32, "virtualThreadExecutor");
    }

    public VirtualThreadContextExecutor(ExecutorService delegate, int maxConcurrency) {
      this(delegate, maxConcurrency, "virtualThreadExecutor");
    }

    public VirtualThreadContextExecutor(
        ExecutorService delegate, int maxConcurrency, String executorName) {
      this(delegate, maxConcurrency, executorName, false);
    }

    public VirtualThreadContextExecutor(
        ExecutorService delegate,
        int maxConcurrency,
        String executorName,
        boolean waitForPermitOnSaturation) {
      this.delegate = delegate;
      this.maxConcurrency = Math.max(1, maxConcurrency);
      this.executorName =
          executorName == null || executorName.isBlank() ? "virtualThreadExecutor" : executorName;
      this.waitForPermitOnSaturation = waitForPermitOnSaturation;
      this.permits = new Semaphore(this.maxConcurrency);
    }

    @Override
    public void execute(Runnable command) {
      if (shutdown) {
        throw new java.util.concurrent.RejectedExecutionException(executorName + " is shut down");
      }
      String traceId = MDC.get(IoTConstant.TRACE_ID);
      SecurityContext securityContext = SecurityContextHolder.getContext();
      Runnable wrapped = () -> runWithContext(command, traceId, securityContext);
      if (Boolean.TRUE.equals(runningInExecutor.get())) {
        wrapped.run();
        return;
      }

      boolean permitAcquired = false;
      if (waitForPermitOnSaturation) {
        try {
          if (!permits.tryAcquire()) {
            long count = callerRunsCount.incrementAndGet();
            if (count == 1 || count % 1000 == 0) {
              log.warn(
                  "{} concurrency saturated, waiting for permit maxConcurrency={}, waitCount={}",
                  executorName,
                  maxConcurrency,
                  count);
            }
            permits.acquire();
          }
          permitAcquired = true;
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new java.util.concurrent.RejectedExecutionException(
              executorName + " interrupted while waiting for permit", ex);
        }
      } else if (!permits.tryAcquire()) {
        long count = callerRunsCount.incrementAndGet();
        if (count == 1 || count % 1000 == 0) {
          log.warn(
              "{} concurrency saturated, running task in caller thread maxConcurrency={},"
                  + " callerRunsCount={}",
              executorName,
              maxConcurrency,
              count);
        }
        wrapped.run();
        return;
      } else {
        permitAcquired = true;
      }

      try {
        delegate.execute(
            () -> {
              try {
                runInExecutorContext(wrapped);
              } finally {
                permits.release();
              }
            });
      } catch (RuntimeException ex) {
        if (permitAcquired) {
          permits.release();
        }
        throw ex;
      }
    }

    private void runInExecutorContext(Runnable task) {
      boolean previous = Boolean.TRUE.equals(runningInExecutor.get());
      runningInExecutor.set(Boolean.TRUE);
      try {
        task.run();
      } finally {
        runningInExecutor.set(previous);
      }
    }

    @Override
    public void shutdown() {
      shutdown = true;
      delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
      shutdown = true;
      return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
      return shutdown || delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
      return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit)
        throws InterruptedException {
      return delegate.awaitTermination(timeout, unit);
    }

    private void runWithContext(Runnable command, String traceId, SecurityContext securityContext) {
      Map<String, String> previousMdc = MDC.getCopyOfContextMap();
      SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
      try {
        if (traceId != null) {
          MDC.put(IoTConstant.TRACE_ID, traceId);
        }
        if (securityContext != null && securityContext.getAuthentication() != null) {
          SecurityContextHolder.setContext(securityContext);
        }
        command.run();
      } finally {
        if (previousMdc == null || previousMdc.isEmpty()) {
          MDC.clear();
        } else {
          MDC.setContextMap(previousMdc);
        }
        if (previousSecurityContext == null) {
          SecurityContextHolder.clearContext();
        } else {
          SecurityContextHolder.setContext(previousSecurityContext);
        }
      }
    }
  }
}
