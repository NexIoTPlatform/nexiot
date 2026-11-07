package cn.universal.core.engine.exception;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String module) {
    super(module);
  }
}
