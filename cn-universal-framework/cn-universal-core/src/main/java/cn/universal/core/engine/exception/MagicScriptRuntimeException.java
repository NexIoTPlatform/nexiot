package cn.universal.core.engine.exception;

public class MagicScriptRuntimeException extends RuntimeException {

  public MagicScriptRuntimeException() {}

  public MagicScriptRuntimeException(String message) {
    super(message);
  }

  public MagicScriptRuntimeException(Throwable cause) {
    super(cause);
  }

  public static MagicScriptRuntimeException create(Object target) {
    if (target instanceof Throwable) {
      return new MagicScriptRuntimeException((Throwable) target);
    } else if (target instanceof String) {
      return new MagicScriptRuntimeException(target.toString());
    }
    return new MagicScriptRuntimeException();
  }
}
