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

package cn.universal.dm.device.service.push;

import cn.hutool.core.collection.CollectionUtil;
import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 上行消息处理器管理器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class UPProcessorManager {

  private final List<UPProcessor<?>> processors;
  private final List<UPProcessor<?>> beforeProcessors;
  private final List<UPProcessor<?>> afterProcessors;

  public UPProcessorManager(List<UPProcessor<?>> processors) {
    this.processors =
        processors == null
            ? List.of()
            : processors.stream()
                .sorted(Comparator.comparingInt(UPProcessor::getOrder))
                .toList();
    this.beforeProcessors =
        this.processors.stream().filter(UPProcessor::isBeforePushEnabled).toList();
    this.afterProcessors =
        this.processors.stream().filter(UPProcessor::isAfterPushEnabled).toList();
  }

  /**
   * 获取所有处理器并按优先级排序
   *
   * @param <T> 请求类型
   * @return 处理器列表
   */
  @SuppressWarnings("unchecked")
  public <T extends BaseUPRequest> List<UPProcessor<T>> getProcessors() {
    return processors.stream()
        .map(processor -> (UPProcessor<T>) processor)
        .collect(Collectors.toList());
  }

  /**
   * 执行推送前处理
   *
   * @param upRequests 上行请求列表
   * @param <T> 请求类型
   * @return 处理后的请求列表
   */
  public <T extends BaseUPRequest> List<T> executeBeforePush(List<T> upRequests) {
    if (CollectionUtil.isEmpty(upRequests)) {
      return upRequests;
    }

    List<UPProcessor<T>> processors = castProcessors(beforeProcessors);
    if (processors.isEmpty()) {
      return upRequests;
    }

    List<T> processedRequests = upRequests;
    for (UPProcessor<T> processor : processors) {
      try {
        // 过滤出支持的请求
        List<T> supportedRequests =
            processedRequests.stream().filter(processor::supports).collect(Collectors.toList());

        if (!supportedRequests.isEmpty()) {
          log.debug(
              "[UP处理器][推送前] 执行处理器: {}, 处理 {} 条消息", processor.getName(), supportedRequests.size());

          List<T> result = processor.beforePush(supportedRequests);
          if (result != null) {
            processedRequests =
                mergeProcessedRequests(processedRequests, supportedRequests, result);
          }
        }
      } catch (Exception e) {
        log.error("[UP处理器][推送前] 处理器 {} 执行异常", processor.getName(), e);
      }
    }

    return processedRequests;
  }

  private <T extends BaseUPRequest> List<T> mergeProcessedRequests(
      List<T> original, List<T> supported, List<T> processedSupported) {
    if (supported.size() == original.size()) {
      return processedSupported;
    }
    Set<T> supportedIdentities = Collections.newSetFromMap(new IdentityHashMap<>());
    supportedIdentities.addAll(supported);
    Iterator<T> replacements = processedSupported.iterator();
    List<T> merged = new ArrayList<>(original.size() + processedSupported.size());
    for (T request : original) {
      if (!supportedIdentities.contains(request)) {
        merged.add(request);
      } else if (replacements.hasNext()) {
        merged.add(replacements.next());
      }
    }
    replacements.forEachRemaining(merged::add);
    return merged;
  }

  /**
   * 执行推送后处理（带推送结果）
   *
   * @param upRequests 上行请求列表
   * @param pushResults 推送结果列表
   * @param <T> 请求类型
   */
  public <T extends BaseUPRequest> void executeAfterPush(
      List<T> upRequests, List<IoTPushResult> pushResults) {
    if (CollectionUtil.isEmpty(upRequests)) {
      return;
    }

    List<UPProcessor<T>> processors = castProcessors(afterProcessors);
    if (processors.isEmpty()) {
      return;
    }

    for (UPProcessor<T> processor : processors) {
      try {
        // 过滤出支持的请求
        List<T> supportedRequests =
            upRequests.stream().filter(processor::supports).collect(Collectors.toList());

        if (!supportedRequests.isEmpty()) {
          log.debug(
              "[UP处理器][推送后] 执行处理器: {}, 处理 {} 条消息, 结果数量: {}",
              processor.getName(),
              supportedRequests.size(),
              pushResults != null ? pushResults.size() : 0);

          processor.afterPush(supportedRequests, pushResults);
        }
      } catch (Exception e) {
        log.error("[UP处理器][推送后] 处理器 {} 执行异常", processor.getName(), e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends BaseUPRequest> List<UPProcessor<T>> castProcessors(
      List<UPProcessor<?>> source) {
    return source.stream().map(processor -> (UPProcessor<T>) processor).toList();
  }
}
