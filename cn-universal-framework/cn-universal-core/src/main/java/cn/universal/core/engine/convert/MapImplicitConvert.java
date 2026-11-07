package cn.universal.core.engine.convert;

import cn.universal.core.engine.functions.MapExtension;
import cn.universal.core.engine.reflection.JavaReflection;
import cn.universal.core.engine.runtime.Variables;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/** Map到Bean的隐式转换 */
public class MapImplicitConvert implements ClassImplicitConvert {

  @Override
  public boolean support(Class<?> from, Class<?> to) {
    return Map.class.isAssignableFrom(from)
        && !JavaReflection.isPrimitiveAssignableFrom(to, to)
        && !to.isArray()
        && !Collection.class.isAssignableFrom(to)
        && !Iterator.class.isAssignableFrom(to)
        && !Enumeration.class.isAssignableFrom(to)
        && !to.isInterface();
  }

  @Override
  public Object convert(Variables variables, Object source, Class<?> target) {
    return MapExtension.asBean((Map<?, ?>) source, target);
  }
}
