package cn.universal.web;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/** 影子数据合并测试 可以直接运行来测试 mergeShadowJson 方法 */
public class test_shadow_merge {

  public static void main(String[] args) {
    test_shadow_merge tester = new test_shadow_merge();
    String t1 =
        """
        {"timestamp":1760750478,"version":1}
        """;
    String t2 =
        """
        {"state":{"desired":{},"reported":{"sourcePayload":"02031002C100AD00000000000000000000000573C3","imei":"860048070262660","csq":31,"timestamp":"1761135669","gps":"120.1672762,30.2146785","iccid":"89861590112330001924","imsi":"460150057031992","heartbeatType":"DTU_HEART"}},"metadata":{"desired":{},"reported":{"sourcePayload":{"timestamp":1761135729},"imei":{"timestamp":1761135678},"csq":{"timestamp":1761135678},"timestamp":{"timestamp":1761135678},"gps":{"timestamp":1761135678},"iccid":{"timestamp":1761135678},"imsi":{"timestamp":1761135678},"heartbeatType":{"timestamp":1761135678}}},"timestamp":1761135729,"version":2479}
        """;
    String tx = tester.mergeShadowJson(t1, t2);
    // 格式化输出结果
    JSONObject resultObj = JSONUtil.parseObj(tx);
    System.out.println("格式化合并结果:");
    System.out.println(JSONUtil.toJsonPrettyStr(resultObj));
  }

  /** 模拟 mergeShadowJson 方法的实现 */
  private String mergeShadowJson(String existingJson, String incomingJson) {
    if (incomingJson == null || incomingJson.trim().isEmpty()) {
      return existingJson == null || existingJson.trim().isEmpty() ? "{}" : existingJson;
    }
    if (existingJson == null || existingJson.trim().isEmpty()) {
      return incomingJson;
    }

    JSONObject existing;
    JSONObject incoming;
    try {
      existing = JSONUtil.parseObj(existingJson);
    } catch (Exception e) {
      existing = new JSONObject();
    }
    try {
      incoming = JSONUtil.parseObj(incomingJson);
    } catch (Exception e) {
      return existingJson;
    }

    JSONObject merged = deepMergeObject(existing, incoming);

    // 顶层特殊字段处理
    try {
      Long tsExisting = merged.getLong("timestamp");
      Long tsIncoming = incoming.getLong("timestamp");
      if (tsExisting != null || tsIncoming != null) {
        long maxTs =
            Math.max(tsExisting == null ? 0L : tsExisting, tsIncoming == null ? 0L : tsIncoming);
        merged.set("timestamp", maxTs);
      }
    } catch (Exception ignore) {
      // ignore
    }

    try {
      Long verExisting = merged.getLong("version");
      Long verIncoming = incoming.getLong("version");
      if (verExisting != null || verIncoming != null) {
        long maxVer =
            Math.max(
                verExisting == null ? 0L : verExisting, verIncoming == null ? 0L : verIncoming);
        merged.set("version", maxVer);
      }
    } catch (Exception ignore) {
      // ignore
    }

    return JSONUtil.toJsonStr(merged);
  }

  /** 对 JSONObject 进行递归深度合并 */
  private JSONObject deepMergeObject(JSONObject base, JSONObject incoming) {
    if (base == null) {
      return incoming == null ? new JSONObject() : incoming;
    }
    if (incoming == null) {
      return base;
    }
    JSONObject result = JSONUtil.parseObj(base.toString());
    for (String key : incoming.keySet()) {
      Object inVal = incoming.get(key);
      Object baseVal = result.get(key);

      if (inVal instanceof JSONObject && baseVal instanceof JSONObject) {
        result.set(key, deepMergeObject((JSONObject) baseVal, (JSONObject) inVal));
      } else {
        // 数组或标量：直接覆盖
        result.set(key, inVal);
      }
    }
    return result;
  }
}
