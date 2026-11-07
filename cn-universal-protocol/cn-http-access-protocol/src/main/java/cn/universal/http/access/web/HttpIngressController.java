package cn.universal.http.access.web;

import cn.universal.http.access.runtime.HttpOperationRegistry;
import cn.universal.http.access.runtime.HttpRouteSnapshot;
import cn.universal.http.access.security.HttpPathSecurity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A stable access-code endpoint. The bound product driver owns all URL semantics below it. */
@RestController
public class HttpIngressController {

  private final HttpOperationRegistry registry;
  private final HttpPathSecurity pathSecurity;
  private final HttpInboundProcessor processor;

  public HttpIngressController(
      HttpOperationRegistry registry, HttpPathSecurity pathSecurity, HttpInboundProcessor processor) {
    this.registry = registry;
    this.pathSecurity = pathSecurity;
    this.processor = processor;
  }

  @RequestMapping({"/iot/http/v2/{accessCode}", "/iot/http/v2/{accessCode}/{*routePath}"})
  public ResponseEntity<Object> ingress(
      @PathVariable String accessCode,
      @PathVariable(required = false) String routePath,
      @RequestBody(required = false) byte[] body,
      HttpServletRequest servletRequest) {
    String path = routePath == null || routePath.isBlank() ? "/" : routePath;
    pathSecurity.validateRawUri(servletRequest.getRequestURI(), path);
    HttpRouteSnapshot snapshot = registry.getByAccessCode(accessCode).orElseThrow(
        () -> new HttpAccessException(404, "ACCESS_CODE_NOT_FOUND", "HTTP access component not found"));
    long contentLength = servletRequest.getContentLengthLong();
    if (contentLength > snapshot.config().getInbound().getMaxBodyBytes()) {
      throw new HttpAccessException(413, "BODY_TOO_LARGE", "Request body exceeds network limit");
    }
    HttpIngressRequest request = new HttpIngressRequest(servletRequest.getMethod(),
        servletRequest.getRequestURI(), path, servletRequest.getRemoteAddr(), headers(servletRequest),
        query(servletRequest), body == null ? new byte[0] : body);
    HttpIngressResponse response = processor.process(snapshot, request);
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.status());
    response.headers().forEach(builder::header);
    return builder.body(response.body());
  }

  private Map<String, List<String>> headers(HttpServletRequest request) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    Enumeration<String> names = request.getHeaderNames();
    if (names != null) while (names.hasMoreElements()) {
      String name = names.nextElement();
      result.put(name, Collections.list(request.getHeaders(name)));
    }
    return result;
  }

  private Map<String, List<String>> query(HttpServletRequest request) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    request.getParameterMap().forEach((key, values) -> result.put(key, List.of(values)));
    return result;
  }
}
