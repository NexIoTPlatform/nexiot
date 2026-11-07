package cn.universal.core.engine.compile;

public class MagicScriptCompileException extends RuntimeException {

  public MagicScriptCompileException(Throwable cause) {
    super(cause);
  }

  public MagicScriptCompileException(String message, Throwable cause) {
    super(message, cause);
  }

  public MagicScriptCompileException(String message) {
    super(message);
  }
}
