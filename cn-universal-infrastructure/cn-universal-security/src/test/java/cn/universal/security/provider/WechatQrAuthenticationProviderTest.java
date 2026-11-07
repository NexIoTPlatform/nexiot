package cn.universal.security.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.universal.common.constant.Constants;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.security.service.AdminLogService;
import cn.universal.security.token.WechatQrAuthenticationToken;
import cn.universal.security.wechat.WechatAuthService;
import java.security.Principal;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

class WechatQrAuthenticationProviderTest {

  private final WechatAuthService sessions = mock(WechatAuthService.class);
  private final IoTUserMapper users = mock(IoTUserMapper.class);
  private final IoTTenantMapper tenants = mock(IoTTenantMapper.class);
  private final UserDetailsService details = mock(UserDetailsService.class);
  private final OAuth2TokenGenerator<?> generator = mock(OAuth2TokenGenerator.class);
  private final OAuth2AuthorizationService authorizations = mock(OAuth2AuthorizationService.class);
  private final AdminLogService log = mock(AdminLogService.class);
  private final IoTUser user =
      IoTUser.builder()
          .unionId("user-1")
          .username("demo")
          .tenantId("tenant-1")
          .identity(1)
          .status(0)
          .deleted(0)
          .build();
  private final IoTTenant tenant =
      IoTTenant.builder().tenantId("tenant-1").status(0).deleted(0).build();
  private WechatQrAuthenticationProvider provider;

  @BeforeEach
  void setUp() {
    provider =
        new WechatQrAuthenticationProvider(
            sessions, users, tenants, details, generator, authorizations, log);
    AuthorizationServerContextHolder.setContext(mock(AuthorizationServerContext.class));
    when(sessions.consumeLogin("session-1", "browser-secret")).thenReturn("user-1");
    when(users.selectOne(any(IoTUser.class))).thenReturn(user);
    when(tenants.selectByPrimaryKey("tenant-1")).thenReturn(tenant);
    when(details.loadUserByUsername("user-1"))
        .thenReturn(User.withUsername("user-1").password("unused").authorities("WEB").build());
  }

  @AfterEach
  void tearDown() {
    AuthorizationServerContextHolder.resetContext();
  }

