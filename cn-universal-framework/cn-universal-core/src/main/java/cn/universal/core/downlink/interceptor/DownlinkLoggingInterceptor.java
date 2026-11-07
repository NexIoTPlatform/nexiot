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
 * 下行日志拦截器 记录下行消息的详细日志信息
 *
 * @version 1.0
 * @since 2025/10/24
 */
@Slf4j
@Component
@Order(1000) // 日志拦截器优先级较低
public class DownlinkLoggingInterceptor implements DownlinkInterceptor {

  @Override
  public String getName() {
    return "下行日志拦截器";
  }

  @Override
  public int getOrder() {
    return 1000;
  }

  @Override
  public InterceptorPhase getPhase() {
    return InterceptorPhase.PRE;
  }

  @Override
  public boolean preHandle(DownlinkContext<?> context) {
    log.info(
        "[下行日志] 开始处理下行消息: protocol={}, productKey={}, deviceId={}, iotId={}",
        context.getProtocolCode(),
        context.getProductKey(),
        context.getDeviceId(),
        context.getIotId());

    if (log.isDebugEnabled() && !"sip".equals(context.getProtocolCode())) {
      log.debug("[下行日志] 原始消息: {}", context.getRawMessage());
    }

    return true;
  }

  @Override
  public void postHandle(DownlinkContext<?> context) {
    long duration = context.getDuration();
    boolean success = context.getResult() != null && context.getResult().isSuccess();

    log.info(
        "[下行日志] 下行消息处理完成: protocol={}, productKey={}, deviceId={}, " + "success={}, duration={}ms",
        context.getProtocolCode(),
        context.getProductKey(),
        context.getDeviceId(),
        success,
        duration);

    if (log.isDebugEnabled() && context.getResult() != null) {
      log.debug("[下行日志] 处理结果: {}", cn.universal.common.utils.JsonDesensitizationUtil.desensitizeObject(context.getResult()));
    }
  }

  @Override
  public void afterCompletion(DownlinkContext<?> context, Exception ex) {
    if (ex != null) {
      log.error(
          "[下行日志] 下行消息处理异常: protocol={}, productKey={}, deviceId={}, error={}",
          context.getProtocolCode(),
          context.getProductKey(),
          context.getDeviceId(),
          ex.getMessage(),
          ex);
    }
  }
}
