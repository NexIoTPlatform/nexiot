package cn.universal.admin.platform.service.debug;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import cn.universal.core.engine.ScriptDebugOutput;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 同步捕获当前运行线程的 log 模块输出，不按共享 logger 名称串联会话。 */
@Component
public class DriverDebugLogCapture extends AppenderBase<ILoggingEvent> {
  private Logger root;

  @PostConstruct
  public void install() {
    if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {
      setContext(context);
      setName("NexIoTDriverDebug");
      start();
      root = context.getLogger(Logger.ROOT_LOGGER_NAME);
      root.addAppender(this);
    }
  }

  @Override
  protected void append(ILoggingEvent event) {
    ScriptDebugOutput.write(event.getLevel() + " " + event.getFormattedMessage());
  }

  @PreDestroy
  public void uninstall() {
    if (root != null) root.detachAppender(this);
    stop();
  }
}
