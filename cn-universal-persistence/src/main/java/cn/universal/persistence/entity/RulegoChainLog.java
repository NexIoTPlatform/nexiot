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
 * rulego规则链执行日志实体
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/15
 */
@Table(name = "rulego_chain_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Rulego规则链日志实体")
public class RulegoChainLog implements Serializable {

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

  /** 执行ID */
  @Schema(description = "执行ID")
  @Column(name = "execution_id")
  private String executionId;

  /** 输入数据 */
  @Schema(description = "输入数据")
  @Column(name = "input_data")
  private String inputData;

  /** 输出数据 */
  @Schema(description = "输出数据")
  @Column(name = "output_data")
  private String outputData;

  /** 执行状态：success-成功，failed-失败 */
  @Schema(description = "执行状态")
  @Column(name = "execution_status")
  private String executionStatus;

  /** 错误信息 */
  @Schema(description = "错误信息")
  @Column(name = "error_message")
  private String errorMessage;

  /** 执行耗时(毫秒) */
  @Schema(description = "执行耗时")
  @Column(name = "execution_time")
  private Long executionTime;

  /** 创建时间 */
  @Schema(description = "创建时间")
  @Column(name = "create_time")
  private Date createTime;
}
