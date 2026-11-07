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

/**
 * rulego规则链管理实体
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/15
 */
@Table(name = "rulego_chain")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Rulego规则链实体")
public class RulegoChain implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "主键ID")
  private Long id;

  /** rulego规则链ID */
  @Schema(description = "rulego规则链ID")
  @Column(name = "rulego_id")
  private String rulegoId;

  /** 规则链名称 */
  @Schema(description = "规则链名称")
  @Column(name = "chain_name")
  private String chainName;

  /** 规则链描述 */
  @Schema(description = "规则链描述")
  @Column(name = "description")
  private String description;

  /** 创建人unionId */
  @Schema(description = "创建人unionId")
  @Column(name = "creator_id")
  private String creatorId;

  /** 租户ID */
  @Schema(description = "租户ID")
  @Column(name = "tenant_id")
  private String tenantId;

  /** 创建人姓名 */
  @Schema(description = "创建人姓名")
  @Column(name = "creator_name")
  private String creatorName;

  /** 状态：draft-草稿，deployed-已部署，stopped-已停止 */
  @Schema(description = "状态")
  @Column(name = "status")
  private String status;

  /** 规则链DSL内容(JSON格式) */
  @Schema(description = "规则链DSL内容")
  @Column(name = "dsl_content")
  private String dslContent;

  /** 最后同步时间 */
  @Schema(description = "最后同步时间")
  @Column(name = "last_sync_time")
  private Date lastSyncTime;

  /** 创建时间 */
  @Schema(description = "创建时间")
  @Column(name = "create_time")
  private Date createTime;

  /** 更新时间 */
  @Schema(description = "更新时间")
  @Column(name = "update_time")
  private Date updateTime;

  /** 是否删除：0-未删除，1-已删除 */
  @Schema(description = "是否删除")
  @Column(name = "deleted")
  private Integer deleted;
}
