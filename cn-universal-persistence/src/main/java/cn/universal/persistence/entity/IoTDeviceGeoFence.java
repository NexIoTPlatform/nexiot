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
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 设备围栏 @Author gitee.com/NexIoT
 *
 * @since 2023/8/5 8:47
 */
@Table(name = "iot_device_geo_fence")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备地理围栏实体")
public class IoTDeviceGeoFence implements Serializable {

  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 围栏名称 */
  @Schema(description = "围栏名称")
  private String name;

  /** 围栏状态 0.启用 1.停用 */
  @Schema(description = "围栏状态")
  private Integer status;

  /** 触发模式 in.进入 out.离开 all.进入&离开 */
  @Schema(description = "触发模式")
  private String touchWay;

  /** 范围 */
  @Schema(description = "围栏范围")
  private String fence;

  /** 类型 circle.圆 polygon.多边形 */
  @Schema(description = "围栏类型")
  private String type;

  /** 圆形中心点 */
  @Schema(description = "中心点坐标")
  private String point;

  /** 半径 */
  @Schema(description = "圆形半径")
  private BigDecimal radius;

  /** 创建人 */
  @Schema(description = "创建人ID")
  private String creatorId;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 周触发(天) */
  @Schema(description = "周触发时间")
  private String weekTime;

  /** 天触发开始时间(时) */
  @Schema(description = "开始时间")
  private String beginTime;

  /** 天触发结束时间(时) */
  @Schema(description = "结束时间")
  private String endTime;

  /** 归属第三方应用 */
  @Schema(description = "创建用户")
  private String creatorUser;

  @Transient
  @Schema(description = "查询用户列表")
  private List<String> queryUserList;

  /** 创建时间 */
  @Schema(description = "创建时间")
  private Date createDate;

  /** 更新时间 */
  @Schema(description = "更新时间")
  private Date updateDate;

  /** 不触发时间 */
  @Schema(description = "不触发时间段")
  private String noTriggerTime;

  /** 延迟时间 分钟 */
  @Schema(description = "延迟时间(分钟)")
  private Integer delayTime;
}
