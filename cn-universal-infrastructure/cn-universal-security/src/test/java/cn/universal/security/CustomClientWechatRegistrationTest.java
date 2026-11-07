package cn.universal.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.universal.persistence.mapper.OauthClientDetailsMapper;
import cn.universal.security.token.WechatQrAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

class CustomClientWechatRegistrationTest {

  @Test
  void legacyWebClientSupportsWechatAndRefreshWithoutDatabaseChanges() {
    OauthClientDetailsMapper mapper = mock(OauthClientDetailsMapper.class);
    CustomClientRegistrationService clients = new CustomClientRegistrationService(mapper);

    RegisteredClient web = clients.findByClientId("web");

    assertTrue(web.getAuthorizationGrantTypes().contains(WechatQrAuthenticationToken.GRANT_TYPE));
    assertTrue(web.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN));
    assertTrue(
        web.getClientAuthenticationMethods()
            .contains(ClientAuthenticationMethod.CLIENT_SECRET_POST));
    verifyNoInteractions(mapper);
    assertNotNull(clients.findById(web.getId()));
  }
}
