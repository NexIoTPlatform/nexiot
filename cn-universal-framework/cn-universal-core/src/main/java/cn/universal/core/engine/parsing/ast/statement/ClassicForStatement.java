package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.asm.Label;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Node;
import cn.universal.core.engine.runtime.handle.OperatorHandle;
import java.util.List;

public class ClassicForStatement extends Node {

  private final Node initializer;
  private final Expression condition;
  private final Expression update;
  private final List<Node> body;

  public ClassicForStatement(
      Span span, Node initializer, Expression condition, Expression update, List<Node> body) {
    super(span);
    this.initializer = initializer;
    this.condition = condition;
    this.update = update;
    this.body = body;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    if (initializer != null) {
      initializer.visitMethod(compiler);
    }
    if (condition != null) {
      condition.visitMethod(compiler);
    }
    if (update != null) {
      update.visitMethod(compiler);
    }
    body.forEach(it -> it.visitMethod(compiler));
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    Label start = new Label();
    Label updateLabel = new Label();
    Label end = new Label();
    Label continueLabel = update == null ? start : updateLabel;
    if (initializer != null) {
      compiler.compile(initializer, true);
    }
    compiler.markLabel(continueLabel, end).label(start);
    if (condition != null) {
      compiler
          .visit(condition)
          .invoke(INVOKESTATIC, OperatorHandle.class, "isTrue", boolean.class, Object.class)
          .jump(IFEQ, end);
    }
    compiler.compile(body);
    if (update != null) {
      compiler.label(updateLabel).compile(update, true);
    }
    compiler.jump(GOTO, start).label(end).exitLabel();
  }
}
