package cn.universal.core.engine.extend;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 类型判断函数，所有方法以 is 开头。 */
@Component
public class TypeCheckFunctions implements IdeMagicFunction {

  @Function
  @Comment("判断是否是json字符串")
  public Boolean isJson(@Comment(name = "value", value = "目标对象") String payload) {
    try {
      return JSONUtil.isTypeJSON(payload);
    } catch (Exception e) {
      return false;
    }
  }

  @Function
  @Comment("判断是否是json数组")
  public Boolean isJsonArray(@Comment(name = "value", value = "目标对象") String payload) {
    try {
      return JSONUtil.isTypeJSONArray(payload);
    } catch (Exception e) {
      return false;
    }
  }

  @Function
  @Comment("判断对象是否为JSONObject")
  public Boolean isJsonObject(@Comment(name = "value", value = "目标对象") Object payload) {
    return payload instanceof JSONObject;
  }

  @Function
  @Comment("判断对象是否为Map")
  public Boolean isMap(@Comment(name = "value", value = "目标对象") Object payload) {
    return payload instanceof Map;
  }

  @Function
  @Comment("判断对象是否为List或Array")
  public Boolean isList(@Comment(name = "value", value = "目标对象") Object payload) {
    return payload instanceof Collection || (payload != null && payload.getClass().isArray());
  }

  @Function
  @Comment("判断字符串是否为空或null")
  public Boolean isEmpty(@Comment(name = "value", value = "目标对象") String payload) {
    return StrUtil.isBlank(payload);
  }

  @Function
  @Comment("判断字符串是否非空")
  public Boolean isNotEmpty(@Comment(name = "value", value = "目标对象") String payload) {
    return StrUtil.isNotBlank(payload);
  }

  @Function
  @Comment("判断是否为数字字符串")
  public Boolean isNumber(@Comment(name = "value", value = "目标对象") String payload) {
    if (StrUtil.isBlank(payload)) {
      return false;
    }
    try {
      Double.parseDouble(payload.trim());
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Function
  @Comment("判断是否为整数字符串")
  public Boolean isInteger(@Comment(name = "value", value = "目标对象") String payload) {
    if (StrUtil.isBlank(payload)) {
      return false;
    }
    try {
      Integer.parseInt(payload.trim());
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Function
  @Comment("判断是否为布尔字符串")
  public Boolean isBoolean(@Comment(name = "value", value = "目标对象") String payload) {
    if (StrUtil.isBlank(payload)) {
      return false;
    }
    String trimmed = payload.trim().toLowerCase();
    return "true".equals(trimmed) || "false".equals(trimmed);
  }

  @Function
  @Comment("判断是否为有效的十六进制字符串")
  public Boolean isHex(@Comment(name = "value", value = "目标对象") String payload) {
    if (StrUtil.isBlank(payload)) {
      return false;
    }
    String cleaned = payload.replaceAll("[^0-9A-Fa-f]", "");
    return cleaned.length() % 2 == 0 && cleaned.length() > 0;
  }

  @Function
  @Comment("判断是否为有效的Base64字符串")
  public Boolean isBase64(@Comment(name = "value", value = "目标对象") String payload) {
    if (StrUtil.isBlank(payload)) {
      return false;
    }
    try {
      String cleaned = payload.trim();
      // Base64 字符集检查
      return cleaned.matches("^[A-Za-z0-9+/]*={0,2}$");
    } catch (Exception e) {
      return false;
    }
  }

  @Function
  @Comment("判断对象是否为null")
  public Boolean isNull(@Comment(name = "value", value = "目标对象") Object payload) {
    return payload == null;
  }

  @Function
  @Comment("判断对象是否非null")
  public Boolean isNotNull(@Comment(name = "value", value = "目标对象") Object payload) {
    return payload != null;
  }
}
