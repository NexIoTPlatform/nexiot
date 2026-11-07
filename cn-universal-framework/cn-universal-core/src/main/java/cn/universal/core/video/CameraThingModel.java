package cn.universal.core.video;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

public final class CameraThingModel {
  /** Top-level aliases and native debug copies omitted from SDK/WVP alarm data. */
  public static final Set<String> COMPACT_VIDEO_FIELDS = Set.of(
      "rawData", "raw", "sourcePayload", "alarmType", "alarmName", "cameraEventName",
      "vendorCameraEventType", "vendorEventName", "vendorEventCodeHex", "standardEventType",
      "deviceName", "deviceIp", "vendor", "command", "nativeChannel", "nativeState",
      "nativeAlarmTime", "nativeAdditionalParameters", "alarmTypeDescription",
      "alarmMethodDescription", "alarmPriorityDescription", "alarmPriority", "createTime", "id",
      "snapshotUrl", "imageUrl", "localUrl", "gatewayStored");
  public static final Set<String> COMPACT_VIDEO_IMAGE_REDUNDANT_FIELDS = Set.of(
      "localKey", "localUrl", "localStatus", "snapshotUrl", "imageUrl", "ossUrl", "cloudUrl",
      "gatewayStored", "storageMode", "provider", "vendor", "instanceKey", "deviceId", "channelId", "sizeBytes",
      "bucket", "s3Status", "cloudReadStatus");
  private static final Pattern FIELD_ID = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.-]{0,99}");
  private static final Pattern PRIVATE_FIELD = Pattern.compile(".*(token|secret|password|authorization|credential).*");
  private CameraThingModel() {}

  /** Keep SDK/WVP event declarations aligned with stored data without touching business schemas. */
  public static String compactVideoMetadata(String metadata) {
    JSONObject root = JSONUtil.parseObj(metadata);
    JSONArray events = root.getJSONArray("events");
    if (events == null) return metadata;
    boolean changed = false;
    for (JSONObject event : events.jsonIter()) {
      JSONObject type = event.getJSONObject("valueType");
      JSONArray fields = type == null ? null : type.getJSONArray("properties");
      if (fields == null) continue;
      for (int index = fields.size() - 1; index >= 0; index--) {
        JSONObject definition = fields.getJSONObject(index);
        String id = definition.getStr("id", "");
        JSONObject fieldType = definition.getJSONObject("valueType");
        JSONArray children = fieldType == null ? null : fieldType.getJSONArray("properties");
        boolean emptyVendorWrapper = ("data".equals(id) || "outputData".equals(id))
            && fieldType != null && "object".equals(fieldType.getStr("type"))
            && (children == null || children.isEmpty());
        if (COMPACT_VIDEO_FIELDS.contains(id) || emptyVendorWrapper) {
          fields.remove(index);
          changed = true;
        } else if ("images".equals(id)) {
          JSONObject arrayType = fieldType;
          JSONObject elementType = arrayType == null ? null : arrayType.getJSONObject("elementType");
          JSONArray imageFields = elementType == null ? null : elementType.getJSONArray("properties");
          if (imageFields != null) for (int imageIndex = imageFields.size() - 1; imageIndex >= 0; imageIndex--) {
            if (COMPACT_VIDEO_IMAGE_REDUNDANT_FIELDS.contains(imageFields.getJSONObject(imageIndex).getStr("id", ""))) {
              imageFields.remove(imageIndex);
              changed = true;
            }
          }
        }
      }
    }
    return changed ? root.toString() : metadata;
  }

  /** Imou keeps nonempty official outputData as the sole copy of declared vendor outputs. */
  public static String compactImoulifeMetadata(String metadata) {
    JSONObject root = JSONUtil.parseObj(compactVideoMetadata(metadata));
    JSONArray events = root.getJSONArray("events");
    if (events == null) return metadata;
    for (JSONObject event : events.jsonIter()) {
      JSONObject type = event.getJSONObject("valueType");
      JSONArray fields = type == null ? null : type.getJSONArray("properties");
      if (fields == null) continue;
      for (int index = fields.size() - 1; index >= 0; index--) {
        JSONObject definition = fields.getJSONObject(index);
        String id = definition.getStr("id", "");
        if (cn.universal.core.alarm.VideoAlarmPayload.IMOULIFE_TRANSPORT_FIELDS.contains(id)) fields.remove(index);
        else if ("monitor".equals(id)) {
          JSONObject monitorType = definition.getJSONObject("valueType");
          JSONArray children = monitorType == null ? null : monitorType.getJSONArray("properties");
          if (children != null) children.removeIf(child -> Set.of("vendor", "vendorV3", "sessionId", "action")
              .contains(JSONUtil.parseObj(child).getStr("id")));
        }
      }
      JSONArray imageFields = new JSONArray();
      for (String id : List.of("url", "id", "kind", "contentType"))
        imageFields.add(field(id, id, new JSONObject().set("type", "string")));
      JSONArray defaults = new JSONArray();
      defaults.add(field("images", "厂家图片来源", new JSONObject().set("type", "array")
          .set("elementType", new JSONObject().set("type", "object").set("properties", imageFields))));
      mergeFields(fields, defaults);
    }
    return root.toString();
  }

  /** Shared event fields are data paths; no expiring URL or sample value is stored in metadata. */
  public static JSONArray eventFields() {
    Map<String, String> names = new LinkedHashMap<>();
    names.put("instanceKey", "平台实例");
    names.put("platformType", "视频平台类型");
    names.put("deviceId", "设备标识");
    names.put("channelId", "通道号");
    names.put("eventId", "事件追踪ID");
    names.put("cameraEventType", "摄像头事件类型");
    names.put("cameraEventName", "摄像头事件名称");
    names.put("standardEventType", "标准事件类型");
    names.put("vendorEventCode", "厂商事件代码");
    names.put("alarmState", "告警状态");
    names.put("sourceState", "来源状态");
    names.put("snapshotUrl", "已转存抓拍地址");
    JSONArray fields = new JSONArray();
    names.forEach((id, name) -> fields.add(field(id, name, new JSONObject().set("type", "string"))));
    fields.add(field("occurredAt", "发生时间", new JSONObject().set("type", "long")));
    fields.add(field("alarmTime", "告警时间", new JSONObject().set("type", "long")));
    for (String id : List.of("data", "outputData", "rawData"))
      fields.add(field(id, id.equals("rawData") ? "原始事件数据" : "厂商事件数据",
          new JSONObject().set("type", "object").set("properties", new JSONArray())));
    JSONArray evidenceFields = new JSONArray();
    for (String id : List.of("id", "ossId", "objectKey", "url", "name", "kind", "mimeType", "status", "sourceUrl"))
      evidenceFields.add(field(id, id, new JSONObject().set("type", "string")));
    fields.add(field("evidence", "已归档取证文件", new JSONObject().set("type", "array")
        .set("elementType", new JSONObject().set("type", "object").set("properties", evidenceFields))));
    return fields;
  }

  public static JSONObject event(String id, String name) {
    return new JSONObject().set("id", id).set("name", name)
        .set("valueType", new JSONObject().set("type", "object").set("properties", eventFields()));
  }

  /** Upgrade legacy scalar camera events and retain provider names, severity and custom fields. */
  public static String enrichEvents(String metadata) {
    JSONObject root = JSONUtil.parseObj(metadata);
    JSONArray events = root.getJSONArray("events");
    if (events == null) return metadata;
    boolean changed = false;
    for (JSONObject event : events.jsonIter()) changed |= mergeEventFields(event, eventFields());
    return changed ? root.toString() : metadata;
  }

  /** Add missing definitions from the provider without overwriting administrator definitions. */
  public static String mergeMetadata(String metadata, String template) {
    JSONObject root = JSONUtil.parseObj(metadata == null || metadata.isBlank() ? "{}" : metadata);
    JSONObject defaults = JSONUtil.parseObj(template);
    boolean changed = false;
    for (String section : List.of("properties", "events", "functions")) {
      JSONArray definitions = root.getJSONArray(section);
      if (definitions == null) {
        definitions = new JSONArray();
        root.set(section, definitions);
        changed = true;
      }
      JSONArray additions = defaults.getJSONArray(section);
      if (additions == null) continue;
      for (JSONObject addition : additions.jsonIter()) {
        JSONObject existing = find(definitions, addition.getStr("id"));
        if (existing == null) { definitions.add(addition); changed = true; }
        else if ("events".equals(section)) {
          JSONObject type = addition.getJSONObject("valueType");
          if (type != null && "object".equals(type.getStr("type")))
            changed |= mergeEventFields(existing, type.getJSONArray("properties"));
        }
      }
    }
    return changed ? root.toString() : metadata;
  }

  /** Resolve the declared name for both current uplinks and historical log/evidence rows. */
  public static String eventName(String metadata, String eventId, String fallback) {
    try {
      JSONArray events = JSONUtil.parseObj(metadata).getJSONArray("events");
      JSONObject event = events == null ? null : find(events, eventId);
      String name = event == null ? null : event.getStr("name");
      return name == null || name.isBlank() ? fallback : name;
    } catch (RuntimeException ignored) {
      return fallback;
    }
  }

  /** Replace learned placeholder labels with provider declarations, retaining custom labels. */
  public static String mergeProviderMetadata(String metadata, String template, String source) {
    JSONObject root = JSONUtil.parseObj(mergeMetadata(metadata, template));
    JSONObject declarations = JSONUtil.parseObj(template);
    for (String section : List.of("properties", "events")) {
      JSONArray declared = declarations.getJSONArray(section);
      JSONArray existing = root.getJSONArray(section);
      if (declared != null && existing != null) mergeProviderFields(existing, declared, source);
    }
    return root.toString();
  }

  private static void mergeProviderFields(JSONArray target, JSONArray declarations, String source) {
    mergeFields(target, declarations);
    for (JSONObject declaration : declarations.jsonIter()) {
      String id = declaration.getStr("id");
      JSONObject current = find(target, id);
      if (current == null) continue;
      JSONObject marker = declaration.getJSONObject("expands");
      JSONObject existingMarker = current.getJSONObject("expands");
      String name = current.getStr("name", "");
      boolean placeholder = name.isBlank() || name.equals(id)
          || name.equals("厂家事件（" + id.replaceFirst("^vendor_", "") + "）");
      boolean managed = existingMarker != null && source.equals(existingMarker.getStr("schemaSource"))
          && name.equals(existingMarker.getStr("providerDeclaredName", name));
      boolean declared = marker != null && source.equals(marker.getStr("schemaSource"));
      if (declared && (placeholder || managed)) {
        current.set("name", declaration.getStr("name", id));
        if (declaration.containsKey("description")) current.set("description", declaration.get("description"));
        if (existingMarker == null) { existingMarker = new JSONObject(); current.set("expands", existingMarker); }
        existingMarker.putAll(marker);
      }
      JSONObject left = current.getJSONObject("valueType");
      JSONObject right = declaration.getJSONObject("valueType");
      if (left == null || right == null) continue;
      if (declared && (placeholder || managed) && !left.getStr("type", "").equals(right.getStr("type", ""))) {
        current.set("valueType", right);
      } else if ("object".equals(left.getStr("type")) && "object".equals(right.getStr("type"))) {
        JSONArray leftFields = left.getJSONArray("properties");
        JSONArray rightFields = right.getJSONArray("properties");
        if (leftFields != null && rightFields != null) mergeProviderFields(leftFields, rightFields, source);
      } else if ("array".equals(left.getStr("type")) && "array".equals(right.getStr("type"))) {
        JSONObject leftElement = left.getJSONObject("elementType");
        JSONObject rightElement = right.getJSONObject("elementType");
        if (leftElement != null && rightElement != null
            && "object".equals(leftElement.getStr("type")) && "object".equals(rightElement.getStr("type"))) {
          JSONArray leftFields = leftElement.getJSONArray("properties");
          JSONArray rightFields = rightElement.getJSONArray("properties");
          if (leftFields != null && rightFields != null) mergeProviderFields(leftFields, rightFields, source);
        }
      } else if (declared && (placeholder || managed)) {
        current.set("valueType", right);
      }
    }
  }

  /** 仅以字段名及类型标识结构；事件值、图片内容、时间戳不进入缓存键。 */
  public static String observationSignature(Map<String, Object> data, Map<String, Object> properties) {
    return fields(data, 0).toString() + "\n"
        + (properties == null || properties.isEmpty() ? "" : fields(properties, 0).toString());
  }

  /** Only schema keys/types are learned, bounded by depth/width; payload values stay in logs. */
  public static String observe(String metadata, String eventId, String eventName,
      Map<String, Object> data, Map<String, Object> properties) {
    JSONObject root = JSONUtil.parseObj(metadata == null || metadata.isBlank() ? "{}" : metadata);
    boolean changed = false;
    if (eventId != null && eventId.matches("[A-Za-z][A-Za-z0-9_]{0,99}")) {
      JSONArray events = root.getJSONArray("events");
      if (events == null) { events = new JSONArray(); root.set("events", events); }
      JSONObject event = find(events, eventId);
      if (event == null && events.size() < 512) {
        event = event(eventId, eventName == null || eventName.isBlank() ? eventId : eventName);
        events.add(event); changed = true;
      }
      if (event != null) {
        changed |= mergeEventFields(event, eventFields());
        changed |= mergeFields(event.getJSONObject("valueType").getJSONArray("properties"), fields(data, 0));
      }
    }
    if (properties != null && !properties.isEmpty()) {
      JSONArray fields = root.getJSONArray("properties");
      if (fields == null) { fields = new JSONArray(); root.set("properties", fields); }
      changed |= mergeFields(fields, fields(properties, 0));
    }
    return changed ? root.toString() : metadata;
  }

  public static boolean mergeEventFields(JSONObject event, JSONArray additions) {
    JSONObject type = event.getJSONObject("valueType");
    boolean changed = false;
    if (type == null || !"object".equals(type.getStr("type"))) {
      type = new JSONObject().set("type", "object").set("properties", new JSONArray());
      event.set("valueType", type); changed = true;
    }
    JSONArray fields = type.getJSONArray("properties");
    if (fields == null) { fields = new JSONArray(); type.set("properties", fields); changed = true; }
    return mergeFields(fields, additions) || changed;
  }

  private static boolean mergeFields(JSONArray target, JSONArray additions) {
    if (additions == null) return false;
    boolean changed = false;
    for (JSONObject addition : additions.jsonIter()) {
      JSONObject existing = find(target, addition.getStr("id"));
      if (existing == null) {
        if (target.size() < 256) { target.add(addition); changed = true; }
      } else {
        JSONObject left = existing.getJSONObject("valueType");
        JSONObject right = addition.getJSONObject("valueType");
        changed |= mergeTypes(left, right);
      }
    }
    return changed;
  }

  private static boolean mergeTypes(JSONObject left, JSONObject right) {
    if (left == null || right == null || !left.getStr("type", "").equals(right.getStr("type", ""))) return false;
    if ("object".equals(left.getStr("type"))) {
      JSONArray target = left.getJSONArray("properties");
      if (target == null) { target = new JSONArray(); left.set("properties", target); }
      return mergeFields(target, right.getJSONArray("properties"));
    }
    if ("array".equals(left.getStr("type")))
      return mergeTypes(left.getJSONObject("elementType"), right.getJSONObject("elementType"));
    return false;
  }

  private static JSONObject find(JSONArray definitions, String id) {
    for (JSONObject definition : definitions.jsonIter())
      if (id != null && id.equals(definition.getStr("id"))) return definition;
    return null;
  }

  private static JSONArray fields(Map<?, ?> values, int depth) {
    JSONArray fields = new JSONArray();
    if (values == null || depth > 5) return fields;
    for (Map.Entry<?, ?> entry : values.entrySet()) {
      String id = String.valueOf(entry.getKey());
      // Official hosted-device models use numeric identifiers such as 111100 and 14603.
      if (!FIELD_ID.matcher(id).matches() || entry.getValue() == null
          || PRIVATE_FIELD.matcher(id.toLowerCase(java.util.Locale.ROOT)).matches()) continue;
      fields.add(field(id, id, type(entry.getValue(), depth)));
      if (fields.size() >= 128) break;
    }
    return fields;
  }

  private static JSONObject type(Object value, int depth) {
    if (value instanceof Map<?, ?> map)
      return new JSONObject().set("type", "object").set("properties", fields(map, depth + 1));
    if (value instanceof List<?> list) {
      Object element = list.stream().filter(java.util.Objects::nonNull).findFirst().orElse(null);
      if (element == null) element = "";
      return new JSONObject().set("type", "array").set("elementType", depth > 5
          ? new JSONObject().set("type", "string") : type(element, depth + 1));
    }
    String type = value instanceof Boolean ? "boolean"
        : value instanceof Float || value instanceof Double || value instanceof java.math.BigDecimal ? "double"
        : value instanceof Number ? "long" : "string";
    return new JSONObject().set("type", type);
  }

  private static JSONObject field(String id, String name, JSONObject type) {
    return new JSONObject().set("id", id).set("name", name).set("valueType", type);
  }

  public static String metadata(String... functions) {
    try {
      JSONObject json =
          JSONUtil.parseObj(
              new ClassPathResource("video/camera-functions.json")
                  .getContentAsString(StandardCharsets.UTF_8));
      Set<String> supported = Set.of(functions);
      JSONArray selected = new JSONArray();
      for (JSONObject function : json.getJSONArray("functions").jsonIter())
        if (supported.contains(function.getStr("id"))) selected.add(function);
      json.set("functions", selected);
      return json.toString();
    } catch (java.io.IOException e) {
      throw new IllegalStateException("视频物模型模板不可用", e);
    }
  }
}
