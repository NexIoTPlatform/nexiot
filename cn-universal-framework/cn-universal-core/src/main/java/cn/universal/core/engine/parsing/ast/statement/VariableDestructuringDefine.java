package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import java.util.ArrayList;
import java.util.List;

public class VariableDestructuringDefine extends VariableDefine {

  private final List<VariableDefine> defines;
  private final Expression right;
  private final boolean mapAccess;

  public VariableDestructuringDefine(Span span, int size, Expression right, boolean mapAccess) {
    super(span, null, null);
    this.defines = new ArrayList<>(size);
    this.right = right;
    this.mapAccess = mapAccess;
  }

  public void add(VariableDefine variableDefine) {
    defines.add(variableDefine);
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    if (right != null) {
      right.visitMethod(compiler);
    }
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler.visit(this.right).store(4);
    for (int i = 0, size = this.defines.size(); i < size; i++) {
      VariableDefine define = this.defines.get(i);
      compiler.pre_store(define.getVarIndex());
      if (this.mapAccess) {
        compiler
            .newRuntimeContext()
            .load4()
            .ldc(define.getVarIndex().getName()) // 成员名
            .insn(ICONST_1) // 是否可空调用 ?.
            .asBoolean()
            .insn(ICONST_0) // 非linq中
            .asBoolean()
            .call("member_access", 5);
      } else {
        compiler.load4().visitInt(i).asInteger().operator("map_or_array_access");
      }
      compiler.scopeStore();
    }
  }
}
