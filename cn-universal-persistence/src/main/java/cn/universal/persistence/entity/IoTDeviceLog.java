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

import cn.universal.persistence.common.inteceptor.SQenGenId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_device_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备日志")
public class IoTDeviceLog implements Serializable {

  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 唯一编码 */
  @Column(name = "iot_id", length = 128)
  @Schema(description = "IoT ID")
  private String iotId;

  /** 设备自身序号 */
  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  /** 产品ID */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** 设备名称 */
  @Column(name = "device_name")
  @Schema(description = "设备名称")
  private String deviceName;

  /** 消息类型 */
  @Column(name = "message_type")
  @Schema(description = "消息类型")
  private String messageType;

  /** 指令ID */
  @Column(name = "command_id")
  @Schema(description = "指令ID")
  private String commandId;

  /** 指令ID */
  @Column(name = "command_status")
  @Schema(description = "指令状态")
  private Integer commandStatus;

  /** 事件名称 */
  @Schema(description = "事件名称")
  private String event;

  /** 创建时间 */
  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private LocalDateTime createTime;

  /** 内容 */
  @Schema(description = "日志内容")
  private String content;

  @Schema(description = "点位信息")
  private String point;

  private static final long serialVersionUID = 1L;
}
