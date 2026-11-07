package cn.universal.admin.platform.service.debug;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** 断点处的有界快照：读取字段，不执行 getter、用户 toString 或 Jackson Bean 序列化。 */
final class DriverVariableInspector {
  private static final int MAX_NODES = 500;
  private static final int MAX_MEMBERS = 50;
  private static final int MAX_DEPTH = 4;
  private final Set<Object> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
  private int remaining = MAX_NODES;
  private int reservedRoots;
  private int remainingText = 65536;

  static List<Map<String, Object>> inspect(Map<String, Object> variables) {
    DriverVariableInspector inspector = new DriverVariableInspector();
    List<Map<String, Object>> result = new ArrayList<>();
    var entries = new ArrayList<>(variables.entrySet());
    Collections.reverse(entries);
    inspector.reservedRoots = Math.min(entries.size(), 100);
    for (var entry : entries) {
      if (result.size() >= 100 || inspector.remaining <= 0) {
        result.add(Map.of("name", "…", "type", "", "value", "变量过多，快照已截断"));
        break;
      }
      inspector.reservedRoots--;
      result.add(inspector.node(entry.getKey(), entry.getValue(), 0));
    }
    return result;
  }

  private Map<String, Object> node(String name, Object value, int depth) {
    remaining--;
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("name", text(name));
    node.put("type", value == null ? "null" : value.getClass().getTypeName());
    node.put("kind", "value");
    node.put("value", value == null ? "null" : "{…}");
    if (value == null) return node;
    Class<?> type = value.getClass();
    if (value instanceof Class<?> clazz) {
      node.put("kind", "class");
      node.put("value", clazz.getTypeName());
      return node;
    }
    if (type == String.class || type == Character.class) {
      node.put("kind", "string");
      node.put("value", text(String.valueOf(value)));
      return node;
    }
    if (type == Integer.class
        || type == Long.class
        || type == Short.class
        || type == Byte.class
        || type == Double.class
        || type == Float.class
        || type == Boolean.class
        || type == BigInteger.class
        || type == BigDecimal.class) {
      node.put("value", text(String.valueOf(value)));
      return node;
    }
    if (value instanceof Enum<?> enumValue) {
      node.put("value", enumValue.name());
      return node;
    }
    if (type.isSynthetic()) {
      node.put("value", "函数");
      return node;
    }
    if (!ancestors.add(value)) {
      node.put("notice", "循环引用");
      return node;
    }
    List<Map<String, Object>> children = new ArrayList<>();
    try {
      int members;
      List<Field> fields = List.of();
      if (type.isArray()) members = Array.getLength(value);
      else if (value instanceof Map<?, ?> map) members = map.size();
      else if (value instanceof Collection<?> collection) members = collection.size();
      else {
        fields = fields(type);
        members = fields.size();
      }
      node.put("members", members);
      node.put(
          "value",
          "members = " + members + (!fields.isEmpty() && members > MAX_MEMBERS ? "+" : ""));
      if (value instanceof byte[] bytes) {
        node.put("kind", "bytes");
        node.put(
            "value",
            "Base64: "
                + Base64.getEncoder()
                    .encodeToString(Arrays.copyOf(bytes, Math.min(bytes.length, 192))));
        if (bytes.length > 192) node.put("notice", "Base64 预览前 192 字节");
      }
      if (depth >= MAX_DEPTH && members > 0) {
        node.put("notice", "达到展开深度上限");
        return node;
      }
      if (type.isArray()) {
        for (int i = 0; i < members && canAdd(children); i++)
          children.add(node("[" + i + "]", Array.get(value, i), depth + 1));
      } else if (value instanceof Map<?, ?> map) {
        for (var entry : map.entrySet()) {
          if (!canAdd(children)) break;
          children.add(node(key(entry.getKey()), entry.getValue(), depth + 1));
        }
      } else if (value instanceof Collection<?> collection) {
        for (Object item : collection) {
          if (!canAdd(children)) break;
          children.add(node("[" + children.size() + "]", item, depth + 1));
        }
      } else {
        for (Field field : fields) {
          if (!canAdd(children)) break;
          try {
            if (!field.trySetAccessible()) throw new IllegalAccessException();
            children.add(node(field.getName(), field.get(value), depth + 1));
          } catch (RuntimeException | IllegalAccessException e) {
            remaining--;
            children.add(
                Map.of(
                    "name",
                    field.getName(),
                    "type",
                    field.getType().getTypeName(),
                    "value",
                    "不可访问",
                    "notice",
                    "Java 模块访问受限"));
          }
        }
      }
      if (children.size() < members) node.put("notice", "仅显示前 " + children.size() + " 个成员，快照已截断");
    } catch (RuntimeException e) {
      // 并发集合变化、模块访问限制不应让脚本调试失败。
      node.put("notice", "无法读取全部成员");
    } finally {
      ancestors.remove(value);
    }
    if (!children.isEmpty()) node.put("children", children);
    return node;
  }

  private boolean canAdd(List<?> children) {
    return remaining > reservedRoots && children.size() < MAX_MEMBERS;
  }

  private List<Field> fields(Class<?> type) {
    List<Field> fields = new ArrayList<>();
    for (Class<?> current = type;
        current != null && current != Object.class;
        current = current.getSuperclass()) {
      for (Field field : current.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) fields.add(field);
        if (fields.size() > MAX_MEMBERS) return fields;
      }
    }
    return fields;
  }

  private String key(Object value) {
    if (value == null) return "null";
    if (value instanceof String string) return string;
    if (value instanceof Enum<?> enumValue) return enumValue.name();
    Class<?> type = value.getClass();
    if (type == Integer.class || type == Long.class || type == Boolean.class)
      return String.valueOf(value);
    return "[" + type.getTypeName() + "]";
  }

  private String text(String value) {
    int length = Math.min(Math.min(2000, remainingText), value.length());
    remainingText -= length;
    return value.substring(0, length) + (length < value.length() ? "…（已截断）" : "");
  }
}
