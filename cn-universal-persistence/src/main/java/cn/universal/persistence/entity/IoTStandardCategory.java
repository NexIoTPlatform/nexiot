/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 公共物模型分类实体类
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
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 公共物模型分类实体 iot_standard_category
 *
 * @since 2026-01-06
 */
@Table(name = "iot_standard_category")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "公共物模型分类实体")
public class IoTStandardCategory implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 父分类ID */
  @Column(name = "parent_id")
  @Schema(description = "父分类ID（0表示顶级分类）")
  private Long parentId;

  /** 分类标识 */
  @Column(name = "category_key")
  @Schema(description = "分类标识（英文唯一标识）")
  private String categoryKey;

  /** 分类名称 */
  @Column(name = "category_name")
  @Schema(description = "分类名称")
  private String categoryName;

  /** 层级 */
  @Schema(description = "层级（1-一级分类，2-二级分类，3-三级分类）")
  private Integer level;

  /** 排序序号 */
  @Column(name = "list_order")
  @Schema(description = "排序序号")
  private Integer listOrder;

  /** 分类描述 */
  @Schema(description = "分类描述")
  private String description;

  /** 分类图标 */
  @Schema(description = "分类图标")
  private String icon;

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

  /** 子分类列表（非数据库字段） */
  @Transient
  @Schema(description = "子分类列表")
  private List<IoTStandardCategory> children = new ArrayList<>();

  /** 是否有子节点（非数据库字段） */
  @Transient
  @Schema(description = "是否有子节点")
  private Boolean hasChildren;

  /** 分类路径（非数据库字段） */
  @Transient
  @Schema(description = "分类路径（如：智能城市/公共事业/路灯照明）")
  private String categoryPath;
}
