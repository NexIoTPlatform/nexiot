package cn.universal.core.engine.extend.method;

import cn.hutool.json.JSONObject;
import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.extend.IdeMagicFunction;
import cn.universal.core.engine.functions.ExtensionMethod;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Hutool JSONObject 类型转换
 *
 * @author NexIoT
 * @version 1.0
 * @since 2025/12/8 16:57
 */
@Component
public class JSONObjectExtension implements ExtensionMethod, IdeMagicFunction {

  @Override
  public Class<?> support() {
    return JSONObject.class;
  }

  /**
   * 必须是public static 修饰,参数至少有一个,且第一个参数必须为support方法返回的类型
   *
   * <p>以将字符串转为int为例: 最终调用时使用stringValue.toInt()调用
   *
   * <p>该方法第一个参数会自动被传入,所以调用时无需传入
   */
  @Comment("将JSONObject的直接子节点转为Map<key, JSONObject>")
  public static Map<String, Object> toMap(JSONObject object) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (object == null) {
      return map;
    }

    object.forEach(
        (k, v) -> {
          if (k == null) {
            return;
          }

          map.put(k, v);
        });
    return map;
  }

  @Comment("获取指定key的值，若不存在返回默认值")
  public static Object getOr(JSONObject object, String key, Object defaultValue) {
    if (object == null || key == null) {
      return defaultValue;
    }
    return object.getOrDefault(key, defaultValue);
  }

  @Comment("获取字符串，缺省返回空串")
  public static String getStr(JSONObject object, String key) {
    return object == null ? "" : Objects.toString(object.getStr(key), "");
  }

  @Comment("获取长整型，缺省返回0")
  public static Long getLong(JSONObject object, String key) {
    return object == null ? 0L : object.getLong(key, 0L);
  }

  @Comment("获取双精度，缺省返回0.0")
  public static Double getDouble(JSONObject object, String key) {
    return object == null ? 0.0 : object.getDouble(key, 0.0);
  }

  @Comment("获取布尔，缺省返回false")
  public static Boolean getBool(JSONObject object, String key) {
    return object == null ? Boolean.FALSE : object.getBool(key, Boolean.FALSE);
  }

  @Comment("按路径获取嵌套值, path支持a.b.c")
  public static Object getByPath(JSONObject object, String path) {
    if (object == null || path == null || path.isEmpty()) {
      return null;
    }
    String[] parts = path.split("\\.");
    Object current = object;
    for (String part : parts) {
      if (!(current instanceof JSONObject json) || part.isEmpty()) {
        return null;
      }
      current = json.get(part);
    }
    return current;
  }
}
