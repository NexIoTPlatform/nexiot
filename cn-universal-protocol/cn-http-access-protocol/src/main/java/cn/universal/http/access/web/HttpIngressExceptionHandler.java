package cn.universal.http.access.web;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = HttpIngressController.class)
public class HttpIngressExceptionHandler {

  @ExceptionHandler(HttpAccessException.class)
  public ResponseEntity<Map<String, Object>> handle(HttpAccessException exception) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("code", exception.getCode());
    body.put("message", exception.getMessage());
    return ResponseEntity.status(exception.getStatus()).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
    log.error("Unexpected HTTP access failure", exception);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("code", "INTERNAL_ERROR");
    body.put("message", "Internal HTTP access error");
    return ResponseEntity.internalServerError().body(body);
  }
}
