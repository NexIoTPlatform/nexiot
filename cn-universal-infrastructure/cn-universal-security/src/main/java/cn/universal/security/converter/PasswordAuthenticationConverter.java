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

package cn.universal.security.converter;

import cn.universal.security.token.PasswordAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

public class PasswordAuthenticationConverter implements AuthenticationConverter {

  private final RegisteredClientRepository clientRepository;

  public PasswordAuthenticationConverter(RegisteredClientRepository clientRepository) {
    this.clientRepository = clientRepository;
  }

  @Override
  public Authentication convert(HttpServletRequest request) {
    // 只处理 POST 请求
    if (!"POST".equals(request.getMethod())) {
      return null;
    }

    // 检查 grant_type 参数
    String grantType = request.getParameter("grant_type");
    if (!AuthorizationGrantType.PASSWORD.getValue().equals(grantType)) {
      return null;
    }

    // 获取客户端ID
    String clientId = request.getParameter("client_id");
    if (!StringUtils.hasText(clientId)) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "invalid_request", null));
    }

    // 查找注册的客户端
    RegisteredClient registeredClient = this.clientRepository.findByClientId(clientId);
    if (registeredClient == null) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "invalid_request", null));
    }

    // 验证客户端是否支持密码模式
    if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.PASSWORD)) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, "invalid_request", null));
    }

    // 获取用户名和密码
    String username = request.getParameter("username");
    String password = request.getParameter("password");

    if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
      throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
    }

    // 创建客户端认证令牌
    Authentication clientPrincipal =
        new OAuth2ClientAuthenticationToken(
            registeredClient, ClientAuthenticationMethod.CLIENT_SECRET_POST, null);

    // 提取请求参数
    Map<String, Object> additionalParameters = new HashMap<>();
    additionalParameters.put("username", username);
    additionalParameters.put("password", password);
    additionalParameters.put("tenantCode", request.getParameter("tenantCode"));
    additionalParameters.put("grant_type", grantType);

    return new PasswordAuthenticationToken(clientPrincipal, additionalParameters);
  }
}
