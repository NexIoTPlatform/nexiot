package cn.universal.mqtt.protocol.controller;

import cn.universal.mqtt.protocol.metrics.MqttMetricsMananer;
import cn.universal.mqtt.protocol.metrics.MqttMetricsSnapshot;
import cn.universal.mqtt.protocol.system.SysMQTTManager;
import cn.universal.mqtt.protocol.third.MQTTStatisticsLogger;
import cn.universal.mqtt.protocol.third.ThirdMQTTConfigService;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MQTT统计查询控制器
 *
 * <p>专门负责MQTT相关的统计查询功能，包括： - 配置统计 - 连接统计 - 性能指标 - 健康状态 - 详细统计信息
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@RestController
@RequestMapping("/monitor/mqtt/v2/statistics")
public class MqttStatisticsController {

  @Autowired private ThirdMQTTConfigService mqttConfigService;

  @Autowired private ThirdMQTTServerManager mqttServerManager;

  @Autowired private SysMQTTManager sysMQTTManager;

  @Autowired private MqttMetricsMananer metricsCollector;

  @Autowired private MQTTStatisticsLogger statisticsLogger;

  // ==================== 配置统计 ====================

  /** 获取完整配置统计信息 */
  @GetMapping("/config")
  public ResponseEntity<Map<String, Object>> getConfigStatistics() {
    try {
      String statistics = mqttConfigService.getCompleteConfigStatistics();

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取配置统计成功");
      result.put("data", statistics);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 获取配置统计失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取配置统计失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 获取产品列表统计 */
  @GetMapping("/products")
  public ResponseEntity<Map<String, Object>> getProductStatistics() {
    try {
      List<String> productKeys = mqttConfigService.getAllProductKeys();
      int totalCount = mqttConfigService.getTotalEnabledProductCount();

      Map<String, Object> productStats = new HashMap<>();
      productStats.put("productKeys", productKeys);
      productStats.put("totalCount", totalCount);
      productStats.put("clientCount", productKeys.size() - (sysMQTTManager.isEnabled() ? 1 : 0));
      productStats.put("systemMqttEnabled", sysMQTTManager.isEnabled());

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取产品统计成功");
      result.put("data", productStats);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 获取产品统计失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取产品统计失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 检查指定产品配置是否存在 */
  @GetMapping("/products/{productKey}/exists")
  public ResponseEntity<Map<String, Object>> checkProductExists(@PathVariable String productKey) {
    try {
      boolean exists = mqttConfigService.supportMQTTNetwork(productKey);

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "检查产品配置完成");
      result.put("data", exists);
      result.put("productKey", productKey);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 检查产品配置失败: productKey={}", productKey, e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "检查产品配置失败: " + e.getMessage());
      result.put("productKey", productKey);

      return ResponseEntity.status(500).body(result);
    }
  }

  // ==================== 连接统计 ====================

  /** 获取连接统计信息 */
  @GetMapping("/connections")
  public ResponseEntity<Map<String, Object>> getConnectionStatistics() {
    try {
      Map<String, String> allStatus = mqttServerManager.getAllClientStatus();

      // 统计各种状态的数量
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

      Map<String, Object> connectionStats = new HashMap<>();
      connectionStats.put("total", allStatus.size());
      connectionStats.put("connected", connectedCount);
      connectionStats.put("disconnected", disconnectedCount);
      connectionStats.put("systemCovered", systemCoveredCount);
      connectionStats.put("error", errorCount);
      connectionStats.put("effectiveConnected", effectiveConnected);
      connectionStats.put(
          "connectionRate",
          allStatus.size() > 0
              ? String.format("%.2f%%", (effectiveConnected * 100.0 / allStatus.size()))
              : "0.00%");

      // 添加详细状态分布
      Map<String, Long> statusDistribution =
          allStatus.values().stream()
              .collect(
                  java.util.stream.Collectors.groupingBy(
                      java.util.function.Function.identity(),
                      java.util.stream.Collectors.counting()));
      connectionStats.put("statusDistribution", statusDistribution);

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取连接统计成功");
      result.put("data", connectionStats);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 获取连接统计失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取连接统计失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 获取详细的客户端连接信息 */
  @GetMapping("/connections/details")
  public ResponseEntity<Map<String, Object>> getConnectionDetails() {
    try {
      Map<String, String> allStatus = mqttServerManager.getAllClientStatus();
      List<Map<String, Object>> configDetails = mqttConfigService.getConfigDetails();

      // 合并状态和配置信息
      List<Map<String, Object>> connectionDetails = new ArrayList<>();

      for (Map.Entry<String, String> entry : allStatus.entrySet()) {
        String unionId = entry.getKey();
        String status = entry.getValue();

        Map<String, Object> detail = new HashMap<>();
        detail.put("unionId", unionId);
        detail.put("status", status);
        detail.put("isSystemMqtt", "SYSTEM_MQTT_BROKER".equals(unionId));
        detail.put("isSystemCovered", "SYSTEM_MQTT_COVERED".equals(status));

        // 查找对应的配置信息
        configDetails.stream()
            .filter(config -> unionId.equals(config.get("unionId")))
            .findFirst()
            .ifPresent(
                config -> {
                  detail.put("networkType", config.get("networkType"));
                  detail.put("host", config.get("host"));
                  detail.put("username", config.get("username"));
                  detail.put("subscribeTopics", config.get("subscribeTopics"));
                  detail.put("enabled", config.get("enabled"));
                });

        connectionDetails.add(detail);
      }

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取连接详情成功");
      result.put("data", connectionDetails);
      result.put("total", connectionDetails.size());
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 获取连接详情失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取连接详情失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  // ==================== 性能指标统计 ====================

  /** 获取性能指标快照 */
  @GetMapping("/metrics/snapshot")
  public ResponseEntity<Map<String, Object>> getMetricsSnapshot() {
    try {
      MqttMetricsSnapshot snapshot = metricsCollector.getSnapshot();

      Map<String, Object> metricsData = new HashMap<>();
      metricsData.put("activeClientCount", snapshot.getActiveClientCount());
      metricsData.put("totalMessageCount", snapshot.getTotalMessageCount());
      metricsData.put("publishMessageCount", snapshot.getPublishMessageCount());
      metricsData.put("subscribeMessageCount", snapshot.getSubscribeMessageCount());
      metricsData.put("errorCount", snapshot.getErrorCount());
      metricsData.put("averageProcessingTime", snapshot.getAverageProcessingTime());
      metricsData.put("maxProcessingTime", snapshot.getMaxProcessingTime());
      metricsData.put("minProcessingTime", snapshot.getMinProcessingTime());
      metricsData.put("successfulProcessingCount", snapshot.getSuccessfulProcessingCount());
      metricsData.put("failedProcessingCount", snapshot.getFailedProcessingCount());
      metricsData.put("successRate", snapshot.getSuccessRate());
      metricsData.put("errorRate", snapshot.getErrorRate());
      metricsData.put("healthStatus", snapshot.getHealthStatus().toString());
      metricsData.put("timestamp", snapshot.getTimestamp());

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取性能指标成功");
      result.put("data", metricsData);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 获取性能指标失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取性能指标失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 获取详细的性能统计 */
  @GetMapping("/metrics/detailed")
  public ResponseEntity<Map<String, Object>> getDetailedMetrics() {
    try {
      String detailedStats = metricsCollector.getDetailedStatistics();

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取详细性能统计成功");
      result.put("data", detailedStats);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 获取详细性能统计失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取详细性能统计失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 重置性能指标 */
  @PostMapping("/metrics/reset")
  public ResponseEntity<Map<String, Object>> resetMetrics() {
    try {
      metricsCollector.reset();

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "性能指标重置成功");
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 重置性能指标失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "重置性能指标失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  // ==================== 健康状态统计 ====================

  /** 获取系统健康状态 */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> getHealthStatus() {
    try {
      Map<String, String> allStatus = mqttServerManager.getAllClientStatus();
      MqttMetricsSnapshot metricsSnapshot = metricsCollector.getSnapshot();
      int enabledCount = mqttConfigService.getTotalEnabledProductCount();

      // 计算健康指标
      long connectedCount =
          allStatus.values().stream().filter(status -> "CONNECTED".equals(status)).count();
      long systemCoveredCount =
          allStatus.values().stream()
              .filter(status -> "SYSTEM_MQTT_COVERED".equals(status))
              .count();
      long errorCount =
          allStatus.values().stream().filter(status -> status.startsWith("ERROR")).count();

      long effectiveConnected = connectedCount + systemCoveredCount;

      // 确定健康状态
      String healthStatus;
      if (errorCount > 0) {
        healthStatus = "ERROR";
      } else if (effectiveConnected == enabledCount && enabledCount > 0) {
        healthStatus = "HEALTHY";
      } else if (effectiveConnected > 0) {
        healthStatus = "PARTIAL";
      } else {
        healthStatus = "DOWN";
      }

      Map<String, Object> health = new HashMap<>();
      health.put("status", healthStatus);
      health.put("enabledProducts", enabledCount);
      health.put("runningClients", allStatus.size());
      health.put("connectedClients", connectedCount);
      health.put("systemCoveredClients", systemCoveredCount);
      health.put("errorClients", errorCount);
      health.put(
          "systemMqttStatus",
          sysMQTTManager.isEnabled()
              ? (sysMQTTManager.isConnected() ? "CONNECTED" : "DISCONNECTED")
              : "DISABLED");
      health.put("metricsHealthStatus", metricsSnapshot.getHealthStatus().toString());
      health.put("metricsSuccessRate", metricsSnapshot.getSuccessRate());
      health.put("metricsErrorRate", metricsSnapshot.getErrorRate());
      health.put("snapshotTimestamp", metricsSnapshot.getTimestamp());
      health.put("timestamp", System.currentTimeMillis());

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取健康状态成功");
      result.put("data", health);

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 获取健康状态失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取健康状态失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  // ==================== 综合统计 ====================

  /** 获取完整的统计概览 */
  @GetMapping("/overview")
  public ResponseEntity<Map<String, Object>> getStatisticsOverview() {
    try {
      // 配置统计
      int totalProducts = mqttConfigService.getTotalEnabledProductCount();
      List<String> productKeys = mqttConfigService.getAllProductKeys();

      // 连接统计
      Map<String, String> allStatus = mqttServerManager.getAllClientStatus();
      long connectedCount =
          allStatus.values().stream().filter(status -> "CONNECTED".equals(status)).count();
      long systemCoveredCount =
          allStatus.values().stream()
              .filter(status -> "SYSTEM_MQTT_COVERED".equals(status))
              .count();

      // 性能指标
      MqttMetricsSnapshot metricsSnapshot = metricsCollector.getSnapshot();

      // 系统状态
      boolean systemMqttEnabled = sysMQTTManager.isEnabled();
      boolean systemMqttConnected = sysMQTTManager.isConnected();

      Map<String, Object> overview = new HashMap<>();

      // 配置概览
      Map<String, Object> configOverview = new HashMap<>();
      configOverview.put("totalProducts", totalProducts);
      configOverview.put("systemMqttEnabled", systemMqttEnabled);
      configOverview.put("databaseConfigCount", totalProducts - (systemMqttEnabled ? 1 : 0));
      overview.put("config", configOverview);

      // 连接概览
      Map<String, Object> connectionOverview = new HashMap<>();
      connectionOverview.put("totalClients", allStatus.size());
      connectionOverview.put("connectedClients", connectedCount);
      connectionOverview.put("systemCoveredClients", systemCoveredCount);
      connectionOverview.put("effectiveConnections", connectedCount + systemCoveredCount);
      connectionOverview.put(
          "connectionRate",
          allStatus.size() > 0
              ? String.format(
                  "%.2f%%", ((connectedCount + systemCoveredCount) * 100.0 / allStatus.size()))
              : "0.00%");
      overview.put("connections", connectionOverview);

      // 性能概览
      Map<String, Object> performanceOverview = new HashMap<>();
      performanceOverview.put("totalMessages", metricsSnapshot.getTotalMessageCount());
      performanceOverview.put("averageProcessingTime", metricsSnapshot.getAverageProcessingTime());
      performanceOverview.put("errorCount", metricsSnapshot.getErrorCount());
      performanceOverview.put("successRate", metricsSnapshot.getSuccessRate());
      performanceOverview.put("healthStatus", metricsSnapshot.getHealthStatus().toString());
      overview.put("performance", performanceOverview);

      // 系统概览
      Map<String, Object> systemOverview = new HashMap<>();
      systemOverview.put("systemMqttEnabled", systemMqttEnabled);
      systemOverview.put("systemMqttConnected", systemMqttConnected);
      systemOverview.put(
          "serverManagerActiveConnections", mqttServerManager.getActiveConnectionCount());
      overview.put("system", systemOverview);

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取统计概览成功");
      result.put("data", overview);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 获取统计概览失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取统计概览失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  // ==================== 统计信息日志 ====================

  /** 手动触发统计信息日志打印 */
  @PostMapping("/logging/trigger")
  public ResponseEntity<Map<String, Object>> triggerStatisticsLog() {
    try {
      statisticsLogger.triggerStatisticsLog();

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "统计信息日志打印已触发");
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 触发统计信息日志打印失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "触发统计信息日志打印失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }

  /** 获取统计信息日志配置 */
  @GetMapping("/logging/config")
  public ResponseEntity<Map<String, Object>> getLoggingConfig() {
    try {
      Map<String, Object> config = statisticsLogger.getLoggingConfig();

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "获取统计信息日志配置成功");
      result.put("data", config);
      result.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("[THIRD_MQTT_STATS] 获取统计信息日志配置失败: ", e);

      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("message", "获取统计信息日志配置失败: " + e.getMessage());

      return ResponseEntity.status(500).body(result);
    }
  }
}
