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

package cn.universal.core.downlink;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 下行拦截器链管理器 负责管理和执行所有下行拦截器
 *
 * @version 1.0
 * @since 2025/10/24
 */
@Slf4j
@Component
public class DownlinkInterceptorChain {

  @Autowired(required = false)
  private List<DownlinkInterceptor> interceptors = new ArrayList<>();

  /**
   * 执行前置拦截器（PRE阶段）
   *
   * @param context 下行上下文
   * @return true-继续执行，false-中断执行
   */
  public boolean executePreInterceptors(DownlinkContext<?> context) {
    return executeInterceptors(context, InterceptorPhase.PRE, true);
  }

  /**
   * 执行中置拦截器（MID阶段）
   *
   * @param context 下行上下文
   * @return true-继续执行，false-中断执行
   */
  public boolean executeMidInterceptors(DownlinkContext<?> context) {
    return executeInterceptors(context, InterceptorPhase.MID, true);
  }

  /**
   * 执行后置拦截器（POST阶段）
   *
   * @param context 下行上下文
   */
  public void executePostInterceptors(DownlinkContext<?> context) {
    executeInterceptors(context, InterceptorPhase.POST, false);
  }

  /**
   * 触发完成拦截器（afterCompletion） 按order倒序执行，确保资源正确释放
   *
   * @param context 下行上下文
   * @param ex 执行过程中的异常（如果有）
   */
  public void triggerAfterCompletion(DownlinkContext<?> context, Exception ex) {
    List<DownlinkInterceptor> sortedInterceptors =
        getSortedInterceptors().stream()
            .filter(i -> i.supports(context))
            .sorted(Comparator.comparingInt(DownlinkInterceptor::getOrder).reversed())
            .collect(Collectors.toList());

    for (DownlinkInterceptor interceptor : sortedInterceptors) {
      try {
        log.debug(
            "[拦截器链][AfterCompletion] 执行拦截器: {} (order={})",
            interceptor.getName(),
            interceptor.getOrder());
        interceptor.afterCompletion(context, ex);
      } catch (Exception e) {
        log.error("[拦截器链][AfterCompletion] 拦截器 {} 执行异常", interceptor.getName(), e);
      }
    }
  }

  /**
   * 执行指定阶段的拦截器
   *
   * @param context 下行上下文
   * @param phase 执行阶段
   * @param preHandle 是否执行preHandle（true）或postHandle（false）
   * @return 是否继续执行（仅preHandle时有效）
   */
  private boolean executeInterceptors(
      DownlinkContext<?> context, InterceptorPhase phase, boolean preHandle) {

    List<DownlinkInterceptor> phaseInterceptors =
        getSortedInterceptors().stream()
            .filter(i -> i.getPhase() == phase)
            .filter(DownlinkInterceptor::isEnabled)
            .filter(i -> i.supports(context))
            .collect(Collectors.toList());

    if (phaseInterceptors.isEmpty()) {
      log.debug("[拦截器链][{}] 没有找到可执行的拦截器", phase);
      return true;
    }

    log.debug(
        "[拦截器链][{}] 找到 {} 个拦截器: {}",
        phase,
        phaseInterceptors.size(),
        phaseInterceptors.stream()
            .map(i -> i.getName() + "(" + i.getOrder() + ")")
            .collect(Collectors.joining(", ")));

    for (DownlinkInterceptor interceptor : phaseInterceptors) {
      try {
        if (preHandle) {
          log.debug(
              "[拦截器链][{}][PreHandle] 执行拦截器: {} (order={})",
              phase,
              interceptor.getName(),
              interceptor.getOrder());

          boolean continueChain = interceptor.preHandle(context);

          if (!continueChain) {
            log.warn("[拦截器链][{}] 拦截器 {} 中断了执行", phase, interceptor.getName());
            context.markIntercepted(interceptor.getName());
            return false;
          }

          log.debug("[拦截器链][{}][PreHandle] 拦截器 {} 执行成功", phase, interceptor.getName());
        } else {
          log.debug(
              "[拦截器链][{}][PostHandle] 执行拦截器: {} (order={})",
              phase,
              interceptor.getName(),
              interceptor.getOrder());

          interceptor.postHandle(context);

          log.debug("[拦截器链][{}][PostHandle] 拦截器 {} 执行成功", phase, interceptor.getName());
        }
      } catch (Exception e) {
        log.error("[拦截器链][{}] 拦截器 {} 执行异常", phase, interceptor.getName(), e);

        if (preHandle) {
          context.markIntercepted("拦截器异常: " + e.getMessage());
          context.setException(e);
          return false;
        }
      }
    }

    return true;
  }

  /**
   * 获取排序后的拦截器列表
   *
   * @return 排序后的拦截器列表
   */
  private List<DownlinkInterceptor> getSortedInterceptors() {
    if (interceptors == null) {
      return new ArrayList<>();
    }
    return interceptors.stream()
        .sorted(Comparator.comparingInt(DownlinkInterceptor::getOrder))
        .collect(Collectors.toList());
  }

  /**
   * 获取所有拦截器数量
   *
   * @return 拦截器数量
   */
  public int getInterceptorCount() {
    return interceptors != null ? interceptors.size() : 0;
  }

  /**
   * 获取启用的拦截器数量
   *
   * @return 启用的拦截器数量
   */
  public long getEnabledInterceptorCount() {
    if (interceptors == null) {
      return 0;
    }
    return interceptors.stream().filter(DownlinkInterceptor::isEnabled).count();
  }

  /**
   * 获取指定阶段的拦截器数量
   *
   * @param phase 执行阶段
   * @return 拦截器数量
   */
  public long getInterceptorCountByPhase(InterceptorPhase phase) {
    if (interceptors == null) {
      return 0;
    }
    return interceptors.stream()
        .filter(i -> i.getPhase() == phase)
        .filter(DownlinkInterceptor::isEnabled)
        .count();
  }

  /**
   * 获取所有拦截器名称列表（用于调试）
   *
   * @return 拦截器名称列表
   */
  public List<String> getInterceptorNames() {
    if (interceptors == null) {
      return new ArrayList<>();
    }
    return interceptors.stream()
        .map(i -> i.getName() + "(" + i.getPhase() + "," + i.getOrder() + ")")
        .collect(Collectors.toList());
  }
}
