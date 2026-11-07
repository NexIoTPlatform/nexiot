/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoTXin 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoTXin
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.security;

import cn.universal.security.converter.OAuth2CaptchaAuthenticationConverter;
import cn.universal.security.converter.WechatQrAuthenticationConverter;
import cn.universal.security.provider.OAuth2CaptchaAuthenticationProvider;
import cn.universal.security.provider.PasswordAuthenticationProvider;
import cn.universal.security.provider.WechatQrAuthenticationProvider;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;

@Configuration
public class AuthorizationServerConfig {

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(
      HttpSecurity http,
      OAuth2CaptchaAuthenticationProvider captchaProvider,
      @Autowired(required = false) PasswordAuthenticationProvider passwordProvider,
      @Autowired(required = false) AuthenticationConverter passwordConverter,
      WechatQrAuthenticationProvider wechatQrProvider,
      OAuth2ErrorHandler oauth2ErrorHandler)
      throws Exception {

    OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

    // 构建认证转换器列表
    var converters = new ArrayList<AuthenticationConverter>();
    converters.add(new OAuth2AuthorizationCodeRequestAuthenticationConverter());
    converters.add(new OAuth2RefreshTokenAuthenticationConverter());
    converters.add(new OAuth2ClientCredentialsAuthenticationConverter());
    converters.add(new WechatQrAuthenticationConverter());

    // 添加密码模式转换器（如果可用）
    if (passwordConverter != null) {
      converters.add(passwordConverter);
    }

    // 添加验证码转换器
    converters.add(new OAuth2CaptchaAuthenticationConverter());

    http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
        .tokenEndpoint(
            tokenEndpoint -> {
              tokenEndpoint.accessTokenRequestConverter(
                  new DelegatingAuthenticationConverter(converters));
              tokenEndpoint.authenticationProvider(captchaProvider);
              tokenEndpoint.authenticationProvider(wechatQrProvider);
              tokenEndpoint.errorResponseHandler(oauth2ErrorHandler);

              // 添加密码模式提供者（如果可用）
              if (passwordProvider != null) {
                tokenEndpoint.authenticationProvider(passwordProvider);
              }
            });

    return http.build();
  }
}
