package cn.universal.core.engine.asm;

/** Information about a class being parsed in a {@link ClassReader}. @Author Eric Bruneton */
final class Context {

  /** The prototypes of the attributes that must be parsed in this class. */
  Attribute[] attributePrototypes;

  /**
   * The options used to parse this class. One or more of {@link ClassReader#SKIP_CODE}, {@link
   * ClassReader#SKIP_DEBUG}, {@link ClassReader#SKIP_FRAMES}, {@link ClassReader#EXPAND_FRAMES} or
   * {@link ClassReader#EXPAND_ASM_INSNS}.
   */
  int parsingOptions;

  /** The buffer used to read strings in the constant pool. */
  char[] charBuffer;

  // Information about the current method, i.e. the one read in the current (or latest) call
  // to {@link ClassReader#readMethod()}.

  /** The access flags of the current method. */
  int currentMethodAccessFlags;

  /** The name of the current method. */
  String currentMethodName;

  /** The descriptor of the current method. */
  String currentMethodDescriptor;

  /**
   * The labels of the current method, indexed by bytecode offset (only bytecode offsets for which a
   * label is needed have a non null associated Label).
   */
  Label[] currentMethodLabels;

  // Information about the current type annotation target, i.e. the one read in the current
  // (or latest) call to {@link ClassReader#readAnnotationTarget()}.

  /**
   * The target_type and target_info of the current type annotation target, encoded as described in
   * {@link TypeReference}.
   */
  int currentTypeAnnotationTarget;

  /** The target_path of the current type annotation target. */
  TypePath currentTypeAnnotationTargetPath;

  /** The start of each local variable range in the current local variable annotation. */
  Label[] currentLocalVariableAnnotationRangeStarts;

  /** The end of each local variable range in the current local variable annotation. */
  Label[] currentLocalVariableAnnotationRangeEnds;

  /**
   * The local variable index of each local variable range in the current local variable annotation.
   */
  int[] currentLocalVariableAnnotationRangeIndices;

  // Information about the current stack map frame, i.e. the one read in the current (or latest)
  // call to {@link ClassReader#readFrame()}.

  /** The bytecode offset of the current stack map frame. */
  int currentFrameOffset;

  /**
   * The type of the current stack map frame. One of {@link Opcodes#F_FULL}, {@link
   * Opcodes#F_APPEND}, {@link Opcodes#F_CHOP}, {@link Opcodes#F_SAME} or {@link Opcodes#F_SAME1}.
   */
  int currentFrameType;

  /**
   * The number of local variable types in the current stack map frame. Each type is represented
   * with a single array element (even long and double).
   */
  int currentFrameLocalCount;

  /**
   * The delta number of local variable types in the current stack map frame (each type is
   * represented with a single array element - even long and double). This is the number of local
   * variable types in this frame, minus the number of local variable types in the previous frame.
   */
  int currentFrameLocalCountDelta;

  /**
   * The types of the local variables in the current stack map frame. Each type is represented with
   * a single array element (even long and double), using the format described in {@link
   * MethodVisitor#visitFrame}. Depending on {@link #currentFrameType}, this contains the types of
   * all the local variables, or only those of the additional ones (compared to the previous frame).
   */
  Object[] currentFrameLocalTypes;

  /**
   * The number stack element types in the current stack map frame. Each type is represented with a
   * single array element (even long and double).
   */
  int currentFrameStackCount;

  /**
   * The types of the stack elements in the current stack map frame. Each type is represented with a
   * single array element (even long and double), using the format described in {@link
   * MethodVisitor#visitFrame}.
   */
  Object[] currentFrameStackTypes;
}
