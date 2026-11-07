package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** 类型判断 */
public class ObjectTypeConditionExtension {

  /** 判断是否是目标类型 */
  @Comment("判断对象是否为指定类型的对象")
  public static boolean is(Object target, @Comment(name = "clazz", value = "目标类型") Class<?> clazz) {
    if (target == null || clazz == null) {
      return false;
    }
    return clazz.isAssignableFrom(target.getClass());
  }

  /** 判断是否是数组 */
  @Comment("判断对象是否是数组")
  public static boolean isArray(Object target) {
    if (target instanceof Class) {
      return ((Class<?>) target).isArray();
    }
    return target.getClass().isArray();
  }

  /** 判断是否是集合 */
  @Comment("判断对象是否是集合")
  public static boolean isCollection(Object target) {
    if (target instanceof Class) {
      return Collection.class.isAssignableFrom((Class<?>) target);
    }
    return Collection.class.isAssignableFrom(target.getClass());
  }

  /** 判断是否是Map */
  @Comment("判断对象是否是Map")
  public static boolean isMap(Object target) {
    if (target instanceof Class) {
      return Map.class.isAssignableFrom((Class<?>) target);
    }
    return Map.class.isAssignableFrom(target.getClass());
  }

  /** 判断是否是目标类型 */
  @Comment("判断对象是否为指定类型的对象，type为null时 返回false，支持类名缩写")
  public boolean is(
      Object target,
      @Comment(
              name = "type",
              value = "类名或全类名或string、int、double、float、long、byte、short、bigdecimal、boolean")
          String type) {
    if (type == null) {
      return false;
    }
    Class<?> clazz;
    if (target instanceof Class) {
      clazz = (Class<?>) target;
    } else {
      clazz = target.getClass();
    }
    if (clazz.getName().equals(type) || clazz.getSimpleName().equals(type)) {
      return true;
    }
    if ("string".equalsIgnoreCase(type) && clazz == String.class) {
      return true;
    }
    if ("int".equalsIgnoreCase(type) && clazz == Integer.class) {
      return true;
    }
    if ("double".equalsIgnoreCase(type) && clazz == Double.class) {
      return true;
    }
    if ("float".equalsIgnoreCase(type) && clazz == Float.class) {
      return true;
    }
    if ("long".equalsIgnoreCase(type) && clazz == Long.class) {
      return true;
    }
    if ("byte".equalsIgnoreCase(type) && clazz == Byte.class) {
      return true;
    }
    if ("short".equalsIgnoreCase(type) && clazz == Short.class) {
      return true;
    }
    if ("boolean".equalsIgnoreCase(type) && clazz == Boolean.class) {
      return true;
    }
    return "decimal".equalsIgnoreCase(type) && clazz == BigDecimal.class;
  }

  /** 判断是否是String */
  @Comment("判断对象是否是String类型")
  public boolean isString(Object target) {
    return is(target, String.class);
  }

  /** 判断是否是int */
  @Comment("判断对象是否是int类型")
  public boolean isInt(Object target) {
    return is(target, Integer.class);
  }

  /** 判断是否是double */
  @Comment("判断对象是否是double类型")
  public boolean isDouble(Object target) {
    return is(target, Double.class);
  }

  /** 判断是否是long */
  @Comment("判断对象是否是long类型")
  public boolean isLong(Object target) {
    return is(target, Long.class);
  }

  /** 判断是否是byte */
  @Comment("判断对象是否是byte类型")
  public boolean isByte(Object target) {
    return is(target, Byte.class);
  }

  /** 判断是否是short */
  @Comment("判断对象是否是short类型")
  public boolean isShort(Object target) {
    return is(target, Short.class);
  }

  /** 判断是否是boolean */
  @Comment("判断对象是否是boolean类型")
  public boolean isBoolean(Object target) {
    return is(target, Boolean.class);
  }

  /** 判断是否是BigDecimal */
  @Comment("判断对象是否是BigDecimal类型")
  public boolean isDecimal(Object target) {
    return is(target, BigDecimal.class);
  }

  /** 判断是否是Date */
  @Comment("判断对象是否是Date类型")
  public boolean isDate(Object target) {
    return is(target, Date.class);
  }

  /** 判断是否是List */
  @Comment("判断对象是否是List")
  public boolean isList(Object target) {
    if (target instanceof Class) {
      return List.class.isAssignableFrom((Class<?>) target);
    }
    return List.class.isAssignableFrom(target.getClass());
  }
}
