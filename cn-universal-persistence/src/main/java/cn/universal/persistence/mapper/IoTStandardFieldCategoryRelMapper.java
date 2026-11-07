/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 标准字段与分类关联Mapper接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTStandardFieldCategoryRel;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 标准字段与分类关联Mapper接口
 *
 * @since 2026-01-06
 */
public interface IoTStandardFieldCategoryRelMapper extends BaseMapper<IoTStandardFieldCategoryRel> {

  /**
   * 根据字段ID查询关联的分类ID列表
   *
   * @param fieldId 字段ID
   * @return 分类ID列表
   */
  List<Long> selectCategoryIdsByFieldId(@Param("fieldId") Long fieldId);

  /**
   * 批量插入字段分类关联
   *
   * @param relations 关联关系列表
   * @return 结果
   */
  int batchInsert(@Param("relations") List<IoTStandardFieldCategoryRel> relations);

  /**
   * 删除字段的所有分类关联
   *
   * @param fieldId 字段ID
   * @return 结果
   */
  int deleteByFieldId(@Param("fieldId") Long fieldId);

  /**
   * 删除分类的所有字段关联
   *
   * @param categoryId 分类ID
   * @return 结果
   */
  int deleteByCategoryId(@Param("categoryId") Long categoryId);
}
