/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA标签Mapper
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTOtaTag;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTOtaTagMapper extends BaseMapper<IoTOtaTag> {

  List<IoTOtaTag> selectTagList(IoTOtaTag query);

  IoTOtaTag selectTagById(@Param("id") Long id);

  int insertTag(IoTOtaTag tag);

  int updateTag(IoTOtaTag tag);

  int deleteTagById(@Param("id") Long id);
}
