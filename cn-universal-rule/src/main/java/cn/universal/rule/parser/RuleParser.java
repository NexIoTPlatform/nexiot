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

package cn.universal.rule.parser;

import cn.universal.rule.enums.ParserFormat;
import cn.universal.rule.model.RuleParserResult;
import java.util.List;

/**
 * 规则解析器 @Author gitee.com/NexIoT
 *
 * @since 2025/12/3 9:07
 */
public interface RuleParser {

  /**
   * 解析指定格式的模型字符为规则模型
   *
   * @param format 模型格式
   * @param modelDefineString 字符模型
   * @return 规则模型
   */
  RuleParserResult parse(ParserFormat format, String modelDefineString);

  /**
   * @return 全部支持的模型格式
   */
  List<ParserFormat> getAllSupportFormat();
}
