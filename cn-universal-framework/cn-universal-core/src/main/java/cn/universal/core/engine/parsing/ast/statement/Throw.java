package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.exception.MagicScriptRuntimeException;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Node;

public class Throw extends Node {

  private final Expression expression;

  public Throw(Span span, Expression expression) {
    super(span);
    this.expression = expression;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    expression.visitMethod(compiler);
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler
        .visit(expression)
        .invoke(
            INVOKESTATIC,
            MagicScriptRuntimeException.class,
            "create",
            MagicScriptRuntimeException.class,
            Object.class)
        .insn(ATHROW);
  }
}
