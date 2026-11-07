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

package cn.universal.rule.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * todo @Author gitee.com/NexIoT
 *
 * @since 2025/12/3 13:47
 */
@Component
public class RuleFunctionTemplate {

  private final Map<String, RuleFunction> ruleFunctionMap;

  public RuleFunctionTemplate(List<RuleFunction> functions) {
    this.ruleFunctionMap =
        functions.stream()
            .collect(Collectors.toMap(RuleFunction::functionName, Function.identity()));
  }

  public Object executeFunction(String functionName, Object[] params) {
    return Optional.ofNullable(ruleFunctionMap.get(functionName))
        .map(fun -> fun.executeFunction(params))
        .orElseThrow(() -> new UnsupportedOperationException("不支持的函数:" + functionName));
  }

  public List<RuleFunction> getAllFunctions() {
    return new ArrayList<>(ruleFunctionMap.values());
  }
}
