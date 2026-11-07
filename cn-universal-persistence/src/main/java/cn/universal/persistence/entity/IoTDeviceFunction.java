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

package cn.universal.persistence.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "IoT设备功能实体")
public class IoTDeviceFunction implements Serializable {

  @Schema(description = "功能标识")
  private String id;

  @Schema(description = "功能名称")
  private String name;

  @Schema(description = "是否是配置")
  private boolean config;

  @Schema(description = "描述")
  private String description;

  @Schema(description = "功能来源")
  private String source;

  @Schema(description = "输入参数")
  private String inputs;

  @Schema(description = "输出定义")
  private String output;
}
