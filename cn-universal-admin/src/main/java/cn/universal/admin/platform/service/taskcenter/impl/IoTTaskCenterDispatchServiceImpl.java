package cn.universal.admin.platform.service.taskcenter.impl;

import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.IoTTaskCenterDispatchService;
import cn.universal.admin.platform.service.taskcenter.IoTTaskCenterExecutor;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.persistence.entity.IoTTaskCenter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IoTTaskCenterDispatchServiceImpl implements IoTTaskCenterDispatchService {

  private final IIoTTaskCenterService taskCenterService;
  private final List<IoTTaskCenterExecutor> executors;

  @Override
  @Async("taskExecutor")
  public void dispatch(Long taskId) {
    IoTTaskCenter taskCenter = taskCenterService.getTaskForExecute(taskId);
    if (taskCenter == null) {
      return;
    }
    Map<String, IoTTaskCenterExecutor> executorMap =
        executors.stream().collect(Collectors.toMap(IoTTaskCenterExecutor::taskType, item -> item));
    IoTTaskCenterExecutor executor = executorMap.get(taskCenter.getTaskType());
    if (executor == null) {
      taskCenterService.appendLog(
          taskId,
          IoTTaskCenterConstants.LogLevel.ERROR,
          IoTTaskCenterConstants.LogType.EXECUTE,
          "未找到任务执行器: " + taskCenter.getTaskType(),
          null);
      taskCenterService.markTaskFinished(
          taskId, IoTTaskCenterConstants.TaskStatus.FAILED, null, "未找到任务执行器");
      return;
    }

    if (!taskCenterService.markTaskRunning(taskId)) {
      log.info("任务已被其他线程抢占或已非待执行状态, taskId={}", taskId);
      return;
    }
    taskCenterService.appendLog(
        taskId,
        IoTTaskCenterConstants.LogLevel.INFO,
        IoTTaskCenterConstants.LogType.EXECUTE,
        "任务开始执行",
        null);
    try {
      executor.execute(taskCenter);
    } catch (Exception e) {
      log.error("任务执行失败, taskId={}", taskId, e);
      taskCenterService.appendLog(
          taskId,
          IoTTaskCenterConstants.LogLevel.ERROR,
          IoTTaskCenterConstants.LogType.EXECUTE,
          "任务执行失败: " + e.getMessage(),
          null);
      taskCenterService.markTaskFinished(
          taskId, IoTTaskCenterConstants.TaskStatus.FAILED, null, e.getMessage());
    }
  }
}
