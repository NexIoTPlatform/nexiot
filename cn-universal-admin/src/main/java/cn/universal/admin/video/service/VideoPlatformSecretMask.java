package cn.universal.admin.video.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.universal.common.utils.JsonDesensitizationUtil;
import java.util.Map;

/** 补充平台专属密钥名称，输出失败时保持关闭，不返回原始秘密。 */
public final class VideoPlatformSecretMask {
  private VideoPlatformSecretMask() {}

  public static String mask(String value) {
    if (value == null || value.isBlank()) return value;
    JSONObject safe = JsonDesensitizationUtil.desensitize(value);
    if (safe == null) return "{}";
    maskKeys(safe);
    return safe.toString();
  }

  private static void maskKeys(JSONObject object) {
    for (Map.Entry<String, Object> entry : object.entrySet()) {
      String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
      Object value = entry.getValue();
      if (key.equals("isupkey") || key.equals("platformkey") || key.equals("apikey") || key.equals("gatewayapikey")) {
        if (value != null && !value.toString().isBlank()) object.set(entry.getKey(), "***");
      } else if (value instanceof JSONObject nested) maskKeys(nested);
      else if (value instanceof JSONArray array)
        for (Object item : array) if (item instanceof JSONObject nested) maskKeys(nested);
    }
  }
}
