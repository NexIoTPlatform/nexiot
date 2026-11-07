package cn.universal.core.engine.runtime.linq;

import cn.universal.core.engine.parsing.ast.BinaryOperation;
import java.util.List;
import java.util.Map;

public class SelectValue implements Comparable<SelectValue> {

  private final Map<String, Object> value;

  private final List<OrderValue> orderValues;

  private final boolean hasOrder;

  public SelectValue(Map<String, Object> value, List<OrderValue> orderValues) {
    this.value = value;
    this.orderValues = orderValues;
    this.hasOrder = !orderValues.isEmpty();
  }

  public Map<String, Object> getValue() {
    return value;
  }

  @Override
  public int compareTo(SelectValue o2) {
    if (!hasOrder) {
      return 0;
    }
    for (int i = 0, size = orderValues.size(); i < size; i++) {
      OrderValue ov1 = orderValues.get(i);
      OrderValue ov2 = o2.orderValues.get(i);
      int compareValue = BinaryOperation.compare(ov1.getValue(), ov2.getValue());
      if (compareValue != 0) {
        return compareValue * ov1.getOrder();
      }
    }
    return 0;
  }
}
