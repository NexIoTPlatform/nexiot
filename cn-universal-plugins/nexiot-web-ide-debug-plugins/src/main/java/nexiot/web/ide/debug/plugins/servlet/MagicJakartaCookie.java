package nexiot.web.ide.debug.plugins.servlet;

import jakarta.servlet.http.Cookie;
import nexiot.web.ide.debug.plugins.core.servlet.MagicCookie;

public class MagicJakartaCookie implements MagicCookie {

  private final Cookie cookie;

  public MagicJakartaCookie(Cookie cookie) {
    this.cookie = cookie;
  }

  @Override
  public String getName() {
    return cookie.getName();
  }

  @Override
  public String getValue() {
    return cookie.getValue();
  }

  @Override
  public <T> T getCookie() {
    return (T) cookie;
  }
}
