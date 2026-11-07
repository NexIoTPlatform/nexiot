/*
 *
 * Copyright (c) 2025, IoT-Universal. All Rights Reserved.
 *
 * @Description: 本文件由 Aleo 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: Aleo
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.persistence.mapper;

import cn.universal.persistence.entity.GatewayPollingConfig;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

/**
 * 网关轮询配置 Mapper
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
public interface GatewayPollingConfigMapper extends Mapper<GatewayPollingConfig> {

  /**
   * 查询待轮询的网关设备
   *
   * @param intervalSeconds 轮询间隔
   * @return 待轮询设备列表
   */
  List<GatewayPollingConfig> selectDuePolling(
      @Param("intervalSeconds") Integer intervalSeconds, @Param("taskType") String taskType);

  /**
   * 根据设备ID和产品KEY查询轮询配置
   *
   * @param productKey 产品KEY
   * @param deviceId 设备ID
   * @return 轮询配置
   */
  GatewayPollingConfig selectByDevice(
      @Param("productKey") String productKey,
      @Param("deviceId") String deviceId,
      @Param("taskType") String taskType);

  /**
   * 更新轮询成功状态
   *
   * @param id 配置ID
   * @param nextPollTime 下次轮询时间
   * @return 更新行数
   */
  int updatePollingSuccess(@Param("id") Long id, @Param("nextPollTime") Date nextPollTime);

  /**
   * 更新轮询失败状态
   *
   * @param id 配置ID
   * @return 更新行数
   */
  int updatePollingFail(@Param("id") Long id, @Param("nextPollTime") Date nextPollTime);

  /**
   * 重置失败计数
   *
   * @param id 配置ID
   * @return 更新行数
   */
  int resetFailCount(@Param("id") Long id);
}
