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

package cn.universal.web.config.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * @version 1.0 @Author ZQQ
 * @desc :
 * @since 2025/9/22 15:54
 */
@Slf4j
public class ResponseHeadFilter implements Filter {

  /** cookies 白名单 */
  private Set<String> whileCookies = Stream.of("").collect(Collectors.toSet());

  @Override
  public void init(FilterConfig filterConfig) {
    log.info("init success ResponseHeadFilter");
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // 增加响应头缺失代码
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    res.addHeader("X-Frame-Options", "SAMEORIGIN");
    res.addHeader("Referrer-Policy", "origin");
    res.addHeader("Content-Security-Policy", "object-src 'self'");
    res.addHeader("X-Permitted-Cross-Domain-Policies", "master-only");
    res.addHeader("X-Content-Type-Options", "nosniff");
    res.addHeader("X-XSS-Protection", "1; mode=block");
    res.addHeader("X-Download-Options", "noopen");
    res.addHeader("Strict-Transport-Security", "max-age=63072000; includeSubdomains; preload");
    // 处理cookie问题、
    try {
      Cookie[] cookies = req.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (StrUtil.isNotBlank(cookie.getName()) && whileCookies.contains(cookie.getName())) {
            String value = cookie.getValue();
            StringBuilder builder = new StringBuilder();
            builder.append(cookie.getName() + "=" + value + ";");
            builder.append("Secure;"); // Cookie设置Secure标识
            builder.append("HttpOnly;"); // Cookie设置HttpOnly
            res.addHeader("Set-Cookie", builder.toString());
          }
        }
      }
    } catch (Exception e) {
      log.error("拦截器异常={}", e);
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {}
}
