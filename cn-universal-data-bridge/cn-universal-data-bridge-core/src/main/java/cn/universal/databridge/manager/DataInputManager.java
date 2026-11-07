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

package cn.universal.databridge.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.DataInputLog;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.logger.DataBridgeLogger;
import cn.universal.databridge.plugin.DataInputPlugin;
import cn.universal.databridge.service.DataBridgeConfigService;
import cn.universal.databridge.service.DataInputLogService;
import cn.universal.databridge.service.ResourceConnectionService;
import cn.universal.persistence.base.BaseUPRequest;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 数据输入管理器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component
@Slf4j
public class DataInputManager {

  @Autowired(required = false)
  private Map<String, DataInputPlugin> inputPlugins;

  @Resource private ResourceConnectionService resourceConnectionService;

  @Resource private DataBridgeConfigService dataBridgeConfigService;

  @Resource private DataInputLogService dataInputLogService;

  @Resource private DataBridgeLogger dataBridgeLogger;

  // 存储正在运行的输入任务
  private final Map<Long, Boolean> runningTasks = new ConcurrentHashMap<>();

  /** 启动数据输入任务 */
  public void startInputTask(Long configId) {
    try {
      DataBridgeConfig config = dataBridgeConfigService.getById(configId);
      if (config == null) {
        throw new RuntimeException("配置不存在");
      }
      // TODO: 需要实现新的输入配置判断逻辑，因为Direction枚举已被移除

      ResourceConnection connection =
          resourceConnectionService.getById(config.getTargetResourceId());
      if (connection == null || connection.getStatus() != 1) {
        throw new RuntimeException("资源连接不存在或已禁用");
      }

      DataInputPlugin plugin = inputPlugins.get(config.getBridgeType().name().toUpperCase());
      if (plugin == null) {
        throw new RuntimeException("未找到对应的输入插件: " + config.getBridgeType());
      }

      // 标记任务为运行状态
      runningTasks.put(configId, true);

      // 创建数据消费者回调
      Consumer<List<BaseUPRequest>> dataConsumer = data -> processInputData(data, config);

      // 启动数据输入
      // TODO: 需要重新设计输入插件的启动机制，因为相关方法已被移除
      log.info("数据输入任务启动逻辑需要重新实现，配置ID: {}", configId);

    } catch (Exception e) {
      runningTasks.remove(configId);
      log.error("启动数据输入任务失败，配置ID: {}, 错误: {}", configId, e.getMessage(), e);
      throw e;
    }
  }

  /** 停止数据输入任务 */
  public void stopInputTask(Long configId) {
    try {
      DataBridgeConfig config = dataBridgeConfigService.getById(configId);
      if (config == null) {
        return;
      }

      ResourceConnection connection =
          resourceConnectionService.getById(config.getTargetResourceId());
      if (connection == null) {
        return;
      }

      DataInputPlugin plugin = inputPlugins.get(config.getBridgeType().name().toUpperCase());
      if (plugin != null) {
        // TODO: 需要重新设计输入插件的停止机制，因为stopDataInput方法已被移除
        log.info("停止数据输入插件逻辑需要重新实现");
      }

      // 移除运行标记
      runningTasks.remove(configId);

      log.info("停止数据输入任务成功，配置ID: {}", configId);

    } catch (Exception e) {
      log.error("停止数据输入任务失败，配置ID: {}, 错误: {}", configId, e.getMessage(), e);
      throw e;
    }
  }

  /** 检查任务是否正在运行 */
  public Boolean isTaskRunning(Long configId) {
    return runningTasks.getOrDefault(configId, false);
  }

  /** 启动定时拉取任务 */
  @Async("dbBoundTaskExecutor")
  public void startScheduledPullTask(
      DataBridgeConfig config, ResourceConnection connection, DataInputPlugin plugin) {
    Long configId = config.getId();

    while (runningTasks.getOrDefault(configId, false)) {
      try {
        // 拉取数据
        // TODO: 需要重新设计数据拉取机制，因为pullInputData方法已被移除
        List<BaseUPRequest> data = null; // plugin.pullInputData(config, connection);

        if (CollectionUtil.isNotEmpty(data)) {
          processInputData(data, config);
        }

        // 等待下次拉取（可以从配置中读取间隔时间）
        Thread.sleep(getScheduleInterval(config));

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.error("定时拉取数据失败，配置ID: {}, 错误: {}", configId, e.getMessage(), e);

        // 记录错误日志
        logInputExecution(config, 0, 0, 0, e.getMessage(), 0);

        // 等待一段时间后重试
        try {
          Thread.sleep(30000); // 30秒后重试
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
  }

  /** 处理输入数据 */
  private void processInputData(List<BaseUPRequest> data, DataBridgeConfig config) {
    if (CollectionUtil.isEmpty(data)) {
      return;
    }

    long startTime = System.currentTimeMillis();
    int successCount = 0;
    int failedCount = 0;
    String errorMessage = null;

    try {
      // TODO: 这里需要将数据推送到IoT平台
      // 可以通过消息队列或直接调用IoT平台的API
      for (BaseUPRequest request : data) {
        try {
          // 推送到IoT平台的逻辑
          pushToIoTPlatform(request, config);
          successCount++;
        } catch (Exception e) {
          failedCount++;
          log.error("推送数据到IoT平台失败: {}", e.getMessage(), e);
        }
      }

      log.debug(
          "处理输入数据完成，配置: {}, 总数: {}, 成功: {}, 失败: {}",
          config.getName(),
          data.size(),
          successCount,
          failedCount);

    } catch (Exception e) {
      failedCount = data.size();
      errorMessage = e.getMessage();
      log.error("处理输入数据异常，配置: {}, 错误: {}", config.getName(), e.getMessage(), e);
    } finally {
      long executionTime = System.currentTimeMillis() - startTime;
      logInputExecution(
          config, data.size(), successCount, failedCount, errorMessage, executionTime);
    }
  }

  /** 推送数据到IoT平台 */
  private void pushToIoTPlatform(BaseUPRequest request, DataBridgeConfig config) {
    // TODO: 实现推送到IoT平台的逻辑
    // 这里可以通过消息队列、HTTP API等方式将数据推送到IoT平台
    log.debug(
        "推送数据到IoT平台: 设备={}, 产品={}",
        request.getIoTDeviceDTO() != null ? request.getIoTDeviceDTO().getDeviceId() : "unknown",
        config.getTargetResourceId());
  }

  /** 记录输入执行日志 */
  private void logInputExecution(
      DataBridgeConfig config,
      int messageCount,
      int successCount,
      int failedCount,
      String errorMessage,
      long executionTime) {
    try {
      DataInputLog log =
          DataInputLog.builder()
              .configId(config.getId())
              .configName(config.getName())
              .sourceSystem(config.getBridgeType().name())
              .messageCount(messageCount)
              .successCount(successCount)
              .failedCount(failedCount)
              .errorMessage(errorMessage)
              .executionTime(executionTime)
              .status(failedCount > 0 ? DataInputLog.Status.FAILED : DataInputLog.Status.SUCCESS)
              .createTime(LocalDateTime.now())
              .createBy(config.getCreateBy())
              .build();

      dataInputLogService.save(log);
    } catch (Exception e) {
      log.error("记录输入执行日志失败: {}", e.getMessage(), e);
    }
  }

  /** 获取调度间隔时间（毫秒） */
  private long getScheduleInterval(DataBridgeConfig config) {
    // 从配置中读取间隔时间，默认60秒
    // TODO: 可以从 inputConfig 中解析间隔时间
    return 60000; // 60秒
  }
}
