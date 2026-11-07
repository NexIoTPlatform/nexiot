/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.config;

import lombok.Getter;

/**
 * 小程序业务异常
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Getter
public class MpBusinessException extends RuntimeException {

  private final int code;

  public MpBusinessException(String message) {
    super(message);
    this.code = 400;
  }

  public MpBusinessException(int code, String message) {
    super(message);
    this.code = code;
  }

  public MpBusinessException(String message, Throwable cause) {
    super(message, cause);
    this.code = 400;
  }
}
