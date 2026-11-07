package nexiot.web.ide.debug.plugins.servlet;

import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletRequest;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletResponse;
import nexiot.web.ide.debug.plugins.core.servlet.MagicRequestContextHolder;
import org.springframework.web.multipart.MultipartResolver;

public class MagicJakartaRequestContextHolder implements MagicRequestContextHolder {

  private final MultipartResolver multipartResolver;

  public MagicJakartaRequestContextHolder(MultipartResolver multipartResolver) {
    this.multipartResolver = multipartResolver;
  }

  @Override
  public MagicHttpServletRequest getRequest() {
    return convert(
        servletRequestAttributes ->
            new MagicJakartaHttpServletRequest(
                servletRequestAttributes.getRequest(), multipartResolver));
  }

  @Override
  public MagicHttpServletResponse getResponse() {
    return convert(
        servletRequestAttributes ->
            new MagicJakartaHttpServletResponse(servletRequestAttributes.getResponse()));
  }
}
