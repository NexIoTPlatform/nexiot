package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.parsing.ast.binary.LessOperation;
import cn.universal.core.engine.reflection.JavaReflection;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class MapExtension {

  @Comment("Map类型对象转JavaBean")
  public static Object asBean(
      Map<?, ?> source, @Comment(name = "target", value = "目标Class") Class<?> target) {
    Object result = null;
    try {
      result = target.newInstance();
      Set<? extends Map.Entry<?, ?>> entries = source.entrySet();
      for (Map.Entry<?, ?> entry : entries) {
        Object value = entry.getValue();
        String member = Objects.toString(entry.getKey(), null);
        if (value != null && member != null) {
          Field field = JavaReflection.getField(target, member);
          setFieldValue(result, field, value);
        }
      }
    } catch (InstantiationException | IllegalAccessException ignored) {
    }
    return result;
  }

  @Comment("map转List")
  public static List<?> asList(
      Map<?, ?> source,
      @Comment(name = "mapping", value = "映射函数，如:(key,value,source)=>{'k' : key,'v' : value}")
          Function<Object[], Object> mapping) {
    List<Object> result = new ArrayList<>();
    source.forEach((key, value) -> result.add(mapping.apply(new Object[] {key, value, source})));
    return result;
  }

  private static void setFieldValue(Object object, Field field, Object value) {
    if (field != null) {
      try {
        if (ObjectTypeConditionExtension.isCollection(field.getType())) {
          Type genericType = field.getGenericType();
          if (genericType instanceof ParameterizedType) {
            Class<?> type =
                (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
            JavaReflection.setFieldValue(object, field, StreamExtension.asBean(value, type));
          }
        } else if (field.getType().isArray()) {
          JavaReflection.setFieldValue(
              object,
              field,
              StreamExtension.asBean(value, field.getType().getComponentType(), true));
        } else if (JavaReflection.isPrimitiveAssignableFrom(value.getClass(), field.getType())
            || field.getType().isAssignableFrom(value.getClass())) {
          JavaReflection.setFieldValue(object, field, value);
        }
      } catch (Exception ignored) {
      }
    }
  }

  @Comment(value = "循环Map", origin = true)
  public Map<?, ?> each(
      Map<?, ?> source,
      @Comment(name = "function", value = "循环函数，如:(key,value,source)=>map['xx'] = key;")
          Function<Object[], Object> function) {
    source.forEach((key, value) -> function.apply(new Object[] {key, value, source}));
    return source;
  }

  @Comment("合并Map")
  public Map<?, ?> merge(
      Map<Object, Object> source,
      @Comment(name = "key", value = "key") Object key,
      @Comment(name = "value", value = "value") Object value) {
    source.put(key, value);
    return source;
  }

  @Comment("合并Map")
  public Map<?, ?> merge(
      Map<Object, Object> source, @Comment("另一个map或多个Map") Map<Object, Object>... targets) {
    if (targets != null) {
      for (int i = 0, len = targets.length; i < len; i++) {
        source.putAll(targets[i]);
      }
    }
    return source;
  }

  @Comment("将Map转为String")
  public String asString(
      Map<?, ?> source,
      @Comment(name = "separator", value = "key与key之间的连接符如&") String separator,
      @Comment(name = "join", value = "key与value之间的连接符，如=") String join) {
    Set<? extends Map.Entry<?, ?>> entries = source.entrySet();
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<?, ?> entry : entries) {
      builder.append(entry.getKey());
      builder.append(join);
      builder.append(Objects.toString(entry.getValue(), ""));
      builder.append(separator);
    }
    if (entries.size() > 0) {
      return builder.substring(0, builder.length() - separator.length());
    }
    return builder.toString();
  }

  @Comment("将Map转为String")
  public String asString(
      Map<?, ?> source,
      @Comment(name = "separator", value = "key与value之间的连接符，如=") String separator,
      @Comment(name = "mapping", value = "转换方法，如：(key,value)=>key + '=' + value || ''")
          Function<Object[], Object> mapping) {
    Set<? extends Map.Entry<?, ?>> entries = source.entrySet();
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<?, ?> entry : entries) {
      builder.append(
          Objects.toString(mapping.apply(new Object[] {entry.getKey(), entry.getValue()}), ""));
      builder.append(separator);
    }
    if (entries.size() > 0) {
      return builder.substring(0, builder.length() - separator.length());
    }
    return builder.toString();
  }

  @Comment("对Map进行排序")
  public Map<?, ?> sort(Map<?, ?> source) {
    Set<?> keys = source.keySet();
    Map<Object, Object> sortedMap = new LinkedHashMap<>();
    keys.stream()
        .sorted(
            (Comparator<Object>)
                (o1, o2) -> {
                  if (Objects.equals(o1, o2)) {
                    return 0;
                  }
                  int value = LessOperation.compare(o1, o2);
                  return value == -2 ? 0 : value;
                })
        .forEach(
            key -> {
              sortedMap.put(key, source.get(key));
            });
    return sortedMap;
  }

  @Comment("对Map进行排序")
  public Map<?, ?> sort(
      Map<?, ?> source,
      @Comment(name = "comparator", value = "比较器，如:(k1,k2,v1,v2)=>k1.compareTo(k2);")
          Function<Object[], Object> comparator) {
    Set<?> keys = source.keySet();
    Map<Object, Object> sortedMap = new LinkedHashMap<>();
    keys.stream()
        .sorted(
            (Comparator<Object>)
                (o1, o2) ->
                    ObjectConvertExtension.asInt(
                        comparator.apply(new Object[] {o1, o2, source.get(o1), source.get(o2)}), 0))
        .forEach(
            key -> {
              sortedMap.put(key, source.get(key));
            });
    return sortedMap;
  }

  @Comment("对Map的key进行替换")
  public Map<String, Object> replaceKey(
      Map<String, Object> source,
      @Comment(name = "target", value = "查找字符串") String target,
      @Comment(name = "replacement", value = "替换字符串") String replacement) {
    return replaceKey(source, src -> src[0].toString().replace(target, replacement));
  }

  @Comment("对Map的key进行正则替换")
  public Map<String, Object> replaceAllKey(
      Map<String, Object> source,
      @Comment(name = "pattern", value = "正则表达式") String regx,
      @Comment(name = "replacement", value = "替换字符串") String replacement) {
    return replaceKey(source, src -> src[0].toString().replaceAll(regx, replacement));
  }

  @Comment("对Map的key进行替换")
  public Map<String, Object> replaceKey(
      Map<String, Object> source,
      @Comment(name = "function", value = "回调函数") Function<Object[], String> functional) {
    Map<String, Object> result = new LinkedHashMap<>();
    Set<Map.Entry<String, Object>> entries = source.entrySet();
    for (Map.Entry<String, Object> entry : entries) {
      result.put(functional.apply(new Object[] {entry.getKey()}), entry.getValue());
    }
    return result;
  }
}
