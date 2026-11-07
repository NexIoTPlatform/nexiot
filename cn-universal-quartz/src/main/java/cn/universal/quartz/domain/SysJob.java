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

package cn.universal.quartz.domain;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.annotation.Excel;
import cn.universal.common.annotation.Excel.ColumnType;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.quartz.constant.ScheduleConstants;
import cn.universal.quartz.util.CronUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.KeySql;

/** 定时任务调度表 sys_job @Author ruoyi */
@Data
@Table(name = "sys_job")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class SysJob implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 任务ID */
  @Excel(name = "任务序号", cellType = ColumnType.NUMERIC)
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long jobId;

  /** 任务名称 */
  @NotBlank(message = "任务名称不能为空")
  @Size(min = 0, max = 64, message = "任务名称不能超过64个字符")
  @Excel(name = "任务名称")
  @Column(name = "job_name")
  private String jobName;

  /** 任务组名 */
  @Excel(name = "任务组名")
  @Column(name = "job_group")
  private String jobGroup;

  /** 调用目标字符串 */
  @NotBlank(message = "调用目标字符串不能为空")
  @Size(min = 0, max = 500, message = "调用目标字符串长度不能超过500个字符")
  @Excel(name = "调用目标字符串")
  @Column(name = "invoke_target")
  private String invokeTarget;

  /** cron执行表达式 */
  @NotBlank(message = "Cron执行表达式不能为空")
  @Size(min = 0, max = 255, message = "Cron执行表达式不能超过255个字符")
  @Excel(name = "执行表达式 ")
  @Column(name = "cron_expression")
  private String cronExpression;

  /** cron计划策略 */
  @Excel(name = "计划策略 ", readConverterExp = "0=默认,1=立即触发执行,2=触发一次执行,3=不触发立即执行")
  @Column(name = "misfire_policy")
  private String misfirePolicy = ScheduleConstants.MISFIRE_DEFAULT;

  /** 是否并发执行（0允许 1禁止） */
  @Excel(name = "并发执行", readConverterExp = "0=允许,1=禁止")
  @Column(name = "concurrent")
  private String concurrent;

  /** 任务状态（0正常 1暂停） */
  @Excel(name = "任务状态", readConverterExp = "0=正常,1=暂停")
  @Column(name = "status")
  private String status;

  /** 创建者 */
  @Column(name = "create_by")
  private String createBy;

  /** 创建时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "create_time")
  private Date createTime;

  /** 更新者 */
  @Column(name = "update_by")
  private String updateBy;

  /** 更新时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "update_time")
  private Date updateTime;

  /** 备注 */
  @Column(name = "remark")
  private String remark;

  /** 请求参数 */
  private Map<String, Object> params = new HashMap<>();

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  public Date getNextValidTime() {
    if (StrUtil.isNotEmpty(cronExpression)) {
      return CronUtils.getNextExecution(cronExpression);
    }
    return null;
  }
}
