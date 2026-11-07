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

package cn.universal.quartz.service;

import cn.universal.quartz.domain.SysJob;
import java.util.List;
import org.quartz.SchedulerException;

/** 定时任务调度信息信息 服务层 @Author ruoyi */
public interface ISysJobService {

  /**
   * 获取quartz调度器的计划任务
   *
   * @param job 调度信息
   * @return 调度任务集合
   */
  //  public TableDataInfo<SysJob> selectPageJobList(SysJob job);
  public List<SysJob> selectJobList(SysJob job);

  /**
   * 通过调度任务ID查询调度信息
   *
   * @param jobId 调度任务ID
   * @return 调度任务对象信息
   */
  public SysJob selectJobById(Long jobId);

  /**
   * 通过调度任务名称查询调度信息
   *
   * @param jobName
   * @return
   */
  public List<SysJob> selectJobByName(String jobName);

  /**
   * 暂停任务
   *
   * @param job 调度信息
   * @return 结果
   */
  public int pauseJob(SysJob job) throws SchedulerException;

  /**
   * 恢复任务
   *
   * @param job 调度信息
   * @return 结果
   */
  public int resumeJob(SysJob job) throws SchedulerException;

  /**
   * 删除任务后，所对应的trigger也将被删除
   *
   * @param job 调度信息
   * @return 结果
   */
  public int deleteJob(SysJob job) throws SchedulerException;

  /**
   * 批量删除调度信息
   *
   * @param jobIds 需要删除的任务ID
   * @return 结果
   */
  public void deleteJobByIds(Long[] jobIds) throws SchedulerException;

  /**
   * 任务调度状态修改
   *
   * @param job 调度信息
   * @return 结果
   */
  public int changeStatus(SysJob job) throws SchedulerException;

  /**
   * 立即运行任务
   *
   * @param job 调度信息
   * @return 结果
   */
  public void run(SysJob job) throws SchedulerException;

  /**
   * 新增任务
   *
   * @param job 调度信息
   * @return 结果
   */
  public int insertJob(SysJob job) throws SchedulerException;

  /**
   * 更新任务
   *
   * @param job 调度信息
   * @return 结果
   */
  public int updateJob(SysJob job) throws SchedulerException;

  /**
   * 校验cron表达式是否有效
   *
   * @param cronExpression 表达式
   * @return 结果
   */
  public boolean checkCronExpressionIsValid(String cronExpression);
}
