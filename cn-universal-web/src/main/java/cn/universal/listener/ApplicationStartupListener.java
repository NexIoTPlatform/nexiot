package cn.universal.listener;

import cn.universal.admin.video.service.VideoPlatformInstanceService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** 应用启动完成监听器 在应用完全启动后（包括所有Bean初始化完成）执行 */
@Slf4j
@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

  private static final long startTime = System.currentTimeMillis();
  private final Environment environment;
  private final ObjectProvider<VideoPlatformInstanceService> videoPlatformInstanceServiceProvider;

  public ApplicationStartupListener(
      Environment environment,
      ObjectProvider<VideoPlatformInstanceService> videoPlatformInstanceServiceProvider) {
    this.environment = environment;
    this.videoPlatformInstanceServiceProvider = videoPlatformInstanceServiceProvider;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // 计算启动耗时
    String durationText = formatDuration(duration);

    // 打印启动成功信息
    printStartupSuccess(duration, durationText);
    restoreVideoSdkAlarmSubscriptions();
  }

  /** 格式化耗时显示 */
  private String formatDuration(long duration) {
    if (duration < 1000) {
      return duration + " ms";
    } else if (duration < 60000) {
      return String.format("%.1f s", duration / 1000.0);
    } else {
      long minutes = duration / 60000;
      long seconds = (duration % 60000) / 1000;
      return String.format("%d分%d秒", minutes, seconds);
    }
  }

  /** 获取应用端口信息 */
  private String getPortInfo() {
    String serverPort = environment.getProperty("server.port", "8080");
    String contextPath = environment.getProperty("server.servlet.context-path", "");

    // 获取本地地址
    String localAddress = "http://localhost:" + serverPort;
    String contextUrl = localAddress + contextPath;

    return String.format("🌐 访问地址: %s", contextUrl);
  }

  /** 打印启动成功信息 */
  private void printStartupSuccess(long duration, String durationText) {
    String separator = "=".repeat(80);
    String successMessage = String.format("NexIoT Run Success (耗时: %s)", durationText);
    String portInfo = getPortInfo();

    // 日志输出
    log.info(separator);
    log.info("🎉 " + successMessage);
    log.info("🚀 应用已完全启动，所有服务就绪");
    log.info("📊 启动耗时: {} ms", duration);
    log.info(portInfo);
    log.info(separator);

    // 控制台输出
    System.out.println();
    System.out.println(separator);
    System.out.println("🎉 " + successMessage);
    System.out.println("🚀 应用已完全启动，所有服务就绪");
    System.out.println("📊 启动耗时: " + duration + " ms");
    System.out.println(portInfo);
    System.out.println(separator);
    System.out.println();
  }

  private void restoreVideoSdkAlarmSubscriptions() {
    VideoPlatformInstanceService service = videoPlatformInstanceServiceProvider.getIfAvailable();
    if (service == null) {
      return;
    }
    try {
      Map<String, Object> result = service.restoreEnabledSubscriptions();
      log.info("[视频SDK告警] 启动恢复订阅结果: {}", result);
    } catch (Exception e) {
      log.warn("[视频SDK告警] 启动恢复订阅异常: {}", e.getMessage(), e);
    }
  }
}
