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

import cn.universal.common.annotation.Excel;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.KeySql;

/** 定时任务调度日志表 sys_job_log @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "sys_job_log")
public class SysJobLog {

  private static final long serialVersionUID = 1L;

  /** ID */
  @Excel(name = "日志序号")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long jobLogId;

  /** 任务名称 */
  @Excel(name = "任务名称")
  @Column(name = "job_name")
  private String jobName;

  /** 任务组名 */
  @Excel(name = "任务组名")
  @Column(name = "job_group")
  private String jobGroup;

  /** 调用目标字符串 */
  @Excel(name = "调用目标字符串")
  @Column(name = "invoke_target")
  private String invokeTarget;

  /** 日志信息 */
  @Excel(name = "日志信息")
  @Column(name = "job_message")
  private String jobMessage;

  /** 执行状态（0正常 1失败） */
  @Excel(name = "执行状态", readConverterExp = "0=正常,1=失败")
  @Column(name = "status")
  private Integer status;

  /** 异常信息 */
  @Excel(name = "异常信息")
  @Column(name = "exception_info")
  private String exceptionInfo;

  /** 创建时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "create_time")
  private Date createTime;

  /** 开始时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "start_time")
  private Date startTime;

  /** 请求参数 */
  @Transient private Map<String, Object> params = new HashMap<>();

  /** 停止时间 */
  @Transient private Date stopTime;
}
