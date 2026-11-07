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
import cn.universal.persistence.entity.IoTAlgorithmResult;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 算法结果Mapper接口
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/2/27
 */
public interface IoTAlgorithmResultMapper extends BaseMapper<IoTAlgorithmResult> {

  /**
   * 根据设备ID查询算法结果列表
   *
   * @param deviceId 设备ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 算法结果列表
   */
  List<IoTAlgorithmResult> selectByDeviceId(
      @Param("deviceId") String deviceId,
      @Param("startTime") Date startTime,
      @Param("endTime") Date endTime);

  /**
   * 根据设备ID和算法类型查询算法结果列表
   *
   * @param deviceId 设备ID
   * @param algorithmType 算法类型
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 算法结果列表
   */
  List<IoTAlgorithmResult> selectByDeviceIdAndType(
      @Param("deviceId") String deviceId,
      @Param("algorithmType") String algorithmType,
      @Param("startTime") Date startTime,
      @Param("endTime") Date endTime);

  /**
   * 根据产品Key查询算法结果列表
   *
   * @param productKey 产品Key
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 算法结果列表
   */
  List<IoTAlgorithmResult> selectByProductKey(
      @Param("productKey") String productKey,
      @Param("startTime") Date startTime,
      @Param("endTime") Date endTime);

  /**
   * 查询设备最新的算法结果
   *
   * @param deviceId 设备ID
   * @param algorithmType 算法类型
   * @return 算法结果
   */
  IoTAlgorithmResult selectLatestByDeviceIdAndType(
      @Param("deviceId") String deviceId, @Param("algorithmType") String algorithmType);

  /**
   * 查询异常结果列表
   *
   * @param productKey 产品Key
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 算法结果列表
   */
  List<IoTAlgorithmResult> selectAnomalyResults(
      @Param("productKey") String productKey,
      @Param("startTime") Date startTime,
      @Param("endTime") Date endTime);

  /**
   * 删除过期的算法结果
   *
   * @param beforeTime 时间点
   * @return 删除行数
   */
  int deleteExpiredResults(@Param("beforeTime") Date beforeTime);
}
