/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA升级包标签关系Mapper
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTOtaPackageTagRel;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTOtaPackageTagRelMapper extends BaseMapper<IoTOtaPackageTagRel> {

  int deleteByPackageId(@Param("packageId") Long packageId);

  int deleteByTagId(@Param("tagId") Long tagId);

  int insertBatch(
      @Param("packageId") Long packageId, @Param("tagIds") List<Long> tagIds);

  List<Long> selectTagIdsByPackageId(@Param("packageId") Long packageId);
}
