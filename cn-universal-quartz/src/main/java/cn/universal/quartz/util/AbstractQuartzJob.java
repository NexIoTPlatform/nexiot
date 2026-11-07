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

package cn.universal.quartz.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.utils.SpringUtils;
import cn.universal.quartz.constant.Constants;
import cn.universal.quartz.constant.ScheduleConstants;
import cn.universal.quartz.domain.SysJob;
import cn.universal.quartz.domain.SysJobLog;
import cn.universal.quartz.service.ISysJobLogService;
import java.util.Date;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 抽象quartz调用 @Author ruoyi */
public abstract class AbstractQuartzJob implements Job {

  private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

  /** 线程本地变量 */
  private static ThreadLocal<Date> threadLocal = new ThreadLocal<>();

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    SysJob sysJob = new SysJob();
    BeanUtil.copyProperties(
        context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES), sysJob);
    Exception jobException = null;
    try {
      before(context, sysJob);
      if (Validator.isNotNull(sysJob)) {
        doExecute(context, sysJob);
      }
    } catch (Exception e) {
      jobException = e;
      log.error("任务执行异常  - ：", e);
    } finally {
      after(context, sysJob, jobException);
    }
  }

  /**
   * 执行前
   *
   * @param context 工作执行上下文对象
   * @param sysJob 系统计划任务
   */
  protected void before(JobExecutionContext context, SysJob sysJob) {
    threadLocal.set(new Date());
  }

  /**
   * 执行后
   *
   * @param context 工作执行上下文对象
   * @param sysJob 系统计划任务
   */
  protected void after(JobExecutionContext context, SysJob sysJob, Exception e) {
    Date startTime = threadLocal.get();
    threadLocal.remove();
    Date stopTime = new Date();
    if (startTime == null) {
      startTime = stopTime;
    }

    final SysJobLog sysJobLog = new SysJobLog();
    sysJobLog.setJobName(sysJob.getJobName());
    sysJobLog.setJobGroup(sysJob.getJobGroup());
    sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
    sysJobLog.setStartTime(startTime);
    sysJobLog.setStopTime(stopTime);
    long runMs = sysJobLog.getStopTime().getTime() - sysJobLog.getStartTime().getTime();
    sysJobLog.setJobMessage(sysJobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");
    if (e != null) {
      sysJobLog.setStatus(Constants.FAIL);
      String errorMsg = StrUtil.sub(ExceptionUtil.stacktraceToString(e), 0, 2000);
      sysJobLog.setExceptionInfo(errorMsg);
    } else {
      sysJobLog.setStatus(Constants.SUCCESS);
    }
    sysJobLog.setCreateTime(new Date());
    try {
      SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
    } catch (Exception ex) {
      log.error("任务日志写入失败: jobName={}, jobGroup={}", sysJobLog.getJobName(), sysJobLog.getJobGroup(), ex);
    }
  }

  /**
   * 执行方法，由子类重载
   *
   * @param context 工作执行上下文对象
   * @param sysJob 系统计划任务
   * @throws Exception 执行过程中的异常
   */
  protected abstract void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception;
}
