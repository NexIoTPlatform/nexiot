package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import java.util.List;

public class MethodCall extends Expression {

  private final MemberAccess method;
  private final List<Expression> arguments;
  private final boolean inLinq;

  public MethodCall(Span span, MemberAccess method, List<Expression> arguments, boolean inLinq) {
    super(span);
    this.method = method;
    this.arguments = arguments;
    this.inLinq = inLinq;
  }

  public MemberAccess getMethod() {
    return method;
  }

  public List<Expression> getArguments() {
    return arguments;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    method.visitMethod(compiler);
    arguments.forEach(it -> it.visitMethod(compiler));
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    int size = arguments.size();
    compiler
        .newRuntimeContext()
        .ldc(method.getName().getText()) // 方法名
        .insn(
            arguments.stream().anyMatch(it -> it instanceof Spread)
                ? ICONST_1
                : ICONST_0) // 是否是 (...xxx)
        .asBoolean()
        .insn(method.isOptional() ? ICONST_1 : ICONST_0) // 是否允许可空调用
        .asBoolean()
        .visitInt(size)
        .typeInsn(ANEWARRAY, Object.class);
    for (int i = 0; i < size; i++) {
      Expression argument = arguments.get(i);
      compiler.insn(DUP).visitInt(i);
      if (inLinq && argument instanceof MemberAccess) {
        ((MemberAccess) argument).compileLinq(compiler);
      } else {
        compiler.visit(argument);
      }
      compiler.insn(AASTORE);
    }
    compiler
        .visit(method.getObject()) // 访问目标对象
        .lineNumber(
            new Span(getSpan().getSource(), method.getName().getStart(), getSpan().getEnd()))
        .call("invoke_method", 6); // 调用方法
  }
}
