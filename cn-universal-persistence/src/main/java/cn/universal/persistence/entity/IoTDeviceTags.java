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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_device_tags")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备标签实体")
public class IoTDeviceTags implements Serializable {

  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 设备deviceId */
  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  /** 产品ID或者设备唯一标识 */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  @Column(name = "iot_id")
  @Schema(description = "IoT ID")
  private String iotId;

  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private Long createTime;

  @Column(name = "tag_name")
  @Schema(description = "标签名称")
  private String name;

  @Schema(description = "标签描述")
  private String description;

  @Column(name = "tag_type")
  @Schema(description = "标签类型")
  private String type;

  @Column(name = "tag_value")
  @Schema(description = "标签值")
  private String value;

  @Column(name = "tag_key")
  @Schema(description = "标签键")
  private String key;

  @Column(name = "tag_instance")
  @Schema(description = "实例名称")
  private String instance;

  private static final long serialVersionUID = 1L;
}
