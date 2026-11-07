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
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_device_shadow")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备影子实体")
public class IoTDeviceShadow implements Serializable {

  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 本平台设备唯一标识符 */
  @Column(name = "iot_id")
  @Schema(description = "IoT ID")
  private String iotId;

  /** 产品KEY */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** 设备自身序号 */
  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  /** 第三方平台设备ID唯一标识符 */
  @Column(name = "ext_device_id")
  @Schema(description = "扩展设备ID")
  private String extDeviceId;

  /** 注册时间 */
  @Column(name = "active_time")
  @Schema(description = "激活时间")
  private Date activeTime;

  /** 激活时间 */
  @Column(name = "online_time")
  @Schema(description = "在线时间")
  private Date onlineTime;

  /** 最后通信时间 */
  @Column(name = "last_time")
  @Schema(description = "最后通信时间")
  private Date lastTime;

  /** 更新时间 */
  @Column(name = "update_date")
  @Schema(description = "更新时间")
  private Date updateDate;

  /** 影子数据 */
  @Schema(description = "影子数据")
  private String metadata;

  @Column(name = "shadow_instance")
  @Schema(description = "实例名称")
  private String instance;

  /** 版本号 */
  @Column(name = "version")
  @Schema(description = "版本号")
  private Long version;

  private static final long serialVersionUID = 1L;
}
