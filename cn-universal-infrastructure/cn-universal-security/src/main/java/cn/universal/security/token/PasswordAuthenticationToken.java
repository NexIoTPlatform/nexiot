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

package cn.universal.security.token;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

public class PasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

  private final String username;
  private final String password;
  private final String tenantCode;

  public PasswordAuthenticationToken(
      Authentication clientPrincipal, Map<String, Object> additionalParameters) {
    super(AuthorizationGrantType.PASSWORD, clientPrincipal, additionalParameters);
    this.username = (String) additionalParameters.get("username");
    this.password = (String) additionalParameters.get("password");
    this.tenantCode = (String) additionalParameters.get("tenantCode");
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getTenantCode() {
    return tenantCode;
  }
}
