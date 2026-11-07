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

package cn.universal.admin.platform.service;

import cn.universal.common.domain.R;
import cn.universal.persistence.dto.GatewayPollingConfigDTO;
import cn.universal.persistence.entity.GatewayPollingCommand;
import cn.universal.persistence.entity.GatewayPollingConfig;
import java.util.List;

/**
 * 网关定时任务服务接口
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
public interface IGatewayPollingService {

  /**
   * 保存或更新网关定时任务配置
   *
   * @param dto 配置DTO
   * @return 操作结果
   */
  R savePollingConfig(GatewayPollingConfigDTO dto);

  R savePollingConfig(String taskType, GatewayPollingConfigDTO dto);

  /**
   * 获取网关定时任务配置
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 配置信息
   */
  R getPollingConfig(String productKey, String deviceId);

  R getPollingConfig(String productKey, String deviceId, String taskType);

  /**
   * 删除网关定时任务配置
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 操作结果
   */
  R deletePollingConfig(String productKey, String deviceId);

  R deletePollingConfig(String productKey, String deviceId, String taskType);

  /**
   * 执行单个网关的轮询任务
   *
   * @param config 定时任务配置
   */
  void pollGatewayDevice(GatewayPollingConfig config);

  /**
   * 查询待轮询的网关设备列表
   *
   * @param intervalSeconds 执行间隔
   * @return 待轮询设备列表
   */
  List<GatewayPollingConfig> getDuePollingDevices(int intervalSeconds, String taskType);

  /**
   * 测试网关定时任务 - 立即执行一次任务
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 操作结果
   */
  R testPolling(String productKey, String deviceId);

  R testPolling(String productKey, String deviceId, String taskType);

  /**
   * 测试单条任务。
   *
   * @param productKey 网关产品Key
   * @param deviceId 网关设备ID
   * @param command 待测试指令
   * @return 测试结果
   */
  R testPollingCommand(String productKey, String deviceId, GatewayPollingCommand command);

  R testPollingCommand(
      String productKey, String deviceId, String taskType, GatewayPollingCommand command);
}
