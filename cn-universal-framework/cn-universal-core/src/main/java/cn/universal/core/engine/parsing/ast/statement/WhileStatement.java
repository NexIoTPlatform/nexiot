package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.asm.Label;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Node;
import cn.universal.core.engine.runtime.handle.OperatorHandle;
import java.util.List;

public class WhileStatement extends Node {

  private final Expression condition;
  private final List<Node> trueBlock;

  public WhileStatement(Span span, Expression condition, List<Node> trueBlock) {
    super(span);
    this.condition = condition;
    this.trueBlock = trueBlock;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    condition.visitMethod(compiler);
    trueBlock.forEach(it -> it.visitMethod(compiler));
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    Label start = new Label();
    Label end = new Label();
    compiler
        .markLabel(start, end) // 标记 continue 和 break 位置
        .label(start).debugLoopCheckpoint()
        // 判断是否为true
        .visit(condition)
        .invoke(INVOKESTATIC, OperatorHandle.class, "isTrue", boolean.class, Object.class)
        // 值为false时，跳出循环
        .jump(IFEQ, end)
        // 执行循环体
        .compile(trueBlock)
        // 执行完毕后跳转到循环起始位置
        .jump(GOTO, start)
        .label(end)
        .exitLabel();
  }
}
