/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型分类Mapper接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTStandardCategory;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 公共物模型分类Mapper接口
 *
 * @since 2026-01-06
 */
public interface IoTStandardCategoryMapper extends BaseMapper<IoTStandardCategory> {

  /**
   * 查询分类列表
   *
   * @param category 查询条件
   * @return 分类列表
   */
  List<IoTStandardCategory> selectCategoryList(IoTStandardCategory category);

  /**
   * 根据ID查询分类
   *
   * @param id 分类ID
   * @return 分类信息
   */
  IoTStandardCategory selectCategoryById(@Param("id") Long id, @Param("tenantId") String tenantId);

  /**
   * 根据分类标识查询分类
   *
   * @param categoryKey 分类标识
   * @return 分类信息
   */
  IoTStandardCategory selectCategoryByKey(
      @Param("categoryKey") String categoryKey, @Param("tenantId") String tenantId);

  /**
   * 查询子分类列表
   *
   * @param parentId 父分类ID
   * @return 子分类列表
   */
  List<IoTStandardCategory> selectChildrenByParentId(
      @Param("parentId") Long parentId, @Param("tenantId") String tenantId);

  /**
   * 查询指定层级的分类列表
   *
   * @param level 层级
   * @return 分类列表
   */
  List<IoTStandardCategory> selectCategoryByLevel(
      @Param("level") Integer level, @Param("tenantId") String tenantId);

  /**
   * 新增分类
   *
   * @param category 分类信息
   * @return 结果
   */
  int insertCategory(IoTStandardCategory category);

  /**
   * 修改分类
   *
   * @param category 分类信息
   * @return 结果
   */
  int updateCategory(IoTStandardCategory category);

  /**
   * 删除分类
   *
   * @param id 分类ID
   * @return 结果
   */
  int deleteCategoryById(@Param("id") Long id, @Param("tenantId") String tenantId);

  /**
   * 批量删除分类
   *
   * @param ids 分类ID数组
   * @return 结果
   */
  int deleteCategoryByIds(@Param("ids") Long[] ids, @Param("tenantId") String tenantId);

  /**
   * 统计子分类数量
   *
   * @param parentId 父分类ID
   * @return 子分类数量
   */
  int countChildrenByParentId(@Param("parentId") Long parentId, @Param("tenantId") String tenantId);

  /**
   * 校验分类标识唯一性
   *
   * @param categoryKey 分类标识
   * @param excludeId 排除的ID
   * @return 数量
   */
  int checkCategoryKeyUnique(
      @Param("categoryKey") String categoryKey,
      @Param("excludeId") Long excludeId,
      @Param("tenantId") String tenantId);
}
