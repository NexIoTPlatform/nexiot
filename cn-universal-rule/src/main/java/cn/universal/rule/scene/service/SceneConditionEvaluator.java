package cn.universal.rule.scene.service;

import cn.universal.persistence.entity.bo.TriggerBO.ConditionFilter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 固定操作符直接求值，不在每条设备消息中拼接、编译表达式。 */
public final class SceneConditionEvaluator {
  private SceneConditionEvaluator() {}

  /** Event conditions apply together to one occurrence; an omitted condition keeps legacy behavior. */
  public static boolean event(List<ConditionFilter> filters, Map<String, Object> data) {
    if (filters == null || filters.isEmpty()) return true;
    if (data == null) return false;
    for (ConditionFilter filter : filters) {
      if (filter == null
          || !compare(valueAt(data, filter.getKey()), filter.getValue(), filter.getOperator()))
        return false;
    }
    return true;
  }

  /** Exact keys take precedence; dot paths support nested vendor data and array indexes. */
  public static Object valueAt(Map<String, Object> data, String key) {
    if (data == null || key == null || key.isBlank()) return null;
    if (data.containsKey(key)) return data.get(key);
    // The uplink envelope exposes event fields under data; accept that prefix in editor paths.
    String path = key.startsWith("data.") ? key.substring("data.".length()) : key;
    if (!data.containsKey(path)) {
      // Old video rules used alarmType; only the event-data root has this compatibility alias.
      if ("alarmType".equals(path)) return data.get("cameraEventType");
      if ("cameraEventType".equals(path)) return data.get("alarmType");
      String head = path.split("\\.", 2)[0];
      if (!data.containsKey(head) && "IMOULIFE".equalsIgnoreCase(String.valueOf(data.get("platformType")))
          && data.get("outputData") instanceof Map<?, ?> output) return descend(output, path);
    }
    return valueAt(data, key, "data.");
  }

  private static Object valueAt(Map<String, Object> values, String key, String prefix) {
    if (values == null || key == null || key.isBlank()) return null;
    if (values.containsKey(key)) return values.get(key);
    String path = key.startsWith(prefix) ? key.substring(prefix.length()) : key;
    if (values.containsKey(path)) return values.get(path);
    return descend(values, path);
  }

  private static Object descend(Object root, String path) {
    Object value = root;
    String[] segments = path.split("\\.", -1);
    int offset = 0;
    for (int at = 0; at < segments.length; at++) {
      String segment = segments[at];
      if (value instanceof Map<?, ?> map) {
        // A vendor may use dots in a literal field ID, including inside outputData objects.
        String remaining = path.substring(offset);
        if (map.containsKey(remaining)) return map.get(remaining);
        value = map.get(segment);
      }
      else if (value instanceof List<?> list && segment.matches("[0-9]{1,6}")) {
        int index = Integer.parseInt(segment);
        value = index < list.size() ? list.get(index) : null;
      } else return null;
      if (value == null) return null;
      offset += segment.length() + 1;
    }
    return value;
  }

  public static boolean properties(
      List<ConditionFilter> filters, Map<String, Object> values, boolean any) {
    if (filters == null || filters.isEmpty() || values == null) return false;
    for (ConditionFilter filter : filters) {
      boolean matched =
          filter != null
              && compare(valueAt(values, filter.getKey(), "properties."),
                  filter.getValue(), filter.getOperator());
      if (any && matched) return true;
      if (!any && !matched) return false;
    }
    return !any;
  }

  public static boolean compare(Object actual, String expected, String operator) {
    if (actual == null || expected == null || operator == null) return false;
    String left = String.valueOf(actual);
    if ("like".equals(operator)) return left.contains(expected);
    if ("in".equals(operator) || "nin".equals(operator)) {
      // The scene editor stores candidate values as a comma-separated list.
      boolean contains = java.util.Arrays.stream(expected.split(",", -1))
          .map(String::trim).anyMatch(candidate -> left.equals(candidate));
      return "in".equals(operator) == contains;
    }
    BigDecimal a = number(left);
    BigDecimal b = number(expected);
    boolean equal = a != null && b != null ? a.compareTo(b) == 0 : Objects.equals(left, expected);
    return switch (operator) {
      case "eq" -> equal;
      case "not", "ne" -> !equal;
      case "gt" -> a != null && b != null && a.compareTo(b) > 0;
      case "gte" -> a != null && b != null && a.compareTo(b) >= 0;
      case "lt" -> a != null && b != null && a.compareTo(b) < 0;
      case "lte" -> a != null && b != null && a.compareTo(b) <= 0;
      default -> false;
    };
  }

  private static BigDecimal number(String value) {
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
