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

import cn.universal.web.config.xss.XssHttpServletRequestWrapper;
import cn.universal.web.config.xss.XssProperties;
import jakarta.annotation.Resource;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

/**
 * @version 1.0 @Author ZQQ
 * @desc :
 * @since 2025/9/22 15:54
 */
@Slf4j
public class XssFilter implements Filter {

  @Resource private XssProperties xssProperties;

  private final AntPathMatcher antPathMatcher = new AntPathMatcher();

  @Override
  public void init(FilterConfig filterConfig) {
    log.info("init success XSS");
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String path = httpRequest.getServletPath();

    if (!xssProperties.getEnabled() || isWechatCallback(httpRequest, path) || isXssSkip(path)) {
      chain.doFilter(request, response);
    } else {
      XssHttpServletRequestWrapper xssRequest =
          new XssHttpServletRequestWrapper((HttpServletRequest) request);
      chain.doFilter(xssRequest, response);
    }
  }

  private boolean isXssSkip(String path) {
    return xssProperties.getSkipUrl().stream()
        .anyMatch(pattern -> antPathMatcher.match(pattern, path));
  }

  private boolean isWechatCallback(HttpServletRequest request, String path) {
    // The callback controller verifies signatures and parses XML with external entities disabled.
    // HTML filtering would corrupt signed XML/CDATA before that verification can happen.
    return ("GET".equals(request.getMethod()) || "POST".equals(request.getMethod()))
        && antPathMatcher.match("/wechat/callback/*", path);
  }

  @Override
  public void destroy() {}
}
