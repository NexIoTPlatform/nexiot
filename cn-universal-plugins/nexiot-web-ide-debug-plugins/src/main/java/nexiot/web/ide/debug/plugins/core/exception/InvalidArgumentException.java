package nexiot.web.ide.debug.plugins.core.exception;

import nexiot.web.ide.debug.plugins.core.model.JsonCode;

/**
 * 参数错误异常
 *
 * @author mxd
 */
public class InvalidArgumentException extends RuntimeException {

  private final transient JsonCode jsonCode;

  public InvalidArgumentException(JsonCode jsonCode) {
    super(jsonCode.getMessage());
    this.jsonCode = jsonCode;
  }

  public int getCode() {
    return jsonCode.getCode();
  }
}
