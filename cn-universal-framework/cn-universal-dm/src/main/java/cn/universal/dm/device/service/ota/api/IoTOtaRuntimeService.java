/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA运行时服务接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.dm.device.service.ota.api;

import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.persistence.base.BaseUPRequest;

/** OTA运行时服务 */
public interface IoTOtaRuntimeService {

  OtaServiceResult handleFirmwareReport(java.util.Map<String, Object> reportData);

  OtaServiceResult handleUpgradeProgress(java.util.Map<String, Object> progressData);

  OtaServiceResult handleUpgradeResult(java.util.Map<String, Object> resultData);

  OtaServiceResult dispatchUpgradeCommand(java.util.Map<String, Object> updateData);

  OtaServiceResult dispatchTask(Long taskId);

  OtaServiceResult dispatchTaskDevice(Long taskDeviceId);

  OtaServiceResult cancelTask(Long taskId);

  OtaServiceResult retryTaskDevice(Long taskDeviceId);

  boolean handleDecodedReport(BaseUPRequest upRequest, String productKey, String deviceId, String protocol);

  void refreshTaskStats(Long taskId);

  void processScheduledTasks();

  void processTaskTimeouts(Long taskId);
}
