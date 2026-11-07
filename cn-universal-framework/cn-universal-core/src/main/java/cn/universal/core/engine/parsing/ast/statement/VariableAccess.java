package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.VarIndex;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.VariableSetter;

public class VariableAccess extends Expression implements VariableSetter {

  private final VarIndex varIndex;

  public VariableAccess(Span name, VarIndex varIndex) {
    super(name);
    this.varIndex = varIndex;
  }

  public VarIndex getVarIndex() {
    return varIndex;
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler.load(varIndex);
  }
}
