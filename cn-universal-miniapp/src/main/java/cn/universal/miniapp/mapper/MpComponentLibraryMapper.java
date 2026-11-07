/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.mapper;

import cn.universal.miniapp.entity.MpComponentLibrary;
import cn.universal.persistence.common.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 装修组件库Mapper
 *
 * @author NexIoT
 * @since 2025/12/15
 */
public interface MpComponentLibraryMapper extends BaseMapper<MpComponentLibrary> {

  /** 查询启用的组件列表 */
  @Select("SELECT * FROM mp_component_library WHERE status = 1 ORDER BY sort_order ASC")
  List<MpComponentLibrary> selectEnabledComponents();

  /** 根据类型查询组件 */
  @Select("SELECT * FROM mp_component_library WHERE component_type = #{type} AND status = 1")
  List<MpComponentLibrary> selectByType(@Param("type") String type);
}
