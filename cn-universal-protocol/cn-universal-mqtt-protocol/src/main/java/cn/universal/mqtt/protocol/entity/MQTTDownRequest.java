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

package cn.universal.mqtt.protocol.entity;

import cn.universal.core.message.DownCommonData;
import cn.universal.persistence.base.BaseDownRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * http下行参数
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/19 11:19
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@Schema(description = "MQTT下行参数")
public class MQTTDownRequest extends BaseDownRequest {

  @Schema(description = "下行结果")
  private String downResult;

  @Schema(description = "下行通用数据")
  private DownCommonData downCommonData;

  @Schema(description = "下行主题")
  private String downTopic;

  @Schema(description = "子设备专用下行主题")
  private String subDeviceDownTopic;

  public DownCommonData getDownCommonData() {
    if (this.downCommonData != null) {
      return this.downCommonData;
    }
    if (getData() != null) {
      this.downCommonData = DownCommonData.from(getData());
      return downCommonData;
    }
    return new DownCommonData();
  }
}
