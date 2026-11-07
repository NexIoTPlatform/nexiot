package cn.universal.core.engine.asm;

/**
 * Exception thrown when the constant pool of a class produced by a {@link ClassWriter} is too
 * large. @Author Jason Zaugg
 */
public final class ClassTooLargeException extends IndexOutOfBoundsException {

  private static final long serialVersionUID = 160715609518896765L;

  private final String className;
  private final int constantPoolCount;

  /**
   * Constructs a new {@link ClassTooLargeException}.
   *
   * @param className the internal name of the class.
   * @param constantPoolCount the number of constant pool items of the class.
   */
  public ClassTooLargeException(final String className, final int constantPoolCount) {
    super("Class too large: " + className);
    this.className = className;
    this.constantPoolCount = constantPoolCount;
  }

  /**
   * Returns the internal name of the class.
   *
   * @return the internal name of the class.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Returns the number of constant pool items of the class.
   *
   * @return the number of constant pool items of the class.
   */
  public int getConstantPoolCount() {
    return constantPoolCount;
  }
}
