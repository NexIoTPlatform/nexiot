/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 标准字段与分类关联实体类
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
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

/**
 * 标准字段与分类关联实体 iot_standard_field_category_rel
 *
 * @since 2026-01-06
 */
@Table(name = "iot_standard_field_category_rel")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "标准字段与分类关联实体")
public class IoTStandardFieldCategoryRel implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 字段ID */
  @Column(name = "field_id")
  @Schema(description = "字段ID")
  private Long fieldId;

  /** 分类ID */
  @Column(name = "category_id")
  @Schema(description = "分类ID")
  private Long categoryId;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;
}
