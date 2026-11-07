package cn.universal.admin.platform.service.taskcenter.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.IoTTaskLog;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.vo.IoTTaskCenterStatisticsVO;
import cn.universal.persistence.mapper.IoTTaskCenterMapper;
import cn.universal.persistence.mapper.IoTTaskItemMapper;
import cn.universal.persistence.mapper.IoTTaskLogMapper;
import cn.universal.security.service.IoTUserService;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IoTTaskCenterServiceImpl implements IIoTTaskCenterService {

  private final IoTTaskCenterMapper taskCenterMapper;
  private final IoTTaskItemMapper taskItemMapper;
  private final IoTTaskLogMapper taskLogMapper;
  private final IoTUserService ioTUserService;

  @Override
  public List<IoTTaskCenter> selectTaskCenterList(IoTTaskCenter query, String unionId, boolean admin) {
    if (query == null) {
      query = new IoTTaskCenter();
    }
    String scopedCreatorId = unionId;
    if (!admin) {
      scopedCreatorId = resolveTenantOwnerUnionId(unionId);
      query.setCreatorId(scopedCreatorId);
      query.setTenantId(resolveTenantId(unionId));
    }
    return taskCenterMapper.selectTaskCenterList(query, scopedCreatorId, admin);
  }

  @Override
  public IoTTaskCenter selectTaskCenterById(Long id, String unionId, boolean admin) {
    IoTTaskCenter taskCenter = taskCenterMapper.selectTaskCenterById(id);
    if (taskCenter == null) {
      return null;
    }
    if (!admin && !canAccessTenantResource(taskCenter.getTenantId(), taskCenter.getCreatorId(), unionId)) {
      return null;
    }
    return taskCenter;
  }

  @Override
  public List<IoTTaskItem> selectTaskItemList(IoTTaskItem query, String unionId, boolean admin) {
    IoTTaskCenter taskCenter = selectTaskCenterById(query.getTaskId(), unionId, admin);
    if (taskCenter == null) {
      return Collections.emptyList();
    }
    return taskItemMapper.selectTaskItemList(query);
  }

  @Override
  public List<IoTTaskLog> selectTaskLogList(Long taskId, String unionId, boolean admin) {
    IoTTaskCenter taskCenter = selectTaskCenterById(taskId, unionId, admin);
    if (taskCenter == null) {
      return Collections.emptyList();
    }
    return taskLogMapper.selectTaskLogList(taskId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long createTask(IoTTaskCenter task, List<IoTTaskItem> items) {
    Date now = new Date();
    if (StrUtil.isBlank(task.getTenantId())) {
      task.setTenantId(resolveTenantId(task.getCreatorId()));
    }
    task.setCreateTime(now);
    task.setUpdateTime(now);
    taskCenterMapper.insert(task);
    if (CollectionUtil.isNotEmpty(items)) {
      for (IoTTaskItem item : items) {
        item.setId(IdUtil.getSnowflakeNextId());
        item.setTaskId(task.getId());
        item.setTenantId(task.getTenantId());
        item.setCreateTime(now);
        item.setUpdateTime(now);
      }
      taskItemMapper.insertTaskItemBatch(items);
    }
    return task.getId();
  }

  @Override
  public IoTTaskCenter getTaskForExecute(Long taskId) {
    return taskCenterMapper.selectTaskCenterById(taskId);
  }

  @Override
  public List<IoTTaskItem> selectPendingItems(Long taskId) {
    return taskItemMapper.selectByTaskId(taskId);
  }

  @Override
  public void appendLog(
      Long taskId, String logLevel, String logType, String content, String extJson) {
    IoTTaskLog taskLog =
        IoTTaskLog.builder()
            .taskId(taskId)
            .logLevel(logLevel)
            .logType(logType)
            .content(content)
            .extJson(extJson)
            .createTime(new Date())
            .build();
    taskLogMapper.insert(taskLog);
  }

  @Override
  public boolean markTaskRunning(Long taskId) {
    return taskCenterMapper.markTaskRunningIfPending(taskId) > 0;
  }

  @Override
  public void markTaskFinished(
      Long taskId, String forcedStatus, String resultSnapshot, String errorMessage) {
    IoTTaskCenterStatisticsVO statistics = refreshTaskStatistics(taskId);
    String finalStatus = forcedStatus;
    if (finalStatus == null) {
      finalStatus = resolveTaskStatus(statistics);
    }
    IoTTaskCenter update =
        IoTTaskCenter.builder()
            .id(taskId)
            .status(finalStatus)
            .resultSnapshot(resultSnapshot)
            .errorMessage(errorMessage)
            .endTime(new Date())
            .updateTime(new Date())
            .build();
    taskCenterMapper.updateByPrimaryKeySelective(update);
  }

  @Override
  public void markItemRunning(Long itemId) {
    IoTTaskItem update =
        IoTTaskItem.builder()
            .id(itemId)
            .status(IoTTaskCenterConstants.ItemStatus.RUNNING)
            .startTime(new Date())
            .updateTime(new Date())
            .build();
    taskItemMapper.updateByPrimaryKeySelective(update);
  }

  @Override
  public void markItemFinished(Long itemId, String status, String message, String resultData) {
    IoTTaskItem update =
        IoTTaskItem.builder()
            .id(itemId)
            .status(status)
            .message(message)
            .resultData(resultData)
            .endTime(new Date())
            .updateTime(new Date())
            .build();
    taskItemMapper.updateByPrimaryKeySelective(update);
  }

  @Override
  public IoTTaskCenterStatisticsVO refreshTaskStatistics(Long taskId) {
    IoTTaskCenterStatisticsVO statistics = taskItemMapper.selectTaskStatistics(taskId);
    if (statistics == null) {
      statistics = new IoTTaskCenterStatisticsVO();
    }
    IoTTaskCenter update =
        IoTTaskCenter.builder()
            .id(taskId)
            .totalCount(defaultZero(statistics.getTotalCount()))
            .successCount(defaultZero(statistics.getSuccessCount()))
            .failCount(defaultZero(statistics.getFailCount()))
            .updateTime(new Date())
            .build();
    taskCenterMapper.updateByPrimaryKeySelective(update);
    return statistics;
  }

  @Override
  public int countActiveTasksByDefinitionId(Long definitionId) {
    if (definitionId == null) {
      return 0;
    }
    return taskCenterMapper.countActiveTasksByDefinitionId(definitionId);
  }

  @Override
  public IoTTaskCenter selectLatestActiveTaskByDefinitionId(Long definitionId) {
    if (definitionId == null) {
      return null;
    }
    return taskCenterMapper.selectLatestActiveTaskByDefinitionId(definitionId);
  }

  private Integer defaultZero(Integer value) {
    return value == null ? 0 : value;
  }

  private String resolveTaskStatus(IoTTaskCenterStatisticsVO statistics) {
    int total = defaultZero(statistics.getTotalCount());
    int success = defaultZero(statistics.getSuccessCount());
    int fail = defaultZero(statistics.getFailCount());
    int pending = defaultZero(statistics.getPendingCount());
    int running = defaultZero(statistics.getRunningCount());
    if (pending > 0 || running > 0) {
      return IoTTaskCenterConstants.TaskStatus.RUNNING;
    }
    if (total == 0) {
      return IoTTaskCenterConstants.TaskStatus.FAILED;
    }
    if (success == total) {
      return IoTTaskCenterConstants.TaskStatus.COMPLETED;
    }
    if (fail == total) {
      return IoTTaskCenterConstants.TaskStatus.FAILED;
    }
    return IoTTaskCenterConstants.TaskStatus.PARTIAL_SUCCESS;
  }

  private String resolveTenantId(String creatorId) {
    if (StrUtil.isBlank(creatorId)) {
      return null;
    }
    IoTUser creator = ioTUserService.selectUserByUnionId(creatorId);
    if (creator == null) {
      return null;
    }
    return StrUtil.blankToDefault(creator.getTenantId(), creator.getUnionId());
  }

  private String resolveTenantOwnerUnionId(String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    IoTUser tenantOwner = ioTUserService.selectUserByUnionId(unionId);
    if (tenantOwner == null) {
      return unionId;
    }
    if (!tenantOwner.isAdmin() && !StrUtil.equalsIgnoreCase(tenantOwner.getTenantRole(), "ADMIN")) {
      String tenantId = resolveTenantId(tenantOwner.getUnionId());
      if (StrUtil.isNotBlank(tenantId)) {
        List<IoTUser> adminUsers =
            ioTUserService.selectUserList(
                IoTUser.builder().tenantId(tenantId).tenantRole("ADMIN").build());
        if (adminUsers != null && !adminUsers.isEmpty()) {
          tenantOwner = adminUsers.get(0);
        }
      }
    }
    return tenantOwner.getUnionId();
  }

  private boolean canAccessTenantResource(String tenantId, String creatorId, String unionId) {
    if (StrUtil.isBlank(unionId)) {
      return false;
    }
    String currentTenantId = resolveTenantId(unionId);
    String resourceTenantId = StrUtil.blankToDefault(tenantId, resolveTenantId(creatorId));
    if (StrUtil.isBlank(resourceTenantId)) {
      return StrUtil.equals(unionId, creatorId);
    }
    return StrUtil.equals(currentTenantId, resourceTenantId)
        || StrUtil.equals(unionId, creatorId);
  }
}
