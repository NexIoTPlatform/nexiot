package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.VarIndex;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Node;

public class VariableDefine extends Node {

  private final Expression right;

  private final VarIndex varIndex;

  public VariableDefine(Span span, VarIndex varIndex, Expression right) {
    super(span);
    this.varIndex = varIndex;
    this.right = right;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    if (right != null) {
      right.visitMethod(compiler);
    }
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler
        .pre_store(varIndex)
        .visit(right) // 读取变量值
        .scopeStore(); // 保存变量
  }

  public VarIndex getVarIndex() {
    return varIndex;
  }
}
