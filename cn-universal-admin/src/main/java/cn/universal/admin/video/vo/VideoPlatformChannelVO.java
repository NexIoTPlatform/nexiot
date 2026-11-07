/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台通道VO
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频平台通道VO
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频平台通道VO")
public class VideoPlatformChannelVO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "通道ID")
  // 厂商通道标识，保留原编码；不使用本地数据库通道主键替换。
  private String channelId;

  @Schema(description = "通道编码")
  private String channelCode;

  @Schema(description = "通道名称")
  private String name;

  @Schema(description = "通道状态")
  private String status;

  @Schema(description = "PTZ类型")
  private Integer ptzType;

  @Schema(description = "设备厂商")
  private String manufacturer;

  @Schema(description = "设备型号")
  private String model;

  @Schema(description = "通道IP")
  private String ipAddress;

  @Schema(description = "通道端口")
  private Integer port;
}
