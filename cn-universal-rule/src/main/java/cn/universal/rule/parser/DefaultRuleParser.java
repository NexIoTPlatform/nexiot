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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 默认规则解析器 @Author gitee.com/NexIoT
 *
 * @since 2025/12/3 9:07
 */
@Component
public class DefaultRuleParser implements RuleParser {

  private final Map<ParserFormat, RuleModelParserStrategy> strategyMap;

  public DefaultRuleParser(List<RuleModelParserStrategy> strategies) {
    strategyMap =
        strategies.stream()
            .collect(Collectors.toMap(RuleModelParserStrategy::getFormat, Function.identity()));
  }

  @Override
  public RuleParserResult parse(ParserFormat format, String modelDefineString) {
    return Optional.ofNullable(strategyMap.get(format))
        .map(strategy -> strategy.parse(modelDefineString))
        .orElseThrow(() -> new UnsupportedOperationException("不支持的模型格式:" + format));
  }

  @Override
  public List<ParserFormat> getAllSupportFormat() {
    return new ArrayList<>(strategyMap.keySet());
  }
}
