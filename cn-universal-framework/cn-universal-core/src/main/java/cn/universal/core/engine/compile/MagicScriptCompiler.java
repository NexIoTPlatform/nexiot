package cn.universal.core.engine.compile;

import cn.universal.core.engine.MagicScriptContext;
import cn.universal.core.engine.asm.ClassWriter;
import cn.universal.core.engine.asm.Handle;
import cn.universal.core.engine.asm.Label;
import cn.universal.core.engine.asm.MethodVisitor;
import cn.universal.core.engine.asm.Opcodes;
import cn.universal.core.engine.asm.Type;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.VarIndex;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Node;
import cn.universal.core.engine.parsing.ast.VariableSetter;
import cn.universal.core.engine.parsing.ast.binary.AssigmentOperation;
import cn.universal.core.engine.parsing.ast.statement.VariableAccess;
import cn.universal.core.engine.runtime.MagicScriptRuntime;
import cn.universal.core.engine.runtime.RuntimeContext;
import cn.universal.core.engine.runtime.Variables;
import cn.universal.core.engine.runtime.function.MagicScriptLambdaFunction;
import cn.universal.core.engine.runtime.handle.ArithmeticHandle;
import cn.universal.core.engine.runtime.handle.BitHandle;
import cn.universal.core.engine.runtime.handle.FunctionCallHandle;
import cn.universal.core.engine.runtime.handle.OperatorHandle;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

/** 脚本编译器 */
public class MagicScriptCompiler implements Opcodes {

  private static final AtomicLong COUNTER = new AtomicLong(1);

  private final ClassWriter classWriter;

  private final Long id = COUNTER.getAndIncrement();

  /** < <= == === != !=== >= > 等操作符处理器 */
  private static final Handle OPERATOR_HANDLE = makeHandle(OperatorHandle.class);

  /** << >> >>> & ^ | 运算 */
  private static final Handle BIT_HANDLE = makeHandle(BitHandle.class);

  /** 方法调用、lambda调用、函数调用处理器 */
  private static final Handle FUNCTION_HANDLE = makeHandle(FunctionCallHandle.class);

  /** + - * / % 处理器 */
  private static final Handle ARITHMETIC_HANDLE = makeHandle(ArithmeticHandle.class);

  private final Stack<MethodVisitor> methodVisitors = new Stack<>();

  private final Stack<List<String>> vars = new Stack<>();

  private final Stack<FlowControlLabel> labelStack = new Stack<>();

  private final Stack<List<Node>> finallyStack = new Stack<>();

  private final Set<VarIndex> varIndices;

  /** -1 ~ 5的int值 */
  private static final int[] ICONST = {
    ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5
  };

  private final List<Span> spans = new ArrayList<>();

  private int functionIndex = 0;

  private int tempIndex = 4;

  private final boolean debug;

  private int lastLineNumber = -1;

  private boolean contextInited = false;

  public MagicScriptCompiler(Set<VarIndex> varIndices, boolean debug) {
    this.varIndices = varIndices;
    this.debug = debug;
    // 创建类并继承 MagicScriptRuntime
    classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    classWriter.visit(
        V1_8,
        ACC_PUBLIC | ACC_SUPER,
        getClassName(),
        null,
        getJvmType(MagicScriptRuntime.class),
        null);
    classWriter.visitSource(getClassName() + ".ms", null);
    createMethod(ACC_PUBLIC, "<init>", Descriptor.make_descriptor(void.class));
    this.load0()
        .invoke(INVOKESPECIAL, MagicScriptRuntime.class, "<init>", void.class)
        .insn(RETURN)
        .pop();
    // 创建execute方法
    createMethod(
        ACC_PUBLIC, "execute", Descriptor.make_descriptor(Object.class, MagicScriptContext.class));
    initContext();
  }

  public List<Span> getSpans() {
    return spans;
  }

