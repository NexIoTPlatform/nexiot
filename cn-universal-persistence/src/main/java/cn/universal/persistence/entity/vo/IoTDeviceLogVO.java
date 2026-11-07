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
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "设备日志视图对象")
public class IoTDeviceLogVO {

  private static final long serialVersionUID = 1L;

  /*-----------------日志信息---------------------*/

  /** 日志ID，非自增 */
  @Schema(description = "日志ID")
  @com.fasterxml.jackson.databind.annotation.JsonSerialize(
      using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
  private Long id;

  /** 设备编码 */
  @Schema(description = "IoT ID")
  private String iotId;

  /** 设备序列号 */
  @Schema(description = "设备ID")
  private String deviceId;

  /** 产品ID */
  @Schema(description = "产品Key")
  private String productKey;

  /** 设备名称 */
  @Schema(description = "设备名称")
  private String deviceName;

  /** 消息类型 */
  @Schema(description = "消息类型")
  private String messageType;

  /** 指令ID */
  @Schema(description = "指令ID")
  private String commandId;

  /** 指令状态 */
  @Schema(description = "指令状态")
  private Integer commandStatus;

  /** 事件名称 */
  @Schema(description = "事件名称")
  private String event;

  /** 经纬度 */
  @Schema(description = "经纬度坐标")
  private String point;

  /** 内容 */
  @Schema(description = "日志内容")
  private String content;

  /** 创建时间 */
  @Schema(description = "创建时间")
  private LocalDateTime createTime;
}
