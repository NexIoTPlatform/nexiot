package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Node;

/** break 语句 */
public class Break extends Node {

  public Break(Span span) {
    super(span);
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler.end();
  }
}
