package cn.universal.core.engine.asm;

/**
 * A visitor to visit a record component. The methods of this class must be called in the following
 * order: ( {@code visitAnnotation} | {@code visitTypeAnnotation} | {@code visitAttribute} )* {@code
 * visitEnd}. @Author Remi Forax @Author Eric Bruneton
 */
public abstract class RecordComponentVisitor {

  /**
   * The ASM API version implemented by this visitor. The value of this field must be one of {@link
   * Opcodes#ASM8} or {@link Opcodes#ASM9}.
   */
  protected final int api;

  /**
   * The record visitor to which this visitor must delegate method calls. May be {@literal null}.
   */
  /*package-private*/ RecordComponentVisitor delegate;

  /**
   * Constructs a new {@link RecordComponentVisitor}.
   *
   * @param api the ASM API version implemented by this visitor. Must be one of {@link Opcodes#ASM8}
   *     or {@link Opcodes#ASM9}.
   */
  public RecordComponentVisitor(final int api) {
    this(api, null);
  }

  /**
   * Constructs a new {@link RecordComponentVisitor}.
   *
   * @param api the ASM API version implemented by this visitor. Must be {@link Opcodes#ASM8}.
   * @param recordComponentVisitor the record component visitor to which this visitor must delegate
   *     method calls. May be null.
   */
  public RecordComponentVisitor(
      final int api, final RecordComponentVisitor recordComponentVisitor) {
    if (api != Opcodes.ASM9
        && api != Opcodes.ASM8
        && api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM10_EXPERIMENTAL) {
      throw new IllegalArgumentException("Unsupported api " + api);
    }
    if (api == Opcodes.ASM10_EXPERIMENTAL) {
      Constants.checkAsmExperimental(this);
    }
    this.api = api;
    this.delegate = recordComponentVisitor;
  }

  /**
   * The record visitor to which this visitor must delegate method calls. May be {@literal null}.
   *
   * @return the record visitor to which this visitor must delegate method calls or {@literal null}.
   */
  public RecordComponentVisitor getDelegate() {
    return delegate;
  }

  /**
   * Visits an annotation of the record component.
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   *     interested in visiting this annotation.
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (delegate != null) {
      return delegate.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * Visits an annotation on a type in the record component signature.
   *
   * @param typeRef a reference to the annotated type. The sort of this type reference must be
   *     {@link TypeReference#CLASS_TYPE_PARAMETER}, {@link
   *     TypeReference#CLASS_TYPE_PARAMETER_BOUND} or {@link TypeReference#CLASS_EXTENDS}. See
   *     {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   *     'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   *     interested in visiting this annotation.
   */
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (delegate != null) {
      return delegate.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * Visits a non standard attribute of the record component.
   *
   * @param attribute an attribute.
   */
  public void visitAttribute(final Attribute attribute) {
    if (delegate != null) {
      delegate.visitAttribute(attribute);
    }
  }

  /**
   * Visits the end of the record component. This method, which is the last one to be called, is
   * used to inform the visitor that everything have been visited.
   */
  public void visitEnd() {
    if (delegate != null) {
      delegate.visitEnd();
    }
  }
}
