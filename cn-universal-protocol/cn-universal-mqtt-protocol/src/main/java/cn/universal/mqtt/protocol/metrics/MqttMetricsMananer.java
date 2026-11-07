/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.mqtt.protocol.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MQTT指标收集器
 *
 * <p>收集MQTT模块的各种统计指标 支持多维度统计：连接、消息、产品、主题、设备等
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Slf4j(topic = "mqtt")
@Component
public class MqttMetricsMananer {

  // 基础计数器
  private final AtomicLong totalMessageCount = new AtomicLong(0);
  private final AtomicLong subscribeMessageCount = new AtomicLong(0);
  private final AtomicLong publishMessageCount = new AtomicLong(0);
  private final AtomicLong successfulProcessingCount = new AtomicLong(0);
  private final AtomicLong failedProcessingCount = new AtomicLong(0);
  private final AtomicLong skippedProcessingCount = new AtomicLong(0);
  private final AtomicLong errorCount = new AtomicLong(0);

  // QoS计数器
  private final AtomicLong qos0MessageCount = new AtomicLong(0);
  private final AtomicLong qos1MessageCount = new AtomicLong(0);
  private final AtomicLong qos2MessageCount = new AtomicLong(0);
  private final AtomicLong retainedMessageCount = new AtomicLong(0);
  private final AtomicLong duplicateMessageCount = new AtomicLong(0);

  // 处理时间计数器
  private final AtomicLong fastProcessingCount = new AtomicLong(0);
  private final AtomicLong normalProcessingCount = new AtomicLong(0);
  private final AtomicLong slowProcessingCount = new AtomicLong(0);

  // 连接指标
  private final AtomicLong activeClientCount = new AtomicLong(0);
  private final AtomicLong totalConnectionCount = new AtomicLong(0);
  private final AtomicLong disconnectionCount = new AtomicLong(0);

  // 产品级指标
  private final Map<String, AtomicLong> productMessageCounts = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> productDeviceCounts = new ConcurrentHashMap<>();

  // 设备级指标
  private final Map<String, AtomicLong> deviceMessageCounts = new ConcurrentHashMap<>();
  private final Map<String, Long> deviceLastActiveTime = new ConcurrentHashMap<>();
  private final Map<String, Boolean> deviceOnlineStatus = new ConcurrentHashMap<>();

  // 主题级指标
  private final Map<String, AtomicLong> topicMessageCounts = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> topicTypeCounts = new ConcurrentHashMap<>();
  private final Map<Integer, AtomicLong> topicLevelCounts = new ConcurrentHashMap<>();

  // 消息大小分布
  private final Map<String, AtomicLong> messageSizeDistribution = new ConcurrentHashMap<>();

  // 处理时间分布
  private final AtomicLong totalProcessingTime = new AtomicLong(0);
  private final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong maxProcessingTime = new AtomicLong(0);

  // 错误类型统计
  private final Map<String, AtomicLong> errorTypeCounts = new ConcurrentHashMap<>();

  /** 基础消息计数 */
  public void incrementMessageCount() {
    totalMessageCount.incrementAndGet();
  }

  public void incrementSubscribeMessageCount() {
    subscribeMessageCount.incrementAndGet();
  }

  public void incrementPublishMessageCount() {
    publishMessageCount.incrementAndGet();
  }

  public void incrementSuccessfulProcessingCount() {
    successfulProcessingCount.incrementAndGet();
  }

  public void incrementFailedProcessingCount() {
    failedProcessingCount.incrementAndGet();
  }

  public void incrementSkippedProcessingCount() {
    skippedProcessingCount.incrementAndGet();
  }

  public void incrementErrorCount() {
    errorCount.incrementAndGet();
  }

  /** QoS计数 */
  public void recordQosDistribution(int qos) {
    switch (qos) {
      case 0:
        qos0MessageCount.incrementAndGet();
        break;
      case 1:
        qos1MessageCount.incrementAndGet();
        break;
      case 2:
        qos2MessageCount.incrementAndGet();
        break;
    }
  }

  public void incrementQos0MessageCount() {
    qos0MessageCount.incrementAndGet();
  }

  public void incrementQos1MessageCount() {
    qos1MessageCount.incrementAndGet();
  }

  public void incrementQos2MessageCount() {
    qos2MessageCount.incrementAndGet();
  }

  public void incrementRetainedMessageCount() {
    retainedMessageCount.incrementAndGet();
  }

