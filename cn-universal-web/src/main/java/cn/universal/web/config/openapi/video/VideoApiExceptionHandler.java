package cn.universal.web.config.openapi.video;

import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.web.controller.openapi.video.VideoApiControllerV1;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Local HTTP semantics and safe errors; existing OpenAPI v1 and management APIs are unchanged. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = VideoApiControllerV1.class)
public class VideoApiExceptionHandler {
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<R<Void>> denied(AccessDeniedException exception) {
    return ResponseEntity.status(403).body(R.error(403, "应用、设备或通道无访问权限"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<R<Void>> status(ResponseStatusException exception) {
    int status = exception.getStatusCode().value();
    return ResponseEntity.status(status).body(R.error(status, exception.getReason()));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
      IllegalArgumentException.class})
  public ResponseEntity<R<Void>> invalid(Exception exception) {
    return ResponseEntity.badRequest().body(R.error(400, "请求参数格式或范围无效"));
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<R<Void>> unavailable(BaseException exception) {
    return ResponseEntity.status(409).body(R.error(409, "设备能力或视频会话当前不可用，请核对设备状态和请求参数"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<R<Void>> failure(Exception exception) {
    return ResponseEntity.status(502).body(R.error(502, "视频服务暂不可用，请通过requestId联系运维排查"));
  }
}
