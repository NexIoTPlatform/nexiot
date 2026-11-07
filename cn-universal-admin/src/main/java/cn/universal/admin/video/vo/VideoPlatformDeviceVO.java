/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台设备VO
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频平台设备VO
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频平台设备VO")
public class VideoPlatformDeviceVO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 视频设备表主键，用于关联通道；与厂商 deviceId、IoT iotId 是不同标识。 */
  private Long id;

  @Schema(description = "平台实例标识")
  // deviceId 仅在该实例内唯一，同一厂商设备可出现在不同实例中。
  private String instanceKey;

  @Schema(description = "设备ID")
  private String deviceId;

  @Schema(description = "设备名称")
  private String deviceName;

  @Schema(description = "设备状态")
  private String deviceStatus;

  @Schema(description = "设备型号")
  private String deviceModel;

  @Schema(description = "设备厂商")
  private String manufacturer;

  @Schema(description = "设备IP")
  private String deviceIp;

  @Schema(description = "设备端口")
  private Integer devicePort;

  @Schema(description = "组织ID")
  private String orgId;

  @Schema(description = "组织名称")
  private String orgName;

  /** 是否启用：0-禁用，1-启用 */
  @Schema(description = "启用状态")
  private Integer enabled;

  private String accessMode;

  private String loginMode;

  private String registrationId;

  @Schema(description = "通道列表")
  private List<VideoPlatformChannelVO> channelList;
}
