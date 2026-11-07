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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** IP白名单过滤器 @Author gitee.com/NexIoT */
@Slf4j
public class IpWhitelistFilter extends OncePerRequestFilter {

  private final List<String> allowedIps;

  public IpWhitelistFilter(String allowedIps) {
    this.allowedIps =
        StringUtils.hasText(allowedIps) ? Arrays.asList(allowedIps.split(",")) : Arrays.asList();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String clientIp = getClientIpAddress(request);
    String requestUri = request.getRequestURI();

    log.info("IP白名单检查 - IP: {}, URI: {}", clientIp, requestUri);

    // 检查IP白名单
    if (!allowedIps.isEmpty() && !allowedIps.contains(clientIp)) {
      log.warn("IP访问被拒绝: {} - {}", clientIp, requestUri);
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.getWriter().write("Access Denied: IP not in whitelist");
      return;
    }

    chain.doFilter(request, response);
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
}
