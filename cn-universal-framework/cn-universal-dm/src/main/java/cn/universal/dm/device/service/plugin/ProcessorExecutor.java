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

package cn.universal.dm.device.service.plugin;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 通用处理器执行器
 *
 * <p>独立出处理器链的通用逻辑：获取处理器、排序、循环执行 提供统一的处理器执行模式，各模块可以复用这些通用逻辑
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j
@Component
public class ProcessorExecutor {

  /**
   * 执行处理器链
   *
   * @param processors 处理器列表
   * @param chainName 处理器链名称（用于日志）
   * @param executor 单个处理器执行函数
   * @param <T> 处理器类型
   * @param <R> 处理结果类型
   * @return 是否全部执行成功
   */
  public <T extends BaseMessageProcessor, R> boolean executeChain(
      List<T> processors, String chainName, Function<T, R> executor, Predicate<R> successChecker) {

    return executeChain(processors, chainName, executor, successChecker, null);
  }

  /**
   * 执行处理器链（带支持性检查）
   *
   * @param processors 处理器列表
   * @param chainName 处理器链名称（用于日志）
   * @param executor 单个处理器执行函数
   * @param successChecker 成功检查函数
   * @param supportChecker 支持性检查函数（可选）
   * @param <T> 处理器类型
   * @param <R> 处理结果类型
   * @return 是否全部执行成功
   */
  public <T extends BaseMessageProcessor, R> boolean executeChain(
      List<T> processors,
      String chainName,
      Function<T, R> executor,
      Predicate<R> successChecker,
      Function<T, Boolean> supportChecker) {

    if (processors == null || processors.isEmpty()) {
      log.warn("[{}] 没有找到任何处理器", chainName);
      return true; // 没有处理器也算成功
    }

    // 1. 过滤启用的处理器并排序
    List<T> sortedProcessors = getSortedProcessors(processors);

    if (sortedProcessors.isEmpty()) {
      log.warn("[{}] 没有启用的处理器", chainName);
      return true;
    }

    log.debug(
        "[{}] 找到 {} 个处理器: {}",
        chainName,
        sortedProcessors.size(),
        sortedProcessors.stream().map(p -> p.getName() + "(" + p.getOrder() + ")").toList());

    // 2. 循环执行处理器
    for (T processor : sortedProcessors) {
      if (!executeProcessor(processor, chainName, executor, successChecker, supportChecker)) {
        return false;
      }
    }

    log.debug("[{}] 处理器链执行完成", chainName);
    return true;
  }

  /** 执行单个处理器 */
  private <T extends BaseMessageProcessor, R> boolean executeProcessor(
      T processor,
      String chainName,
      Function<T, R> executor,
      Predicate<R> successChecker,
      Function<T, Boolean> supportChecker) {

    try {
      // 1. 支持性检查
      if (supportChecker != null && !supportChecker.apply(processor)) {
        log.debug("[{}] 处理器 {} 不支持当前请求，跳过执行", chainName, processor.getName());
        return true;
      }

      log.debug("[{}] 执行处理器: {} (order={})", chainName, processor.getName(), processor.getOrder());

      // 2. 执行处理器
      R result = executor.apply(processor);

      // 3. 检查执行结果
      boolean success = successChecker.test(result);

      if (success) {
        log.debug("[{}] 处理器 {} 执行成功", chainName, processor.getName());
      } else {
        log.error("[{}] 处理器 {} 执行失败", chainName, processor.getName());
      }

      return success;

    } catch (Exception e) {
      log.error("[{}] 处理器 {} 执行异常: {}", chainName, processor.getName(), e.getMessage(), e);
      return false;
    }
  }

  /** 获取排序后的处理器列表 */
  public <T extends BaseMessageProcessor> List<T> getSortedProcessors(List<T> processors) {
    return processors.stream()
        .filter(BaseMessageProcessor::isEnabled)
        .sorted(getProcessorComparator())
        .toList();
  }

  /** 获取处理器比较器 */
  public Comparator<BaseMessageProcessor> getProcessorComparator() {
    return Comparator.comparingInt(BaseMessageProcessor::getOrder)
        .thenComparingInt(BaseMessageProcessor::getPriority)
        .thenComparing(BaseMessageProcessor::getName);
  }

  /** 获取处理器名称列表（用于调试） */
  public List<String> getProcessorNames(List<? extends BaseMessageProcessor> processors) {
    return getSortedProcessors((List<BaseMessageProcessor>) processors).stream()
        .map(BaseMessageProcessor::getName)
        .toList();
  }

  /**
   * 获取启用的处理器数量
   *
   * @param processors 处理器列表
   * @param <T> 处理器类型
   * @return 启用的处理器数量
   */
  public <T extends BaseMessageProcessor> long getEnabledProcessorCount(List<T> processors) {
    if (processors == null) {
      return 0;
    }

    return processors.stream().filter(BaseMessageProcessor::isEnabled).count();
  }

  /**
   * 检查指定名称的处理器是否存在且启用
   *
   * @param processors 处理器列表
   * @param name 处理器名称
   * @param <T> 处理器类型
   * @return 是否存在且启用
   */
  public <T extends BaseMessageProcessor> boolean isProcessorEnabled(
      List<T> processors, String name) {
    if (processors == null || name == null) {
      return false;
    }

    return processors.stream().anyMatch(p -> name.equals(p.getName()) && p.isEnabled());
  }

  /** 支持返回处理器链的第一个非null结果（比如R<?>），不影响原有boolean链式执行 */
  public <T extends BaseMessageProcessor, R> R executeChainWithResult(
      List<T> processors,
      String chainName,
      Function<T, R> executor,
      Predicate<R> successChecker,
      Function<T, Boolean> supportChecker) {
    if (processors == null || processors.isEmpty()) {
      log.warn("[{}] 没有找到任何处理器", chainName);
      return null;
    }
    List<T> sortedProcessors = getSortedProcessors(processors);
    if (sortedProcessors.isEmpty()) {
      log.warn("[{}] 没有启用的处理器", chainName);
      return null;
    }
    for (T processor : sortedProcessors) {
      if (supportChecker != null && !supportChecker.apply(processor)) {
        continue;
      }
      R result = executor.apply(processor);
      if (successChecker.test(result)) {
        return result; // 返回第一个成功的结果
      }
    }
    return null;
  }
}
