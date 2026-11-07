/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA升级包Mapper
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTOtaPackage;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTOtaPackageMapper extends BaseMapper<IoTOtaPackage> {

  List<IoTOtaPackage> selectPackageList(IoTOtaPackage query);

  IoTOtaPackage selectPackageById(@Param("id") Long id);

  List<IoTOtaPackage> selectRecentPackagesByProductKey(@Param("productKey") String productKey);

  int countTaskByPackageId(@Param("packageId") Long packageId);

  int insertPackage(IoTOtaPackage otaPackage);

  int updatePackage(IoTOtaPackage otaPackage);

  int deletePackageById(@Param("id") Long id);
}
