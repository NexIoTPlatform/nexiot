package cn.universal.core.metadata;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Keeps command JSON values consistent with the current product's function input types. */
public final class FunctionParameterNormalizer {

  private FunctionParameterNormalizer() {}

  public static List<JSONObject> functions(String metadata) {
    if (metadata == null || metadata.isBlank()) return List.of();
    return objects(JSONUtil.parseObj(metadata).get("functions"));
  }

  public static Map<String, Object> normalize(
      String metadata, String functionId, Map<String, Object> values) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (values != null) result.putAll(values);
    for (JSONObject function : functions(metadata)) {
      if (!Objects.equals(functionId, function.getStr("id"))) continue;
      normalizeFields(objects(function.get("inputs")), result);
      break;
    }
    return result;
  }

  private static void normalizeFields(List<JSONObject> fields, Map<String, Object> values) {
    for (JSONObject field : fields) {
      String id = field.getStr("id");
      if (!values.containsKey(id)) continue;
      try {
        values.put(id, normalizeValue(field.getOrDefault("valueType", field.get("type")), values.get(id)));
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("功能参数 " + id + " 类型不匹配: " + ex.getMessage(), ex);
      }
    }
  }

  /** Unknown types/fields are preserved; known invalid values fail instead of being sent silently. */
  public static Object normalizeValue(Object valueType, Object value) {
    if (JSONUtil.isNull(value)) return null;
    JSONObject schema = schema(valueType);
    String type = schema.getStr("type", "").toLowerCase(Locale.ROOT);
    try {
      return switch (type) {
        case "int", "integer" -> number(value).intValueExact();
        case "short" -> number(value).shortValueExact();
        case "byte" -> number(value).byteValueExact();
        case "long" -> number(value).longValueExact();
        case "float", "double", "decimal", "number" -> number(value);
        case "boolean", "bool" -> booleanValue(value);
        case "string", "text", "password" -> value instanceof String ? value : String.valueOf(value);
        case "enum" -> enumValue(schema, value);
        case "object" -> objectValue(schema, value);
        case "array" -> arrayValue(schema, value);
        default -> value;
      };
    } catch (ArithmeticException | NumberFormatException ex) {
      throw new IllegalArgumentException("需要有效的 " + type + " 值", ex);
    }
  }

  private static BigDecimal number(Object value) {
    if (!(value instanceof String) && !(value instanceof Number)) {
      throw new IllegalArgumentException("需要数值");
    }
    return new BigDecimal(value.toString().trim());
  }

  private static boolean booleanValue(Object value) {
    if (value instanceof Boolean bool) return bool;
    String text = value.toString().trim();
    if ("true".equalsIgnoreCase(text) || "1".equals(text)) return true;
    if ("false".equalsIgnoreCase(text) || "0".equals(text)) return false;
    throw new IllegalArgumentException("需要 boolean 值");
  }

  private static Object enumValue(JSONObject schema, Object value) {
    if (Boolean.TRUE.equals(schema.getBool("multi")) && value instanceof List<?> values) {
      return values.stream().map(item -> enumScalar(schema, item)).toList();
    }
    return enumScalar(schema, value);
  }

  private static Object enumScalar(JSONObject schema, Object value) {
    Object underlying = schema.getOrDefault("valueType", schema.get("dataType"));
    if (underlying != null && !"enum".equals(schema(underlying).getStr("type"))) {
      return normalizeValue(underlying, value);
    }
    for (JSONObject element : objects(schema.get("elements"))) {
      Object candidate = element.get("value");
      if (candidate != null && candidate.toString().equals(String.valueOf(value))) return candidate;
    }
    return value;
  }

  private static Map<String, Object> objectValue(JSONObject schema, Object value) {
    Object parsed = value instanceof String text ? JSONUtil.parseObj(text) : value;
    if (!(parsed instanceof Map<?, ?> map)) throw new IllegalArgumentException("需要 object 值");
    Map<String, Object> result = new LinkedHashMap<>();
    map.forEach((key, item) -> result.put(String.valueOf(key), item));
    normalizeFields(objects(schema.get("properties")), result);
    return result;
  }

  private static List<Object> arrayValue(JSONObject schema, Object value) {
    Object parsed = value instanceof String text ? JSONUtil.parseArray(text) : value;
    if (!(parsed instanceof List<?> list)) throw new IllegalArgumentException("需要 array 值");
    List<Object> result = new ArrayList<>();
    for (Object item : list) result.add(normalizeValue(schema.get("elementType"), item));
    return result;
  }

  private static JSONObject schema(Object value) {
    if (value == null) return new JSONObject();
    if (value instanceof String text && !text.stripLeading().startsWith("{")) {
      return new JSONObject().set("type", text);
    }
    return JSONUtil.parseObj(value);
  }

  private static List<JSONObject> objects(Object value) {
    if (value == null) return List.of();
    JSONArray array = JSONUtil.parseArray(value);
    List<JSONObject> result = new ArrayList<>();
    for (Object item : array) if (item instanceof Map<?, ?>) result.add(JSONUtil.parseObj(item));
    return result;
  }
}
