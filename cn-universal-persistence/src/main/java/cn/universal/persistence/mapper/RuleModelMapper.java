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

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.RuleModel;
import cn.universal.persistence.entity.bo.RuleModelBO;
import cn.universal.persistence.entity.vo.RuleModelVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 规则模型Mapper @Author gitee.com/NexIoT
 *
 * @since 2023/1/13 14:31
 */
public interface RuleModelMapper extends BaseMapper<RuleModel> {

  /**
   * 查询设备相关的规则模型
   *
   * @param ruleModelBo
   * @return
   */
  List<RuleModel> selectRuleByBo(@Param("bo") RuleModelBO ruleModelBo);

  /**
   * 查询规则模型
   *
   * @param ruleModelBo
   * @return
   */
  List<RuleModelVO> selectRuleListByBo(@Param("bo") RuleModelBO ruleModelBo);
}
