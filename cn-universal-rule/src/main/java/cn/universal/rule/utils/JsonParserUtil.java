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

package cn.universal.rule.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * json解析工具类 @Author gitee.com/NexIoT
 *
 * @since 2025/11/30 10:43
 */
public class JsonParserUtil {
  private static final Pattern INDEXED_FIELD = Pattern.compile("[^\\[\\]]+(?:\\[[0-9]{1,6}\\])+");
  private static final Pattern ARRAY_INDEX = Pattern.compile("\\[([0-9]{1,6})\\]");

  /**
   * 获取指定json节点数据
   *
   * @param json json数据
   * @param key 获取数据key
   * @param clazz 获取数据类型
   * @return T 数据
   */
  public static <T> T getValue(JSONObject json, String key, Class<T> clazz) {
    return Convert.convert(clazz, valueAt(json, key));
  }

  /** SQL 字段路径支持事件 data 中的对象和数组，厂商字面点键优先于嵌套路径。 */
  private static Object valueAt(Object root, String path) {
    if (path == null || path.isBlank()) return null;
    Object value = root;
    String[] segments = path.split("\\.", -1);
    int offset = 0;
    for (String segment : segments) {
      if (value instanceof Map<?, ?> map) {
        String remaining = path.substring(offset);
        if (map.containsKey(remaining)) return map.get(remaining);
        // Compatibility is limited to the envelope's event-data root, never vendor objects.
        if (offset == "data.".length() && path.startsWith("data.")
            && root instanceof Map<?, ?> envelope
            && envelope.get("event") instanceof String event && !event.isBlank()) {
          if ("alarmType".equals(remaining)) return map.get("cameraEventType");
          if ("cameraEventType".equals(remaining)) return map.get("alarmType");
          // Imou official outputs are stored only under outputData; old flattened rules still read them.
          String field = INDEXED_FIELD.matcher(segment).matches() ? segment.substring(0, segment.indexOf('[')) : segment;
          if (!map.containsKey(field) && !map.containsKey(segment)
              && "IMOULIFE".equalsIgnoreCase(String.valueOf(map.get("platformType")))
              && map.get("outputData") instanceof Map<?, ?> output) {
            if (output.containsKey(remaining)) return output.get(remaining);
            value = output.containsKey(segment) ? output.get(segment) : indexedField(output, segment);
            if (value == null) return null;
            offset += segment.length() + 1;
            continue;
          }
        }
        value = map.containsKey(segment) ? map.get(segment) : indexedField(map, segment);
      } else if (value instanceof List<?> list && segment.matches("[0-9]{1,6}")) {
        int index = Integer.parseInt(segment);
        value = index < list.size() ? list.get(index) : null;
      } else return null;
      if (value == null) return null;
      offset += segment.length() + 1;
    }
    return value;
  }

  private static Object indexedField(Map<?, ?> map, String segment) {
    if (!INDEXED_FIELD.matcher(segment).matches()) return null;
    int bracket = segment.indexOf('[');
    Object value = map.get(segment.substring(0, bracket));
    var indexes = ARRAY_INDEX.matcher(segment.substring(bracket));
    while (indexes.find()) {
      if (!(value instanceof List<?> list)) return null;
      int index = Integer.parseInt(indexes.group(1));
      if (index >= list.size()) return null;
      value = list.get(index);
    }
    return value;
  }

  /**
   * 获取指定json节点数据
   *
   * @param json json数据
   * @param key 获取数据key
   * @param clazz 获取数据类型
   * @return T 数据
   */
  public static <T> T getValue(String json, String key, Class<T> clazz) {
    return getValue(JSONUtil.parseObj(json), key, clazz);
  }

  /**
   * 获取指定json节点数据
   *
   * @param json json数据
   * @param key 获取数据key
   * @return 数据
   */
  public static Object getValue(JSONObject json, String key) {
    return getValue((Map<?, ?>) json, key);
  }

  /** WHERE 解析函数与 SELECT 投影共用同一条安全字段路径。 */
  public static Object getValue(Map<?, ?> json, String key) {
    try {
      return Convert.convert(Object.class, valueAt(json, key.replace("\"", "").replace("'", "")));
    } catch (Exception e) {
      return null;
    }
  }

  public static void main(String[] args) {
    JSONObject set =
        new JSONObject().set("a", new JSONObject().set("b", new JSONObject().set("c", "2")));
    Integer value = getValue(set, "a.b.c", Integer.class);
    System.out.println(value);
  }
}
