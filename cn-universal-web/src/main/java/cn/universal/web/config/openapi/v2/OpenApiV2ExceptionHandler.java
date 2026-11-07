package cn.universal.web.config.openapi.v2;

import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.common.exception.IoTException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** V2 专用错误契约，不改变 V1 及管理接口的异常处理和 HTTP 状态。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "cn.universal.web.controller.openapi.v2")
public class OpenApiV2ExceptionHandler {
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<R<Void>> denied(AccessDeniedException exception) {
    return ResponseEntity.status(403)
        .contentType(MediaType.APPLICATION_JSON)
        .body(R.error(403, "应用、设备或通道无访问权限，或应用已停用、过期"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<R<Void>> status(ResponseStatusException exception) {
    int status = exception.getStatusCode().value();
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(R.error(status, exception.getReason()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    ConstraintViolationException.class,
    IllegalArgumentException.class,
    ServletRequestBindingException.class,
    HandlerMethodValidationException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<R<Void>> invalid(Exception exception) {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_JSON)
        .body(R.error(400, "请求参数格式、必填字段或取值范围无效，请参照接口文档"));
  }

  @ExceptionHandler(IoTException.class)
  public ResponseEntity<R<Void>> business(IoTException exception) {
    if (exception.getCode() == 400)
      return ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body(R.error(400, "请求参数、时间范围或游标无效，请参照接口文档"));
    if (exception.getCode() == 503)
      return ResponseEntity.status(503)
          .contentType(MediaType.APPLICATION_JSON)
          .body(R.error(503, "历史查询暂不可用，请核对应用配置或稍后重试"));
    if (exception.getCode() == 429)
      return ResponseEntity.status(429)
          .contentType(MediaType.APPLICATION_JSON)
          .body(R.error(429, "查询过于频繁，请稍后重试"));
    return ResponseEntity.status(409)
        .contentType(MediaType.APPLICATION_JSON)
        .body(R.error(exception.getCode(), "设备操作未完成，请核对设备状态、产品配置及请求参数"));
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<R<Void>> unavailable(BaseException exception) {
    return ResponseEntity.status(409)
        .contentType(MediaType.APPLICATION_JSON)
        .body(R.error(409, "当前设备能力或服务状态不支持该操作"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<R<Void>> failure(Exception exception) {
    return ResponseEntity.status(500)
        .contentType(MediaType.APPLICATION_JSON)
        .body(R.error(500, "服务暂不可用，请通过 requestId 联系运维排查"));
  }
}
