/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 标准物模型字段Mapper接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTStandardMetadataField;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 标准物模型字段Mapper接口
 *
 * @since 2026-01-06
 */
public interface IoTStandardMetadataFieldMapper extends BaseMapper<IoTStandardMetadataField> {

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
  List<IoTStandardMetadataField> selectFieldByType(
      @Param("fieldType") String fieldType, @Param("tenantId") String tenantId);

  /**
   * 根据ID查询字段
   *
   * @param id 字段ID
   * @return 字段信息
   */
  IoTStandardMetadataField selectFieldById(@Param("id") Long id, @Param("tenantId") String tenantId);

  /**
   * 根据字段标识查询字段
   *
   * @param fieldId 字段标识
   * @return 字段信息
   */
  IoTStandardMetadataField selectFieldByFieldId(
      @Param("fieldId") String fieldId, @Param("tenantId") String tenantId);

  /**
   * 根据字段类型和标识查询字段
   *
   * @param fieldType 字段类型
   * @param fieldId 字段标识
   * @return 字段信息
   */
  IoTStandardMetadataField selectFieldByTypeAndId(
      @Param("fieldType") String fieldType,
      @Param("fieldId") String fieldId,
      @Param("tenantId") String tenantId);

  /**
   * 关键字搜索字段（用于联想功能）
   *
   * @param fieldType 字段类型
   * @param keyword 关键字
   * @param limit 返回数量限制
   * @return 字段列表
   */
  List<IoTStandardMetadataField> searchFieldByKeyword(
      @Param("fieldType") String fieldType,
      @Param("keyword") String keyword,
      @Param("limit") Integer limit,
      @Param("tenantId") String tenantId);

  /**
   * 全文搜索字段
   *
   * @param fieldType 字段类型
   * @param keyword 关键字
   * @return 字段列表
   */
  List<IoTStandardMetadataField> fullTextSearchField(
      @Param("fieldType") String fieldType,
      @Param("keyword") String keyword,
      @Param("tenantId") String tenantId);

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
  int incrementUseCount(@Param("id") Long id, @Param("tenantId") String tenantId);

  /**
   * 删除字段
   *
   * @param id 字段ID
   * @return 结果
   */
  int deleteFieldById(@Param("id") Long id, @Param("tenantId") String tenantId);

  /**
   * 批量删除字段
   *
   * @param ids 字段ID数组
   * @return 结果
   */
  int deleteFieldByIds(@Param("ids") Long[] ids, @Param("tenantId") String tenantId);

  /**
   * 校验字段唯一性
   *
   * @param fieldType 字段类型
   * @param fieldId 字段标识
   * @param excludeId 排除的ID
   * @return 数量
   */
  int checkFieldUnique(
      @Param("fieldType") String fieldType,
      @Param("fieldId") String fieldId,
      @Param("excludeId") Long excludeId,
      @Param("tenantId") String tenantId);

  /**
   * 获取热门字段（按使用次数排序）
   *
   * @param fieldType 字段类型
   * @param limit 返回数量限制
   * @return 字段列表
   */
  List<IoTStandardMetadataField> selectPopularFields(
      @Param("fieldType") String fieldType,
      @Param("limit") Integer limit,
      @Param("tenantId") String tenantId);

  /**
   * 根据分类获取推荐字段
   *
   * @param categoryId 分类ID
   * @param fieldType 字段类型
   * @param limit 返回数量限制
   * @return 字段列表
   */
  List<IoTStandardMetadataField> selectRecommendFieldsByCategory(
      @Param("categoryId") Long categoryId,
      @Param("fieldType") String fieldType,
      @Param("limit") Integer limit,
      @Param("tenantId") String tenantId);
}
