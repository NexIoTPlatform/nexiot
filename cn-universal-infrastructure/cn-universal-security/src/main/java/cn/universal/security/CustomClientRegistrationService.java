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

import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.token.WechatQrAuthenticationToken;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Primary
public class CustomClientRegistrationService implements RegisteredClientRepository {

  private static final Logger logger =
      LoggerFactory.getLogger(CustomClientRegistrationService.class);
  private String DEGAULT_CLIENT_ID = "web";
  private final OauthClientDetailsMapper oauthClientDetailsMapper;
  private final InMemoryRegisteredClientRepository fallbackRepository;

  public CustomClientRegistrationService(
      @Autowired(required = false) OauthClientDetailsMapper oauthClientDetailsMapper) {
    this.oauthClientDetailsMapper = oauthClientDetailsMapper;

    logger.info(
        "CustomClientRegistrationService 初始化，oauthClientDetailsMapper: {}",
        oauthClientDetailsMapper != null ? "可用" : "不可用");

    // 创建回退的内存客户端配置，保持与原有系统的兼容性
    RegisteredClient univIotClient =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("web")
            .clientSecret("{noop}web")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .authorizationGrantType(new AuthorizationGrantType("password"))
            .authorizationGrantType(new AuthorizationGrantType("captcha"))
            .authorizationGrantType(new AuthorizationGrantType("sms"))
            .authorizationGrantType(WechatQrAuthenticationToken.GRANT_TYPE)
            .redirectUri("http://127.0.0.1:8080/login/oauth2/code/cn-universal-client")
            .redirectUri("http://127.0.0.1:8080/authorized")
            .scope("read")
            .scope("write")
            .scope("all")
            .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofHours(1))
                    .refreshTokenTimeToLive(Duration.ofDays(1))
                    .build())
            .build();

    this.fallbackRepository = new InMemoryRegisteredClientRepository(univIotClient);
    logger.info("内存回退客户端配置已创建，client_id: web");
  }

  @Override
  public void save(RegisteredClient registeredClient) {
    // 这里可以实现保存逻辑，暂时不实现
  }

  @Override
  public RegisteredClient findById(String id) {
    logger.debug("查找客户端 by ID: {}", id);

    // 首先尝试从数据库查找,排除web
    if (oauthClientDetailsMapper != null && !DEGAULT_CLIENT_ID.equals(id)) {
      OAuth2ClientDetails clientDetails = findByClientIdFromDb(id);
      if (clientDetails != null) {
        logger.debug("从数据库找到客户端: {}", clientDetails.getClientId());
        return convertToRegisteredClient(clientDetails);
      }
    }

    // 如果数据库中没有找到，回退到内存配置
    logger.debug("数据库中没有找到客户端，回退到内存配置");
    return fallbackRepository.findById(id);
  }

  @Override
  public RegisteredClient findByClientId(String clientId) {
    logger.debug("查找客户端 by ClientId: {}", clientId);

    // 首先尝试从数据库查找
    if (oauthClientDetailsMapper != null && !DEGAULT_CLIENT_ID.equals(clientId)) {
      OAuth2ClientDetails clientDetails = findByClientIdFromDb(clientId);
      if (clientDetails != null) {
        logger.debug(
            "从数据库找到客户端: {}, 授权类型: {}",
            clientDetails.getClientId(),
            clientDetails.getAuthorizedGrantTypes());
        return convertToRegisteredClient(clientDetails);
      } else {
        logger.debug("数据库中没有找到客户端: {}", clientId);
      }
    } else {
      logger.debug("oauthClientDetailsMapper 不可用，跳过数据库查询");
    }

    // 如果数据库中没有找到，回退到内存配置
    logger.debug("回退到内存配置查找客户端: {}", clientId);
    RegisteredClient fallbackClient = fallbackRepository.findByClientId(clientId);
    if (fallbackClient != null) {
      logger.debug("从内存配置找到客户端: {}", clientId);
    } else {
      logger.debug("内存配置中也没有找到客户端: {}", clientId);
    }
    return fallbackClient;
  }

  private OAuth2ClientDetails findByClientIdFromDb(String clientId) {
    try {
      // 使用 selectOne 方法查询
      OAuth2ClientDetails query = new OAuth2ClientDetails();
      query.setClientId(clientId);
      OAuth2ClientDetails result = oauthClientDetailsMapper.selectOne(query);
      logger.debug("数据库查询结果: {}", result != null ? "找到" : "未找到");
      return result;
    } catch (Exception e) {
      logger.error("数据库查询异常: {}", e.getMessage(), e);
      return null;
    }
  }

  private RegisteredClient convertToRegisteredClient(OAuth2ClientDetails clientDetails) {
    logger.debug("转换客户端配置: {}", clientDetails.getClientId());

    // 解析授权类型
    Set<String> authorizedGrantTypes =
        parseCommaDelimitedString(clientDetails.getAuthorizedGrantTypes());

    // 解析作用域
    Set<String> scopes = parseCommaDelimitedString(clientDetails.getScope());

    // 解析重定向URI
    Set<String> redirectUris = parseCommaDelimitedString(clientDetails.getWebServerRedirectUri());

    // 解析权限
    Set<String> authorities = parseCommaDelimitedString(clientDetails.getAuthorities());

    logger.debug(
        "解析结果 - 授权类型: {}, 作用域: {}, 重定向URI: {}", authorizedGrantTypes, scopes, redirectUris);

    // 检查客户端密钥格式
    String clientSecret = clientDetails.getClientSecret();
    logger.debug(
        "客户端密钥: {}",
        clientSecret != null
            ? clientSecret.substring(0, Math.min(10, clientSecret.length())) + "..."
            : "null");

    // 处理不同格式的客户端密钥
    if (clientSecret != null && !DEGAULT_CLIENT_ID.equalsIgnoreCase(clientSecret)) {
      if (clientSecret.startsWith("$2a$")
          || clientSecret.startsWith("$2b$")
          || clientSecret.startsWith("$2y$")) {
        // BCrypt 格式，添加 {bcrypt} 前缀
        clientSecret = "{bcrypt}" + clientSecret;
        logger.debug(
            "添加 {bcrypt} 前缀后的客户端密钥: {}",
            clientSecret.substring(0, Math.min(15, clientSecret.length())) + "...");
      } else if (!clientSecret.startsWith("{noop}")
          && !clientSecret.startsWith("{bcrypt}")
          && !clientSecret.startsWith("{pbkdf2}")
          && !clientSecret.startsWith("{scrypt}")) {
        // 明文密码，添加 {noop} 前缀
        clientSecret = "{noop}" + clientSecret;
        logger.debug(
            "添加 {noop} 前缀后的客户端密钥: {}",
            clientSecret.substring(0, Math.min(15, clientSecret.length())) + "...");
      }
    }

    // 构建客户端设置
    ClientSettings.Builder clientSettingsBuilder =
        ClientSettings.builder()
            .requireAuthorizationConsent("1".equals(clientDetails.getAutoapprove()));

    // 构建令牌设置
    TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();
    if (clientDetails.getAccessTokenValidity() != null) {
      tokenSettingsBuilder.accessTokenTimeToLive(
          Duration.ofSeconds(clientDetails.getAccessTokenValidity()));
    }
    if (clientDetails.getRefreshTokenValidity() != null) {
      tokenSettingsBuilder.refreshTokenTimeToLive(
          Duration.ofSeconds(clientDetails.getRefreshTokenValidity()));
    }

    RegisteredClient registeredClient =
        RegisteredClient.withId(clientDetails.getClientId())
            .clientId(clientDetails.getClientId())
            .clientSecret(clientSecret)
            .clientAuthenticationMethods(
                authenticationMethods -> {
                  if (authorizedGrantTypes.contains("client_credentials")) {
                    authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                    authenticationMethods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                  }
                })
            .authorizationGrantTypes(
                authorizationGrantTypes -> {
                  if (authorizedGrantTypes.contains("password")) {
                    authorizationGrantTypes.add(AuthorizationGrantType.PASSWORD);
                  }
                  if (authorizedGrantTypes.contains("authorization_code")) {
                    authorizationGrantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                  }
                  if (authorizedGrantTypes.contains("refresh_token")) {
                    authorizationGrantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                  }
                  if (authorizedGrantTypes.contains("client_credentials")) {
                    authorizationGrantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                  }
                })
            .redirectUris(redirectUris::addAll)
            .scopes(scopes::addAll)
            .clientSettings(clientSettingsBuilder.build())
            .tokenSettings(tokenSettingsBuilder.build())
            .build();

    logger.debug(
        "创建的 RegisteredClient: clientId={}, hasClientSecret={}, authMethods={}, grantTypes={}",
        registeredClient.getClientId(),
        registeredClient.getClientSecret() != null,
        registeredClient.getClientAuthenticationMethods(),
        registeredClient.getAuthorizationGrantTypes());

    return registeredClient;
  }

  private Set<String> parseCommaDelimitedString(String value) {
    if (!StringUtils.hasText(value)) {
      return Collections.emptySet();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .collect(Collectors.toSet());
  }
}
