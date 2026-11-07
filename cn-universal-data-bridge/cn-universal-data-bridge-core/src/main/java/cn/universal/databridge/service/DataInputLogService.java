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

package cn.universal.databridge.service;

import cn.universal.databridge.entity.DataInputLog;
import cn.universal.databridge.mapper.DataInputLogMapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据输入日志服务
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Service
@Slf4j
public class DataInputLogService {

  @Resource private DataInputLogMapper dataInputLogMapper;

  /** 保存日志 */
  @Transactional(rollbackFor = Exception.class)
  public void save(DataInputLog log) {
    if (log.getCreateTime() == null) {
      log.setCreateTime(LocalDateTime.now());
    }
    dataInputLogMapper.insertSelective(log);
  }

  /** 根据配置ID查询日志 */
  public List<DataInputLog> getByConfigId(Long configId) {
    DataInputLog condition = new DataInputLog();
    condition.setConfigId(configId);
    return dataInputLogMapper.select(condition);
  }

  /** 查询最近的日志 */
  public List<DataInputLog> getRecentLogs(Long configId, int limit) {
    return dataInputLogMapper.selectRecentLogs(configId, limit);
  }

  /** 统计成功率 */
  public Double getSuccessRate(Long configId, LocalDateTime startTime, LocalDateTime endTime) {
    return dataInputLogMapper.calculateSuccessRate(configId, startTime, endTime);
  }

  /** 删除过期日志 */
  @Transactional(rollbackFor = Exception.class)
  public int deleteExpiredLogs(LocalDateTime expireTime) {
    return dataInputLogMapper.deleteExpiredLogs(expireTime);
  }
}