  /**
   * 创建方法
   *
   * @param access 访问属性
   * @param methodName 方法名
   * @param descriptor 方法描述
   */
  public MagicScriptCompiler createMethod(int access, String methodName, String descriptor) {
    MethodVisitor visitor = classWriter.visitMethod(access, methodName, descriptor, null, null);
    visitor.visitCode();
    methodVisitors.push(visitor);
    vars.push(new ArrayList<>());
    finallyStack.push(null);
    labelStack.push(new FlowControlLabel(null, null));
    return this;
  }

  public int getTempIndex() {
    return tempIndex++;
  }

  /** 获取lambda函数下标 */
  public int getFunctionIndex() {
    return ++functionIndex;
  }

  /** 标识continue和break位置 */
  public MagicScriptCompiler markLabel(Label start, Label end) {
    labelStack.push(new FlowControlLabel(start, end));
    return this;
  }

  /** 标识仅支持break的位置，例如 switch */
  public MagicScriptCompiler markBreakLabel(Label end) {
    labelStack.push(new FlowControlLabel(null, end));
    return this;
  }

  /** 消除标记 */
  public MagicScriptCompiler exitLabel() {
    labelStack.pop();
    return this;
  }

  /** 跳转到continue位置 */
  public MagicScriptCompiler start() {
    for (int i = labelStack.size() - 1; i >= 0; i--) {
      Label label = labelStack.get(i).getContinueLabel();
      if (label != null) {
        return jump(GOTO, label);
      }
    }
    throw new MagicScriptCompileException("continue 只能在循环中使用");
  }

  /** 跳转到break位置 */
  public MagicScriptCompiler end() {
    for (int i = labelStack.size() - 1; i >= 0; i--) {
      Label label = labelStack.get(i).getBreakLabel();
      if (label != null) {
        return jump(GOTO, label);
      }
    }
    throw new MagicScriptCompileException("break 只能在循环或 switch 中使用");
  }

  /** 访问AST节点 */
  public MagicScriptCompiler visit(Node node) {
    // 对于赋值语句的特殊处理
    if (node instanceof AssigmentOperation) {
      AssigmentOperation operation = (AssigmentOperation) node;
      // 如果是a = b = 1这种情况，则需要读取a的值
      if (operation.getLeftOperand() instanceof VariableAccess) {
        return compile(node, true).compile(operation.getLeftOperand());
      }
    }
    return compile(node, false);
  }

  /** 编译AST节点 */
  public MagicScriptCompiler compile(Node node) {
    return compile(node, false);
  }

  public MagicScriptCompiler lineNumber(Span span) {
    spans.add(span);
    MethodVisitor mv = self();
    Label label = new Label();
    // 设置行号（节点序号）
    mv.visitLabel(label);
    mv.visitLineNumber(spans.size() - 1, label);
    return this;
  }

  public MagicScriptCompiler loadContext() {
    if (contextInited || methodVisitors.size() > 1) {
      load0()
          .self()
          .visitFieldInsn(
              GETFIELD, getClassName(), "context", Type.getDescriptor(MagicScriptContext.class));
      return this;
    }
    return load1();
  }

  public MagicScriptCompiler newRuntimeContext() {
    return this.typeInsn(NEW, RuntimeContext.class)
        .insn(DUP)
        .loadContext()
        .load2()
        .invoke(
            INVOKESPECIAL,
            RuntimeContext.class,
            "<init>",
            void.class,
            MagicScriptContext.class,
            Variables.class);
  }

