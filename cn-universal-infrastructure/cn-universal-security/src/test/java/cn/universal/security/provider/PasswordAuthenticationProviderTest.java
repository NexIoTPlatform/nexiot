package cn.universal.security.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.universal.common.constant.Constants;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.security.service.AdminLogService;
import cn.universal.security.support.TenantLoginResolver;
import cn.universal.security.token.PasswordAuthenticationToken;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

class PasswordAuthenticationProviderTest {

  private static final String PASSWORD = "Login-test-123!";
  private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);

  private final TenantLoginResolver tenantLoginResolver = mock(TenantLoginResolver.class);
  private final AdminLogService adminLogService = mock(AdminLogService.class);
  private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
  private final OAuth2AuthorizationService authorizationService =
      mock(OAuth2AuthorizationService.class);
  private final OAuth2TokenGenerator<?> tokenGenerator = mock(OAuth2TokenGenerator.class);
  private final RegisteredClient registeredClient =
      RegisteredClient.withId("web-client")
          .clientId("web")
          .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
          .authorizationGrantType(AuthorizationGrantType.PASSWORD)
          .scope("web")
          .build();
  private final IoTUser user =
      IoTUser.builder()
          .username("shared-name")
          .unionId("tenant-user-123")
          .password(PASSWORD_HASH)
          .build();
  private PasswordAuthenticationProvider provider;

  @BeforeEach
  void setUp() {
    provider =
        new PasswordAuthenticationProvider(
            tokenGenerator,
            mock(RegisteredClientRepository.class),
            mock(IoTUserMapper.class),
            authorizationService,
            userDetailsService,
            tenantLoginResolver,
            adminLogService);
    AuthorizationServerContextHolder.setContext(mock(AuthorizationServerContext.class));
  }

  @AfterEach
  void tearDown() {
    AuthorizationServerContextHolder.resetContext();
  }

  @Test
  void successfulLoginRecordsUnionIdAfterAuthorizationIsSaved() {
    prepareUser();
    Instant now = Instant.now();
    doReturn(
            new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token", now, now.plusSeconds(300)))
        .when(tokenGenerator)
        .generate(any());

    Authentication result = provider.authenticate(request("shared-name", PASSWORD));

    OAuth2AccessTokenAuthenticationToken accessToken =
        assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, result);
    assertEquals("access-token", accessToken.getAccessToken().getTokenValue());
    InOrder order = inOrder(authorizationService, adminLogService);
    order.verify(authorizationService).save(any(OAuth2Authorization.class));
    order
        .verify(adminLogService)
        .recordLogininfor("tenant-user-123", Constants.LOGIN_SUCCESS, "密码登录成功", null);
    verifyNoMoreInteractions(adminLogService);
  }

  @Test
  void incorrectPasswordRecordsFailureUsingResolvedUnionId() {
    when(tenantLoginResolver.resolveLoginUser("shared-name", "test")).thenReturn(user);

    OAuth2AuthenticationException error =
        assertThrows(
            OAuth2AuthenticationException.class,
            () -> provider.authenticate(request("shared-name", "incorrect-password")));

    assertEquals("用户名或密码错误", error.getError().getDescription());
    verify(adminLogService)
        .recordLogininfor("tenant-user-123", Constants.LOGIN_FAIL, "用户名或密码错误", null);
    verifyNoMoreInteractions(adminLogService);
    verifyNoInteractions(userDetailsService, tokenGenerator, authorizationService);
  }

  @Test
  void unknownAccountRecordsAttemptedAccount() {
    assertThrows(
        OAuth2AuthenticationException.class,
        () -> provider.authenticate(request("unknown-account", PASSWORD)));

    verify(adminLogService)
        .recordLogininfor("unknown-account", Constants.LOGIN_FAIL, "用户名或密码错误", null);
    verifyNoMoreInteractions(adminLogService);
    verifyNoInteractions(userDetailsService, tokenGenerator, authorizationService);
  }

  @Test
  void tenantRejectionRecordsReasonAndPreservesOriginalError() {
    OAuth2AuthenticationException rejected =
        new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "当前租户已停用", null));
    when(tenantLoginResolver.resolveLoginUser("shared-name", "test")).thenThrow(rejected);

    assertSame(
        rejected,
        assertThrows(
            OAuth2AuthenticationException.class,
            () -> provider.authenticate(request("shared-name", PASSWORD))));

    verify(adminLogService)
        .recordLogininfor("shared-name", Constants.LOGIN_FAIL, "当前租户已停用", null);
    verifyNoMoreInteractions(adminLogService);
    verifyNoInteractions(userDetailsService, tokenGenerator, authorizationService);
  }

  @Test
  void missingPasswordRecordsFailure() {
    assertThrows(
        OAuth2AuthenticationException.class,
        () -> provider.authenticate(request("shared-name", null)));

    verify(adminLogService)
        .recordLogininfor("shared-name", Constants.LOGIN_FAIL, "用户名或密码不能为空", null);
    verifyNoMoreInteractions(adminLogService);
    verifyNoInteractions(tenantLoginResolver, userDetailsService, tokenGenerator, authorizationService);
  }

  @Test
  void tokenGenerationFailureDoesNotRecordSuccessfulLogin() {
    prepareUser();

    OAuth2AuthenticationException error =
        assertThrows(
            OAuth2AuthenticationException.class,
            () -> provider.authenticate(request("shared-name", PASSWORD)));

    assertEquals("server_error", error.getError().getErrorCode());
    verify(adminLogService)
        .recordLogininfor("tenant-user-123", Constants.LOGIN_FAIL, "无法生成访问令牌", null);
    verifyNoMoreInteractions(adminLogService);
    verifyNoInteractions(authorizationService);
  }

  private void prepareUser() {
    when(tenantLoginResolver.resolveLoginUser("shared-name", "test")).thenReturn(user);
    when(userDetailsService.loadUserByUsername("tenant-user-123"))
        .thenReturn(
            User.withUsername("tenant-user-123")
                .password(PASSWORD_HASH)
                .authorities("WEB")
                .build());
  }

  private PasswordAuthenticationToken request(String username, String password) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("username", username);
    parameters.put("password", password);
    parameters.put("tenantCode", "test");
    return new PasswordAuthenticationToken(
        new OAuth2ClientAuthenticationToken(
            registeredClient, ClientAuthenticationMethod.CLIENT_SECRET_POST, null),
        parameters);
  }
}
