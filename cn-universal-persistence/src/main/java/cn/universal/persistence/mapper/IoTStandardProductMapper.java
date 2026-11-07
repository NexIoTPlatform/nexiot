/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型产品Mapper接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTStandardProduct;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 公共物模型产品Mapper接口
 *
 * @since 2026-01-06
 */
public interface IoTStandardProductMapper extends BaseMapper<IoTStandardProduct> {

  /**
   * 查询产品列表
   *
   * @param product 查询条件
   * @return 产品列表
   */
  List<IoTStandardProduct> selectProductList(IoTStandardProduct product);

  /**
   * 根据分类ID查询产品列表
   *
   * @param categoryId 分类ID
   * @return 产品列表
   */
  List<IoTStandardProduct> selectProductByCategoryId(
      @Param("categoryId") Long categoryId, @Param("tenantId") String tenantId);

  /**
   * 根据ID查询产品
   *
   * @param id 产品ID
   * @return 产品信息
   */
  IoTStandardProduct selectProductById(@Param("id") Long id, @Param("tenantId") String tenantId);

  /**
   * 根据产品标识查询产品
   *
   * @param productKey 产品标识
   * @return 产品信息
   */
  IoTStandardProduct selectProductByKey(
      @Param("productKey") String productKey, @Param("tenantId") String tenantId);

  /**
   * 新增产品
   *
   * @param product 产品信息
   * @return 结果
   */
  int insertProduct(IoTStandardProduct product);

  /**
   * 修改产品
   *
   * @param product 产品信息
   * @return 结果
   */
  int updateProduct(IoTStandardProduct product);

  /**
   * 删除产品
   *
   * @param id 产品ID
   * @return 结果
   */
  int deleteProductById(@Param("id") Long id, @Param("tenantId") String tenantId);

  /**
   * 批量删除产品
   *
   * @param ids 产品ID数组
   * @return 结果
   */
  int deleteProductByIds(@Param("ids") Long[] ids, @Param("tenantId") String tenantId);

  /**
   * 校验产品标识唯一性
   *
   * @param productKey 产品标识
   * @param excludeId 排除的ID
   * @return 数量
   */
  int checkProductKeyUnique(
      @Param("productKey") String productKey,
      @Param("excludeId") Long excludeId,
      @Param("tenantId") String tenantId);

  /**
   * 统计分类下的产品数量
   *
   * @param categoryId 分类ID
   * @return 产品数量
   */
  int countProductByCategoryId(@Param("categoryId") Long categoryId, @Param("tenantId") String tenantId);

  /**
   * 关键字搜索产品
   *
   * @param keyword 关键字
   * @return 产品列表
   */
  List<IoTStandardProduct> searchProductByKeyword(
      @Param("keyword") String keyword, @Param("tenantId") String tenantId);

  /**
   * 设置模板标记
   *
   * @param id 产品ID
   * @param isTemplate 是否为模板（0-否，1-是）
   * @return 结果
   */
  int updateTemplateStatus(
      @Param("id") Long id,
      @Param("isTemplate") Integer isTemplate,
      @Param("tenantId") String tenantId);

  /**
   * 增加使用次数
   *
   * @param id 产品ID
   * @return 结果
   */
  int incrementUseCount(@Param("id") Long id, @Param("tenantId") String tenantId);

  /**
   * 获取模板列表
   *
   * @return 模板列表
   */
  List<IoTStandardProduct> selectTemplateList(@Param("tenantId") String tenantId);

  /**
   * 获取热门产品（按使用次数排序）
   *
   * @param limit 返回数量限制
   * @return 产品列表
   */
  List<IoTStandardProduct> selectPopularProducts(
      @Param("limit") Integer limit, @Param("tenantId") String tenantId);
}
