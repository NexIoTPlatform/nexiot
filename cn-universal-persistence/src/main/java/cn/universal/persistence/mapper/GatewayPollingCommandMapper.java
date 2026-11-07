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

import cn.universal.persistence.entity.GatewayPollingCommand;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

/**
 * 网关轮询指令 Mapper
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
public interface GatewayPollingCommandMapper extends Mapper<GatewayPollingCommand> {

  /**
   * 根据网关设备查询指令列表
   *
   * @param gatewayProductKey 网关产品KEY
   * @param gatewayDeviceId 网关设备ID
   * @return 指令列表(按执行顺序排序)
   */
  List<GatewayPollingCommand> selectByGateway(
      @Param("gatewayProductKey") String gatewayProductKey,
      @Param("gatewayDeviceId") String gatewayDeviceId,
      @Param("taskType") String taskType);

  /**
   * 根据网关设备查询全部指令列表，包含禁用指令，用于配置页回显。
   *
   * @param gatewayProductKey 网关产品KEY
   * @param gatewayDeviceId 网关设备ID
   * @param taskType 任务类型
   * @return 指令列表(按执行顺序排序)
   */
  List<GatewayPollingCommand> selectAllByGateway(
      @Param("gatewayProductKey") String gatewayProductKey,
      @Param("gatewayDeviceId") String gatewayDeviceId,
      @Param("taskType") String taskType);

  /**
   * 根据网关设备删除指令
   *
   * @param gatewayProductKey 网关产品KEY
   * @param gatewayDeviceId 网关设备ID
   * @return 删除行数
   */
  int deleteByGateway(
      @Param("gatewayProductKey") String gatewayProductKey,
      @Param("gatewayDeviceId") String gatewayDeviceId,
      @Param("taskType") String taskType);

  /**
   * 批量插入指令
   *
   * @param commands 指令列表
   * @return 插入行数
   */
  int batchInsert(@Param("commands") List<GatewayPollingCommand> commands);
}
