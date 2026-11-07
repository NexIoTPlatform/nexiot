package cn.universal.core.engine.parsing.ast;

import cn.universal.core.engine.asm.Label;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.runtime.handle.OperatorHandle;

public class TernaryOperation extends Expression {

  private final Expression condition;
  private final Expression trueExpression;
  private final Expression falseExpression;

  public TernaryOperation(
      Expression condition, Expression trueExpression, Expression falseExpression) {
    super(new Span(condition.getSpan(), falseExpression.getSpan()));
    this.condition = condition;
    this.trueExpression = trueExpression;
    this.falseExpression = falseExpression;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    condition.visitMethod(compiler);
    trueExpression.visitMethod(compiler);
    falseExpression.visitMethod(compiler);
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    Label end = new Label();
    Label falseValue = new Label();
    // condition ? trueExpr : falseExpr
    compiler
        .compile(condition) // 访问表达式
        .invoke(
            INVOKESTATIC, OperatorHandle.class, "isTrue", boolean.class, Object.class) // 判断是否为true
        .jump(IFEQ, falseValue) // 为false时跳转
        .visit(trueExpression) // 访问true表达式
        .jump(GOTO, end) // 跳转至结束
        .label(falseValue)
        .visit(falseExpression) // 访问false表达式
        .label(end);
  }
}
