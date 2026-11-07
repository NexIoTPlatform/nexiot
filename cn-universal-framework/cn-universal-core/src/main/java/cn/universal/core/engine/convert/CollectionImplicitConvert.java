package cn.universal.core.engine.convert;

import cn.universal.core.engine.functions.StreamExtension;
import cn.universal.core.engine.reflection.JavaReflection;
import cn.universal.core.engine.runtime.Variables;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/** 集合、数组隐式转换 */
public class CollectionImplicitConvert implements ClassImplicitConvert {

  private Class<?> fromClazz;

  private Class<?> toClazz;

  @Override
  public boolean support(Class<?> from, Class<?> to) {
    if ((from.isArray()
            || Collection.class.isAssignableFrom(from)
            || Iterator.class.isAssignableFrom(from)
            || Enumeration.class.isAssignableFrom(from))
        && (to.isArray()
            || Collection.class.isAssignableFrom(to)
            || Iterator.class.isAssignableFrom(to)
            || Enumeration.class.isAssignableFrom(to))) {
      return (fromClazz = getGenericType(from)) != null
          && !JavaReflection.isPrimitiveAssignableFrom(fromClazz, fromClazz)
          && (toClazz = getGenericType(to)) != null
          && !JavaReflection.isPrimitiveAssignableFrom(toClazz, toClazz)
          && toClazz.isAssignableFrom(fromClazz);
    }
    return false;
  }

  private Class<?> getGenericType(Class<?> target) {
    Type type = target.getGenericSuperclass();
    if (type instanceof ParameterizedType) {
      type = ((ParameterizedType) type).getActualTypeArguments()[0];
      if (type instanceof Class) {
        return (Class<?>) type;
      }
    }
    return null;
  }

  @Override
  public Object convert(Variables variables, Object source, Class<?> target) {
    return StreamExtension.asBean(source, target, target.isArray());
  }
}
