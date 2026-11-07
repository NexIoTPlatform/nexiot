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

package cn.universal.admin.system.service.impl;

import cn.universal.admin.system.service.ISysOperLogService;
import cn.universal.persistence.entity.SysOperLog;
import cn.universal.persistence.mapper.SysOperLogMapper;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;

/** 操作日志 服务层处理 @Author ruoyi */
@Service
public class SysOperLogServiceImpl implements ISysOperLogService {

  @Resource private SysOperLogMapper sysOperLogMapper;

  @Override
  public List<SysOperLog> selectPageOperLogList(SysOperLog operLog) {
    List<SysOperLog> sysOperLogs = sysOperLogMapper.selectList(operLog);
    return sysOperLogs;
  }

  /**
   * 新增操作日志
   *
   * @param operLog 操作日志对象
   */
  @Override
  public void insertOperlog(SysOperLog operLog) {
    operLog.setOperTime(new Date());
    sysOperLogMapper.insertSelective(operLog);
  }

  /**
   * 查询系统操作日志集合
   *
   * @param operLog 操作日志对象
   * @return 操作日志集合
   */
  @Override
  public List<SysOperLog> selectOperLogList(SysOperLog operLog) {
    List<SysOperLog> sysOperLogs = sysOperLogMapper.selectList(operLog);
    return sysOperLogs;
  }

  /**
   * 批量删除系统操作日志
   *
   * @param operIds 需要删除的操作日志ID
   * @return 结果
   */
  @Override
  public int deleteOperLogByIds(Long[] operIds) {
    return sysOperLogMapper.deleteByIds(Arrays.toString(operIds));
  }

  /**
   * 查询操作日志详细
   *
   * @param operId 操作ID
   * @return 操作日志对象
   */
  @Override
  public SysOperLog selectOperLogById(Long operId) {
    return sysOperLogMapper.selectByPrimaryKey(operId);
  }

  /** 清空操作日志 */
  @Override
  public void cleanOperLog() {}
}
