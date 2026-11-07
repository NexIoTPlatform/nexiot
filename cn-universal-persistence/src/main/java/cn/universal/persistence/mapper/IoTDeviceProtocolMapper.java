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
import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.entity.IoTProduct;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface IoTDeviceProtocolMapper extends BaseMapper<IoTDeviceProtocol> {

  @Select("SELECT id FROM iot_device_protocol WHERE id = #{id} FOR UPDATE")
  String lockForUpdate(@Param("id") String id);

  /**
   * 查询设备协议
   *
   * @param id 设备协议主键
   * @return 设备协议
   */
  public IoTDeviceProtocol selectDevProtocolById(
      @Param("id") String id,
      @Param("unionId") String unionId,
      @Param("tenantId") String tenantId);

  public int countByProvider(String provider);

  int insertProtocolList(@Param("list") List<IoTDeviceProtocol> ioTDeviceProtocols);

  public List<IoTDeviceProtocol> selectDevProtocolByIds(String[] ids);

  /**
   * 查询设备协议列表
   *
   * @param ioTDeviceProtocol 设备协议
   * @return 设备协议集合
   */
  public List<IoTDeviceProtocol> selectDevProtocolList(
      @Param("ioTDeviceProtocol") IoTDeviceProtocol ioTDeviceProtocol,
      @Param("unionId") String unionId);

  /**
   * 新增设备协议
   *
   * @param ioTDeviceProtocol 设备协议
   * @return 结果
   */
  public int insertDevProtocol(IoTDeviceProtocol ioTDeviceProtocol);

  /**
   * 修改设备协议
   *
   * @param ioTDeviceProtocol 设备协议
   * @return 结果
   */
  public int updateDevProtocol(IoTDeviceProtocol ioTDeviceProtocol);

  /**
   * 删除设备协议
   *
   * @param id 设备协议主键
   * @return 结果
   */
  public int deleteDevProtocolById(String id);

  /**
   * 批量删除设备协议
   *
   * @param ids 需要删除的数据主键集合
   * @return 结果
   */
  public int deleteDevProtocolByIds(String[] ids);

  IoTDeviceProtocol selectDevProtocolByProductKey(@Param("productKey") String productKey);

  /**
   * 查询未创建协议的产品列表（归属人为当前用户）
   *
   * @param unionId 用户unionId
   * @param searchKey 搜索关键词（产品名称或productKey）
   * @return 产品列表
   */
  List<IoTProduct> selectProductsWithoutProtocol(
      @Param("unionId") String unionId,
      @Param("tenantId") String tenantId,
      @Param("searchKey") String searchKey,
      @Param("scope") IoTProduct scope);
}