  @Test
  void createsRefreshableAuthorizationWithClaimsAndNoBrowserProof() {
    Instant now = Instant.now();
    doAnswer(
            invocation -> {
              OAuth2TokenContext context = invocation.getArgument(0);
              assertEquals(
                  WechatQrAuthenticationToken.GRANT_TYPE, context.getAuthorizationGrantType());
              assertEquals("user-1", context.getPrincipal().getName());
              if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return Jwt.withTokenValue("jwt-access")
                    .header("alg", "RS256")
                    .subject("user-1")
                    .claim("tenantId", "tenant-1")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(300))
                    .build();
              }
              return new OAuth2RefreshToken("refresh-one", now, now.plusSeconds(600));
            })
        .when(generator)
        .generate(any());
    WechatQrAuthenticationToken request = request(client("web", true));

    OAuth2AccessTokenAuthenticationToken result =
        assertInstanceOf(
            OAuth2AccessTokenAuthenticationToken.class, provider.authenticate(request));

    assertEquals("jwt-access", result.getAccessToken().getTokenValue());
    assertEquals("refresh-one", result.getRefreshToken().getTokenValue());
    ArgumentCaptor<OAuth2Authorization> saved = ArgumentCaptor.forClass(OAuth2Authorization.class);
    verify(authorizations).save(saved.capture());
    assertEquals("user-1", saved.getValue().getPrincipalName());
    assertEquals(
        WechatQrAuthenticationToken.GRANT_TYPE, saved.getValue().getAuthorizationGrantType());
    assertEquals("tenant-1", saved.getValue().getAccessToken().getClaims().get("tenantId"));
    Authentication principal = saved.getValue().getAttribute(Principal.class.getName());
    assertEquals("user-1", principal.getName());
    assertFalse(saved.getValue().getAttributes().containsKey("browser_proof"));
    assertNull(request.getBrowserProof());
    verify(log).recordLogininfor("user-1", Constants.LOGIN_SUCCESS, "微信扫码登录成功", null);
  }

  @Test
  void rejectsOtherClientsBeforeConsumingSession() {
    assertEquals("unauthorized_client", rejected(request(client("other", true))));
    verifyNoInteractions(sessions, users, tenants, details, generator, authorizations);
  }

  @Test
  void requiresExplicitGrantPermission() {
    assertEquals("unauthorized_client", rejected(request(client("web", false))));
    verifyNoInteractions(sessions, generator, authorizations);
  }

  @Test
  void rejectsUnauthenticatedClient() {
    WechatQrAuthenticationToken request =
        new WechatQrAuthenticationToken(
            new OAuth2ClientAuthenticationToken(
                "web", ClientAuthenticationMethod.CLIENT_SECRET_POST, "web", Map.of()),
            "session-1",
            "browser-secret");
    assertEquals("invalid_client", rejected(request));
    verifyNoInteractions(sessions, generator, authorizations);
    assertNull(request.getBrowserProof());
  }

  @Test
  void replayedOrUnconfirmedSessionCannotIssueTokens() {
    when(sessions.consumeLogin("session-1", "browser-secret"))
        .thenThrow(new OAuth2AuthenticationException(new OAuth2Error("invalid_grant")));
    assertEquals("invalid_grant", rejected(request(client("web", true))));
    verifyNoInteractions(users, tenants, details, generator, authorizations);
  }

  @Test
  void refusesDeletedUserEvenAfterSessionWasConfirmed() {
    user.setDeleted(1);
    assertEquals("invalid_grant", rejected(request(client("web", true))));
    verifyNoInteractions(tenants, details, generator, authorizations);
  }

  @Test
  void refusesFrozenUserAndPlatformAdministrator() {
    user.setStatus(1);
    assertEquals("invalid_grant", rejected(request(client("web", true))));
    user.setStatus(0);
    user.setIdentity(0);
    assertEquals("invalid_grant", rejected(request(client("web", true))));
    verifyNoInteractions(tenants, details, generator, authorizations);
  }

  @Test
  void refusesExpiredDisabledAndDeletedTenants() {
    tenant.setExpireTime(Date.from(Instant.now().minusSeconds(1)));
    assertEquals("invalid_grant", rejected(request(client("web", true))));
    tenant.setExpireTime(null);
    tenant.setStatus(1);
    assertEquals("invalid_grant", rejected(request(client("web", true))));
    tenant.setStatus(0);
    tenant.setDeleted(1);
    assertEquals("invalid_grant", rejected(request(client("web", true))));
    verifyNoInteractions(details, generator, authorizations);
  }

  @Test
  void neverSignsForAUserDetailsIdentityDifferentFromConsumedUser() {
    when(details.loadUserByUsername("user-1"))
        .thenReturn(
            User.withUsername("another-user").password("unused").authorities("WEB").build());
    assertEquals("invalid_grant", rejected(request(client("web", true))));
    verifyNoInteractions(generator, authorizations);
  }

  @Test
  void tokenGenerationFailureNeverSavesAuthorization() {
    assertEquals("server_error", rejected(request(client("web", true))));
    verifyNoInteractions(authorizations);
  }

  private String rejected(WechatQrAuthenticationToken request) {
    return assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(request))
        .getError()
        .getErrorCode();
  }

  private WechatQrAuthenticationToken request(RegisteredClient client) {
    return new WechatQrAuthenticationToken(
        new OAuth2ClientAuthenticationToken(
            client, ClientAuthenticationMethod.CLIENT_SECRET_POST, null),
        "session-1",
        "browser-secret");
  }

  private RegisteredClient client(String id, boolean wechatGrant) {
    RegisteredClient.Builder builder =
        RegisteredClient.withId(id)
            .clientId(id)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .scope("web");
    if (wechatGrant) {
      builder.authorizationGrantType(WechatQrAuthenticationToken.GRANT_TYPE);
    }
    return builder.build();
  }
}
