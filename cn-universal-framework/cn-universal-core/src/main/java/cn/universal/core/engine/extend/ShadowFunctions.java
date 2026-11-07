package cn.universal.core.engine.extend;

import cn.hutool.json.JSONObject;
import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 设备影子 JSON 解析函数（Magic-API 调用） */
@Component
public class ShadowFunctions implements IdeMagicFunction {

  // ------------------------------ 单字段查询 ------------------------------
  @Function
  @Comment("获取指定字段当前值（来自 state.reported）")
  public Object shadow_getVal(
      @Comment(name = "shadow", value = "设备影子(Map/JSONObject均可)") Map<String, Object> shadow,
      @Comment(name = "field", value = "字段名，如 voltage") String fieldName) {
    JSONObject stateReported = getStateReportedNode(shadow);
    if (stateReported == null || fieldName == null) {
      return null;
    }
    if (!stateReported.containsKey(fieldName)) {
      return null;
    }
    return stateReported.get(fieldName);
  }

  @Function
  @Comment("获取指定字段上次上报时间戳（来自 metadata.reported）")
  public Long shadow_getTs(
      @Comment(name = "shadow", value = "设备影子(Map/JSONObject均可)") Map<String, Object> shadow,
      @Comment(name = "field", value = "字段名，如 voltage") String fieldName) {
    JSONObject metadataReported = getMetadataReportedNode(shadow);
    if (metadataReported == null || fieldName == null) {
      return null;
    }
    if (!metadataReported.containsKey(fieldName)) {
      return null;
    }
    JSONObject fieldMeta = metadataReported.getJSONObject(fieldName);
    if (fieldMeta == null || !fieldMeta.containsKey("timestamp")) {
      return null;
    }
    return fieldMeta.getLong("timestamp");
  }

  // ------------------------------ 批量查询 ------------------------------
  @Function
  @Comment("获取所有reported字段的当前值，返回JSONObject: {key:value}")
  public JSONObject shadow_getAllVals(
      @Comment(name = "shadow", value = "设备影子(Map/JSONObject均可)") Map<String, Object> shadow) {
    JSONObject stateReported = getStateReportedNode(shadow);
    if (stateReported == null) {
      return null;
    }
    JSONObject out = new JSONObject();
    Set<String> fieldNames = stateReported.keySet();
    for (String field : fieldNames) {
      out.set(field, stateReported.get(field));
    }
    return out;
  }

  @Function
  @Comment("获取所有reported字段的上次上报时间戳，返回JSONObject: {key:ts}")
  public JSONObject shadow_getAllTs(
      @Comment(name = "shadow", value = "设备影子(Map/JSONObject均可)") Map<String, Object> shadow) {
    JSONObject metadataReported = getMetadataReportedNode(shadow);
    if (metadataReported == null) {
      return null;
    }
    JSONObject out = new JSONObject();
    Set<String> fieldNames = metadataReported.keySet();
    for (String field : fieldNames) {
      JSONObject fieldMeta = metadataReported.getJSONObject(field);
      if (fieldMeta != null && fieldMeta.containsKey("timestamp")) {
        out.set(field, fieldMeta.getLong("timestamp"));
      }
    }
    return out;
  }

  // ------------------------------ 私有辅助 ------------------------------
  private JSONObject asJson(Object obj) {
    try {
      if (obj instanceof JSONObject) {
        return (JSONObject) obj;
      }
      if (obj instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) obj;
        return new JSONObject(map);
      }
    } catch (Exception ignore) {
      // fallthrough
    }
    return null;
  }

  private JSONObject getStateReportedNode(Map<String, Object> shadowObj) {
    if (shadowObj == null) {
      return null;
    }
    JSONObject root = asJson(shadowObj);
    if (root == null) {
      return null;
    }
    JSONObject stateObj = root.getJSONObject("state");
    if (stateObj == null) {
      return null;
    }
    JSONObject reportedObj = stateObj.getJSONObject("reported");
    if (reportedObj == null) {
      return null;
    }
    return reportedObj;
  }

  private JSONObject getMetadataReportedNode(Map<String, Object> shadowObj) {
    if (shadowObj == null) {
      return null;
    }
    JSONObject root = asJson(shadowObj);
    if (root == null) {
      return null;
    }
    JSONObject metadataObj = root.getJSONObject("metadata");
    if (metadataObj == null) {
      return null;
    }
    JSONObject reportedObj = metadataObj.getJSONObject("reported");
    if (reportedObj == null) {
      return null;
    }
    return reportedObj;
  }
}
