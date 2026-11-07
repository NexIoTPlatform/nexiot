/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA任务设备明细Mapper
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTOtaTaskDeviceMapper extends BaseMapper<IoTOtaTaskDevice> {

  List<IoTOtaTaskDevice> selectTaskDeviceList(IoTOtaTaskDevice query);

  IoTOtaTaskDevice selectTaskDeviceById(@Param("id") Long id);

  /** 在当前事务中锁定设备传输会话，串行处理分包发送、确认和重试。 */
  IoTOtaTaskDevice selectTaskDeviceForUpdate(@Param("id") Long id);

  /** 仅扫描运行中的持久化传输会话，供逐片超时调度使用。 */
  List<IoTOtaTaskDevice> selectActiveTransfers();

  List<IoTOtaTaskDevice> selectByTaskId(@Param("taskId") Long taskId);

  /** 已锁定任务后，读取并锁定全部设备最新状态，用于准确汇总任务统计。 */
  List<IoTOtaTaskDevice> selectByTaskIdForUpdate(@Param("taskId") Long taskId);

  List<IoTOtaTaskDevice> selectRecentByDevice(
      @Param("productKey") String productKey, @Param("deviceId") String deviceId);

  IoTOtaTaskDevice selectLatestActiveByDevice(
      @Param("productKey") String productKey, @Param("deviceId") String deviceId);

  List<IoTOtaTaskDevice> selectActiveCandidatesByDevice(
      @Param("productKey") String productKey, @Param("deviceId") String deviceId);

  List<IoTOtaTaskDevice> selectActiveConflictsByDeviceIds(
      @Param("productKey") String productKey,
      @Param("module") String module,
      @Param("deviceIds") List<String> deviceIds);

  List<IoTOtaTaskDevice> selectTimedOutActiveByTaskId(
      @Param("taskId") Long taskId, @Param("timeoutSeconds") Integer timeoutSeconds);

  int insertTaskDeviceBatch(@Param("list") List<IoTOtaTaskDevice> list);

  int updateTaskDevice(IoTOtaTaskDevice taskDevice);

  int updateTaskDeviceIfActive(IoTOtaTaskDevice taskDevice);

  /** 在已锁定的重新下发事务中显式清除上一次失败信息，保留普通 patch 的 null 语义。 */
  int resetRetryFields(@Param("id") Long id);

  int markWaitingAsDispatching(@Param("id") Long id);

  int updateStatusByTaskId(@Param("taskId") Long taskId, @Param("status") String status);
}
