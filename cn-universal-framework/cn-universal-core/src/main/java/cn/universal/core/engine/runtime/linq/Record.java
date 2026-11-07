package cn.universal.core.engine.runtime.linq;

import cn.universal.core.engine.runtime.Variables;
import java.util.Collections;
import java.util.List;

public class Record {

  private final Object value;

  private final List<JoinValue> joinValues;

  public Record(Object value) {
    this(value, Collections.emptyList());
  }

  public Record(Object value, List<JoinValue> joinValues) {
    this.value = value;
    this.joinValues = joinValues;
  }

  public Object getValue() {
    return value;
  }

  public List<JoinValue> getJoinValues() {
    return joinValues;
  }

  public void setVariableValue(Variables variables) {
    this.joinValues.forEach(
        joinValue -> variables.setValue(joinValue.getIndex(), joinValue.getValue()));
  }

  public void removeVariableValue(Variables variables) {
    this.joinValues.forEach(
        joinValue -> variables.setValue(joinValue.getIndex(), Collections.emptyMap()));
  }
}
