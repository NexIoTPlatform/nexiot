/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序设备查询DTO
 *
 * @author NexIoT
 * @since 2025/12/26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "小程序设备查询条件")
public class MpDeviceQueryDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "页码", example = "1")
  private Integer pageNum = 1;

  @Schema(description = "每页条数", example = "10")
  private Integer pageSize = 10;

  @Schema(description = "关键词（设备名称、设备ID模糊搜索）")
  private String keyword;

  @Schema(description = "设备名称（模糊搜索）")
  private String deviceName;

  @Schema(description = "设备ID（模糊搜索）")
  private String deviceId;

  @Schema(description = "设备类型（DEVICE/GATEWAY/GATEWAY_SUB_DEVICE）")
  private String deviceNode;

  @Schema(description = "设备标签")
  private String deviceTag;

  @Schema(description = "设备分组名称")
  private String devGroupName;

  @Schema(description = "设备分类ID")
  private String classifiedId;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "在线状态（0-离线，1-在线）")
  private Integer onlineStatus;

  @Schema(description = "IoT应用ID（内部使用，从appId获取）")
  private String linkedAppId;

  @Schema(description = "按用户授权过滤的设备ID列表（内部使用）")
  private java.util.List<String> iotIds;
}
