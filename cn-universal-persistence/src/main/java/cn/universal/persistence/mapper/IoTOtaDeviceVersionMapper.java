/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA设备版本快照Mapper
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTOtaDeviceVersion;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface IoTOtaDeviceVersionMapper extends BaseMapper<IoTOtaDeviceVersion> {

  IoTOtaDeviceVersion selectVersionByDevice(
      @Param("productKey") String productKey,
      @Param("deviceId") String deviceId,
      @Param("packageType") String packageType,
      @Param("module") String module);

  IoTOtaDeviceVersion selectLatestVersionByDevice(
      @Param("productKey") String productKey, @Param("deviceId") String deviceId);

  List<IoTOtaDeviceVersion> selectVersionListByProductKey(@Param("productKey") String productKey);

  List<Map<String, Object>> selectVersionDistributionByProductKey(
      @Param("productKey") String productKey);

  int upsertVersion(IoTOtaDeviceVersion version);
}
