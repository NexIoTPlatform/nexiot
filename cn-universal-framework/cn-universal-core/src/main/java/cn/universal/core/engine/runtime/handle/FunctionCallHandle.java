package cn.universal.core.engine.runtime.handle;

import static java.lang.invoke.MethodType.methodType;

import cn.universal.core.engine.exception.MagicScriptRuntimeException;
import cn.universal.core.engine.functions.ClassExtension;
import cn.universal.core.engine.functions.DynamicAttribute;
import cn.universal.core.engine.functions.DynamicMethod;
import cn.universal.core.engine.functions.StreamExtension;
import cn.universal.core.engine.parsing.ast.statement.AsyncCall;
import cn.universal.core.engine.parsing.ast.statement.ClassConverter;
import cn.universal.core.engine.reflection.JavaInvoker;
import cn.universal.core.engine.reflection.JavaReflection;
import cn.universal.core.engine.reflection.MethodInvoker;
import cn.universal.core.engine.runtime.RuntimeContext;
import cn.universal.core.engine.runtime.SpreadValue;
import cn.universal.core.engine.runtime.Variables;
import cn.universal.core.engine.runtime.function.MagicScriptLambdaFunction;
import cn.universal.core.engine.runtime.lang.ArrayKeyValueIterator;
import cn.universal.core.engine.runtime.lang.ArrayValueIterator;
import cn.universal.core.engine.runtime.lang.KeyValueIterator;
import cn.universal.core.engine.runtime.lang.MapKeyValueIterator;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FunctionCallHandle {

  private static final MethodHandle FALLBACK;

  private static final MethodHandle INVOKE_METHOD;

  private static final MethodHandle INVOKE_FUNCTION;

  private static final MethodHandle MEMBER_ACCESS;

  private static final MethodHandle INVOKE_NEW_INSTANCE;

  private static final MethodHandle SET_VARIABLE_VALUE;

  private static final Object[] EMPTY_ARGS = new Object[0];

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      FALLBACK =
          lookup.findStatic(
              FunctionCallHandle.class,
              "fallback",
              methodType(Object.class, MethodCallSite.class, Object[].class));
      INVOKE_METHOD =
          lookup.findStatic(
              FunctionCallHandle.class,
              "invoke_method",
              methodType(
                  Object.class,
                  RuntimeContext.class,
                  String.class,
                  boolean.class,
                  boolean.class,
                  Object[].class,
                  Object.class));
      INVOKE_FUNCTION =
          lookup.findStatic(
              FunctionCallHandle.class,
              "invoke_function",
              methodType(
                  Object.class,
                  RuntimeContext.class,
                  String.class,
                  boolean.class,
                  Object[].class,
                  Object.class));
      MEMBER_ACCESS =
          lookup.findStatic(
              FunctionCallHandle.class,
              "member_access",
              methodType(
                  Object.class,
                  RuntimeContext.class,
                  Object.class,
                  String.class,
                  boolean.class,
                  boolean.class));
      INVOKE_NEW_INSTANCE =
          lookup.findStatic(
              FunctionCallHandle.class,
              "invoke_new_instance",
              methodType(Object.class, RuntimeContext.class, Object.class, Object[].class));
      SET_VARIABLE_VALUE =
          lookup.findStatic(
              FunctionCallHandle.class,
              "set_variable_value",
              methodType(
                  Object.class, RuntimeContext.class, Object.class, Object.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new Error("FunctionCallHandle初始化失败", e);
    }
  }

  public static CallSite bootstrap(
      MethodHandles.Lookup caller, String name, MethodType type, int flag) {
    MethodCallSite callSite = new MethodCallSite(caller, name, type, FunctionCallHandle.class);
    MethodHandle fallback = null;
    switch (name) {
      case "invoke_method":
        fallback = INVOKE_METHOD;
        break;
      case "invoke_function":
        fallback = INVOKE_FUNCTION;
        break;
      case "member_access":
        fallback = MEMBER_ACCESS;
        break;
      case "invoke_new_instance":
        fallback = INVOKE_NEW_INSTANCE;
        break;
      case "set_variable_value":
        fallback = SET_VARIABLE_VALUE;
        break;
    }
    if (fallback != null) {
      fallback = fallback.asType(type);
      callSite.setTarget(fallback);
      callSite.fallback = fallback;
    } else {
      fallback =
          FALLBACK.bindTo(callSite).asCollector(Object[].class, type.parameterCount()).asType(type);
      callSite.setTarget(fallback);
      callSite.fallback = fallback;
    }
    return callSite;
  }

  public static Object fallback(MethodCallSite callSite, Object[] args) throws Throwable {
    JavaInvoker<Method> method =
        JavaReflection.getMethod(FunctionCallHandle.class, callSite.methodName, args);
    if (method != null) {
      return method.invoke0(null, null, args);
    }
    return null;
  }

  public static Object invoke_function(
      RuntimeContext runtimeContext, String name, boolean hasSpread, Object[] args, Object target)
      throws Throwable {
    JavaInvoker<Method> method = null;
    if (hasSpread) {
      args = do_spread(args);
    }
    if (args != null) {
      for (int i = 0, len = args.length; i < len; i++) {
        if (args[i] instanceof MagicScriptLambdaFunction) {
          MagicScriptLambdaFunction function = (MagicScriptLambdaFunction) args[i];
          args[i] =
              (Function<Object[], Object>)
                  objects -> function.apply(runtimeContext.getVariables(), objects);
        }
      }
    }
    if (target == null) {
      method = JavaReflection.getFunction(name, args);
    } else if (target instanceof MagicScriptLambdaFunction) {
      return ((MagicScriptLambdaFunction) target).apply(runtimeContext.getVariables(), args);
    } else if (target instanceof Function) {
      return ((Function) target).apply(args);
    }
    if (method != null) {
      return method.invoke0(target, runtimeContext, args);
    }
    throw new NoSuchMethodException(
        String.format("找不到函数%s(%s)", name, String.join(",", JavaReflection.getStringTypes(args))));
  }

  public static Object invoke_method(
      RuntimeContext runtimeContext,
      String name,
      boolean hasSpread,
      boolean optional,
      Object[] args,
      Object target)
      throws Throwable {
    if (target == null && optional) {
      return null;
    }
    JavaInvoker<Method> method;
    if (hasSpread) {
      args = do_spread(args);
    }
    if (target == null) {
      throw new NullPointerException("对象为空");
    } else if (target instanceof MagicScriptLambdaFunction) {
      return ((MagicScriptLambdaFunction) target).apply(runtimeContext.getVariables(), args);
    } else if (target instanceof Function) {
      return ((Function) target).apply(args);
    } else {
      method = JavaReflection.getMethod(target, name, args);
    }
    if (method != null) {
      return method.invoke0(target, runtimeContext, args);
    }
    if (target instanceof DynamicMethod) {
      MethodInvoker invoker =
          new MethodInvoker(
              DynamicMethod.class.getDeclaredMethod("execute", String.class, List.class));
      Object[] newArgumentValues = new Object[] {name, Arrays.asList(args)};
      return invoker.invoke0(target, runtimeContext, newArgumentValues);
    }
    try {
      Object function = member_access(runtimeContext, target, name, optional, false);
      if (function != null) {
        return invoke_function(runtimeContext, name, false, args, function);
      }
    } catch (Exception ignored) {
    }
    throw new NoSuchMethodException(
        String.format(
            "在%s中找不到方法%s(%s)",
            target.getClass().getName(),
            name,
            String.join(",", JavaReflection.getStringTypes(args))));
  }

  public static Object spread(List<Object> source, List<Object> target) {
    source.addAll(target);
    return source;
  }

  public static Object spread(Map<Object, Object> source, Map<Object, Object> target) {
    source.putAll(target);
    return source;
  }

  public static Object member_access(
      RuntimeContext runtimeContext, Object target, String name, boolean optional, boolean inLinq) {
    if (target == null) {
      if (optional) {
        return null;
      }
      throw new NullPointerException("target is null");
    }
    if ("class".equals(name)) {
      return target instanceof Class ? target : target.getClass();
    } else if (target instanceof Map) {
      return ((Map) target).get(name);
    } else if (target instanceof DynamicAttribute) {
      return ((DynamicAttribute<?, ?>) target).getDynamicAttribute(name);
    }
    String methodName;
    Field field = JavaReflection.getField(target, name);
    if (field != null) {
      return JavaReflection.getFieldValue(target, field);
    } else {
      Object innerClass = JavaReflection.getInnerClass(target, name);
      if (innerClass != null) {
        return innerClass;
      }
      if (name.length() > 1) {
        methodName = name.substring(0, 1).toUpperCase() + name.substring(1);
      } else {
        methodName = name.toUpperCase();
      }
      JavaInvoker<Method> invoker =
          JavaReflection.getMethod(target, "get" + methodName, EMPTY_ARGS);
      try {
        if (invoker != null) {
          return invoker.invoke0(target, runtimeContext, EMPTY_ARGS);
        } else if ((invoker = JavaReflection.getMethod(target, "is" + methodName, EMPTY_ARGS))
            != null) {
          return invoker.invoke0(target, runtimeContext, EMPTY_ARGS);
        }
      } catch (Throwable throwable) {
        throw new MagicScriptRuntimeException(throwable);
      }
    }
    if (target instanceof List) {
      List<?> list = (List<?>) target;
      if (inLinq) {
        return list.stream()
            .map(it -> member_access(runtimeContext, it, name, optional, inLinq))
            .collect(Collectors.toList());
      } else if (list.size() > 0) {
        return member_access(runtimeContext, list.get(0), name, optional, false);
      }
      return null;
    }
    if (optional) {
      return null;
    }
    throw new MagicScriptRuntimeException(
        String.format(
            "在%s中找不到属性%s或者方法get%s、方法is%s,内部类%s", target, name, methodName, methodName, name));
  }

  public static Object call_async(
      MagicScriptLambdaFunction function, Variables variables, Object... args) {
    return AsyncCall.execute(function, variables, args);
  }

  public static Object invoke_new_instance(
      RuntimeContext runtimeContext, Object target, Object[] args) throws Throwable {
    return ClassExtension.newInstance(target, runtimeContext, args);
  }

  public static Object newArrayList(boolean hasSpread, Object[] args) {
    if (!hasSpread) {
      return new ArrayList<>(Arrays.asList(args));
    }
    List<Object> list = new ArrayList<>(args.length);
    for (Object item : args) {
      if (item instanceof SpreadValue) {
        Object res = ((SpreadValue) item).getValue();
        if (res == null) {
          // 其实是因为该变量未定义
        } else if (res instanceof Collection) {
          list.addAll(((Collection) res));
        } else if (res instanceof Map) {
          throw new MagicScriptRuntimeException("不能在list中展开map");
        } else {
          throw new MagicScriptRuntimeException("不能展开的类型:" + res.getClass());
        }
      } else {
        list.add(item);
      }
    }
    return list;
  }

  public static Iterator<?> newValueIterator(Object target) {
    if (target instanceof Iterable) {
      return ((Iterable<?>) target).iterator();
    } else if (target instanceof Iterator) {
      return (Iterator<?>) target;
    } else if (target instanceof Map) {
      return ((Map) target).values().iterator();
    } else if (target.getClass().isArray()) {
      return new ArrayValueIterator(target);
    } else {
      throw new MagicScriptRuntimeException("不支持循环" + target.getClass());
    }
  }

  public static Iterator<?> newKeyValueIterator(Object target) {
    if (target instanceof Iterable) {
      return new KeyValueIterator(((Iterable) target).iterator());
    } else if (target instanceof Iterator) {
      return new KeyValueIterator((Iterator) target);
    } else if (target instanceof Map) {
      return new MapKeyValueIterator((Map<Object, Object>) target);
    } else if (target.getClass().isArray()) {
      return new ArrayKeyValueIterator(target);
    } else {
      throw new MagicScriptRuntimeException("不支持循环" + target.getClass());
    }
  }

  public static Object newLinkedHashMap(Boolean hasSpread, Object[] args) {
    Map<Object, Object> map = new LinkedHashMap<>();
    if (args != null) {
      for (int i = 0, len = args.length; i < len; ) {
        Object key = args[i++];
        if (hasSpread && key instanceof SpreadValue) {
          Object res = ((SpreadValue) key).getValue();
          if (res == null) {
            // 其实是因为该变量未定义
          } else if (res instanceof Map) {
            // 可能导致 map put 入非String 的 key
            map.putAll((Map<String, ?>) res);
          } else if (res instanceof Collection) {
            int index = 0;
            for (Object obj : ((Collection) res)) {
              map.put(String.valueOf(index++), obj);
            }
          } else {
            throw new MagicScriptRuntimeException("不能展开的类型:" + res.getClass());
          }
          continue;
        }
        map.put(key, args[i++]);
      }
    }
    return map;
  }

  public static Object set_variable_value(
      RuntimeContext runtimeContext, Object target, Object name, Object value) throws Throwable {
    if (target == null) {
      throw new NullPointerException("target is null");
    }
    if (name == null) {
      throw new NullPointerException("key is null");
    }
    if (target instanceof Map) {
      ((Map) target).put(name, value);
    } else if (target instanceof DynamicAttribute) {
      ((DynamicAttribute) target).setDynamicAttribute(Objects.toString(name, null), value);
    } else if (target instanceof Collection) {
      if (name instanceof Number) {
        // TODO NEW EXCEPTION
        ((List) target).set(((Number) name).intValue(), value);
      } else {
        throw new MagicScriptRuntimeException("不支持此赋值操作");
      }
    } else if (target.getClass().isArray()) {
      if (name instanceof Number) {
        // TODO NEW EXCEPTION
        Array.set(target, ((Number) name).intValue(), value);
      } else {
        throw new MagicScriptRuntimeException("不支持此赋值操作");
      }
    } else {
      String text = name.toString();
      Field field = JavaReflection.getField(target, text);
      if (field != null) {
        JavaReflection.setFieldValue(target, field, value);
      } else {
        String methodName;
        if (text.length() > 1) {
          methodName = text.substring(0, 1).toUpperCase() + text.substring(1);
        } else {
          methodName = text.toUpperCase();
        }
        JavaInvoker<Method> invoker = JavaReflection.getMethod(target, "set" + methodName, value);
        if (invoker == null) {
          throw new MagicScriptRuntimeException(
              String.format("在%s中找不到属性%s或者方法set%s", target.getClass(), name, methodName));
        }
        invoker.invoke0(target, runtimeContext, new Object[] {value});
      }
    }
    return value;
  }

  public static Object type_cast(Object object, String target, Object... args) {
    return ClassConverter.process(object, target, args);
  }

  private static Object[] do_spread(Object[] args) {
    Object[] dest = new Object[args.length];
    for (int i = 0, n = args.length, pIndex = 0; i < n; i++) {
      Object item = args[i];
      if (item instanceof SpreadValue) {
        Object value = ((SpreadValue) item).getValue();
        Object[] spreadValues = StreamExtension.arrayLikeToList(value).toArray();
        int spreadLength = spreadValues.length;
        if (spreadLength > 0) {
          Object[] valTemp = dest;
          dest = new Object[dest.length + spreadLength - 1];
          System.arraycopy(valTemp, 0, dest, 0, valTemp.length);
          System.arraycopy(spreadValues, 0, dest, pIndex, spreadLength);
          pIndex += spreadLength;
        }
      } else {
        dest[pIndex++] = item;
      }
    }
    return dest;
  }
}
