/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型产品实体类
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.common.typehandler.PostgreSqlJsonbStringTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 公共物模型产品实体 iot_standard_product
 *
 * @since 2026-01-06
 */
@Table(name = "iot_standard_product")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "公共物模型产品实体")
public class IoTStandardProduct implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 所属分类ID */
  @Column(name = "category_id")
  @Schema(description = "所属分类ID（支持任意分类层级）")
  private Long categoryId;

  /** 产品标识 */
  @Column(name = "product_key")
  @Schema(description = "产品标识（英文唯一标识）")
  private String productKey;

  /** 产品名称 */
  @Column(name = "product_name")
  @Schema(description = "产品名称")
  private String productName;

  /** 产品描述 */
  @Schema(description = "产品描述")
  private String description;

  /** 产品图片URL */
  @Column(name = "photo_url")
  @Schema(description = "产品图片URL")
  private String photoUrl;

  /** 物模型数据（JSON格式） */
  @ColumnType(jdbcType = JdbcType.OTHER, typeHandler = PostgreSqlJsonbStringTypeHandler.class)
  @Schema(description = "物模型数据（包含properties、events、functions）")
  private String metadata;

  /** 状态 */
  @Schema(description = "状态（0-禁用，1-启用）")
  private Integer status;

  /** 是否为模板 */
  @Column(name = "is_template")
  @Schema(description = "是否为模板（0-否，1-是）")
  private Integer isTemplate;

  /** 使用次数 */
  @Column(name = "use_count")
  @Schema(description = "使用次数（被产品引用的次数）")
  private Integer useCount;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 创建者 */
  @Column(name = "create_by")
  @Schema(description = "创建者")
  private String createBy;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 更新者 */
  @Column(name = "update_by")
  @Schema(description = "更新者")
  private String updateBy;

  /** 更新时间 */
  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  /** 分类名称（非数据库字段，用于展示） */
  @Transient
  @Schema(description = "分类名称")
  private String categoryName;

  /** 分类路径（非数据库字段，如：智能城市/公共事业/路灯照明） */
  @Transient
  @Schema(description = "分类路径")
  private String categoryPath;
}
