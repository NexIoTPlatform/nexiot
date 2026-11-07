package cn.universal.core.engine.parsing.ast.linq;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Literal;

public class WholeLiteral extends Literal {

  public WholeLiteral(Span span) {
    super(span);
  }

  public WholeLiteral(Span span, Object value) {
    super(span, value);
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler.load2();
  }
}
