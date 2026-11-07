package cn.universal.admin.platform.service.taskcenter;

import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.IoTTaskLog;
import cn.universal.persistence.entity.vo.IoTTaskCenterStatisticsVO;
import java.util.List;

public interface IIoTTaskCenterService {

  List<IoTTaskCenter> selectTaskCenterList(IoTTaskCenter query, String unionId, boolean admin);

  IoTTaskCenter selectTaskCenterById(Long id, String unionId, boolean admin);

  List<IoTTaskItem> selectTaskItemList(IoTTaskItem query, String unionId, boolean admin);

  List<IoTTaskLog> selectTaskLogList(Long taskId, String unionId, boolean admin);

  Long createTask(IoTTaskCenter task, List<IoTTaskItem> items);

  IoTTaskCenter getTaskForExecute(Long taskId);

  List<IoTTaskItem> selectPendingItems(Long taskId);

  void appendLog(Long taskId, String logLevel, String logType, String content, String extJson);

  boolean markTaskRunning(Long taskId);

  void markTaskFinished(Long taskId, String forcedStatus, String resultSnapshot, String errorMessage);

  void markItemRunning(Long itemId);

  void markItemFinished(Long itemId, String status, String message, String resultData);

  IoTTaskCenterStatisticsVO refreshTaskStatistics(Long taskId);

  int countActiveTasksByDefinitionId(Long definitionId);

  IoTTaskCenter selectLatestActiveTaskByDefinitionId(Long definitionId);
}
