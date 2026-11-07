/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.platform.service;

import cn.universal.common.constant.IoTConstant.TcpFlushType;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceFunction;
import cn.universal.persistence.entity.IoTDeviceProperties;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.bo.IoTProductBO;
import cn.universal.persistence.entity.bo.IoTProductKeyUpdateBO;
import cn.universal.persistence.entity.vo.IoTDeviceModelVO;
import cn.universal.persistence.entity.vo.IoTProductExportVO;
import cn.universal.persistence.entity.vo.IoTProductOptionVO;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.persistence.query.IoTProductQuery;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设备产品Service接口
 *
 * @since 2025-12-24
 */
public interface IIoTProductService {

  /**
   * 查询设备产品
   *
   * @param id 设备产品主键
   * @return 设备产品
   */
  IoTProductVO selectDevProductById(String id);

  /**
   * 查询设备产品列表
   *
   * @param ioTProduct 设备产品
   * @return 设备产品集合
   */
  List<IoTProduct> selectDevProductList(IoTProduct ioTProduct);

  /**
   * 查询设备产品列表
   *
   * @param ioTProductQuery 设备产品QU
   * @return 设备产品集合
   */
  List<IoTProduct> selectDevProductV2List(IoTProductQuery ioTProductQuery);

  /**
   * 查询设备产品列表V3,单表
   *
   * @param ioTProductQuery 设备产品QU
   * @return 设备产品集合
   */
  List<IoTProductVO> selectDevProductV3List(IoTProductQuery ioTProductQuery);

  /**
   * 查询设备产品列表V4,单表
   *
   * @param ioTProductQuery 设备产品QU
   * @return 设备产品集合
   */
  List<IoTProductVO> selectDevProductV4List(IoTProductQuery ioTProductQuery);

  /** 分页搜索当前用户可见的产品选项，仅返回标识与名称。 */
  List<IoTProductOptionVO> selectProductOptions(IoTProductQuery query);

  /** 查询当前已启用的产品日志存储策略 */
  Set<String> getEnabledStorePolicies();

  /**
   * 搜索当前条件下产品的所有字段
   *
   * @param ioTProductQuery 设备产品QU
   * @return 设备产品集合
   */
  List<IoTProductExportVO> selectAllDevProductV2List(IoTProductQuery ioTProductQuery);

  /**
   * 新增设备产品
   *
   * @param ioTProduct 设备产品
   * @return 结果
   */
  int insertDevProduct(IoTProduct ioTProduct);

  /**
   * 新增设备产品
   *
   * @param ioTProductList 设备产品列表
   * @return 结果
   */
  int insertList(List<IoTProduct> ioTProductList);

  /** 导出产品完整包（包含协议、网络配置、物模型） */
  List<cn.universal.persistence.entity.dto.ProductExportPackageDTO> exportProductPackages(
      IoTProductQuery query);

  /** 导入产品完整包 */
  cn.universal.persistence.entity.dto.ProductImportResultDTO importProductPackages(
      List<cn.universal.persistence.entity.dto.ProductExportPackageDTO> packages,
      String unionId,
      String tenantId);

  /**
   * 修改设备产品
   *
   * @param ioTProduct 设备产品
   * @return 结果
   */
  int updateDevProduct(IoTProduct ioTProduct);

  /**
   * 修改产品 ProductKey
   *
   * @param updateBO 修改请求
   * @return 结果
   */
  int updateProductKey(IoTProductKeyUpdateBO updateBO);

  /**
   * 修改网络信息
   *
   * @param id
   * @param networkUnionId
   * @return
   */
  int updateNetworkUnionId(Long id, String networkUnionId);

  /**
   * 批量删除设备产品
   *
   * @param ids 需要删除的设备产品主键集合
   * @return 结果
   */
  int deleteDevProductByIds(String[] ids);

  /**
   * 删除设备产品信息
   *
   * @param id 设备产品主键
   * @return 结果
   */
  int deleteDevProductById(String id);

  /** 修改产品配置信息 */
  int updateDevProductConfig(IoTProductVO devProduct);

  /**
   * 查询设备产品物模型属性列表
   *
   * @return 设备产品物模型属性集合
   */
  List<IoTDeviceProperties> selectDevProperties(String Id);

  /**
   * 查询设备产品物模型事件列表
   *
   * @return 设备产品物模型事件集合
   */
  List<IoTDeviceEvents> selectDevEvents(String Id);

  /**
   * 查询设备产品物模型方法列表
   *
   * @return 设备产品物模型方法集合
   */
  List<IoTDeviceFunction> selectDevFunctions(String Id);

  /**
   * 根据产品key查model配置
   *
   * @param productKey 产品key
   * @return 结果
   */
  IoTDeviceModelVO getModelByProductKey(String productKey);

  int insertMetadata(IoTProductBO ioTProductBO);

  int insertMetadataV2(IoTProductBO ioTProductBO);

  int deleteMetadata(IoTProductBO ioTProductBO);

  int updateMetadata(IoTProductBO ioTProductBO);

  IoTProductBO getMetadata(IoTProductBO ioTProductBO);

  String getMetadataProductKey(String productKey);

  Map<String, Integer> countDevNumberByProductKey(String unionId);

  AjaxResult<IoTProduct> selectIoTProductByKey(String key);

  AjaxResult<List<IoTProduct>> selectGatewaySubProductsByKey(String gwProductKey);

  int updateDevProductOtherConfig(String otherConfig);

  int updateDevProductStoreConfig(IoTProductVO storeConfig);

  void flushNettyServer(String config, String productKey, TcpFlushType type);

  List<IoTProductVO> selectDevProductAllList();

  List<IoTProductVO> getGatewayList();

  List<IoTProductVO> getGatewaySubDeviceList(String productKey);

  /** 绑定证书到产品（将sslKey和ssl=true写入configuration） */
  int bindCertificate(String productKey, String sslKey);

  /** 解绑证书（将ssl=false, sslKey设空） */
  int unbindCertificate(String productKey);

  /**
   * 批量更新物模型
   * 支持从公共物模型初始化、批量导入等场景
   * 用Java代码处理，不使用SQL的JSON函数
   *
   * @param batchBO 批量更新请求对象
   * @return 操作结果
   */
  int batchUpdateMetadata(cn.universal.persistence.entity.bo.IoTProductMetadataBatchBO batchBO);
}