  /**
   * 编译AST节点
   *
   * @param node AST节点
   * @param pop 是否需要弹出栈顶
   */
  public MagicScriptCompiler compile(Node node, boolean pop) {
    if (node == null) {
      return insn(ACONST_NULL);
    } else {
      lineNumber(node.getSpan());
      if (debug) {
        Span.Line currentLine = node.getSpan().getLine();
        int line = currentLine.getLineNumber();
        if (lastLineNumber != line) {
          this.loadContext()
              .visitInt(line)
              .visitInt(currentLine.getStartCol())
              .visitInt(currentLine.getEndLineNumber())
              .visitInt(currentLine.getEndCol())
              .load2()
              .invoke(
                  INVOKEVIRTUAL,
                  MagicScriptContext.class,
                  "pause",
                  void.class,
                  int.class,
                  int.class,
                  int.class,
                  int.class,
                  Variables.class);
          lastLineNumber = line;
        }
      }
      // 编译该节点
      node.compile(this);
    }
    // 对于赋值语句的特殊处理，因为赋值语句有两种
    // 不带返回值的： a+=1
    // 带返回值的 xxx.xx = 1
    if (node instanceof AssigmentOperation) {
      AssigmentOperation operation = (AssigmentOperation) node;
      if (operation.getLeftOperand() instanceof VariableSetter
          && operation.getLeftOperand() instanceof VariableAccess) {
        return this;
      }
    }
    // 对于带有返回值的表达式，且需要弹出栈顶时，追加指令POP
    return pop && node instanceof Expression ? insn(POP) : this;
  }

  public MagicScriptCompiler tryCatch(Label start, Label end, Label handle, Class<?> target) {
    self().visitTryCatchBlock(start, end, handle, getJvmType(target));
    return this;
  }

  /** 访问 */
  public MagicScriptCompiler visit(List<? extends Node> nodes) {
    nodes.forEach(this::visit);
    return this;
  }

  /** 编译 */
  public MagicScriptCompiler compile(List<? extends Node> nodes) {
    nodes.forEach(it -> compile(it, true));
    return this;
  }

  /** 加载this */
  public MagicScriptCompiler load0() {
    self().visitVarInsn(ALOAD, 0);
    return this;
  }

  /** 加载context */
  public MagicScriptCompiler load1() {
    self().visitVarInsn(ALOAD, 1);
    return this;
  }

  /** 加载context */
  public void newArrayList() {
    this.typeInsn(NEW, ArrayList.class)
        .insn(DUP)
        .invoke(INVOKESPECIAL, ArrayList.class, "<init>", void.class);
  }

  /** 加载Variables */
  public MagicScriptCompiler load2() {
    self().visitVarInsn(ALOAD, 2);
    return this;
  }

  /** 加载3号变量，一般指异常(临时变量) */
  public MagicScriptCompiler load3() {
    self().visitVarInsn(ALOAD, 3);
    return this;
  }

  public MagicScriptCompiler load4() {
    self().visitVarInsn(ALOAD, 4);
    return this;
  }

  /** 加载本地临时变量 */
  public MagicScriptCompiler loadLocal(int index) {
    self().visitVarInsn(ALOAD, index);
    return this;
  }

  /** 加载变量 */
  public MagicScriptCompiler load(int index) {
    return load2()
        .visitInt(index)
        .invoke(INVOKEVIRTUAL, Variables.class, "getValue", Object.class, int.class);
  }

  /** 加载变量 */
  public MagicScriptCompiler load(VarIndex varIndex) {
    return load(varIndex.getIndex());
  }

  /**
   * 加载变量
   *
   * @param name 变量名
   */
  public MagicScriptCompiler load(String name) {
    int index = vars.peek().indexOf(name) + 1;
    if (index > 0) { // 如果当前栈中有，则直接使用
      return load(index);
    } else {
      // 从环境中获取
      this.load1()
          .ldc(name)
          .invoke(
              INVOKEVIRTUAL,
              MagicScriptContext.class,
              "getEnvironmentValue",
              Object.class,
              String.class);
    }
    return this;
  }

  public MagicScriptCompiler label(Label label) {
    self().visitLabel(label);
    return this;
  }

  /** 跳转 */
  public MagicScriptCompiler jump(int opcode, Label label) {
    self().visitJumpInsn(opcode, label);
    return this;
  }

