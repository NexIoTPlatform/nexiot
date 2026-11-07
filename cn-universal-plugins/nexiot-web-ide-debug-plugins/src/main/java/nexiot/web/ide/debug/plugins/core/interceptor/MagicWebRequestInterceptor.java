package nexiot.web.ide.debug.plugins.core.interceptor;

import nexiot.web.ide.debug.plugins.core.annotation.Valid;
import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.config.JsonCodeConstants;
import nexiot.web.ide.debug.plugins.core.config.MagicCorsFilter;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.exception.InvalidArgumentException;
import nexiot.web.ide.debug.plugins.core.exception.MagicLoginException;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletRequest;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletResponse;
import nexiot.web.ide.debug.plugins.core.web.MagicController;
import org.springframework.web.method.HandlerMethod;

public abstract class MagicWebRequestInterceptor {

  private final MagicCorsFilter magicCorsFilter;

  private final AuthorizationInterceptor authorizationInterceptor;

  public MagicWebRequestInterceptor(
      MagicCorsFilter magicCorsFilter, AuthorizationInterceptor authorizationInterceptor) {
    this.magicCorsFilter = magicCorsFilter;
    this.authorizationInterceptor = authorizationInterceptor;
  }

  public void handle(
      Object handler, MagicHttpServletRequest request, MagicHttpServletResponse response)
      throws MagicLoginException {
    HandlerMethod handlerMethod;
    if (handler instanceof HandlerMethod) {
      handlerMethod = (HandlerMethod) handler;
      handler = handlerMethod.getBean();
      if (handler instanceof MagicController) {
        if (magicCorsFilter != null) {
          magicCorsFilter.process(request, response);
        }
        Valid valid = handlerMethod.getMethodAnnotation(Valid.class);
        boolean requiredLogin = authorizationInterceptor.requireLogin();
        boolean validRequiredLogin = (valid == null || valid.requireLogin());
        if (validRequiredLogin && requiredLogin) {
          request.setAttribute(
              Constants.ATTRIBUTE_MAGIC_USER,
              authorizationInterceptor.getUserByToken(
                  request.getHeader(Constants.MAGIC_TOKEN_HEADER)));
        }
        MagicUser user = (MagicUser) request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
        if (user != null
            && !authorizationInterceptor.allowRequest(
                user,
                handlerMethod.getMethod().getDeclaringClass().getSimpleName(),
                handlerMethod.getMethod().getName())) {
          throw new InvalidArgumentException(JsonCodeConstants.PERMISSION_INVALID);
        }
        ((MagicController) handler).doValid(request, valid);
      }
    }
  }
}
