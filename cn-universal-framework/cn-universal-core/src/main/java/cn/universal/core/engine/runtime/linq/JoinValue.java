package cn.universal.core.engine.runtime.linq;

public class JoinValue {

  private final int index;

  private final Object value;

  public JoinValue(int index, Object value) {
    this.index = index;
    this.value = value;
  }

  public int getIndex() {
    return index;
  }

  public Object getValue() {
    return value;
  }
}
