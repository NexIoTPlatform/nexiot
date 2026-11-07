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

@Table(name = "iot_device_log_metadata")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备日志元数据实体")
public class IoTDeviceLogMetadata implements Serializable {

  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "主键ID")
  private Long id;

  @Column(name = "iot_id")
  @Schema(description = "IoT ID")
  private String iotId;

  /** 产品唯一标识 */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** 设备名称 */
  @Column(name = "device_name", length = 64)
  @Schema(description = "设备名称")
  private String deviceName;

  @Column(name = "device_id", length = 64)
  @Schema(description = "设备ID")
  private String deviceId;

  /** 消息类型 */
  @Column(name = "message_type")
  @Schema(description = "消息类型")
  private String messageType;

  @Column(length = 32)
  @Schema(description = "事件名称")
  private String event;

  /** 属性 */
  @Schema(description = "属性")
  private String property;

  @Schema(description = "扩展字段1")
  private String ext1;

  @Schema(description = "扩展字段2")
  private String ext2;

  @Schema(description = "扩展字段3")
  private String ext3;

  /** 发生时间 */
  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private LocalDateTime createTime;

  /** 其他 */
  @Schema(description = "内容")
  private String content;

  private static final long serialVersionUID = 1L;
}
