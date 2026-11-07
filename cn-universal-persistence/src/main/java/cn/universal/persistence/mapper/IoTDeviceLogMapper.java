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
import cn.universal.persistence.query.LogQuery;
import java.util.List;
import org.apache.ibatis.annotations.Param;

@TableShard(
    tableNamePrefix = "iot_device_log",
    value = "iotId",
    fieldFlag = true,
    shardStrategy = DeviceLogHashSharding.class)
public interface IoTDeviceLogMapper extends BaseMapper<IoTDeviceLog> {

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
  EventCountAndTimeVO queryEventCountAndTime(
      @Param("event") String event, @Param("iotId") String iotId);

  /** 查询最新包含坐标的第二条日志 */
  IoTDeviceLog queryCoordinatesLogByIotId(@Param("iotId") String iotId);

}
