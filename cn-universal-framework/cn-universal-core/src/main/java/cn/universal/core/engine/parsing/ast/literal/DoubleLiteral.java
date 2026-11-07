package cn.universal.core.engine.parsing.ast.literal;

import cn.universal.core.engine.MagicScriptError;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Literal;

/** double常量 */
public class DoubleLiteral extends Literal {

  public DoubleLiteral(Span literal) {
    super(literal);
    try {
      setValue(Double.parseDouble(literal.getText().replace("_", "")));
    } catch (NumberFormatException e) {
      MagicScriptError.error("定义double变量值不合法", literal, e);
    }
  }

  @Override
  public void compile(MagicScriptCompiler context) {
    context.ldc(value).invoke(INVOKESTATIC, Double.class, "valueOf", Double.class, double.class);
  }
}
