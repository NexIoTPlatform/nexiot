/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: SCADA OAuth2认证转换器，将HTTP请求转换为认证令牌
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.security.converter;

import cn.universal.security.token.OAuth2ScadaAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

/**
 * SCADA OAuth2认证转换器
 * 将HTTP请求转换为OAuth2ScadaAuthenticationToken
 */
public class OAuth2ScadaAuthenticationConverter implements AuthenticationConverter {

  @Override
  public Authentication convert(HttpServletRequest request) {
    String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
    
    // 只处理 scada_password 类型的请求
    if (!"scada_password".equals(grantType)) {
      return null;
    }

    // 获取客户端认证信息
    Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
    if (clientPrincipal == null) {
      return null;
    }

    // 构建附加参数
    Map<String, Object> additionalParameters = new HashMap<>();
    
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    String tenantId = request.getParameter("tenantId");
    
    if (StringUtils.hasText(username)) {
      additionalParameters.put("username", username);
    }
    if (StringUtils.hasText(password)) {
      additionalParameters.put("password", password);
    }
    if (StringUtils.hasText(tenantId)) {
      additionalParameters.put("tenantId", tenantId);
    } else {
      additionalParameters.put("tenantId", "000000");
    }

    return new OAuth2ScadaAuthenticationToken(clientPrincipal, additionalParameters);
  }
}
