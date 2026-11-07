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
 * @since 2023/4/19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "IoT设备订阅业务对象")
public class IoTDeviceSubscribeBO {

  @Schema(description = "消息类型")
  private String msgType;

  @Schema(description = "订阅URL")
  private String url;

  @Schema(description = "订阅主题")
  private String topic;
}
