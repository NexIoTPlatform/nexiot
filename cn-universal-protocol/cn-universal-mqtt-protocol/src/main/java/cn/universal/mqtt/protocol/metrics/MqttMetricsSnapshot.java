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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT指标快照
 *
 * <p>用于保存某个时间点的MQTT指标数据 提供不可变的指标数据视图
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttMetricsSnapshot {

  /** 快照时间戳 */
  @Builder.Default private long timestamp = System.currentTimeMillis();

  /** 基础计数指标 */
  private long totalMessageCount;

  private long subscribeMessageCount;
  private long publishMessageCount;
  private long successfulProcessingCount;
  private long failedProcessingCount;
  private long skippedProcessingCount;
  private long errorCount;

  /** QoS相关指标 */
  private long qos0MessageCount;

  private long qos1MessageCount;
  private long qos2MessageCount;
  private long retainedMessageCount;
  private long duplicateMessageCount;

  /** 处理时间相关指标 */
  private long fastProcessingCount;

  private long normalProcessingCount;
  private long slowProcessingCount;
  private double averageProcessingTime;
  private long minProcessingTime;
  private long maxProcessingTime;

  /** 连接相关指标 */
  private long activeClientCount;

  private long totalConnectionCount;
  private long disconnectionCount;

  /** 计算处理成功率 */
  public double getSuccessRate() {
    long totalProcessed = successfulProcessingCount + failedProcessingCount;
    if (totalProcessed == 0) {
      return 0.0;
    }
    return (double) successfulProcessingCount / totalProcessed * 100;
  }

  /** 计算错误率 */
  public double getErrorRate() {
    if (totalMessageCount == 0) {
      return 0.0;
    }
    return (double) errorCount / totalMessageCount * 100;
  }

  /** 计算QoS分布 */
  public QosDistribution getQosDistribution() {
    long totalQosMessages = qos0MessageCount + qos1MessageCount + qos2MessageCount;
    if (totalQosMessages == 0) {
      return new QosDistribution(0.0, 0.0, 0.0);
    }

    double qos0Percentage = (double) qos0MessageCount / totalQosMessages * 100;
    double qos1Percentage = (double) qos1MessageCount / totalQosMessages * 100;
    double qos2Percentage = (double) qos2MessageCount / totalQosMessages * 100;

    return new QosDistribution(qos0Percentage, qos1Percentage, qos2Percentage);
  }

  /** 计算处理时间分布 */
  public ProcessingTimeDistribution getProcessingTimeDistribution() {
    long totalTimeProcessed = fastProcessingCount + normalProcessingCount + slowProcessingCount;
    if (totalTimeProcessed == 0) {
      return new ProcessingTimeDistribution(0.0, 0.0, 0.0);
    }

    double fastPercentage = (double) fastProcessingCount / totalTimeProcessed * 100;
    double normalPercentage = (double) normalProcessingCount / totalTimeProcessed * 100;
    double slowPercentage = (double) slowProcessingCount / totalTimeProcessed * 100;

    return new ProcessingTimeDistribution(fastPercentage, normalPercentage, slowPercentage);
  }

  /** 检查系统健康状态 */
  public HealthStatus getHealthStatus() {
    double successRate = getSuccessRate();
    double errorRate = getErrorRate();

    if (successRate >= 95 && errorRate <= 1 && averageProcessingTime <= 100) {
      return HealthStatus.HEALTHY;
    } else if (successRate >= 80 && errorRate <= 5 && averageProcessingTime <= 500) {
      return HealthStatus.WARNING;
    } else {
      return HealthStatus.CRITICAL;
    }
  }

  /** 获取格式化的统计摘要 */
  public String getSummary() {
    StringBuilder summary = new StringBuilder();
    summary.append("MQTT 指标摘要 (").append(java.time.Instant.ofEpochMilli(timestamp)).append(")\n");
    summary.append("----------------------------------------\n");
    summary.append("总消息数: ").append(totalMessageCount).append("\n");
    summary.append("处理成功率: ").append(String.format("%.2f", getSuccessRate())).append("%\n");
    summary.append("错误率: ").append(String.format("%.2f", getErrorRate())).append("%\n");
    summary.append("平均处理时间: ").append(String.format("%.2f", averageProcessingTime)).append("ms\n");
    summary.append("活跃客户端: ").append(activeClientCount).append("\n");
    summary.append("健康状态: ").append(getHealthStatus()).append("\n");

    return summary.toString();
  }

  /** QoS分布内部类 */
  @Data
  @AllArgsConstructor
  public static class QosDistribution {

    private double qos0Percentage;
    private double qos1Percentage;
    private double qos2Percentage;
  }

  /** 处理时间分布内部类 */
  @Data
  @AllArgsConstructor
  public static class ProcessingTimeDistribution {

    private double fastPercentage;
    private double normalPercentage;
    private double slowPercentage;
  }

  /** 健康状态枚举 */
  public enum HealthStatus {
    HEALTHY("健康"),
    WARNING("警告"),
    CRITICAL("严重");

    private final String description;

    HealthStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return description;
    }
  }
}
