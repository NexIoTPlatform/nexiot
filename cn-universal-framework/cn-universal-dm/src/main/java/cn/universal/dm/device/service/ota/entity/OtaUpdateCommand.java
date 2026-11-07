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
 * OTA升级命令实体
 *
 * <p>用于封装平台向设备下发的固件升级命令
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/3/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtaUpdateCommand {

  /** 产品Key */
  private String productKey;

  /** 设备ID */
  private String deviceId;

  /** 升级任务ID */
  private String taskId;

  /** 命令类型 */
  private CommandType commandType;

  /** 固件信息 */
  private FirmwareInfo firmwareInfo;

  /** 升级配置 */
  private UpgradeConfig upgradeConfig;

  /** 重试策略 */
  private RetryStrategy retryStrategy;

  /** 验证配置 */
  private VerificationConfig verificationConfig;

  /** 扩展参数 */
  private Map<String, Object> extraParams;

  /** 命令发送时间 */
  @Builder.Default private LocalDateTime commandTime = LocalDateTime.now();

  /** 命令优先级（1-10，数字越大优先级越高） */
  @Builder.Default private Integer priority = 5;

  /** 协议类型 */
  private String protocol;

  /** 命令类型枚举 */
  public enum CommandType {
    /** 开始升级 */
    START_UPGRADE("开始升级"),

    /** 暂停升级 */
    PAUSE_UPGRADE("暂停升级"),

    /** 恢复升级 */
    RESUME_UPGRADE("恢复升级"),

    /** 取消升级 */
    CANCEL_UPGRADE("取消升级"),

    /** 重新开始升级 */
    RESTART_UPGRADE("重新开始升级"),

    /** 查询升级状态 */
    QUERY_STATUS("查询升级状态");

    private final String description;

    CommandType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** 固件信息 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FirmwareInfo {

    /** 固件名称 */
    private String firmwareName;

    /** 固件版本 */
    private String firmwareVersion;

    /** 固件类型 */
    private String firmwareType;

    /** 固件下载URL */
    private String downloadUrl;

    /** 固件大小（字节） */
    private Long fileSize;

    /** 文件校验和（MD5/SHA256等） */
    private String checksum;

    /** 校验和算法 */
    private String checksumAlgorithm;

    /** 固件描述 */
    private String description;

    /** 最低硬件版本要求 */
    private String minHardwareVersion;

    /** 最高硬件版本要求 */
    private String maxHardwareVersion;

    /** 发布时间 */
    private LocalDateTime releaseTime;

    /** 更新内容 */
    private String changeLog;

    /** 是否强制升级 */
    @Builder.Default private Boolean forcedUpgrade = false;

    /** 固件标签 */
    private Map<String, String> tags;
  }

  /** 升级配置 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpgradeConfig {

    /** 升级模式 */
    @Builder.Default private UpgradeMode upgradeMode = UpgradeMode.NORMAL;

    /** 超时时间（秒） */
    @Builder.Default private Integer timeoutSeconds = 3600;

    /** 升级窗口开始时间（24小时制，如：20:00） */
    private String upgradeWindowStart;

    /** 升级窗口结束时间（24小时制，如：06:00） */
    private String upgradeWindowEnd;

    /** 是否在低电量时升级 */
    @Builder.Default private Boolean allowLowBattery = false;

    /** 最低电量要求（百分比） */
    @Builder.Default private Integer minBatteryLevel = 20;

    /** 是否需要WiFi */
    @Builder.Default private Boolean requireWifi = false;

    /** 是否在充电时升级 */
    @Builder.Default private Boolean requireCharging = false;

    /** 下载分块大小（字节） */
    @Builder.Default private Integer chunkSize = 4096;

    /** 下载并发数 */
    @Builder.Default private Integer maxConcurrent = 1;

    /** 是否自动重启 */
    @Builder.Default private Boolean autoReboot = true;

    /** 重启延迟（秒） */
    @Builder.Default private Integer rebootDelaySeconds = 10;

    /** 备份当前固件 */
    @Builder.Default private Boolean backupCurrent = true;
  }

  /** 升级模式枚举 */
  public enum UpgradeMode {
    /** 普通模式（下载完成后升级） */
    NORMAL("普通模式"),

    /** 增量模式（差分升级） */
    INCREMENTAL("增量模式"),

    /** 流式模式（边下载边升级） */
    STREAMING("流式模式");

    private final String description;

    UpgradeMode(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** 重试策略 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RetryStrategy {

    /** 最大重试次数 */
    @Builder.Default private Integer maxRetries = 3;

    /** 重试间隔（秒） */
    @Builder.Default private Integer retryIntervalSeconds = 60;

    /** 重试间隔增长因子 */
    @Builder.Default private Double backoffMultiplier = 2.0;

    /** 最大重试间隔（秒） */
    @Builder.Default private Integer maxRetryIntervalSeconds = 3600;

    /** 失败类型重试策略 */
    private Map<String, Integer> failureTypeRetries;

    /** 是否在网络错误时重试 */
    @Builder.Default private Boolean retryOnNetworkError = true;

    /** 是否在校验失败时重试 */
    @Builder.Default private Boolean retryOnVerificationError = false;

    /** 是否在存储空间不足时重试 */
    @Builder.Default private Boolean retryOnStorageError = false;
  }

  /** 验证配置 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VerificationConfig {

    /** 是否验证数字签名 */
    @Builder.Default private Boolean verifySignature = true;

    /** 公钥信息 */
    private String publicKey;

    /** 签名算法 */
    @Builder.Default private String signatureAlgorithm = "SHA256withRSA";

    /** 是否验证证书链 */
    @Builder.Default private Boolean verifyCertificateChain = false;

    /** 证书颁发机构 */
    private String certificateAuthority;

    /** 验证超时时间（秒） */
    @Builder.Default private Integer verificationTimeoutSeconds = 300;

    /** 严格模式（验证失败时停止升级） */
    @Builder.Default private Boolean strictMode = true;
  }

  /** 获取设备唯一标识 */
  public String getDeviceUniqueId() {
    return productKey + ":" + deviceId;
  }

  /** 是否为开始升级命令 */
  public boolean isStartUpgradeCommand() {
    return commandType == CommandType.START_UPGRADE || commandType == CommandType.RESTART_UPGRADE;
  }

  /** 是否为控制类命令（暂停、恢复、取消） */
  public boolean isControlCommand() {
    return commandType == CommandType.PAUSE_UPGRADE
        || commandType == CommandType.RESUME_UPGRADE
        || commandType == CommandType.CANCEL_UPGRADE;
  }

  /** 是否为查询命令 */
  public boolean isQueryCommand() {
    return commandType == CommandType.QUERY_STATUS;
  }

  /** 获取升级超时时间戳 */
  public LocalDateTime getUpgradeTimeoutTime() {
    if (upgradeConfig == null || upgradeConfig.getTimeoutSeconds() == null) {
      return commandTime.plusHours(1); // 默认1小时超时
    }
    return commandTime.plusSeconds(upgradeConfig.getTimeoutSeconds());
  }

  /** 检查是否在升级窗口内 */
  public boolean isInUpgradeWindow() {
    if (upgradeConfig == null
        || upgradeConfig.getUpgradeWindowStart() == null
        || upgradeConfig.getUpgradeWindowEnd() == null) {
      return true; // 无时间窗口限制
    }

    // 简化实现，实际应该根据时区和具体时间判断
    return true;
  }

  /** 计算下一次重试时间 */
  public LocalDateTime getNextRetryTime(int currentRetryCount) {
    if (retryStrategy == null) {
      return commandTime.plusMinutes(1);
    }

    int intervalSeconds = retryStrategy.getRetryIntervalSeconds();
    for (int i = 0; i < currentRetryCount; i++) {
      intervalSeconds = (int) (intervalSeconds * retryStrategy.getBackoffMultiplier());
      if (intervalSeconds > retryStrategy.getMaxRetryIntervalSeconds()) {
        intervalSeconds = retryStrategy.getMaxRetryIntervalSeconds();
        break;
      }
    }

    return LocalDateTime.now().plusSeconds(intervalSeconds);
  }

  @Override
  public String toString() {
    return String.format(
        "OtaUpdateCommand{device=%s, task=%s, type=%s, version=%s, priority=%d}",
        getDeviceUniqueId(),
        taskId,
        commandType,
        firmwareInfo != null ? firmwareInfo.getFirmwareVersion() : "unknown",
        priority);
  }
}
