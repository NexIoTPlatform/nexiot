package cn.universal.core.engine.parsing.ast.binary;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;

/** !=、!==操作 */
public class NotEqualOperation extends EqualOperation {

  public NotEqualOperation(
      Expression leftOperand, Span span, Expression rightOperand, boolean accurate) {
    super(leftOperand, span, rightOperand, accurate);
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler
        .visit(getLeftOperand())
        .visit(getRightOperand())
        .lineNumber(getSpan())
        .operator(accurate ? "not_accurate_equals" : "not_equals");
  }
}