  public void incrementDuplicateMessageCount() {
    duplicateMessageCount.incrementAndGet();
  }

  /** 处理时间统计 */
  public void recordProcessingTime(long processingTime) {
    totalProcessingTime.addAndGet(processingTime);

    // 更新最小值
    minProcessingTime.updateAndGet(current -> Math.min(current, processingTime));

    // 更新最大值
    maxProcessingTime.updateAndGet(current -> Math.max(current, processingTime));
  }

  public void incrementFastProcessingCount() {
    fastProcessingCount.incrementAndGet();
  }

  public void incrementNormalProcessingCount() {
    normalProcessingCount.incrementAndGet();
  }

  public void incrementSlowProcessingCount() {
    slowProcessingCount.incrementAndGet();
  }

  /** 连接指标 */
  public void incrementActiveClientCount() {
    activeClientCount.incrementAndGet();
  }

  public void decrementActiveClientCount() {
    activeClientCount.decrementAndGet();
  }

  public void incrementTotalConnectionCount() {
    totalConnectionCount.incrementAndGet();
  }

  public void incrementDisconnectionCount() {
    disconnectionCount.incrementAndGet();
  }

  /** 产品级指标 */
  public void incrementProductMessageCount(String productKey) {
    productMessageCounts.computeIfAbsent(productKey, k -> new AtomicLong(0)).incrementAndGet();
  }

  public void recordActiveDevice(String productKey, String deviceId) {
    productDeviceCounts.computeIfAbsent(productKey, k -> new AtomicLong(0)).incrementAndGet();
  }

  /** 设备级指标 */
  public void incrementDeviceMessageCount(String productKey, String deviceId) {
    String deviceKey = productKey + ":" + deviceId;
    deviceMessageCounts.computeIfAbsent(deviceKey, k -> new AtomicLong(0)).incrementAndGet();
  }

  public void recordDeviceActivity(String productKey, String deviceId) {
    String deviceKey = productKey + ":" + deviceId;
    deviceLastActiveTime.put(deviceKey, System.currentTimeMillis());
  }

  public void updateDeviceOnlineStatus(String productKey, String deviceId, boolean online) {
    String deviceKey = productKey + ":" + deviceId;
    deviceOnlineStatus.put(deviceKey, online);

    if (online) {
      recordDeviceActivity(productKey, deviceId);
    }
  }

  /** 主题级指标 */
  public void incrementTopicMessageCount(String topic) {
    topicMessageCounts.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();
  }

  public void incrementTopicTypeCount(String topicType) {
    topicTypeCounts.computeIfAbsent(topicType, k -> new AtomicLong(0)).incrementAndGet();
  }

  public void recordTopicLevelDistribution(int level) {
    topicLevelCounts.computeIfAbsent(level, k -> new AtomicLong(0)).incrementAndGet();
  }

  /** 消息大小统计 */
  public void recordMessageSize(int size) {
    String sizeRange = getSizeRange(size);
    messageSizeDistribution.computeIfAbsent(sizeRange, k -> new AtomicLong(0)).incrementAndGet();
  }

  private String getSizeRange(int size) {
    if (size <= 100) {
      return "0-100";
    } else if (size <= 500) {
      return "101-500";
    } else if (size <= 1024) {
      return "501-1K";
    } else if (size <= 5120) {
      return "1K-5K";
    } else if (size <= 10240) {
      return "5K-10K";
    } else {
      return "10K+";
    }
  }

  /** 错误统计 */
  public void recordError(String errorType, String errorMessage) {
    errorTypeCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
  }

  /** 获取指标快照 */
  public MqttMetricsSnapshot getSnapshot() {
    return MqttMetricsSnapshot.builder()
        .totalMessageCount(totalMessageCount.get())
        .subscribeMessageCount(subscribeMessageCount.get())
        .publishMessageCount(publishMessageCount.get())
        .successfulProcessingCount(successfulProcessingCount.get())
        .failedProcessingCount(failedProcessingCount.get())
        .skippedProcessingCount(skippedProcessingCount.get())
        .errorCount(errorCount.get())
        .qos0MessageCount(qos0MessageCount.get())
        .qos1MessageCount(qos1MessageCount.get())
        .qos2MessageCount(qos2MessageCount.get())
        .retainedMessageCount(retainedMessageCount.get())
        .duplicateMessageCount(duplicateMessageCount.get())
        .fastProcessingCount(fastProcessingCount.get())
        .normalProcessingCount(normalProcessingCount.get())
        .slowProcessingCount(slowProcessingCount.get())
        .activeClientCount(activeClientCount.get())
        .totalConnectionCount(totalConnectionCount.get())
        .disconnectionCount(disconnectionCount.get())
        .averageProcessingTime(calculateAverageProcessingTime())
        .minProcessingTime(minProcessingTime.get() == Long.MAX_VALUE ? 0 : minProcessingTime.get())
        .maxProcessingTime(maxProcessingTime.get())
        .build();
  }

