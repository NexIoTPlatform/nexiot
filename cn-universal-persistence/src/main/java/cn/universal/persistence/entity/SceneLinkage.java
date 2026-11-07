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

import cn.universal.common.annotation.Excel;
import cn.universal.common.domain.BaseEntity;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * scene_linkage表 SceneLinkage @Author gitee.com/NexIoT
 *
 * @since 2023-03-01
 */
@Table(name = "scene_linkage")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "场景联动实体")
public class SceneLinkage extends BaseEntity {

  private static final long serialVersionUID = 1L;

  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 场景名称 */
  @Excel(name = "场景名称")
  @Column(name = "scene_name")
  @Schema(description = "场景名称")
  private String sceneName;

  /** 触发条件 all.全部 one.任意一个 */
  @Excel(name = "触发条件 all.全部 one.任意一个")
  @Column(name = "touch")
  @Schema(description = "触发条件 all.全部 one.任意一个")
  private String touch;

  /** 触发条件 */
  @Excel(name = "触发条件")
  @Column(name = "trigger_condition")
  @Schema(description = "触发条件")
  private String triggerCondition;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 执行动作 */
  @Excel(name = "执行动作")
  @Column(name = "exec_action")
  @Schema(description = "执行动作")
  private String execAction;

  /** 沉默周期 */
  @Excel(name = "沉默周期")
  @Column(name = "sleep_cycle")
  @Schema(description = "沉默周期")
  private Integer sleepCycle;

  /** 0启用 1停用 */
  @Excel(name = "0启用 1停用")
  @Column(name = "status")
  @Schema(description = "状态 0启用 1停用")
  private Integer status;

  /** 设备id */
  @Excel(name = "设备序列号")
  @Column(name = "deviceId")
  @Schema(description = "设备ID")
  private String devId;
}
