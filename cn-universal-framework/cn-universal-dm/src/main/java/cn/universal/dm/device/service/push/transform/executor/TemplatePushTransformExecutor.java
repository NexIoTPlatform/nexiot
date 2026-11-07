package cn.universal.dm.device.service.push.transform.executor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TemplatePushTransformExecutor {

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

  public Object execute(String templateContent, Map<String, Object> templateContext) {
    if (StrUtil.isBlank(templateContent)) {
      return templateContext.get("source");
    }

    String decodedContent = htmlDecode(templateContent);
    try {
      Object jsonTemplate = JSONUtil.parse(decodedContent);
      return processValue(jsonTemplate, templateContext);
    } catch (Exception ignored) {
      return replacePlaceholders(decodedContent, templateContext);
    }
  }

  public void validate(String templateContent) {
    if (StrUtil.isBlank(templateContent)) {
      throw new IllegalArgumentException("模板内容不能为空");
    }
    JSONUtil.parse(htmlDecode(templateContent));
  }

  private Object processValue(Object value, Map<String, Object> templateContext) {
    if (value instanceof JSONObject jsonObject) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
        result.put(entry.getKey(), processValue(entry.getValue(), templateContext));
      }
      return result;
    }
    if (value instanceof JSONArray jsonArray) {
      List<Object> result = new ArrayList<>();
      for (Object item : jsonArray) {
        result.add(processValue(item, templateContext));
      }
      return result;
    }
    if (value instanceof String stringValue) {
      Matcher matcher = PLACEHOLDER_PATTERN.matcher(stringValue.trim());
      if (matcher.matches()) {
        return resolvePath(matcher.group(1), templateContext);
      }
      return replacePlaceholders(stringValue, templateContext);
    }
    return value;
  }

  private String replacePlaceholders(String content, Map<String, Object> templateContext) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      Object resolved = resolvePath(matcher.group(1), templateContext);
      matcher.appendReplacement(
          buffer, Matcher.quoteReplacement(resolved == null ? "" : String.valueOf(resolved)));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private Object resolvePath(String path, Map<String, Object> context) {
    String normalizedPath = StrUtil.trim(path);
    if (StrUtil.isBlank(normalizedPath)) {
      return null;
    }

    Object current = context;
    for (String pathSegment : normalizedPath.split("\\.")) {
      if (current == null) {
        return null;
      }
      if (current instanceof Map<?, ?> map) {
        current = map.get(pathSegment);
        continue;
      }
      if (current instanceof JSONObject jsonObject) {
        current = jsonObject.get(pathSegment);
        continue;
      }
      return null;
    }
    return current;
  }

  private String htmlDecode(String text) {
    if (text == null) {
      return null;
    }
    String current = text;
    for (int index = 0; index < 2; index++) {
      String previous = current;
      current =
          current
              .replace("&lt;", "<")
              .replace("&gt;", ">")
              .replace("&quot;", "\"")
              .replace("&#39;", "'")
              .replace("&amp;", "&");
      if (previous.equals(current)) {
        break;
      }
    }
    return current;
  }
}
