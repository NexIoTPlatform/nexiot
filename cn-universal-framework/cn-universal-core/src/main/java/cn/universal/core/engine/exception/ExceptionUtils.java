package cn.universal.core.engine.exception;

public class ExceptionUtils {

  public static int indexOfThrowable(Throwable root, Class<? extends Throwable> clazz) {
    if (root == null) {
      return -1;
    }
    int index = 0;
    do {
      if (clazz.isAssignableFrom(root.getClass())) {
        return index;
      }
      index++;
    } while ((root = root.getCause()) != null);
    return -1;
  }
}
