package cn.universal.core.event;

import java.util.*;

/** Trusted routing identity and a detached JSON snapshot. No application binding is required. */
public record DeviceUplinkEvent(
    Identity identity, Map<String, Object> payload, long estimatedBytes) {
  public record DeviceKey(String productKey, String deviceId) {}

  public record Identity(
      String tenantId,
      String iotId,
      String productKey,
      String deviceId,
      String productName,
      String deviceName,
      String applicationId,
      String appId,
      List<String> groupIds,
      String messageType,
      String event) {
    public Identity {
      groupIds = groupIds == null ? List.of() : List.copyOf(groupIds);
    }

    public DeviceKey deviceKey() {
      return new DeviceKey(productKey, deviceId);
    }
  }

  public static DeviceUplinkEvent snapshot(Identity identity, Map<String, Object> payload) {
    long[] bytes = {256};
    @SuppressWarnings("unchecked")
    Map<String, Object> detached = (Map<String, Object>) freeze(payload, bytes, 0);
    return new DeviceUplinkEvent(identity, detached, bytes[0]);
  }

  private static final ClassValue<java.lang.reflect.RecordComponent[]> RECORD_FIELDS =
      new ClassValue<>() {
        @Override
        protected java.lang.reflect.RecordComponent[] computeValue(Class<?> type) {
          var components = type.getRecordComponents();
          for (var component : components) component.getAccessor().trySetAccessible();
          return components;
        }
      };

  private static Object freeze(Object value, long[] bytes, int depth) {
    if (depth > 64) throw new IllegalArgumentException("设备消息嵌套超过 64 层");
    bytes[0] += 32;
    if (bytes[0] > 4 * 1024 * 1024) throw new IllegalArgumentException("设备消息快照超过 4 MiB");
    if (value == cn.hutool.json.JSONNull.NULL) return null;
    if (value instanceof Enum<?> enumeration) return freeze(enumeration.name(), bytes, depth);
    if (value instanceof java.time.temporal.TemporalAccessor temporal)
      return freeze(temporal.toString(), bytes, depth);
    if (value instanceof java.util.Date date) return freeze(date.getTime(), bytes, depth);
    if (value != null && value.getClass().isArray()) {
      int length = java.lang.reflect.Array.getLength(value);
      if (length > 131072) throw new IllegalArgumentException("设备消息数组过大");
      List<Object> result = new ArrayList<>(length);
      for (int i = 0; i < length; i++)
        result.add(freeze(java.lang.reflect.Array.get(value, i), bytes, depth + 1));
      return Collections.unmodifiableList(result);
    }
    if (value != null && value.getClass().isRecord()) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (var field : RECORD_FIELDS.get(value.getClass())) {
        try {
          result.put(field.getName(), field.getAccessor().invoke(value));
        } catch (ReflectiveOperationException e) {
          throw new IllegalArgumentException("设备采样无法转换为 JSON", e);
        }
      }
      return freeze(result, bytes, depth + 1);
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> result = new LinkedHashMap<>();
      map.forEach(
          (key, item) -> {
            if (!(key instanceof String name)) throw new IllegalArgumentException("设备消息字段名必须为字符串");
            bytes[0] += 48L + name.length() * 2L;
            result.put(name, freeze(item, bytes, depth + 1));
          });
      return Collections.unmodifiableMap(result);
    }
    if (value instanceof Collection<?> values) {
      List<Object> result = new ArrayList<>(values.size());
      for (Object item : values) result.add(freeze(item, bytes, depth + 1));
      return Collections.unmodifiableList(result);
    }
    if (value instanceof String text) bytes[0] += text.length() * 2L;
    else if (value != null
        && !(value instanceof Byte
            || value instanceof Short
            || value instanceof Integer
            || value instanceof Long
            || value instanceof Float
            || value instanceof Double
            || value instanceof java.math.BigDecimal
            || value instanceof java.math.BigInteger)
        && !(value instanceof Boolean))
      throw new IllegalArgumentException("设备事件不支持的字段类型: " + value.getClass().getName());
    if (bytes[0] > 4 * 1024 * 1024) throw new IllegalArgumentException("设备消息快照超过 4 MiB");
    return value;
  }
}
