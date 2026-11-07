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

public class OAuth2CaptchaAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

  public static final AuthorizationGrantType CAPTCHA_GRANT_TYPE =
      new AuthorizationGrantType("captcha");

  private final String username;
  private final String password;
  private final String code;
  private final String uuid;
  private final String tenantCode;

  public OAuth2CaptchaAuthenticationToken(
      Authentication clientPrincipal, Map<String, Object> additionalParameters) {
    super(CAPTCHA_GRANT_TYPE, clientPrincipal, additionalParameters);
    this.username = (String) additionalParameters.get("username");
    this.password = (String) additionalParameters.get("password");
    this.code = (String) additionalParameters.get("code");
    this.uuid = (String) additionalParameters.get("uuid");
    this.tenantCode = (String) additionalParameters.get("tenantCode");
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getCode() {
    return code;
  }

  public String getUuid() {
    return uuid;
  }

  public String getTenantCode() {
    return tenantCode;
  }
}
