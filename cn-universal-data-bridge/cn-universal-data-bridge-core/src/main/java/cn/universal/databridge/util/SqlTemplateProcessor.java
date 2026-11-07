/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: SQL模板处理器
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 */

package cn.universal.databridge.util;

import cn.hutool.json.JSONUtil;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * SQL模板处理器 智能处理SQL模板中的变量替换，避免引号冲突
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Slf4j
public class SqlTemplateProcessor {

  /**
   * 处理SQL模板，智能处理引号冲突
   *
   * @param template SQL模板
   * @param variables 变量映射
   * @return 处理后的SQL
   */
  public static String processTemplate(String template, Map<String, Object> variables) {
    if (template == null || template.trim().isEmpty()) {
      return "";
    }

    // 使用正则表达式匹配所有 #{...} 占位符
    Pattern pattern = Pattern.compile("#\\{([a-zA-Z0-9_.]+)\\}");
    Matcher matcher = pattern.matcher(template);

    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String paramPath = matcher.group(1);
      Object value = getNestedValue(variables, paramPath);
      String replacement = convertToSqlValue(value, template, matcher.start());
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /**
   * 智能转换为SQL值，根据上下文选择合适的引号
   *
   * @param value 要转换的值
   * @param template 完整模板
   * @param position 当前位置
   * @return SQL安全格式的字符串
   */
  private static String convertToSqlValue(Object value, String template, int position) {
    if (value == null) {
      return "NULL";
    }

    // 检查上下文，判断是否在JSON字符串中
    boolean inJsonContext = isInJsonContext(template, position);

    if (inJsonContext) {
      // 在JSON上下文中，使用双引号
      return convertToJsonValue(value);
    } else {
      // 在普通SQL上下文中，使用单引号
      return SqlValueConverter.convertToSqlValue(value);
    }
  }

  /**
   * 检查当前位置是否在JSON字符串上下文中
   *
   * @param template 模板
   * @param position 位置
   * @return 是否在JSON上下文中
   */
  private static boolean isInJsonContext(String template, int position) {
    // 向前查找最近的引号
    int singleQuotePos = template.lastIndexOf("'", position);
    int doubleQuotePos = template.lastIndexOf("\"", position);

    // 如果最近的是双引号，说明在JSON上下文中
    if (doubleQuotePos > singleQuotePos) {
      return true;
    }

    // 检查是否在JSON对象中（包含大括号）
    String beforePosition = template.substring(0, position);
    int lastOpenBrace = beforePosition.lastIndexOf("{");
    int lastCloseBrace = beforePosition.lastIndexOf("}");

    // 如果最近的大括号是开括号，说明在JSON对象中
    if (lastOpenBrace > lastCloseBrace) {
      return true;
    }

    return false;
  }

  /**
   * 转换为JSON值
   *
   * @param value 要转换的值
   * @return JSON格式的字符串
   */
  private static String convertToJsonValue(Object value) {
    if (value == null) {
      return "null";
    }

    if (value instanceof String) {
      // 字符串：转义双引号
      String strValue = (String) value;
      return "\"" + strValue.replace("\"", "\\\"") + "\"";
    } else if (value instanceof Number || value instanceof Boolean) {
      // 数字和布尔类型直接转换
      return value.toString();
    } else {
      // 其他类型：转为JSON字符串
      return JSONUtil.toJsonStr(value);
    }
  }

  /**
   * 获取嵌套属性值
   *
   * @param variables 变量映射
   * @param path 属性路径
   * @return 属性值
   */
  @SuppressWarnings("unchecked")
  private static Object getNestedValue(Map<String, Object> variables, String path) {
    if (path == null || path.trim().isEmpty()) {
      return null;
    }

    String[] keys = path.split("\\.");
    Object current = variables;

    for (String key : keys) {
      if (current == null) {
        return null;
      }

      if (current instanceof Map) {
        current = ((Map<String, Object>) current).get(key);
      } else {
        return null;
      }
    }

    return current;
  }
}
