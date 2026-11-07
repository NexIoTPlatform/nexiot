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

package cn.universal.persistence.mapper;

import cn.universal.persistence.entity.RulegoChainLog;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

/**
 * rulego规则链执行日志Mapper接口
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/15
 */
public interface RulegoChainLogMapper extends Mapper<RulegoChainLog> {

  /**
   * 根据rulegoId查询执行日志
   *
   * @param rulegoId rulego规则链ID
   * @param limit 限制条数
   * @return 执行日志列表
   */
  List<RulegoChainLog> selectByRulegoId(
      @Param("rulegoId") String rulegoId, @Param("limit") Integer limit);

  /**
   * 根据执行ID查询日志
   *
   * @param executionId 执行ID
   * @return 执行日志
   */
  RulegoChainLog selectByExecutionId(@Param("executionId") String executionId);
}
