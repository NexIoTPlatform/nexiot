package nexiot.web.ide.debug.plugins.utils;

import java.security.Principal;
import java.util.Optional;
import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletRequest;
import nexiot.web.ide.debug.plugins.core.servlet.MagicRequestContextHolder;

/**
 * Web相关工具类
 *
 * @author mxd
 */
public class WebUtils {

  public static MagicRequestContextHolder magicRequestContextHolder;

  public static String currentUserName() {
    Optional<MagicHttpServletRequest> request =
        Optional.ofNullable(magicRequestContextHolder.getRequest());
    return request
        .map(r -> (MagicUser) r.getAttribute(Constants.ATTRIBUTE_MAGIC_USER))
        .map(MagicUser::getUsername)
        .orElseGet(
            () ->
                request
                    .map(MagicHttpServletRequest::getUserPrincipal)
                    .map(Principal::getName)
                    .orElse(null));
  }
}
