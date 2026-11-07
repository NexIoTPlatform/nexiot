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

package cn.universal.security;

import cn.hutool.core.util.StrUtil;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.TenantJwtValidator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

@Configuration
public class OAuth2ServerConfig {

  private static final String PLATFORM_TENANT_ID = "0";
  private static final String PLATFORM_TENANT_ROLE = "PLATFORM_ADMIN";
  private static final String PLATFORM_TENANT_NAME = "平台管理";

  @Value("${spring.security.oauth2.authorizationserver.issuer:http://localhost:9092}")
  private String issuer;

  @Bean
  public AuthorizationServerSettings AuthorizationServerSettings() {
    return AuthorizationServerSettings.builder().issuer(issuer).build();
  }

  @Bean
  public OAuth2AuthorizationService AuthorizationService() {
    return new InMemoryOAuth2AuthorizationService();
  }

  @Bean
  public OAuth2TokenGenerator<?> tokenGenerator(
      JWKSource<SecurityContext> jwkSource,
      OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
    JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
    jwtGenerator.setJwtCustomizer(jwtCustomizer);
    OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
    OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
    return new DelegatingOAuth2TokenGenerator(
        jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    KeyPair keyPair = generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    RSAKey rsaKey =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  @Bean
  public JwtDecoder jwtDecoder(
      JWKSource<SecurityContext> jwkSource,
      IoTUserService ioTUserService,
      IoTTenantMapper ioTTenantMapper,
      OauthClientDetailsMapper oauthClientDetailsMapper) {
    JwtDecoder decoder = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    if (decoder instanceof NimbusJwtDecoder nimbusJwtDecoder) {
      OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuer);
      OAuth2TokenValidator<Jwt> tenantValidator =
          new TenantJwtValidator(ioTUserService, ioTTenantMapper, oauthClientDetailsMapper);
      nimbusJwtDecoder.setJwtValidator(
          new DelegatingOAuth2TokenValidator<>(defaultValidator, tenantValidator));
    }
    return decoder;
  }

  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(
      IoTUserService ioTUserService,
      IoTTenantMapper ioTTenantMapper,
      OauthClientDetailsMapper oauthClientDetailsMapper) {
    return context -> {
      if (context.getPrincipal() != null
          && context.getPrincipal().getPrincipal()
              instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
        IoTUser currentUser = resolveCurrentUser(userDetails, ioTUserService);
        IoTTenant currentTenant = resolveCurrentTenant(currentUser, ioTTenantMapper);
        customizeUserClaims(context.getClaims(), userDetails, currentUser, currentTenant);
        return;
      }
      customizeClientClaims(context, oauthClientDetailsMapper, ioTUserService, ioTTenantMapper);
    };
  }

  private static IoTUser resolveCurrentUser(
      org.springframework.security.core.userdetails.UserDetails userDetails,
      IoTUserService ioTUserService) {
    IoTUser currentUser = ioTUserService.selectUserByUnionId(userDetails.getUsername());
    if (currentUser != null) {
      return currentUser;
    }
    return ioTUserService.selectUserByUserName(userDetails.getUsername());
  }

  static IoTTenant resolveCurrentTenant(
      IoTUser currentUser, IoTTenantMapper ioTTenantMapper) {
    if (currentUser != null && currentUser.isAdmin()) {
      return null;
    }
    String tenantId = resolveTenantId(currentUser);
    if (StrUtil.isBlank(tenantId)) {
      return null;
    }
    return ioTTenantMapper.selectByPrimaryKey(tenantId);
  }

  static void customizeUserClaims(
      JwtClaimsSet.Builder claims,
      org.springframework.security.core.userdetails.UserDetails userDetails,
      IoTUser currentUser,
      IoTTenant currentTenant) {
    String unionId =
        StrUtil.blankToDefault(
            currentUser != null ? currentUser.getUnionId() : null, userDetails.getUsername());
    String username =
        StrUtil.blankToDefault(
            currentUser != null ? currentUser.getUsername() : null, userDetails.getUsername());

    claims.subject(unionId);
    claims.claim("unionId", unionId);
    claims.claim("username", username);
    if (currentUser != null && currentUser.isAdmin()) {
      claims.claim("tenantId", PLATFORM_TENANT_ID);
      claims.claim("tenantCode", PLATFORM_TENANT_ID);
      claims.claim("tenantRole", PLATFORM_TENANT_ROLE);
      claims.claim("tenantName", PLATFORM_TENANT_NAME);
    } else {
      String tenantId = resolveTenantId(currentUser);
      addTextClaim(claims, "tenantId", tenantId);
      addTextClaim(
          claims,
          "tenantCode",
          currentTenant != null && StrUtil.isNotBlank(currentTenant.getTenantCode())
              ? currentTenant.getTenantCode()
              : tenantId);
      addTextClaim(claims, "tenantRole", currentUser != null ? currentUser.getTenantRole() : null);
      addTextClaim(
          claims, "tenantName", currentTenant != null ? currentTenant.getTenantName() : null);
    }

    Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
    claims.claim("roles", authorities.stream().map(GrantedAuthority::getAuthority).toList());
  }

  private static String resolveTenantId(IoTUser currentUser) {
    if (currentUser == null) {
      return null;
    }
    return currentUser.getTenantId() != null ? currentUser.getTenantId() : currentUser.getUnionId();
  }

  private static void customizeClientClaims(
      JwtEncodingContext context,
      OauthClientDetailsMapper oauthClientDetailsMapper,
      IoTUserService ioTUserService,
      IoTTenantMapper ioTTenantMapper) {
    if (context.getRegisteredClient() == null) {
      return;
    }
    String clientId = context.getRegisteredClient().getClientId();
    OAuth2ClientDetails clientDetails = oauthClientDetailsMapper.findByClientId(clientId);
    if (clientDetails == null) {
      return;
    }
    IoTUser owner =
        StrUtil.isBlank(clientDetails.getIotUnionId())
            ? null
            : ioTUserService.selectUserByUnionId(clientDetails.getIotUnionId());
    String tenantId =
        StrUtil.blankToDefault(clientDetails.getTenantId(), resolveTenantId(owner));
    IoTTenant tenant = tenantId == null ? null : ioTTenantMapper.selectByPrimaryKey(tenantId);

    JwtClaimsSet.Builder claims = context.getClaims();
    claims.subject(clientId);
    claims.claim("clientId", clientId);
    addTextClaim(claims, "unionId", clientDetails.getIotUnionId());
    addTextClaim(claims, "tenantId", tenantId);
    addTextClaim(
        claims,
        "tenantCode",
        tenant != null && tenant.getTenantCode() != null ? tenant.getTenantCode() : tenantId);
    addTextClaim(claims, "tenantRole", owner != null ? owner.getTenantRole() : null);
    addTextClaim(claims, "tenantName", tenant != null ? tenant.getTenantName() : null);
  }

  private static void addTextClaim(JwtClaimsSet.Builder claims, String name, String value) {
    if (StrUtil.isNotBlank(value)) {
      claims.claim(name, value);
    }
  }

  private static KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }
}
