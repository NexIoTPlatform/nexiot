package cn.universal.core.engine.runtime;

import cn.universal.core.engine.MagicScriptContext;

public class MagicScriptVariableAccessRuntime extends MagicScriptRuntime {

  private final String varName;

  public MagicScriptVariableAccessRuntime(String varName) {
    this.varName = varName;
  }

  @Override
  public Object execute(MagicScriptContext context) {
    return context.get(varName);
  }
}
