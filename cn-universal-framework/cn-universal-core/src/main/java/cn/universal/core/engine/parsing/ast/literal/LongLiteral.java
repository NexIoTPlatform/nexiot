package cn.universal.core.engine.parsing.ast.literal;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Literal;

/** long 常量 */
public class LongLiteral extends Literal {

  public LongLiteral(Span literal) {
    this(
        literal,
        Long.parseLong(
            literal.getText().substring(0, literal.getText().length() - 1).replace("_", "")));
  }

  public LongLiteral(Span span, Object value) {
    super(span);
    this.value = value;
  }

  @Override
  public void compile(MagicScriptCompiler context) {
    context.ldc(value).invoke(INVOKESTATIC, Long.class, "valueOf", Long.class, long.class);
  }
}
