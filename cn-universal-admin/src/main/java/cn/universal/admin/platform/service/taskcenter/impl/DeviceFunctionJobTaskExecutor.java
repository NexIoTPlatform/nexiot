package cn.universal.admin.platform.service.taskcenter.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.IoTTaskCenterExecutor;
import cn.universal.admin.platform.service.taskcenter.support.DeviceJobArgumentResolver;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.bo.IoTDeviceJobParamSpec;
import cn.universal.persistence.entity.vo.IoTTaskCenterStatisticsVO;
import cn.universal.persistence.mapper.IoTDeviceShadowMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceFunctionJobTaskExecutor implements IoTTaskCenterExecutor {

  private static final String COMMAND_STATUS_SUCCESS = "SUCCESS";
  private static final String COMMAND_STATUS_FAILED = "FAILED";
  private static final String COMMAND_STATUS_SKIPPED = "SKIPPED";

  private final IIoTTaskCenterService taskCenterService;
  private final IIoTDeviceService ioTDeviceService;
  private final IoTDeviceShadowMapper ioTDeviceShadowMapper;

  @Qualifier("taskExecutor")
  private final Executor taskExecutor;

  @Override
  public String taskType() {
    return IoTTaskCenterConstants.TaskType.DEVICE_FUNCTION_JOB;
  }

  @Override
  public void execute(IoTTaskCenter taskCenter) {
    JSONObject request = JSONUtil.parseObj(taskCenter.getRequestSnapshot());
    String productKey = request.getStr("productKey");
    String executionMode = normalizeCommandExecutionMode(request.getStr("commandExecutionMode"));
    List<DeviceJobCommandRequest> commands = resolveCommands(request);

    List<IoTTaskItem> items = taskCenterService.selectPendingItems(taskCenter.getId());
    Map<String, IoTDeviceShadow> shadowMap = loadShadowMap(items);
    for (IoTTaskItem item : items) {
      processItem(taskCenter, item, productKey, executionMode, commands, shadowMap);
    }

    IoTTaskCenterStatisticsVO statistics = taskCenterService.refreshTaskStatistics(taskCenter.getId());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("productKey", productKey);
    result.put("commandExecutionMode", executionMode);
    result.put("commandCount", commands.size());
    result.put("commands", buildCommandSummary(commands));
    if (commands.size() == 1) {
      DeviceJobCommandRequest first = commands.get(0);
      result.put("functionId", first.functionId);
      result.put("functionName", first.functionName);
    }
    result.put("totalCount", statistics.getTotalCount());
    result.put("successCount", statistics.getSuccessCount());
    result.put("failCount", statistics.getFailCount());
    result.put("pendingCount", statistics.getPendingCount());
    taskCenterService.appendLog(
        taskCenter.getId(),
        IoTTaskCenterConstants.LogLevel.INFO,
        IoTTaskCenterConstants.LogType.FINISH,
        String.format(
            "设备作业执行完成，共 %s 条指令，成功 %s 台，失败 %s 台",
            commands.size(), defaultZero(statistics.getSuccessCount()), defaultZero(statistics.getFailCount())),
        JSONUtil.toJsonStr(result));
    taskCenterService.markTaskFinished(taskCenter.getId(), null, JSONUtil.toJsonStr(result), null);
  }

  private void processItem(
      IoTTaskCenter taskCenter,
      IoTTaskItem item,
      String productKey,
      String executionMode,
      List<DeviceJobCommandRequest> commands,
      Map<String, IoTDeviceShadow> shadowMap) {
    taskCenterService.markItemRunning(item.getId());
    IoTDevice device = JSONUtil.toBean(item.getRawData(), IoTDevice.class);
    try {
      if (device == null || StrUtil.isBlank(device.getDeviceId())) {
        throw new BaseException("设备信息缺失");
      }
      if (CollectionUtil.isEmpty(commands)) {
        throw new BaseException("任务未配置有效指令");
      }

      IoTDeviceShadow shadow = shadowMap.get(StrUtil.blankToDefault(device.getIotId(), ""));
      List<CommandExecutionResult> commandResults =
          executeCommands(taskCenter, productKey, device, shadow, executionMode, commands);

      String itemStatus = resolveItemStatus(commandResults);
      String message = buildItemMessage(executionMode, commandResults);
      Map<String, Object> resultData = buildItemResultData(device, executionMode, commandResults);

      if (IoTTaskCenterConstants.ItemStatus.FAILED.equals(itemStatus)) {
        taskCenterService.appendLog(
            taskCenter.getId(),
            IoTTaskCenterConstants.LogLevel.WARN,
            IoTTaskCenterConstants.LogType.ITEM,
            String.format("设备 %s 执行失败: %s", item.getItemKey(), message),
            JSONUtil.toJsonStr(resultData));
      }

      taskCenterService.markItemFinished(
          item.getId(), itemStatus, message, JSONUtil.toJsonStr(resultData));
    } catch (Exception e) {
      taskCenterService.appendLog(
          taskCenter.getId(),
          IoTTaskCenterConstants.LogLevel.WARN,
          IoTTaskCenterConstants.LogType.ITEM,
          String.format("设备 %s 执行失败: %s", item.getItemKey(), e.getMessage()),
          item.getRawData());
      Map<String, Object> failureData = new LinkedHashMap<>();
      failureData.put("deviceId", device == null ? item.getItemKey() : device.getDeviceId());
      failureData.put("deviceName", device == null ? item.getItemName() : device.getDeviceName());
      failureData.put("message", e.getMessage());
      taskCenterService.markItemFinished(
          item.getId(),
          IoTTaskCenterConstants.ItemStatus.FAILED,
          e.getMessage(),
          JSONUtil.toJsonStr(failureData));
    } finally {
      taskCenterService.refreshTaskStatistics(taskCenter.getId());
    }
  }

  private List<CommandExecutionResult> executeCommands(
      IoTTaskCenter taskCenter,
      String productKey,
      IoTDevice device,
      IoTDeviceShadow shadow,
      String executionMode,
      List<DeviceJobCommandRequest> commands) {
    if (IoTTaskCenterConstants.CommandExecutionMode.PARALLEL.equals(executionMode)) {
      return executeCommandsInParallel(taskCenter, productKey, device, shadow, commands);
    }
    return executeCommandsInSerial(taskCenter, productKey, device, shadow, commands);
  }

  private List<CommandExecutionResult> executeCommandsInSerial(
      IoTTaskCenter taskCenter,
      String productKey,
      IoTDevice device,
      IoTDeviceShadow shadow,
      List<DeviceJobCommandRequest> commands) {
    List<CommandExecutionResult> results = new ArrayList<>();
    for (int i = 0; i < commands.size(); i++) {
      DeviceJobCommandRequest command = commands.get(i);
      CommandExecutionResult result = executeSingleCommand(taskCenter, productKey, device, shadow, command);
      results.add(result);
      if (COMMAND_STATUS_FAILED.equals(result.status)) {
        for (int j = i + 1; j < commands.size(); j++) {
          DeviceJobCommandRequest skippedCommand = commands.get(j);
          results.add(
              CommandExecutionResult.skipped(
                  skippedCommand.commandId,
                  skippedCommand.functionId,
                  skippedCommand.functionName,
                  "前序串行指令执行失败，已跳过"));
        }
        break;
      }
    }
    return results;
  }

  private List<CommandExecutionResult> executeCommandsInParallel(
      IoTTaskCenter taskCenter,
      String productKey,
      IoTDevice device,
      IoTDeviceShadow shadow,
      List<DeviceJobCommandRequest> commands) {
    List<CompletableFuture<CommandExecutionResult>> futures =
        commands.stream()
            .map(
                command ->
                    CompletableFuture.supplyAsync(
                        () -> executeSingleCommand(taskCenter, productKey, device, shadow, command),
                        taskExecutor))
            .collect(Collectors.toList());
    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }

  private CommandExecutionResult executeSingleCommand(
      IoTTaskCenter taskCenter,
      String productKey,
      IoTDevice device,
      IoTDeviceShadow shadow,
      DeviceJobCommandRequest command) {
    JSONObject payload = null;
    DeviceJobArgumentResolver.ResolvedArguments resolvedArguments = null;
    try {
      resolvedArguments =
          DeviceJobArgumentResolver.resolve(
              command.functionArgsSpec, command.functionDefinition, device, shadow);
      payload =
          buildDownPayload(
              taskCenter,
              productKey,
              device,
              command.functionId,
              command.source,
              command.serviceType,
              resolvedArguments.getData());
      R result = ioTDeviceService.iotFunctionsDown(productKey, payload.toString());
      if (result == null || result.getCode() != 0) {
        throw new BaseException(result == null ? "下发失败" : result.getMsg());
      }
      return CommandExecutionResult.success(
          command.commandId,
          command.functionId,
          command.functionName,
          StrUtil.blankToDefault(result.getMsg(), "下发成功"),
          resolvedArguments == null ? new JSONObject() : resolvedArguments.getData(),
          resolvedArguments == null ? new JSONObject() : resolvedArguments.getDetails(),
          payload == null ? new JSONObject() : payload);
    } catch (Exception e) {
      return CommandExecutionResult.failed(
          command.commandId,
          command.functionId,
          command.functionName,
          e.getMessage(),
          resolvedArguments == null ? new JSONObject() : resolvedArguments.getData(),
          resolvedArguments == null ? new JSONObject() : resolvedArguments.getDetails(),
          payload == null ? null : payload);
    }
  }

  private Map<String, Object> buildItemResultData(
      IoTDevice device, String executionMode, List<CommandExecutionResult> commandResults) {
    Map<String, Object> resultData = new LinkedHashMap<>();
    resultData.put("deviceId", device.getDeviceId());
    resultData.put("deviceName", device.getDeviceName());
    resultData.put("commandExecutionMode", executionMode);
    resultData.put("commandCount", commandResults.size());
    resultData.put(
        "functionIds",
        commandResults.stream().map(item -> item.functionId).collect(Collectors.toList()));
    resultData.put(
        "functionNames",
        commandResults.stream().map(item -> item.functionName).collect(Collectors.toList()));

    JSONArray commandResultArray = new JSONArray();
    JSONObject resolvedDataMap = new JSONObject();
    JSONObject resolvedDetailsMap = new JSONObject();
    for (CommandExecutionResult commandResult : commandResults) {
      commandResultArray.add(commandResult.toJson());
      if (hasResolvedData(commandResult.resolvedData)) {
        resolvedDataMap.set(commandResult.commandId, commandResult.resolvedData);
      }
      if (hasResolvedData(commandResult.resolvedDetails)) {
        resolvedDetailsMap.set(commandResult.commandId, commandResult.resolvedDetails);
      }
    }
    resultData.put("commandResults", commandResultArray);
    resultData.put("resolvedData", resolvedDataMap);
    resultData.put("resolvedDetails", resolvedDetailsMap);

    if (!commandResults.isEmpty()) {
      CommandExecutionResult first = commandResults.get(0);
      resultData.put("functionId", first.functionId);
      resultData.put(
          "functionName",
          commandResults.size() == 1
              ? first.functionName
              : commandResults.stream().map(item -> item.functionName).collect(Collectors.joining("、")));
      if (commandResults.size() == 1) {
        resultData.put("message", first.message);
        resultData.put("requestPayload", first.requestPayload == null ? new JSONObject() : first.requestPayload);
        resultData.put("resolvedData", first.resolvedData == null ? new JSONObject() : first.resolvedData);
        resultData.put(
            "resolvedDetails", first.resolvedDetails == null ? new JSONObject() : first.resolvedDetails);
      }
    }
    return resultData;
  }

  private List<Map<String, Object>> buildCommandSummary(List<DeviceJobCommandRequest> commands) {
    return commands.stream()
        .map(
            item -> {
              Map<String, Object> summary = new LinkedHashMap<>();
              summary.put("commandId", item.commandId);
              summary.put("functionId", item.functionId);
              summary.put("functionName", item.functionName);
              summary.put("source", item.source);
              summary.put("serviceType", item.serviceType);
              return summary;
            })
        .collect(Collectors.toList());
  }

  private String resolveItemStatus(List<CommandExecutionResult> commandResults) {
    boolean allSuccess =
        commandResults.stream().allMatch(item -> COMMAND_STATUS_SUCCESS.equals(item.status));
    return allSuccess
        ? IoTTaskCenterConstants.ItemStatus.SUCCESS
        : IoTTaskCenterConstants.ItemStatus.FAILED;
  }

  private String buildItemMessage(String executionMode, List<CommandExecutionResult> commandResults) {
    long successCount =
        commandResults.stream().filter(item -> COMMAND_STATUS_SUCCESS.equals(item.status)).count();
    long failCount =
        commandResults.stream().filter(item -> COMMAND_STATUS_FAILED.equals(item.status)).count();

    if (failCount == 0) {
      if (commandResults.size() == 1) {
        return commandResults.get(0).message;
      }
      return String.format(
          "%s执行完成，%s 条指令全部下发成功",
          executionModeText(executionMode), commandResults.size());
    }

    if (IoTTaskCenterConstants.CommandExecutionMode.SERIAL.equals(executionMode)) {
      int failedIndex = 0;
      for (int i = 0; i < commandResults.size(); i++) {
        if (COMMAND_STATUS_FAILED.equals(commandResults.get(i).status)) {
          failedIndex = i + 1;
          break;
        }
      }
      return String.format(
          "串行执行在第 %s 条指令失败：%s",
          failedIndex,
          commandResults.stream()
              .filter(item -> COMMAND_STATUS_FAILED.equals(item.status))
              .findFirst()
              .map(item -> item.message)
              .orElse("下发失败"));
    }

    return String.format("并行执行完成，成功 %s 条，失败 %s 条", successCount, failCount);
  }

  private String executionModeText(String executionMode) {
    return IoTTaskCenterConstants.CommandExecutionMode.PARALLEL.equals(executionMode) ? "并行" : "串行";
  }

  private boolean hasResolvedData(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof JSONObject jsonObject) {
      return !jsonObject.isEmpty();
    }
    if (value instanceof JSONArray jsonArray) {
      return !jsonArray.isEmpty();
    }
    return true;
  }

  private List<DeviceJobCommandRequest> resolveCommands(JSONObject request) {
    JSONArray commands = request.getJSONArray("commands");
    if (commands != null && !commands.isEmpty()) {
      List<DeviceJobCommandRequest> result = new ArrayList<>();
      for (int i = 0; i < commands.size(); i++) {
        JSONObject command = commands.getJSONObject(i);
        if (command == null || StrUtil.isBlank(command.getStr("functionId"))) {
          continue;
        }
        JSONObject functionData = command.getJSONObject("functionData");
        result.add(
            new DeviceJobCommandRequest(
                StrUtil.blankToDefault(command.getStr("commandId"), String.format("CMD_%02d", i + 1)),
                command.getStr("functionId"),
                command.getStr("functionName", command.getStr("functionId")),
                command.getStr("source"),
                command.getStr("serviceType"),
                command.getJSONObject("functionDefinition"),
                DeviceJobArgumentResolver.normalizeSpecs(
                    command.getJSONObject("functionArgsSpec"),
                    functionData == null ? Collections.emptyMap() : functionData)));
      }
      if (!result.isEmpty()) {
        return result;
      }
    }

    String functionId = request.getStr("functionId");
    if (StrUtil.isBlank(functionId)) {
      return Collections.emptyList();
    }
    JSONObject functionData = request.getJSONObject("functionData");
    return List.of(
        new DeviceJobCommandRequest(
            "CMD_01",
            functionId,
            request.getStr("functionName", functionId),
            request.getStr("source"),
            request.getStr("serviceType"),
            request.getJSONObject("functionDefinition"),
            DeviceJobArgumentResolver.normalizeSpecs(
                request.getJSONObject("functionArgsSpec"),
                functionData == null ? Collections.emptyMap() : functionData)));
  }

  private String normalizeCommandExecutionMode(String mode) {
    return IoTTaskCenterConstants.CommandExecutionMode.PARALLEL.equalsIgnoreCase(mode)
        ? IoTTaskCenterConstants.CommandExecutionMode.PARALLEL
        : IoTTaskCenterConstants.CommandExecutionMode.SERIAL;
  }

  private JSONObject buildDownPayload(
      IoTTaskCenter taskCenter,
      String productKey,
      IoTDevice device,
      String functionId,
      String source,
      String serviceType,
      JSONObject functionData) {
    JSONObject payload = new JSONObject();
    payload.set("appUnionId", taskCenter.getCreatorId());
    payload.set("productKey", productKey);
    payload.set("deviceId", device.getDeviceId());
    payload.set("cmd", "DEV_FUNCTION");

    JSONObject function = new JSONObject();
    function.set("messageType", "FUNCTIONS");
    if (StrUtil.isNotBlank(serviceType)) {
      function.set("serviceType", serviceType);
    }
    if (StrUtil.isNotBlank(source)) {
      function.set("source", source);
    }
    function.set("function", functionId);
    function.set(
        "data",
        functionData == null ? new JSONObject() : JSONUtil.parseObj(JSONUtil.toJsonStr(functionData)));
    payload.set("function", function);
    return payload;
  }

  private Map<String, IoTDeviceShadow> loadShadowMap(List<IoTTaskItem> items) {
    if (items == null || items.isEmpty()) {
      return Collections.emptyMap();
    }
    List<String> iotIds = new ArrayList<>();
    for (IoTTaskItem item : items) {
      IoTDevice device = JSONUtil.toBean(item.getRawData(), IoTDevice.class);
      if (device != null && StrUtil.isNotBlank(device.getIotId())) {
        iotIds.add(device.getIotId());
      }
    }
    if (iotIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<IoTDeviceShadow> shadows = ioTDeviceShadowMapper.selectByIotIds(iotIds);
    if (shadows == null || shadows.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, IoTDeviceShadow> shadowMap = new HashMap<>();
    for (IoTDeviceShadow shadow : shadows) {
      if (shadow != null && StrUtil.isNotBlank(shadow.getIotId())) {
        shadowMap.put(shadow.getIotId(), shadow);
      }
    }
    return shadowMap;
  }

  private int defaultZero(Integer value) {
    return value == null ? 0 : value;
  }

  private record DeviceJobCommandRequest(
      String commandId,
      String functionId,
      String functionName,
      String source,
      String serviceType,
      JSONObject functionDefinition,
      Map<String, IoTDeviceJobParamSpec> functionArgsSpec) {}

  private record CommandExecutionResult(
      String commandId,
      String functionId,
      String functionName,
      String status,
      String message,
      JSONObject resolvedData,
      JSONObject resolvedDetails,
      JSONObject requestPayload) {

    private static CommandExecutionResult success(
        String commandId,
        String functionId,
        String functionName,
        String message,
        JSONObject resolvedData,
        JSONObject resolvedDetails,
        JSONObject requestPayload) {
      return new CommandExecutionResult(
          commandId,
          functionId,
          functionName,
          COMMAND_STATUS_SUCCESS,
          message,
          resolvedData,
          resolvedDetails,
          requestPayload);
    }

    private static CommandExecutionResult failed(
        String commandId,
        String functionId,
        String functionName,
        String message,
        JSONObject resolvedData,
        JSONObject resolvedDetails,
        JSONObject requestPayload) {
      return new CommandExecutionResult(
          commandId,
          functionId,
          functionName,
          COMMAND_STATUS_FAILED,
          message,
          resolvedData,
          resolvedDetails,
          requestPayload);
    }

    private static CommandExecutionResult skipped(
        String commandId, String functionId, String functionName, String message) {
      return new CommandExecutionResult(
          commandId,
          functionId,
          functionName,
          COMMAND_STATUS_SKIPPED,
          message,
          new JSONObject(),
          new JSONObject(),
          null);
    }

    private JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.set("commandId", commandId);
      json.set("functionId", functionId);
      json.set("functionName", functionName);
      json.set("status", status);
      json.set("message", message);
      json.set("resolvedData", resolvedData == null ? new JSONObject() : resolvedData);
      json.set("resolvedDetails", resolvedDetails == null ? new JSONObject() : resolvedDetails);
      json.set("requestPayload", requestPayload == null ? new JSONObject() : requestPayload);
      return json;
    }
  }
}
