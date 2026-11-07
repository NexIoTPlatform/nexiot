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

package cn.universal.core.downlink.interceptor;

import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.DownlinkInterceptor;
import cn.universal.core.downlink.InterceptorPhase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 下行监控拦截器 收集下行消息的监控指标（耗时、成功率等）
 *
 * @version 1.0
 * @since 2025/10/24
 */
@Slf4j
@Component
@Order(10) // 监控拦截器优先级高，尽早开始计时
public class DownlinkMetricsInterceptor implements DownlinkInterceptor {

  @Override
  public String getName() {
    return "下行监控拦截器";
  }

  @Override
  public int getOrder() {
    return 10;
  }

  @Override
  public InterceptorPhase getPhase() {
    return InterceptorPhase.PRE;
  }

  @Override
  public boolean preHandle(DownlinkContext<?> context) {
    // 记录开始时间（在 context 中已经设置，这里可以做额外的监控初始化）
    context.setAttribute("metrics.startTime", System.currentTimeMillis());
    context.setAttribute("metrics.protocol", context.getProtocolCode());

    log.debug("[监控拦截器] 开始监控下行消息: protocol={}", context.getProtocolCode());

    return true;
  }

  @Override
  public void postHandle(DownlinkContext<?> context) {
    long duration = context.getDuration();
    String protocol = context.getProtocolCode();

    // 这里可以集成 Prometheus、Micrometer 等监控框架
    log.debug("[监控拦截器] 下行消息耗时: protocol={}, duration={}ms", protocol, duration);

    // 示例：记录耗时到属性中，供后续使用
    context.setAttribute("metrics.duration", duration);
  }

  @Override
  public void afterCompletion(DownlinkContext<?> context, Exception ex) {
    String protocol = context.getProtocolCode();
    String status = ex != null ? "error" : "success";
    long duration = context.getDuration();

    // 这里可以上报监控数据
    log.debug("[监控拦截器] 下行消息完成: protocol={}, status={}, duration={}ms", protocol, status, duration);

    // 示例：可以在这里集成监控系统
    // meterRegistry.counter("downlink.count", "protocol", protocol, "status", status).increment();
    // meterRegistry.timer("downlink.duration", "protocol", protocol).record(duration,
    // TimeUnit.MILLISECONDS);
  }
}
