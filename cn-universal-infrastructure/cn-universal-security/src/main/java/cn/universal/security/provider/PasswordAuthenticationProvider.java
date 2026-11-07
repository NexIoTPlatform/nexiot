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

package cn.universal.security.provider;

import cn.universal.common.constant.Constants;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.security.service.AdminLogService;
import cn.universal.security.support.TenantLoginResolver;
import cn.universal.security.token.PasswordAuthenticationToken;
import java.security.Principal;
import java.util.Collections;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

public class PasswordAuthenticationProvider implements AuthenticationProvider {

  private final OAuth2TokenGenerator<?> tokenGenerator;
  private final RegisteredClientRepository clientRepository;
  private final IoTUserMapper iotUserMapper;
  private final BCryptPasswordEncoder passwordEncoder;
  private final OAuth2AuthorizationService authorizationService;
  private final UserDetailsService userDetailsService;
  private final TenantLoginResolver tenantLoginResolver;
  private final AdminLogService adminLogService;

  public PasswordAuthenticationProvider(
      OAuth2TokenGenerator<?> tokenGenerator,
      RegisteredClientRepository clientRepository,
      IoTUserMapper iotUserMapper,
      OAuth2AuthorizationService authorizationService,
      UserDetailsService userDetailsService,
      TenantLoginResolver tenantLoginResolver,
      AdminLogService adminLogService) {
    Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
    Assert.notNull(clientRepository, "clientRepository cannot be null");
    Assert.notNull(iotUserMapper, "iotUserMapper cannot be null");
    Assert.notNull(authorizationService, "authorizationService cannot be null");
    Assert.notNull(userDetailsService, "userDetailsService cannot be null");
    Assert.notNull(tenantLoginResolver, "tenantLoginResolver cannot be null");
    Assert.notNull(adminLogService, "adminLogService cannot be null");
    this.tokenGenerator = tokenGenerator;
    this.clientRepository = clientRepository;
    this.iotUserMapper = iotUserMapper;
    this.authorizationService = authorizationService;
    this.userDetailsService = userDetailsService;
    this.tenantLoginResolver = tenantLoginResolver;
    this.adminLogService = adminLogService;
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    PasswordAuthenticationToken passwordAuthenticationToken =
        (PasswordAuthenticationToken) authentication;

    // 从客户端认证令牌中获取RegisteredClient
    OAuth2ClientAuthenticationToken clientPrincipal =
        (OAuth2ClientAuthenticationToken) passwordAuthenticationToken.getPrincipal();
    RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

    // 验证用户名和密码
    String username = passwordAuthenticationToken.getUsername();
    String password = passwordAuthenticationToken.getPassword();
    String tenantCode = passwordAuthenticationToken.getTenantCode();

    if (username == null || password == null) {
      throw loginFailure(username, "invalid_request", "用户名或密码不能为空");
    }

    IoTUser iotUser;
    try {
      iotUser = tenantLoginResolver.resolveLoginUser(username, tenantCode);
    } catch (OAuth2AuthenticationException ex) {
      adminLogService.recordLogininfor(
          username, Constants.LOGIN_FAIL, ex.getError().getDescription(), null);
      throw ex;
    }
    if (iotUser == null) {
      throw loginFailure(username, "invalid_request", "用户名或密码错误");
    }

    // 验证密码
    if (!passwordEncoder.matches(password, iotUser.getPassword())) {
      throw loginFailure(iotUser.getUnionId(), "invalid_request", "用户名或密码错误");
    }

    // 创建用户详情
    UserDetails userDetails = userDetailsService.loadUserByUsername(iotUser.getUnionId());
    if (userDetails.getAuthorities() == null || userDetails.getAuthorities().isEmpty()) {
      userDetails =
          org.springframework.security.core.userdetails.User.builder()
              .username(iotUser.getUnionId())
              .password(iotUser.getPassword())
              .authorities(Collections.singletonList(new SimpleGrantedAuthority("USER")))
              .build();
    }

    // 创建用户认证令牌
    UsernamePasswordAuthenticationToken userAuth =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    // 生成 OAuth2 令牌
    DefaultOAuth2TokenContext.Builder tokenContextBuilder =
        DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(userAuth)
            .authorizationServerContext(AuthorizationServerContextHolder.getContext())
            .authorizationGrantType(
                org.springframework.security.oauth2.core.AuthorizationGrantType.PASSWORD)
            .authorizationGrant(passwordAuthenticationToken);

    OAuth2TokenContext tokenContext =
        tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();
    OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);
    if (generatedAccessToken == null) {
      throw loginFailure(iotUser.getUnionId(), "server_error", "无法生成访问令牌");
    }

    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            generatedAccessToken.getTokenValue(),
            generatedAccessToken.getIssuedAt(),
            generatedAccessToken.getExpiresAt(),
            registeredClient.getScopes());

    OAuth2RefreshToken refreshToken = null;
    if (registeredClient
        .getAuthorizationGrantTypes()
        .contains(org.springframework.security.oauth2.core.AuthorizationGrantType.REFRESH_TOKEN)) {
      tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
      OAuth2Token generatedRefreshToken = tokenGenerator.generate(tokenContext);
      if (generatedRefreshToken != null) {
        refreshToken =
            new OAuth2RefreshToken(
                generatedRefreshToken.getTokenValue(),
                generatedRefreshToken.getIssuedAt(),
                generatedRefreshToken.getExpiresAt());
      }
    }

    // 保存授权信息
    OAuth2Authorization.Builder authorizationBuilder =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(userDetails.getUsername())
            .attribute(Principal.class.getName(), userAuth)
            .authorizationGrantType(
                org.springframework.security.oauth2.core.AuthorizationGrantType.PASSWORD)
            .authorizedScopes(registeredClient.getScopes());

    if (accessToken != null) {
      authorizationBuilder.accessToken(accessToken);
    }
    if (refreshToken != null) {
      authorizationBuilder.refreshToken(refreshToken);
    }

    OAuth2Authorization authorization = authorizationBuilder.build();
    authorizationService.save(authorization);
    adminLogService.recordLogininfor(
        iotUser.getUnionId(), Constants.LOGIN_SUCCESS, "密码登录成功", null);

    return new OAuth2AccessTokenAuthenticationToken(
        registeredClient, clientPrincipal, accessToken, refreshToken);
  }

  private OAuth2AuthenticationException loginFailure(
      String account, String errorCode, String message) {
    adminLogService.recordLogininfor(account, Constants.LOGIN_FAIL, message, null);
    return new OAuth2AuthenticationException(new OAuth2Error(errorCode, message, null));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return PasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
