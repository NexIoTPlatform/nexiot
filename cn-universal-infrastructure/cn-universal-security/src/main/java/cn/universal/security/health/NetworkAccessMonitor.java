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

package cn.universal.security.health;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 网络访问监控 @Author gitee.com/NexIoT */
@Slf4j
@Component
public class NetworkAccessMonitor implements HandlerInterceptor {

  @Value("${security.production.enabled:false}")
  private boolean productionSecurityEnabled;

  @Value("${security.access.monitor.enabled:true}")
  private boolean accessMonitorEnabled;

  private final ConcurrentHashMap<String, AtomicInteger> accessCountMap = new ConcurrentHashMap<>();

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!accessMonitorEnabled) {
      return true;
    }

    String clientIp = getClientIpAddress(request);
    String userAgent = request.getHeader("User-Agent");
    String requestUri = request.getRequestURI();
    String method = request.getMethod();

    // 记录访问日志
    log.info(
        "网络访问 - IP: {}, URI: {}, Method: {}, User-Agent: {}",
        clientIp,
        requestUri,
        method,
        userAgent);

    // 统计访问次数
    AtomicInteger count = accessCountMap.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
    int currentCount = count.incrementAndGet();

    // 生产环境下的异常访问监控
    if (productionSecurityEnabled && isExternalNetwork(clientIp)) {
      log.warn("检测到外网访问 - IP: {}, 访问次数: {}, URI: {}", clientIp, currentCount, requestUri);

      // 如果外网IP访问次数过多，可以考虑告警
      if (currentCount > 100) {
        log.error("外网IP访问频率异常 - IP: {}, 访问次数: {}", clientIp, currentCount);
        // 这里可以添加告警逻辑，比如发送钉钉、邮件等
      }
    }

    return true;
  }

  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null
        && !xForwardedFor.isEmpty()
        && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }

  private boolean isExternalNetwork(String ip) {
    if (ip == null) {
      return true;
    }

    return !(ip.startsWith("127.")
        || ip.startsWith("10.")
        || ip.startsWith("172.16.")
        || ip.startsWith("172.17.")
        || ip.startsWith("172.18.")
        || ip.startsWith("172.19.")
        || ip.startsWith("172.20.")
        || ip.startsWith("172.21.")
        || ip.startsWith("172.22.")
        || ip.startsWith("172.23.")
        || ip.startsWith("172.24.")
        || ip.startsWith("172.25.")
        || ip.startsWith("172.26.")
        || ip.startsWith("172.27.")
        || ip.startsWith("172.28.")
        || ip.startsWith("172.29.")
        || ip.startsWith("172.30.")
        || ip.startsWith("172.31.")
        || ip.startsWith("192.168."));
  }
}
