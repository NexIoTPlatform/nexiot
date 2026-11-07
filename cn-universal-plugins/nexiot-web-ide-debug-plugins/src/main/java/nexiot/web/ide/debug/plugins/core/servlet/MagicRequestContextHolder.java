package nexiot.web.ide.debug.plugins.core.servlet;

import java.util.function.Function;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public interface MagicRequestContextHolder {

  default <R> R convert(Function<ServletRequestAttributes, R> function) {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes instanceof ServletRequestAttributes) {
      ServletRequestAttributes servletRequestAttributes =
          ((ServletRequestAttributes) requestAttributes);
      return function.apply(servletRequestAttributes);
    }
    return null;
  }

  MagicHttpServletRequest getRequest();

  MagicHttpServletResponse getResponse();
}
