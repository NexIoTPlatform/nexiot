/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.vo;

import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备详情VO
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpDeviceDetailVO {

  /** 设备IotId */
  private String iotId;

  /** 绑定ID */
  private Long bindId;

  /** 设备名称 */
  private String deviceName;

  /** 产品Key */
  private String productKey;

  /** 设备ID */
  private String deviceId;

  /** 设备属性列表 */
  private List<IoTDevicePropertiesBO> properties;

  /** 在线状态 */
  @Schema(description = "在线状态: 0-离线, 1-在线")
  private Boolean online;

  /** 最后上线时间 */
  @Schema(description = "最后上线时间")
  private Long lastOnlineTime;

  /** 产品图片URL */
  @Schema(description = "产品图片URL")
  private String productImage;
}
