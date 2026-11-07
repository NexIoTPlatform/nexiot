/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.vo;

import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.persistence.entity.IoTDevice;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序设备列表VO
 *
 * @author NexIoT
 * @since 2025/12/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "小程序设备信息")
public class MpDeviceVO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "设备ID")
  private String iotId;

  @Schema(description = "接入方式")
  private String thirdPlatform;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "设备标识")
  private String deviceId;

  @Schema(description = "扩展设备标识")
  private String extDeviceId;

  @Schema(description = "设备名称")
  private String deviceName;

  @Schema(description = "产品名称")
  private String productName;

  @Schema(description = "产品图片URL")
  private String productImage;

  @Schema(description = "坐标")
  private String coordinate;

  @Schema(description = "设备类型")
  private String deviceType;

  @Schema(description = "在线状态: 0-离线, 1-在线")
  private Integer onlineStatus;

  @Schema(description = "最后在线时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date lastOnlineTime;

  @Schema(description = "设备属性列表（简要）")
  private List<IoTDevicePropertiesBO> properties;

  @Schema(description = "网关产品")
  private String gwProductKey;

  @Schema(description = "网关设备ID")
  private String gwDeviceId;

  @Schema(description = "网关设备")
  private IoTDevice gwDevice;

  @Schema(description = "子设备列表（仅网关设备有值）")
  private List<MpDeviceVO> subDevices;

  @Schema(description = "设备标签（暂未使用）")
  private String deviceTag;

  @Schema(description = "设备地址（暂未使用）")
  private String deviceAddress;

  @Schema(description = "设备类型")
  private String deviceNode;

  @Schema(description = "设备配置JSON")
  private String configuration;

  @Schema(description = "产品物模型JSON")
  private String metaData;

  @Schema(description = "创建时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  private String ext1;
  private String ext2;
  private String ext3;
  private String ext4;
}
