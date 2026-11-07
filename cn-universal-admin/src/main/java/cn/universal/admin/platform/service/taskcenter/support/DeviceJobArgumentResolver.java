package cn.universal.admin.platform.service.taskcenter.support;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.entity.bo.IoTDeviceJobParamSpec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

public final class DeviceJobArgumentResolver {

  public static final String MODE_FIXED = "FIXED";
  public static final String MODE_AUTO = "AUTO";
  public static final String MODE_TEMPLATE = "TEMPLATE";

  private static final Pattern TEMPLATE_PATTERN =
      Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.$]+)\\s*}}");

  private DeviceJobArgumentResolver() {}

  public static LinkedHashMap<String, IoTDeviceJobParamSpec> normalizeSpecs(
      Map<String, ?> specs, Map<String, ?> legacyFunctionData) {
    LinkedHashMap<String, IoTDeviceJobParamSpec> result = new LinkedHashMap<>();
    if (specs != null && !specs.isEmpty()) {
      specs.forEach(
        (key, value) -> {
            IoTDeviceJobParamSpec parsedSpec = toParamSpec(value);
            if (StrUtil.isBlank(key) || parsedSpec == null) {
              return;
            }
            IoTDeviceJobParamSpec next = new IoTDeviceJobParamSpec();
            next.setMode(normalizeMode(parsedSpec.getMode(), parsedSpec));
            next.setValue(parsedSpec.getValue());
            next.setTemplate(parsedSpec.getTemplate());
            result.put(key, next);
          });
      return result;
    }
    if (legacyFunctionData == null || legacyFunctionData.isEmpty()) {
      return result;
    }
    legacyFunctionData.forEach(
        (key, value) -> {
          if (StrUtil.isBlank(key)) {
            return;
          }
          IoTDeviceJobParamSpec next = new IoTDeviceJobParamSpec();
          next.setMode(MODE_FIXED);
          next.setValue(value);
          result.put(key, next);
        });
    return result;
  }

  public static Map<String, Object> buildDisplayValues(Map<String, IoTDeviceJobParamSpec> specs) {
    LinkedHashMap<String, Object> preview = new LinkedHashMap<>();
    if (specs == null || specs.isEmpty()) {
      return preview;
    }
    specs.forEach(
        (key, spec) -> {
          if (StrUtil.isBlank(key) || spec == null) {
            return;
          }
          String mode = normalizeMode(spec.getMode(), spec);
          if (MODE_TEMPLATE.equals(mode)) {
            preview.put(key, StrUtil.blankToDefault(spec.getTemplate(), ""));
            return;
          }
          if (MODE_AUTO.equals(mode)) {
            preview.put(key, String.format("{{auto.%s}}", key));
            return;
          }
          preview.put(key, spec.getValue());
        });
    return preview;
  }

  public static ResolvedArguments resolve(
      Map<String, IoTDeviceJobParamSpec> specs,
      JSONObject functionDefinition,
      IoTDevice device,
      IoTDeviceShadow shadow) {
    LinkedHashMap<String, JSONObject> inputDefinitionMap = buildInputDefinitionMap(functionDefinition);
    JSONObject resolvedData = new JSONObject();
    JSONObject resolvedDetails = new JSONObject();
    JSONObject shadowMetadata = parseShadowMetadata(shadow);
    if (specs == null || specs.isEmpty()) {
      return new ResolvedArguments(resolvedData, resolvedDetails);
    }
    specs.forEach(
        (fieldId, spec) -> {
          JSONObject inputDefinition = inputDefinitionMap.get(fieldId);
          String mode = normalizeMode(spec == null ? null : spec.getMode(), spec);
          Object rawValue = resolveRawValue(fieldId, mode, spec, device, shadowMetadata);
          Object value = coerceValueByDefinition(rawValue, inputDefinition);
          if (isRequiredField(inputDefinition) && isMissing(value)) {
            throw new BaseException(String.format("参数 %s 未解析到有效值", fieldId));
          }
          if (!isMissing(value)) {
            resolvedData.set(fieldId, value);
          }
          JSONObject detail = new JSONObject();
          detail.set("mode", mode);
          if (MODE_TEMPLATE.equals(mode) && spec != null && StrUtil.isNotBlank(spec.getTemplate())) {
            detail.set("template", spec.getTemplate());
          }
          if (MODE_FIXED.equals(mode) && spec != null) {
            detail.set("value", spec.getValue());
          }
          detail.set("resolvedValue", value);
          resolvedDetails.set(fieldId, detail);
        });
    return new ResolvedArguments(resolvedData, resolvedDetails);
  }

  public static String supportedVariableHint() {
    return "{{deviceId}}, {{deviceName}}, {{iotId}}, {{application}}, {{productKey}}, "
        + "{{productName}}, {{instance}}, {{onlineTime}}, {{createTime}}, "
        + "{{configuration.xxx}}, {{shadow.reported.xxx}}, {{shadow.desired.xxx}}";
  }

  private static LinkedHashMap<String, JSONObject> buildInputDefinitionMap(JSONObject functionDefinition) {
    LinkedHashMap<String, JSONObject> result = new LinkedHashMap<>();
    if (functionDefinition == null) {
      return result;
    }
    JSONArray inputs = functionDefinition.getJSONArray("inputs");
    if (inputs == null || inputs.isEmpty()) {
      return result;
    }
    for (int i = 0; i < inputs.size(); i++) {
      JSONObject input = inputs.getJSONObject(i);
      if (input == null) {
        continue;
      }
      String id = input.getStr("id");
      if (StrUtil.isNotBlank(id)) {
        result.put(id, input);
      }
    }
    return result;
  }

  private static String normalizeMode(String mode, IoTDeviceJobParamSpec spec) {
    String normalized = StrUtil.blankToDefault(mode, "").trim().toUpperCase();
    if (MODE_AUTO.equals(normalized)) {
      return MODE_AUTO;
    }
    if (MODE_TEMPLATE.equals(normalized)) {
      return MODE_TEMPLATE;
    }
    if (MODE_FIXED.equals(normalized)) {
      return MODE_FIXED;
    }
    if (spec != null && StrUtil.isNotBlank(spec.getTemplate())) {
      return MODE_TEMPLATE;
    }
    return MODE_FIXED;
  }

  private static IoTDeviceJobParamSpec toParamSpec(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof IoTDeviceJobParamSpec) {
      return (IoTDeviceJobParamSpec) value;
    }
    try {
      return JSONUtil.toBean(JSONUtil.parseObj(value), IoTDeviceJobParamSpec.class);
    } catch (Exception e) {
      return null;
    }
  }

  private static JSONObject parseShadowMetadata(IoTDeviceShadow shadow) {
    if (shadow == null || StrUtil.isBlank(shadow.getMetadata())) {
      return null;
    }
    try {
      return JSONUtil.parseObj(shadow.getMetadata());
    } catch (Exception e) {
      return null;
    }
  }

  private static Object resolveRawValue(
      String fieldId,
      String mode,
      IoTDeviceJobParamSpec spec,
      IoTDevice device,
      JSONObject shadowMetadata) {
    if (MODE_AUTO.equals(mode)) {
      return getAutoFillValue(fieldId, device, shadowMetadata);
    }
    if (MODE_TEMPLATE.equals(mode)) {
      return resolveTemplateValue(spec == null ? null : spec.getTemplate(), device, shadowMetadata);
    }
    return spec == null ? null : spec.getValue();
  }

  private static Object resolveTemplateValue(
      String template, IoTDevice device, JSONObject shadowMetadata) {
    if (StrUtil.isBlank(template)) {
      return null;
    }
    Matcher directMatcher = TEMPLATE_PATTERN.matcher(template.trim());
    if (directMatcher.matches()) {
      return resolveVariable(directMatcher.group(1), device, shadowMetadata);
    }
    Matcher matcher = TEMPLATE_PATTERN.matcher(template);
    StringBuffer sb = new StringBuffer();
    boolean matched = false;
    while (matcher.find()) {
      matched = true;
      Object value = resolveVariable(matcher.group(1), device, shadowMetadata);
      if (value == null) {
        return null;
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(stringifyTemplateValue(value)));
    }
    matcher.appendTail(sb);
    return matched ? sb.toString() : template;
  }

  private static String stringifyTemplateValue(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
      return String.valueOf(value);
    }
    return JSONUtil.toJsonStr(value);
  }

  private static Object resolveVariable(String expression, IoTDevice device, JSONObject shadowMetadata) {
    if (StrUtil.isBlank(expression) || device == null) {
      return null;
    }
    String path = expression.trim();
    if (StrUtil.startWith(path, "device.")) {
      path = path.substring("device.".length());
    }
    switch (path) {
      case "deviceId":
        return device.getDeviceId();
      case "deviceName":
        return device.getDeviceName();
      case "iotId":
        return device.getIotId();
      case "application":
        return device.getApplication();
      case "productKey":
        return device.getProductKey();
      case "productName":
        return device.getProductName();
      case "instance":
        return device.getInstance();
      case "onlineTime":
        return device.getOnlineTime();
      case "createTime":
        return device.getCreateTime();
      default:
        break;
    }
    if (StrUtil.startWith(path, "configuration.")) {
      JSONObject configuration = parseJsonObject(device.getConfiguration());
      return readJsonPath(configuration, path.substring("configuration.".length()));
    }
    if (StrUtil.equals(path, "configuration")) {
      return parseJsonObject(device.getConfiguration());
    }
    if (StrUtil.startWith(path, "shadow.")) {
      return readShadowPath(shadowMetadata, path.substring("shadow.".length()));
    }
    return null;
  }

  private static Object readShadowPath(JSONObject shadowMetadata, String path) {
    if (shadowMetadata == null || StrUtil.isBlank(path)) {
      return null;
    }
    if (StrUtil.equals(path, "reported") || StrUtil.equals(path, "desired")) {
      JSONObject state = shadowMetadata.getJSONObject("state");
      return state == null ? null : state.get(path);
    }
    if (StrUtil.startWith(path, "reported.")) {
      JSONObject state = shadowMetadata.getJSONObject("state");
      return readJsonPath(state == null ? null : state.getJSONObject("reported"), path.substring("reported.".length()));
    }
    if (StrUtil.startWith(path, "desired.")) {
      JSONObject state = shadowMetadata.getJSONObject("state");
      return readJsonPath(state == null ? null : state.getJSONObject("desired"), path.substring("desired.".length()));
    }
    return null;
  }

  private static Object readJsonPath(JSONObject source, String path) {
    if (source == null || StrUtil.isBlank(path)) {
      return null;
    }
    Object current = source;
    String[] segments = path.split("\\.");
    for (String segment : segments) {
      if (current == null || StrUtil.isBlank(segment)) {
        return null;
      }
      if (current instanceof JSONObject) {
        current = ((JSONObject) current).get(segment);
      } else if (current instanceof Map) {
        current = ((Map<?, ?>) current).get(segment);
      } else {
        return null;
      }
    }
    return current;
  }

  private static JSONObject parseJsonObject(String json) {
    if (StrUtil.isBlank(json)) {
      return null;
    }
    try {
      return JSONUtil.parseObj(json);
    } catch (Exception e) {
      return null;
    }
  }

  private static Object getAutoFillValue(String fieldName, IoTDevice device, JSONObject shadowMetadata) {
    if (device == null || StrUtil.isBlank(fieldName)) {
      return null;
    }
    if (matchesAny(fieldName, "deviceId", "childDeviceId", "deviceCode")) {
      return device.getDeviceId();
    }

    Object shadowValue = getSimpleValueFromShadow(shadowMetadata, fieldName);
    if (shadowValue != null) {
      return shadowValue;
    }

    Object configValue = getSimpleValueFromConfig(device.getConfiguration(), fieldName);
    if (configValue != null) {
      return configValue;
    }

    if (matchesAny(fieldName, "meterNo", "imei", "imsi", "iccid")) {
      return device.getDeviceId();
    }
    return null;
  }

  private static boolean matchesAny(String target, String... values) {
    for (String value : values) {
      if (StrUtil.equals(target, value)) {
        return true;
      }
    }
    return false;
  }

  private static Object getSimpleValueFromShadow(JSONObject shadowMetadata, String fieldName) {
    if (shadowMetadata == null || StrUtil.isBlank(fieldName)) {
      return null;
    }
    JSONObject state = shadowMetadata.getJSONObject("state");
    if (state == null) {
      return null;
    }
    Object reportedValue = getSimpleValue(state.getJSONObject("reported"), fieldName);
    if (reportedValue != null) {
      return reportedValue;
    }
    return getSimpleValue(state.getJSONObject("desired"), fieldName);
  }

  private static Object getSimpleValueFromConfig(String configuration, String fieldName) {
    JSONObject config = parseJsonObject(configuration);
    return getSimpleValue(config, fieldName);
  }

  private static Object getSimpleValue(JSONObject source, String fieldName) {
    if (source == null || StrUtil.isBlank(fieldName)) {
      return null;
    }
    Object value = source.get(fieldName);
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
      return value;
    }
    return null;
  }

  private static Object coerceValueByDefinition(Object rawValue, JSONObject inputDefinition) {
    if (rawValue == null || inputDefinition == null) {
      return rawValue;
    }
    JSONObject valueType = inputDefinition.getJSONObject("valueType");
    String type = normalizeType(valueType == null ? null : valueType.getStr("type"));
    if (StrUtil.isBlank(type)) {
      return rawValue;
    }
    if ("string".equals(type)
        || "text".equals(type)
        || "password".equals(type)
        || "file".equals(type)
        || "date".equals(type)) {
      return rawValue instanceof String ? rawValue : String.valueOf(rawValue);
    }
    if ("boolean".equals(type)) {
      return coerceBoolean(rawValue);
    }
    if ("int".equals(type)) {
      return coerceInteger(rawValue, false);
    }
    if ("long".equals(type)) {
      return coerceInteger(rawValue, true);
    }
    if ("float".equals(type) || "double".equals(type)) {
      return coerceDecimal(rawValue);
    }
    if ("object".equals(type)) {
      if (rawValue instanceof JSONObject) {
        return rawValue;
      }
      if (rawValue instanceof Map) {
        return JSONUtil.parseObj(rawValue);
      }
      if (rawValue instanceof String && JSONUtil.isTypeJSONObject((String) rawValue)) {
        return JSONUtil.parseObj(rawValue);
      }
      return rawValue;
    }
    if ("array".equals(type)) {
      if (rawValue instanceof JSONArray) {
        return rawValue;
      }
      if (rawValue instanceof Iterable || rawValue.getClass().isArray()) {
        return JSONUtil.parseArray(rawValue);
      }
      if (rawValue instanceof String && JSONUtil.isTypeJSONArray((String) rawValue)) {
        return JSONUtil.parseArray(rawValue);
      }
      return rawValue;
    }
    return rawValue;
  }

  private static Object coerceInteger(Object value, boolean preferLong) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number) {
      Number number = (Number) value;
      return preferLong ? number.longValue() : number.intValue();
    }
    String text = StrUtil.trim(String.valueOf(value));
    if (!NumberUtil.isNumber(text)) {
      return value;
    }
    if (preferLong) {
      return text.contains(".") ? Double.valueOf(text).longValue() : Long.valueOf(text);
    }
    return text.contains(".") ? Double.valueOf(text).intValue() : Integer.valueOf(text);
  }

  private static Object coerceDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    String text = StrUtil.trim(String.valueOf(value));
    if (!NumberUtil.isNumber(text)) {
      return value;
    }
    return Double.valueOf(text);
  }

  private static Object coerceBoolean(Object value) {
    if (value == null || value instanceof Boolean) {
      return value;
    }
    String text = StrUtil.trim(String.valueOf(value));
    if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
      return Boolean.valueOf(text);
    }
    return value;
  }

  private static String normalizeType(String type) {
    String normalized = StrUtil.trim(type);
    if (StrUtil.isBlank(normalized)) {
      return null;
    }
    if ("bool".equalsIgnoreCase(normalized)) {
      return "boolean";
    }
    if ("struct".equalsIgnoreCase(normalized)) {
      return "object";
    }
    if ("geo_point".equalsIgnoreCase(normalized)) {
      return "geoPoint";
    }
    return normalized;
  }

  private static boolean isRequiredField(JSONObject inputDefinition) {
    if (inputDefinition == null) {
      return false;
    }
    if (inputDefinition.containsKey("required")) {
      return inputDefinition.getBool("required", false);
    }
    JSONObject expands = inputDefinition.getJSONObject("expands");
    return expands != null && expands.getBool("required", false);
  }

  private static boolean isMissing(Object value) {
    return value == null || (value instanceof CharSequence && StrUtil.isBlank((CharSequence) value));
  }

  @Getter
  public static class ResolvedArguments {

    private final JSONObject data;
    private final JSONObject details;

    public ResolvedArguments(JSONObject data, JSONObject details) {
      this.data = data == null ? new JSONObject() : data;
      this.details = details == null ? new JSONObject() : details;
    }
  }
}
