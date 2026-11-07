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

@Table(name = "iot_device_rule_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备规则日志实体")
public class IoTDeviceRuleLog implements Serializable {

  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 业务ID */
  @Column(name = "c_id")
  @Schema(description = "业务ID")
  private String cId;

  /** 业务名称 */
  @Column(name = "c_name")
  @Schema(description = "业务名称")
  private String cName;

  /** 执行状态 */
  @Column(name = "c_status")
  @Schema(description = "执行状态")
  private Byte cStatus;

  /** 1-场景联动，2-数据流转 */
  @Column(name = "c_type")
  @Schema(description = "业务类型")
  private Byte cType;

  /** 条件 */
  @Schema(description = "执行条件")
  private String conditions;

  @Column(name = "create_by")
  @Schema(description = "创建人")
  private String createBy;

  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private Date createTime;

  @Column(name = "update_time")
  @Schema(description = "更新时间")
  private Date updateTime;

  @Column(name = "c_device_meta")
  @Schema(description = "设备元数据")
  private String cDeviceMeta;

  @Schema(description = "日志内容")
  private String content;

  private static final long serialVersionUID = 1L;
}
