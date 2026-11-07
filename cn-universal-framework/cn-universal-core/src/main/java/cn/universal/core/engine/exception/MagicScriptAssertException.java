package cn.universal.core.engine.exception;

public class MagicScriptAssertException extends RuntimeException {

  private final int code;

  private final String message;

  public MagicScriptAssertException(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
