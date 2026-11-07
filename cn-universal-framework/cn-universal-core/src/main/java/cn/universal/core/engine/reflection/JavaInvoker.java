package cn.universal.core.engine.reflection;

import cn.universal.core.engine.convert.ClassImplicitConvert;
import cn.universal.core.engine.runtime.RuntimeContext;
import cn.universal.core.engine.runtime.Variables;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class JavaInvoker<T extends Executable> {

  private final Map<Integer, ClassImplicitConvert> converts = new HashMap<>();
  private final T executable;
  private final Class<?>[] parameterTypes;
  private final Class<?>[] fixedParameterTypes;
  private boolean implicit = false;
  private boolean extension = false;
  private Object defaultTarget;
  private final boolean hasRuntimeContext;

  JavaInvoker(T executable) {
    this.executable = executable;
    this.executable.setAccessible(true);
    this.parameterTypes = this.executable.getParameterTypes();
    this.hasRuntimeContext =
        this.parameterTypes != null
            && Stream.of(this.parameterTypes).anyMatch(RuntimeContext.class::isAssignableFrom);
    if (this.hasRuntimeContext) {
      this.fixedParameterTypes =
          Stream.of(this.parameterTypes)
              .filter(it -> !RuntimeContext.class.isAssignableFrom(it))
              .toArray(Class[]::new);
    } else {
      this.fixedParameterTypes = this.parameterTypes;
    }
  }

  JavaInvoker(JavaInvoker<T> invoker) {
    this.executable = invoker.executable;
    this.parameterTypes = invoker.parameterTypes;
    this.fixedParameterTypes = invoker.fixedParameterTypes;
    this.hasRuntimeContext = invoker.hasRuntimeContext;
    this.implicit = invoker.implicit;
    this.extension = invoker.extension;
    this.defaultTarget = invoker.defaultTarget;
  }

  public JavaInvoker<T> copy() {
    throw new UnsupportedOperationException();
  }

  public boolean isImplicit() {
    return implicit;
  }

  public void setImplicit(boolean implicit) {
    this.implicit = implicit;
  }

  public boolean isExtension() {
    return extension;
  }

  public void setExtension(boolean extension) {
    this.extension = extension;
  }

  public Object getDefaultTarget() {
    return defaultTarget;
  }

  public void setDefaultTarget(Object defaultTarget) {
    this.defaultTarget = defaultTarget;
  }

  public T getExecutable() {
    return this.executable;
  }

  public Class<?>[] getParameterTypes() {
    return fixedParameterTypes;
  }

  public boolean isVarArgs() {
    return this.executable.isVarArgs();
  }

  public boolean hasRuntimeContext() {
    return hasRuntimeContext;
  }

  public Object invoke0(Object target, RuntimeContext runtimeContext, Object[] arguments)
      throws Throwable {
    try {
      if (hasRuntimeContext) {
        arguments = insertArgument(arguments, runtimeContext);
      }
      if (extension) {
        arguments = insertArgument(arguments, target);
        if (target.getClass().isArray()) {
          Object[] objs = new Object[Array.getLength(target)];
          for (int i = 0, len = objs.length; i < len; i++) {
            Array.set(objs, i, Array.get(target, i));
          }
          arguments[0] = objs;
        }
      } else if (isVarArgs()
          && parameterTypes.length == 1
          && arguments.length == 1
          && (arguments[0] == null || arguments[0].getClass().isArray())) {
        return invoke(target, arguments);
      }
      return invoke(
          target,
          processArguments(
              runtimeContext == null ? null : runtimeContext.getVariables(), arguments));
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    } catch (IllegalAccessException e1) {
      throw e1;
    }
  }

  Object invoke(Object target, Object... arguments)
      throws InvocationTargetException, IllegalAccessException, InstantiationException {
    throw new UnsupportedOperationException();
  }

  protected Object[] insertArgument(Object[] arguments, Object value) {
    int argumentLength = arguments == null ? 0 : arguments.length;
    Object[] dest = new Object[argumentLength + 1];
    System.arraycopy(arguments, 0, dest, 1, argumentLength);
    dest[0] = value;
    return dest;
  }

  /**
   * 给参数设置隐式转换方法
   *
   * @param index 索引
   * @param classImplicitConvert 转换方法
   */
  protected void addClassImplicitConvert(int index, ClassImplicitConvert classImplicitConvert) {
    converts.put(index, classImplicitConvert);
  }

  /** 预处理参数，用来实现隐式转换 */
  protected Object[] processArguments(Variables variables, Object[] arguments) {
    int count = this.executable.getParameterCount();
    int maxIndex = Integer.MAX_VALUE;
    Class<?> componentType = null;
    if (isVarArgs()) {
      componentType = this.executable.getParameterTypes()[count - 1].getComponentType();
      maxIndex = count - 1;
    }
    if (arguments != null) {
      for (Map.Entry<Integer, ClassImplicitConvert> entry : converts.entrySet()) {
        int index = entry.getKey();
        arguments[index] =
            entry
                .getValue()
                .convert(
                    variables,
                    arguments[index],
                    index < maxIndex ? parameterTypes[index] : componentType);
      }
    }
    if (isVarArgs()) {
      Object[] args = new Object[count];
      if (arguments != null) {
        if (count - 1 >= 0) {
          System.arraycopy(arguments, 0, args, 0, count - 1);
        }
        int len = arguments.length - count + 1;
        Object varArgs = null;
        if (len == 1) {
          Object target = arguments[arguments.length - 1];
          if (target != null
              && target.getClass() == this.executable.getParameterTypes()[count - 1]) {
            varArgs = target;
          }
        }
        if (varArgs == null) {
          varArgs = Array.newInstance(componentType, len);
          if (len > 0) {
            for (int i = 0; i < len; i++) {
              Array.set(varArgs, i, arguments[count - 1 + i]);
            }
          }
        }
        args[count - 1] = varArgs;
      }
      arguments = args;
    }
    return arguments;
  }
}
