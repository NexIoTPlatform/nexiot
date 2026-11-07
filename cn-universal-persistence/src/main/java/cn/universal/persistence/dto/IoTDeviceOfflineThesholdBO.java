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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/2/9
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "IoT设备离线阈值业务对象")
public class IoTDeviceOfflineThesholdBO {

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "平台类型")
  private String platform;

  @Schema(description = "离线阈值")
  private int offlineThreshold;
}
