package nexiot.web.ide.debug.plugins.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nexiot.web.ide.debug.plugins.core.config.MagicCorsFilter;
import nexiot.web.ide.debug.plugins.core.interceptor.AuthorizationInterceptor;
import nexiot.web.ide.debug.plugins.core.interceptor.MagicWebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

public class MagicJakartaWebRequestInterceptor extends MagicWebRequestInterceptor
    implements HandlerInterceptor {

  public MagicJakartaWebRequestInterceptor(
      MagicCorsFilter magicCorsFilter, AuthorizationInterceptor authorizationInterceptor) {
    super(magicCorsFilter, authorizationInterceptor);
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    super.handle(
        handler,
        new MagicJakartaHttpServletRequest(request, null),
        new MagicJakartaHttpServletResponse(response));
    return true;
  }
}
