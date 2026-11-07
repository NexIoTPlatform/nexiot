/// *
// *
// * Copyright (c) 2025, NexIoT. All Rights Reserved.
// *
// * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
// * @Author: gitee.com/NexIoT
// * @Email: wo8335224@gmail.com
// * @Wechat: outlookFil
// *
// *
// */
//
// package cn.universal.protocol.mqtt.v2.third;
//
// import entity.cn.universal.protocol.mqtt.MQTTProductConfig;
// import entity.cn.universal.protocol.mqtt.MQTTPublishMessage;
// import metrics.cn.universal.protocol.mqtt.MqttMetricsSnapshot;
// import java.util.Map;
// import lombok.extern.slf4j.Slf4j;
//
/// **
// * MQTT 主服务类
// * <p>
// * 提供MQTT消息服务能力 管理MQTT服务器、处理器链、指标收集等核心功能
// *
// * @Author gitee.com/NexIoT
// * @version 3.0
// * @since 2025/1/20
// */
// @Slf4j(topic = "mqtt")
// public class ThirdMQTTFacade {
//
//
//  // 服务状态
//  private volatile boolean initialized = false;
//  private volatile boolean running = false;
//
//
//  public String name() {
//    return "MQTT";
//  }
//
//  public String version() {
//    return "3.0.0";
//  }
//
//  public String description() {
//    return "MQTT消息服务，支持MQTT协议的设备接入和消息处理";
//  }
//
//  public boolean isEnabled() {
//    return true;
//  }
//
//  public boolean isRunning() {
//    return running;
//  }
//
//  /**
//   * 初始化服务
//   */
//  public synchronized boolean initialize() {
//    if (initialized) {
//      log.warn("[THIRD_MQTT] 服务已初始化，无需重复初始化");
//      return true;
//    }
//
//    try {
//      log.info("[THIRD_MQTT] 开始初始化MQTT服务...");
//
//      // 1. 检查依赖组件
//      if (!checkDependencies()) {
//        log.error("[THIRD_MQTT] 依赖组件检查失败");
//        return false;
//      }
//
//      // 2. 服务器管理器会在PostConstruct中自动初始化
//      // 无需手动启动，等待初始化完成即可
//
//      // 3. 验证处理器链
//      if (!validateProcessorChain()) {
//        log.error("[THIRD_MQTT] 处理器链验证失败");
//        return false;
//      }
//
//      initialized = true;
//      running = true;
//
//      // 4. 输出初始化信息
//      logInitializationInfo();
//
//      log.info("[THIRD_MQTT] MQTT服务初始化成功");
//      return true;
//
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] MQTT服务初始化失败: ", e);
//      return false;
//    }
//  }
//
//  /**
//   * 启动服务
//   */
//  public synchronized boolean start() {
//    if (!initialized) {
//      return initialize();
//    }
//
//    if (running) {
//      log.warn("[THIRD_MQTT] 服务已在运行中");
//      return true;
//    }
//
//    try {
//      log.info("[THIRD_MQTT] 启动MQTT服务...");
//
//      // 重新加载和启动所有客户端
//      serverManager.loadAndStartAllClients();
//      running = true;
//      log.info("[THIRD_MQTT] MQTT服务启动成功");
//      return true;
//
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] MQTT服务启动异常: ", e);
//      return false;
//    }
//  }
//
//  /**
//   * 停止服务
//   */
//  public synchronized boolean stop() {
//    if (!running) {
//      log.warn("[THIRD_MQTT] 服务未在运行中");
//      return true;
//    }
//
//    try {
//      log.info("[THIRD_MQTT] 停止MQTT服务...");
//
//      // 调用destroy方法来停止所有客户端
//      serverManager.destroy();
//      running = false;
//      log.info("[THIRD_MQTT] MQTT服务停止成功");
//      return true;
//
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] MQTT服务停止异常: ", e);
//      return false;
//    }
//  }
//
//  /**
//   * 重启服务
//   */
//  public boolean restart() {
//    log.info("[THIRD_MQTT] 重启MQTT服务...");
//    return stop() && start();
//  }
//
//  /**
//   * 主动发送消息
//   */
//  public boolean sendMessage(String productKey, String deviceId, String topic, String payload) {
//    return sendMessage(productKey, deviceId, topic, payload, 1, false);
//  }
//
//  /**
//   * 主动发送消息（完整参数）
//   */
//  public boolean sendMessage(String productKey, String deviceId, String topic, String payload,
//      int qos, boolean retained) {
//    try {
//      if (!running) {
//        log.warn("[THIRD_MQTT] 服务未运行，无法发送消息");
//        return false;
//      }
//
//      // 构建发布消息
//      MQTTPublishMessage message = MQTTPublishMessage.builder().topic(topic)
//          .content(payload).qos(qos).retained(retained).productKey(productKey)
//          .deviceId(deviceId).messageType("API_SEND").build();
//
//      // 发送消息
//      boolean success = serverManager.publishMessage(productKey, message);
//
//      if (success) {
//        log.debug("[THIRD_MQTT] API发送消息成功 - 产品: {}, 设备: {}, 主题: {}", productKey,
//            deviceId, topic);
//      } else {
//        log.warn("[THIRD_MQTT] API发送消息失败 - 产品: {}, 设备: {}, 主题: {}", productKey,
//            deviceId, topic);
//      }
//
//      return success;
//
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] API发送消息异常 - 产品: {}, 设备: {}, 主题: {}, 异常: ", productKey,
//          deviceId, topic, e);
//      return false;
//    }
//  }
//
//  /**
//   * 添加产品配置
//   */
//  public boolean addProductConfig(MQTTProductConfig config) {
//    try {
//      // 重构后通过重新加载配置来添加产品
//      serverManager.reloadAllConfigs();
//      log.info("[THIRD_MQTT] 添加产品配置成功: {}", config.getProductKey());
//      return true;
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] 添加产品配置异常: ", e);
//      return false;
//    }
//  }
//
//  /**
//   * 移除产品配置
//   */
//  public boolean removeProductConfig(String productKey) {
//    try {
//      // 停止指定产品的客户端
//      boolean success = serverManager.stopMqttClient(productKey);
//      if (success) {
//        log.info("[THIRD_MQTT] 移除产品配置成功: {}", productKey);
//      } else {
//        log.warn("[THIRD_MQTT] 移除产品配置失败: {}", productKey);
//      }
//      return success;
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] 移除产品配置异常: ", e);
//      return false;
//    }
//  }
//
//  /**
//   * 重新加载配置
//   */
//  public boolean reloadConfigs() {
//    try {
//      log.info("[THIRD_MQTT] 重新加载配置...");
//
//      // 使用新的配置服务重新加载配置
//      serverManager.reloadAllConfigs();
//
//      Map<String, MQTTProductConfig> configs = configService.loadAllConfigs();
//      log.info("[THIRD_MQTT] 配置重新加载完成，配置数: {}", configs.size());
//      return true;
//
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] 重新加载配置失败: ", e);
//      return false;
//    }
//  }
//
//  /**
//   * 获取服务状态
//   */
//  public Map<String, Object> getStatus() {
//    Map<String, Object> status = new java.util.HashMap<>();
//
//    status.put("serviceName", name());
//    status.put("version", version());
//    status.put("initialized", initialized);
//    status.put("running", running);
//    status.put("serverManagerStarted", isServerManagerStarted());
//    status.put("processorCount", processorChain.getProcessorCount());
//    status.put("enabledProcessorCount", processorChain.getEnabledProcessorCount());
//
//    return status;
//  }
//
//  /**
//   * 检查服务器管理器是否启动
//   */
//  private boolean isServerManagerStarted() {
//    try {
//      // 通过检查活跃连接数来判断是否启动
//      int activeCount = serverManager.getActiveConnectionCount();
//      return activeCount >= 0; // 如果能获取到连接数，说明已启动
//    } catch (Exception e) {
//      return false;
//    }
//  }
//
//  /**
//   * 获取指标快照
//   */
//  public MqttMetricsSnapshot getMetricsSnapshot() {
//    return metricsCollector.getSnapshot();
//  }
//
//  /**
//   * 获取详细统计信息
//   */
//  public String getDetailedStatistics() {
//    StringBuilder stats = new StringBuilder();
//
//    // 服务基础信息
//    stats.append("=== MQTT 服务信息 ===\n");
//    stats.append("服务名称: ").append(name()).append("\n");
//    stats.append("版本: ").append(version()).append("\n");
//    stats.append("描述: ").append(description()).append("\n");
//    stats.append("初始化状态: ").append(initialized ? "已初始化" : "未初始化").append("\n");
//    stats.append("运行状态: ").append(running ? "运行中" : "已停止").append("\n");
//
//    // 处理器链信息
//    stats.append("\n").append(processorChain.getStatistics()).append("\n");
//
//    // 服务器管理器信息
//    stats.append(serverManager.getStatistics()).append("\n");
//
//    // 指标信息
//    stats.append(metricsCollector.getDetailedStatistics());
//
//    return stats.toString();
//  }
//
//  /**
//   * 重置指标
//   */
//  public void resetMetrics() {
//    metricsCollector.reset();
//    log.info("[THIRD_MQTT] 指标已重置");
//  }
//
//  /**
//   * 检查依赖组件
//   */
//  private boolean checkDependencies() {
//    try {
//      // 检查核心组件是否存在
//      if (serverManager == null) {
//        log.error("[THIRD_MQTT] MqttServerManager未注入");
//        return false;
//      }
//
//      if (processorChain == null) {
//        log.error("[THIRD_MQTT] MqttProcessorChain未注入");
//        return false;
//      }
//
//      if (metricsCollector == null) {
//        log.error("[THIRD_MQTT] MqttMetricsCollector未注入");
//        return false;
//      }
//
//      if (configService == null) {
//        log.error("[THIRD_MQTT] MqttConfigService未注入");
//        return false;
//      }
//
//      log.debug("[THIRD_MQTT] 依赖组件检查通过");
//      return true;
//
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] 依赖组件检查异常: ", e);
//      return false;
//    }
//  }
//
//  /**
//   * 验证处理器链
//   */
//  private boolean validateProcessorChain() {
//    try {
//      int processorCount = processorChain.getProcessorCount();
//      if (processorCount == 0) {
//        log.warn("[THIRD_MQTT] 处理器链中没有处理器");
//        return false;
//      }
//
//      long enabledCount = processorChain.getEnabledProcessorCount();
//      if (enabledCount == 0) {
//        log.warn("[THIRD_MQTT] 处理器链中没有启用的处理器");
//        return false;
//      }
//
//      if (!processorChain.isHealthy()) {
//        log.warn("[THIRD_MQTT] 处理器链健康检查失败");
//        return false;
//      }
//
//      log.debug("[THIRD_MQTT] 处理器链验证通过，总处理器: {}, 启用处理器: {}", processorCount,
//          enabledCount);
//      return true;
//
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] 处理器链验证异常: ", e);
//      return false;
//    }
//  }
//
//  /**
//   * 输出初始化信息
//   */
//  private void logInitializationInfo() {
//    log.info("[THIRD_MQTT] ==========================================");
//    log.info("[THIRD_MQTT] MQTT 服务初始化信息");
//    log.info("[THIRD_MQTT] 服务名称: {}", name());
//    log.info("[THIRD_MQTT] 版本: {}", version());
//    log.info("[THIRD_MQTT] 描述: {}", description());
//    log.info("[THIRD_MQTT] 处理器数量: {}", processorChain.getProcessorCount());
//    log.info("[THIRD_MQTT] 启用处理器: {}", processorChain.getEnabledProcessorCount());
//    log.info("[THIRD_MQTT] 处理器列表: {}", processorChain.getProcessorNames());
//    log.info("[THIRD_MQTT] 配置产品数: {}", configService.getTotalEnabledProductCount());
//    log.info("[THIRD_MQTT] ==========================================");
//  }
//
//  /**
//   * 健康检查
//   */
//  public boolean healthCheck() {
//    try {
//      // 检查服务状态
//      if (!initialized || !running) {
//        return false;
//      }
//
//      // 检查服务器管理器
//      if (!isServerManagerStarted()) {
//        return false;
//      }
//
//      // 检查处理器链
//      if (!processorChain.isHealthy()) {
//        return false;
//      }
//
//      // 检查指标状态
//      MqttMetricsSnapshot snapshot = metricsCollector.getSnapshot();
//      if (snapshot.getHealthStatus() == MqttMetricsSnapshot.HealthStatus.CRITICAL) {
//        return false;
//      }
//
//      return true;
//
//    } catch (Exception e) {
//      log.error("[THIRD_MQTT] 健康检查异常: ", e);
//      return false;
//    }
//  }
// }
