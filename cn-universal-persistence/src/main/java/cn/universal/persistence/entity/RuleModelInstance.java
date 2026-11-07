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

/**
 * 规则实例 @Author gitee.com/NexIoT
 *
 * @since 2023/1/13 14:28
 */
@Table(name = "rule_model_instance")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "规则模型实例实体")
public class RuleModelInstance implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "主键ID")
  private Long id;

  /** 模型id */
  @Column(name = "model_id")
  @Schema(description = "模型ID")
  private Long modelId;

  /** 关联类型 */
  @Column(name = "relation_type")
  @Schema(description = "关联类型")
  private String relationType;

  /** 关联id */
  @Column(name = "relation_id")
  @Schema(description = "关联ID")
  private String relationId;
}
