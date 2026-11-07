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

package cn.universal.persistence.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备详情VO - V2接口返回完整字段
 * 用于SaaS层设备导入时获取完整设备信息
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2026/02/03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备详情VO（V2完整字段）")
public class IoTDeviceDetailVO {

  private static final long serialVersionUID = 1L;

  /** 对外设备唯一标识符 */
  @Schema(description = "IoT ID")
  private String iotId;

  /** 设备自身序号 */
  @Schema(description = "设备ID")
  private String deviceId;

  /** 产品key */
  @Schema(description = "产品Key")
  private String productKey;

  /** 设备名称 */
  @Schema(description = "产品名称")
  private String productName;

  /** 设备实例名称 */
  @Schema(description = "设备名称")
  private String deviceName;

  @Schema(description = "设备节点类型")
  private String deviceNode;

  @Schema(description = "网关产品Key")
  private String gwProductKey;

  @Schema(description = "设备状态(0-离线 1-在线)")
  private Integer state;

  // ========== V2新增字段 ==========

  /** 设备密钥 */
  @Schema(description = "设备密钥")
  private String deviceSecret;

  /** 第三方设备ID（也用作gwDeviceId） */
  @Schema(description = "第三方设备ID")
  private String extDeviceId;

  /** 设备配置（原始JSON） */
  @Schema(description = "设备配置")
  @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = cn.universal.persistence.common.serialization.PictureSecretConfigurationSerializer.class)
  private String configuration;

  /** 设备地址 */
  @Schema(description = "设备地址")
  private String deviceAddress;

  /** 经纬度坐标 */
  @Schema(description = "经纬度坐标")
  private String coordinate;

  /** 设备标签（JSON字符串） */
  @Schema(description = "设备标签")
  private String deviceTag;

  /** 创建时间（时间戳） */
  @Schema(description = "创建时间")
  private Long createTime;

  /** 激活时间（时间戳） */
  @Schema(description = "激活时间")
  private Long registryTime;

  /** 最后通信时间（时间戳） */
  @Schema(description = "最后通信时间")
  private Long onlineTime;

  /** 备注说明 */
  @Schema(description = "备注")
  private String detail;
}
