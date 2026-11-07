package cn.universal.http.access.web;

import lombok.Getter;

@Getter
public class HttpAccessException extends RuntimeException {
  private final int status;
  private final String code;

  public HttpAccessException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
