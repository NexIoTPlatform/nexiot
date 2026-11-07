package cn.universal.core.engine.runtime;

public class ExitValue {

  private final Object[] values;

  public ExitValue() {
    this(new Object[0]);
  }

  public ExitValue(Object[] values) {
    this.values = values;
  }

  public Object[] getValues() {
    return values;
  }

  public int getLength() {
    return values.length;
  }
}
