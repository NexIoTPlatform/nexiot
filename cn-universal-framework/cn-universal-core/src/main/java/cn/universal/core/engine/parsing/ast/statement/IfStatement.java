package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.asm.Label;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Node;
import cn.universal.core.engine.runtime.handle.OperatorHandle;
import java.util.List;

public class IfStatement extends Node {

  private final Expression condition;
  private final List<Node> trueBlock;
  private final List<IfStatement> elseIfs;
  private final List<Node> falseBlock;

  public IfStatement(
      Span span,
      Expression condition,
      List<Node> trueBlock,
      List<IfStatement> elseIfs,
      List<Node> falseBlock) {
    super(span);
    this.condition = condition;
    this.trueBlock = trueBlock;
    this.elseIfs = elseIfs;
    this.falseBlock = falseBlock;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    condition.visitMethod(compiler);
    trueBlock.forEach(it -> it.visitMethod(compiler));
    elseIfs.forEach(it -> it.visitMethod(compiler));
    falseBlock.forEach(it -> it.visitMethod(compiler));
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    Label end = new Label();
    Label next = new Label();
    compiler
        .visit(condition) // 访问表达式
        .invoke(
            INVOKESTATIC, OperatorHandle.class, "isTrue", boolean.class, Object.class) // 判断是否为true
        .jump(IFEQ, next)
        .compile(trueBlock) // 编译true代码块
        .jump(GOTO, end); // 跳转至结束
    for (IfStatement elseIf : elseIfs) {
      compiler
          .label(next)
          .visit(elseIf.condition) // 访问else if 表达式
          .invoke(
              INVOKESTATIC,
              OperatorHandle.class,
              "isTrue",
              boolean.class,
              Object.class); // 判断是否为true
      next = new Label();
      compiler
          .jump(IFEQ, next)
          .compile(elseIf.trueBlock) // 编译 else if true代码块
          .jump(GOTO, end);
    }
    compiler.label(next);
    if (!falseBlock.isEmpty()) {
      // 编译 false 代码块
      compiler.compile(falseBlock);
    }
    compiler.label(end);
  }
}
