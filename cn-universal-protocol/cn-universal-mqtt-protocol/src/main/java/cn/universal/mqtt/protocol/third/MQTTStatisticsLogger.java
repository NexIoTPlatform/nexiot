package cn.universal.mqtt.protocol.third;

import cn.universal.common.utils.DelayedTaskUtil;
import cn.universal.mqtt.protocol.metrics.MqttMetricsMananer;
import cn.universal.mqtt.protocol.metrics.MqttMetricsSnapshot;
import cn.universal.mqtt.protocol.system.SysMQTTManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * MQTT统计信息定期打印服务
 *
 * <p>定期收集并打印MQTT模块的关键统计信息，包括： - 配置统计 - 连接状态统计 - 性能指标统计 - 系统健康状态 @Author gitee.com/NexIoT
 *
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Service
public class MQTTStatisticsLogger {

  @Autowired private ThirdMQTTConfigService mqttConfigService;

  @Autowired private ThirdMQTTServerManager mqttServerManager;

  @Autowired private SysMQTTManager sysMQTTManager;

  @Autowired private MqttMetricsMananer metricsCollector;

  @Value("${mqtt.v2.statistics.logging.enabled:true}")
  private boolean loggingEnabled;

  @Value("${mqtt.v2.statistics.logging.interval:3600}")
  private int loggingIntervalSeconds;

  @Value("${mqtt.v2.statistics.logging.detailed:false}")
  private boolean detailedLogging;

  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Resource private DelayedTaskUtil delayedTaskUtil;

  @PostConstruct
  public void init() {
    if (!loggingEnabled) {
      log.info("[MQTT_STATS] 统计信息日志打印已禁用");
      return;
    }

    log.info(
        "[MQTT_STATS] 启动统计信息定期打印服务，间隔: {}秒 ({}分钟)",
        loggingIntervalSeconds,
        loggingIntervalSeconds / 60);
    delayedTaskUtil.putFixedDelayTask(
        () -> {
          logStatistics();
        },
        300,
        loggingIntervalSeconds,
        TimeUnit.SECONDS);
  }

  @PreDestroy
  public void destroy() {
    log.warn("开始destroy，MQTTStatisticsLogger");
  }

  /** 打印统计信息 */
  private void logStatistics() {
    try {
      String timestamp = LocalDateTime.now().format(TIME_FORMATTER);

      // 打印基础统计信息
      logBasicStatistics(timestamp);

      // 如果启用详细日志，打印更多信息
      if (detailedLogging) {
        logDetailedStatistics(timestamp);
      }

    } catch (Exception e) {
      log.error("[MQTT_STATS] 打印统计信息失败: ", e);
    }
  }

  /** 打印基础统计信息 */
  private void logBasicStatistics(String timestamp) {
    try {
      // 配置统计
      String configStats = mqttConfigService.getCompleteConfigStatistics();

      // 连接统计
      Map<String, String> allStatus = mqttServerManager.getAllClientStatus();
      long connectedCount =
          allStatus.values().stream().filter(status -> "CONNECTED".equals(status)).count();
      long disconnectedCount =
          allStatus.values().stream().filter(status -> "DISCONNECTED".equals(status)).count();
      long systemCoveredCount =
          allStatus.values().stream()
              .filter(status -> "SYSTEM_MQTT_COVERED".equals(status))
              .count();
      long errorCount =
          allStatus.values().stream().filter(status -> status.startsWith("ERROR")).count();
      long effectiveConnected = connectedCount + systemCoveredCount;

      // 性能指标
      MqttMetricsSnapshot snapshot = metricsCollector.getSnapshot();

      log.debug("[MQTT_STATS] ========== MQTT 统计信息 [{}] ==========", timestamp);
      log.debug("[MQTT_STATS] 配置统计: {}", configStats);
      double connectionRate =
          allStatus.size() > 0 ? (effectiveConnected * 100.0 / allStatus.size()) : 0.0;
      log.info(
          "[MQTT_STATS] 连接状态: 总数={}, 已连接={}, 断开={}, 系统覆盖={}, 错误={}, 有效连接率={}%",
          allStatus.size(),
          connectedCount,
          disconnectedCount,
          systemCoveredCount,
          errorCount,
          String.format("%.2f", connectionRate));
      log.debug(
          "[MQTT_STATS] 性能指标: 消息处理={}, 平均延迟={}ms, 错误率={}%",
          snapshot.getTotalMessageCount(),
          snapshot.getAverageProcessingTime(),
          String.format("%.2f", snapshot.getErrorRate()));
      log.debug("[MQTT_STATS] 系统MQTT: {}", sysMQTTManager.isEnabled() ? "已启用" : "未启用");
      log.debug("[MQTT_STATS] =================================================");

    } catch (Exception e) {
      log.error("[MQTT_STATS] 获取基础统计信息失败: ", e);
    }
  }

