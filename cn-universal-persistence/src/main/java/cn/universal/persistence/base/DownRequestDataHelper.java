package cn.universal.persistence.base;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.message.DownCommonData;
import cn.universal.core.message.DownRequest;
import java.util.HashMap;
import java.util.Map;

/** 下行 data 访问辅助工具，统一处理 JSONObject / Map / DownCommonData。 */
public final class DownRequestDataHelper {

  private DownRequestDataHelper() {}

  public static Object getFieldValue(Object data, String fieldName) {
    if (data == null || fieldName == null) {
      return null;
    }

    if (data instanceof DownCommonData downCommonData) {
      return downCommonData.get(fieldName);
    }

    if (data instanceof Map<?, ?> map) {
      return map.get(fieldName);
    }

    try {
      return BeanUtil.getFieldValue(data, fieldName);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 统一获取字段值，优先读取下行 data，再回退到 DownRequest 强类型字段。
   *
   * <p>设计目标：
   *
   * <ul>
   *   <li>任意动态参数统一走 {@code BaseDownRequest.data}
   *   <li>网关/从站等核心字段允许继续保留在 {@link DownRequest} 上
   *   <li>校验和配置提取只关心“这个字段是否真实存在”，不再关心它落在哪一层
   * </ul>
   */
  public static Object getFieldValue(DownRequest downRequest, Object data, String fieldName) {
    if (fieldName == null) {
      return null;
    }

    Object value = getFieldValue(data, fieldName);
    if (value != null) {
      return value;
    }

    if (downRequest instanceof BaseDownRequest baseDownRequest) {
      value = getFieldValue(baseDownRequest.getData(), fieldName);
      if (value != null) {
        return value;
      }
    }

    return getFieldValue((Object) downRequest, fieldName);
  }

  /**
   * 解析下行前置拦截器使用的数据源。
   *
   * <p>如果请求本身携带了 {@code BaseDownRequest.data}，优先使用它作为动态参数真源，避免外部再传一个
   * DownCommonData 投影对象导致字段视图不一致。
   */
  public static Object resolveBeforeDownData(DownRequest downRequest, Object data) {
    if (downRequest instanceof BaseDownRequest baseDownRequest && baseDownRequest.getData() != null) {
      return baseDownRequest.getData();
    }
    return data;
  }

  public static Map<String, Object> toMap(Object data) {
    if (data == null) {
      return new HashMap<>();
    }

    if (data instanceof DownCommonData downCommonData) {
      return new HashMap<>(downCommonData.toMap());
    }

    if (data instanceof Map<?, ?> map) {
      Map<String, Object> result = new HashMap<>();
      map.forEach(
          (key, value) -> {
            if (key != null) {
              result.put(String.valueOf(key), value);
            }
          });
      return result;
    }

    return BeanUtil.beanToMap(data);
  }

  public static JSONObject toJsonObject(Object data) {
    return JSONUtil.parseObj(toMap(data));
  }
}
