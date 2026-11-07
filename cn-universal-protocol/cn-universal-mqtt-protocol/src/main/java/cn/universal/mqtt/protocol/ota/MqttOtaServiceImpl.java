/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: MQTT协议OTA服务实现
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.mqtt.protocol.ota;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.dm.device.service.ota.api.OtaService;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.persistence.entity.IoTOtaPackage;
import cn.universal.persistence.entity.IoTOtaTask;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.mapper.IoTOtaPackageMapper;
import cn.universal.persistence.mapper.IoTOtaTaskDeviceMapper;
import cn.universal.persistence.mapper.IoTOtaTaskMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** MQTT OTA 服务，统一委托到 OTA 运行时状态机。 */
@Slf4j(topic = "mqtt")
@Service("mqttOtaService")
@RequiredArgsConstructor
public class MqttOtaServiceImpl implements OtaService {

  private final IoTOtaRuntimeService otaRuntimeService;
  private final IoTOtaTaskMapper otaTaskMapper;
  private final IoTOtaTaskDeviceMapper otaTaskDeviceMapper;
  private final IoTOtaPackageMapper otaPackageMapper;

  @Override
  public String getServiceType() {
    return "MQTT";
  }

  @Override
  public String getServiceDescription() {
    return "基于MQTT协议的OTA升级服务";
  }

  @Override
  public boolean supportsProduct(String productKey) {
    return StrUtil.isNotBlank(productKey);
  }

  @Override
  public boolean isServiceAvailable() {
    return true;
  }

  @Override
  public OtaServiceResult handleFirmwareReport(Map<String, Object> reportData) {
    return otaRuntimeService.handleFirmwareReport(reportData);
  }

  @Override
  public OtaServiceResult handleUpgradeProgress(Map<String, Object> progressData) {
    return otaRuntimeService.handleUpgradeProgress(progressData);
  }

  @Override
  public OtaServiceResult handleUpgradeResult(Map<String, Object> resultData) {
    return otaRuntimeService.handleUpgradeResult(resultData);
  }

  @Override
  public OtaServiceResult sendUpgradeCommand(Map<String, Object> updateData) {
    return otaRuntimeService.dispatchUpgradeCommand(updateData);
  }

  @Override
  public OtaServiceResult cancelUpgrade(String productKey, String deviceId, String taskId) {
    if (StrUtil.isNotBlank(taskId) && StrUtil.isNumeric(taskId)) {
      return otaRuntimeService.cancelTask(Convert.toLong(taskId));
    }
    if (StrUtil.isNotBlank(productKey) && StrUtil.isNotBlank(deviceId)) {
      IoTOtaTaskDevice latestTaskDevice =
          otaTaskDeviceMapper.selectLatestActiveByDevice(productKey, deviceId);
      if (latestTaskDevice != null) {
        return otaRuntimeService.cancelTask(latestTaskDevice.getTaskId());
      }
    }
    return OtaServiceResult.UNSUPPORTED_OPERATION;
  }

  @Override
  public Map<String, Object> queryUpgradeStatus(String productKey, String deviceId, String taskId) {
    Map<String, Object> result = new HashMap<>();
    IoTOtaTaskDevice taskDevice = resolveTaskDevice(productKey, deviceId, taskId);
    if (taskDevice == null) {
      result.put("success", false);
      result.put("message", "未找到升级任务");
      return result;
    }

    result.put("success", true);
    result.put("taskId", String.valueOf(taskDevice.getTaskId()));
    result.put("taskDeviceId", taskDevice.getId());
    result.put("upgradeStatus", taskDevice.getStatus());
    result.put("progress", taskDevice.getProgress());
    result.put("currentVersion", taskDevice.getCurrentVersion());
    result.put("targetVersion", taskDevice.getTargetVersion());
    result.put("statusDetail", taskDevice.getStatusDetail());
    result.put("lastReportTime", taskDevice.getLastReportTime());
    result.put("retryCount", taskDevice.getRetryCount());
    return result;
  }

  @Override
  public List<Map<String, Object>> getAvailablePackages(String productKey) {
    IoTOtaPackage query = IoTOtaPackage.builder().productKey(productKey).build();
    List<IoTOtaPackage> packages = otaPackageMapper.selectPackageList(query);
    List<Map<String, Object>> result = new ArrayList<>();
    for (IoTOtaPackage item : packages) {
      if (!OtaConstants.PackageStatus.PUBLISHED.equals(item.getStatus())) {
        continue;
      }
      Map<String, Object> packageInfo = new HashMap<>();
      packageInfo.put("packageId", item.getId());
      packageInfo.put("packageName", item.getPackageName());
      packageInfo.put("firmwareVersion", item.getVersion());
      packageInfo.put("packageType", item.getPackageType());
      packageInfo.put("customType", item.getCustomType());
      packageInfo.put("module", item.getModule());
      packageInfo.put("downloadUrl", item.getFileUrl());
      packageInfo.put("fileSize", item.getFileSize());
      packageInfo.put("checksum", item.getChecksum());
      packageInfo.put("checksumAlgorithm", item.getChecksumAlgorithm());
      packageInfo.put("releaseNotes", item.getReleaseNotes());
      result.add(packageInfo);
    }
    return result;
  }

