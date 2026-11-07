package cn.universal.admin.platform.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;

/** 产品物模型归一化，确保系统 function 与 rw 属性保持同步。 */
public final class ProductMetadataNormalizer {

  private ProductMetadataNormalizer() {}

  public static String normalizeMetadataJson(String metadata) {
    return normalizeMetadataObject(parseMetadata(metadata)).toString();
  }

  public static JSONObject normalizeMetadataObject(JSONObject metadata) {
    JSONObject normalized = metadata == null ? parseMetadata(null) : metadata;
    getOrCreateMetadataArray(normalized, "properties");
    getOrCreateMetadataArray(normalized, "events");
    getOrCreateMetadataArray(normalized, "functions");
    normalizeSystemFunctions(normalized);
    return normalized;
  }

  public static JSONObject parseMetadata(String metadata) {
    if (StrUtil.isBlank(metadata) || "{}".equals(metadata)) {
      JSONObject empty = new JSONObject();
      empty.set("properties", new JSONArray());
      empty.set("events", new JSONArray());
      empty.set("functions", new JSONArray());
      return empty;
    }
    try {
      return JSONUtil.parseObj(metadata);
    } catch (Exception e) {
      JSONObject empty = new JSONObject();
      empty.set("properties", new JSONArray());
      empty.set("events", new JSONArray());
      empty.set("functions", new JSONArray());
      return empty;
    }
  }

  private static void normalizeSystemFunctions(JSONObject metadata) {
    if (metadata == null) {
      return;
    }
    JSONArray functions = getOrCreateMetadataArray(metadata, "functions");
    JSONArray desiredInputs =
        buildDesiredPropertyInputs(getOrCreateMetadataArray(metadata, "properties"));
    int desiredIndex = findMetadataIndex(functions, IoTConstant.SET_DEVICE_DESIRED_PROPERTIES);

    JSONObject desiredFunction =
        desiredIndex >= 0 ? functions.getJSONObject(desiredIndex) : new JSONObject();
    desiredFunction.set("id", IoTConstant.SET_DEVICE_DESIRED_PROPERTIES);
    desiredFunction.set("config", true);
    desiredFunction.set("name", StrUtil.blankToDefault(desiredFunction.getStr("name"), "设置期望值"));
    desiredFunction.set(
        "description",
        StrUtil.blankToDefault(
            desiredFunction.getStr("description"), "对所有可写的属性设置期望值，可用于标签、位置、个性化扩展使用"));
    desiredFunction.set("inputs", desiredInputs);
    if (!desiredFunction.containsKey("output") || desiredFunction.get("output") == null) {
      desiredFunction.set("output", new JSONObject());
    }

    if (desiredIndex >= 0) {
      functions.set(desiredIndex, desiredFunction);
    } else {
      functions.add(desiredFunction);
    }
    metadata.set("functions", functions);
  }

  private static JSONArray buildDesiredPropertyInputs(JSONArray properties) {
    JSONArray inputs = new JSONArray();
    if (properties == null || properties.isEmpty()) {
      return inputs;
    }
    for (int i = 0; i < properties.size(); i++) {
      JSONObject property = properties.getJSONObject(i);
      if (property == null || !StrUtil.equalsIgnoreCase("rw", property.getStr("mode"))) {
        continue;
      }
      JSONObject input = new JSONObject();
      input.set("id", property.getStr("id"));
      input.set("name", property.getStr("name"));
      Object valueType = property.get("valueType");
      if (valueType != null) {
        input.set("valueType", JSONUtil.parse(valueType));
      }
      input.set("description", property.getStr("description"));
      inputs.add(input);
    }
    return inputs;
  }

  private static JSONArray getOrCreateMetadataArray(JSONObject metadata, String arrayName) {
    JSONArray array = metadata.getJSONArray(arrayName);
    if (array == null) {
      array = new JSONArray();
      metadata.set(arrayName, array);
    }
    return array;
  }

  private static int findMetadataIndex(JSONArray array, String targetId) {
    if (array == null || StrUtil.isBlank(targetId)) {
      return -1;
    }
    for (int i = 0; i < array.size(); i++) {
      JSONObject item = array.getJSONObject(i);
      if (item != null && targetId.equals(item.getStr("id"))) {
        return i;
      }
    }
    return -1;
  }
}