  /** 移除变量 */
  public MagicScriptCompiler remove(VarIndex varIndex) {
    if (varIndex == null) {
      return this;
    }
    return remove(varIndex.getName());
  }

  /** 移除变量 */
  public MagicScriptCompiler remove(String name) {
    List<String> varList = vars.peek();
    int index = varList.indexOf(name);
    if (index > -1) {
      varList.set(index, null);
    }
    return this;
  }

  /** 配合pre_store使用，保存至数组中 */
  public MagicScriptCompiler store() {
    return invoke(INVOKEVIRTUAL, Variables.class, "setValue", void.class, int.class, Object.class);
  }

  /** 配合pre_store使用，保存至数组中 */
  public MagicScriptCompiler store(VarIndex varIndex) {
    return varIndex.isScoped() ? scopeStore() : store();
  }

  /** 配合pre_store使用，保存至数组中 */
  public MagicScriptCompiler scopeStore() {
    return invoke(
        INVOKEVIRTUAL, Variables.class, "setScopeValue", void.class, int.class, Object.class);
  }

  /** 保存变量 */
  public MagicScriptCompiler store(int index) {
    self().visitVarInsn(ASTORE, index);
    return this;
  }

  /** 保存变量 */
  public MagicScriptCompiler frame(
      int type, int numLocal, Object[] local, int numStack, Object[] stack) {
    self().visitFrame(type, numLocal, local, numStack, stack);
    return this;
  }

  /** 写变量前的准备 */
  public MagicScriptCompiler pre_store(int index) {
    return load2().visitInt(index);
  }

  /** 写变量前的准备 */
  public MagicScriptCompiler pre_store(VarIndex varIndex) {
    return pre_store(varIndex.getIndex());
  }

  public MagicScriptCompiler bipush(int value) {
    self().visitIntInsn(BIPUSH, value);
    return this;
  }

  public MagicScriptCompiler typeInsn(int opcode, Class<?> target) {
    self().visitTypeInsn(opcode, getJvmType(target));
    return this;
  }

  /**
   * 二元运算
   *
   * @param methodName 运算方法
   */
  public MagicScriptCompiler operator(String methodName) {
    self()
        .visitInvokeDynamicInsn(
            methodName,
            MethodType.genericMethodType(2).toMethodDescriptorString(),
            OPERATOR_HANDLE,
            2);
    return this;
  }

  /**
   * 位运算
   *
   * @param methodName 运算方法
   */
  public MagicScriptCompiler bit(String methodName) {
    self()
        .visitInvokeDynamicInsn(
            methodName, MethodType.genericMethodType(2).toMethodDescriptorString(), BIT_HANDLE, 2);
    return this;
  }

  /**
   * 将方法转为lambda
   *
   * @param methodName 方法名
   */
  public MagicScriptCompiler lambda(String methodName) {
    load0();
    Handle metaFactory =
        new Handle(
            H_INVOKESTATIC,
            getJvmType(LambdaMetafactory.class),
            "metafactory",
            Descriptor.make_descriptor(
                CallSite.class,
                MethodHandles.Lookup.class,
                String.class,
                MethodType.class,
                MethodType.class,
                MethodHandle.class,
                MethodType.class),
            false);
    String descriptor = Descriptor.make_descriptor(Object.class, Variables.class, Object[].class);
    Handle impl = new Handle(H_INVOKEVIRTUAL, getClassName(), methodName, descriptor, false);
    self()
        .visitInvokeDynamicInsn(
            "apply",
            "(L"
                + getClassName()
                + ";)"
                + Type.getType(MagicScriptLambdaFunction.class).getDescriptor(),
            metaFactory,
            Type.getType(descriptor),
            impl,
            Type.getType(descriptor));
    load2()
        .self()
        .visitMethodInsn(
            INVOKEVIRTUAL,
            getClassName(),
            "createLambda",
            Descriptor.make_descriptor(
                MagicScriptLambdaFunction.class, MagicScriptLambdaFunction.class, Variables.class),
            false);
    return this;
  }

