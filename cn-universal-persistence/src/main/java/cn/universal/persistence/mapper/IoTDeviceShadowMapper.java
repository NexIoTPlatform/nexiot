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
import cn.universal.persistence.entity.IoTDeviceShadow;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTDeviceShadowMapper extends BaseMapper<IoTDeviceShadow> {

  IoTDeviceShadow getDeviceShadow(String iotId);

  String getShadowMetadata(
      @Param("productKey") String productKey, @Param("deviceId") String deviceId);

  /**
   * 批量查询设备影子
   *
   * @param iotIds 设备ID列表
   * @return 设备影子列表
   */
  List<IoTDeviceShadow> selectByIotIds(@Param("iotIds") List<String> iotIds);

  /**
   * 批量插入设备影子
   *
   * @param shadows 设备影子列表
   * @return 插入数量
   */
  int batchInsert(@Param("shadows") List<IoTDeviceShadow> shadows);

  /**
   * 批量更新设备影子
   *
   * @param shadows 设备影子列表
   * @return 更新数量
   */
  int batchUpdate(@Param("shadows") List<IoTDeviceShadow> shadows);
}
