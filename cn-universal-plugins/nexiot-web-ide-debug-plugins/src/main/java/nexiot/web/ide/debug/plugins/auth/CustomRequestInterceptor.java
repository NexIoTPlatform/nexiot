package nexiot.web.ide.debug.plugins.auth;

import cn.universal.core.engine.MagicScriptContext;
import lombok.extern.slf4j.Slf4j;
import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.interceptor.Authorization;
import nexiot.web.ide.debug.plugins.core.interceptor.AuthorizationInterceptor;
import nexiot.web.ide.debug.plugins.core.interceptor.RequestInterceptor;
import nexiot.web.ide.debug.plugins.core.model.ApiInfo;
import nexiot.web.ide.debug.plugins.core.model.JsonBean;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletRequest;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 自定义未授权访问 */
@Component
@Slf4j
public class CustomRequestInterceptor implements RequestInterceptor {
  @Autowired private AuthorizationInterceptor authorizationInterceptor;

  @Override
  public Object preHandle(
      ApiInfo info,
      MagicScriptContext context,
      MagicHttpServletRequest request,
      MagicHttpServletResponse response)
      throws Exception {
    String token = request.getHeader(Constants.MAGIC_TOKEN_HEADER);

    if (token == null || token.isEmpty()) {
      return new JsonBean<>(403, "未授权访问");
    }
    MagicUser user = authorizationInterceptor.getUserByToken(token);
    if (MagicSsoSessionService.supports(token)) {
      return authorizationInterceptor.allowVisit(user, request, Authorization.VIEW, info)
          ? null
          : new JsonBean<>(403, "你没有调用权限");
    }
    String username = user.getUsername();
    if (username == null || username.isEmpty()) {
      return new JsonBean<>(403, "用户无权限");
    }
    if (!username.equals(info.getCreateBy())) {
      return new JsonBean<>(403, "你没有调用权限");
    }
    log.info("info={},context={}", info, context);
    return null;
  }
}
