package nexiot.web.ide.debug.plugins.core.logging;

import nexiot.web.ide.debug.plugins.core.config.WebSocketSessionManager;

/**
 * 日志上下文
 *
 * @author mxd
 */
public interface MagicLoggerContext {

  String LOGGER_NAME = "magic";

  ThreadLocal<String> SESSION = new InheritableThreadLocal<>();

  /**
   * 打印日志 re
   *
   * @param logInfo 日志信息
   */
  static void println(String logInfo) {
    // 获取SessionId
    String sessionId = SESSION.get();
    if (sessionId != null) {
      WebSocketSessionManager.sendLogs(sessionId, logInfo);
    }
  }

  /** 移除ThreadLocal中的sessionId */
  static void remove() {
    SESSION.remove();
  }

  /** 生成appender */
  void generateAppender();
}
