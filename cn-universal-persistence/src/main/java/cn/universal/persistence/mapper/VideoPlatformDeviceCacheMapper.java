/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.VideoPlatformDeviceCache;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface VideoPlatformDeviceCacheMapper extends BaseMapper<VideoPlatformDeviceCache> {
  List<VideoPlatformDeviceCache> selectByInstanceKey(@Param("instanceKey") String instanceKey);

  VideoPlatformDeviceCache selectOneByInstanceAndDevice(
      @Param("instanceKey") String instanceKey, @Param("deviceId") String deviceId);
}
