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

package cn.universal.persistence.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 首页统计数据 */
@Data
@Schema(description = "首页统计数据DTO")
public class IndexQueryDTO {

  @Schema(description = "应用数")
  private Integer apps;

  @Schema(description = "产品数")
  private Integer product;

  @Schema(description = "规则数")
  private Integer rule;

  @Schema(description = "设备总数")
  private Integer device;

  @Schema(description = "在线数")
  private Integer online;

  @Schema(description = "离线数")
  private Integer offline;
}
