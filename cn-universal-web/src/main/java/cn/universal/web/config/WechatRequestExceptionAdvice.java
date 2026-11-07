package cn.universal.web.config;

import cn.universal.admin.system.web.WechatAccountConfigController;
import cn.universal.admin.system.web.WechatProfileController;
import cn.universal.common.domain.R;
import cn.universal.web.controller.common.WechatAuthController;
import cn.universal.web.controller.common.WechatCallbackController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 认证请求的解析错误可能含凭据字段值，禁止进入通用异常原文日志和错误响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
    assignableTypes = {
      WechatAuthController.class,
      WechatCallbackController.class,
      WechatAccountConfigController.class,
      WechatProfileController.class
    })
public class WechatRequestExceptionAdvice {
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<R<Void>> unreadableBody() {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_JSON)
        .body(R.error(400, "微信请求内容格式不正确"));
  }
}
