package nexiot.web.ide.debug.plugins.core.config;

/**
 * Debug配置
 *
 * @author mxd
 */
public class Debug {

  /** 断点超时时间 */
  private int timeout = 60;

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
}
