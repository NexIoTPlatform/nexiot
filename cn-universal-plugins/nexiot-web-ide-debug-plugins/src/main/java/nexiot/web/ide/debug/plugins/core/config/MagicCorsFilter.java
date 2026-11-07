package nexiot.web.ide.debug.plugins.core.config;

import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletRequest;
import nexiot.web.ide.debug.plugins.core.servlet.MagicHttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

public abstract class MagicCorsFilter {

  public void process(MagicHttpServletRequest request, MagicHttpServletResponse response) {
    if (StringUtils.isNotBlank(Constants.HEADER_REQUEST_CLIENT_ID)) {
      String value = request.getHeader(HttpHeaders.ORIGIN);
      response.setHeader(
          HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, StringUtils.isBlank(value) ? "*" : value);
      response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
      response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, Constants.CONST_STRING_TRUE);
      value = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
      if (StringUtils.isNotBlank(value)) {
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, value);
      }
      value = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
      response.setHeader(
          HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
          StringUtils.isBlank(value) ? "GET,POST,OPTIONS,PUT,DELETE" : value);
    }
  }
}
