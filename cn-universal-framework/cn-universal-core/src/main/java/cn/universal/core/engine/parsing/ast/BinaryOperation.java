package cn.universal.core.engine.parsing.ast;

import cn.universal.core.engine.MagicScriptError;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.functions.ObjectConvertExtension;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.Token;
import cn.universal.core.engine.parsing.ast.binary.AddOperation;
import cn.universal.core.engine.parsing.ast.binary.AndOperation;
import cn.universal.core.engine.parsing.ast.binary.AssigmentOperation;
import cn.universal.core.engine.parsing.ast.binary.BitAndOperation;
import cn.universal.core.engine.parsing.ast.binary.BitOrOperation;
import cn.universal.core.engine.parsing.ast.binary.DivisionOperation;
import cn.universal.core.engine.parsing.ast.binary.EqualOperation;
import cn.universal.core.engine.parsing.ast.binary.GreaterEqualOperation;
import cn.universal.core.engine.parsing.ast.binary.GreaterOperation;
import cn.universal.core.engine.parsing.ast.binary.InstanceofOperation;
import cn.universal.core.engine.parsing.ast.binary.LShiftOperation;
import cn.universal.core.engine.parsing.ast.binary.LessEqualOperation;
import cn.universal.core.engine.parsing.ast.binary.LessOperation;
import cn.universal.core.engine.parsing.ast.binary.ModuloOperation;
import cn.universal.core.engine.parsing.ast.binary.MultiplicationOperation;
import cn.universal.core.engine.parsing.ast.binary.NotEqualOperation;
import cn.universal.core.engine.parsing.ast.binary.OrOperation;
import cn.universal.core.engine.parsing.ast.binary.RShift2Operation;
import cn.universal.core.engine.parsing.ast.binary.RShiftOperation;
import cn.universal.core.engine.parsing.ast.binary.SubtractionOperation;
import cn.universal.core.engine.parsing.ast.binary.XorOperation;
import cn.universal.core.engine.parsing.ast.statement.VariableAccess;
import java.math.BigDecimal;
import java.util.Date;

public abstract class BinaryOperation extends Expression {

  private Expression leftOperand;
  private Expression rightOperand;

  public BinaryOperation(Expression leftOperand, Span span, Expression rightOperand) {
    super(span);
    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
  }