  private double calculateAverageProcessingTime() {
    long totalMessages = successfulProcessingCount.get() + failedProcessingCount.get();
    if (totalMessages == 0) {
      return 0.0;
    }
    return (double) totalProcessingTime.get() / totalMessages;
  }

  /** 重置所有指标 */
  public void reset() {
    totalMessageCount.set(0);
    subscribeMessageCount.set(0);
    publishMessageCount.set(0);
    successfulProcessingCount.set(0);
    failedProcessingCount.set(0);
    skippedProcessingCount.set(0);
    errorCount.set(0);

    qos0MessageCount.set(0);
    qos1MessageCount.set(0);
    qos2MessageCount.set(0);
    retainedMessageCount.set(0);
    duplicateMessageCount.set(0);

    fastProcessingCount.set(0);
    normalProcessingCount.set(0);
    slowProcessingCount.set(0);

    activeClientCount.set(0);
    totalConnectionCount.set(0);
    disconnectionCount.set(0);

    totalProcessingTime.set(0);
    minProcessingTime.set(Long.MAX_VALUE);
    maxProcessingTime.set(0);

    productMessageCounts.clear();
    productDeviceCounts.clear();
    deviceMessageCounts.clear();
    deviceLastActiveTime.clear();
    deviceOnlineStatus.clear();
    topicMessageCounts.clear();
    topicTypeCounts.clear();
    topicLevelCounts.clear();
    messageSizeDistribution.clear();
    errorTypeCounts.clear();

    log.info("MQTT指标已重置");
  }

  /** 获取详细统计信息 */
  public String getDetailedStatistics() {
    StringBuilder stats = new StringBuilder();
    stats.append("=== MQTT 指标统计 ===\n");

    // 基础统计
    stats.append("基础统计:\n");
    stats.append("  总消息数: ").append(totalMessageCount.get()).append("\n");
    stats.append("  订阅消息数: ").append(subscribeMessageCount.get()).append("\n");
    stats.append("  发布消息数: ").append(publishMessageCount.get()).append("\n");
    stats.append("  处理成功数: ").append(successfulProcessingCount.get()).append("\n");
    stats.append("  处理失败数: ").append(failedProcessingCount.get()).append("\n");
    stats.append("  跳过处理数: ").append(skippedProcessingCount.get()).append("\n");
    stats.append("  错误数: ").append(errorCount.get()).append("\n");

    // QoS统计
    stats.append("QoS统计:\n");
    stats.append("  QoS 0: ").append(qos0MessageCount.get()).append("\n");
    stats.append("  QoS 1: ").append(qos1MessageCount.get()).append("\n");
    stats.append("  QoS 2: ").append(qos2MessageCount.get()).append("\n");
    stats.append("  保留消息: ").append(retainedMessageCount.get()).append("\n");
    stats.append("  重复消息: ").append(duplicateMessageCount.get()).append("\n");

    // 连接统计
    stats.append("连接统计:\n");
    stats.append("  活跃客户端: ").append(activeClientCount.get()).append("\n");
    stats.append("  总连接数: ").append(totalConnectionCount.get()).append("\n");
    stats.append("  断开连接数: ").append(disconnectionCount.get()).append("\n");

    // 处理时间统计
    stats.append("处理时间统计:\n");
    stats
        .append("  平均处理时间: ")
        .append(String.format("%.2f", calculateAverageProcessingTime()))
        .append("ms\n");
    stats
        .append("  最小处理时间: ")
        .append(minProcessingTime.get() == Long.MAX_VALUE ? 0 : minProcessingTime.get())
        .append("ms\n");
    stats.append("  最大处理时间: ").append(maxProcessingTime.get()).append("ms\n");
    stats.append("  快速处理(<10ms): ").append(fastProcessingCount.get()).append("\n");
    stats.append("  正常处理(10-100ms): ").append(normalProcessingCount.get()).append("\n");
    stats.append("  慢速处理(>100ms): ").append(slowProcessingCount.get()).append("\n");

    return stats.toString();
  }
}
