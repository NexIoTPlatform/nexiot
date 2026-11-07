/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 标准字段与分类关联服务接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service;

import java.util.List;

/**
 * 标准字段与分类关联服务接口
 *
 * @since 2026-01-06
 */
public interface IIoTStandardFieldCategoryRelService {

  /**
   * 设置字段与分类的关联
   *
   * @param fieldId 字段ID
   * @param categoryIds 分类ID列表
   * @return 结果
   */
  int setFieldCategories(Long fieldId, List<Long> categoryIds);

  /**
   * 获取字段关联的分类ID列表
   *
   * @param fieldId 字段ID
   * @return 分类ID列表
   */
  List<Long> getFieldCategories(Long fieldId);

  /**
   * 删除字段的所有分类关联
   *
   * @param fieldId 字段ID
   * @return 结果
   */
  int deleteByFieldId(Long fieldId);

  /**
   * 删除分类的所有字段关联
   *
   * @param categoryId 分类ID
   * @return 结果
   */
  int deleteByCategoryId(Long categoryId);
}
