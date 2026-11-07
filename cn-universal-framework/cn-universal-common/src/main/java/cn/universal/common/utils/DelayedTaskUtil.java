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

package cn.universal.common.utils;

import static cn.hutool.script.ScriptUtil.eval;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.script.ScriptContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 延迟任务工具类 - 使用Spring配置的虚拟线程 @Author gitee.com/NexIoT
 *
 * @since 2019年07月24日 上午09:49
 */
@Slf4j
@Component
public class DelayedTaskUtil {

  @Autowired
  @Qualifier("virtualScheduledExecutor")
  private ScheduledExecutorService scheduledExecutor;

  // 使用Spring配置的虚拟线程执行器
  @Autowired
  @Qualifier("virtualThreadExecutor")
  private ExecutorService scriptExecutor;

  /**
   * 添加延迟任务
   *
   * @param command 要执行的任务
   * @param delay 延迟时间
   * @param unit 时间单位
   */
  public void putTask(Runnable command, long delay, TimeUnit unit) {
    log.info(
        "[CORE][定时任务] 线程: {}, 执行延迟任务, 时间: {}, 延迟: {} {}",
        Thread.currentThread().getName(),
        LocalDateTime.now(),
        delay,
        unit.name().toLowerCase());

    // 使用Spring配置的虚拟线程调度器
    scheduledExecutor.schedule(command, delay, unit);
  }

  /**
   * 添加固定频率的重复任务
   *
   * @param command 要执行的任务
   * @param initialDelay 初始延迟时间
   * @param period 重复周期
   * @param unit 时间单位
   */
  public void putFixedRateTask(Runnable command, long initialDelay, long period, TimeUnit unit) {
    log.info(
        "[CORE][定时任务] 线程: {}, 执行固定频率任务, 时间: {}, 初始延迟: {}, 周期: {} {}",
        Thread.currentThread().getName(),
        LocalDateTime.now(),
        initialDelay,
        period,
        unit.name().toLowerCase());

    scheduledExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  /**
   * 添加固定延迟的重复任务
   *
   * @param command 要执行的任务
   * @param initialDelay 初始延迟时间
   * @param delay 重复延迟
   * @param unit 时间单位
   */
  public void putFixedDelayTask(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    log.info(
        "[CORE][定时任务] 线程: {}, 执行固定延迟任务, 时间: {}, 初始延迟: {}, 延迟: {} {}",
        Thread.currentThread().getName(),
        LocalDateTime.now(),
        initialDelay,
        delay,
        unit.name().toLowerCase());

    scheduledExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }

  // 使用Record优化方法信息
  public record ScriptMethodInfo(String name, Class<?> returnType, List<Class<?>> parameters) {

    public static ScriptMethodInfo from(Method method) {
      return new ScriptMethodInfo(
          method.getName(), method.getReturnType(), Arrays.asList(method.getParameterTypes()));
    }
  }

  // 异步执行脚本
  public CompletableFuture<Object> executeAsync(String script, ScriptContext context) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return eval(script, context);
          } catch (Exception e) {
            log.error("[CORE][定时任务] 脚本执行失败: {}", script, e);
            throw new RuntimeException("脚本执行失败", e);
          }
        },
        scriptExecutor);
  }

  // 异步执行脚本（带超时）
  public CompletableFuture<Object> executeAsyncWithTimeout(
      String script, ScriptContext context, long timeout, TimeUnit unit) {
    CompletableFuture<Object> future = executeAsync(script, context);

    return future
        .orTimeout(timeout, unit)
        .exceptionally(
            throwable -> {
              if (throwable instanceof TimeoutException) {
                log.error("[CORE][定时任务] 脚本执行超时: {}", script);
                throw new RuntimeException("脚本执行超时", throwable);
              }
              throw new RuntimeException("脚本执行失败", throwable);
            });
  }

  // 批量异步执行脚本
  public List<CompletableFuture<Object>> executeBatchAsync(
      List<String> scripts, ScriptContext context) {
    return scripts.stream()
        .map(script -> executeAsync(script, context))
        .collect(Collectors.toList());
  }

  // 等待所有脚本执行完成
  public List<Object> executeBatchAndWait(List<String> scripts, ScriptContext context) {
    List<CompletableFuture<Object>> futures = executeBatchAsync(scripts, context);

    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }
}
