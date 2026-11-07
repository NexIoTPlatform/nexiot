package cn.universal.security.token;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

/** A browser-bound, single-use WeChat login session; contains no user-selected account. */
public final class WechatQrAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

  public static final AuthorizationGrantType GRANT_TYPE = new AuthorizationGrantType("wechat_qr");

  private final String sessionId;
  private String browserProof;

  public WechatQrAuthenticationToken(
      Authentication clientPrincipal, String sessionId, String browserProof) {
    super(GRANT_TYPE, clientPrincipal, Map.of());
    this.sessionId = sessionId;
    this.browserProof = browserProof;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getBrowserProof() {
    return browserProof;
  }

  @Override
  public void eraseCredentials() {
    super.eraseCredentials();
    browserProof = null;
  }
}
