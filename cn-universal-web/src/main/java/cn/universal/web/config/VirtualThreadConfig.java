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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 虚拟线程配置 - Spring Boot 3.5 + JDK 25 新特性
 *
 * <p>虚拟线程优势：
 * <ul>
 *   <li>轻量级：可创建数百万个虚拟线程</li>
 *   <li>高性能：I/O密集型任务性能大幅提升</li>
 *   <li>简化编程：无需手动管理线程池</li>
 *   <li>适合IoT场景：大量并发连接处理</li>
 * </ul>
 *
 * <p><b>注意：</b>@EnableAsync 已在主启动类中全局启用，此处无需重复配置</p>
 *
 * <p><b>定时任务配置：</b>实现 SchedulingConfigurer 接口，全局配置 @Scheduled 注解的线程池</p>
 */
@Slf4j
@Configuration
public class VirtualThreadConfig implements SchedulingConfigurer {

  /** 主要虚拟线程执行器 - 用于一般异步任务 */
  @Bean("virtualThreadExecutor")
  @Primary
  public ExecutorService virtualThreadExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
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
  public Executor taskExecutor() {
    ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    return new VirtualThreadContextExecutor(virtualExecutor);
  }

  /**
   * 全局配置 @Scheduled 定时任务的线程池
   *
   * <p>重要：这个配置会覆盖 Spring 默认的单线程调度器，
   * 让所有 @Scheduled 注解的任务都使用虚拟线程执行</p>
   *
   * <p>优势：
   * <ul>
   *   <li>并发执行：多个定时任务不会互相阻塞</li>
   *   <li>虚拟线程：轻量级，可支持大量并发任务</li>
   *   <li>上下文传递：自动传递 MDC 和请求上下文</li>
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

  /** 虚拟线程任务装饰器 - 传递MDC和请求上下文 */
  public static class VirtualThreadTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
      String traceId = MDC.get("traceId");
      RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

      return () -> {
        try {
          // 传递MDC上下文
          if (traceId != null) {
            MDC.put("traceId", traceId);
          }
          // 传递请求上下文
          if (requestAttributes != null) {
            RequestContextHolder.setRequestAttributes(requestAttributes);
          }
          runnable.run();
        } finally {
          MDC.clear();
          RequestContextHolder.resetRequestAttributes();
        }
      };
    }
  }

  /** 虚拟线程上下文执行器 - 包装虚拟线程执行器 */
  public static class VirtualThreadContextExecutor implements Executor {

    private final ExecutorService delegate;

    public VirtualThreadContextExecutor(ExecutorService delegate) {
      this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
      String traceId = MDC.get(IoTConstant.TRACE_ID);
      RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

      delegate.execute(
          () -> {
            try {
              if (traceId != null) {
                MDC.put(IoTConstant.TRACE_ID, traceId);
              }
              if (requestAttributes != null) {
                RequestContextHolder.setRequestAttributes(requestAttributes);
              }
              command.run();
            } finally {
              MDC.clear();
              RequestContextHolder.resetRequestAttributes();
            }
          });
    }
  }
}
