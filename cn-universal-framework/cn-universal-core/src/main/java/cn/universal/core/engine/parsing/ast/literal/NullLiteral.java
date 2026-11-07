package cn.universal.core.engine.parsing.ast.literal;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Literal;

/** null 常量 */
public class NullLiteral extends Literal {

  public NullLiteral(Span span) {
    super(span);
  }

  @Override
  public void compile(MagicScriptCompiler context) {
    context.insn(ACONST_NULL);
  }
}
