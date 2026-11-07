package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import java.util.List;

public class FunctionCall extends Expression {

  private final Expression function;
  private final List<Expression> arguments;

  private final boolean inLinq;

  public FunctionCall(Span span, Expression function, List<Expression> arguments, boolean inLinq) {
    super(span);
    this.function = function;
    this.arguments = arguments;
    this.inLinq = inLinq;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    function.visitMethod(compiler);
    arguments.forEach(it -> it.visitMethod(compiler));
  }

  public Expression getFunction() {
    return function;
  }

  public List<Expression> getArguments() {
    return arguments;
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    int size = arguments.size();
    compiler
        .newRuntimeContext()
        .ldc(getFunction().getSpan().getText()) // 函数名
        .insn(arguments.stream().anyMatch(it -> it instanceof Spread) ? ICONST_1 : ICONST_0)
        .asBoolean() // 是否有扩展参数(...xxx)
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
        .visit(function) // 访问函数
        .lineNumber(getSpan())
        .call("invoke_function", 5); // 调用函数
  }
}
