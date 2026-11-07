package cn.universal.security.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.universal.security.token.WechatQrAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

class WechatQrAuthenticationConverterTest {

  private final WechatQrAuthenticationConverter converter = new WechatQrAuthenticationConverter();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void usesAuthenticatedClientAndOnlyBrowserSessionIdentity() {
    authenticateClient("web");
    MockHttpServletRequest request = request();
    request.addParameter("username", "another-user");
    request.addParameter("tenantCode", "another-tenant");

    WechatQrAuthenticationToken token =
        assertInstanceOf(WechatQrAuthenticationToken.class, converter.convert(request));

    assertEquals("session-one", token.getSessionId());
    assertEquals("private-browser-proof", token.getBrowserProof());
    assertEquals(0, token.getAdditionalParameters().size());
    token.eraseCredentials();
    assertNull(token.getBrowserProof());
  }

  @Test
  void ignoresOtherGrantsAndNonPostRequests() {
    MockHttpServletRequest request = request();
    request.setParameter("grant_type", "password");
    assertNull(converter.convert(request));
    request.setParameter("grant_type", "wechat_qr");
    request.setMethod("GET");
    assertNull(converter.convert(request));
  }

  @Test
  void clientIdAloneCannotAuthenticateAClient() {
    MockHttpServletRequest request = request();
    request.addParameter("client_secret", "web");
    assertEquals("invalid_client", rejected(request));
  }

  @Test
  void rejectsOtherAuthenticatedClientsAndConflictingClientId() {
    authenticateClient("other-client");
    assertEquals("unauthorized_client", rejected(request()));
    authenticateClient("web");
    MockHttpServletRequest request = request();
    request.setParameter("client_id", "other-client");
    assertEquals("invalid_client", rejected(request));
  }

  @Test
  void rejectsRepeatedParametersAndEmptyOrOversizedProof() {
    authenticateClient("web");
    for (String parameter : new String[] {"grant_type", "session_id", "browser_proof"}) {
      MockHttpServletRequest request = request();
      request.addParameter(parameter, "another-value");
      assertEquals("invalid_request", rejected(request));
    }
    MockHttpServletRequest blank = request();
    blank.setParameter("browser_proof", " ");
    assertEquals("invalid_request", rejected(blank));
    MockHttpServletRequest oversized = request();
    oversized.setParameter("browser_proof", "a".repeat(257));
    assertEquals("invalid_request", rejected(oversized));
  }

  private String rejected(MockHttpServletRequest request) {
    return assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(request))
        .getError()
        .getErrorCode();
  }

  private MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
    request.addParameter("grant_type", "wechat_qr");
    request.addParameter("client_id", "web");
    request.addParameter("session_id", "session-one");
    request.addParameter("browser_proof", "private-browser-proof");
    return request;
  }

  private void authenticateClient(String clientId) {
    RegisteredClient client =
        RegisteredClient.withId(clientId)
            .clientId(clientId)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(WechatQrAuthenticationToken.GRANT_TYPE)
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new OAuth2ClientAuthenticationToken(
                client, ClientAuthenticationMethod.CLIENT_SECRET_POST, null));
  }
}
