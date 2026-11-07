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

package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.bo.IoTProductBO;
import cn.universal.persistence.entity.vo.IoTDeviceModelVO;
import cn.universal.persistence.entity.vo.IoTProductExportVO;
import cn.universal.persistence.entity.vo.IoTProductOptionVO;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.query.IoTAPIQuery;
import cn.universal.persistence.query.IoTProductQuery;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.springframework.cache.annotation.Cacheable;

public interface IoTProductMapper extends BaseMapper<IoTProduct> {

  List<IoTProductVO> openAPIProductList(IoTAPIQuery iotAPIQuery);

  IoTProductVO apiProductDetail(String productKey);

  /**
   * 查询设备产品
   *
   * @param id 设备产品主键
   * @return 设备产品
   */
  public IoTProduct selectDevProductById(String id);

  /**
   * 查询设备产品列表
   *
   * @param ioTProduct 设备产品
   * @return 设备产品集合
   */
  public List<IoTProduct> selectDevProductList(IoTProduct ioTProduct);

  /**
   * 查询设备产品列表
   *
   * @param ioTProductQuery 设备产品
   * @return 设备产品集合
   */
  public List<IoTProduct> selectDevProductV2List(IoTProductQuery ioTProductQuery);

  public List<IoTProductVO> selectDevProductV3List(IoTProductQuery ioTProductQuery);

  List<IoTProductOptionVO> selectProductOptions(IoTProductQuery query);

  public List<IoTProductExportVO> selectAllDevProductV2List(IoTProductQuery ioTProductQuery);

  /**
   * 新增设备产品
   *
   * @param ioTProduct 设备产品
   * @return 结果
   */
  public int insertDevProduct(IoTProduct ioTProduct);

  /**
   * 修改设备产品
   *
   * @param ioTProduct 设备产品
   * @return 结果
   */
  public int updateDevProduct(IoTProduct ioTProduct);

  /**
   * 删除设备产品
   *
   * @param id 设备产品主键
   * @return 结果
   */
  public int deleteDevProductById(String id);

  /**
   * 批量删除设备产品
   *
   * @param ids 需要删除的数据主键集合
   * @return 结果
   */
  public int deleteDevProductByIds(String[] ids);

  /**
   * 根据产品key查产品
   *
   * @param productKey 产品key
   * @return 结果
   */
  IoTProduct getProductByProductKey(String productKey);

  /** 在当前事务中锁定产品配置，避免 OTA 适配配置并发更新丢失。 */
  IoTProduct selectProductForUpdate(@Param("productKey") String productKey);

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

  String getMetadataProductKey(@Param("productKey") String productKey);

  List<IoTProductVO> countDevNumberByProductKey(@Param("unionId") String unionId);

  List<IoTProductVO> selectDevProductAllList();

  List<IoTProductVO> selectProductName(String[] productKey);

  List<IoTProductVO> getGatewaySubDeviceList(String productKey);

  String selectNetworkUnionId(String productKey);

  @Cacheable("selectAllEnableNetworkProductKey")
  List<String> selectAllEnableNetworkProductKey();

  /** 统计引用指定network_union_id的产品数量 */
  int countByNetworkUnionId(String networkUnionId);

  /** 模糊搜索productKey和name */
  List<Map<String, String>> searchProductKeyAndName(IoTProductQuery query);

  /** 根据productKey更新network_union_id */
  int updateNetworkUnionIdByProductKey(String productKey, String networkUnionId);

  /** 根据network_union_id查找productKey */
  String findProductKeyByNetworkUnionId(String networkUnionId);

  /** 查询mqtt绑定使用networkUnionId的产品列表* */
  List<IoTProductBO> selectMqttProductsUseNetwork(String networkUnionId);

  /** 查询tcp绑定使用networkUnionId的产品* */
  IoTProductBO selectTcpProductsUseNetwork(String productKey);

  int updateNetworkProductsUseNetwork(@Param("networkUnionId") String networkUnionId);

  /**
   * 根据产品Key列表批量查询产品信息
   *
   * @param productKeys 产品Key列表
   * @return 产品信息列表
   */
  List<IoTProductBO> selectProductNameByProductKeys(@Param("productKeys") List<String> productKeys);

  IoTProductBO selectTransportProtocol(@Param("productKey") String productKey);

  /**
   * 统计全部产品数量（管理员）
   *
   * @return 产品总数
   */
  long countAllProducts();

  /**
   * 统计指定创建者的产品数量（普通用户）
   *
   * @param creatorId 创建者ID
   * @return 产品数量
   */
  long countProductsByCreator(@Param("creatorId") String creatorId);

  /** 按统一部门数据权限范围统计产品数量 */
  long countProductsByScope(
      @Param("tenantId") String tenantId,
      @Param("scopeUnionIds") List<String> scopeUnionIds,
      @Param("scopeDenied") boolean scopeDenied);

  /** 统计指定用户自己创建的产品数量 */
  long countProductsCreatedByUser(@Param("creatorId") String creatorId);
}
