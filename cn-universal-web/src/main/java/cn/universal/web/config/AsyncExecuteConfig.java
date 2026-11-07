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
// import java.lang.reflect.Method;
// import java.util.concurrent.Executor;
// import java.util.concurrent.ThreadPoolExecutor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.aop.consistent.AsyncUncaughtExceptionHandler;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.scheduling.annotation.AsyncConfigurer;
// import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
// @Configuration
// @Slf4j
// public class AsyncExecuteConfig implements AsyncConfigurer {
//
//  private int corePoolSize;
//
//  private int maxPoolSize;
//
//  private int keepAliveSeconds;
//
//  private int queueCapacity;
//
//  @Bean("taskExecutor")
//  public Executor taskExecutor() {
//    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//    //核心线程池大小
//    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
//    //最大线程数
//    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
//    //队列容量
//    executor.setQueueCapacity(10);
//    //活跃时间
//    executor.setKeepAliveSeconds(60);
//    //线程名字前缀
//    executor.setThreadNamePrefix("univ-pool-");
//    // 设置线程任务装饰器
//    executor.setTaskDecorator(new AsyncTaskDecorator());
//    // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
//    // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
//    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//    executor.setWaitForTasksToCompleteOnShutdown(true);
//    executor.setAwaitTerminationSeconds(60);
//    executor.initialize();
//    return executor;
//  }
//
//  @Override
//  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {// 异步任务中异常处理
//    return new AsyncUncaughtExceptionHandler() {
//
//      @Override
//      public void handleUncaughtException(Throwable arg0, Method arg1, Object... arg2) {
//        log.error("==========================" + arg0.getMessage() + "=======================",
//            arg0);
//        log.error("exception method:" + arg1.getName());
//      }
//    };
//  }
// }
