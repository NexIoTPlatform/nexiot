package cn.universal.core.engine.parsing.ast.binary;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.functions.ObjectTypeConditionExtension;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.BinaryOperation;
import cn.universal.core.engine.parsing.ast.Expression;

/** instanceof */
public class InstanceofOperation extends BinaryOperation {

  public InstanceofOperation(Expression leftOperand, Span span, Expression rightOperand) {
    super(leftOperand, span, rightOperand);
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler
        .visit(getLeftOperand())
        .visit(getRightOperand())
        .typeInsn(CHECKCAST, Class.class)
        .lineNumber(getSpan())
        .invoke(
            INVOKESTATIC,
            ObjectTypeConditionExtension.class,
            "is",
            boolean.class,
            Object.class,
            Class.class)
        .asBoolean();
  }
}
