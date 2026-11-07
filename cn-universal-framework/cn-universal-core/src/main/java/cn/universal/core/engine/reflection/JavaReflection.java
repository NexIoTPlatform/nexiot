package cn.universal.core.engine.reflection;

import cn.universal.core.engine.convert.BooleanImplicitConvert;
import cn.universal.core.engine.convert.ClassImplicitConvert;
import cn.universal.core.engine.convert.CollectionImplicitConvert;
import cn.universal.core.engine.convert.FunctionalImplicitConvert;
import cn.universal.core.engine.convert.MapImplicitConvert;
import cn.universal.core.engine.functions.ArrayFunctions;
import cn.universal.core.engine.functions.ClassExtension;
import cn.universal.core.engine.functions.CollectionFunctions;
import cn.universal.core.engine.functions.DateExtension;
import cn.universal.core.engine.functions.MagicScriptFunctions;
import cn.universal.core.engine.functions.MapExtension;
import cn.universal.core.engine.functions.NumberExtension;
import cn.universal.core.engine.functions.ObjectConvertExtension;
import cn.universal.core.engine.functions.ObjectTypeConditionExtension;
import cn.universal.core.engine.functions.PatternExtension;
import cn.universal.core.engine.functions.StreamExtension;
import cn.universal.core.engine.functions.StringExtension;
import cn.universal.core.engine.functions.StringFunctions;
import cn.universal.core.engine.functions.TemporalAccessorExtension;
import cn.universal.core.engine.functions.linq.AggregationFunctions;
import cn.universal.core.engine.functions.linq.LinqFunctions;
import cn.universal.core.engine.functions.linq.MathFunctions;
import cn.universal.core.engine.runtime.RuntimeContext;
import java.beans.Transient;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaReflection {

  private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();
  private static final List<ClassImplicitConvert> CONVERTS = new ArrayList<>();
  private static final Map<Class<?>, Map<String, List<JavaInvoker<Method>>>>
      EXTENSION_METHOD_CACHE = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Map<MethodSignature, JavaInvoker<Method>>> METHOD_CACHE =
      new ConcurrentHashMap<>();
  private static final Map<Class<?>, List<Class<?>>> EXTENSION_MAP = new ConcurrentHashMap<>();
  private static final List<JavaInvoker<Method>> FUNCTIONS = new ArrayList<>();

  static {
    registerMethodExtension(Class.class, new ClassExtension());
    StreamExtension streamExtension = new StreamExtension();
    registerMethodExtension(Collection.class, streamExtension);
    registerMethodExtension(Object[].class, streamExtension);
    registerMethodExtension(Enumeration.class, streamExtension);
    registerMethodExtension(Iterator.class, streamExtension);
    registerMethodExtension(Object.class, new ObjectConvertExtension());
    registerMethodExtension(Object.class, new ObjectTypeConditionExtension());
    registerMethodExtension(Map.class, new MapExtension());
    registerMethodExtension(Date.class, new DateExtension());
    registerMethodExtension(TemporalAccessorExtension.class, new TemporalAccessorExtension());
    registerMethodExtension(Number.class, new NumberExtension());
    registerMethodExtension(Pattern.class, new PatternExtension());
    registerMethodExtension(String.class, new StringExtension());
    // Map 到 Bean 隐式转换
    registerImplicitConvert(new MapImplicitConvert());
    // 集合 到 List<Bean> 的转换
    registerImplicitConvert(new CollectionImplicitConvert());
    // lambda 到 functional 的转换
    registerImplicitConvert(new FunctionalImplicitConvert());
    // 任意值 到 Boolean 的转换
    registerImplicitConvert(new BooleanImplicitConvert());

    registerFunction(new AggregationFunctions());
    registerFunction(new LinqFunctions());
    registerFunction(new CollectionFunctions());
    registerFunction(new MathFunctions());
    registerFunction(new StringFunctions());
    registerFunction(new MagicScriptFunctions());
    registerFunction(new ArrayFunctions());
    /** 功能扩展 */
  }

  public static void registerFunction(Object target) {
    Stream.of(target.getClass().getMethods())
        .filter(
            method ->
                method.getAnnotation(cn.universal.core.engine.annotation.Function.class) != null)
        .map(MethodInvoker::new)
        .forEach(
            it -> {
              it.setDefaultTarget(target);
              FUNCTIONS.add(it);
            });
  }

  public static Map<Class<?>, List<Class<?>>> getExtensionMap() {
    return EXTENSION_MAP;
  }

  public static List<JavaInvoker<Method>> getFunctions() {
    return FUNCTIONS;
  }

  private static MethodInvoker findApply(Class<?> cls) {
    for (Method method : cls.getDeclaredMethods()) {
      if ("apply".equals(method.getName())) {
        return new MethodInvoker(method);
      }
    }
    return null;
  }

  private static int calcToObjectDistanceWithInterface(
      Class<?>[] interfaces, int distance, int score) {
    if (interfaces == null) {
      return distance;
    }
    return Arrays.stream(interfaces)
        .mapToInt(
            i -> {
              int v = calcToObjectDistanceWithInterface(i.getInterfaces(), distance, score + 2);
              return v + distance + score;
            })
        .sum();
  }

  private static int calcToObjectDistance(Class<?> clazz) {
    return calcToObjectDistance(clazz, 0);
  }

  private static int calcToObjectDistance(Class<?> clazz, int distance) {
    if (clazz == null) {
      return distance + 3;
    }
    if (Object.class.equals(clazz)) {
      return distance;
    }
    int interfaceScore = calcToObjectDistanceWithInterface(clazz.getInterfaces(), distance + 2, 0);
    if (clazz.isInterface()) {
      return interfaceScore;
    }
    int classScore = calcToObjectDistance(clazz.getSuperclass(), distance + 3);
    return classScore + interfaceScore;
  }

  private static boolean isImplicitConvert(Class<?> from, Class<?> to) {
    if (isPrimitiveAssignableFrom(from, from) || isPrimitiveAssignableFrom(to, to)) {
      return false;
    } else if (Collection.class.isAssignableFrom(to)
        || Iterator.class.isAssignableFrom(to)
        || Enumeration.class.isAssignableFrom(to)
        || to.isArray()) {
      Class<?> toClazz = getGenericType(to);
      return toClazz != null && (!isPrimitiveAssignableFrom(toClazz, toClazz));
    }
    return Map.class.isAssignableFrom(from);
  }

  private static int matchTypes(
      JavaInvoker<?> invoker,
      Class<?>[] parameterTypes,
      Class<?>[] otherTypes,
      boolean matchCount) {
    if (matchCount && parameterTypes.length != otherTypes.length) {
      return -1;
    }
    int score = 0;
    for (int ii = 0, nn = parameterTypes.length; ii < nn; ii++) {
      Class<?> type = parameterTypes[ii];
      Class<?> otherType = otherTypes[ii];
      if (RuntimeContext.class.isAssignableFrom(otherType)) {
        score += 1000;
      } else if (Null.class.equals(type)) {
        if (otherType.isPrimitive()) {
          score = -1;
          break;
        }
        score += 1000;
      } else if (!isPrimitiveAssignableFrom(type, otherType)) {
        score += 1000;
        if (!otherType.isAssignableFrom(type)) {
          score += 1000;
          if (!isCoercible(type, otherType)) {
            score += 2000;
            boolean found = false;
            for (ClassImplicitConvert convert : CONVERTS) {
              if (convert.support(type, otherType)) {
                invoker.addClassImplicitConvert(ii, convert);
                found = true;
                break;
              }
            }
            invoker.setImplicit(found);
            if (!found) {
              return -1;
            }
          }
        }
      }
    }
    return score;
  }

  private static Class<?> getGenericType(Class<?> target) {
    Type type = target.getGenericSuperclass();
    if (type instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }
    return null;
  }

  public static JavaInvoker<Method> findMethodInvoker(
      List<JavaInvoker<Method>> methods, Class<?>[] parameterTypes) {
    return findInvoker(methods, parameterTypes);
  }

  public static JavaInvoker<Constructor> findConstructorInvoker(
      List<Constructor<?>> constructors, Class<?>[] parameterTypes) {
    return findInvoker(
        constructors.stream().map(ConstructorInvoker::new).collect(Collectors.toList()),
        parameterTypes);
  }

  public static <T extends Executable> JavaInvoker<T> findInvoker(
      List<JavaInvoker<T>> executables, Class<?>[] parameterTypes) {
    JavaInvoker<T> foundInvoker = null;
    int foundScore = 0;
    List<JavaInvoker<T>> executableWithVarArgs = new ArrayList<>();
    for (JavaInvoker<T> invoker : executables) {
      // Check if the types match.
      Class<?>[] otherTypes = invoker.getParameterTypes();
      invoker = invoker.copy();
      int score = matchTypes(invoker, parameterTypes, otherTypes, true);
      if (score > -1) {
        if (foundInvoker == null) {
          foundInvoker = invoker;
          foundScore = score;
        } else {
          if (score < foundScore) {
            foundScore = score;
            foundInvoker = invoker;
          }
        }
      } else if (invoker.isVarArgs()) {
        executableWithVarArgs.add(invoker);
      }
    }
    if (foundInvoker == null) {
      for (JavaInvoker<T> invoker : executableWithVarArgs) {
        Class<?>[] otherTypes = invoker.getParameterTypes();
        int score = -1;
        int fixedParaLength = otherTypes.length - 1;
        if (parameterTypes.length >= fixedParaLength) {
          Class<?>[] argTypes = new Class<?>[fixedParaLength];
          System.arraycopy(parameterTypes, 0, argTypes, 0, fixedParaLength);
          invoker = invoker.copy();
          score = matchTypes(invoker, argTypes, otherTypes, false);
          if (score > -1) {
            Class<?> target = otherTypes[fixedParaLength].getComponentType();
            for (int i = fixedParaLength; i < parameterTypes.length; i++) {
              Class<?> type = parameterTypes[i];
              if (RuntimeContext.class.isAssignableFrom(type)) {
                score++;
              } else if (Null.class.equals(type)) {
                if (!target.isPrimitive()) {
                  score++;
                } else {
                  score = -1;
                  break;
                }
              } else if (!isPrimitiveAssignableFrom(type, target)) {
                score++;
                if (!target.isAssignableFrom(type)) {
                  score++;
                  if (!isCoercible(type, target)) {
                    boolean found = false;
                    for (ClassImplicitConvert convert : CONVERTS) {
                      if (convert.support(type, target)) {
                        invoker.addClassImplicitConvert(i, convert);
                        found = true;
                      }
                    }
                    invoker.setImplicit(found);
                    if (!found) {
                      score = -1;
                      break;
                    }
                    score++;
                  } else {
                    score++;
                  }
                }
              }
            }
          }
        }
        if (score > -1) {
          if (foundInvoker == null) {
            foundInvoker = invoker;
            foundScore = score;
          } else {
            if (score < foundScore) {
              foundScore = score;
              foundInvoker = invoker;
            }
          }
        }
      }
    }
    return foundInvoker;
  }

  /** Returns the method best matching the given signature, including type coercion, or null. */
  public static JavaInvoker<Method> findInvoker(
      Class<?> cls, String name, Class<?>[] parameterTypes) {
    List<JavaInvoker<Method>> methodList = new ArrayList<>();
    Method[] methods = cls.getMethods();
    for (int i = 0, n = methods.length; i < n; i++) {
      Method method = methods[i];
      if (!method.getName().equals(name)) {
        continue;
      }
      if (method.getAnnotation(Transient.class) != null) {
        continue;
      }
      if (Modifier.isPublic(method.getModifiers())) {
        methodList.add(new MethodInvoker(method));
      }
    }
    return findMethodInvoker(methodList, parameterTypes);
  }

  public static JavaInvoker<Method> findInvoker(Class<?> cls, String name) {
    return findInvoker(cls, name, new Class<?>[0]);
  }

  /** 是否可以自动装修拆箱 */
  public static boolean isPrimitiveAssignableFrom(Class<?> from, Class<?> to) {
    if ((from == Boolean.class || from == boolean.class)
        && (to == boolean.class || to == Boolean.class)) {
      return true;
    }
    if ((from == Integer.class || from == int.class) && (to == int.class || to == Integer.class)) {
      return true;
    }
    if ((from == Float.class || from == float.class) && (to == float.class || to == Float.class)) {
      return true;
    }
    if ((from == Double.class || from == double.class)
        && (to == double.class || to == Double.class)) {
      return true;
    }
    if ((from == Byte.class || from == byte.class) && (to == byte.class || to == Byte.class)) {
      return true;
    }
    if ((from == Short.class || from == short.class) && (to == short.class || to == Short.class)) {
      return true;
    }
    if ((from == Long.class || from == long.class) && (to == long.class || to == Long.class)) {
      return true;
    }
    return (from == Character.class || from == char.class)
        && (to == char.class || to == Character.class);
  }

  /** 获取String类型的参数描述 */
  public static String[] getStringTypes(Object[] objects) {
    String[] parameterTypes = new String[objects == null ? 0 : objects.length];
    if (objects != null) {
      for (int i = 0, len = objects.length; i < len; i++) {
        Object value = objects[i];
        parameterTypes[i] = value == null ? "null" : value.getClass().getSimpleName();
      }
    }
    return parameterTypes;
  }

  /** 是否可以自动隐式转换 https://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html */
  private static boolean isCoercible(Class<?> from, Class<?> to) {
    if (from == Integer.class || from == int.class) {
      return to == float.class
          || to == Float.class
          || to == double.class
          || to == Double.class
          || to == long.class
          || to == Long.class;
    }

    if (from == Float.class || from == float.class) {
      return to == double.class || to == Double.class;
    }

    if (from == Double.class || from == double.class) {
      return false;
    }

    if (from == Character.class || from == char.class) {
      return to == int.class
          || to == Integer.class
          || to == float.class
          || to == Float.class
          || to == double.class
          || to == Double.class
          || to == long.class
          || to == Long.class;
    }

    if (from == Byte.class || from == byte.class) {
      return to == int.class
          || to == Integer.class
          || to == float.class
          || to == Float.class
          || to == double.class
          || to == Double.class
          || to == long.class
          || to == Long.class
          || to == short.class
          || to == Short.class;
    }

    if (from == Short.class || from == short.class) {
      return to == int.class
          || to == Integer.class
          || to == float.class
          || to == Float.class
          || to == double.class
          || to == Double.class
          || to == long.class
          || to == Long.class;
    }

    if (from == Long.class || from == long.class) {
      return to == float.class || to == Float.class || to == double.class || to == Double.class;
    }

    if (from == int[].class || from == Integer[].class) {
      return to == Object[].class
          || to == float[].class
          || to == Float[].class
          || to == double[].class
          || to == Double[].class
          || to == long[].class
          || to == Long[].class;
    }

    return false;
  }

  /**
   * 获取内部类
   *
   * @param obj 目标对象，可以是实例，可以是Class
   * @param name 内部类名称
   */
  public static Object getInnerClass(Object obj, String name) {
    Class cls = obj instanceof Class ? (Class) obj : obj.getClass();
    Class[] classes = cls.getDeclaredClasses();
    for (int i = 0, len = classes.length; i < len; i++) {
      Class clazz = classes[i];
      if (name.equalsIgnoreCase(clazz.getSimpleName())) {
        return clazz;
      }
    }
    return null;
  }

  /**
   * 获取字段
   *
   * @param obj 目标对象可以是实例，可以是Class
   * @param name 字段名称
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static Field getField(Object obj, String name) {
    Class cls = obj instanceof Class ? (Class) obj : obj.getClass();
    Map<String, Field> fields = FIELD_CACHE.get(cls);
    if (fields == null) {
      fields = new ConcurrentHashMap<>();
      FIELD_CACHE.put(cls, fields);
    }

    Field field = fields.get(name);
    if (field == null) {
      try {
        field = cls.getDeclaredField(name);
        if (field.getAnnotation(Transient.class) != null) {
          field = null;
        } else {
          field.setAccessible(true);
          fields.put(name, field);
        }
      } catch (Throwable t) {
        // fall through, try super classes
      }

      if (field == null) {
        Class parentClass = cls.getSuperclass();
        while (parentClass != Object.class && parentClass != null) {
          try {
            field = parentClass.getDeclaredField(name);
            if (field.getAnnotation(Transient.class) != null) {
              field = null;
            } else {
              field.setAccessible(true);
              fields.put(name, field);
            }
          } catch (NoSuchFieldException e) {
            // fall through
          }
          parentClass = parentClass.getSuperclass();
        }
      }
    }

    return field;
  }

  /** 注册隐式转换器 */
  public static void registerImplicitConvert(ClassImplicitConvert classImplicitConvert) {
    CONVERTS.add(classImplicitConvert);
  }

  /**
   * 注册扩展方法
   *
   * @param target 目标类
   * @param extensionObject 实现类
   */
  public static void registerMethodExtension(Class<?> target, Object extensionObject) {
    List<Class<?>> classList = EXTENSION_MAP.get(target);
    if (classList == null) {
      classList = new ArrayList<>();
      EXTENSION_MAP.put(target, classList);
    }
    Class<?> clazz = extensionObject.getClass();
    classList.add(clazz);
    Method[] methods = clazz.getDeclaredMethods();
    if (methods != null) {
      Map<String, List<JavaInvoker<Method>>> cachedMethodMap = EXTENSION_METHOD_CACHE.get(target);
      if (cachedMethodMap == null) {
        cachedMethodMap = new HashMap<>();
        EXTENSION_METHOD_CACHE.put(target, cachedMethodMap);
      }
      for (Method method : methods) {
        if (Modifier.isPublic(method.getModifiers())
            && method.getParameterCount() > 0
            && method.getAnnotation(Transient.class) == null) {
          List<JavaInvoker<Method>> cachedList = cachedMethodMap.get(method.getName());
          if (cachedList == null) {
            cachedList = new ArrayList<>();
            cachedMethodMap.put(method.getName(), cachedList);
          }
          cachedList.add(new MethodInvoker(method, extensionObject));
        }
      }
      Collection<List<JavaInvoker<Method>>> methodsValues = cachedMethodMap.values();
      for (List<JavaInvoker<Method>> methodList : methodsValues) {
        methodList.sort(
            (m1, m2) -> {
              int sum1 =
                  Arrays.stream(m1.getParameterTypes())
                      .mapToInt(JavaReflection::calcToObjectDistance)
                      .sum();
              int sum2 =
                  Arrays.stream(m2.getParameterTypes())
                      .mapToInt(JavaReflection::calcToObjectDistance)
                      .sum();
              return sum2 - sum1;
            });
      }
    }
  }

  public static Object getFieldValue(Object obj, Field field) {
    try {
      return field.get(obj);
    } catch (Throwable e) {
      throw new RuntimeException(
          "Couldn't get value of field '"
              + field.getName()
              + "' from object of type '"
              + obj.getClass().getSimpleName()
              + "'");
    }
  }

  public static void setFieldValue(Object obj, Field field, Object value) {
    try {
      field.set(obj, value);
    } catch (Throwable e) {
      throw new RuntimeException(
          "Couldn't set value of field '"
              + field.getName()
              + "' from object of type '"
              + obj.getClass().getSimpleName()
              + "'");
    }
  }

  public static JavaInvoker<Method> getExtensionMethod(
      Object obj, String name, Object... arguments) {
    boolean isClass = obj instanceof Class;
    Class<?> cls = isClass ? Class.class : obj.getClass();
    if (cls.isArray()) {
      cls = Object[].class;
    }
    return getExtensionMethod(cls, name, arguments);
  }

  private static Class[] getParameterTypes(Class<?> cls, Object... arguments) {
    int begin = cls == null ? 0 : 1;
    Class<?>[] parameterTypes = new Class[arguments.length + begin];
    if (begin > 0) {
      parameterTypes[0] = cls;
    }
    for (int i = 0; i < arguments.length; i++) {
      parameterTypes[i + begin] = arguments[i] == null ? Null.class : arguments[i].getClass();
    }
    return parameterTypes;
  }

  private static JavaInvoker<Method> getExtensionMethod(
      Class<?> cls, String name, Object... arguments) {
    if (cls == null) {
      cls = Object.class;
    }
    Map<String, List<JavaInvoker<Method>>> methodMap = EXTENSION_METHOD_CACHE.get(cls);
    if (methodMap != null) {
      List<JavaInvoker<Method>> methodList = methodMap.get(name);
      if (methodList != null) {
        return findMethodInvoker(methodList, getParameterTypes(cls, arguments));
      }
    }
    if (cls != Object.class) {
      Class<?>[] interfaces = cls.getInterfaces();
      if (interfaces != null) {
        for (Class<?> clazz : interfaces) {
          JavaInvoker<Method> invoker = getExtensionMethod(clazz, name, arguments);
          if (invoker != null) {
            return invoker;
          }
        }
      }
      return getExtensionMethod(cls.getSuperclass(), name, arguments);
    }
    return null;
  }

  public static JavaInvoker<Method> getMethod(Object obj, String name, Object... arguments) {
    boolean isClass = obj instanceof Class;
    Class<?> cls =
        isClass ? (Class<?>) obj : (obj instanceof Function ? Function.class : obj.getClass());
    Map<MethodSignature, JavaInvoker<Method>> methods = METHOD_CACHE.get(cls);
    if (methods == null) {
      methods = new ConcurrentHashMap<>();
      METHOD_CACHE.put(cls, methods);
    }

    Class<?>[] parameterTypes = getParameterTypes(null, arguments);
    MethodSignature signature = new MethodSignature(name, parameterTypes);
    JavaInvoker<Method> invoker = methods.get(signature);

    if (invoker == null) {
      try {
        if (name == null) {
          invoker = findApply(cls);
        } else {
          invoker = findInvoker(cls, name, parameterTypes);
          //					if (invoker == null) {
          //						invoker = findInvoker(cls, name, new Class<?>[]{Object[].class});
          //					}
        }
      } catch (Throwable e) {
        // fall through
      }

      if (invoker == null) {
        Class<?> parentClass = cls.getSuperclass();
        while (parentClass != null) {
          try {
            if (name == null) {
              invoker = findApply(parentClass);
            } else {
              invoker = findInvoker(parentClass, name, parameterTypes);
            }
            if (invoker != null) {
              break;
            }
          } catch (Throwable e) {
            // fall through
          }
          parentClass = parentClass.getSuperclass();
        }
      }
    }
    if (invoker == null || invoker.isImplicit()) {
      JavaInvoker<Method> extensionInvoker = getExtensionMethod(obj, name, arguments);
      if (extensionInvoker != null) {
        extensionInvoker.setExtension(true);
        invoker = extensionInvoker;
      }
    }
    if (invoker != null) {
      methods.put(signature, invoker);
    }
    return invoker;
  }

  public static JavaInvoker<Method> getFunction(String name, Object... arguments) {
    List<JavaInvoker<Method>> methodList =
        FUNCTIONS.stream()
            .filter(it -> it.getExecutable().getName().equals(name))
            .collect(Collectors.toList());
    return findMethodInvoker(methodList, getParameterTypes(null, arguments));
  }

  /** NULL值 */
  public static final class Null {}

  /** 方法签名 */
  private static class MethodSignature {

    private final String name;

    @SuppressWarnings("rawtypes")
    private final Class[] parameters;

    private final int hashCode;

    @SuppressWarnings("rawtypes")
    MethodSignature(String name, Class[] parameters) {
      this.name = name;
      this.parameters = parameters;
      final int prime = 31;
      int hash = 1;
      hash = prime * hash + ((name == null) ? 0 : name.hashCode());
      hash = prime * hash + Arrays.hashCode(parameters);
      hashCode = hash;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MethodSignature other = (MethodSignature) obj;
      if (name == null) {
        if (other.name != null) {
          return false;
        }
      } else if (!name.equals(other.name)) {
        return false;
      }
      return Arrays.equals(parameters, other.parameters);
    }
  }
}
