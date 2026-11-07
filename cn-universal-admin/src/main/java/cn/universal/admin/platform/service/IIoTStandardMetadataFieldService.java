/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 标准物模型字段服务接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service;

import cn.universal.persistence.entity.IoTStandardMetadataField;
import java.util.List;

/**
 * 标准物模型字段服务接口
 *
 * @since 2026-01-06
 */
public interface IIoTStandardMetadataFieldService {

  /**
   * 查询字段列表
   *
   * @param field 查询条件
   * @return 字段列表
   */
  List<IoTStandardMetadataField> selectFieldList(IoTStandardMetadataField field);

  /**
   * 根据字段类型查询字段列表
   *
   * @param fieldType 字段类型
   * @return 字段列表
   */
  List<IoTStandardMetadataField> selectFieldByType(String fieldType);

  /**
   * 根据ID查询字段
   *
   * @param id 字段ID
   * @return 字段信息
   */
  IoTStandardMetadataField selectFieldById(Long id);

  /**
   * 根据字段标识查询字段
   *
   * @param fieldId 字段标识
   * @return 字段信息
   */
  IoTStandardMetadataField selectFieldByFieldId(String fieldId);

  /**
   * 关键字搜索字段（用于联想功能）
   *
   * @param keyword 关键字
   * @param fieldType 字段类型
   * @param limit 返回数量限制
   * @return 字段列表
   */
  List<IoTStandardMetadataField> searchFieldByKeyword(String keyword, String fieldType, Integer limit);

  /**
   * 全文搜索字段
   *
   * @param keyword 关键字
   * @param fieldType 字段类型
   * @param limit 返回数量限制
   * @return 字段列表
   */
  List<IoTStandardMetadataField> fulltextSearch(String keyword, String fieldType, Integer limit);

  /**
   * 获取热门字段
   *
   * @param fieldType 字段类型
   * @param limit 返回数量限制
   * @return 字段列表
   */
  List<IoTStandardMetadataField> selectPopularFields(String fieldType, Integer limit);

  /**
   * 新增字段
   *
   * @param field 字段信息
   * @return 结果
   */
  int insertField(IoTStandardMetadataField field);

  /**
   * 修改字段
   *
   * @param field 字段信息
   * @return 结果
   */
  int updateField(IoTStandardMetadataField field);

  /**
   * 增加使用次数
   *
   * @param id 字段ID
   * @return 结果
   */
  int incrementUseCount(Long id);

  /**
   * 删除字段
   *
   * @param id 字段ID
   * @return 结果
   */
  int deleteFieldById(Long id);

  /**
   * 批量删除字段
   *
   * @param ids 字段ID数组
   * @return 结果
   */
  int deleteFieldByIds(Long[] ids);

  /**
   * 校验字段标识唯一性
   *
   * @param field 字段信息
   * @return 是否唯一
   */
  boolean checkFieldIdUnique(IoTStandardMetadataField field);

  /**
   * 根据分类获取推荐字段
   *
   * @param categoryId 分类ID
   * @param fieldType 字段类型
   * @param limit 返回数量限制
   * @return 字段列表
   */
  List<IoTStandardMetadataField> selectRecommendFieldsByCategory(Long categoryId, String fieldType, Integer limit);
}
