package cn.universal.core.engine.compile;

import cn.universal.core.engine.asm.Type;

public class Descriptor {

  public static String make_descriptor(Class<?> target, String methodName, Class<?>... args) {
    try {
      return Type.getMethodDescriptor(target.getMethod(methodName, args));
    } catch (NoSuchMethodException e) {
      throw new MagicScriptCompileException(e);
    }
  }

  public static String make_descriptor(Class<?> type, Class<?>... args) {
    int len = args.length;
    Type[] types = new Type[len];
    for (int i = 0; i < len; i++) {
      types[i] = Type.getType(args[i]);
    }
    return Type.getMethodDescriptor(Type.getType(type), types);
  }
}
