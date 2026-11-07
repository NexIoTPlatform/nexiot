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

package cn.universal.quartz.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.quartz.constant.Constants;
import cn.universal.quartz.constant.ScheduleConstants;
import cn.universal.quartz.domain.SysJob;
import cn.universal.quartz.mapper.SysJobMapper;
import cn.universal.quartz.service.ISysJobService;
import cn.universal.quartz.util.CronUtils;
import cn.universal.quartz.util.ScheduleUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.List;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;
import lombok.extern.slf4j.Slf4j;

/** 定时任务调度信息 服务层 @Author ruoyi */
@Slf4j
@Service
public class SysJobServiceImpl implements ISysJobService {

  private static final SQenGenId JOB_ID_GENERATOR = new SQenGenId();

  @Autowired private Scheduler scheduler;
  @Resource private SysJobMapper sysJobMapper;

  /** 项目启动时，初始化定时器 主要是防止手动修改数据库导致未同步到定时任务处理（注：不能手动修改数据库ID和任务组名，否则会导致脏数据） */
  @PostConstruct
  public void init() throws SchedulerException {
    List<SysJob> jobList = sysJobMapper.selectAll();
    for (SysJob job : jobList) {
      JobKey jobKey = ScheduleUtils.getJobKey(job.getJobId(), job.getJobGroup());
      if (scheduler.checkExists(jobKey)) {
        continue;
      }
      try {
        ScheduleUtils.createScheduleJob(scheduler, job);
      } catch (ObjectAlreadyExistsException e) {
        log.debug("Quartz 任务已存在，跳过补齐: jobId={}, jobGroup={}", job.getJobId(), job.getJobGroup());
      }
    }
  }

  /**
   * 获取quartz调度器的计划任务列表
   *
   * @param job 调度信息
   * @return
   */
  @Override
  public List<SysJob> selectJobList(SysJob job) {
    return sysJobMapper.select(job);
  }

  /**
   * 通过调度任务ID查询调度信息
   *
   * @param jobId 调度任务ID
   * @return 调度任务对象信息
   */
  @Override
  public SysJob selectJobById(Long jobId) {
    return sysJobMapper.selectByPrimaryKey(jobId);
  }

  @Override
  public List<SysJob> selectJobByName(String jobName) {
    Example example = new Example(SysJob.class);
    example.createCriteria().andEqualTo("jobName", jobName);
    return sysJobMapper.selectByExample(example);
  }

  /**
   * 暂停任务
   *
   * @param job 调度信息
   */
  @Override
  @Transactional
  public int pauseJob(SysJob job) throws SchedulerException {
    Long jobId = job.getJobId();
    String jobGroup = job.getJobGroup();
    job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
    int rows = sysJobMapper.updateByPrimaryKeySelective(job);
    if (rows > 0) {
      scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
    }
    return rows;
  }

  /**
   * 恢复任务
   *
   * @param job 调度信息
   */
  @Override
  @Transactional
  public int resumeJob(SysJob job) throws SchedulerException {
    Long jobId = job.getJobId();
    String jobGroup = job.getJobGroup();
    job.setStatus(ScheduleConstants.Status.NORMAL.getValue());
    int rows = sysJobMapper.updateByPrimaryKeySelective(job);
    if (rows > 0) {
      scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
    }
    return rows;
  }

  /**
   * 删除任务后，所对应的trigger也将被删除
   *
   * @param job 调度信息
   */
  @Override
  @Transactional
  public int deleteJob(SysJob job) throws SchedulerException {
    Long jobId = job.getJobId();
    String jobGroup = job.getJobGroup();
    JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
    if (scheduler.checkExists(jobKey)) {
      scheduler.deleteJob(jobKey);
    }
    return sysJobMapper.deleteByPrimaryKey(jobId);
  }

  /**
   * 批量删除调度信息
   *
   * @param jobIds 需要删除的任务ID
   * @return 结果
   */
  @Override
  @Transactional
  public void deleteJobByIds(Long[] jobIds) throws SchedulerException {
    for (Long jobId : jobIds) {
      SysJob job = sysJobMapper.selectByPrimaryKey(jobId);
      deleteJob(job);
    }
  }

  /**
   * 任务调度状态修改
   *
   * @param job 调度信息
   */
  @Override
  @Transactional
  public int changeStatus(SysJob job) throws SchedulerException {
    int rows = 0;
    String status = job.getStatus();
    if (ScheduleConstants.Status.NORMAL.getValue().equals(status)) {
      rows = resumeJob(job);
    } else if (ScheduleConstants.Status.PAUSE.getValue().equals(status)) {
      rows = pauseJob(job);
    }
    return rows;
  }

  /**
   * 立即运行任务
   *
   * @param job 调度信息
   */
  @Override
  @Transactional
  public void run(SysJob job) throws SchedulerException {
    Long jobId = job.getJobId();
    String jobGroup = job.getJobGroup();
    SysJob properties = selectJobById(job.getJobId());
    // 参数
    JobDataMap dataMap = new JobDataMap();
    dataMap.put(ScheduleConstants.TASK_PROPERTIES, properties);
    scheduler.triggerJob(ScheduleUtils.getJobKey(jobId, jobGroup), dataMap);
  }

  /**
   * 新增任务
   *
   * @param job 调度信息 调度信息
   */
  @Override
  @Transactional
  public int insertJob(SysJob job) throws SchedulerException {
    if (!CronUtils.isValid(job.getCronExpression())) {
      throw new IoTException("新增任务'" + job.getJobName() + "'失败，Cron表达式不正确");
    } else if (StrUtil.containsIgnoreCase(job.getInvokeTarget(), Constants.LOOKUP_RMI)) {
      throw new IoTException("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi://'调用");
    }
    if (job.getJobId() == null) {
      job.setJobId(JOB_ID_GENERATOR.genId("sys_job", "job_id"));
    }
    //    job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
    int rows = sysJobMapper.insertJob(job);
    if (rows > 0) {
      ScheduleUtils.createScheduleJob(scheduler, job);
    }
    return rows;
  }

  /**
   * 更新任务的时间表达式
   *
   * @param job 调度信息
   */
  @Override
  @Transactional
  public int updateJob(SysJob job) throws SchedulerException {
    SysJob properties = selectJobById(job.getJobId());
    int rows = sysJobMapper.updateByPrimaryKeySelective(job);
    if (rows > 0) {
      updateSchedulerJob(job, properties.getJobGroup());
    }
    return rows;
  }

  /**
   * 更新任务
   *
   * @param job 任务对象
   * @param jobGroup 任务组名
   */
  public void updateSchedulerJob(SysJob job, String jobGroup) throws SchedulerException {
    Long jobId = job.getJobId();
    // 判断是否存在
    JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
    if (scheduler.checkExists(jobKey)) {
      // 防止创建时存在数据问题 先移除，然后在执行创建操作
      scheduler.deleteJob(jobKey);
    }
    ScheduleUtils.createScheduleJob(scheduler, job);
  }

  /**
   * 校验cron表达式是否有效
   *
   * @param cronExpression 表达式
   * @return 结果
   */
  @Override
  public boolean checkCronExpressionIsValid(String cronExpression) {
    return CronUtils.isValid(cronExpression);
  }
}
