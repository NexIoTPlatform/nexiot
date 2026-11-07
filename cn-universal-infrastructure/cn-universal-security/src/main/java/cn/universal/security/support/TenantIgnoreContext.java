package cn.universal.security.support;

/** 当前线程的租户忽略上下文。 */
public final class TenantIgnoreContext {

  private static final ThreadLocal<Integer> IGNORE_DEPTH =
      ThreadLocal.withInitial(() -> 0);

  private TenantIgnoreContext() {}

  public static void enter() {
    IGNORE_DEPTH.set(IGNORE_DEPTH.get() + 1);
  }

  public static void exit() {
    int depth = IGNORE_DEPTH.get() - 1;
    if (depth <= 0) {
      IGNORE_DEPTH.remove();
      return;
    }
    IGNORE_DEPTH.set(depth);
  }

  public static boolean isIgnore() {
    return IGNORE_DEPTH.get() > 0;
  }
}
