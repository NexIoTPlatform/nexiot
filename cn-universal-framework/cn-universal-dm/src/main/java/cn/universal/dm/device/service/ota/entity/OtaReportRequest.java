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

package cn.universal.dm.device.service.ota.entity;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OTA上报请求实体
 *
 * <p>用于封装设备向平台上报的固件信息、升级进度、升级结果等数据
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/3/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtaReportRequest {

  /** 产品Key */
  private String productKey;

  /** 设备ID */
  private String deviceId;

  /** 报告类型 */
  private ReportType reportType;

  /** 当前固件版本 */
  private String currentVersion;

  /** 硬件版本 */
  private String hardwareVersion;

  /** 固件类型 */
  private String firmwareType;

  /** 固件校验和 */
  private String checksum;

  /** 固件大小 */
  private Long fileSize;

  /** 升级任务ID */
  private String taskId;

  /** 目标版本（升级时） */
  private String targetVersion;

  /** 升级进度（0-100） */
  private Integer progress;

  /** 升级状态 */
  private UpgradeStatus upgradeStatus;

  /** 错误代码（失败时） */
  private String errorCode;

  /** 错误消息（失败时） */
  private String errorMessage;

  /** 设备状态信息 */
  private DeviceStatus deviceStatus;

  /** 扩展属性 */
  private Map<String, Object> properties;

  /** 上报时间 */
  @Builder.Default private LocalDateTime reportTime = LocalDateTime.now();

  /** 协议类型 */
  private String protocol;

  /** 报告类型枚举 */
  public enum ReportType {
    /** 固件信息上报 */
    FIRMWARE_INFO("固件信息上报"),

    /** 升级进度上报 */
    UPGRADE_PROGRESS("升级进度上报"),

    /** 升级结果上报 */
    UPGRADE_RESULT("升级结果上报");

    private final String description;

    ReportType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** 升级状态枚举 */
  public enum UpgradeStatus {
    /** 空闲状态 */
    IDLE("空闲"),

    /** 下载中 */
    DOWNLOADING("下载中"),

    /** 校验中 */
    VERIFYING("校验中"),

    /** 安装中 */
    INSTALLING("安装中"),

    /** 重启中 */
    REBOOTING("重启中"),

    /** 升级成功 */
    SUCCESS("升级成功"),

    /** 升级失败 */
    FAILED("升级失败");

    private final String description;

    UpgradeStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** 设备状态信息 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DeviceStatus {

    /** 电池电量（百分比） */
    private Integer batteryLevel;

    /** 信号强度 */
    private Integer signalStrength;

    /** 可用存储空间（字节） */
    private Long freeSpace;

    /** CPU使用率（百分比） */
    private Integer cpuUsage;

    /** 内存使用率（百分比） */
    private Integer memoryUsage;

    /** 设备温度（摄氏度） */
    private Double temperature;

    /** 网络类型 */
    private String networkType;

    /** 是否在充电 */
    private Boolean isCharging;
  }

  /** 获取设备唯一标识 */
  public String getDeviceUniqueId() {
    return productKey + ":" + deviceId;
  }

  /** 检查是否为升级相关的上报 */
  public boolean isUpgradeRelated() {
    return reportType == ReportType.UPGRADE_PROGRESS || reportType == ReportType.UPGRADE_RESULT;
  }

  /** 检查升级是否成功 */
  public boolean isUpgradeSuccessful() {
    return upgradeStatus == UpgradeStatus.SUCCESS;
  }

  /** 检查升级是否失败 */
  public boolean isUpgradeFailed() {
    return upgradeStatus == UpgradeStatus.FAILED;
  }

  /** 检查升级是否进行中 */
  public boolean isUpgradeInProgress() {
    return upgradeStatus == UpgradeStatus.DOWNLOADING
        || upgradeStatus == UpgradeStatus.VERIFYING
        || upgradeStatus == UpgradeStatus.INSTALLING
        || upgradeStatus == UpgradeStatus.REBOOTING;
  }

  /** 获取设备健康状态评分（0-100） */
  public int getDeviceHealthScore() {
    if (deviceStatus == null) {
      return 50; // 默认中等健康状态
    }

    int score = 100;

    // 电池电量影响
    if (deviceStatus.getBatteryLevel() != null) {
      if (deviceStatus.getBatteryLevel() < 20) {
        score -= 30;
      } else if (deviceStatus.getBatteryLevel() < 50) {
        score -= 10;
      }
    }

    // 存储空间影响
    if (deviceStatus.getFreeSpace() != null) {
      if (deviceStatus.getFreeSpace() < 100 * 1024 * 1024) { // 少于100MB
        score -= 20;
      }
    }

    // CPU和内存使用率影响
    if (deviceStatus.getCpuUsage() != null && deviceStatus.getCpuUsage() > 80) {
      score -= 10;
    }
    if (deviceStatus.getMemoryUsage() != null && deviceStatus.getMemoryUsage() > 80) {
      score -= 10;
    }

    // 温度影响
    if (deviceStatus.getTemperature() != null) {
      if (deviceStatus.getTemperature() > 70) {
        score -= 15;
      } else if (deviceStatus.getTemperature() > 60) {
        score -= 5;
      }
    }

    return Math.max(0, Math.min(100, score));
  }

  @Override
  public String toString() {
    return String.format(
        "OtaReportRequest{device=%s, type=%s, version=%s, status=%s, progress=%d%%}",
        getDeviceUniqueId(),
        reportType,
        currentVersion,
        upgradeStatus,
        progress != null ? progress : 0);
  }
}
