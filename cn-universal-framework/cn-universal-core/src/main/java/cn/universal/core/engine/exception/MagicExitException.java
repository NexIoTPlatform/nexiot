package cn.universal.core.engine.exception;

import cn.universal.core.engine.runtime.ExitValue;

public class MagicExitException extends RuntimeException {

  private final ExitValue exitValue;

  public MagicExitException(ExitValue exitValue) {
    this.exitValue = exitValue;
  }

  public ExitValue getExitValue() {
    return exitValue;
  }
}
