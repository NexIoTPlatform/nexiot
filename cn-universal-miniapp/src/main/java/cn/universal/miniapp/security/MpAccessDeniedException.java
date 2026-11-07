/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.security;

/**
 * 小程序访问拒绝异常
 *
 * @author NexIoT
 * @since 2025/12/15
 */
public class MpAccessDeniedException extends RuntimeException {

  public MpAccessDeniedException(String message) {
    super(message);
  }

  public MpAccessDeniedException(String message, Throwable cause) {
    super(message, cause);
  }
}
