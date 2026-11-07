/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.security;

import cn.hutool.core.util.StrUtil;
import cn.universal.miniapp.vo.MpUserInfoVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 小程序Token认证过滤器 仅处理 /mp/api/** 路径的请求
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@Component
public class MpAuthenticationFilter extends OncePerRequestFilter {

  /** 小程序API路径前缀 */
  private static final String MP_API_PREFIX = "/mp/api";

  /** 不需要认证的路径 */
  private static final String[] WHITE_LIST = {
    "/mp/api/auth/login", "/mp/api/app/config", "/mp/api/auth/wxLogin", "/mp/api/bind/scan"
  };

  @Autowired private MpTokenService mpTokenService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestUri = request.getRequestURI();

    // 只处理小程序API请求
    if (!requestUri.startsWith(MP_API_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    // 白名单放行
    for (String whitePath : WHITE_LIST) {
      if (requestUri.startsWith(whitePath)) {
        filterChain.doFilter(request, response);
        return;
      }
    }

    // 获取Token
    String token = extractToken(request);
    if (StrUtil.isBlank(token)) {
      sendUnauthorized(response, "未提供认证Token");
      return;
    }

    // 验证Token
    MpUserInfoVO userInfo = mpTokenService.validateAndParse(token);
    if (userInfo == null) {
      sendUnauthorized(response, "Token无效或已过期");
      return;
    }

    // 设置上下文
    MpAppIdContextHolder.set(userInfo.getAppId(), userInfo.getUserId(), null);

    try {
      // 将用户信息放入请求属性
      request.setAttribute("mpUserInfo", userInfo);
      filterChain.doFilter(request, response);
    } finally {
      // 清除上下文
      MpAppIdContextHolder.clear();
    }
  }

  /** 从请求中提取Token */
  private String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (StrUtil.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    // 也支持从Header的token字段获取
    return request.getHeader("token");
  }

  /** 发送401未授权响应 */
  private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"code\":401,\"msg\":\"" + message + "\"}");
  }
}
