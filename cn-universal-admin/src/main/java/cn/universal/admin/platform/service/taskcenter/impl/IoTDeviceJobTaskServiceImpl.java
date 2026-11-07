package cn.universal.admin.platform.service.taskcenter.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskDefinitionService;
import cn.universal.admin.platform.service.taskcenter.IoTDeviceJobTaskService;
import cn.universal.admin.platform.service.taskcenter.IoTTaskCenterDispatchService;
import cn.universal.admin.platform.service.taskcenter.support.DeviceJobArgumentResolver;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskDefinition;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.bo.IoTDeviceJobCommandBO;
import cn.universal.persistence.entity.bo.IoTDeviceJobParamSpec;
import cn.universal.persistence.entity.bo.IoTDeviceJobTaskSubmitBO;
import cn.universal.persistence.entity.vo.IoTTaskSubmitResultVO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.quartz.constant.ScheduleConstants;
import cn.universal.quartz.domain.SysJob;
import cn.universal.quartz.service.ISysJobService;
import cn.universal.security.service.IoTUserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IoTDeviceJobTaskServiceImpl implements IoTDeviceJobTaskService {

  private static final int MAX_TASK_DEVICE_COUNT = 2000;
  private static final String TASK_JOB_GROUP = "TASK_CENTER";

  private final IIoTTaskCenterService taskCenterService;
  private final IIoTTaskDefinitionService taskDefinitionService;
  private final IoTTaskCenterDispatchService dispatchService;
  private final IIoTDeviceService ioTDeviceService;
  private final IoTProductMapper ioTProductMapper;
  private final IoTDeviceMapper ioTDeviceMapper;
  private final IoTUserService ioTUserService;
  private final ISysJobService sysJobService;
  private final RedissonClient redissonClient;

  @Override
  public IoTTaskSubmitResultVO submitFunctionJobTask(
      IoTDeviceJobTaskSubmitBO submitBO, IoTUser creator) {
    PreparedJobContext context = prepareJobContext(submitBO);
    List<IoTDevice> devices = resolveTaskDevices(submitBO, creator);
    validateMatchedDevices(devices);

    Map<String, Object> requestSnapshot = buildRequestSnapshot(submitBO, context, devices.size());
    String taskName = buildExecutionTaskName(submitBO.getTaskName(), context.product, context.commandSummary);
    return createAndDispatchTask(
        null,
        requestSnapshot,
        creator.getUnionId(),
        resolveCreatorName(creator),
        taskName,
        devices,
        IoTTaskCenterConstants.TriggerType.DIRECT);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public IoTTaskDefinition createFunctionJobDefinition(
      IoTDeviceJobTaskSubmitBO submitBO, IoTUser creator) {
    PreparedJobContext context = prepareJobContext(submitBO);
    normalizeDefinitionSchedule(submitBO);

    IoTTaskDefinition definition =
        IoTTaskDefinition.builder()
            .definitionNo(generateDefinitionNo())
            .taskType(IoTTaskCenterConstants.TaskType.DEVICE_FUNCTION_JOB)
            .taskName(buildDefinitionName(submitBO, context.product, context.commandSummary))
            .bizModule("device")
            .bizKey(context.product.getProductKey())
            .productKey(context.product.getProductKey())
            .productName(context.product.getName())
            .scheduleType(resolveScheduleType(submitBO))
            .scheduleStatus(resolveScheduleStatus(submitBO))
            .cronExpression(resolveCronExpression(submitBO))
            .misfirePolicy(resolveMisfirePolicy(submitBO))
            .creatorId(creator.getUnionId())
            .creatorName(resolveCreatorName(creator))
            .requestSnapshot(JSONUtil.toJsonStr(buildRequestSnapshot(submitBO, context, null)))
            .build();

    taskDefinitionService.createTaskDefinition(definition);
    syncDefinitionSchedule(definition);
    if (definition.getJobId() != null) {
      taskDefinitionService.updateTaskDefinition(
          IoTTaskDefinition.builder().id(definition.getId()).jobId(definition.getJobId()).build());
    }
    return taskDefinitionService.selectTaskDefinitionById(definition.getId(), creator.getUnionId(), creator.isAdmin());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public IoTTaskDefinition updateFunctionJobDefinition(
      Long definitionId, IoTDeviceJobTaskSubmitBO submitBO, IoTUser creator) {
    IoTTaskDefinition existing =
        taskDefinitionService.selectTaskDefinitionById(definitionId, creator.getUnionId(), creator.isAdmin());
    if (existing == null) {
      throw new BaseException("任务定义不存在或无权访问");
    }
    if (!IoTTaskCenterConstants.TaskType.DEVICE_FUNCTION_JOB.equals(existing.getTaskType())) {
      throw new BaseException("当前任务定义不支持设备作业编辑");
    }

    PreparedJobContext context = prepareJobContext(submitBO);
    normalizeDefinitionSchedule(submitBO);

    IoTTaskDefinition update =
        IoTTaskDefinition.builder()
            .id(definitionId)
            .taskName(buildDefinitionName(submitBO, context.product, context.commandSummary))
            .bizModule("device")
            .bizKey(context.product.getProductKey())
            .productKey(context.product.getProductKey())
            .productName(context.product.getName())
            .scheduleType(resolveScheduleType(submitBO))
            .scheduleStatus(resolveScheduleStatus(submitBO))
            .cronExpression(resolveCronExpression(submitBO))
            .misfirePolicy(resolveMisfirePolicy(submitBO))
            .requestSnapshot(JSONUtil.toJsonStr(buildRequestSnapshot(submitBO, context, null)))
            .build();
    taskDefinitionService.updateTaskDefinition(update);

    IoTTaskDefinition refreshed =
        taskDefinitionService.selectTaskDefinitionById(definitionId, creator.getUnionId(), creator.isAdmin());
    if (refreshed == null) {
      throw new BaseException("任务定义不存在或无权访问");
    }
    if (existing.getJobId() != null) {
      refreshed.setJobId(existing.getJobId());
    }
    syncDefinitionSchedule(refreshed);
    taskDefinitionService.updateTaskDefinition(
        IoTTaskDefinition.builder().id(refreshed.getId()).jobId(refreshed.getJobId()).build());
    return taskDefinitionService.selectTaskDefinitionById(refreshed.getId(), creator.getUnionId(), creator.isAdmin());
  }

  @Override
  public IoTTaskSubmitResultVO runFunctionJobDefinition(Long definitionId, IoTUser operator) {
    IoTTaskDefinition definition = getDefinitionForRun(definitionId, operator);
    if (definition == null) {
      throw new BaseException("任务定义不存在或无权访问");
    }

    String lockKey = IoTTaskCenterConstants.LockKey.TASK_DEFINITION_RUN + definitionId;
    RLock lock = redissonClient.getLock(lockKey);
    boolean acquired = false;
    try {
      acquired = lock.tryLock(0, 10, TimeUnit.SECONDS);
      if (!acquired) {
        return warnAndReturnActiveDefinitionTask(definition, "未获取到执行锁");
      }
      if (taskCenterService.countActiveTasksByDefinitionId(definitionId) > 0) {
        return warnAndReturnActiveDefinitionTask(definition, "已有活动执行记录");
      }

      JSONObject request = parseRequestSnapshot(definition.getRequestSnapshot());
      IoTUser owner = ioTUserService.selectUserByUnionId(definition.getCreatorId());
      if (owner == null) {
        return createFailedDefinitionTask(
            definition,
            request,
            "任务创建人不存在，无法执行",
            resolveTriggerType(operator));
      }

      List<IoTDevice> devices = resolveTaskDevicesFromDefinition(request, owner);
      if (CollectionUtil.isEmpty(devices)) {
        return createFailedDefinitionTask(
            definition, request, "当前任务没有匹配设备", resolveTriggerType(operator));
      }
      if (devices.size() > MAX_TASK_DEVICE_COUNT) {
        return createFailedDefinitionTask(
            definition,
            request,
            "单次设备作业最多支持 2000 台设备",
            resolveTriggerType(operator));
      }

      request.set("deviceCount", devices.size());
      String taskName = buildExecutionTaskName(definition.getTaskName(), null, null);
      IoTTaskSubmitResultVO result =
          createAndDispatchTask(
              definition,
              request,
              definition.getCreatorId(),
              definition.getCreatorName(),
              taskName,
              devices,
              resolveTriggerType(operator));
      taskDefinitionService.updateLastExecution(definitionId, result.getTaskId(), new Date());
      return result;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BaseException("获取任务执行锁被中断");
    } finally {
      if (acquired && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void changeDefinitionScheduleStatus(Long definitionId, String scheduleStatus, IoTUser operator) {
    IoTTaskDefinition definition =
        taskDefinitionService.selectTaskDefinitionById(definitionId, operator.getUnionId(), operator.isAdmin());
    if (definition == null) {
      throw new BaseException("任务定义不存在或无权访问");
    }
    if (!IoTTaskCenterConstants.DefinitionScheduleType.CRON.equals(definition.getScheduleType())) {
      throw new BaseException("手动任务不支持启停调度");
    }
    String nextStatus =
        IoTTaskCenterConstants.DefinitionScheduleStatus.ENABLED.equals(scheduleStatus)
            ? IoTTaskCenterConstants.DefinitionScheduleStatus.ENABLED
            : IoTTaskCenterConstants.DefinitionScheduleStatus.PAUSED;
    definition.setScheduleStatus(nextStatus);
    taskDefinitionService.updateTaskDefinition(
        IoTTaskDefinition.builder().id(definition.getId()).scheduleStatus(nextStatus).build());
    definition.setScheduleStatus(nextStatus);
    syncDefinitionSchedule(definition);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteFunctionJobDefinitions(Long[] definitionIds, IoTUser operator) {
    if (definitionIds == null || definitionIds.length == 0) {
      return;
    }
    for (Long definitionId : definitionIds) {
      IoTTaskDefinition definition =
          taskDefinitionService.selectTaskDefinitionById(definitionId, operator.getUnionId(), operator.isAdmin());
      if (definition == null) {
        continue;
      }
      if (taskCenterService.countActiveTasksByDefinitionId(definitionId) > 0) {
        throw new BaseException("任务正在执行中，暂不允许删除");
      }
      deleteScheduleJobIfExists(definition.getJobId());
    }
    taskDefinitionService.deleteTaskDefinitionByIds(definitionIds, operator.getUnionId(), operator.isAdmin());
  }

  private PreparedJobContext prepareJobContext(IoTDeviceJobTaskSubmitBO submitBO) {
    validateSubmitParam(submitBO);

    IoTProduct product = ioTProductMapper.getProductByProductKey(submitBO.getProductKey());
    if (product == null) {
      throw new BaseException("产品不存在");
    }

    List<IoTDeviceJobCommandBO> commands = resolveSubmitCommands(submitBO);
    List<PreparedCommandContext> preparedCommands = new ArrayList<>();
    for (int i = 0; i < commands.size(); i++) {
      IoTDeviceJobCommandBO command = commands.get(i);
      JSONObject functionSnapshot = resolveFunctionSnapshot(product, command.getFunctionId());
      LinkedHashMap<String, IoTDeviceJobParamSpec> functionArgsSpec =
          DeviceJobArgumentResolver.normalizeSpecs(
              command.getFunctionArgsSpec(), command.getFunctionData());
      preparedCommands.add(
          new PreparedCommandContext(
              StrUtil.blankToDefault(command.getCommandId(), String.format("CMD_%02d", i + 1)),
              command.getFunctionId(),
              functionSnapshot.getStr("name", command.getFunctionId()),
              StrUtil.blankToDefault(command.getSource(), functionSnapshot.getStr("source")),
              command.getServiceType(),
              functionSnapshot,
              functionArgsSpec));
    }
    return new PreparedJobContext(
        product,
        preparedCommands,
        buildCommandSummary(preparedCommands),
        normalizeCommandExecutionMode(submitBO.getCommandExecutionMode()));
  }

  private void validateSubmitParam(IoTDeviceJobTaskSubmitBO submitBO) {
    if (submitBO == null) {
      throw new BaseException("请求参数不能为空");
    }
    if (StrUtil.isBlank(submitBO.getProductKey())) {
      throw new BaseException("请选择产品");
    }
    List<IoTDeviceJobCommandBO> commands = resolveSubmitCommands(submitBO);
    if (CollectionUtil.isEmpty(commands)) {
      throw new BaseException("请至少配置一条指令");
    }
    for (int i = 0; i < commands.size(); i++) {
      IoTDeviceJobCommandBO command = commands.get(i);
      if (command == null || StrUtil.isBlank(command.getFunctionId())) {
        throw new BaseException(String.format("请为第 %s 条指令选择功能", i + 1));
      }
    }
    if (!Boolean.TRUE.equals(submitBO.getChooseAll()) && ArrayUtil.isEmpty(submitBO.getIds())) {
      throw new BaseException("请至少选择一台设备");
    }
  }

  private List<IoTDeviceJobCommandBO> resolveSubmitCommands(IoTDeviceJobTaskSubmitBO submitBO) {
    if (submitBO == null) {
      return new ArrayList<>();
    }
    if (CollectionUtil.isNotEmpty(submitBO.getCommands())) {
      return submitBO.getCommands().stream().filter(item -> item != null).collect(Collectors.toList());
    }
    if (StrUtil.isBlank(submitBO.getFunctionId())) {
      return new ArrayList<>();
    }
    IoTDeviceJobCommandBO legacyCommand = new IoTDeviceJobCommandBO();
    legacyCommand.setCommandId("CMD_01");
    legacyCommand.setFunctionId(submitBO.getFunctionId());
    legacyCommand.setServiceType(submitBO.getServiceType());
    legacyCommand.setSource(submitBO.getSource());
    legacyCommand.setFunctionArgsSpec(
        submitBO.getFunctionArgsSpec() == null ? new LinkedHashMap<>() : submitBO.getFunctionArgsSpec());
    legacyCommand.setFunctionData(
        submitBO.getFunctionData() == null ? new LinkedHashMap<>() : submitBO.getFunctionData());
    return List.of(legacyCommand);
  }

  private JSONObject resolveFunctionSnapshot(IoTProduct product, String functionId) {
    JSONObject metadata =
        StrUtil.isBlank(product.getMetadata()) ? new JSONObject() : JSONUtil.parseObj(product.getMetadata());
    JSONArray functions = metadata.getJSONArray("functions");
    if (CollectionUtil.isEmpty(functions)) {
      throw new BaseException("当前产品没有可执行功能");
    }
    for (int i = 0; i < functions.size(); i++) {
      JSONObject item = functions.getJSONObject(i);
      if (StrUtil.equals(functionId, item.getStr("id"))) {
        return item;
      }
    }
    throw new BaseException("功能不存在或已变更");
  }

  private List<IoTDevice> resolveTaskDevices(IoTDeviceJobTaskSubmitBO submitBO, IoTUser creator) {
    List<IoTDevice> devices;
    if (Boolean.TRUE.equals(submitBO.getChooseAll())) {
      devices = ioTDeviceService.selectDevInstanceListWithTags(buildDeviceQuery(submitBO), creator);
    } else {
      devices = ioTDeviceMapper.listByIds(submitBO.getIds());
      devices =
          devices.stream()
              .filter(item -> StrUtil.equals(item.getProductKey(), submitBO.getProductKey()))
              .map(item -> filterAccessibleDevice(item, creator))
              .filter(java.util.Objects::nonNull)
              .collect(Collectors.toList());
    }
    return dedupeDevices(devices);
  }

  private List<IoTDevice> resolveTaskDevicesFromDefinition(JSONObject request, IoTUser creator) {
    if (creator == null) {
      return new ArrayList<>();
    }
    boolean chooseAll = request.getBool("chooseAll", false);
    if (chooseAll) {
      IoTDeviceBO query = new IoTDeviceBO();
      query.setProductKey(request.getStr("productKey"));
      query.setApplication(request.getStr("application"));
      query.setDevGroupId(request.getLong("devGroupId"));
      String keyword = request.getStr("keyword");
      if (StrUtil.isNotBlank(keyword)) {
        query.getParams().put("keyword", keyword);
      }
      return dedupeDevices(ioTDeviceService.selectDevInstanceListWithTags(query, creator));
    }

    String[] ids = toStringArray(request.get("ids"));
    if (ArrayUtil.isEmpty(ids)) {
      return new ArrayList<>();
    }
    List<IoTDevice> devices = ioTDeviceMapper.listByIds(ids);
    devices =
        devices.stream()
            .filter(item -> StrUtil.equals(item.getProductKey(), request.getStr("productKey")))
            .map(item -> filterAccessibleDevice(item, creator))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    return dedupeDevices(devices);
  }

  private IoTDevice filterAccessibleDevice(IoTDevice device, IoTUser scopeUser) {
    if (device == null || device.getId() == null) {
      return null;
    }
    try {
      return ioTDeviceService.selectDevInstanceById(String.valueOf(device.getId()), scopeUser);
    } catch (Exception ex) {
      log.debug(
          "设备作业过滤无权限设备, deviceId={}, scopeUser={}, message={}",
          device.getDeviceId(),
          scopeUser == null ? null : scopeUser.getUnionId(),
          ex.getMessage());
      return null;
    }
  }

  private List<IoTDevice> dedupeDevices(List<IoTDevice> devices) {
    Map<Long, IoTDevice> deduped = new LinkedHashMap<>();
    if (devices != null) {
      for (IoTDevice device : devices) {
        if (device != null && device.getId() != null) {
          deduped.put(device.getId(), device);
        }
      }
    }
    return new ArrayList<>(deduped.values());
  }

  private IoTDeviceBO buildDeviceQuery(IoTDeviceJobTaskSubmitBO submitBO) {
    IoTDeviceBO query = new IoTDeviceBO();
    query.setProductKey(submitBO.getProductKey());
    query.setApplication(submitBO.getApplication());
    query.setDevGroupId(submitBO.getDevGroupId());
    if (StrUtil.isNotBlank(submitBO.getKeyword())) {
      query.getParams().put("keyword", submitBO.getKeyword());
    }
    return query;
  }

  private void validateMatchedDevices(List<IoTDevice> devices) {
    if (CollectionUtil.isEmpty(devices)) {
      throw new BaseException("无匹配设备");
    }
    if (devices.size() > MAX_TASK_DEVICE_COUNT) {
      throw new BaseException("单次设备作业最多支持 2000 台设备");
    }
  }

  private Map<String, Object> buildRequestSnapshot(
      IoTDeviceJobTaskSubmitBO submitBO, PreparedJobContext context, Integer deviceCount) {
    Map<String, Object> requestSnapshot = new LinkedHashMap<>();
    requestSnapshot.put("productKey", context.product.getProductKey());
    requestSnapshot.put("productName", context.product.getName());
    requestSnapshot.put("application", submitBO.getApplication());
    requestSnapshot.put("devGroupId", submitBO.getDevGroupId());
    requestSnapshot.put("keyword", submitBO.getKeyword());
    requestSnapshot.put("chooseAll", Boolean.TRUE.equals(submitBO.getChooseAll()));
    requestSnapshot.put("ids", submitBO.getIds() == null ? new String[0] : submitBO.getIds());
    requestSnapshot.put("deviceCount", deviceCount);
    requestSnapshot.put("commandExecutionMode", context.commandExecutionMode);
    requestSnapshot.put("commandCount", context.commands.size());
    requestSnapshot.put("commands", buildRequestCommands(context.commands));
    PreparedCommandContext firstCommand =
        CollectionUtil.isEmpty(context.commands) ? null : context.commands.get(0);
    if (firstCommand != null) {
      requestSnapshot.put("functionId", firstCommand.functionId);
      requestSnapshot.put("functionName", firstCommand.functionName);
      requestSnapshot.put("source", firstCommand.source);
      requestSnapshot.put("serviceType", firstCommand.serviceType);
      requestSnapshot.put("functionDefinition", JSONUtil.parseObj(firstCommand.functionSnapshot));
      requestSnapshot.put(
          "functionArgsSpec", JSONUtil.parseObj(JSONUtil.toJsonStr(firstCommand.functionArgsSpec)));
      requestSnapshot.put(
          "functionData", DeviceJobArgumentResolver.buildDisplayValues(firstCommand.functionArgsSpec));
    }
    requestSnapshot.put("supportedVariables", DeviceJobArgumentResolver.supportedVariableHint());
    return requestSnapshot;
  }

  private String buildDefinitionName(
      IoTDeviceJobTaskSubmitBO submitBO, IoTProduct product, String commandSummary) {
    if (StrUtil.isNotBlank(submitBO.getTaskName())) {
      return submitBO.getTaskName().trim();
    }
    return String.format("设备任务-%s-%s", product.getName(), commandSummary);
  }

  private String buildExecutionTaskName(String baseName, IoTProduct product, String commandSummary) {
    String resolvedBase = baseName;
    if (StrUtil.isBlank(resolvedBase) && product != null && StrUtil.isNotBlank(commandSummary)) {
      resolvedBase = String.format("设备作业-%s-%s", product.getName(), commandSummary);
    }
    if (StrUtil.isBlank(resolvedBase)) {
      resolvedBase = "设备作业";
    }
    return String.format(
        "%s-%s", resolvedBase, cn.hutool.core.date.DateUtil.formatDateTime(new Date()));
  }

  private String resolveCreatorName(IoTUser creator) {
    return StrUtil.blankToDefault(
        StrUtil.blankToDefault(creator.getAlias(), creator.getUsername()), creator.getUnionId());
  }

  private IoTTaskSubmitResultVO createAndDispatchTask(
      IoTTaskDefinition definition,
      Object requestSnapshot,
      String creatorId,
      String creatorName,
      String taskName,
      List<IoTDevice> devices,
      String triggerType) {
    Date now = new Date();
    String taskNo = "TASK-" + now.getTime() + "-" + IdUtil.fastSimpleUUID().substring(0, 8);
    JSONObject request = requestSnapshot instanceof JSONObject
        ? (JSONObject) requestSnapshot
        : JSONUtil.parseObj(JSONUtil.toJsonStr(requestSnapshot));

    IoTTaskCenter taskCenter =
        IoTTaskCenter.builder()
            .taskNo(taskNo)
            .taskType(IoTTaskCenterConstants.TaskType.DEVICE_FUNCTION_JOB)
            .taskName(taskName)
            .bizModule("device")
            .bizKey(request.getStr("productKey"))
            .productKey(request.getStr("productKey"))
            .productName(request.getStr("productName"))
            .definitionId(definition == null ? null : definition.getId())
            .triggerType(triggerType)
            .status(IoTTaskCenterConstants.TaskStatus.PENDING)
            .totalCount(devices == null ? 0 : devices.size())
            .successCount(0)
            .failCount(0)
            .creatorId(creatorId)
            .creatorName(creatorName)
            .requestSnapshot(request.toString())
            .build();

    List<IoTTaskItem> items = buildTaskItems(devices);
    Long taskId = taskCenterService.createTask(taskCenter, items);
    taskCenterService.appendLog(
        taskId,
        IoTTaskCenterConstants.LogLevel.INFO,
        IoTTaskCenterConstants.LogType.SUBMIT,
        String.format("设备作业已创建，共 %s 台设备待执行", items.size()),
        request.toString());
    dispatchService.dispatch(taskId);

    return IoTTaskSubmitResultVO.builder()
        .taskId(taskId)
        .taskNo(taskNo)
        .taskType(IoTTaskCenterConstants.TaskType.DEVICE_FUNCTION_JOB)
        .taskName(taskName)
        .status(IoTTaskCenterConstants.TaskStatus.PENDING)
        .build();
  }

  private IoTTaskSubmitResultVO warnAndReturnActiveDefinitionTask(
      IoTTaskDefinition definition, String reason) {
    IoTTaskCenter activeTask = taskCenterService.selectLatestActiveTaskByDefinitionId(definition.getId());
    if (activeTask == null) {
      log.warn("任务定义正在执行，跳过重复触发, definitionId={}, reason={}", definition.getId(), reason);
      return IoTTaskSubmitResultVO.builder()
          .taskType(IoTTaskCenterConstants.TaskType.DEVICE_FUNCTION_JOB)
          .taskName(definition.getTaskName())
          .status(IoTTaskCenterConstants.TaskStatus.RUNNING)
          .build();
    }
    log.warn(
        "任务定义正在执行，跳过重复触发, definitionId={}, activeTaskId={}, taskNo={}, status={}, reason={}",
        definition.getId(),
        activeTask.getId(),
        activeTask.getTaskNo(),
        activeTask.getStatus(),
        reason);
    return IoTTaskSubmitResultVO.builder()
        .taskId(activeTask.getId())
        .taskNo(activeTask.getTaskNo())
        .taskType(activeTask.getTaskType())
        .taskName(activeTask.getTaskName())
        .status(activeTask.getStatus())
        .build();
  }

  private List<IoTTaskItem> buildTaskItems(List<IoTDevice> devices) {
    List<IoTTaskItem> items = new ArrayList<>();
    if (devices == null || devices.isEmpty()) {
      return items;
    }
    for (int i = 0; i < devices.size(); i++) {
      IoTDevice device = devices.get(i);
      items.add(
          IoTTaskItem.builder()
              .itemNo(String.format("DEV-%04d", i + 1))
              .itemKey(device.getDeviceId())
              .itemName(device.getDeviceName())
              .rowNo(i + 1)
              .status(IoTTaskCenterConstants.ItemStatus.PENDING)
              .rawData(JSONUtil.toJsonStr(buildDeviceSnapshot(device)))
              .build());
    }
    return items;
  }

  private IoTTaskSubmitResultVO createFailedDefinitionTask(
      IoTTaskDefinition definition, JSONObject request, String errorMessage, String triggerType) {
    String taskName = buildExecutionTaskName(definition.getTaskName(), null, null);
    IoTTaskSubmitResultVO result =
        createTaskRecordOnly(
            definition, request, definition.getCreatorId(), definition.getCreatorName(), taskName, triggerType);
    taskCenterService.appendLog(
        result.getTaskId(),
        IoTTaskCenterConstants.LogLevel.WARN,
        IoTTaskCenterConstants.LogType.EXECUTE,
        errorMessage,
        request == null ? null : request.toString());
    taskCenterService.markTaskFinished(
        result.getTaskId(),
        IoTTaskCenterConstants.TaskStatus.FAILED,
        JSONUtil.toJsonStr(Map.of("message", errorMessage)),
        errorMessage);
    taskDefinitionService.updateLastExecution(definition.getId(), result.getTaskId(), new Date());
    return result;
  }

  private IoTTaskSubmitResultVO createTaskRecordOnly(
      IoTTaskDefinition definition,
      JSONObject request,
      String creatorId,
      String creatorName,
      String taskName,
      String triggerType) {
    Date now = new Date();
    String taskNo = "TASK-" + now.getTime() + "-" + IdUtil.fastSimpleUUID().substring(0, 8);
    IoTTaskCenter taskCenter =
        IoTTaskCenter.builder()
            .taskNo(taskNo)
            .taskType(IoTTaskCenterConstants.TaskType.DEVICE_FUNCTION_JOB)
            .taskName(taskName)
            .bizModule("device")
            .bizKey(request == null ? null : request.getStr("productKey"))
            .productKey(request == null ? null : request.getStr("productKey"))
            .productName(request == null ? null : request.getStr("productName"))
            .definitionId(definition == null ? null : definition.getId())
            .triggerType(triggerType)
            .status(IoTTaskCenterConstants.TaskStatus.PENDING)
            .totalCount(0)
            .successCount(0)
            .failCount(0)
            .creatorId(creatorId)
            .creatorName(creatorName)
            .requestSnapshot(request == null ? null : request.toString())
            .build();
    Long taskId = taskCenterService.createTask(taskCenter, new ArrayList<>());
    return IoTTaskSubmitResultVO.builder()
        .taskId(taskId)
        .taskNo(taskNo)
        .taskType(IoTTaskCenterConstants.TaskType.DEVICE_FUNCTION_JOB)
        .taskName(taskName)
        .status(IoTTaskCenterConstants.TaskStatus.FAILED)
        .build();
  }

  private Map<String, Object> buildDeviceSnapshot(IoTDevice device) {
    LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
    if (device == null) {
      return snapshot;
    }
    snapshot.put("id", device.getId());
    snapshot.put("iotId", device.getIotId());
    snapshot.put("deviceId", device.getDeviceId());
    snapshot.put("deviceName", device.getDeviceName());
    snapshot.put("productKey", device.getProductKey());
    snapshot.put("productName", device.getProductName());
    snapshot.put("application", device.getApplication());
    snapshot.put("instance", device.getInstance());
    snapshot.put("state", device.getState());
    snapshot.put("onlineTime", device.getOnlineTime());
    snapshot.put("createTime", device.getCreateTime());
    snapshot.put("configuration", device.getConfiguration());
    return snapshot;
  }

  private void normalizeDefinitionSchedule(IoTDeviceJobTaskSubmitBO submitBO) {
    String scheduleType = resolveScheduleType(submitBO);
    if (!IoTTaskCenterConstants.DefinitionScheduleType.CRON.equals(scheduleType)) {
      submitBO.setScheduleType(IoTTaskCenterConstants.DefinitionScheduleType.MANUAL);
      submitBO.setScheduleStatus(IoTTaskCenterConstants.DefinitionScheduleStatus.PAUSED);
      submitBO.setCronExpression(null);
      submitBO.setMisfirePolicy(null);
      return;
    }
    if (StrUtil.isBlank(submitBO.getCronExpression())) {
      throw new BaseException("请输入Cron表达式");
    }
    if (!sysJobService.checkCronExpressionIsValid(submitBO.getCronExpression())) {
      throw new BaseException("Cron表达式不正确");
    }
    if (StrUtil.isBlank(submitBO.getScheduleStatus())) {
      submitBO.setScheduleStatus(IoTTaskCenterConstants.DefinitionScheduleStatus.ENABLED);
    }
    if (StrUtil.isBlank(submitBO.getMisfirePolicy())) {
      submitBO.setMisfirePolicy(ScheduleConstants.MISFIRE_FIRE_AND_PROCEED);
    }
  }

  private String resolveScheduleType(IoTDeviceJobTaskSubmitBO submitBO) {
    if (IoTTaskCenterConstants.DefinitionScheduleType.CRON.equalsIgnoreCase(submitBO.getScheduleType())) {
      return IoTTaskCenterConstants.DefinitionScheduleType.CRON;
    }
    return IoTTaskCenterConstants.DefinitionScheduleType.MANUAL;
  }

  private String resolveScheduleStatus(IoTDeviceJobTaskSubmitBO submitBO) {
    if (!IoTTaskCenterConstants.DefinitionScheduleType.CRON.equals(resolveScheduleType(submitBO))) {
      return IoTTaskCenterConstants.DefinitionScheduleStatus.PAUSED;
    }
    return IoTTaskCenterConstants.DefinitionScheduleStatus.ENABLED.equals(submitBO.getScheduleStatus())
        ? IoTTaskCenterConstants.DefinitionScheduleStatus.ENABLED
        : IoTTaskCenterConstants.DefinitionScheduleStatus.PAUSED;
  }

  private String resolveCronExpression(IoTDeviceJobTaskSubmitBO submitBO) {
    if (!IoTTaskCenterConstants.DefinitionScheduleType.CRON.equals(resolveScheduleType(submitBO))) {
      return null;
    }
    return submitBO.getCronExpression();
  }

  private String resolveMisfirePolicy(IoTDeviceJobTaskSubmitBO submitBO) {
    if (!IoTTaskCenterConstants.DefinitionScheduleType.CRON.equals(resolveScheduleType(submitBO))) {
      return null;
    }
    return StrUtil.blankToDefault(submitBO.getMisfirePolicy(), ScheduleConstants.MISFIRE_FIRE_AND_PROCEED);
  }

  private JSONArray buildRequestCommands(List<PreparedCommandContext> commands) {
    JSONArray commandArray = new JSONArray();
    if (commands == null || commands.isEmpty()) {
      return commandArray;
    }
    for (PreparedCommandContext command : commands) {
      JSONObject item = new JSONObject();
      item.set("commandId", command.commandId);
      item.set("functionId", command.functionId);
      item.set("functionName", command.functionName);
      item.set("source", command.source);
      item.set("serviceType", command.serviceType);
      item.set("functionDefinition", JSONUtil.parseObj(command.functionSnapshot));
      item.set("functionArgsSpec", JSONUtil.parseObj(JSONUtil.toJsonStr(command.functionArgsSpec)));
      item.set("functionData", DeviceJobArgumentResolver.buildDisplayValues(command.functionArgsSpec));
      commandArray.add(item);
    }
    return commandArray;
  }

  private String buildCommandSummary(List<PreparedCommandContext> commands) {
    if (CollectionUtil.isEmpty(commands)) {
      return "未配置指令";
    }
    PreparedCommandContext first = commands.get(0);
    if (commands.size() == 1) {
      return first.functionName;
    }
    return String.format("%s等%s条指令", first.functionName, commands.size());
  }

  private String normalizeCommandExecutionMode(String mode) {
    return IoTTaskCenterConstants.CommandExecutionMode.PARALLEL.equalsIgnoreCase(mode)
        ? IoTTaskCenterConstants.CommandExecutionMode.PARALLEL
        : IoTTaskCenterConstants.CommandExecutionMode.SERIAL;
  }

  private void syncDefinitionSchedule(IoTTaskDefinition definition) {
    try {
      if (!IoTTaskCenterConstants.DefinitionScheduleType.CRON.equals(definition.getScheduleType())) {
        deleteScheduleJobIfExists(definition.getJobId());
        definition.setJobId(null);
        return;
      }

      SysJob sysJob = buildScheduleJob(definition);
      if (definition.getJobId() == null || sysJobService.selectJobById(definition.getJobId()) == null) {
        sysJobService.insertJob(sysJob);
        definition.setJobId(sysJob.getJobId());
        return;
      }
      sysJob.setJobId(definition.getJobId());
      sysJobService.updateJob(sysJob);
    } catch (SchedulerException e) {
      log.error("同步任务定义调度失败, definitionId={}", definition.getId(), e);
      throw new BaseException("同步调度任务失败: " + extractRootCauseMessage(e));
    }
  }

  private SysJob buildScheduleJob(IoTTaskDefinition definition) {
    return SysJob.builder()
        .jobId(definition.getJobId())
        .jobName(buildScheduleJobName(definition.getId()))
        .jobGroup(TASK_JOB_GROUP)
        .invokeTarget(buildInvokeTarget(definition.getId()))
        .cronExpression(definition.getCronExpression())
        .misfirePolicy(
            StrUtil.blankToDefault(definition.getMisfirePolicy(), ScheduleConstants.MISFIRE_FIRE_AND_PROCEED))
        .concurrent("1")
        .status(
            IoTTaskCenterConstants.DefinitionScheduleStatus.ENABLED.equals(definition.getScheduleStatus())
                ? ScheduleConstants.Status.NORMAL.getValue()
                : ScheduleConstants.Status.PAUSE.getValue())
        .remark(StrUtil.blankToDefault(definition.getDefinitionNo(), definition.getTaskName()))
        .build();
  }

  private void deleteScheduleJobIfExists(Long jobId) {
    if (jobId == null) {
      return;
    }
    SysJob existing = sysJobService.selectJobById(jobId);
    if (existing == null) {
      return;
    }
    try {
      sysJobService.deleteJob(existing);
    } catch (SchedulerException e) {
      log.error("删除任务定义调度失败, jobId={}", jobId, e);
      throw new BaseException("删除调度任务失败: " + extractRootCauseMessage(e));
    }
  }

  private String extractRootCauseMessage(Throwable throwable) {
    String message = null;
    Throwable current = throwable;
    while (current != null) {
      if (StrUtil.isNotBlank(current.getMessage())) {
        message = current.getMessage();
      }
      current = current.getCause();
    }
    return StrUtil.blankToDefault(message, "未知调度异常");
  }

  private String buildScheduleJobName(Long definitionId) {
    return "TASK_DEF_" + definitionId;
  }

  private String buildInvokeTarget(Long definitionId) {
    return String.format("iotTaskDefinitionQuartzInvoker.execute(%sL)", definitionId);
  }

  private String generateDefinitionNo() {
    return "DEF-" + System.currentTimeMillis() + "-" + IdUtil.fastSimpleUUID().substring(0, 8);
  }

  private IoTTaskDefinition getDefinitionForRun(Long definitionId, IoTUser operator) {
    if (operator == null) {
      return taskDefinitionService.selectTaskDefinitionById(definitionId, null, true);
    }
    return taskDefinitionService.selectTaskDefinitionById(definitionId, operator.getUnionId(), operator.isAdmin());
  }

  private JSONObject parseRequestSnapshot(String requestSnapshot) {
    if (StrUtil.isBlank(requestSnapshot)) {
      return new JSONObject();
    }
    return JSONUtil.parseObj(requestSnapshot);
  }

  private String[] toStringArray(Object rawValue) {
    if (rawValue == null) {
      return new String[0];
    }
    if (rawValue instanceof String[]) {
      return (String[]) rawValue;
    }
    if (rawValue instanceof JSONArray jsonArray) {
      String[] result = new String[jsonArray.size()];
      for (int i = 0; i < jsonArray.size(); i++) {
        result[i] = jsonArray.getStr(i);
      }
      return result;
    }
    if (rawValue instanceof List<?> list) {
      return list.stream().map(String::valueOf).toArray(String[]::new);
    }
    if (rawValue instanceof String str) {
      if (StrUtil.isBlank(str)) {
        return new String[0];
      }
      if (str.trim().startsWith("[")) {
        JSONArray jsonArray = JSONUtil.parseArray(str);
        String[] result = new String[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
          result[i] = jsonArray.getStr(i);
        }
        return result;
      }
      return Arrays.stream(str.split(","))
          .map(String::trim)
          .filter(StrUtil::isNotBlank)
          .toArray(String[]::new);
    }
    return new String[] {String.valueOf(rawValue)};
  }

  private String resolveTriggerType(IoTUser operator) {
    return operator == null
        ? IoTTaskCenterConstants.TriggerType.SCHEDULE
        : IoTTaskCenterConstants.TriggerType.MANUAL;
  }

  private record PreparedJobContext(
      IoTProduct product,
      List<PreparedCommandContext> commands,
      String commandSummary,
      String commandExecutionMode) {}

  private record PreparedCommandContext(
      String commandId,
      String functionId,
      String functionName,
      String source,
      String serviceType,
      JSONObject functionSnapshot,
      LinkedHashMap<String, IoTDeviceJobParamSpec> functionArgsSpec) {}
}
