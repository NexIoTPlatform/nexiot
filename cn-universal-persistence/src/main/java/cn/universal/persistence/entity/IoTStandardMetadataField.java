/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 标准物模型字段实体类
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
 * 标准物模型字段实体 iot_standard_metadata_field
 *
 * @since 2026-01-06
 */
@Table(name = "iot_standard_metadata_field")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "标准物模型字段实体")
public class IoTStandardMetadataField implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 字段类型 */
  @Column(name = "field_type")
  @Schema(description = "字段类型（property-属性，event-事件，function-功能）")
  private String fieldType;

  /** 字段标识 */
  @Column(name = "field_id")
  @Schema(description = "字段标识")
  private String fieldId;

  /** 字段名称 */
  @Column(name = "field_name")
  @Schema(description = "字段名称")
  private String fieldName;

  /** 推荐分类ID */
  @Column(name = "category_id")
  @Schema(description = "推荐分类ID（可为空，表示通用字段）")
  private Long categoryId;

  /** 数据类型 */
  @Column(name = "data_type")
  @Schema(description = "数据类型（int/float/string/boolean/enum/object/array）")
  private String dataType;

  /** 单位 */
  @Schema(description = "单位")
  private String unit;

  /** 读写模式 */
  @Schema(description = "读写模式（r-只读，w-只写，rw-读写），仅属性有效")
  private String mode;

  /** 字段描述 */
  @Schema(description = "字段描述")
  private String description;

  /** 值类型配置 */
  @Column(name = "value_type_config")
  @ColumnType(jdbcType = JdbcType.OTHER, typeHandler = PostgreSqlJsonbStringTypeHandler.class)
  @Schema(description = "值类型配置（JSON格式，包含elements、expands等）")
  private String valueTypeConfig;

  /** 完整物模型配置 */
  @Column(name = "metadata_config")
  @Schema(description = "完整物模型配置（JSON格式，存储properties/events/functions的完整结构，包括inputs、outputs等）")
  private String metadataConfig;

  /** 搜索关键词 */
  @Column(name = "search_keywords")
  @Schema(description = "搜索关键词（逗号分隔，用于联想搜索）")
  private String searchKeywords;

  /** 使用次数 */
  @Column(name = "use_count")
  @Schema(description = "使用次数（用于排序推荐）")
  private Integer useCount;

  /** 推荐优先级 */
  @Column(name = "recommend_priority")
  @Schema(description = "推荐优先级（数值越大越优先）")
  private Integer recommendPriority;

  /** 状态 */
  @Schema(description = "状态（0-禁用，1-启用）")
  private Integer status;

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

  /** 分类名称（非数据库字段） */
  @Transient
  @Schema(description = "分类名称")
  private String categoryName;
}
