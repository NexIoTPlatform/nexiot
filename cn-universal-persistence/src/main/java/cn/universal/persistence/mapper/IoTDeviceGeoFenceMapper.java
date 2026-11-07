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
import cn.universal.persistence.entity.IoTDeviceGeoFence;
import cn.universal.persistence.entity.vo.IoTDeviceGeoFenceVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 设备围栏表 @Author gitee.com/NexIoT
 *
 * @since 2023/8/5 8:54
 */
public interface IoTDeviceGeoFenceMapper extends BaseMapper<IoTDeviceGeoFence> {

  List<IoTDeviceGeoFence> selectByIotId(
      @Param("iotId") String iotId, @Param("appUnionId") String appUnionId);

  List<IoTDeviceGeoFenceVO> selectList(
      @Param("ioTDeviceGeoFence") IoTDeviceGeoFence ioTDeviceGeoFence);

  int updateFence(@Param("ioTDeviceGeoFence") IoTDeviceGeoFence ioTDeviceGeoFence);
}
