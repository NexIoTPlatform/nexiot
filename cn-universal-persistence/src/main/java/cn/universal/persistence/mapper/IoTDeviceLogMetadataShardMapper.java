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
import cn.universal.persistence.consistent.DeviceLogMetaHashSharding;
import cn.universal.persistence.consistent.TableShard;
import cn.universal.persistence.entity.IoTDeviceLogMetadata;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.util.List;
import org.apache.ibatis.annotations.Param;

@TableShard(
    tableNamePrefix = "iot_device_log_metadata",
    value = "iotId",
    fieldFlag = true,
    shardStrategy = DeviceLogMetaHashSharding.class)
public interface IoTDeviceLogMetadataShardMapper extends BaseMapper<IoTDeviceLogMetadata> {

  List<IoTDeviceLogMetadataVO> selectLogMetaCursor(LogQuery query);

  /**
   * 批量插入元数据记录。
   *
   * <p>显式携带 iotId，确保分表拦截器能够正确路由到目标分表。
   */
  int batchInsertByIotId(
      @Param("iotId") String iotId, @Param("metadatas") List<IoTDeviceLogMetadata> metadatas);

  List<IoTDeviceLogMetadataVO> selectLogMetaList(LogQuery logQuery);

  List<PropertyTrendVO.Bucket> selectPropertyTrend(PropertyTrendQuery query);

  List<PropertyTrendSampleVO> selectPropertyTrendSamples(PropertyTrendQuery query);

  /**
   * 获取设备事件的统计信息
   *
   * @param event 事件id
   * @param iotId 设备iotId
   */
  List<String> queryEventTotalByEventAndId(
      @Param("event") String event, @Param("iotId") String iotId);
}
