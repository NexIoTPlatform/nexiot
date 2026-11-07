package cn.universal.core.engine.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class ConstructorInvoker extends JavaInvoker<Constructor> {

  ConstructorInvoker(Constructor constructor) {
    super(constructor);
  }

  ConstructorInvoker(JavaInvoker<Constructor> invoker) {
    super(invoker);
  }

  @Override
  public ConstructorInvoker copy() {
    return new ConstructorInvoker(this);
  }

  @Override
  Object invoke(Object target, Object... args)
      throws InvocationTargetException, IllegalAccessException, InstantiationException {
    return getExecutable().newInstance(args);
  }
}
