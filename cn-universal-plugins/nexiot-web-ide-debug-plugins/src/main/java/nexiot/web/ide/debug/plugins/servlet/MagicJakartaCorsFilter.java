package nexiot.web.ide.debug.plugins.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import nexiot.web.ide.debug.plugins.core.config.MagicCorsFilter;

public class MagicJakartaCorsFilter extends MagicCorsFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    super.process(
        new MagicJakartaHttpServletRequest((HttpServletRequest) request, null),
        new MagicJakartaHttpServletResponse((HttpServletResponse) response));
    chain.doFilter(request, response);
  }
}
