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

import cn.universal.quartz.domain.SysJobLog;
import cn.universal.quartz.mapper.SysJobLogMapper;
import cn.universal.quartz.service.ISysJobLogService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;

/** 定时任务调度日志信息 服务层 @Author ruoyi */
@Service
public class SysJobLogServiceImpl implements ISysJobLogService {

  @Resource private SysJobLogMapper sysJobLogMapper;

  /**
   * 获取quartz调度器日志的计划任务
   *
   * @param jobLog 调度日志信息
   * @return 调度任务日志集合
   */
  @Override
  public List<SysJobLog> selectJobLogList(SysJobLog jobLog) {
    //    Map<String, Object> params = jobLog.getParams();
    return sysJobLogMapper.select(jobLog);
  }

  /**
   * 通过调度任务日志ID查询调度信息
   *
   * @param jobLogId 调度任务日志ID
   * @return 调度任务日志对象信息
   */
  @Override
  public SysJobLog selectJobLogById(Long jobLogId) {
    return sysJobLogMapper.selectByPrimaryKey(jobLogId);
  }

  /**
   * 新增任务日志
   *
   * @param jobLog 调度日志信息
   */
  @Override
  public void addJobLog(SysJobLog jobLog) {
    sysJobLogMapper.insertSelective(jobLog);
  }

  //  /**
  //   * 批量删除调度日志信息
  //   *
  //   * @param logIds 需要删除的数据ID
  //   * @return 结果
  //   */
  //  @Override
  //  public int deleteJobLogByIds(Long[] logIds) {
  //    return sysJobLogMapper.delete(Arrays.asList(logIds));
  //  }

  /**
   * 删除任务日志
   *
   * @param jobId 调度日志ID
   */
  @Override
  public int deleteJobLogById(Long jobId) {
    return sysJobLogMapper.deleteByPrimaryKey(jobId);
  }

  /** 清空任务日志 */
  @Override
  public void cleanJobLog() {
    sysJobLogMapper.delete(new SysJobLog());
  }
}
