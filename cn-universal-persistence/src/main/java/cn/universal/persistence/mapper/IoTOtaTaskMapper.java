/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA任务Mapper
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTOtaTask;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTOtaTaskMapper extends BaseMapper<IoTOtaTask> {

  List<IoTOtaTask> selectTaskList(IoTOtaTask query);

  IoTOtaTask selectTaskById(@Param("id") Long id);

  /** 在锁定任务设备前锁定任务，保持下发、确认、取消和重试的锁顺序一致。 */
  IoTOtaTask selectTaskForUpdate(@Param("id") Long id);

  List<IoTOtaTask> selectRecentTasksByProductKey(@Param("productKey") String productKey);

  int insertTask(IoTOtaTask task);

  int updateTask(IoTOtaTask task);
}
