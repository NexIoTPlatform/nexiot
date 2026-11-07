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

package cn.universal.dm.device.service.ota.api;

import cn.universal.core.service.IOta;
import java.util.List;
import java.util.Map;

/**
 * OTA服务通用接口
 *
 * <p>定义设备固件升级的通用服务接口，支持多种协议实现： - MQTT协议实现 - HTTP协议实现 - TCP协议实现 - 其他自定义协议实现
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/3/22
 */
public interface OtaService extends IOta {

  /**
   * 获取OTA服务类型
   *
   * @return OTA服务类型（如：MQTT、HTTP、TCP等）
   */
  String getServiceType();

  /**
   * 获取OTA服务描述
   *
   * @return 服务描述信息
   */
  String getServiceDescription();

  /**
   * 检查服务是否支持指定产品
   *
   * @param productKey 产品Key
   * @return true-支持，false-不支持
   */
  boolean supportsProduct(String productKey);

  /**
   * 检查服务是否可用
   *
   * @return true-可用，false-不可用
   */
  boolean isServiceAvailable();

  // ==================== 设备端上报接口 ====================

  /**
   * 处理设备上报的固件信息
   *
   * @param reportData 固件上报数据
   * @return 处理结果
   */
  OtaServiceResult handleFirmwareReport(Map<String, Object> reportData);

  /**
   * 处理设备上报的升级进度
   *
   * @param progressData 进度上报数据
   * @return 处理结果
   */
  OtaServiceResult handleUpgradeProgress(Map<String, Object> progressData);

  /**
   * 处理设备上报的升级结果
   *
   * @param resultData 结果上报数据
   * @return 处理结果
   */
  OtaServiceResult handleUpgradeResult(Map<String, Object> resultData);

  // ==================== 平台端下发接口 ====================

  /**
   * 下发固件升级指令
   *
   * @param updateData 升级数据
   * @return 下发结果
   */
  OtaServiceResult sendUpgradeCommand(Map<String, Object> updateData);

  /**
   * 取消固件升级
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param taskId 升级任务ID
   * @return 取消结果
   */
  OtaServiceResult cancelUpgrade(String productKey, String deviceId, String taskId);

  /**
   * 查询升级状态
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param taskId 升级任务ID
   * @return 升级状态数据
   */
  Map<String, Object> queryUpgradeStatus(String productKey, String deviceId, String taskId);

  // ==================== 固件包管理接口 ====================

  /**
   * 获取可用的固件包列表
   *
   * @param productKey 产品Key
   * @return 固件包列表
   */
  List<Map<String, Object>> getAvailablePackages(String productKey);

  /**
   * 获取指定版本的固件包信息
   *
   * @param productKey 产品Key
   * @param packageVersion 固件包版本
   * @return 固件包信息
   */
  Map<String, Object> getPackageInfo(String productKey, String packageVersion);

  /**
   * 验证固件包
   *
   * @param packageInfo 固件包信息
   * @return 验证结果
   */
  boolean validatePackage(Map<String, Object> packageInfo);

  // ==================== 统计和监控接口 ====================

  /**
   * 获取OTA统计信息
   *
   * @param productKey 产品Key（可选，为null时返回全局统计）
   * @return 统计信息
   */
  OtaStatistics getStatistics(String productKey);

  /**
   * 获取活跃升级任务数量
   *
   * @param productKey 产品Key（可选）
   * @return 活跃任务数量
   */
  int getActiveUpgradeCount(String productKey);

  /**
   * 获取升级成功率
   *
   * @param productKey 产品Key（可选）
   * @return 成功率（0.0-1.0）
   */
  double getUpgradeSuccessRate(String productKey);

  // ==================== 内部枚举和类 ====================

  /** OTA服务结果 */
  enum OtaServiceResult {
    SUCCESS("成功"),
    FAILED("失败"),
    DEVICE_OFFLINE("设备离线"),
    PACKAGE_NOT_FOUND("固件包不存在"),
    VERSION_CONFLICT("版本冲突"),
    INSUFFICIENT_SPACE("存储空间不足"),
    NETWORK_ERROR("网络错误"),
    PROTOCOL_ERROR("协议错误"),
    UNSUPPORTED_OPERATION("不支持的操作");

    private final String description;

    OtaServiceResult(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** OTA统计信息 */
  class OtaStatistics {

    private long totalUpgrades; // 总升级次数
    private long successfulUpgrades; // 成功升级次数
    private long failedUpgrades; // 失败升级次数
    private long activeUpgrades; // 活跃升级数
    private double averageUpgradeTime; // 平均升级时间（分钟）
    private String lastUpgradeTime; // 最后升级时间

    // Constructors
    public OtaStatistics() {}

    public OtaStatistics(
        long totalUpgrades,
        long successfulUpgrades,
        long failedUpgrades,
        long activeUpgrades,
        double averageUpgradeTime,
        String lastUpgradeTime) {
      this.totalUpgrades = totalUpgrades;
      this.successfulUpgrades = successfulUpgrades;
      this.failedUpgrades = failedUpgrades;
      this.activeUpgrades = activeUpgrades;
      this.averageUpgradeTime = averageUpgradeTime;
      this.lastUpgradeTime = lastUpgradeTime;
    }

    // Getters and Setters
    public long getTotalUpgrades() {
      return totalUpgrades;
    }

    public void setTotalUpgrades(long totalUpgrades) {
      this.totalUpgrades = totalUpgrades;
    }

    public long getSuccessfulUpgrades() {
      return successfulUpgrades;
    }

    public void setSuccessfulUpgrades(long successfulUpgrades) {
      this.successfulUpgrades = successfulUpgrades;
    }

    public long getFailedUpgrades() {
      return failedUpgrades;
    }

    public void setFailedUpgrades(long failedUpgrades) {
      this.failedUpgrades = failedUpgrades;
    }

    public long getActiveUpgrades() {
      return activeUpgrades;
    }

    public void setActiveUpgrades(long activeUpgrades) {
      this.activeUpgrades = activeUpgrades;
    }

    public double getAverageUpgradeTime() {
      return averageUpgradeTime;
    }

    public void setAverageUpgradeTime(double averageUpgradeTime) {
      this.averageUpgradeTime = averageUpgradeTime;
    }

    public String getLastUpgradeTime() {
      return lastUpgradeTime;
    }

    public void setLastUpgradeTime(String lastUpgradeTime) {
      this.lastUpgradeTime = lastUpgradeTime;
    }

    public double getSuccessRate() {
      return totalUpgrades > 0 ? (double) successfulUpgrades / totalUpgrades : 0.0;
    }

    @Override
    public String toString() {
      return String.format(
          "OtaStatistics{total=%d, success=%d, failed=%d, active=%d, avgTime=%.2fmin, successRate=%.2f%%}",
          totalUpgrades,
          successfulUpgrades,
          failedUpgrades,
          activeUpgrades,
          averageUpgradeTime,
          getSuccessRate() * 100);
    }
  }
}