  /**
   * invokedynamic调用
   *
   * @param methodName 方法名
   * @param arguments 参数个数
   */
  public MagicScriptCompiler call(String methodName, int arguments) {
    self()
        .visitInvokeDynamicInsn(
            methodName,
            MethodType.genericMethodType(arguments).toMethodDescriptorString(),
            FUNCTION_HANDLE,
            arguments);
    return this;
  }

  /**
   * 执行算术运算
   *
   * @param methodName 方法名
   */
  public MagicScriptCompiler arithmetic(String methodName) {
    self()
        .visitInvokeDynamicInsn(
            methodName,
            MethodType.genericMethodType(2).toMethodDescriptorString(),
            ARITHMETIC_HANDLE,
            2);
    return this;
  }

  /** 将int值装箱 */
  public MagicScriptCompiler asInteger() {
    return invoke(INVOKESTATIC, Integer.class, "valueOf", Integer.class, int.class);
  }

  /** 将boolean值装箱 */
  public MagicScriptCompiler asBoolean() {
    return invoke(INVOKESTATIC, Boolean.class, "valueOf", Boolean.class, boolean.class);
  }

  /**
   * 调用方法
   *
   * @param opcode 调用类型
   * @param target 目标类
   * @param method 方法名
   * @param returnType 返回值类型
   * @param argumentTypes 参数类型
   */
  public MagicScriptCompiler invoke(
      int opcode, Class<?> target, String method, Class<?> returnType, Class<?>... argumentTypes) {
    return invoke(opcode, target, method, false, returnType, argumentTypes);
  }

  /**
   * 调用方法
   *
   * @param opcode 调用类型
   * @param target 目标类
   * @param method 方法名
   * @param isInterface 是否是接口
   * @param returnType 返回值类型
   * @param argumentTypes 参数类型
   */
  public MagicScriptCompiler invoke(
      int opcode,
      Class<?> target,
      String method,
      boolean isInterface,
      Class<?> returnType,
      Class<?>... argumentTypes) {
    self()
        .visitMethodInsn(
            opcode,
            getJvmType(target),
            method,
            Descriptor.make_descriptor(returnType, argumentTypes),
            isInterface);
    return this;
  }

  public MagicScriptCompiler ldc(Object value) {
    self().visitLdcInsn(value);
    return this;
  }

  public MagicScriptCompiler insn(int opcode) {
    self().visitInsn(opcode);
    return this;
  }

  public void intInsn(int opcode, int operand) {
    self().visitIntInsn(opcode, operand);
  }

  /** 编译数组 */
  public MagicScriptCompiler newArray(List<Expression> values) {
    int size = values.size();
    visitInt(size).typeInsn(ANEWARRAY, Object.class);
    for (int i = 0; i < size; i++) {
      insn(DUP).visitInt(i).visit(values.get(i)).insn(AASTORE);
    }
    return this;
  }

  /** 编译int值 */
  public MagicScriptCompiler visitInt(int value) {
    if (value >= -1 && value <= 5) {
      insn(ICONST[value + 1]);
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      intInsn(BIPUSH, value);
    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
      intInsn(SIPUSH, value);
    } else {
      ldc(value);
    }
    return this;
  }

  private void initContext() {
    if (!this.contextInited) {
      this.load0().load1();
      self()
          .visitFieldInsn(
              PUTFIELD, getClassName(), "context", Type.getDescriptor(MagicScriptContext.class));
      this.contextInited = true;
      // var2 = context.createVariables(this, varIndices.size())
      this.load1()
          .load0()
          .visitInt(varIndices.size())
          .invoke(
              INVOKEVIRTUAL,
              MagicScriptContext.class,
              "createVariables",
              Variables.class,
              MagicScriptRuntime.class,
              int.class)
          .store(2);
    }
  }

