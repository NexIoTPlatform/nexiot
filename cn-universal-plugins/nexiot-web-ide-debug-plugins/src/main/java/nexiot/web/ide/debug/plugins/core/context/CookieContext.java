package nexiot.web.ide.debug.plugins.core.context;

import java.util.HashMap;
import nexiot.web.ide.debug.plugins.core.servlet.MagicCookie;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletRequest;

/**
 * Cookie Context 用于脚本中获取cookie信息
 *
 * @author mxd
 */
public class CookieContext extends HashMap<String, String> {

  private final MagicCookie[] cookies;

  public CookieContext(MagicHttpServletRequest request) {
    this.cookies = request.getCookies();
  }

  @Override
  public String get(Object key) {
    if (cookies != null) {
      for (MagicCookie cookie : cookies) {
        if (cookie.getName().equalsIgnoreCase("" + key)) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }
}
