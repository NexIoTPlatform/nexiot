package cn.universal.core.engine.extend.convert;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import cn.universal.core.engine.extend.IdeMagicFunction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 常用类型/格式转换函数。所有转换方法以 to 开头。 */
@Component
public class ConvertFunctions implements IdeMagicFunction {

  // =====================
  // Base64 转换（新方法 to 开头，保留旧方法兼容）
  // =====================

  @Function
  @Comment("Base64解码（新方法）")
  public String toBase64Decode(@Comment(name = "value", value = "目标对象") String payload) {
    return Base64.decodeStr(payload);
  }

  @Function
  @Comment("base64解码（兼容旧方法）")
  public String base64Decode(@Comment(name = "value", value = "目标对象") String payload) {
    return toBase64Decode(payload);
  }

  @Function
  @Comment("Base64编码（新方法）")
  public String toBase64Encode(@Comment(name = "value", value = "目标对象") String payload) {
    return Base64.encode(payload);
  }

  @Function
  @Comment("base64编码（兼容旧方法）")
  public String base64Encode(@Comment(name = "value", value = "目标对象") String payload) {
    return toBase64Encode(payload);
  }

  // =====================
  // JSON 转换（新方法 to 开头，保留旧方法兼容）
  // =====================

  @Function
  @Comment("转JSON对象（新方法）")
  public Object toJson(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return null;
    }
    // 已是JSON对象或Map，直接返回
    if (payload instanceof JSONObject || payload instanceof Map) {
      return payload;
    }
    // 字符串则尝试解析为JSON对象
    if (payload instanceof String) {
      String s = (String) payload;
      if (JSONUtil.isTypeJSON(s)) {
        return JSONUtil.parseObj(s);
      }
    }
    return payload;
  }

  @Function
  @Comment("字符串转JSON数组（新方法）")
  public Object toJsonArray(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return null;
    }
    // 已是JSON数组或List，直接返回
    if (payload instanceof JSONArray || payload instanceof List) {
      return payload;
    }
    // 字符串则尝试解析为JSON数组
    if (payload instanceof String) {
      String s = (String) payload;
      if (JSONUtil.isTypeJSONArray(s)) {
        return JSONUtil.parseArray(s);
      }
    }
    return payload;
  }

  @Function
  @Comment("JSON转字符串（新方法）")
  public String toJsonString(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return null;
    }
    if (payload instanceof JSONObject
        || payload instanceof JSONArray
        || payload instanceof Map
        || payload instanceof List) {
      return JSONUtil.toJsonStr(payload);
    }
    if (JSONUtil.isTypeJSON(payload + "")) {
      return JSONUtil.toJsonStr(payload);
    }
    return payload + "";
  }

  @Function
  @Comment("JSON转字符串（兼容旧方法）")
  public String toJsonStr(@Comment(name = "value", value = "目标对象") Object payload) {
    return toJsonString(payload);
  }

  @Function
  @Comment("JSON数组转字符串（新方法）")
  public String toJsonArrayString(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return null;
    }
    if (payload instanceof JSONArray || payload instanceof List) {
      return JSONUtil.toJsonStr(payload);
    }
    if (JSONUtil.isTypeJSONArray(payload + "")) {
      return JSONUtil.toJsonStr(payload);
    }
    return payload + "";
  }

  @Function
  @Comment("JSON数组转字符串（兼容旧方法）")
  public String jsonArrayToStr(@Comment(name = "value", value = "目标对象") Object payload) {
    return toJsonArrayString(payload);
  }

  // =====================
  // Map/List 转换
  // =====================

  @Function
  @Comment("对象转Map")
  @SuppressWarnings("unchecked")
  public Map<String, Object> toMap(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return new LinkedHashMap<>();
    }
    if (payload instanceof Map) {
      return (Map<String, Object>) payload;
    }
    if (payload instanceof JSONObject) {
      Map<String, Object> map = ((JSONObject) payload).toBean(HashMap.class);
      return new LinkedHashMap<>(map);
    }
    if (payload instanceof String) {
      String s = (String) payload;
      if (JSONUtil.isTypeJSON(s)) {
        JSONObject obj = JSONUtil.parseObj(s);
        Map<String, Object> map = obj.toBean(HashMap.class);
        return new LinkedHashMap<>(map);
      }
    }
    return new LinkedHashMap<>();
  }

  @Function
  @Comment("对象转List")
  @SuppressWarnings("unchecked")
  public List<Object> toList(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return new ArrayList<>();
    }
    if (payload instanceof List) {
      return (List<Object>) payload;
    }
    if (payload instanceof JSONArray) {
      return ((JSONArray) payload).toList(Object.class);
    }
    if (payload instanceof String) {
      String s = (String) payload;
      if (JSONUtil.isTypeJSONArray(s)) {
        JSONArray arr = JSONUtil.parseArray(s);
        return arr.toList(Object.class);
      }
    }
    if (payload.getClass().isArray()) {
      List<Object> list = new ArrayList<>();
      int length = java.lang.reflect.Array.getLength(payload);
      for (int i = 0; i < length; i++) {
        list.add(java.lang.reflect.Array.get(payload, i));
      }
      return list;
    }
    return new ArrayList<>();
  }

  // =====================
  // 字符串转换
  // =====================

  @Function
  @Comment("对象转字符串")
  public String toString(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return null;
    }
    return payload.toString();
  }

  @Function
  @Comment("对象转字符串（默认空串）")
  public String toStringOrEmpty(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return "";
    }
    return payload.toString();
  }

  // =====================
  // 数字转换
  // =====================

  @Function
  @Comment("对象转整数")
  public Integer toInt(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return 0;
    }
    return Convert.toInt(payload, 0);
  }

  @Function
  @Comment("对象转整数（带默认值）")
  public Integer toInt(
      @Comment(name = "value", value = "目标对象") Object payload,
      @Comment(name = "defaultValue", value = "默认值") Integer defaultValue) {
    if (payload == null) {
      return defaultValue;
    }
    return Convert.toInt(payload, defaultValue);
  }

  @Function
  @Comment("对象转长整数")
  public Long toLong(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return 0L;
    }
    return Convert.toLong(payload, 0L);
  }

  @Function
  @Comment("对象转长整数（带默认值）")
  public Long toLong(
      @Comment(name = "value", value = "目标对象") Object payload,
      @Comment(name = "defaultValue", value = "默认值") Long defaultValue) {
    if (payload == null) {
      return defaultValue;
    }
    return Convert.toLong(payload, defaultValue);
  }

  @Function
  @Comment("对象转双精度浮点数")
  public Double toDouble(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return 0.0;
    }
    return Convert.toDouble(payload, 0.0);
  }

  @Function
  @Comment("对象转双精度浮点数（带默认值）")
  public Double toDouble(
      @Comment(name = "value", value = "目标对象") Object payload,
      @Comment(name = "defaultValue", value = "默认值") Double defaultValue) {
    if (payload == null) {
      return defaultValue;
    }
    return Convert.toDouble(payload, defaultValue);
  }

  @Function
  @Comment("对象转布尔值")
  public Boolean toBoolean(@Comment(name = "value", value = "目标对象") Object payload) {
    if (payload == null) {
      return false;
    }
    return Convert.toBool(payload, false);
  }

  @Function
  @Comment("对象转布尔值（带默认值）")
  public Boolean toBoolean(
      @Comment(name = "value", value = "目标对象") Object payload,
      @Comment(name = "defaultValue", value = "默认值") Boolean defaultValue) {
    if (payload == null) {
      return defaultValue;
    }
    return Convert.toBool(payload, defaultValue);
  }

  // =====================
  // 十六进制转换（新方法 to 开头，保留旧方法兼容）
  // =====================

  @Function
  @Comment("Base64转16进制（新方法）")
  public String toHexFromBase64(@Comment(name = "value", value = "目标对象") String payload) {
    if (StrUtil.isBlank(payload)) {
      return "";
    }
    return Convert.toHex(Base64.decode(payload));
  }

  @Function
  @Comment("base64转16进制（兼容旧方法）")
  public String base64ToHex(@Comment(name = "value", value = "目标对象") String payload) {
    return toHexFromBase64(payload);
  }

  @Function
  @Comment("16进制转Base64（新方法）")
  public String toBase64FromHex(@Comment(name = "value", value = "目标对象") String payload) {
    if (StrUtil.isBlank(payload)) {
      return "";
    }
    int len = payload.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(payload.charAt(i), 16) << 4)
                  + Character.digit(payload.charAt(i + 1), 16));
    }
    return Base64.encode(data);
  }

  @Function
  @Comment("16进制转base64（兼容旧方法）")
  public String hexToBase64(@Comment(name = "value", value = "目标对象") String payload) {
    return toBase64FromHex(payload);
  }

  @Function
  @Comment("字节数组转16进制字符串")
  public String toHex(@Comment(name = "value", value = "字节数组") byte[] payload) {
    if (payload == null) {
      return "";
    }
    return Convert.toHex(payload);
  }

  @Function
  @Comment("16进制字符串转字节数组")
  public byte[] toBytes(@Comment(name = "value", value = "16进制字符串") String payload) {
    if (StrUtil.isBlank(payload)) {
      return new byte[0];
    }
    String cleaned = payload.replaceAll("[^0-9A-Fa-f]", "");
    int len = cleaned.length();
    if (len % 2 != 0) {
      cleaned = "0" + cleaned;
      len = cleaned.length();
    }
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(cleaned.charAt(i), 16) << 4)
                  + Character.digit(cleaned.charAt(i + 1), 16));
    }
    return data;
  }
}
