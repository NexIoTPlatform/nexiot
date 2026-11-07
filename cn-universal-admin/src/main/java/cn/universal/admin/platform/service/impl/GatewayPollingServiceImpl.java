/*
 *
 * Copyright (c) 2025, IoT-Universal. All Rights Reserved.
 *
 * @Description: 本文件由 Aleo 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: Aleo
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.platform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.platform.service.DeviceCommandDispatcher;
import cn.universal.admin.platform.service.IGatewayPollingService;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.domain.R;
import cn.universal.persistence.base.ModbusPollingExecutor;
import cn.universal.persistence.dto.GatewayPollingConfigDTO;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.GatewayPollingCommand;
import cn.universal.persistence.entity.GatewayPollingConfig;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.GatewayPollingCommandMapper;
import cn.universal.persistence.mapper.GatewayPollingConfigMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 网关定时任务服务实现
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
@Service
@Slf4j
public class GatewayPollingServiceImpl implements IGatewayPollingService {

  private static final Set<Integer> SUPPORTED_INTERVAL_SECONDS = Set.of(30, 60, 120, 300, 600);
  private static final int DEFAULT_INTERVAL_SECONDS = 120;
  private static final int DEFAULT_TIMEOUT_SECONDS = 10;
  private static final int DEFAULT_RETRY_TIMES = 3;
  private static final int DEFAULT_COMMAND_INTERVAL_MS = 1200;
  private static final int MIN_COMMAND_INTERVAL_MS = 300;
  private static final int MAX_COMMAND_INTERVAL_MS = 10000;
  private static final String TASK_TYPE_CLOUD_POLLING =
      GatewayPollingConfig.TASK_TYPE_CLOUD_POLLING;
  private static final String TASK_TYPE_MODBUS_COLLECT =
      GatewayPollingConfig.TASK_TYPE_MODBUS_COLLECT;
  private static final String COMMAND_TYPE_CUSTOM = "CUSTOM";
  private static final String COMMAND_TYPE_MODBUS = "MODBUS";
  private static final String PROTOCOL_MODBUS = "modbus";

  @Autowired private GatewayPollingConfigMapper pollingConfigMapper;

  @Autowired private GatewayPollingCommandMapper pollingCommandMapper;

  @Autowired private IoTDeviceMapper deviceMapper;

  @Autowired private DeviceCommandDispatcher commandDispatcher;

  @Autowired(required = false)
  private ModbusPollingExecutor modbusPollingExecutor;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public R savePollingConfig(GatewayPollingConfigDTO dto) {
    return savePollingConfig(TASK_TYPE_CLOUD_POLLING, dto);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public R savePollingConfig(String taskType, GatewayPollingConfigDTO dto) {
    try {
      if (dto == null) {
        return R.error("配置不能为空");
      }
      String normalizedTaskType = normalizeTaskType(taskType);
      dto.setTaskType(normalizedTaskType);

      // 1. 验证是否为可配置定时任务的设备
      IoTDeviceDTO device =
          deviceMapper.selectIoTDeviceBO(
              BeanUtil.beanToMap(
                  IoTDevice.builder()
                      .productKey(dto.getProductKey())
                      .deviceId(dto.getDeviceId())
                      .build()));
      if (device == null) {
        return R.error("设备不存在");
      }

      if (!supportsPollingDevice(normalizedTaskType, device)) {
        return R.error(pollingDeviceUnsupportedMessage(normalizedTaskType));
      }
      R supportCheck = validateTaskSupported(normalizedTaskType, device.getThirdPlatform());
      if (!supportCheck.isSuccess()) {
        return supportCheck;
      }

      normalizePollingConfig(dto);

      // 2. 保存或更新定时任务配置
      GatewayPollingConfig config = new GatewayPollingConfig();
      BeanUtils.copyProperties(dto, config);
      config.setTaskType(normalizedTaskType);
      config.setEnabled(GatewayPollingConfig.toEnabledValue(Boolean.TRUE.equals(dto.getEnabled())));

      // 计算下次轮询时间
      if (config.isEnabledFlag()) {
        config.setNextPollTime(calculateNextPollTime(new Date(), config.getIntervalSeconds()));
      }

      // 初始化状态
      if (config.getPollingStatus() == null) {
        config.setPollingStatus("NORMAL");
      }

      // 判断是新增还是更新 (使用 productKey + deviceId 组合键)
      GatewayPollingConfig existConfig =
          pollingConfigMapper.selectByDevice(
              dto.getProductKey(), dto.getDeviceId(), normalizedTaskType);
      if (existConfig != null) {
        config.setId(existConfig.getId());
        config.setUpdateTime(new Date());
        pollingConfigMapper.updateByPrimaryKeySelective(config);
      } else {
        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());
        pollingConfigMapper.insertSelective(config);
      }

      // 3. 保存任务明细
      pollingCommandMapper.deleteByGateway(
          dto.getProductKey(), dto.getDeviceId(), normalizedTaskType);
      if (CollUtil.isNotEmpty(dto.getCommands())) {
        // 插入新指令
        for (int i = 0; i < dto.getCommands().size(); i++) {
          GatewayPollingCommand cmd = dto.getCommands().get(i);
          normalizeCommandForTask(cmd, normalizedTaskType);
          cmd.setGatewayProductKey(dto.getProductKey());
          cmd.setGatewayDeviceId(dto.getDeviceId());
          cmd.setTaskType(normalizedTaskType);
          cmd.setExecutionOrder(i);
          cmd.setCreateTime(new Date());
          cmd.setUpdateTime(new Date());
          pollingCommandMapper.insertSelective(cmd);
        }
      }

      log.info(
          "保存网关定时任务配置成功: productKey={}, deviceId={}, taskType={}, interval={}s, commands={}",
          dto.getProductKey(),
          dto.getDeviceId(),
          normalizedTaskType,
          dto.getIntervalSeconds(),
          dto.getCommands() != null ? dto.getCommands().size() : 0);

      return R.ok("保存成功");

    } catch (Exception e) {
      log.error(
          "保存网关定时任务配置失败: productKey={}, deviceId={}, taskType={}",
          dto == null ? null : dto.getProductKey(),
          dto == null ? null : dto.getDeviceId(),
          taskType,
          e);
      return R.error("保存失败: " + e.getMessage());
    }
  }

  @Override
  public R getPollingConfig(String productKey, String deviceId) {
    return getPollingConfig(productKey, deviceId, TASK_TYPE_CLOUD_POLLING);
  }

  @Override
  public R getPollingConfig(String productKey, String deviceId, String taskType) {
    try {
      String normalizedTaskType = normalizeTaskType(taskType);
      GatewayPollingConfig config =
          pollingConfigMapper.selectByDevice(productKey, deviceId, normalizedTaskType);
      if (config == null) {
        return R.ok(null);
      }

      // 查询指令列表 (使用 productKey + deviceId)
      List<GatewayPollingCommand> commands =
          pollingCommandMapper.selectAllByGateway(productKey, deviceId, normalizedTaskType);

      // 构建DTO返回
      GatewayPollingConfigDTO dto = new GatewayPollingConfigDTO();
      BeanUtils.copyProperties(config, dto);
      dto.setTaskType(normalizedTaskType);
      dto.setEnabled(config.isEnabledFlag());
      dto.setCommands(commands);

      return R.ok(dto);

    } catch (Exception e) {
      log.error(
          "查询网关定时任务配置失败: productKey={}, deviceId={}, taskType={}",
          productKey,
          deviceId,
          taskType,
          e);
      return R.error("查询失败: " + e.getMessage());
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public R deletePollingConfig(String productKey, String deviceId) {
    return deletePollingConfig(productKey, deviceId, TASK_TYPE_CLOUD_POLLING);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public R deletePollingConfig(String productKey, String deviceId, String taskType) {
    try {
      String normalizedTaskType = normalizeTaskType(taskType);
      // 删除配置 (使用 productKey + deviceId)
      GatewayPollingConfig config =
          pollingConfigMapper.selectByDevice(productKey, deviceId, normalizedTaskType);
      if (config != null) {
        pollingConfigMapper.deleteByPrimaryKey(config.getId());
      }

      // 删除指令 (使用 productKey + deviceId)
      pollingCommandMapper.deleteByGateway(productKey, deviceId, normalizedTaskType);

      log.info(
          "删除网关定时任务配置成功: productKey={}, deviceId={}, taskType={}",
          productKey,
          deviceId,
          normalizedTaskType);
      return R.ok("删除成功");

    } catch (Exception e) {
      log.error(
          "删除网关定时任务配置失败: productKey={}, deviceId={}, taskType={}",
          productKey,
          deviceId,
          taskType,
          e);
      return R.error("删除失败: " + e.getMessage());
    }
  }

  @Override
  public void pollGatewayDevice(GatewayPollingConfig config) {
    try {
      // 1. 检查任务所属设备
      IoTDevice device =
          deviceMapper.selectIoTDevice(config.getProductKey(), config.getDeviceId());

      if (device == null) {
        log.warn(
            "轮询设备不存在，跳过执行: productKey={}, deviceId={}",
            config.getProductKey(),
            config.getDeviceId());
        pollingConfigMapper.updatePollingFail(config.getId(), calculateNextPollTime(config));
        return;
      }
      if (!supportsPollingDevice(config.getTaskType(), device)) {
        log.warn(
            "设备节点不支持当前定时任务，跳过执行: productKey={}, deviceId={}, taskType={}, deviceNode={}",
            config.getProductKey(),
            config.getDeviceId(),
            config.getTaskType(),
            device.getDeviceNode());
        pollingConfigMapper.updatePollingFail(config.getId(), calculateNextPollTime(config));
        return;
      }
      if (isModbusCollectTask(config.getTaskType())
          && !StrUtil.equalsIgnoreCase(PROTOCOL_MODBUS, device.getThirdPlatform())) {
        log.warn(
            "Modbus采集任务仅支持third_platform=modbus，跳过执行: productKey={}, deviceId={}, thirdPlatform={}",
            config.getProductKey(),
            config.getDeviceId(),
            device.getThirdPlatform());
        pollingConfigMapper.updatePollingFail(config.getId(), calculateNextPollTime(config));
        return;
      }

      //      if (gateway == null || !Boolean.TRUE.equals(gateway.getState())) {
      //        log.warn(
      //            "网关离线，跳过轮询: productKey={}, deviceId={}", config.getProductKey(),
      // config.getDeviceId());
      //        return;
      //      }

      // 2. 查询任务明细 (使用 productKey + deviceId)
      List<GatewayPollingCommand> commands =
          pollingCommandMapper.selectByGateway(
              config.getProductKey(), config.getDeviceId(), normalizeTaskType(config.getTaskType()));

      if (CollUtil.isEmpty(commands)) {
        log.warn(
            "网关没有配置定时任务: productKey={}, deviceId={}, taskType={}",
            config.getProductKey(),
            config.getDeviceId(),
            config.getTaskType());
        return;
      }

      log.info(
          "开始执行网关定时任务: productKey={}, deviceId={}, taskType={}, commands={}",
          config.getProductKey(),
          config.getDeviceId(),
          config.getTaskType(),
          commands.size());

      // 3. 按顺序执行任务，每条任务间添加延迟避免设备缓冲区溢出
      boolean allSuccess = true;
      int executedCount = 0;
      // 获取配置的指令间隔，默认按推荐值 1200ms 保护现场总线。
      int commandIntervalMs = resolveCommandIntervalMs(config.getCommandIntervalMs());

      for (GatewayPollingCommand command : commands) {
        if (command.isEnabledFlag()) {
          // 非第一条指令先等待，给设备响应时间
          if (executedCount > 0) {
            try {
              Thread.sleep(commandIntervalMs);
              log.debug(
                  "[指令间隔] 等待{}ms，防止设备缓冲区溢出: deviceId={}", commandIntervalMs, config.getDeviceId());
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              log.warn("指令间隔等待被中断", e);
              allSuccess = false;
              break;
            }
          }

          applyConfigDefaults(config, command);
          boolean success = executeCommand(config, device, command);
          if (!success) {
            allSuccess = false;
          }
          executedCount++;
        }
      }

      // 4. 更新轮询状态
      if (allSuccess) {
        Date nextPollTime = calculateNextPollTime(config);
        pollingConfigMapper.updatePollingSuccess(config.getId(), nextPollTime);
        log.info(
            "网关定时任务执行成功: productKey={}, deviceId={}, taskType={}, nextPollTime={}",
            config.getProductKey(),
            config.getDeviceId(),
            config.getTaskType(),
            DateUtil.formatDateTime(nextPollTime));
      } else {
        pollingConfigMapper.updatePollingFail(config.getId(), calculateNextPollTime(config));
        log.warn(
            "网关定时任务部分失败: productKey={}, deviceId={}, taskType={}",
            config.getProductKey(),
            config.getDeviceId(),
            config.getTaskType());
      }

    } catch (Exception e) {
      log.error(
          "网关定时任务异常: productKey={}, deviceId={}", config.getProductKey(), config.getDeviceId(), e);
      pollingConfigMapper.updatePollingFail(config.getId(), calculateNextPollTime(config));
    }
  }

  @Override
  public List<GatewayPollingConfig> getDuePollingDevices(int intervalSeconds, String taskType) {
    return pollingConfigMapper.selectDuePolling(intervalSeconds, normalizeTaskType(taskType));
  }

  @Override
  public R testPolling(String productKey, String deviceId) {
    return testPolling(productKey, deviceId, TASK_TYPE_CLOUD_POLLING);
  }

  @Override
  public R testPolling(String productKey, String deviceId, String taskType) {
    try {
      String normalizedTaskType = normalizeTaskType(taskType);
      // 1. 查询定时任务配置
      GatewayPollingConfig config =
          pollingConfigMapper.selectByDevice(productKey, deviceId, normalizedTaskType);
      if (config == null) {
        return R.error("未配置定时任务，请先保存配置");
      }

      // 2. 验证是否为可执行任务的设备
      IoTDeviceDTO device =
          deviceMapper.selectIoTDeviceBO(
              BeanUtil.beanToMap(
                  IoTDevice.builder().productKey(productKey).deviceId(deviceId).build()));

      if (device == null) {
        return R.error("设备不存在");
      }

      if (!supportsPollingDevice(normalizedTaskType, device)) {
        return R.error(pollingDeviceUnsupportedMessage(normalizedTaskType));
      }
      R supportCheck = validateTaskSupported(normalizedTaskType, device.getThirdPlatform());
      if (!supportCheck.isSuccess()) {
        return supportCheck;
      }

      // 3. 查询任务明细
      List<GatewayPollingCommand> commands =
          pollingCommandMapper.selectByGateway(productKey, deviceId, normalizedTaskType);

      if (CollUtil.isEmpty(commands)) {
        return R.error("未配置任务明细，请先添加任务");
      }

      log.info(
          "开始测试网关定时任务: productKey={}, deviceId={}, taskType={}, commands={}",
          productKey,
          deviceId,
          normalizedTaskType,
          commands.size());

      // 4. 立即执行任务
      pollGatewayDevice(config);

      return R.ok(
          "测试任务已执行，共执行 "
              + commands.stream().filter(GatewayPollingCommand::isEnabledFlag).count()
              + " 条任务");

    } catch (Exception e) {
      log.error(
          "测试网关定时任务失败: productKey={}, deviceId={}, taskType={}",
          productKey,
          deviceId,
          taskType,
          e);
      return R.error("测试任务失败: " + e.getMessage());
    }
  }

  @Override
  public R testPollingCommand(String productKey, String deviceId, GatewayPollingCommand command) {
    return testPollingCommand(productKey, deviceId, TASK_TYPE_CLOUD_POLLING, command);
  }

  @Override
  public R testPollingCommand(
      String productKey, String deviceId, String taskType, GatewayPollingCommand command) {
    try {
      if (command == null) {
        return R.error("测试指令不能为空");
      }
      String normalizedTaskType = normalizeTaskType(taskType);

      IoTDeviceDTO device =
          deviceMapper.selectIoTDeviceBO(
              BeanUtil.beanToMap(
                  IoTDevice.builder().productKey(productKey).deviceId(deviceId).build()));
      if (device == null) {
        return R.error("设备不存在");
      }
      if (!supportsPollingDevice(normalizedTaskType, device)) {
        return R.error(pollingDeviceUnsupportedMessage(normalizedTaskType));
      }
      R supportCheck = validateTaskSupported(normalizedTaskType, device.getThirdPlatform());
      if (!supportCheck.isSuccess()) {
        return supportCheck;
      }

      IoTDevice targetDevice = deviceMapper.selectIoTDevice(productKey, deviceId);
      if (targetDevice == null) {
        return R.error("设备不存在");
      }

      command.setGatewayProductKey(productKey);
      command.setGatewayDeviceId(deviceId);
      normalizeCommandForTask(command, normalizedTaskType);
      if (command.getEnabled() == null) {
        command.setEnabled(GatewayPollingCommand.ENABLED_YES);
      }

      if (isModbusCollectTask(normalizedTaskType)) {
        if (modbusPollingExecutor == null) {
          return R.error("Modbus采集执行器未启用");
        }
        return modbusPollingExecutor.preview(targetDevice, command);
      }

      if (StrUtil.isBlank(command.getCommandHex())) {
        return R.error("透传内容不能为空");
      }
      return commandDispatcher.sendCommand(productKey, deviceId, command.getCommandHex());
    } catch (Exception e) {
      log.error(
          "测试任务失败: productKey={}, deviceId={}, taskType={}",
          productKey,
          deviceId,
          taskType,
          e);
      return R.error("测试任务失败: " + e.getMessage());
    }
  }

  // ==================== 私有方法 ====================

  /** 执行单条指令 */
  private boolean executeCommand(
      GatewayPollingConfig config, IoTDevice device, GatewayPollingCommand command) {
    try {
      if (isModbusCollectTask(config.getTaskType())) {
        return executeModbusCommand(device, command);
      }

      log.info(
          "执行定时指令: productKey={}, deviceId={}, commandName={}, commandType={}, hex={}",
          device.getProductKey(),
          device.getDeviceId(),
          command.getCommandName(),
          command.getCommandType(),
          command.getCommandHex());

      // 通过统一调度器发送指令
      R<?> result =
          commandDispatcher.sendCommand(
              device.getProductKey(), device.getDeviceId(), command.getCommandHex());

      if (result.isSuccess()) {
        log.info(
            "定时指令发送成功: productKey={}, deviceId={}, commandName={}",
            device.getProductKey(),
            device.getDeviceId(),
            command.getCommandName());
        return true;
      } else {
        log.warn(
            "定时指令发送失败: productKey={}, deviceId={}, commandName={}, reason={}",
            device.getProductKey(),
            device.getDeviceId(),
            command.getCommandName(),
            result.getMsg());
        return false;
      }

    } catch (Exception e) {
      log.error(
          "执行定时指令异常: productKey={}, deviceId={}, commandName={}",
          device.getProductKey(),
          device.getDeviceId(),
          command.getCommandName(),
          e);
      return false;
    }
  }

  private boolean executeModbusCommand(IoTDevice device, GatewayPollingCommand command) {
    if (modbusPollingExecutor == null) {
      log.warn(
          "Modbus采集执行器未启用,跳过执行: productKey={}, deviceId={}, commandName={}",
          device.getProductKey(),
          device.getDeviceId(),
          command.getCommandName());
      return false;
    }

    log.info(
        "执行Modbus采集任务: productKey={}, deviceId={}, commandName={}, commandType={}",
        device.getProductKey(),
        device.getDeviceId(),
        command.getCommandName(),
        command.getCommandType());

    R<?> result = modbusPollingExecutor.execute(device, command);
    if (result == null) {
      log.warn(
          "Modbus采集任务返回空结果: productKey={}, deviceId={}, commandName={}",
          device.getProductKey(),
          device.getDeviceId(),
          command.getCommandName());
      return false;
    }

    if (result.isSuccess()) {
      log.info(
          "Modbus采集任务执行成功: productKey={}, deviceId={}, commandName={}",
          device.getProductKey(),
          device.getDeviceId(),
          command.getCommandName());
      return true;
    }

    log.warn(
        "Modbus采集任务执行失败: productKey={}, deviceId={}, commandName={}, reason={}",
        device.getProductKey(),
        device.getDeviceId(),
        command.getCommandName(),
        result.getMsg());
    return false;
  }

  private boolean isModbusCollectTask(String taskType) {
    return StrUtil.equalsIgnoreCase(TASK_TYPE_MODBUS_COLLECT, normalizeTaskType(taskType));
  }

  private boolean supportsPollingDevice(String taskType, IoTDeviceDTO device) {
    if (device == null || device.getDeviceNode() == null) {
      return false;
    }
    if (isModbusCollectTask(taskType)) {
      return DeviceNode.GATEWAY.equals(device.getDeviceNode())
          || DeviceNode.DEVICE.equals(device.getDeviceNode());
    }
    return DeviceNode.GATEWAY.equals(device.getDeviceNode());
  }

  private boolean supportsPollingDevice(String taskType, IoTDevice device) {
    if (device == null || StrUtil.isBlank(device.getDeviceNode())) {
      return false;
    }
    if (isModbusCollectTask(taskType)) {
      return StrUtil.equalsAnyIgnoreCase(
          device.getDeviceNode(), DeviceNode.GATEWAY.name(), DeviceNode.DEVICE.name());
    }
    return StrUtil.equalsIgnoreCase(DeviceNode.GATEWAY.name(), device.getDeviceNode());
  }

  private String pollingDeviceUnsupportedMessage(String taskType) {
    return isModbusCollectTask(taskType) ? "只有网关或直连设备才能配置Modbus采集" : "只有网关设备才能配置定时任务";
  }

  private String normalizeTaskType(String taskType) {
    if (StrUtil.isBlank(taskType)) {
      return TASK_TYPE_CLOUD_POLLING;
    }
    String normalized = taskType.trim().toUpperCase();
    if (StrUtil.equalsAnyIgnoreCase(normalized, "CLOUD", "CUSTOM", "PASSTHROUGH")) {
      return TASK_TYPE_CLOUD_POLLING;
    }
    if (StrUtil.equalsAnyIgnoreCase(normalized, "MODBUS", "MODBUS_POLLING")) {
      return TASK_TYPE_MODBUS_COLLECT;
    }
    if (StrUtil.equalsIgnoreCase(TASK_TYPE_MODBUS_COLLECT, normalized)) {
      return TASK_TYPE_MODBUS_COLLECT;
    }
    if (StrUtil.equalsIgnoreCase(TASK_TYPE_CLOUD_POLLING, normalized)) {
      return TASK_TYPE_CLOUD_POLLING;
    }
    throw new IllegalArgumentException("不支持的任务类型: " + taskType);
  }

  private R validateTaskSupported(String taskType, String thirdPlatform) {
    if (isModbusCollectTask(taskType) && !StrUtil.equalsIgnoreCase(PROTOCOL_MODBUS, thirdPlatform)) {
      return R.error("Modbus采集仅支持 third_platform 为 modbus 的产品");
    }
    return R.ok();
  }

  private void normalizeCommandForTask(GatewayPollingCommand command, String taskType) {
    if (command == null) {
      return;
    }
    String normalizedTaskType = normalizeTaskType(taskType);
    command.setTaskType(normalizedTaskType);
    if (isModbusCollectTask(normalizedTaskType)) {
      command.setCommandType(COMMAND_TYPE_MODBUS);
      return;
    }
    if (StrUtil.isBlank(command.getCommandType())
        || StrUtil.equalsIgnoreCase(COMMAND_TYPE_MODBUS, command.getCommandType())) {
      command.setCommandType(COMMAND_TYPE_CUSTOM);
    }
  }

  private void applyConfigDefaults(GatewayPollingConfig config, GatewayPollingCommand command) {
    if (config == null || command == null) {
      return;
    }
    if (command.getTimeoutMs() == null && config.getTimeoutSeconds() != null) {
      command.setTimeoutMs(config.getTimeoutSeconds() * 1000);
    }
    if (isModbusCollectTask(config.getTaskType())) {
      command.setProtocolParams(
          mergeRetryTimes(StrUtil.blankToDefault(command.getProtocolParams(), "{}"), config.getRetryTimes()));
    }
  }

  private String mergeRetryTimes(String protocolParams, Integer retryTimes) {
    if (retryTimes == null) {
      return protocolParams;
    }
    try {
      cn.hutool.json.JSONObject params = cn.hutool.json.JSONUtil.parseObj(protocolParams);
      if (!params.containsKey("retryTimes") && !params.containsKey("retry")) {
        params.set("retryTimes", retryTimes);
      }
      return params.toString();
    } catch (Exception e) {
      return protocolParams;
    }
  }

  /** 计算下次轮询时间 */
  private Date calculateNextPollTime(GatewayPollingConfig config) {
    Date baseTime = config == null || config.getNextPollTime() == null ? new Date() : config.getNextPollTime();
    int intervalSeconds =
        config == null || config.getIntervalSeconds() == null
            ? DEFAULT_INTERVAL_SECONDS
            : config.getIntervalSeconds();
    return calculateNextPollTime(baseTime, intervalSeconds);
  }

  private Date calculateNextPollTime(Date baseTime, int intervalSeconds) {
    Date now = new Date();
    Date nextPollTime = DateUtil.offsetSecond(baseTime == null ? now : baseTime, intervalSeconds);
    while (!nextPollTime.after(now)) {
      nextPollTime = DateUtil.offsetSecond(nextPollTime, intervalSeconds);
    }
    return nextPollTime;
  }

  private void normalizePollingConfig(GatewayPollingConfigDTO dto) {
    Integer intervalSeconds =
        dto.getIntervalSeconds() == null ? DEFAULT_INTERVAL_SECONDS : dto.getIntervalSeconds();
    if (!SUPPORTED_INTERVAL_SECONDS.contains(intervalSeconds)) {
      throw new IllegalArgumentException("轮询间隔仅支持 30/60/120/300/600 秒");
    }
    dto.setIntervalSeconds(intervalSeconds);

    if (dto.getTimeoutSeconds() == null || dto.getTimeoutSeconds() <= 0) {
      dto.setTimeoutSeconds(DEFAULT_TIMEOUT_SECONDS);
    }
    if (dto.getRetryTimes() == null || dto.getRetryTimes() < 0) {
      dto.setRetryTimes(DEFAULT_RETRY_TIMES);
    }
    dto.setCommandIntervalMs(resolveCommandIntervalMs(dto.getCommandIntervalMs()));
  }

  private int resolveCommandIntervalMs(Integer commandIntervalMs) {
    if (commandIntervalMs == null) {
      return DEFAULT_COMMAND_INTERVAL_MS;
    }
    return Math.min(Math.max(commandIntervalMs, MIN_COMMAND_INTERVAL_MS), MAX_COMMAND_INTERVAL_MS);
  }
}