  public static Expression create(
      Expression left, Token operator, Expression right, int linqLevel) {
    if (operator.getType().isModifiable()
        && left instanceof VariableAccess
        && ((VariableAccess) left).getVarIndex().isReadonly()) {
      MagicScriptError.error("const定义的变量不能被修改", new Span(left.getSpan(), right.getSpan()));
    }
    Expression expression = null;
    Span span = new Span(left.getSpan(), right.getSpan());
    switch (operator.getType()) {
      case Assignment:
        expression =
            linqLevel == 0
                ? new AssigmentOperation(left, span, right)
                : new EqualOperation(left, span, right, false);
        break;
      case Plus:
        expression = new AddOperation(left, span, right);
        break;
      case Minus:
        expression = new SubtractionOperation(left, span, right);
        break;
      case Asterisk:
        expression = new MultiplicationOperation(left, span, right);
        break;
      case ForwardSlash:
        expression = new DivisionOperation(left, span, right);
        break;
      case Percentage:
        expression = new ModuloOperation(left, span, right);
        break;
      case PlusEqual:
        expression = new AssigmentOperation(left, span, new AddOperation(left, span, right));
        break;
      case MinusEqual:
        expression =
            new AssigmentOperation(left, span, new SubtractionOperation(left, span, right));
        break;
      case AsteriskEqual:
        expression =
            new AssigmentOperation(left, span, new MultiplicationOperation(left, span, right));
        break;
      case ForwardSlashEqual:
        expression = new AssigmentOperation(left, span, new DivisionOperation(left, span, right));
        break;
      case PercentEqual:
        expression = new AssigmentOperation(left, span, new ModuloOperation(left, span, right));
        break;
      case Less:
        expression = new LessOperation(left, span, right);
        break;
      case LessEqual:
        expression = new LessEqualOperation(left, span, right);
        break;
      case Greater:
        expression = new GreaterOperation(left, span, right);
        break;
      case GreaterEqual:
        expression = new GreaterEqualOperation(left, span, right);
        break;
      case Equal:
        expression = new EqualOperation(left, span, right, false);
        break;
      case EqualEqualEqual:
        expression = new EqualOperation(left, span, right, true);
        break;
      case SqlNotEqual:
      case NotEqual:
        expression = new NotEqualOperation(left, span, right, false);
        break;
      case NotEqualEqual:
        expression = new NotEqualOperation(left, span, right, true);
        break;
      case SqlAnd:
      case And:
        expression = new AndOperation(left, span, right);
        break;
      case SqlOr:
      case Or:
        expression = new OrOperation(left, span, right);
        break;
      case LShift:
        expression = new LShiftOperation(left, span, right);
        break;
      case RShift:
        expression = new RShiftOperation(left, span, right);
        break;
      case Rshift2:
        expression = new RShift2Operation(left, span, right);
        break;
      case Xor:
        expression = new XorOperation(left, span, right);
        break;
      case BitAnd:
        expression = new BitAndOperation(left, span, right);
        break;
      case BitOr:
        expression = new BitOrOperation(left, span, right);
        break;
      case LShiftEqual:
        expression = new AssigmentOperation(left, span, new LShiftOperation(left, span, right));
        break;
      case RShiftEqual:
        expression = new AssigmentOperation(left, span, new RShiftOperation(left, span, right));
        break;
      case RShift2Equal:
        expression = new AssigmentOperation(left, span, new RShift2Operation(left, span, right));
        break;
      case XorEqual:
        expression = new AssigmentOperation(left, span, new XorOperation(left, span, right));
        break;
      case BitAndEqual:
        expression = new AssigmentOperation(left, span, new BitAndOperation(left, span, right));
        break;
      case BitOrEqual:
        expression = new AssigmentOperation(left, span, new BitOrOperation(left, span, right));
        break;
      case InstanceOf:
        expression = new InstanceofOperation(left, span, right);
        break;
      default:
        MagicScriptError.error("[" + operator.getText() + "]操作符未实现", span);
    }
    return expression;
  }

  /** 比较两个值 1 左边大 0 相等 -1 右边大 -2 无法比较 */
  public static int compare(Object left, Object right) {
    if (left == null && right == null) {
      return -2;
    }
    if (left == null) {
      return -1;
    }
    if (right == null) {
      return 1;
    }
    if (left instanceof Number && right instanceof Number) {
      if (left instanceof BigDecimal || right instanceof BigDecimal) {
        return ObjectConvertExtension.asDecimal(left)
            .compareTo(ObjectConvertExtension.asDecimal(right));
      } else if (left instanceof Double || right instanceof Double) {
        return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
      } else if (left instanceof Float || right instanceof Float) {
        return Float.compare(((Number) left).floatValue(), ((Number) right).floatValue());
      } else if (left instanceof Long || right instanceof Long) {
        return Long.compare(((Number) left).longValue(), ((Number) right).longValue());
      } else if (left instanceof Integer || right instanceof Integer) {
        return Integer.compare(((Number) left).intValue(), ((Number) right).intValue());
      } else if (left instanceof Short || right instanceof Short) {
        return Short.compare(((Number) left).shortValue(), ((Number) right).shortValue());
      } else if (left instanceof Byte || right instanceof Byte) {
        return Byte.compare(((Number) left).byteValue(), ((Number) right).byteValue());
      }
    }
    if (left instanceof Date && right instanceof Date) {
      return ((Date) left).compareTo((Date) right);
    }
    if (left instanceof String && right instanceof String) {
      return ((String) left).compareTo((String) right);
    }
    return -2;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    leftOperand.visitMethod(compiler);
    rightOperand.visitMethod(compiler);
  }

  public Expression getLeftOperand() {
    return leftOperand;
  }

  public void setLeftOperand(Expression leftOperand) {
    this.leftOperand = leftOperand;
  }

  public Expression getRightOperand() {
    return rightOperand;
  }

  public void setRightOperand(Expression rightOperand) {
    this.rightOperand = rightOperand;
  }
}
