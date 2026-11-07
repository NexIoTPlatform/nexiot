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

package cn.universal.persistence.dto;

import cn.universal.persistence.entity.GatewayPollingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关轮询配置 DTO
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "网关轮询配置DTO")
public class GatewayPollingConfigDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "配置ID")
  private Long id;

  @Schema(description = "网关设备ID", required = true)
  private String deviceId;

  @Schema(description = "产品KEY")
  private String productKey;

  @Schema(description = "IoT ID")
  private String iotId;

  @Schema(description = "任务类型: CLOUD_POLLING-云端轮询透传, MODBUS_COLLECT-Modbus采集")
  private String taskType;

  @Schema(description = "是否启用轮询")
  private Boolean enabled;

  @Schema(description = "轮询间隔(秒): 30/60/120/300/600", required = true)
  private Integer intervalSeconds;

  @Schema(description = "超时时间(秒)")
  private Integer timeoutSeconds;

  @Schema(description = "失败重试次数")
  private Integer retryTimes;

  @Schema(description = "指令间隔(毫秒): 500/800/1200/1800/2300")
  private Integer commandIntervalMs;

  @Schema(description = "轮询指令列表")
  private List<GatewayPollingCommand> commands;
}
