package cn.universal.core.engine.asm.signature;

import cn.universal.core.engine.asm.Opcodes;

/**
 * A SignatureVisitor that generates signature literals, as defined in the Java Virtual Machine
 * Specification (JVMS). @Author Thomas Hallgren @Author Eric Bruneton
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1">JVMS
 *     4.7.9.1</a>
 */
public class SignatureWriter extends SignatureVisitor {

  /** The builder used to construct the visited signature. */
  private final StringBuilder stringBuilder = new StringBuilder();

  /** Whether the visited signature contains formal type parameters. */
  private boolean hasFormals;

  /** Whether the visited signature contains method parameter types. */
  private boolean hasParameters;

  /**
   * The stack used to keep track of class types that have arguments. Each element of this stack is
   * a boolean encoded in one bit. The top of the stack is the least significant bit. Pushing false
   * = *2, pushing true = *2+1, popping = /2.
   *
   * <p>Class type arguments must be surrounded with '&lt;' and '&gt;' and, because
   *
   * <ol>
   *   <li>class types can be nested (because type arguments can themselves be class types),
   *   <li>SignatureWriter always returns 'this' in each visit* method (to avoid allocating new
   *       SignatureWriter instances),
   * </ol>
   *
   * <p>we need a stack to properly balance these 'parentheses'. A new element is pushed on this
   * stack for each new visited type, and popped when the visit of this type ends (either is
   * visitEnd, or because visitInnerClassType is called).
   */
  private int argumentStack;

  /** Constructs a new {@link SignatureWriter}. */
  public SignatureWriter() {
    super(/* latest api =*/ Opcodes.ASM9);
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the SignatureVisitor processer
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visitFormalTypeParameter(final String name) {
    if (!hasFormals) {
      hasFormals = true;
      stringBuilder.append('<');
    }
    stringBuilder.append(name);
    stringBuilder.append(':');
  }

  @Override
  public SignatureVisitor visitClassBound() {
    return this;
  }

  @Override
  public SignatureVisitor visitInterfaceBound() {
    stringBuilder.append(':');
    return this;
  }

  @Override
  public SignatureVisitor visitSuperclass() {
    endFormals();
    return this;
  }

  @Override
  public SignatureVisitor visitInterface() {
    return this;
  }

  @Override
  public SignatureVisitor visitParameterType() {
    endFormals();
    if (!hasParameters) {
      hasParameters = true;
      stringBuilder.append('(');
    }
    return this;
  }

  @Override
  public SignatureVisitor visitReturnType() {
    endFormals();
    if (!hasParameters) {
      stringBuilder.append('(');
    }
    stringBuilder.append(')');
    return this;
  }

  @Override
  public SignatureVisitor visitExceptionType() {
    stringBuilder.append('^');
    return this;
  }

  @Override
  public void visitBaseType(final char descriptor) {
    stringBuilder.append(descriptor);
  }

  @Override
  public void visitTypeVariable(final String name) {
    stringBuilder.append('T');
    stringBuilder.append(name);
    stringBuilder.append(';');
  }

  @Override
  public SignatureVisitor visitArrayType() {
    stringBuilder.append('[');
    return this;
  }

  @Override
  public void visitClassType(final String name) {
    stringBuilder.append('L');
    stringBuilder.append(name);
    // Pushes 'false' on the stack, meaning that this type does not have type arguments (as far as
    // we can tell at this point).
    argumentStack *= 2;
  }

  @Override
  public void visitInnerClassType(final String name) {
    endArguments();
    stringBuilder.append('.');
    stringBuilder.append(name);
    // Pushes 'false' on the stack, meaning that this type does not have type arguments (as far as
    // we can tell at this point).
    argumentStack *= 2;
  }

  @Override
  public void visitTypeArgument() {
    // If the top of the stack is 'false', this means we are visiting the first type argument of the
    // currently visited type. We therefore need to append a '<', and to replace the top stack
    // element with 'true' (meaning that the current type does have type arguments).
    if (argumentStack % 2 == 0) {
      argumentStack |= 1;
      stringBuilder.append('<');
    }
    stringBuilder.append('*');
  }

  @Override
  public SignatureVisitor visitTypeArgument(final char wildcard) {
    // If the top of the stack is 'false', this means we are visiting the first type argument of the
    // currently visited type. We therefore need to append a '<', and to replace the top stack
    // element with 'true' (meaning that the current type does have type arguments).
    if (argumentStack % 2 == 0) {
      argumentStack |= 1;
      stringBuilder.append('<');
    }
    if (wildcard != '=') {
      stringBuilder.append(wildcard);
    }
    return this;
  }

  @Override
  public void visitEnd() {
    endArguments();
    stringBuilder.append(';');
  }

  /**
   * Returns the signature that was built by this signature writer.
   *
   * @return the signature that was built by this signature writer.
   */
  @Override
  public String toString() {
    return stringBuilder.toString();
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /** Ends the formal type parameters section of the signature. */
  private void endFormals() {
    if (hasFormals) {
      hasFormals = false;
      stringBuilder.append('>');
    }
  }

  /** Ends the type arguments of a class or inner class type. */
  private void endArguments() {
    // If the top of the stack is 'true', this means that some type arguments have been visited for
    // the type whose visit is now ending. We therefore need to append a '>', and to pop one element
    // from the stack.
    if (argumentStack % 2 == 1) {
      stringBuilder.append('>');
    }
    argumentStack /= 2;
  }
}
