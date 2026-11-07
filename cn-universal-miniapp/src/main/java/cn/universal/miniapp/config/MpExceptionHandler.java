/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.config;

import cn.universal.miniapp.security.MpAccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 小程序模块全局异常处理
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@RestControllerAdvice(basePackages = "cn.universal.miniapp.controller")
public class MpExceptionHandler {

  /** 处理权限不足异常 */
  @ExceptionHandler(MpAccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(MpAccessDeniedException e) {
    log.warn("权限校验失败: {}", e.getMessage());
    Map<String, Object> result = new HashMap<>();
    result.put("code", 403);
    result.put("msg", e.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
  }

  /** 处理业务异常 */
  @ExceptionHandler(MpBusinessException.class)
  public ResponseEntity<Map<String, Object>> handleBusinessException(MpBusinessException e) {
    log.warn("业务异常: {}", e.getMessage());
    Map<String, Object> result = new HashMap<>();
    result.put("code", e.getCode());
    result.put("msg", e.getMessage());
    return ResponseEntity.ok(result);
  }

  /** 处理通用异常 */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleException(Exception e) {
    log.error("系统异常: ", e);
    Map<String, Object> result = new HashMap<>();
    result.put("code", 500);
    result.put("msg", "系统繁忙，请稍后重试");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
  }
}
