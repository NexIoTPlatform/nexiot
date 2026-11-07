package cn.universal.security.provider;

import cn.universal.common.constant.Constants;
import cn.universal.common.constant.IoTConstant;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.security.service.AdminLogService;
import cn.universal.security.token.WechatQrAuthenticationToken;
import cn.universal.security.wechat.WechatAuthService;
import java.security.Principal;
import java.util.Date;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public final class WechatQrAuthenticationProvider implements AuthenticationProvider {

  private final WechatAuthService wechatAuthService;
  private final IoTUserMapper userMapper;
  private final IoTTenantMapper tenantMapper;
  private final UserDetailsService userDetailsService;
  private final OAuth2TokenGenerator<?> tokenGenerator;
  private final OAuth2AuthorizationService authorizationService;
  private final AdminLogService adminLogService;

  public WechatQrAuthenticationProvider(
      WechatAuthService wechatAuthService,
      IoTUserMapper userMapper,
      IoTTenantMapper tenantMapper,
      UserDetailsService userDetailsService,
      OAuth2TokenGenerator<?> tokenGenerator,
      OAuth2AuthorizationService authorizationService,
      AdminLogService adminLogService) {
    this.wechatAuthService = wechatAuthService;
    this.userMapper = userMapper;
    this.tenantMapper = tenantMapper;
    this.userDetailsService = userDetailsService;
    this.tokenGenerator = tokenGenerator;
    this.authorizationService = authorizationService;
    this.adminLogService = adminLogService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    WechatQrAuthenticationToken request = (WechatQrAuthenticationToken) authentication;
    String account = null;
    try {
      OAuth2ClientAuthenticationToken clientPrincipal = requireClient(request);
      RegisteredClient client = clientPrincipal.getRegisteredClient();
      if (!StringUtils.hasText(request.getSessionId())
          || !StringUtils.hasText(request.getBrowserProof())) {
        throw failure(OAuth2ErrorCodes.INVALID_REQUEST, "扫码登录参数不完整");
      }
      account = wechatAuthService.consumeLogin(request.getSessionId(), request.getBrowserProof());
      if (!StringUtils.hasText(account)) {
        throw failure(OAuth2ErrorCodes.INVALID_GRANT, "扫码登录未完成或已失效");
      }
      IoTUser user = userMapper.selectOne(IoTUser.builder().unionId(account).deleted(0).build());
      validateUserAndTenant(user, account);
      UserDetails details = userDetailsService.loadUserByUsername(account);
      if (!account.equals(details.getUsername())
          || !details.isEnabled()
          || !details.isAccountNonLocked()
          || !details.isAccountNonExpired()) {
        throw failure(OAuth2ErrorCodes.INVALID_GRANT, "当前账号不可登录");
      }
      Authentication userPrincipal =
          UsernamePasswordAuthenticationToken.authenticated(
              details, null, details.getAuthorities());
      DefaultOAuth2TokenContext.Builder context =
          DefaultOAuth2TokenContext.builder()
              .registeredClient(client)
              .principal(userPrincipal)
              .authorizedScopes(client.getScopes())
              .authorizationServerContext(AuthorizationServerContextHolder.getContext())
              .authorizationGrantType(WechatQrAuthenticationToken.GRANT_TYPE)
              .authorizationGrant(request);
      OAuth2Token generated =
          tokenGenerator.generate(context.tokenType(OAuth2TokenType.ACCESS_TOKEN).build());
      if (generated == null) {
        throw failure(OAuth2ErrorCodes.SERVER_ERROR, "无法生成访问令牌");
      }
      OAuth2AccessToken accessToken =
          new OAuth2AccessToken(
              OAuth2AccessToken.TokenType.BEARER,
              generated.getTokenValue(),
              generated.getIssuedAt(),
              generated.getExpiresAt(),
              client.getScopes());
      OAuth2Authorization.Builder authorization =
          OAuth2Authorization.withRegisteredClient(client)
              .principalName(account)
              .attribute(Principal.class.getName(), userPrincipal)
              .authorizationGrantType(WechatQrAuthenticationToken.GRANT_TYPE)
              .authorizedScopes(client.getScopes());
      authorization.token(
          accessToken,
          metadata -> {
            if (generated instanceof ClaimAccessor claims) {
              metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims.getClaims());
            }
          });
      OAuth2RefreshToken refreshToken = null;
      if (client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
        OAuth2Token refresh =
            tokenGenerator.generate(context.tokenType(OAuth2TokenType.REFRESH_TOKEN).build());
        if (!(refresh instanceof OAuth2RefreshToken)) {
          throw failure(OAuth2ErrorCodes.SERVER_ERROR, "无法生成刷新令牌");
        }
        refreshToken = (OAuth2RefreshToken) refresh;
        authorization.refreshToken(refreshToken);
      }
      authorizationService.save(authorization.build());
      adminLogService.recordLogininfor(account, Constants.LOGIN_SUCCESS, "微信扫码登录成功", null);
      return new OAuth2AccessTokenAuthenticationToken(
          client, clientPrincipal, accessToken, refreshToken);
    } catch (OAuth2AuthenticationException ex) {
      adminLogService.recordLogininfor(account, Constants.LOGIN_FAIL, "微信扫码登录失败", null);
      throw ex;
    } catch (RuntimeException ex) {
      adminLogService.recordLogininfor(account, Constants.LOGIN_FAIL, "微信扫码登录失败", null);
      throw failure(OAuth2ErrorCodes.INVALID_GRANT, "扫码登录未完成或已失效");
    } finally {
      request.eraseCredentials();
    }
  }

  private OAuth2ClientAuthenticationToken requireClient(WechatQrAuthenticationToken request) {
    if (!(request.getPrincipal() instanceof OAuth2ClientAuthenticationToken client)
        || !client.isAuthenticated()
        || client.getRegisteredClient() == null) {
      throw failure(OAuth2ErrorCodes.INVALID_CLIENT, "客户端认证失败");
    }
    RegisteredClient registration = client.getRegisteredClient();
    if (!"web".equals(registration.getClientId())
        || !registration
            .getAuthorizationGrantTypes()
            .contains(WechatQrAuthenticationToken.GRANT_TYPE)) {
      throw failure(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, "客户端不支持微信扫码登录");
    }
    return client;
  }

  private void validateUserAndTenant(IoTUser user, String unionId) {
    if (user == null
        || !unionId.equals(user.getUnionId())
        || !Integer.valueOf(0).equals(user.getDeleted())
        || !IoTConstant.NORMAL.equals(user.getStatus())
        || user.isAdmin()
        || !StringUtils.hasText(user.getTenantId())) {
      throw failure(OAuth2ErrorCodes.INVALID_GRANT, "当前账号不可登录");
    }
    IoTTenant tenant = tenantMapper.selectByPrimaryKey(user.getTenantId());
    if (tenant == null
        || Integer.valueOf(1).equals(tenant.getDeleted())
        || Integer.valueOf(1).equals(tenant.getStatus())
        || (tenant.getExpireTime() != null && !tenant.getExpireTime().after(new Date()))) {
      throw failure(OAuth2ErrorCodes.INVALID_GRANT, "当前租户不可登录");
    }
  }

  private OAuth2AuthenticationException failure(String code, String description) {
    return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return WechatQrAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
