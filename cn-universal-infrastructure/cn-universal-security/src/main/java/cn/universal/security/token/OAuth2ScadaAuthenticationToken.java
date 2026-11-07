/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: SCADA OAuth2认证令牌，用于SCADA前端登录
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.security.token;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

/**
 * SCADA OAuth2认证令牌
 * 用于SCADA前端的简化密码登录（不需要验证码）
 * 与管理后台共享相同的JWT token格式
 */
public class OAuth2ScadaAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

  public static final AuthorizationGrantType SCADA_GRANT_TYPE =
      new AuthorizationGrantType("scada_password");

  private final String username;
  private final String password;
  private final String tenantId;

  public OAuth2ScadaAuthenticationToken(
      Authentication clientPrincipal, Map<String, Object> additionalParameters) {
    super(SCADA_GRANT_TYPE, clientPrincipal, additionalParameters);
    this.username = (String) additionalParameters.get("username");
    this.password = (String) additionalParameters.get("password");
    this.tenantId = (String) additionalParameters.getOrDefault("tenantId", "000000");
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getTenantId() {
    return tenantId;
  }
}
