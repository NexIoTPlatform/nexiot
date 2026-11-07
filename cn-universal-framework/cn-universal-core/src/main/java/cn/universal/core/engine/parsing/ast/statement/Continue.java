package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Node;

/** continue语句 */
public class Continue extends Node {

  public Continue(Span span) {
    super(span);
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler.start();
  }
}
