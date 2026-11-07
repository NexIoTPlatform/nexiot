package cn.universal.dm.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统监控数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMonitorDTO {

  /**
   * 系统指标
   */
  private SystemMetricsDTO systemMetrics;

  /**
   * 系统信息
   */
  private SystemInfoDTO systemInfo;

  /**
   * 是否为Linux系统
   */
  private Boolean isLinuxSystem;

  /**
   * 时间戳
   */
  private Long timestamp;

  /**
   * 系统指标详情
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SystemMetricsDTO {
    /**
     * CPU使用率（%）
     */
    private Double cpuUsage;

    /**
     * 内存使用率（%）
     */
    private Double memoryUsage;

    /**
     * 磁盘使用率（%）
     */
    private Double diskUsage;

    /**
     * 网络接收速率（KB/s）
     */
    private Double networkReceiveRate;

    /**
     * 网络发送速率（KB/s）
     */
    private Double networkSendRate;

    /**
     * JVM堆内存使用率（%）
     */
    private Double jvmHeapUsage;

    /**
     * JVM非堆内存使用率（%）
     */
    private Double jvmNonHeapUsage;

    /**
     * 线程数
     */
    private Integer threadCount;
  }

  /**
   * 系统信息详情
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SystemInfoDTO {
    /**
     * 操作系统名称
     */
    private String osName;

    /**
     * 操作系统版本
     */
    private String osVersion;

    /**
     * 操作系统架构
     */
    private String osArch;

    /**
     * CPU核心数
     */
    private Integer cpuCores;

    /**
     * 总内存（MB）
     */
    private Long totalMemory;

    /**
     * 可用内存（MB）
     */
    private Long availableMemory;

    /**
     * 总磁盘空间（GB）
     */
    private Long totalDiskSpace;

    /**
     * 可用磁盘空间（GB）
     */
    private Long availableDiskSpace;

    /**
     * JVM版本
     */
    private String jvmVersion;

    /**
     * JVM供应商
     */
    private String jvmVendor;

    /**
     * 系统启动时间
     */
    private Long systemStartTime;

    /**
     * 系统运行时长（秒）
     */
    private Long systemUptime;
  }
}

