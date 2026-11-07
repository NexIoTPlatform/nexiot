package cn.universal.core.engine.runtime;

public class MagicScriptClassLoader extends ClassLoader {

  public MagicScriptClassLoader(ClassLoader parent) {
    super(parent);
  }

  public synchronized Class<MagicScriptRuntime> load(String className, byte[] bytecode)
      throws ClassNotFoundException {
    defineClass(className, bytecode, 0, bytecode.length);
    return (Class<MagicScriptRuntime>) loadClass(className);
  }
}
