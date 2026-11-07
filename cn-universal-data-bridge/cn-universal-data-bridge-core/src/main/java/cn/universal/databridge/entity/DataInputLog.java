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

package cn.universal.databridge.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 数据输入日志实体
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "iot_data_input_log")
@Schema(description = "数据输入日志实体")
public class DataInputLog implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "主键ID")
  private Long id;

  /** 配置ID */
  @Column(name = "config_id")
  @Schema(description = "配置ID")
  private Long configId;

  /** 配置名称 */
  @Column(name = "config_name")
  @Schema(description = "配置名称")
  private String configName;

  /** 源系统 */
  @Column(name = "source_system")
  @Schema(description = "源系统")
  private String sourceSystem;

  /** 消息数量 */
  @Column(name = "message_count")
  @Schema(description = "消息数量")
  private Integer messageCount;

  /** 成功数量 */
  @Column(name = "success_count")
  @Schema(description = "成功数量")
  private Integer successCount;

  /** 失败数量 */
  @Column(name = "failed_count")
  @Schema(description = "失败数量")
  private Integer failedCount;

  /** 错误信息 */
  @Column(name = "error_message")
  @Schema(description = "错误信息")
  private String errorMessage;

  /** 执行时间(毫秒) */
  @Column(name = "execution_time")
  @Schema(description = "执行时间(毫秒)")
  private Long executionTime;

  /** 状态 */
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  @Schema(description = "状态")
  private Status status;

  /** 创建时间 */
  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private LocalDateTime createTime;

  /** 创建者 */
  @Column(name = "create_by")
  @Schema(description = "创建者")
  private String createBy;

  /** 状态枚举 */
  public enum Status {
    SUCCESS, // 成功
    FAILED, // 失败
    RUNNING // 运行中
  }
}
