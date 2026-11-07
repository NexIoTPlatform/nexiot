package nexiot.web.ide.debug.plugins.core.context;

import java.util.HashMap;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpSession;

/**
 * Session Context 用于脚本中获取Session信息
 *
 * @author mxd
 */
public class SessionContext extends HashMap<String, Object> {

  private final MagicHttpSession session;

  public SessionContext(MagicHttpSession session) {
    this.session = session;
  }

  @Override
  public Object get(Object key) {
    return session != null ? session.getAttribute(key.toString()) : null;
  }

  @Override
  public Object put(String key, Object value) {
    Object oldValue = session.getAttribute(key);
    session.setAttribute(key, value);
    return oldValue;
  }
}
