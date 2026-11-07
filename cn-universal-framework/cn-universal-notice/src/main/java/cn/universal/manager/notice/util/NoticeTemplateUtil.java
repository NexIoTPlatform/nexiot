package cn.universal.manager.notice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoticeTemplateUtil {

  // 支持 #xxx 和 #{xxx} 两种格式（全部用普通字符串，不用字符串模板）
  private static final Pattern PARAM_PATTERN =
      Pattern.compile("#([a-zA-Z0-9_]+)|#\\{([a-zA-Z0-9_]+)\\}");

  // 支持嵌套属性访问，如 #{properties.meterNo}、#{deviceName} 等
  private static final Pattern NESTED_PARAM_PATTERN = Pattern.compile("#\\{([a-zA-Z0-9_.]+)\\}");

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /** 替换模板中的参数，兼容 #xxx 和 #{xxx} 两种格式 */
  public static String replaceParams(String template, Map<String, Object> params) {
    if (template == null || params == null) {
      return template;
    }
    Matcher matcher = PARAM_PATTERN.matcher(template);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String paramName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
      Object value = params.get(paramName);
      String replacement = value != null ? value.toString() : "";
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /** 替换模板中的参数，支持嵌套属性访问，如 #{properties.meterNo}、#{deviceName} 等 */
  public static String replaceNestedParams(String template, Map<String, Object> params) {
    if (template == null || params == null) {
      return template;
    }
    Matcher matcher = NESTED_PARAM_PATTERN.matcher(template);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String paramPath = matcher.group(1);
      Object value = getNestedValue(params, paramPath);
      String replacement = value != null ? value.toString() : "";
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /** 获取嵌套属性值，支持如 properties.meterNo 这样的路径 */
  @SuppressWarnings("unchecked")
  private static Object getNestedValue(Map<String, Object> params, String path) {
    if (path == null || path.trim().isEmpty()) {
      return null;
    }

    if (params != null && params.containsKey(path)) return params.get(path);

    String[] keys = path.split("\\.");
    Object current = params;

    for (String key : keys) {
      if (current == null) {
        return null;
      }

      if (current instanceof Map) {
        current = ((Map<String, Object>) current).get(key);
      } else {
        // 如果不是Map类型，无法继续访问嵌套属性
        return null;
      }
    }

    return current;
  }

  /** 解析JSON字符串为Map */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> parseJson(String json) {
    try {
      if (json == null || json.trim().isEmpty()) {
        return Map.of();
      }
      return objectMapper.readValue(json, Map.class);
    } catch (Exception e) {
      return Map.of();
    }
  }

  /** 将对象转换为JSON字符串 */
  public static String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      return "{}";
    }
  }
}
