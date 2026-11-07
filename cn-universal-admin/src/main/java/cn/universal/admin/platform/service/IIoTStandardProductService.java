/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型产品服务接口
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.admin.platform.service;

import cn.universal.persistence.entity.IoTStandardProduct;
import java.util.List;

/**
 * 公共物模型产品服务接口
 *
 * @since 2026-01-06
 */
public interface IIoTStandardProductService {

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
  List<IoTStandardProduct> selectProductByCategoryId(Long categoryId);

  /**
   * 根据ID查询产品
   *
   * @param id 产品ID
   * @return 产品信息
   */
  IoTStandardProduct selectProductById(Long id);

  /**
   * 根据产品标识查询产品
   *
   * @param productKey 产品标识
   * @return 产品信息
   */
  IoTStandardProduct selectProductByKey(String productKey);

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
  int deleteProductById(Long id);

  /**
   * 批量删除产品
   *
   * @param ids 产品ID数组
   * @return 结果
   */
  int deleteProductByIds(Long[] ids);

  /**
   * 校验产品标识唯一性
   *
   * @param product 产品信息
   * @return 是否唯一
   */
  boolean checkProductKeyUnique(IoTStandardProduct product);

  /**
   * 关键字搜索产品
   *
   * @param keyword 关键字
   * @return 产品列表
   */
  List<IoTStandardProduct> searchProductByKeyword(String keyword);

  /**
   * 导入标准产品到用户产品（覆盖模式）
   *
   * @param standardProductId 标准产品ID
   * @param targetProductId 目标产品ID
   * @param operator 操作人
   * @return 结果
   */
  int importStandardProduct(Long standardProductId, Long targetProductId, String operator);

  /**
   * 更新产品物模型
   *
   * @param id 产品ID
   * @param metadata 物模型JSON
   * @param operator 操作人
   * @return 结果
   */
  int updateProductMetadata(Long id, String metadata, String operator);

  /**
   * 设置/取消模板标记
   *
   * @param id 产品ID
   * @param isTemplate 是否为模板（0-否，1-是）
   * @return 结果
   */
  int setTemplate(Long id, Integer isTemplate);

  /**
   * 增加使用次数
   *
   * @param id 产品ID
   * @return 结果
   */
  int incrementUseCount(Long id);

  /**
   * 获取模板列表
   *
   * @return 模板列表
   */
  List<IoTStandardProduct> selectTemplateList();

  /**
   * 获取热门产品（按使用次数排序）
   *
   * @param limit 返回数量限制
   * @return 产品列表
   */
  List<IoTStandardProduct> selectPopularProducts(Integer limit);
}
