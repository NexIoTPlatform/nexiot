package cn.universal.admin.platform.service.taskcenter;

import cn.universal.persistence.entity.IoTTaskCenter;

public interface IoTTaskCenterExecutor {

  String taskType();

  void execute(IoTTaskCenter taskCenter);
}
