package nexiot.web.ide.debug.plugins.core.servlet;

public interface MagicHttpSession {

  Object getAttribute(String key);

  void setAttribute(String key, Object value);

  <T> T getSession();
}
