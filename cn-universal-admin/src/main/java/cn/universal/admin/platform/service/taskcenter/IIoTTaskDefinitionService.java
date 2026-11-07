package cn.universal.admin.platform.service.taskcenter;

import cn.universal.persistence.entity.IoTTaskDefinition;
import java.util.Date;
import java.util.List;

public interface IIoTTaskDefinitionService {

  List<IoTTaskDefinition> selectTaskDefinitionList(
      IoTTaskDefinition query, String unionId, boolean admin);

  IoTTaskDefinition selectTaskDefinitionById(Long id, String unionId, boolean admin);

  Long createTaskDefinition(IoTTaskDefinition definition);

  void updateTaskDefinition(IoTTaskDefinition definition);

  int deleteTaskDefinitionByIds(Long[] ids, String unionId, boolean admin);

  void updateLastExecution(Long id, Long taskId, Date executeTime);
}
