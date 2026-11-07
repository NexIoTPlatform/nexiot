package cn.universal.core.engine.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodInvoker extends JavaInvoker<Method> {

  public MethodInvoker(Method method) {
    super(method);
  }

  public MethodInvoker(MethodInvoker invoker) {
    super(invoker);
  }

  public MethodInvoker(Method method, Object defaultTarget) {
    super(method);
    setDefaultTarget(defaultTarget);
  }

  @Override
  public MethodInvoker copy() {
    return new MethodInvoker(this);
  }

  @Override
  Object invoke(Object target, Object... args)
      throws InvocationTargetException, IllegalAccessException {
    Object defaultTarget = getDefaultTarget();
    return getExecutable().invoke(defaultTarget == null ? target : defaultTarget, args);
  }
}
