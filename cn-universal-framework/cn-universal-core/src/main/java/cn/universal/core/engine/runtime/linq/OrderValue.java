package cn.universal.core.engine.runtime.linq;

public class OrderValue {

  private final Object value;

  private final int order;

  public OrderValue(Object value, int order) {
    this.value = value;
    this.order = order;
  }

  public Object getValue() {
    return value;
  }

  public int getOrder() {
    return order;
  }
}
