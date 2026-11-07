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

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class CaptchaAuthenticationToken extends AbstractAuthenticationToken {

  private final String username;
  private final String password;
  private final String code;
  private final String uuid;

  public CaptchaAuthenticationToken(String username, String password, String code, String uuid) {
    super(null);
    this.username = username;
    this.password = password;
    this.code = code;
    this.uuid = uuid;
    setAuthenticated(false);
  }

  @Override
  public Object getCredentials() {
    return password;
  }

  @Override
  public Object getPrincipal() {
    return username;
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
}
