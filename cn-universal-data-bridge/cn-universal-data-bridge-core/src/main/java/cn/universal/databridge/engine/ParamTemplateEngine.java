package cn.universal.databridge.engine;

import cn.hutool.core.util.StrUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 参数化模板引擎：支持 #{path} 值占位 所有值均以 '?' 参数占位绑定，避免引号与转义问题 */
public class ParamTemplateEngine {

  private static final Pattern TEMPLATE_PATTERN =
      Pattern.compile("#\\{([a-zA-Z0-9_.]+)}|\\$\\{(json|json_text)\\((.*?)\\)}", Pattern.DOTALL);

  public ParamSql process(
      String template, Map<String, Object> variables, SqlDialectAdapter adapter) {
    if (StrUtil.isBlank(template)) {
      return new ParamSql("", List.of());
    }
    List<Object> params = new ArrayList<>();
    Matcher m = TEMPLATE_PATTERN.matcher(template);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String varPath = m.group(1);
      String macroName = m.group(2);
      String replacement;
      if (varPath != null) {
        Object paramValue = convertValueForDatabase(getNestedValue(variables, varPath), varPath);
        params.add(paramValue);
        replacement = jsonParameterPlaceholder(varPath, paramValue, adapter);
      } else {
        replacement = processJsonMacro(macroName, m.group(3), variables, adapter, params);
      }
      m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    m.appendTail(sb);
    return new ParamSql(sb.toString(), params);
  }

  private String processJsonMacro(
      String macroName,
      String argumentsText,
      Map<String, Object> variables,
      SqlDialectAdapter adapter,
      List<Object> params) {
    List<String> arguments = splitArguments(argumentsText);
    if (arguments.isEmpty() || arguments.size() % 2 != 0) {
      throw new IllegalArgumentException("invalid " + macroName + " macro arguments: " + argumentsText);
    }
    List<String> keys = new ArrayList<>(arguments.size() / 2);
    for (int index = 0; index < arguments.size(); index += 2) {
      keys.add(unquote(arguments.get(index)));
      String valuePath = arguments.get(index + 1).trim();
      params.add(convertValueForDatabase(getNestedValue(variables, valuePath), valuePath));
    }
    if ("json_text".equals(macroName)) {
      return adapter.jsonTextFragment(keys, keys.size());
    }
    return adapter.jsonFragment(keys, keys.size());
  }

  private List<String> splitArguments(String text) {
    List<String> result = new ArrayList<>();
    if (text == null || text.isBlank()) {
      return result;
    }
    StringBuilder current = new StringBuilder();
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    for (int index = 0; index < text.length(); index++) {
      char ch = text.charAt(index);
      if (ch == '\'' && !inDoubleQuote) {
        inSingleQuote = !inSingleQuote;
        current.append(ch);
        continue;
      }
      if (ch == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
        current.append(ch);
        continue;
      }
      if (ch == ',' && !inSingleQuote && !inDoubleQuote) {
        result.add(current.toString().trim());
        current.setLength(0);
        continue;
      }
      current.append(ch);
    }
    if (!current.isEmpty()) {
      result.add(current.toString().trim());
    }
    return result;
  }

  private String unquote(String value) {
    String trimmed = value == null ? "" : value.trim();
    if (trimmed.length() >= 2) {
      char first = trimmed.charAt(0);
      char last = trimmed.charAt(trimmed.length() - 1);
      if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
        return trimmed.substring(1, trimmed.length() - 1).replace("\\'", "'").replace("\\\"", "\"");
      }
    }
    return trimmed;
  }

  @SuppressWarnings("unchecked")
  private Object getNestedValue(Map<String, Object> variables, String path) {
    if (path == null || path.isEmpty()) return null;
    String[] keys = path.split("\\.");
    Object curr = variables;
    for (String k : keys) {
      if (curr == null) return null;
      if (curr instanceof Map) {
        curr = ((Map<String, Object>) curr).get(k);
      } else {
        return null;
      }
    }
    // 若直接访问 properties 或 data，返回 JSON 字符串，避免各库的 JSON 绑定差异
    if (curr instanceof Map && ("properties".equals(path) || "data".equals(path))) {
      return cn.hutool.json.JSONUtil.toJsonStr(curr);
    }
    return curr;
  }

  private String jsonParameterPlaceholder(String path, Object paramValue, SqlDialectAdapter adapter) {
    if (("properties".equals(path) || "data".equals(path) || "rawData".equals(path))
        && paramValue instanceof String strValue
        && strValue.trim().startsWith("{")
        && strValue.trim().endsWith("}")) {
      return adapter.wrapJsonParameter();
    }
    return "?";
  }

  /**
   * 将值转换为适合数据库存储的格式
   *
   * @param value 原始值
   * @param path 变量路径
   * @return 转换后的值
   */
  private Object convertValueForDatabase(Object value, String path) {
    if (value == null) {
      return null;
    }

    if (value instanceof java.util.Date) {
      return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
    }

    if (value instanceof Number || value instanceof Boolean || value instanceof String) {
      return value;
    }

    // Map 类型序列化为 JSON
    if (value instanceof Map) {
      return cn.hutool.json.JSONUtil.toJsonStr(value);
    }

    // 其他类型默认转为字符串
    return value.toString();
  }
}
