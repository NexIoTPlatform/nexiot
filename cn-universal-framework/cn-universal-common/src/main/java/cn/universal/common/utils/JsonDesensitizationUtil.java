package cn.universal.common.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JSON脱敏工具类 用于检测和脱敏包含敏感关键字的字段 */
public class JsonDesensitizationUtil {

  /** 敏感字段关键字列表 */
  private static final List<String> SENSITIVE_KEYWORDS =
      Arrays.asList(
          "secret", "password", "token", "playinfo", "accesstoken", "refreshtoken", "clientSecret");

  /** 脱敏替换值 */
  private static final String MASK_VALUE = "***";

  /**
   * 对JSON对象进行脱敏处理
   *
   * @param jsonString JSON字符串
   * @return 脱敏后的JSON对象
   */
  public static JSONObject desensitize(String jsonString) {
    if (jsonString == null || jsonString.trim().isEmpty()) {
      return null;
    }

    try {
      JSONObject jsonObject = JSONUtil.parseObj(jsonString);
      return desensitizeJsonObject(jsonObject);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 对JSONObject进行脱敏处理
   *
   * @param jsonObject JSON对象
   * @return 脱敏后的JSON对象
   */
  public static JSONObject desensitizeJsonObject(JSONObject jsonObject) {
    if (jsonObject == null) {
      return null;
    }

    JSONObject result = new JSONObject();
    Iterator<Map.Entry<String, Object>> iterator = jsonObject.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<String, Object> entry = iterator.next();
      String key = entry.getKey();
      Object value = entry.getValue();

      // 检查key是否包含敏感关键字（忽略大小写）
      if (containsSecretKeyword(key)) {
        // 脱敏处理：只对非空值进行脱敏
        result.put(key, value != null && !String.valueOf(value).isEmpty() ? MASK_VALUE : value);
      } else if (value instanceof JSONObject) {
        // 递归处理嵌套的JSONObject
        result.put(key, desensitizeJsonObject((JSONObject) value));
      } else if (value instanceof JSONArray) {
        // 递归处理嵌套的JSONArray
        result.put(key, desensitizeJsonArray((JSONArray) value));
      } else if (value instanceof String text
          && ("configuration".equals(key) || "productConfiguration".equals(key))
          && (text.contains("sipPassword") || text.contains("sipEslPassword"))) {
        // Product/device configuration is often a JSON string rather than a nested object.
        JSONObject configuration = desensitize(text);
        result.put(key, configuration == null ? "{}" : configuration.toString());
      } else {
        // 直接复制其他值
        result.put(key, value);
      }
    }

    return result;
  }

  /**
   * 对JSONArray进行脱敏处理
   *
   * @param jsonArray JSON数组
   * @return 脱敏后的JSON数组
   */
  private static JSONArray desensitizeJsonArray(JSONArray jsonArray) {
    if (jsonArray == null) {
      return null;
    }

    JSONArray result = new JSONArray();
    for (Object item : jsonArray) {
      if (item instanceof JSONObject) {
        result.add(desensitizeJsonObject((JSONObject) item));
      } else if (item instanceof JSONArray) {
        result.add(desensitizeJsonArray((JSONArray) item));
      } else {
        result.add(item);
      }
    }

    return result;
  }

  /**
   * 检查字符串是否包含敏感关键字（忽略大小写）
   *
   * @param text 要检查的字符串
   * @return 是否包含敏感关键字
   */
  private static boolean containsSecretKeyword(String text) {
    if (text == null) {
      return false;
    }
    String lowerText = text.toLowerCase();
    // 检查是否包含任意一个敏感关键字
    return SENSITIVE_KEYWORDS.stream().anyMatch(lowerText::contains);
  }

  /**
   * 将Object转换为JSONObject并进行脱敏处理
   *
   * @param obj 要处理的对象
   * @return 脱敏后的JSONObject
   */
  public static JSONObject desensitizeObject(Object obj) {
    if (obj == null) {
      return null;
    }

    try {
      if (obj instanceof JSONObject) {
        return desensitizeJsonObject((JSONObject) obj);
      } else {
        // 先转换为JSON字符串，再解析为JSONObject
        String jsonString = JSONUtil.toJsonStr(obj);
        return desensitize(jsonString);
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 检查值是否为脱敏占位符，兼容***、****等纯星号写法
   *
   * @param value 要检查的值
   * @return 是否为脱敏占位符
   */
  public static boolean isMaskedValue(Object value) {
    if (!(value instanceof CharSequence)) {
      return false;
    }
    String text = value.toString().trim();
    if (text.length() < MASK_VALUE.length()) {
      return false;
    }
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) != '*') {
        return false;
      }
    }
    return true;
  }

  /**
   * 合并配置：将前端提交的配置与数据库原配置合并，如果新配置中某字段值为***，则保留原配置中的值
   *
   * @param newAuthJson 前端提交的新配置JSON字符串
   * @param oldAuthJson 数据库中的原配置JSON字符串
   * @return 合并后的配置JSON字符串
   */
  public static String mergeAuthConfig(String newAuthJson, String oldAuthJson) {
    if (newAuthJson == null || newAuthJson.trim().isEmpty()) {
      return oldAuthJson;
    }
    if (oldAuthJson == null || oldAuthJson.trim().isEmpty()) {
      return newAuthJson;
    }

    try {
      JSONObject newConfig = JSONUtil.parseObj(newAuthJson);
      JSONObject oldConfig = JSONUtil.parseObj(oldAuthJson);
      return mergeJsonObject(newConfig, oldConfig).toString();
    } catch (Exception e) {
      return newAuthJson;
    }
  }

  /**
   * 按指定字段范围替换配置：范围内字段以本次提交为准，未提交的字段会被删除；范围外字段保留原值。
   * 若字段值为***，则继续沿用原值。
   *
   * @param newAuthJson 前端提交的新配置JSON字符串
   * @param oldAuthJson 数据库中的原配置JSON字符串
   * @param scopedKeys 允许被本次替换的字段集合
   * @return 替换后的配置JSON字符串
   */
  public static String replaceScopedAuthConfig(
      String newAuthJson, String oldAuthJson, Collection<String> scopedKeys) {
    if (scopedKeys == null || scopedKeys.isEmpty()) {
      return mergeAuthConfig(newAuthJson, oldAuthJson);
    }

    JSONObject newConfig = parseOrEmpty(newAuthJson);
    JSONObject oldConfig = parseOrEmpty(oldAuthJson);
    return replaceJsonObjectScope(newConfig, oldConfig, scopedKeys).toString();
  }

  private static JSONObject parseOrEmpty(String json) {
    if (json == null || json.trim().isEmpty()) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(json);
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  private static JSONObject replaceJsonObjectScope(
      JSONObject newConfig, JSONObject oldConfig, Collection<String> scopedKeys) {
    Set<String> scopedKeySet = new LinkedHashSet<>(scopedKeys);
    JSONObject merged = new JSONObject();

    if (oldConfig != null) {
      for (Map.Entry<String, Object> entry : oldConfig.entrySet()) {
        if (!scopedKeySet.contains(entry.getKey())) {
          merged.put(entry.getKey(), entry.getValue());
        }
      }
    }

    if (newConfig == null) {
      return merged;
    }

    for (String key : scopedKeySet) {
      if (!newConfig.containsKey(key)) {
        continue;
      }
      Object newValue = newConfig.get(key);
      Object oldValue = oldConfig == null ? null : oldConfig.get(key);
      merged.put(key, mergeValue(newValue, oldValue));
    }

    return merged;
  }

  private static JSONObject mergeJsonObject(JSONObject newConfig, JSONObject oldConfig) {
    JSONObject merged = new JSONObject();

    for (Map.Entry<String, Object> entry : newConfig.entrySet()) {
      String key = entry.getKey();
      Object newValue = entry.getValue();
      Object oldValue = oldConfig == null ? null : oldConfig.get(key);
      merged.put(key, mergeValue(newValue, oldValue));
    }

    if (oldConfig != null) {
      for (Map.Entry<String, Object> entry : oldConfig.entrySet()) {
        String key = entry.getKey();
        if (!merged.containsKey(key)) {
          merged.put(key, entry.getValue());
        }
      }
    }

    return merged;
  }

  private static JSONArray mergeJsonArray(JSONArray newArray, JSONArray oldArray) {
    JSONArray merged = new JSONArray();
    for (int i = 0; i < newArray.size(); i++) {
      Object newValue = newArray.get(i);
      Object oldValue = oldArray != null && i < oldArray.size() ? oldArray.get(i) : null;
      merged.add(mergeValue(newValue, oldValue));
    }
    if (oldArray != null) {
      for (int i = newArray.size(); i < oldArray.size(); i++) {
        merged.add(oldArray.get(i));
      }
    }
    return merged;
  }

  private static Object mergeValue(Object newValue, Object oldValue) {
    if (isMaskedValue(newValue)) {
      return oldValue != null ? oldValue : newValue;
    }
    if (newValue instanceof JSONObject) {
      return mergeJsonObject(
          (JSONObject) newValue, oldValue instanceof JSONObject ? (JSONObject) oldValue : null);
    }
    if (newValue instanceof JSONArray) {
      return mergeJsonArray(
          (JSONArray) newValue, oldValue instanceof JSONArray ? (JSONArray) oldValue : null);
    }
    return newValue;
  }
}
