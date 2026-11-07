package cn.universal.core.engine.parsing;

import java.util.ArrayList;

public class VarScope extends ArrayList<VarIndex> {

  private VarScope parent;

  public VarScope(VarScope parent) {
    this.parent = parent;
  }

  public VarScope() {}

  public VarScope push() {
    return new VarScope(this);
  }

  public VarScope getParent() {
    return parent;
  }

  public VarScope pop() {
    return parent;
  }
}
