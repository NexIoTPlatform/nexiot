package cn.universal.miniapp.util;

import cn.hutool.core.util.ObjectUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nexiot
 * @version 1.0
 * @since 2025/12/22 21:28
 */
public class MapUtil {
  public static Map<String, String> convert(Map<String, Object> streamUrls) {
    // 1. 创建返回的String类型Map
    Map<String, String> resultMap = new HashMap<>();

    // 2. 判空，避免空指针异常
    if (streamUrls == null || streamUrls.isEmpty()) {
      return resultMap;
    }

    // 3. 遍历原Map，逐个转换
    for (Map.Entry<String, Object> entry : streamUrls.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      // 4. 安全将Object转为String（处理null值）
      String stringValue = null;
      if (value != null) {
        // 使用Object的toString()方法，大多数Java对象都已重写该方法
        stringValue = ObjectUtil.toString(value);
        // 可选：去除字符串前后空格（根据你的业务需求决定是否添加）
        // stringValue = value.toString().trim();
      }

      // 5. 将键值对放入结果Map
      resultMap.put(key, stringValue);
    }

    return resultMap;
  }
}
