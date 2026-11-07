/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型分类服务接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service;

import cn.universal.persistence.entity.IoTStandardCategory;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 公共物模型分类服务接口
 *
 * @since 2026-01-06
 */
public interface IIoTStandardCategoryService {

  /**
   * 查询分类列表
   *
   * @param category 查询条件
   * @return 分类列表
   */
  List<IoTStandardCategory> selectCategoryList(IoTStandardCategory category);

  /**
   * 查询分类树结构
   *
   * @return 分类树
   */
  List<IoTStandardCategory> selectCategoryTree();

  /**
   * 根据ID查询分类
   *
   * @param id 分类ID
   * @return 分类信息
   */
  IoTStandardCategory selectCategoryById(Long id);

  /**
   * 根据分类标识查询分类
   *
   * @param categoryKey 分类标识
   * @return 分类信息
   */
  IoTStandardCategory selectCategoryByKey(String categoryKey);

  /**
   * 查询子分类列表
   *
   * @param parentId 父分类ID
   * @return 子分类列表
   */
  List<IoTStandardCategory> selectChildrenByParentId(Long parentId);

  /**
   * 查询第三级分类列表（用于产品选择）
   *
   * @return 第三级分类列表
   */
  List<IoTStandardCategory> selectThirdLevelCategories();

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
  int deleteCategoryById(Long id);

  /**
   * 批量删除分类
   *
   * @param ids 分类ID数组
   * @return 结果
   */
  int deleteCategoryByIds(Long[] ids);

  /**
   * 校验分类标识唯一性
   *
   * @param category 分类信息
   * @return 是否唯一
   */
  boolean checkCategoryKeyUnique(IoTStandardCategory category);

  /**
   * 判断是否有子分类
   *
   * @param id 分类ID
   * @return 是否有子分类
   */
  boolean hasChildren(Long id);

  /**
   * 获取分类路径
   *
   * @param categoryId 分类ID
   * @return 分类路径（如：智能城市/公共事业/路灯照明）
   */
  String getCategoryPath(Long categoryId);

  /** 批量获取当前租户可见的分类路径，避免列表逐条查询祖先分类。 */
  Map<Long, String> getCategoryPaths(Collection<Long> categoryIds);
}