  /** 打印详细统计信息 */
  private void logDetailedStatistics(String timestamp) {
    try {
      log.info("[MQTT_STATS] ========== 详细统计信息 [{}] ==========", timestamp);

      // 产品配置详情
      logProductDetails();

      // 连接详情
      logConnectionDetails();

      // 性能指标详情
      logPerformanceDetails();

      log.info("[MQTT_STATS] =================================================");

    } catch (Exception e) {
      log.error("[MQTT_STATS] 获取详细统计信息失败: ", e);
    }
  }

  /** 打印产品配置详情 */
  private void logProductDetails() {
    try {
      var configDetails = mqttConfigService.getConfigDetails();
      log.info("[MQTT_STATS] 产品配置详情 (共{}个):", configDetails.size());

      for (var detail : configDetails) {
        log.info(
            "[MQTT_STATS]   - {}: {} ({}), 状态: {}",
            detail.get("productKey"),
            detail.get("networkName"),
            detail.get("networkType"),
            detail.get("enabled"));
      }
    } catch (Exception e) {
      log.error("[MQTT_STATS] 获取产品配置详情失败: ", e);
    }
  }

  /** 打印连接详情 */
  private void logConnectionDetails() {
    try {
      Map<String, String> allStatus = mqttServerManager.getAllClientStatus();
      log.info("[MQTT_STATS] 连接状态详情 (共{}个):", allStatus.size());

      // 按状态分组统计
      Map<String, Long> statusCounts =
          allStatus.values().stream()
              .collect(
                  java.util.stream.Collectors.groupingBy(
                      java.util.function.Function.identity(),
                      java.util.stream.Collectors.counting()));

      statusCounts.forEach(
          (status, count) -> {
            log.info("[MQTT_STATS]   - {}: {}个", status, count);
          });

      // 打印错误状态的详细信息
      allStatus.entrySet().stream()
          .filter(entry -> entry.getValue().startsWith("ERROR"))
          .forEach(
              entry -> {
                log.warn("[MQTT_STATS]   错误连接: {} -> {}", entry.getKey(), entry.getValue());
              });

    } catch (Exception e) {
      log.error("[MQTT_STATS] 获取连接详情失败: ", e);
    }
  }

  /** 打印性能指标详情 */
  private void logPerformanceDetails() {
    try {
      MqttMetricsSnapshot snapshot = metricsCollector.getSnapshot();
      log.info("[MQTT_STATS] 性能指标详情:");
      log.info("[MQTT_STATS]   - 总处理消息: {}", snapshot.getTotalMessageCount());
      log.info("[MQTT_STATS]   - 成功处理: {}", snapshot.getSuccessfulProcessingCount());
      log.info("[MQTT_STATS]   - 处理错误: {}", snapshot.getFailedProcessingCount());
      log.info("[MQTT_STATS]   - 平均处理时间: {}ms", snapshot.getAverageProcessingTime());
      log.info("[MQTT_STATS]   - 最大处理时间: {}ms", snapshot.getMaxProcessingTime());
      log.info("[MQTT_STATS]   - 最小处理时间: {}ms", snapshot.getMinProcessingTime());
      log.info("[MQTT_STATS]   - 活跃客户端: {}", snapshot.getActiveClientCount());
      log.info("[MQTT_STATS]   - 健康状态: {}", snapshot.getHealthStatus());

    } catch (Exception e) {
      log.error("[MQTT_STATS] 获取性能指标详情失败: ", e);
    }
  }

  /** 手动触发统计信息打印 */
  public void triggerStatisticsLog() {
    if (loggingEnabled) {
      log.info("[MQTT_STATS] 手动触发统计信息打印");
      logStatistics();
    }
  }

  /** 获取统计信息打印配置 */
  public Map<String, Object> getLoggingConfig() {
    return Map.of(
        "enabled", loggingEnabled,
        "intervalSeconds", loggingIntervalSeconds,
        "detailedLogging", detailedLogging);
  }
}