  public void loadVars() {
    // 对于未定义变量值的，从环境中获取
    // var2[xx] = super.getEnvironmentValue(varIndex.getName())
    this.varIndices.stream()
        .filter(VarIndex::isReference)
        .forEach(
            varIndex ->
                this.load2()
                    .visitInt(varIndex.getIndex())
                    .load1()
                    .ldc(varIndex.getName())
                    .invoke(
                        INVOKEVIRTUAL,
                        MagicScriptContext.class,
                        "getEnvironmentValue",
                        Object.class,
                        String.class)
                    .store());
  }

  public String visitMethod(String methodName, Runnable callback) {
    return visitMethod(methodName, Collections.emptyList(), Collections.emptyList(), callback);
  }

  public String visitMethod(
      String methodName, List<Node> childNodes, List<VarIndex> parameters, Runnable callback) {
    childNodes.forEach(it -> it.visitMethod(this));
    int index = this.getFunctionIndex();
    methodName = methodName + "_" + index;
    this.createMethod(
            ACC_PRIVATE,
            methodName,
            Descriptor.make_descriptor(Object.class, Variables.class, Object[].class))
        .load1() // Variables
        .load2() // 传入的参数
        .visitInt(index)
        // 构建参数
        .visitInt(parameters.size())
        .intInsn(NEWARRAY, T_INT); // new int[parameters.size()]
    for (int i = 0; i < parameters.size(); i++) {
      this.insn(DUP).visitInt(i).visitInt(parameters.get(i).getIndex()).insn(IASTORE);
    }
    // 复制变量
    this.invoke(
            INVOKEVIRTUAL,
            Variables.class,
            "copy",
            Variables.class,
            Object[].class,
            int.class,
            int[].class)
        .store(2);
    callback.run();
    this.pop();
    return methodName;
  }

  public String visitMethod(String methodName, List<Node> childNodes, List<VarIndex> parameters) {
    return visitMethod(methodName, childNodes, parameters, () -> this.compile(childNodes));
  }

  public List<Node> finallyBlock() {
    if (finallyStack.isEmpty()) {
      return null;
    }
    return finallyStack.peek();
  }

  public void putHeadFinallyBlock(List<Node> finallyBlock) {
    finallyStack.insertElementAt(finallyBlock, 0);
  }

  public void putFinallyBlock(List<Node> finallyBlock) {
    finallyStack.push(finallyBlock);
  }

  public List<Node> getFinallyBlock() {
    if (finallyStack.isEmpty()) {
      return null;
    }
    return finallyStack.pop();
  }

  public MagicScriptCompiler pop() {
    getFinallyBlock();
    MethodVisitor visitor = methodVisitors.pop();
    visitor.visitInsn(ACONST_NULL);
    visitor.visitInsn(ARETURN);
    visitor.visitMaxs(0, 0);
    visitor.visitEnd();
    vars.pop();
    labelStack.pop();
    return this;
  }

  public byte[] bytecode() {
    pop();
    classWriter.visitEnd();
    return classWriter.toByteArray();
  }

  /** 获取类名 */
  public String getClassName() {
    return "MagicScript_" + id;
  }

  private MethodVisitor self() {
    return methodVisitors.peek();
  }

  private static class FlowControlLabel {

    private final Label continueLabel;

    private final Label breakLabel;

    private FlowControlLabel(Label continueLabel, Label breakLabel) {
      this.continueLabel = continueLabel;
      this.breakLabel = breakLabel;
    }

    public Label getContinueLabel() {
      return continueLabel;
    }

    public Label getBreakLabel() {
      return breakLabel;
    }
  }

  private static String getJvmType(Class<?> target) {
    return target == null ? null : target.getName().replace(".", "/");
  }

  private static Handle makeHandle(Class<?> target) {
    return new Handle(
        H_INVOKESTATIC,
        getJvmType(target),
        "bootstrap",
        Descriptor.make_descriptor(
            CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class),
        false);
  }
}
