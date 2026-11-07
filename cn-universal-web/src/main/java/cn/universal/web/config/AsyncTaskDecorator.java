/// *
// *
// * Copyright (c) 2025, NexIoT. All Rights Reserved.
// *
// * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
// * @Author: gitee.com/NexIoT
// * @Email: wo8335224@gmail.com
// * @Wechat: outlookFil
// *
// *
// */
//
// package cn.universal.web.config;
//
// import java.util.concurrent.Executor;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.ScheduledExecutorService;
// import org.slf4j.MDC;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
// import org.springframework.web.context.request.RequestAttributes;
// import org.springframework.web.context.request.RequestContextHolder;
//
// @Configuration
// public class AsyncTaskDecorator {
//
//  @Bean("virtualThreadExecutor")
//  public ExecutorService virtualThreadExecutor() {
//    return Executors.newVirtualThreadPerTaskExecutor();
//  }
//
//  @Bean("virtualScheduledExecutor")
//  public ScheduledExecutorService virtualScheduledExecutor() {
//    // 使用Spring Boot的ThreadPoolTaskScheduler，支持虚拟线程
//    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
//    scheduler.setPoolSize(10);
//    scheduler.setThreadNamePrefix("virtual-scheduled-");
//    scheduler.setVirtualThreads(true); // 启用虚拟线程
//    scheduler.initialize();
//    return scheduler.getScheduledExecutor();
//  }
//
//  // 设置默认的异步执行器为虚拟线程，并支持MDC传递和请求上下文传递
//  @Bean("taskExecutor")
//  public Executor taskExecutor() {
//    ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
//    // 返回包装后的Executor，支持MDC传递和请求上下文传递
//    return new Executor() {
//      @Override
//      public void execute(Runnable command) {
//        String traceId = MDC.get("traceId");
//        // 获取当前请求上下文
//        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//
//        virtualExecutor.execute(() -> {
//          try {
//            // 传递MDC上下文
//            if (traceId != null) {
//              MDC.put("traceId", traceId);
//            }
//            // 传递请求上下文到异步线程
//            if (requestAttributes != null) {
//              RequestContextHolder.setRequestAttributes(requestAttributes);
//            }
//            command.run();
//          } finally {
//            // 清理MDC和请求上下文
//            MDC.remove("traceId");
//            RequestContextHolder.resetRequestAttributes();
//          }
//        });
//      }
//    };
//  }
// }
