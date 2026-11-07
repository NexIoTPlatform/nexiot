/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * @Description: 数据桥接核心引擎 - 模板处理组件
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 */

package cn.universal.databridge.engine;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 轻量级模板引擎实现
 *
 * <p>提供数据桥接场景下的模板渲染能力,支持变量替换和JSON属性构建。 主要用于数据转换、消息格式化等场景。
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component
@Slf4j
public class SimpleTemplateEngine implements TemplateEngine {

  @Override
  public String process(String template, Map<String, Object> variables) {
    if (StrUtil.isBlank(template) || MapUtil.isEmpty(variables)) {
      return template;
    }

    String result = template;

    // 替换 {variable} 格式的变量
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      String placeholder = "{" + entry.getKey() + "}";
      String value = entry.getValue() != null ? entry.getValue().toString() : "";
      result = result.replace(placeholder, value);
    }

    // 处理JSON格式的属性
    if (result.contains("{properties}")) {
      String propertiesJson = buildPropertiesJson(variables);
      result = result.replace("{properties}", propertiesJson);
    }

    return result;
  }

  /** 构建属性JSON */
  private String buildPropertiesJson(Map<String, Object> variables) {
    Map<String, Object> properties = new HashMap<>();

    variables.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith("property_"))
        .forEach(
            entry -> {
              String key = entry.getKey().substring("property_".length());
              properties.put(key, entry.getValue());
            });

    return JSONUtil.toJsonStr(properties);
  }

  @Override
  public Boolean validateTemplate(String template) {
    if (StrUtil.isBlank(template)) {
      return false;
    }

    try {
      // 检查模板语法
      Pattern pattern = Pattern.compile("\\{[^}]+\\}");
      Matcher matcher = pattern.matcher(template);

      while (matcher.find()) {
        String placeholder = matcher.group();
        // 这里可以添加更复杂的验证逻辑
      }

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public List<String> getSupportedTypes() {
    return List.of("SQL", "JSON", "TEXT");
  }
}