  @Override
  public Map<String, Object> getPackageInfo(String productKey, String packageVersion) {
    IoTOtaPackage query =
        IoTOtaPackage.builder().productKey(productKey).version(packageVersion).build();
    List<IoTOtaPackage> packages = otaPackageMapper.selectPackageList(query);
    if (packages == null || packages.isEmpty()) {
      return null;
    }
    IoTOtaPackage item = packages.get(0);
    Map<String, Object> packageInfo = new HashMap<>();
    packageInfo.put("packageId", item.getId());
    packageInfo.put("packageName", item.getPackageName());
    packageInfo.put("firmwareVersion", item.getVersion());
    packageInfo.put("packageType", item.getPackageType());
    packageInfo.put("customType", item.getCustomType());
    packageInfo.put("module", item.getModule());
    packageInfo.put("downloadUrl", item.getFileUrl());
    packageInfo.put("fileSize", item.getFileSize());
    packageInfo.put("checksum", item.getChecksum());
    packageInfo.put("checksumAlgorithm", item.getChecksumAlgorithm());
    packageInfo.put("releaseNotes", item.getReleaseNotes());
    return packageInfo;
  }

  @Override
  public boolean validatePackage(Map<String, Object> packageInfo) {
    return packageInfo != null
        && StrUtil.isNotBlank(Convert.toStr(packageInfo.get("firmwareVersion")))
        && StrUtil.isNotBlank(Convert.toStr(packageInfo.get("downloadUrl")));
  }

  @Override
  public OtaStatistics getStatistics(String productKey) {
    List<IoTOtaTask> tasks =
        otaTaskMapper.selectTaskList(IoTOtaTask.builder().productKey(productKey).build());
    OtaStatistics statistics = new OtaStatistics();
    statistics.setTotalUpgrades(tasks.size());
    statistics.setSuccessfulUpgrades(
        tasks.stream().filter(task -> OtaConstants.TaskStatus.COMPLETED.equals(task.getStatus())).count());
    statistics.setFailedUpgrades(
        tasks.stream()
            .filter(
                task ->
                    OtaConstants.TaskStatus.FAILED.equals(task.getStatus())
                        || OtaConstants.TaskStatus.PARTIAL_SUCCESS.equals(task.getStatus()))
            .count());
    statistics.setActiveUpgrades(getActiveUpgradeCount(productKey));
    return statistics;
  }

  @Override
  public int getActiveUpgradeCount(String productKey) {
    List<IoTOtaTask> tasks =
        otaTaskMapper.selectTaskList(IoTOtaTask.builder().productKey(productKey).build());
    return (int)
        tasks.stream()
            .filter(
                task ->
                    OtaConstants.TaskStatus.PENDING.equals(task.getStatus())
                        || OtaConstants.TaskStatus.RUNNING.equals(task.getStatus()))
            .count();
  }

  @Override
  public double getUpgradeSuccessRate(String productKey) {
    OtaStatistics statistics = getStatistics(productKey);
    return statistics.getSuccessRate();
  }

  private IoTOtaTaskDevice resolveTaskDevice(String productKey, String deviceId, String taskId) {
    if (StrUtil.isNotBlank(taskId) && StrUtil.isNumeric(taskId)) {
      IoTOtaTaskDevice query =
          IoTOtaTaskDevice.builder()
              .taskId(Convert.toLong(taskId))
              .productKey(productKey)
              .deviceId(deviceId)
              .build();
      List<IoTOtaTaskDevice> taskDevices = otaTaskDeviceMapper.selectTaskDeviceList(query);
      if (taskDevices != null && !taskDevices.isEmpty()) {
        return taskDevices.get(0);
      }
    }
    if (StrUtil.isNotBlank(productKey) && StrUtil.isNotBlank(deviceId)) {
      IoTOtaTaskDevice activeTaskDevice =
          otaTaskDeviceMapper.selectLatestActiveByDevice(productKey, deviceId);
      if (activeTaskDevice != null) {
        return activeTaskDevice;
      }
      List<IoTOtaTaskDevice> recentTaskDevices =
          otaTaskDeviceMapper.selectRecentByDevice(productKey, deviceId);
      if (recentTaskDevices != null && !recentTaskDevices.isEmpty()) {
        return recentTaskDevices.get(0);
      }
    }
    return null;
  }
}
