package cn.universal.security.converter;

import cn.universal.security.token.WechatQrAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

public final class WechatQrAuthenticationConverter implements AuthenticationConverter {

  @Override
  public Authentication convert(HttpServletRequest request) {
    if (!"POST".equals(request.getMethod())
        || !WechatQrAuthenticationToken.GRANT_TYPE
            .getValue()
            .equals(request.getParameter("grant_type"))) {
      return null;
    }
    singleParameter(request, "grant_type", 32);
    String sessionId = singleParameter(request, "session_id", 128);
    String browserProof = singleParameter(request, "browser_proof", 256);
    Authentication principal = SecurityContextHolder.getContext().getAuthentication();
    if (!(principal instanceof OAuth2ClientAuthenticationToken client)
        || !client.isAuthenticated()
        || client.getRegisteredClient() == null) {
      throw failure(OAuth2ErrorCodes.INVALID_CLIENT);
    }
    if (!"web".equals(client.getRegisteredClient().getClientId())) {
      throw failure(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
    }
    String[] clientIds = request.getParameterValues("client_id");
    if (clientIds != null && (clientIds.length != 1 || !"web".equals(clientIds[0]))) {
      throw failure(OAuth2ErrorCodes.INVALID_CLIENT);
    }
    return new WechatQrAuthenticationToken(client, sessionId, browserProof);
  }

  private String singleParameter(HttpServletRequest request, String name, int maxLength) {
    String[] values = request.getParameterValues(name);
    if (values == null
        || values.length != 1
        || !StringUtils.hasText(values[0])
        || values[0].length() > maxLength) {
      throw failure(OAuth2ErrorCodes.INVALID_REQUEST);
    }
    return values[0];
  }

  private OAuth2AuthenticationException failure(String errorCode) {
    return new OAuth2AuthenticationException(new OAuth2Error(errorCode));
  }
}
