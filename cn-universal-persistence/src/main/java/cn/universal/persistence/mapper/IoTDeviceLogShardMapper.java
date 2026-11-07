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

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.consistent.DeviceLogHashSharding;
import cn.universal.persistence.consistent.TableShard;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.bo.IoTDeviceLogBO;
import cn.universal.persistence.entity.vo.EventCountAndTimeVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.query.DeviceLogQuery;
import cn.universal.persistence.query.LogQuery;
import java.util.List;
import org.apache.ibatis.annotations.Param;

@TableShard(
    tableNamePrefix = "iot_device_log",
    value = "iotId",
    fieldFlag = true,
    shardStrategy = DeviceLogHashSharding.class)
public interface IoTDeviceLogShardMapper extends BaseMapper<IoTDeviceLog> {

  List<IoTDeviceLogVO> queryLogCursorList(LogQuery query);

  EventCountAndTimeVO queryHistoryEventStats(LogQuery query);

  List<EventCountAndTimeVO> queryHistoryEventSummaries(
      @Param("iotId") String iotId,
      @Param("productKey") String productKey,
      @Param("events") List<String> events);

  List<IoTDeviceLogVO> queryLogPageV2ByIdList(LogQuery logQuery);

  /** 分页查询设备日志列表 */
  List<IoTDeviceLogVO> queryLogPageList(IoTDeviceLogBO bo);

  /** 分页查询设备日志列表 */
  List<IoTDeviceLogVO> queryLogPageV2List(LogQuery logQuery);

  /** 根据主键查询 */
  IoTDeviceLogVO queryLogById(LogQuery logQuery);

  /**
   * 获取设备事件的统计信息
   *
   * @param event 事件id
   * @param iotId 设备iotId
   */
  List<String> queryEventTotalByEventAndId(
      @Param("event") String event, @Param("iotId") String iotId);

  void addFunctionLog(IoTDeviceLogBO bo);

  /** 查询最新包含坐标的第二条日志 */
  IoTDeviceLog queryCoordinatesLogByIotId(@Param("iotId") String iotId);

  /** 查询最新包含坐标的第一条日志 */
  IoTDeviceLog queryLatestCoordinatesLogByIotId(@Param("iotId") String iotId);

  IoTDeviceLog getRecentCommand(
      @Param("iotId") String iotId,
      @Param("commandId") String commandId,
      @Param("messageType") String messageType,
      @Param("createTime") Long createTime,
      @Param("commandStatus") Integer commandStatus);

  void updateRecentCommandSuccess(
      @Param("ioTDeviceLog") IoTDeviceLog ioTDeviceLog, @Param("iotId") String iotId);

  /**
   * 通用设备日志查询接口
   *
   * @param query 查询条件
   * @return 设备日志列表
   */
  List<IoTDeviceLog> queryDeviceLogs(@Param("query") DeviceLogQuery query);

  /**
   * 批量插入设备日志。
   *
   * <p>显式携带 iotId，确保分表拦截器能够正确路由到目标分表。
   */
  int batchInsertByIotId(@Param("iotId") String iotId, @Param("logs") List<IoTDeviceLog> logs);
}
