package nexiot.web.ide.debug.plugins.core.servlet;

public interface MagicCookie {

  String getName();

  String getValue();

  <T> T getCookie();
}
