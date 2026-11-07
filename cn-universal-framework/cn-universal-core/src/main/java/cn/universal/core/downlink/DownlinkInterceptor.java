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

/**
 * 下行拦截器接口 所有下行拦截器都需要实现此接口
 *
 * @version 1.0
 * @since 2025/10/24
 */
public interface DownlinkInterceptor {

  /**
   * 拦截器名称（用于日志和调试）
   *
   * @return 拦截器名称
   */
  String getName();

  /**
   * 执行顺序（数字越小越先执行） 建议： - 0-100: 系统级拦截器（鉴权、限流等） - 100-500: 业务级拦截器（数据处理、编解码等） - 500-1000:
   * 辅助级拦截器（日志、监控等）
   *
   * @return 执行顺序
   */
  int getOrder();

  /**
   * 是否启用此拦截器 可通过配置动态控制拦截器的启用状态
   *
   * @return true-启用，false-禁用
   */
  default boolean isEnabled() {
    return true;
  }

  /**
   * 拦截器执行阶段
   *
   * @return 执行阶段
   */
  InterceptorPhase getPhase();

  /**
   * 是否支持当前上下文 可根据协议类型、产品类型等条件判断是否需要执行此拦截器
   *
   * @param context 下行上下文
   * @return true-支持，false-不支持
   */
  default boolean supports(DownlinkContext<?> context) {
    return true;
  }

  /**
   * 前置处理（在主流程之前执行） 此方法在拦截器链中按order顺序执行
   *
   * @param context 下行上下文
   * @return true-继续执行后续拦截器和主流程，false-中断执行
   * @throws Exception 处理异常
   */
  boolean preHandle(DownlinkContext<?> context) throws Exception;

  /**
   * 后置处理（在主流程之后执行） 此方法在拦截器链中按order顺序执行 注意：即使preHandle返回false，postHandle也不会被调用
   *
   * @param context 下行上下文
   * @throws Exception 处理异常
   */
  void postHandle(DownlinkContext<?> context) throws Exception;

  /**
   * 完成处理（无论成功失败都会执行） 此方法在拦截器链中按order倒序执行（类似finally块） 可用于资源清理、异常处理等
   *
   * @param context 下行上下文
   * @param ex 执行过程中的异常（如果有）
   */
  default void afterCompletion(DownlinkContext<?> context, Exception ex) {
    // 默认不做任何处理
  }
}
