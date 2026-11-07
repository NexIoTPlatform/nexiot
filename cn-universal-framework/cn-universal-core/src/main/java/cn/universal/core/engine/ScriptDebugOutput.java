package cn.universal.core.engine;

import java.util.function.Consumer;

/** 仅当前调试线程捕获输出；正常设备线程保持原有输出行为。 */
public final class ScriptDebugOutput {
  private static final ThreadLocal<Consumer<String>> OUTPUT = new ThreadLocal<>();

  private ScriptDebugOutput() {}

  public static void bind(Consumer<String> sink) {
    OUTPUT.set(sink);
  }

  public static void clear() {
    OUTPUT.remove();
  }

  public static boolean write(String message) {
    Consumer<String> sink = OUTPUT.get();
    if (sink == null) return false;
    sink.accept(message);
    return true;
  }
}
