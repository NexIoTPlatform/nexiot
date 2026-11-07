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
import cn.universal.persistence.entity.SupportMapAreas;
import org.apache.ibatis.annotations.Param;

public interface SupportMapAreasMapper extends BaseMapper<SupportMapAreas> {

  /**
   * 根据经纬度查询区域id
   *
   * @param lon 经度
   * @param lat 维度
   * @return
   */
  SupportMapAreas selectMapAreas(@Param("lon") String lon, @Param("lat") String lat);
}
