/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.mapper;

import cn.universal.miniapp.dto.MpDeviceQueryDTO;
import cn.universal.miniapp.vo.MpDeviceVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 小程序设备 Mapper 专用于小程序端设备查询，关联产品表获取完整信息
 *
 * @author NexIoT
 * @since 2025/12/17
 */
@Mapper
public interface MpDeviceMapper {

  /**
   * 根据应用ID查询设备列表 关联产品表获取产品图片、设备类型等信息
   *
   * @param linkedAppId IoT应用ID
   * @return 设备列表
   */
  List<MpDeviceVO> selectDeviceListByAppId(@Param("linkedAppId") String linkedAppId);

  /**
   * 根据应用ID和查询条件查询设备列表 支持设备名称、设备ID模糊搜索，以及设备类型、标签、分组、分类筛选
   *
   * @param queryDTO 查询条件
   * @return 设备列表
   */
  List<MpDeviceVO> selectDeviceListByQuery(@Param("query") MpDeviceQueryDTO queryDTO);

  /**
   * 根据iotId查询设备详情
   *
   * @param iotId 设备ID
   * @return 设备信息
   */
  MpDeviceVO selectDeviceByIotId(@Param("iotId") String iotId);

  /**
   * 根据productKey和deviceId查询子设备列表（网关的子设备）
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 子设备列表
   */
  List<MpDeviceVO> selectSubDevicesByGateway(
      @Param("productKey") String productKey, @Param("deviceId") String deviceId);

  /**
   * 统计应用下设备数量
   *
   * @param linkedAppId IoT应用ID
   * @return 设备数量
   */
  int countDevicesByAppId(@Param("linkedAppId") String linkedAppId);

  /**
   * 统计应用下在线设备数量
   *
   * @param linkedAppId IoT应用ID
   * @return 在线设备数量
   */
  int countOnlineDevicesByAppId(@Param("linkedAppId") String linkedAppId);
}
