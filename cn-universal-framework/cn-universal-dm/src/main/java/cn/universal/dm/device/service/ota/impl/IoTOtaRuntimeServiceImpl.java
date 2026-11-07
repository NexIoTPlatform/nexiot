/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA运行时服务实现
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.dm.device.service.ota.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.constant.IoTConstant;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.domain.R;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.dm.device.service.ota.support.OtaReportExtractor;
import cn.universal.dm.device.service.ota.support.OtaVersionUtils;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.IoTOtaDeviceVersion;
import cn.universal.persistence.entity.IoTOtaPackage;
import cn.universal.persistence.entity.IoTOtaTask;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTOtaDeviceVersionMapper;
import cn.universal.persistence.mapper.IoTOtaPackageMapper;
import cn.universal.persistence.mapper.IoTOtaTaskDeviceMapper;
import cn.universal.persistence.mapper.IoTOtaTaskMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** OTA运行时服务实现 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IoTOtaRuntimeServiceImpl implements IoTOtaRuntimeService {

  private static final String OTA_DISPATCH_FAILED_EVENT = "OTA_DISPATCH_FAILED";

  private final IoTOtaTaskMapper otaTaskMapper;
  private final IoTOtaTaskDeviceMapper otaTaskDeviceMapper;
  private final IoTOtaPackageMapper otaPackageMapper;
  private final IoTOtaDeviceVersionMapper otaDeviceVersionMapper;
  private final IoTProductMapper productMapper;
  private final IoTDeviceMapper deviceMapper;
  private final OtaReportExtractor otaReportExtractor;
  private final OtaTransferService otaTransferService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OtaServiceResult handleFirmwareReport(Map<String, Object> reportData) {
    String productKey = stringValue(reportData, "productKey");
    String deviceId = stringValue(reportData, "deviceId");
    if (StrUtil.isBlank(productKey) || StrUtil.isBlank(deviceId)) {
      return OtaServiceResult.PROTOCOL_ERROR;
    }

    IoTOtaTaskDevice taskDevice = lockReportDevice(resolveTaskDevice(reportData, productKey, deviceId));
    if (taskDevice != null && StrUtil.isNotBlank(stringValue(reportData, "sessionId"))
        && !otaTransferService.matchesSession(taskDevice, reportData)) return OtaServiceResult.PROTOCOL_ERROR;
    String currentVersion = firstNonBlank(reportData, "currentVersion", "version");
    updateDeviceVersionSnapshot(reportData, taskDevice, currentVersion, null);

    if (taskDevice != null
        && StrUtil.isNotBlank(currentVersion)
        && StrUtil.equals(currentVersion, taskDevice.getTargetVersion())
        && !OtaConstants.DeviceStatus.WAITING_PUSH.equals(taskDevice.getStatus())
        && otaTransferService.canConfirmInstallation(taskDevice)
        && !OtaConstants.isFinalDeviceStatus(taskDevice.getStatus())) {
      // 只有已经下发过的任务才允许通过版本确认补判成功，避免未下发任务被普通版本上报误收口。
      taskDevice.setStatus(OtaConstants.DeviceStatus.SUCCESS);
      taskDevice.setStatusDetail("设备已上报目标版本，自动判定升级成功");
      taskDevice.setProgress(100);
      taskDevice.setCurrentVersion(currentVersion);
      taskDevice.setLastReportTime(new Date());
      taskDevice.setSuccessTime(new Date());
      taskDevice.setRawReport(JSONUtil.toJsonStr(reportData));
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(taskDevice.getTaskId());
    }
    return OtaServiceResult.SUCCESS;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OtaServiceResult handleUpgradeProgress(Map<String, Object> progressData) {
    String productKey = stringValue(progressData, "productKey");
    String deviceId = stringValue(progressData, "deviceId");
    if (StrUtil.isBlank(productKey) || StrUtil.isBlank(deviceId)) {
      return OtaServiceResult.PROTOCOL_ERROR;
    }

    IoTOtaTaskDevice taskDevice = lockReportDevice(resolveTaskDevice(progressData, productKey, deviceId));
    if (taskDevice == null) {
      updateDeviceVersionSnapshot(progressData, null, firstNonBlank(progressData, "currentVersion", "version"), null);
      return OtaServiceResult.SUCCESS;
    }
    if (OtaConstants.isFinalDeviceStatus(taskDevice.getStatus())) {
      // 终态由结果上报或平台超时产生，迟到的进度包不能把它改回执行中。
      updateDeviceVersionSnapshot(
          progressData,
          taskDevice,
          firstNonBlank(progressData, "currentVersion", "version"),
          taskDevice.getStatus());
      return OtaServiceResult.SUCCESS;
    }

    if (!otaTransferService.matchesSession(taskDevice, progressData)) {
      return OtaServiceResult.PROTOCOL_ERROR;
    }

    Integer progress = Convert.toInt(progressData.get("progress"), taskDevice.getProgress());
    String normalizedStatus = normalizeDeviceStatus(stringValue(progressData, "upgradeStatus"), progress, false);
    if (OtaConstants.DeviceStatus.SUCCESS.equals(normalizedStatus)
        && !otaTransferService.canConfirmInstallation(taskDevice)) return OtaServiceResult.PROTOCOL_ERROR;
    taskDevice.setProgress(progress);
    taskDevice.setStatus(normalizedStatus);
    taskDevice.setStatusDetail(buildStatusDetail(progressData, normalizedStatus));
    taskDevice.setLastReportTime(new Date());
    taskDevice.setRawReport(JSONUtil.toJsonStr(progressData));
    if (StrUtil.isNotBlank(firstNonBlank(progressData, "currentVersion", "version"))) {
      taskDevice.setCurrentVersion(firstNonBlank(progressData, "currentVersion", "version"));
    }
    if (OtaConstants.DeviceStatus.SUCCESS.equals(normalizedStatus)) {
      taskDevice.setSuccessTime(new Date());
      updateDeviceVersionSnapshot(progressData, taskDevice, taskDevice.getTargetVersion(), OtaConstants.DeviceStatus.SUCCESS);
    } else {
      updateDeviceVersionSnapshot(progressData, taskDevice, taskDevice.getCurrentVersion(), normalizedStatus);
    }
    if (otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice) > 0) {
      refreshTaskStats(taskDevice.getTaskId());
    }
    return OtaServiceResult.SUCCESS;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OtaServiceResult handleUpgradeResult(Map<String, Object> resultData) {
    String productKey = stringValue(resultData, "productKey");
    String deviceId = stringValue(resultData, "deviceId");
    if (StrUtil.isBlank(productKey) || StrUtil.isBlank(deviceId)) {
      return OtaServiceResult.PROTOCOL_ERROR;
    }

    IoTOtaTaskDevice taskDevice = lockReportDevice(resolveTaskDevice(resultData, productKey, deviceId));
    if (taskDevice != null && !otaTransferService.matchesSession(taskDevice, resultData)) {
      return OtaServiceResult.PROTOCOL_ERROR;
    }
    String normalizedStatus = normalizeDeviceStatus(stringValue(resultData, "upgradeStatus"), Convert.toInt(resultData.get("progress")), true);
    if (taskDevice != null && OtaConstants.DeviceStatus.SUCCESS.equals(normalizedStatus)
        && !otaTransferService.canConfirmInstallation(taskDevice)) return OtaServiceResult.PROTOCOL_ERROR;
    String currentVersion = firstNonBlank(resultData, "currentVersion", "version");

    if (taskDevice != null) {
      if (OtaConstants.isFinalDeviceStatus(taskDevice.getStatus())) {
        updateDeviceVersionSnapshot(resultData, taskDevice, currentVersion, taskDevice.getStatus());
        return OtaServiceResult.SUCCESS;
      }
      taskDevice.setStatus(normalizedStatus);
      taskDevice.setStatusDetail(buildStatusDetail(resultData, normalizedStatus));
      taskDevice.setErrorCode(stringValue(resultData, "errorCode"));
      taskDevice.setErrorMessage(stringValue(resultData, "errorMessage"));
      taskDevice.setLastReportTime(new Date());
      taskDevice.setRawReport(JSONUtil.toJsonStr(resultData));
      taskDevice.setProgress(OtaConstants.DeviceStatus.SUCCESS.equals(normalizedStatus) ? 100 : Convert.toInt(resultData.get("progress"), taskDevice.getProgress()));
      if (OtaConstants.DeviceStatus.SUCCESS.equals(normalizedStatus)) {
        taskDevice.setCurrentVersion(StrUtil.blankToDefault(currentVersion, taskDevice.getTargetVersion()));
        taskDevice.setSuccessTime(new Date());
      }
      if (otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice) > 0) {
        refreshTaskStats(taskDevice.getTaskId());
      }
    }

    updateDeviceVersionSnapshot(resultData, taskDevice, currentVersion, normalizedStatus);
    return OtaServiceResult.SUCCESS;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OtaServiceResult dispatchUpgradeCommand(Map<String, Object> updateData) {
    String taskDeviceId = Convert.toStr(updateData.get("taskDeviceId"));
    if (StrUtil.isNotBlank(taskDeviceId) && StrUtil.isNumeric(taskDeviceId)) {
      return dispatchTaskDevice(Convert.toLong(taskDeviceId));
    }

    String taskId = Convert.toStr(updateData.get("taskId"));
    if (StrUtil.isNotBlank(taskId) && StrUtil.isNumeric(taskId)) {
      return dispatchTask(Convert.toLong(taskId));
    }

    String productKey = Convert.toStr(updateData.get("productKey"));
    String deviceId = Convert.toStr(updateData.get("deviceId"));
    if (StrUtil.isNotBlank(productKey) && StrUtil.isNotBlank(deviceId)) {
      IoTOtaTaskDevice latestTaskDevice =
          otaTaskDeviceMapper.selectLatestActiveByDevice(productKey, deviceId);
      if (latestTaskDevice != null) {
        return dispatchTaskDevice(latestTaskDevice.getId());
      }
    }
    return OtaServiceResult.UNSUPPORTED_OPERATION;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OtaServiceResult dispatchTask(Long taskId) {
    IoTOtaTask task = otaTaskMapper.selectTaskForUpdate(taskId);
    if (task == null) {
      return OtaServiceResult.FAILED;
    }
    List<IoTOtaTaskDevice> taskDevices = otaTaskDeviceMapper.selectByTaskId(taskId);
    if (taskDevices == null || taskDevices.isEmpty()) {
      return OtaServiceResult.FAILED;
    }
    JSONObject strategy = parseStrategy(task);
    List<IoTOtaTaskDevice> waitingDevices = new ArrayList<>();
    for (IoTOtaTaskDevice taskDevice : taskDevices) {
      if (OtaConstants.DeviceStatus.WAITING_PUSH.equals(taskDevice.getStatus())) {
        waitingDevices.add(taskDevice);
      }
    }
    waitingDevices.sort((left, right) -> Long.compare(left.getId(), right.getId()));
    if (waitingDevices.isEmpty()) {
      refreshTaskStats(taskId);
      return OtaServiceResult.SUCCESS;
    }

    task.setStatus(OtaConstants.TaskStatus.RUNNING);
    if (task.getStartTime() == null) {
      task.setStartTime(new Date());
    }
    otaTaskMapper.updateTask(task);

    OtaServiceResult lastResult = OtaServiceResult.SUCCESS;
    int dispatchCount = resolveDispatchCount(waitingDevices, strategy);
    for (int i = 0; i < dispatchCount; i++) {
      lastResult = dispatchTaskDevice(waitingDevices.get(i).getId());
    }
    if (dispatchCount > 0) {
      strategy.set(OtaConstants.StrategyKey.LAST_BATCH_DISPATCH_TIME, System.currentTimeMillis());
      strategy.set(
          OtaConstants.StrategyKey.CURRENT_BATCH_NO,
          strategy.getInt(OtaConstants.StrategyKey.CURRENT_BATCH_NO, 0) + 1);
      persistTaskStrategy(task, strategy);
    }
    refreshTaskStats(taskId);
    return lastResult;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OtaServiceResult dispatchTaskDevice(Long taskDeviceId) {
    IoTOtaTaskDevice taskDevice = otaTaskDeviceMapper.selectTaskDeviceById(taskDeviceId);
    if (taskDevice == null) {
      return OtaServiceResult.FAILED;
    }
    IoTOtaTask task = otaTaskMapper.selectTaskForUpdate(taskDevice.getTaskId());
    taskDevice = otaTaskDeviceMapper.selectTaskDeviceForUpdate(taskDeviceId);
    if (taskDevice == null) return OtaServiceResult.FAILED;
    if (OtaConstants.isFinalDeviceStatus(taskDevice.getStatus())) {
      return OtaServiceResult.SUCCESS;
    }
    if (!OtaConstants.DeviceStatus.WAITING_PUSH.equals(taskDevice.getStatus())) {
      return OtaServiceResult.SUCCESS;
    }
    IoTOtaPackage otaPackage = otaPackageMapper.selectPackageById(taskDevice.getPackageId());
    if (task == null || otaPackage == null) {
      taskDevice.setStatus(OtaConstants.DeviceStatus.FAILED);
      taskDevice.setStatusDetail("任务或升级包不存在，无法下发 OTA 指令");
      taskDevice.setErrorMessage("task or ota package not found");
      taskDevice.setRawReport(buildDispatchFailureLog(task, taskDevice, otaPackage, null, null, "任务或升级包不存在"));
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(taskDevice.getTaskId());
      return OtaServiceResult.PACKAGE_NOT_FOUND;
    }
    if (!isDispatchablePackage(otaPackage)) {
      taskDevice.setStatus(OtaConstants.DeviceStatus.FAILED);
      taskDevice.setStatusDetail("升级包未发布或缺少强校验信息，已拒绝下发");
      taskDevice.setErrorMessage("ota package is not dispatchable");
      taskDevice.setRawReport(
          buildDispatchFailureLog(task, taskDevice, otaPackage, null, null, "升级包未发布或缺少强校验信息"));
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(task.getId());
      return OtaServiceResult.PACKAGE_NOT_FOUND;
    }
    IoTProduct product = productMapper.getProductByProductKey(taskDevice.getProductKey());
    if (product == null) {
      String detail = "产品不存在: " + taskDevice.getProductKey();
      taskDevice.setStatus(OtaConstants.DeviceStatus.FAILED);
      taskDevice.setStatusDetail(detail);
      taskDevice.setErrorMessage(detail);
      taskDevice.setRawReport(buildDispatchFailureLog(task, taskDevice, otaPackage, null, null, detail));
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(task.getId());
      return OtaServiceResult.PROTOCOL_ERROR;
    }
    IoTDevice device = deviceMapper.selectIoTDevice(taskDevice.getProductKey(), taskDevice.getDeviceId());
    if (device == null) {
      String detail = "设备不存在: " + taskDevice.getDeviceId();
      taskDevice.setStatus(OtaConstants.DeviceStatus.FAILED);
      taskDevice.setStatusDetail(detail);
      taskDevice.setErrorMessage(detail);
      taskDevice.setRawReport(buildDispatchFailureLog(task, taskDevice, otaPackage, null, null, detail));
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(task.getId());
      return OtaServiceResult.FAILED;
    }
    if (!device.isOnline()) {
      taskDevice.setStatus(OtaConstants.DeviceStatus.FAILED);
      taskDevice.setStatusDetail("设备离线，无法下发 OTA 指令");
      taskDevice.setErrorMessage("device offline");
      taskDevice.setRetryCount(defaultRetry(taskDevice) + 1);
      taskDevice.setRawReport(buildDispatchFailureLog(task, taskDevice, otaPackage, null, null, "设备离线"));
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(task.getId());
      return OtaServiceResult.DEVICE_OFFLINE;
    }

    String protocolCode = resolveProtocolCode(product);
    if (StrUtil.isBlank(protocolCode)) {
      String detail = "产品未配置可用的 OTA 下发协议";
      taskDevice.setStatus(OtaConstants.DeviceStatus.FAILED);
      taskDevice.setStatusDetail(detail);
      taskDevice.setErrorMessage(detail);
      taskDevice.setRawReport(buildDispatchFailureLog(task, taskDevice, otaPackage, null, null, detail));
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(task.getId());
      return OtaServiceResult.PROTOCOL_ERROR;
    }

    if (!shouldDispatch(task, taskDevice)) {
      taskDevice.setStatus(OtaConstants.DeviceStatus.SKIPPED);
      taskDevice.setStatusDetail("设备当前版本已达到或超过目标版本，已跳过");
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(task.getId());
      return OtaServiceResult.SUCCESS;
    }

    // 先用状态条件抢占 WAITING_PUSH，避免多节点调度或人工重复点击导致重复下发。
    if (otaTaskDeviceMapper.markWaitingAsDispatching(taskDevice.getId()) <= 0) {
      return OtaServiceResult.SUCCESS;
    }
    taskDevice.setStatus(OtaConstants.DeviceStatus.PUSHED);
    taskDevice.setStatusDetail("升级指令下发中");

    Map<String, Object> function = buildOtaFunction(task, taskDevice, otaPackage, OtaConstants.ACTION_START);
    try {
      otaTransferService.initialize(task, taskDevice, otaPackage, function);
    } catch (Exception e) {
      taskDevice.setStatus(OtaConstants.DeviceStatus.FAILED);
      taskDevice.setStatusDetail("OTA 传输准备失败: " + e.getMessage());
      taskDevice.setErrorMessage(taskDevice.getStatusDetail());
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(task.getId());
      return OtaServiceResult.PACKAGE_NOT_FOUND;
    }
    UnifiedDownlinkCommand command =
        UnifiedDownlinkCommand.builder()
            .productKey(taskDevice.getProductKey())
            .deviceId(taskDevice.getDeviceId())
            .iotId(taskDevice.getIotId())
            .cmd(DownCmd.DEV_FUNCTION)
            .function(function)
            .applicationId("system-ota")
            .appUnionId("system-ota")
            .msgId(IdUtil.fastSimpleUUID())
            .timestamp(System.currentTimeMillis())
            .detail(task.getTaskNo())
            .build();
    command.withSource("system-ota");

    taskDevice.setRetryCount(defaultRetry(taskDevice) + 1);
    taskDevice.setCommandPayload(JSONUtil.toJsonStr(function));
    R<?> result = null;
    String dispatchError = null;
    try {
      result = IoTDownlFactory.getIDownOrThrow(protocolCode).doAction(command);
    } catch (Exception e) {
      log.warn(
          "OTA指令下发异常, taskId={}, taskDeviceId={}, deviceId={}, error={}",
          task.getId(),
          taskDevice.getId(),
          taskDevice.getDeviceId(),
          e.getMessage());
      dispatchError = e.getMessage();
      taskDevice.setErrorMessage(dispatchError);
    }
    if (result != null && result.isSuccess()) {
      taskDevice.setStatus(OtaConstants.DeviceStatus.PUSHED);
      taskDevice.setStatusDetail("升级指令已下发，等待设备确认");
      taskDevice.setProgress(0);
      taskDevice.setPushedTime(new Date());
      taskDevice.setProtocol(protocolCode);
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(task.getId());
      return OtaServiceResult.SUCCESS;
    }

    if (StrUtil.isNotBlank(taskDevice.getTransferState())) {
      // 分包会话已有完整快照，首个 start 发送失败交由同样的有限重试恢复。
      taskDevice.setStatus(OtaConstants.DeviceStatus.PUSHED);
      taskDevice.setStatusDetail("升级准备指令发送失败，等待自动重试");
      taskDevice.setProtocol(protocolCode);
      taskDevice.setPushedTime(new Date());
      otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
      refreshTaskStats(task.getId());
      return OtaServiceResult.SUCCESS;
    }

    taskDevice.setStatus(OtaConstants.DeviceStatus.FAILED);
    String failureDetail =
        StrUtil.blankToDefault(
            result != null ? result.getMsg() : null,
            StrUtil.blankToDefault(dispatchError, "OTA 指令下发失败"));
    taskDevice.setStatusDetail(failureDetail);
    taskDevice.setErrorMessage(
        StrUtil.blankToDefault(
            taskDevice.getErrorMessage(), StrUtil.blankToDefault(result != null ? result.getMsg() : null, "OTA 指令下发失败")));
    taskDevice.setProtocol(protocolCode);
    taskDevice.setRawReport(buildDispatchFailureLog(task, taskDevice, otaPackage, command, result, failureDetail));
    otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice);
    refreshTaskStats(task.getId());
    return OtaServiceResult.NETWORK_ERROR;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OtaServiceResult cancelTask(Long taskId) {
    IoTOtaTask task = otaTaskMapper.selectTaskForUpdate(taskId);
    if (task == null) {
      return OtaServiceResult.FAILED;
    }
    List<IoTOtaTaskDevice> taskDevices = otaTaskDeviceMapper.selectByTaskId(taskId);
    if (taskDevices != null) {
      for (IoTOtaTaskDevice taskDevice : taskDevices) {
        // 与分包 ACK / 超时重发使用同一行锁，取消提交后再无后续分包发送。
        IoTOtaTaskDevice locked = otaTaskDeviceMapper.selectTaskDeviceForUpdate(taskDevice.getId());
        if (locked != null && !OtaConstants.isFinalDeviceStatus(locked.getStatus())) {
          sendCancelCommand(task, locked);
        }
      }
    }
    otaTaskDeviceMapper.updateStatusByTaskId(taskId, OtaConstants.DeviceStatus.CANCELED);
    task.setStatus(OtaConstants.TaskStatus.CANCELED);
    task.setCancelTime(new Date());
    task.setFinishTime(new Date());
    otaTaskMapper.updateTask(task);
    refreshTaskStats(taskId);
    return OtaServiceResult.SUCCESS;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public OtaServiceResult retryTaskDevice(Long taskDeviceId) {
    IoTOtaTaskDevice taskDevice = otaTaskDeviceMapper.selectTaskDeviceById(taskDeviceId);
    if (taskDevice == null) {
      return OtaServiceResult.FAILED;
    }
    otaTaskMapper.selectTaskForUpdate(taskDevice.getTaskId());
    taskDevice = otaTaskDeviceMapper.selectTaskDeviceForUpdate(taskDeviceId);
    if (taskDevice == null) return OtaServiceResult.FAILED;
    if (OtaConstants.DeviceStatus.SUCCESS.equals(taskDevice.getStatus())) {
      return OtaServiceResult.UNSUPPORTED_OPERATION;
    }
    if (defaultRetry(taskDevice) >= Convert.toInt(taskDevice.getMaxRetryCount(), 3)) {
      return OtaServiceResult.FAILED;
    }
    taskDevice.setStatus(OtaConstants.DeviceStatus.WAITING_PUSH);
    taskDevice.setProgress(0);
    taskDevice.setStatusDetail("已重置为重新下发");
    taskDevice.setErrorCode(null);
    taskDevice.setErrorMessage(null);
    taskDevice.setTransferState("");
    otaTaskDeviceMapper.updateTaskDevice(taskDevice);
    // 动态 patch 会忽略 null；重试必须显式清除数据库中的旧错误。
    otaTaskDeviceMapper.resetRetryFields(taskDeviceId);
    return dispatchTaskDevice(taskDeviceId);
  }

  @Override
  public void processScheduledTasks() {
    // 第一阶段：处理待执行任务。满足自动下发且达到计划时间后，开始首批设备下发。
    List<IoTOtaTask> pendingTasks =
        otaTaskMapper.selectTaskList(IoTOtaTask.builder().status(OtaConstants.TaskStatus.PENDING).build());
    for (IoTOtaTask task : pendingTasks) {
      if (isAutoDispatchEnabled(task) && isDueForDispatch(task)) {
        safeDispatchScheduledTask(task);
      }
    }

    // 第二阶段：处理运行中任务。若启用了分批发布，并且还有待下发设备，则按批次间隔继续放量。
    List<IoTOtaTask> runningTasks =
        otaTaskMapper.selectTaskList(IoTOtaTask.builder().status(OtaConstants.TaskStatus.RUNNING).build());
    for (IoTOtaTask task : runningTasks) {
      if (hasWaitingDevices(task) && shouldDispatchNextBatch(task)) {
        safeDispatchScheduledTask(task);
      }
      currentProxy().processTaskTimeouts(task.getId());
    }
  }

  @Override
  public boolean handleDecodedReport(
      BaseUPRequest upRequest, String productKey, String deviceId, String protocol) {
    // 先把不同协议、不同字段命名的上报内容抽取成统一 OTA 语义。
    // 这里并不要求协议层提前整理成固定格式，只要能从 payload/data/properties 中提取出 OTA 关键字段即可。
    Map<String, Object> reportData = otaReportExtractor.extract(upRequest, productKey, deviceId, protocol);
    if (reportData == null || reportData.isEmpty()) {
      return false;
    }

    // reportType 决定本次上报属于“版本信息”“过程进度”还是“最终结果”。
    // 当前推荐约定是:
    // 1. messageType = PROPERTIES -> 版本确认
    // 2. messageType = EVENT, event = otaProgress -> 过程进度
    // 3. messageType = EVENT, event = otaResult -> 最终结果
    // 若设备未显式携带 reportType，抽取器会结合 messageType/event/status 做归类。
    String reportType = stringValue(reportData, "reportType");
    OtaServiceResult result =
        switch (StrUtil.blankToDefault(reportType, OtaConstants.ReportType.FIRMWARE_INFO)) {
          case OtaConstants.ReportType.UPGRADE_PROGRESS -> currentProxy().handleUpgradeProgress(reportData);
          case OtaConstants.ReportType.UPGRADE_RESULT -> currentProxy().handleUpgradeResult(reportData);
          case OtaConstants.ReportType.TRANSFER_ACK, OtaConstants.ReportType.TRANSFER_REQUEST -> otaTransferService.handleReport(reportData);
          default -> currentProxy().handleFirmwareReport(reportData);
        };
    if (OtaConstants.ReportType.TRANSFER_ACK.equals(reportType)
        || OtaConstants.ReportType.TRANSFER_REQUEST.equals(reportType)) {
      IoTOtaTaskDevice device = resolveTaskDevice(reportData, productKey, deviceId);
      if (device != null) currentProxy().refreshTaskStats(device.getTaskId());
      // Transfer packets are consumed even when rejected, never treated as normal telemetry.
      return true;
    }
    return OtaServiceResult.SUCCESS.equals(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void refreshTaskStats(Long taskId) {
    IoTOtaTask task = otaTaskMapper.selectTaskForUpdate(taskId);
    if (task == null) {
      return;
    }
    // Locking read keeps aggregate status current under MySQL REPEATABLE READ as well.
    List<IoTOtaTaskDevice> taskDevices = otaTaskDeviceMapper.selectByTaskIdForUpdate(taskId);
    if (taskDevices == null) {
      taskDevices = new ArrayList<>();
    }
    int waiting = 0;
    int inProgress = 0;
    int success = 0;
    int failed = 0;
    int canceled = 0;
    int skipped = 0;
    for (IoTOtaTaskDevice device : taskDevices) {
      String status = device.getStatus();
      if (OtaConstants.DeviceStatus.WAITING_PUSH.equals(status)
          || OtaConstants.DeviceStatus.PUSHED.equals(status)) {
        waiting++;
      } else if (OtaConstants.DeviceStatus.DOWNLOADING.equals(status)
          || OtaConstants.DeviceStatus.VERIFYING.equals(status)
          || OtaConstants.DeviceStatus.INSTALLING.equals(status)
          || OtaConstants.DeviceStatus.REBOOTING.equals(status)) {
        inProgress++;
      } else if (OtaConstants.DeviceStatus.SUCCESS.equals(status)) {
        success++;
      } else if (OtaConstants.DeviceStatus.CANCELED.equals(status)) {
        canceled++;
      } else if (OtaConstants.DeviceStatus.SKIPPED.equals(status)) {
        skipped++;
      } else if (StrUtil.isNotBlank(status)) {
        failed++;
      }
    }

    task.setDeviceCount(taskDevices.size());
    task.setWaitingCount(waiting);
    task.setInProgressCount(inProgress);
    task.setSuccessCount(success);
    task.setFailedCount(failed);
    task.setCanceledCount(canceled);
    task.setSkippedCount(skipped);

    int total = taskDevices.size();
    if (maybeTriggerRollback(task, taskDevices)) {
      refreshTaskStats(taskId);
      return;
    }
    JSONObject strategy = parseStrategy(task);
    String nextStatus = task.getStatus();
    if (total == 0) {
      nextStatus = OtaConstants.TaskStatus.DRAFT;
    } else if (strategy.getBool(OtaConstants.StrategyKey.ROLLBACK_TRIGGERED, false)) {
      nextStatus = OtaConstants.TaskStatus.FAILED;
    } else if (task.getCancelTime() != null && waiting == 0 && inProgress == 0) {
      nextStatus = OtaConstants.TaskStatus.CANCELED;
    } else if (waiting > 0 || inProgress > 0) {
      nextStatus = OtaConstants.TaskStatus.RUNNING;
    } else if (canceled == total) {
      nextStatus = OtaConstants.TaskStatus.CANCELED;
    } else if (failed == total) {
      nextStatus = OtaConstants.TaskStatus.FAILED;
    } else if (success + skipped == total) {
      nextStatus = OtaConstants.TaskStatus.COMPLETED;
    } else {
      nextStatus = OtaConstants.TaskStatus.PARTIAL_SUCCESS;
    }
    task.setStatus(nextStatus);
    if ((OtaConstants.TaskStatus.COMPLETED.equals(nextStatus)
            || OtaConstants.TaskStatus.PARTIAL_SUCCESS.equals(nextStatus)
            || OtaConstants.TaskStatus.CANCELED.equals(nextStatus)
            || OtaConstants.TaskStatus.FAILED.equals(nextStatus))
        && task.getFinishTime() == null) {
      task.setFinishTime(new Date());
    }
    otaTaskMapper.updateTask(task);
  }

  private void sendCancelCommand(IoTOtaTask task, IoTOtaTaskDevice taskDevice) {
    IoTOtaPackage otaPackage = otaPackageMapper.selectPackageById(taskDevice.getPackageId());
    IoTProduct product = productMapper.getProductByProductKey(taskDevice.getProductKey());
    if (otaPackage == null || product == null) {
      return;
    }
    Map<String, Object> function = buildOtaFunction(task, taskDevice, otaPackage, OtaConstants.ACTION_CANCEL);
    UnifiedDownlinkCommand command =
        UnifiedDownlinkCommand.builder()
            .productKey(taskDevice.getProductKey())
            .deviceId(taskDevice.getDeviceId())
            .iotId(taskDevice.getIotId())
            .cmd(DownCmd.DEV_FUNCTION)
            .function(function)
            .applicationId("system-ota")
            .appUnionId("system-ota")
            .msgId(IdUtil.fastSimpleUUID())
            .timestamp(System.currentTimeMillis())
            .build();
    command.withSource("system-ota");
    try {
      IoTDownlFactory.getIDownOrThrow(resolveProtocolCode(product)).doAction(command);
    } catch (Exception e) {
      log.warn("发送 OTA 取消指令失败, taskId={}, deviceId={}, error={}", task.getId(), taskDevice.getDeviceId(), e.getMessage());
    }
  }

  private boolean shouldDispatch(IoTOtaTask task, IoTOtaTaskDevice taskDevice) {
    boolean requireVersionCheck =
        parseStrategy(task).getBool(OtaConstants.StrategyKey.REQUIRE_VERSION_CHECK, true);
    if (!requireVersionCheck) {
      return true;
    }
    return OtaVersionUtils.isTargetNewer(taskDevice.getCurrentVersion(), taskDevice.getTargetVersion());
  }

  private boolean isDispatchablePackage(IoTOtaPackage otaPackage) {
    return otaPackage != null
        && OtaConstants.PackageStatus.PUBLISHED.equals(otaPackage.getStatus())
        && StrUtil.isNotBlank(otaPackage.getFileUrl())
        && otaPackage.getFileSize() != null
        && otaPackage.getFileSize() > 0
        && StrUtil.isNotBlank(otaPackage.getChecksum())
        && StrUtil.equalsAnyIgnoreCase(otaPackage.getChecksumAlgorithm(), "SHA256", "SHA-256", "SHA512", "SHA-512");
  }

  private String resolveProtocolCode(IoTProduct product) {
    String protocolCode = product.getThirdPlatform();
    if (StrUtil.isBlank(protocolCode)) {
      protocolCode = product.getTransportProtocol();
    }
    return StrUtil.isBlank(protocolCode) ? null : protocolCode.toLowerCase();
  }

  private Map<String, Object> buildOtaFunction(
      IoTOtaTask task, IoTOtaTaskDevice taskDevice, IoTOtaPackage otaPackage, String action) {
    // strategy 是任务级规则快照，设备拿到后可以按需使用其中的重试、超时、分批等辅助信息。
    Map<String, Object> strategy = new LinkedHashMap<>();
    strategy.putAll(parseStrategy(task));

    // data 是标准 OTA 业务载荷。
    // 当前平台的标准模式不是“把二进制包体通过 MQTT/TCP 直接推给设备”，
    // 而是把升级元数据和 downloadUrl 通过协议消息下发给设备，由设备自行拉取升级包。
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("action", action);
    data.put("taskId", String.valueOf(task.getId()));
    data.put("taskNo", task.getTaskNo());
    data.put("taskDeviceId", taskDevice.getId());
    data.put("packageId", otaPackage.getId());
    data.put("packageName", otaPackage.getPackageName());
    data.put("packageType", otaPackage.getPackageType());
    data.put("customType", otaPackage.getCustomType());
    data.put("module", StrUtil.blankToDefault(otaPackage.getModule(), OtaConstants.DEFAULT_MODULE));
    data.put("currentVersion", taskDevice.getCurrentVersion());
    data.put("targetVersion", taskDevice.getTargetVersion());
    data.put("downloadUrl", otaPackage.getFileUrl());
    data.put("fileName", otaPackage.getFileName());
    data.put("fileSize", otaPackage.getFileSize());
    data.put("checksum", otaPackage.getChecksum());
    data.put("checksumAlgorithm", otaPackage.getChecksumAlgorithm());
    data.put("signatureUrl", otaPackage.getSignatureUrl());
    data.put("forceUpgrade", otaPackage.isForceUpgradeFlag());
    data.put("releaseNotes", otaPackage.getReleaseNotes());
    data.put("minHardwareVersion", otaPackage.getMinHardwareVersion());
    data.put("maxHardwareVersion", otaPackage.getMaxHardwareVersion());
    data.put("minSoftwareVersion", otaPackage.getMinSoftwareVersion());
    data.put("strategy", strategy);
    if (StrUtil.isNotBlank(taskDevice.getTransferState())) {
      JSONObject transfer = JSONUtil.parseObj(taskDevice.getTransferState());
      data.put("sessionId", transfer.getStr("sessionId"));
    }

    // function 必须严格复用平台既有的物模型函数下发结构:
    // {"messageType":"FUNCTIONS","function":"otaUpgrade","data":{...}}
    // 这样 OTA 与普通功能下发共用同一条 encode -> DownFunction -> 协议发送链路，
    // TCP/MQTT/UDP/HTTP 等协议插件都不需要为 OTA 维护额外的专用报文模型。
    Map<String, Object> function = new LinkedHashMap<>();
    function.put("messageType", MessageType.FUNCTIONS.name());
    function.put("function", OtaConstants.DEFAULT_FUNCTION_ID);
    function.put("data", data);
    return function;
  }

  private IoTOtaTaskDevice resolveTaskDevice(
      Map<String, Object> reportData, String productKey, String deviceId) {
    // 优先使用 taskDeviceId 精确定位，适合设备按平台下发参数原样回传。
    String taskDeviceId = stringValue(reportData, "taskDeviceId");
    if (StrUtil.isNotBlank(taskDeviceId) && StrUtil.isNumeric(taskDeviceId)) {
      IoTOtaTaskDevice directMatch = otaTaskDeviceMapper.selectTaskDeviceById(Convert.toLong(taskDeviceId));
      if (directMatch != null
          && StrUtil.equals(directMatch.getProductKey(), productKey)
          && StrUtil.equals(directMatch.getDeviceId(), deviceId)) {
        return directMatch;
      }
    }

    // 其次使用 taskId + productKey + deviceId 组合定位，适合设备只保留任务编号而不保留 taskDeviceId。
    String taskId = stringValue(reportData, "taskId");
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

    List<IoTOtaTaskDevice> activeCandidates =
        otaTaskDeviceMapper.selectActiveCandidatesByDevice(productKey, deviceId);
    if (activeCandidates == null || activeCandidates.isEmpty()) {
      return null;
    }
    String sessionId = stringValue(reportData, "sessionId");
    if (StrUtil.isNotBlank(sessionId)) {
      // 同设备可并行升级不同模块，session 精确路由必须先于历史的最近任务兜底。
      for (IoTOtaTaskDevice candidate : activeCandidates) {
        if (StrUtil.isNotBlank(candidate.getTransferState())
            && StrUtil.equals(sessionId, JSONUtil.parseObj(candidate.getTransferState()).getStr("sessionId"))) {
          return candidate;
        }
      }
      return null;
    }
    if (activeCandidates.size() == 1) {
      return activeCandidates.get(0);
    }

    IoTOtaTaskDevice bestCandidate = pickBestCandidate(reportData, activeCandidates);
    if (bestCandidate != null) {
      return bestCandidate;
    }

    // 仍无法唯一定位时，退化为最近一个活跃任务兜底兼容历史协议或极简上报。
    return activeCandidates.get(0);
  }

  private IoTOtaTaskDevice lockReportDevice(IoTOtaTaskDevice device) {
    if (device == null) return null;
    otaTaskMapper.selectTaskForUpdate(device.getTaskId());
    IoTOtaTaskDevice locked = otaTaskDeviceMapper.selectTaskDeviceForUpdate(device.getId());
    if (locked == null) return null;
    // Use the locking read itself: a plain SELECT could return an older snapshot under MySQL
    // REPEATABLE READ. Only task identity metadata is copied from the original joined view.
    locked.setModule(device.getModule());
    locked.setPackageType(device.getPackageType());
    locked.setCustomType(device.getCustomType());
    return locked;
  }

  private void updateDeviceVersionSnapshot(
      Map<String, Object> source,
      IoTOtaTaskDevice taskDevice,
      String currentVersion,
      String lastStatus) {
    // 版本快照表是“设备当前 OTA 视图”的最终落点。
    // 即使没有精确匹配到 taskDevice，也尽量保留设备当前版本、最后状态、最近上报时间，便于设备详情页展示。
    String productKey =
        taskDevice != null ? taskDevice.getProductKey() : stringValue(source, "productKey");
    String deviceId = taskDevice != null ? taskDevice.getDeviceId() : stringValue(source, "deviceId");
    if (StrUtil.isBlank(productKey) || StrUtil.isBlank(deviceId)) {
      return;
    }
    String packageType = firstNonBlank(source, "packageType");
    if (StrUtil.isBlank(packageType)) {
      packageType = OtaConstants.DEFAULT_PACKAGE_TYPE;
    }
    String module = StrUtil.blankToDefault(firstNonBlank(source, "module"), OtaConstants.DEFAULT_MODULE);
    String customType = stringValue(source, "customType");
    String protocol =
        StrUtil.blankToDefault(
            stringValue(source, "protocol"), taskDevice != null ? taskDevice.getProtocol() : null);
    String hardwareVersion = firstNonBlank(source, "hardwareVersion");
    String targetVersion =
        taskDevice != null ? taskDevice.getTargetVersion() : stringValue(source, "targetVersion");
    String retryCount =
        taskDevice != null ? String.valueOf(defaultRetry(taskDevice)) : stringValue(source, "retryCount");

    IoTOtaDeviceVersion version =
        IoTOtaDeviceVersion.builder()
            .productKey(productKey)
            .deviceId(deviceId)
            .iotId(taskDevice != null ? taskDevice.getIotId() : stringValue(source, "iotId"))
            .packageType(packageType)
            .customType(customType)
            .module(module)
            .currentVersion(StrUtil.blankToDefault(currentVersion, stringValue(source, "currentVersion")))
            .targetVersion(targetVersion)
            .hardwareVersion(hardwareVersion)
            .lastTaskId(taskDevice != null ? taskDevice.getTaskId() : null)
            .lastStatus(StrUtil.blankToDefault(lastStatus, taskDevice != null ? taskDevice.getStatus() : null))
            .protocol(protocol)
            .retryCount(Convert.toInt(retryCount, 0))
            .statusDetail(buildStatusDetail(source, StrUtil.blankToDefault(lastStatus, taskDevice != null ? taskDevice.getStatus() : null)))
            .extInfo(JSONUtil.toJsonStr(source))
            .lastReportTime(new Date())
            .lastSuccessTime(
                OtaConstants.DeviceStatus.SUCCESS.equals(
                        StrUtil.blankToDefault(lastStatus, taskDevice != null ? taskDevice.getStatus() : null))
                    ? new Date()
                    : null)
            .build();
    otaDeviceVersionMapper.upsertVersion(version);
  }

  private String buildStatusDetail(Map<String, Object> source, String normalizedStatus) {
    // 优先采用设备主动上报的详细说明；若没有，再回退为错误信息或“状态 + 进度”的通用描述。
    String detail = stringValue(source, "statusDetail");
    if (StrUtil.isNotBlank(detail)) {
      return detail;
    }
    String errorMessage = stringValue(source, "errorMessage");
    if (StrUtil.isNotBlank(errorMessage)) {
      return errorMessage;
    }
    Integer progress = Convert.toInt(source.get("progress"), null);
    if (progress != null && progress >= 0) {
      return StrUtil.blankToDefault(normalizedStatus, "REPORTED") + " (" + progress + "%)";
    }
    return StrUtil.blankToDefault(normalizedStatus, "REPORTED");
  }

  private String normalizeDeviceStatus(String rawStatus, Integer progress, boolean resultReport) {
    // 兼容不同设备的自由状态命名，把它们收敛到平台统一状态机。
    // 例如 success/done/completed 都归一为 SUCCESS，verify/check 归一为 VERIFYING。
    if (StrUtil.isBlank(rawStatus)) {
      if (progress == null) {
        return OtaConstants.DeviceStatus.PUSHED;
      }
      // 进度 100 只能表示下载/刷写阶段结束，不能替代最终结果；结果事件才可据此兜底成功。
      return progress >= 100
          ? (resultReport ? OtaConstants.DeviceStatus.SUCCESS : OtaConstants.DeviceStatus.VERIFYING)
          : OtaConstants.DeviceStatus.DOWNLOADING;
    }
    String value = rawStatus.toLowerCase();
    if (value.contains("success") || value.contains("done") || value.contains("completed")) {
      return resultReport ? OtaConstants.DeviceStatus.SUCCESS : OtaConstants.DeviceStatus.VERIFYING;
    }
    if (value.contains("waiting")) {
      return OtaConstants.DeviceStatus.WAITING_PUSH;
    }
    if (value.contains("push") || value.contains("sent")) {
      return OtaConstants.DeviceStatus.PUSHED;
    }
    if (value.contains("cancel")) {
      return OtaConstants.DeviceStatus.CANCELED;
    }
    if (value.contains("timeout")) {
      return OtaConstants.DeviceStatus.TIMEOUT;
    }
    if (value.contains("fail") || value.contains("error")) {
      return OtaConstants.DeviceStatus.FAILED;
    }
    if (value.contains("reboot")) {
      return OtaConstants.DeviceStatus.REBOOTING;
    }
    if (value.contains("install")) {
      return OtaConstants.DeviceStatus.INSTALLING;
    }
    if (value.contains("verify") || value.contains("check")) {
      return OtaConstants.DeviceStatus.VERIFYING;
    }
    if (value.contains("download")) {
      return OtaConstants.DeviceStatus.DOWNLOADING;
    }
    if (resultReport && progress != null && progress >= 100) {
      return OtaConstants.DeviceStatus.SUCCESS;
    }
    return OtaConstants.DeviceStatus.DOWNLOADING;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void processTaskTimeouts(Long taskId) {
    IoTOtaTask task = otaTaskMapper.selectTaskForUpdate(taskId);
    if (task == null || task.getId() == null) {
      return;
    }
    JSONObject strategy = parseStrategy(task);
    int timeoutSeconds = strategy.getInt(OtaConstants.StrategyKey.TIMEOUT_SECONDS, 3600);
    if (timeoutSeconds <= 0) {
      return;
    }

    List<IoTOtaTaskDevice> timedOutDevices =
        otaTaskDeviceMapper.selectTimedOutActiveByTaskId(task.getId(), timeoutSeconds);
    if (timedOutDevices == null || timedOutDevices.isEmpty()) {
      return;
    }

    for (IoTOtaTaskDevice candidate : timedOutDevices) {
      IoTOtaTaskDevice taskDevice = lockReportDevice(candidate);
      if (taskDevice == null || OtaConstants.isFinalDeviceStatus(taskDevice.getStatus())) {
        continue;
      }
      Date lastActivity = taskDevice.getLastReportTime();
      if (taskDevice.getPushedTime() != null
          && (lastActivity == null || taskDevice.getPushedTime().after(lastActivity))) lastActivity = taskDevice.getPushedTime();
      if (lastActivity == null) lastActivity = taskDevice.getUpdateTime() != null ? taskDevice.getUpdateTime() : taskDevice.getCreateTime();
      if (lastActivity == null || System.currentTimeMillis() - lastActivity.getTime() < timeoutSeconds * 1000L) continue;
      // 分包阶段由逐片的有限超时重试负责，避免整体超时扫描覆盖刚确认的新会话状态。
      if (otaTransferService.hasIncompleteTransfer(taskDevice)) continue;
      String timeoutDetail = buildTimeoutDetail(taskDevice, timeoutSeconds);
      taskDevice.setStatus(OtaConstants.DeviceStatus.TIMEOUT);
      taskDevice.setStatusDetail(timeoutDetail);
      taskDevice.setErrorCode(OtaConstants.DeviceStatus.TIMEOUT);
      taskDevice.setErrorMessage(timeoutDetail);
      taskDevice.setRawReport(JSONUtil.toJsonStr(buildSyntheticRuntimeReport(task, taskDevice, timeoutDetail)));
      if (otaTaskDeviceMapper.updateTaskDeviceIfActive(taskDevice) <= 0) {
        continue;
      }
      updateDeviceVersionSnapshot(
          buildSyntheticRuntimeReport(task, taskDevice, timeoutDetail),
          taskDevice,
          taskDevice.getCurrentVersion(),
          OtaConstants.DeviceStatus.TIMEOUT);
    }
    refreshTaskStats(task.getId());
  }

  private int defaultRetry(IoTOtaTaskDevice taskDevice) {
    return Objects.requireNonNullElse(taskDevice.getRetryCount(), 0);
  }

  private IoTOtaTaskDevice pickBestCandidate(
      Map<String, Object> reportData, List<IoTOtaTaskDevice> activeCandidates) {
    if (activeCandidates == null || activeCandidates.isEmpty()) {
      return null;
    }
    List<IoTOtaTaskDevice> filtered = new ArrayList<>(activeCandidates);

    filtered = narrowCandidatesByField(filtered, stringValue(reportData, "targetVersion"), "targetVersion");
    if (filtered.size() == 1) {
      return filtered.get(0);
    }

    if (isLikelySuccessReport(reportData)) {
      filtered = narrowCandidatesByField(filtered, firstNonBlank(reportData, "currentVersion", "version"), "candidateTargetVersion");
      if (filtered.size() == 1) {
        return filtered.get(0);
      }
    }

    filtered = narrowCandidatesByField(filtered, firstNonBlank(reportData, "module"), "module");
    if (filtered.size() == 1) {
      return filtered.get(0);
    }

    filtered = narrowCandidatesByField(filtered, firstNonBlank(reportData, "packageType"), "packageType");
    if (filtered.size() == 1) {
      return filtered.get(0);
    }

    filtered = narrowCandidatesByField(filtered, stringValue(reportData, "customType"), "customType");
    if (filtered.size() == 1) {
      return filtered.get(0);
    }
    return null;
  }

  private List<IoTOtaTaskDevice> narrowCandidatesByField(
      List<IoTOtaTaskDevice> candidates, String expectedValue, String fieldName) {
    if (candidates == null || candidates.isEmpty() || StrUtil.isBlank(expectedValue)) {
      return candidates;
    }
    List<IoTOtaTaskDevice> matched = new ArrayList<>();
    for (IoTOtaTaskDevice candidate : candidates) {
      if (candidate == null) {
        continue;
      }
      String actualValue = switch (fieldName) {
        case "targetVersion", "candidateTargetVersion" -> candidate.getTargetVersion();
        case "module" -> StrUtil.blankToDefault(candidate.getModule(), OtaConstants.DEFAULT_MODULE);
        case "packageType" -> StrUtil.blankToDefault(candidate.getPackageType(), OtaConstants.DEFAULT_PACKAGE_TYPE);
        case "customType" -> candidate.getCustomType();
        default -> null;
      };
      if (StrUtil.equals(actualValue, expectedValue)) {
        matched.add(candidate);
      }
    }
    return matched.isEmpty() ? candidates : matched;
  }

  private boolean isLikelySuccessReport(Map<String, Object> reportData) {
    String reportType = stringValue(reportData, "reportType");
    if (StrUtil.isNotBlank(reportType)) {
      String normalizedType = reportType.toUpperCase();
      if (normalizedType.contains("RESULT")) {
        String status = stringValue(reportData, "upgradeStatus");
        return StrUtil.isNotBlank(status)
            && (status.toLowerCase().contains("success")
                || status.toLowerCase().contains("done")
                || status.toLowerCase().contains("completed"));
      }
      if (normalizedType.contains("FIRMWARE")) {
        return StrUtil.isNotBlank(firstNonBlank(reportData, "currentVersion", "version"));
      }
    }
    Integer progress = Convert.toInt(reportData.get("progress"), null);
    return progress != null && progress >= 100;
  }

  private String buildTimeoutDetail(IoTOtaTaskDevice taskDevice, int timeoutSeconds) {
    long timeoutMinutes = Math.max(1L, timeoutSeconds / 60L);
    Date lastActivity =
        taskDevice.getLastReportTime() != null
            ? taskDevice.getLastReportTime()
            : taskDevice.getPushedTime();
    if (lastActivity == null) {
      return "超过" + timeoutMinutes + "分钟未收到设备升级反馈，已判定超时";
    }
    return "自" + lastActivity + "起超过" + timeoutMinutes + "分钟未收到设备升级反馈，已判定超时";
  }

  private Map<String, Object> buildSyntheticRuntimeReport(
      IoTOtaTask task, IoTOtaTaskDevice taskDevice, String detail) {
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("productKey", taskDevice.getProductKey());
    report.put("deviceId", taskDevice.getDeviceId());
    report.put("taskId", String.valueOf(task.getId()));
    report.put("taskDeviceId", String.valueOf(taskDevice.getId()));
    report.put("packageType", StrUtil.blankToDefault(task.getPackageType(), OtaConstants.DEFAULT_PACKAGE_TYPE));
    report.put("customType", task.getCustomType());
    report.put("module", StrUtil.blankToDefault(task.getModule(), OtaConstants.DEFAULT_MODULE));
    report.put("currentVersion", taskDevice.getCurrentVersion());
    report.put("targetVersion", taskDevice.getTargetVersion());
    report.put("protocol", taskDevice.getProtocol());
    report.put("upgradeStatus", OtaConstants.DeviceStatus.TIMEOUT);
    report.put("statusDetail", detail);
    report.put("errorCode", OtaConstants.DeviceStatus.TIMEOUT);
    report.put("errorMessage", detail);
    report.put("retryCount", defaultRetry(taskDevice));
    return report;
  }

  private String buildDispatchFailureLog(
      IoTOtaTask task,
      IoTOtaTaskDevice taskDevice,
      IoTOtaPackage otaPackage,
      UnifiedDownlinkCommand command,
      R<?> result,
      String detail) {
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("event", OTA_DISPATCH_FAILED_EVENT);
    report.put("timestamp", System.currentTimeMillis());
    report.put("taskId", task != null ? task.getId() : taskDevice.getTaskId());
    report.put("taskNo", task != null ? task.getTaskNo() : null);
    report.put("taskDeviceId", taskDevice.getId());
    report.put("packageId", otaPackage != null ? otaPackage.getId() : taskDevice.getPackageId());
    report.put("packageVersion", otaPackage != null ? otaPackage.getVersion() : taskDevice.getTargetVersion());
    report.put("productKey", taskDevice.getProductKey());
    report.put("deviceId", taskDevice.getDeviceId());
    report.put("iotId", taskDevice.getIotId());
    report.put("protocol", taskDevice.getProtocol());
    report.put("msgId", command != null ? command.getMsgId() : null);
    report.put("status", taskDevice.getStatus());
    report.put("statusDetail", detail);
    report.put("errorMessage", taskDevice.getErrorMessage());
    report.put("retryCount", defaultRetry(taskDevice));
    report.put("downlinkCode", result != null ? result.getCode() : null);
    report.put("downlinkMessage", result != null ? result.getMsg() : null);
    // raw_report 保存平台侧下发失败现场，避免设备无上报时只剩一个简短 error_message。
    if (StrUtil.isNotBlank(taskDevice.getCommandPayload())) {
      report.put("commandPayload", parseCommandPayloadSnapshot(taskDevice.getCommandPayload()));
    }
    return JSONUtil.toJsonStr(report);
  }

  private Object parseCommandPayloadSnapshot(String commandPayload) {
    try {
      return JSONUtil.parseObj(commandPayload);
    } catch (Exception e) {
      return commandPayload;
    }
  }

  private void safeDispatchScheduledTask(IoTOtaTask task) {
    try {
      currentProxy().dispatchTask(task.getId());
    } catch (Exception e) {
      log.warn("OTA定时/分批任务执行失败, taskId={}, error={}", task.getId(), e.getMessage());
    }
  }

  private boolean maybeTriggerRollback(IoTOtaTask task, List<IoTOtaTaskDevice> taskDevices) {
    JSONObject strategy = parseStrategy(task);
    if (!isFlagEnabled(strategy.get(OtaConstants.StrategyKey.ROLLBACK_ON_FAILURE))
        || strategy.getBool(OtaConstants.StrategyKey.ROLLBACK_TRIGGERED, false)) {
      return false;
    }
    int threshold = strategy.getInt(OtaConstants.StrategyKey.FAILURE_THRESHOLD_PERCENT, 0);
    Long rollbackPackageId = strategy.getLong(OtaConstants.StrategyKey.ROLLBACK_PACKAGE_ID);
    if (threshold <= 0 || rollbackPackageId == null || taskDevices == null || taskDevices.isEmpty()) {
      return false;
    }

    // 回滚判定采用“已尝试设备”为分母，而不是任务总设备数。
    // 这样可以避免还未下发的 WAITING_PUSH 设备稀释失败率，导致止损触发过晚。
    List<IoTOtaTaskDevice> rollbackCandidates = new ArrayList<>();
    boolean hasActiveDevices = false;
    int attempted = 0;
    int failed = 0;
    for (IoTOtaTaskDevice taskDevice : taskDevices) {
      String status = taskDevice.getStatus();
      if (!OtaConstants.DeviceStatus.WAITING_PUSH.equals(status)
          && !OtaConstants.DeviceStatus.SKIPPED.equals(status)
          && !OtaConstants.DeviceStatus.CANCELED.equals(status)) {
        attempted++;
      }
      if (OtaConstants.DeviceStatus.SUCCESS.equals(status)) {
        rollbackCandidates.add(taskDevice);
      }
      if (isRollbackFailedStatus(status)) {
        failed++;
      }
      if (OtaConstants.isActiveDeviceStatus(status)) {
        hasActiveDevices = true;
      }
    }
    if (attempted <= 0) {
      return false;
    }
    double failureRate = failed * 100D / attempted;
    if (failureRate < threshold) {
      return false;
    }

    // 一旦触发回滚，先尝试取消仍在执行中的设备，避免继续扩大故障面。
    if (hasActiveDevices) {
      for (IoTOtaTaskDevice taskDevice : taskDevices) {
        IoTOtaTaskDevice locked = otaTaskDeviceMapper.selectTaskDeviceForUpdate(taskDevice.getId());
        if (locked != null && OtaConstants.isActiveDeviceStatus(locked.getStatus())) {
          sendCancelCommand(task, locked);
        }
      }
      otaTaskDeviceMapper.updateStatusByTaskId(task.getId(), OtaConstants.DeviceStatus.CANCELED);
    }

    // 回滚任务只针对已经升级成功的设备生成，因为这些设备才有“需要回退”的意义。
    strategy.set(OtaConstants.StrategyKey.ROLLBACK_TRIGGERED, true);
    strategy.set(OtaConstants.StrategyKey.ROLLBACK_TRIGGERED_TIME, System.currentTimeMillis());
    Long rollbackTaskId = createRollbackTask(task, rollbackCandidates, rollbackPackageId);
    if (rollbackTaskId != null) {
      strategy.set(OtaConstants.StrategyKey.ROLLBACK_TASK_ID, rollbackTaskId);
    }
    task.setStrategyJson(strategy.toString());
    if (task.getFinishTime() == null) {
      task.setFinishTime(new Date());
    }
    otaTaskMapper.updateTask(task);
    return true;
  }

  private Long createRollbackTask(
      IoTOtaTask sourceTask, List<IoTOtaTaskDevice> rollbackCandidates, Long rollbackPackageId) {
    if (rollbackCandidates == null || rollbackCandidates.isEmpty()) {
      return null;
    }
    IoTOtaPackage rollbackPackage = otaPackageMapper.selectPackageById(rollbackPackageId);
    if (rollbackPackage == null
        || !OtaConstants.PackageStatus.PUBLISHED.equals(rollbackPackage.getStatus())
        || !StrUtil.equals(sourceTask.getProductKey(), rollbackPackage.getProductKey())) {
      log.warn("自动回滚创建失败, taskId={}, rollbackPackageId={}", sourceTask.getId(), rollbackPackageId);
      return null;
    }

    JSONObject sourceStrategy = parseStrategy(sourceTask);
    JSONObject rollbackStrategy = new JSONObject();
    rollbackStrategy.set("transferProfile", sourceStrategy.get("transferProfile"));
    rollbackStrategy.set(OtaConstants.StrategyKey.AUTO_DISPATCH, IoTConstant.ONE_TRUE);
    rollbackStrategy.set(
        OtaConstants.StrategyKey.MAX_RETRY_COUNT,
        sourceStrategy.getInt(OtaConstants.StrategyKey.MAX_RETRY_COUNT, 3));
    rollbackStrategy.set(
        OtaConstants.StrategyKey.TIMEOUT_SECONDS,
        sourceStrategy.getInt(OtaConstants.StrategyKey.TIMEOUT_SECONDS, 3600));
    rollbackStrategy.set(OtaConstants.StrategyKey.REQUIRE_VERSION_CHECK, false);
    rollbackStrategy.set(
        OtaConstants.StrategyKey.BATCH_SIZE,
        sourceStrategy.getInt(OtaConstants.StrategyKey.BATCH_SIZE, 0));
    rollbackStrategy.set(
        OtaConstants.StrategyKey.BATCH_INTERVAL_SECONDS,
        sourceStrategy.getInt(OtaConstants.StrategyKey.BATCH_INTERVAL_SECONDS, 0));
    rollbackStrategy.set(OtaConstants.StrategyKey.ROLLBACK_ON_FAILURE, IoTConstant.ZERO_FALSE);
    rollbackStrategy.set(OtaConstants.StrategyKey.SOURCE_TASK_ID, sourceTask.getId());

    String snowflakeId = String.valueOf(IdUtil.getSnowflake().nextId());
    String taskSuffix =
        snowflakeId.length() > 4
            ? snowflakeId.substring(snowflakeId.length() - 4)
            : snowflakeId;
    IoTOtaTask rollbackTask =
        IoTOtaTask.builder()
            .taskNo("RB" + System.currentTimeMillis() + taskSuffix)
            .productId(sourceTask.getProductId())
            .productKey(sourceTask.getProductKey())
            .packageId(rollbackPackage.getId())
            .taskName(sourceTask.getTaskName() + " 自动回滚")
            .targetVersion(rollbackPackage.getVersion())
            .packageType(rollbackPackage.getPackageType())
            .customType(rollbackPackage.getCustomType())
            .module(rollbackPackage.getModule())
            .rolloutMode("SPECIFIED")
            .description("由 OTA 任务[" + sourceTask.getTaskNo() + "]失败阈值触发自动回滚")
            .status(OtaConstants.TaskStatus.PENDING)
            .deviceCount(rollbackCandidates.size())
            .waitingCount(rollbackCandidates.size())
            .inProgressCount(0)
            .successCount(0)
            .failedCount(0)
            .canceledCount(0)
            .skippedCount(0)
            .creatorId(sourceTask.getCreatorId())
            .strategyJson(rollbackStrategy.toString())
            .build();
    otaTaskMapper.insertTask(rollbackTask);

    List<IoTOtaTaskDevice> rollbackDevices = new ArrayList<>();
    for (IoTOtaTaskDevice taskDevice : rollbackCandidates) {
      rollbackDevices.add(
          IoTOtaTaskDevice.builder()
              .taskId(rollbackTask.getId())
              .packageId(rollbackPackage.getId())
              .productKey(taskDevice.getProductKey())
              .deviceId(taskDevice.getDeviceId())
              .iotId(taskDevice.getIotId())
              .currentVersion(taskDevice.getCurrentVersion())
              .targetVersion(rollbackPackage.getVersion())
              .status(OtaConstants.DeviceStatus.WAITING_PUSH)
              .progress(0)
              .retryCount(0)
              .maxRetryCount(
                  sourceStrategy.getInt(OtaConstants.StrategyKey.MAX_RETRY_COUNT, 3))
              .statusDetail("等待执行自动回滚")
              .build());
    }
    otaTaskDeviceMapper.insertTaskDeviceBatch(rollbackDevices);
    currentProxy().dispatchTask(rollbackTask.getId());
    return rollbackTask.getId();
  }

  private boolean isRollbackFailedStatus(String status) {
    return OtaConstants.DeviceStatus.FAILED.equals(status)
        || OtaConstants.DeviceStatus.TIMEOUT.equals(status);
  }

  private IoTOtaRuntimeService currentProxy() {
    return (IoTOtaRuntimeService) AopContext.currentProxy();
  }

  private JSONObject parseStrategy(IoTOtaTask task) {
    if (task == null || StrUtil.isBlank(task.getStrategyJson()) || !JSONUtil.isTypeJSONObject(task.getStrategyJson())) {
      return new JSONObject();
    }
    return JSONUtil.parseObj(task.getStrategyJson());
  }

  private void persistTaskStrategy(IoTOtaTask task, JSONObject strategy) {
    task.setStrategyJson(strategy.toString());
    otaTaskMapper.updateTask(task);
  }

  private int resolveDispatchCount(List<IoTOtaTaskDevice> waitingDevices, JSONObject strategy) {
    int batchSize = strategy.getInt(OtaConstants.StrategyKey.BATCH_SIZE, 0);
    if (batchSize <= 0) {
      return waitingDevices.size();
    }
    return Math.min(batchSize, waitingDevices.size());
  }

  private boolean isAutoDispatchEnabled(IoTOtaTask task) {
    return isFlagEnabled(parseStrategy(task).get(OtaConstants.StrategyKey.AUTO_DISPATCH));
  }

  private boolean isDueForDispatch(IoTOtaTask task) {
    Long plannedStartTime =
        parseStrategy(task).getLong(OtaConstants.StrategyKey.PLANNED_START_TIME);
    return plannedStartTime == null || plannedStartTime <= System.currentTimeMillis();
  }

  private boolean hasWaitingDevices(IoTOtaTask task) {
    return Objects.requireNonNullElse(task.getWaitingCount(), 0) > 0;
  }

  private boolean shouldDispatchNextBatch(IoTOtaTask task) {
    JSONObject strategy = parseStrategy(task);
    if (!isFlagEnabled(strategy.get(OtaConstants.StrategyKey.AUTO_DISPATCH))
        || strategy.getInt(OtaConstants.StrategyKey.BATCH_SIZE, 0) <= 0
        || Objects.requireNonNullElse(task.getWaitingCount(), 0) <= 0) {
      return false;
    }
    int intervalSeconds = strategy.getInt(OtaConstants.StrategyKey.BATCH_INTERVAL_SECONDS, 0);
    Long lastBatchDispatchTime =
        strategy.getLong(OtaConstants.StrategyKey.LAST_BATCH_DISPATCH_TIME);
    if (lastBatchDispatchTime == null) {
      return true;
    }
    return System.currentTimeMillis() >= lastBatchDispatchTime + (intervalSeconds * 1000L);
  }

  private String stringValue(Map<String, Object> source, String key) {
    if (source == null || key == null || !source.containsKey(key) || source.get(key) == null) {
      return null;
    }
    return Convert.toStr(source.get(key));
  }

  private boolean isFlagEnabled(Object value) {
    return normalizeFlagValue(value, IoTConstant.ZERO_FALSE) == IoTConstant.ONE_TRUE;
  }

  private int normalizeFlagValue(Object value, int defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean booleanValue) {
      return booleanValue ? IoTConstant.ONE_TRUE : IoTConstant.ZERO_FALSE;
    }
    Integer normalized = Convert.toInt(value, defaultValue);
    if (normalized == null) {
      return defaultValue;
    }
    return normalized == IoTConstant.ONE_TRUE ? IoTConstant.ONE_TRUE : IoTConstant.ZERO_FALSE;
  }

  private String firstNonBlank(Map<String, Object> source, String... keys) {
    if (keys == null) {
      return null;
    }
    for (String key : keys) {
      if (StrUtil.isBlank(key)) {
        continue;
      }
      String value = stringValue(source, key);
      if (StrUtil.isNotBlank(value)) {
        return value;
      }
    }
    return null;
  }
}
