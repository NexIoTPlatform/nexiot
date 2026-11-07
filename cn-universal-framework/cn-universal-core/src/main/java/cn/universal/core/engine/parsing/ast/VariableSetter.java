package cn.universal.core.engine.parsing.ast;

import cn.universal.core.engine.compile.MagicScriptCompiler;

public interface VariableSetter {

  default void compile_visit_variable(MagicScriptCompiler compiler) {
    throw new UnsupportedOperationException("暂不支持编译" + this.getClass().getSimpleName());
  }
}
