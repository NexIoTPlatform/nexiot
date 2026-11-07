/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台设备主表Mapper
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.query.VideoPlatformDeviceQuery;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 视频平台设备主表Mapper
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/11/08
 */
public interface VideoPlatformDeviceMapper extends BaseMapper<VideoPlatformDevice> {
  
  /**
   * 根据查询条件查询设备列表
   * 
   * @param query 查询条件
   * @return 设备列表
   */
  List<VideoPlatformDevice> selectDevicesByQuery(VideoPlatformDeviceQuery query);

  /**
   * 批量更新指定实例的所有设备启用状态
   *
   * @param instanceKey 实例Key
   * @param enabled 启用状态(0-禁用, 1-启用)
   * @return 影响行数
   */
  int batchUpdateEnabledByInstanceKey(
      @Param("instanceKey") String instanceKey, @Param("enabled") Integer enabled);
}
