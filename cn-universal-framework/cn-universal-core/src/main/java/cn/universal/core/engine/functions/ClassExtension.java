package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.reflection.JavaInvoker;
import cn.universal.core.engine.reflection.JavaReflection;
import cn.universal.core.engine.runtime.RuntimeContext;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

public class ClassExtension {

  public static Object newInstance(Class<?> clazz)
      throws IllegalAccessException, InstantiationException {
    return clazz.newInstance();
  }

  public static Object newInstance(
      Class<?> clazz,
      RuntimeContext runtimeContext,
      @Comment(name = "values", value = "构造参数") Object... values)
      throws Throwable {
    if (values == null || values.length == 0) {
      return newInstance(clazz);
    }
    Class<?>[] parametersTypes = new Class<?>[values.length];
    for (int i = 0; i < values.length; i++) {
      Object value = values[i];
      parametersTypes[i] = value == null ? JavaReflection.Null.class : value.getClass();
    }
    List<Constructor<?>> constructors = Arrays.asList(clazz.getConstructors());
    JavaInvoker<Constructor> invoker =
        JavaReflection.findConstructorInvoker(constructors, parametersTypes);
    if (invoker == null) {
      throw new RuntimeException(
          String.format(
              "can not found constructor for [%s] with types: [%s]",
              clazz, Arrays.toString(parametersTypes)));
    }
    return invoker.invoke0(null, runtimeContext, values);
  }

  public static Object newInstance(
      Object target,
      RuntimeContext runtimeContext,
      @Comment(name = "values", value = "构造参数") Object... values)
      throws Throwable {
    if (target == null) {
      throw new NullPointerException("NULL不能被new");
    }
    if (target instanceof Class) {
      return newInstance((Class<?>) target, runtimeContext, values);
    }
    return newInstance(target.getClass(), runtimeContext, values);
  }

  /**
   * @since 1.6.2
   */
  @Comment("获取Java类全名")
  public static String getName(Class<?> clazz) {
    return clazz.getName();
  }

  /**
   * @since 1.6.2
   */
  @Comment("获取Java类名")
  public static String getSimpleName(Class<?> clazz) {
    return clazz.getSimpleName();
  }

  /**
   * @since 1.6.2
   */
  @Comment("获取Java类规范全名")
  public static String getCanonicalName(Class<?> clazz) {
    return clazz.getCanonicalName();
  }
}
