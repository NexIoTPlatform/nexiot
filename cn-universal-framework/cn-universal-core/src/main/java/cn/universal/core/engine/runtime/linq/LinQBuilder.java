package cn.universal.core.engine.runtime.linq;

import cn.universal.core.engine.functions.MapExtension;
import cn.universal.core.engine.functions.ObjectConvertExtension;
import cn.universal.core.engine.functions.StreamExtension;
import cn.universal.core.engine.runtime.Variables;
import cn.universal.core.engine.runtime.function.MagicScriptLambdaFunction;
import cn.universal.core.engine.runtime.handle.OperatorHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinQBuilder {

  private final Variables variables;

  private static final Object[] EMPTY_PARAMETER = new Object[0];

  private List<Object> fromObjects;

  private int fromAliasIndex;

  private final List<SelectField> selects = new ArrayList<>();

  private MagicScriptLambdaFunction where;

  private MagicScriptLambdaFunction having;

  private final List<MagicScriptLambdaFunction> groups = new ArrayList<>();

  private final List<LinQJoinValue> joins = new ArrayList<>();

  private int joinSize = 0;

  private final List<LinQOrder> orders = new ArrayList<>();

  private long limit = Long.MIN_VALUE;

  private long offset = Long.MIN_VALUE;

  private LinQBuilder(Variables variables) {
    this.variables = variables;
  }

  public static LinQBuilder create(Variables variables) {
    return new LinQBuilder(variables);
  }

  public LinQBuilder where(MagicScriptLambdaFunction condition) {
    where = condition;
    return this;
  }

  public LinQBuilder from(Object object, int aliasIndex) {
    this.fromAliasIndex = aliasIndex;
    this.fromObjects = convertToList(object);
    return this;
  }

  private List<Object> convertToList(Object object) {
    if (object instanceof Map) {
      return (List<Object>)
          MapExtension.asList(
              (Map<?, ?>) object, (entry) -> Collections.singletonMap(entry[0], entry[1]));
    } else {
      try {
        return StreamExtension.arrayLikeToList(object);
      } catch (Exception e) {
        return Collections.singletonList(object);
      }
    }
  }

  public LinQBuilder group(MagicScriptLambdaFunction function) {
    groups.add(function);
    return this;
  }

  public LinQBuilder join(
      MagicScriptLambdaFunction condition, Object object, boolean isLeftJoin, int aliasIndex) {
    joins.add(new LinQJoinValue(condition, convertToList(object), isLeftJoin, aliasIndex));
    joinSize++;
    return this;
  }

  public LinQBuilder having(MagicScriptLambdaFunction condition) {
    having = condition;
    return this;
  }

  public LinQBuilder order(MagicScriptLambdaFunction function, int order) {
    this.orders.add(new LinQOrder(function, order));
    return this;
  }

  public LinQBuilder select(MagicScriptLambdaFunction function, String aliasName, int aliasIndex) {
    selects.add(new SelectField(function, aliasName, aliasIndex));
    return this;
  }

  public Object executeAndFetchFirst() {
    List<?> list = execute();
    return list.isEmpty() ? null : list.get(0);
  }

  public LinQBuilder limit(Object limit) {
    this.limit = ObjectConvertExtension.asLong(limit, Long.MIN_VALUE);
    return this;
  }

  public LinQBuilder offset(Object offset) {
    this.offset = ObjectConvertExtension.asLong(offset, Long.MIN_VALUE);
    return this;
  }

  public List<?> execute() {
    List<Record> records = new ArrayList<>();
    for (Object object : fromObjects) {
      variables.setValue(fromAliasIndex, object);
      // 处理 join + where
      records.addAll(processWhere(processJoin(object), object));
    }
    // 处理 group + having
    records = processGroup(records);
    // 处理 select
    List<SelectValue> result = processSelect(records);
    Stream<Map<String, Object>> stream = result.stream().sorted().map(SelectValue::getValue);
    if (offset > 0) {
      stream = stream.skip(offset);
    }
    if (limit > 0) {
      stream = stream.limit(limit);
    }
    return stream.collect(Collectors.toList());
  }

  private void processRow(Object item, Map<String, Object> row, SelectField field) {
    if (item instanceof Map) {
      row.putAll((Map<String, Object>) item);
    } else if (field.isWhole() && item instanceof List) {
      row.putAll((Map<String, Object>) StreamExtension.first(item));
    } else {
      row.put(field.getAliasName(), item);
    }
  }

  private List<SelectValue> processSelect(List<Record> records) {
    List<SelectValue> result = new ArrayList<>(records.size());
    int fieldSize = selects.size();
    for (Record record : records) {
      Map<String, Object> row = new LinkedHashMap<>(fieldSize);
      variables.setValue(fromAliasIndex, record.getValue());
      record.setVariableValue(variables);
      for (SelectField field : selects) {
        MagicScriptLambdaFunction function = field.getFunction();
        if (function == null) {
          processRow(record.getValue(), row, field);
          record.getJoinValues().forEach(item -> processRow(item, row, field));
        } else {
          processRow(apply(function, variables, EMPTY_PARAMETER), row, field);
        }
      }
      List<OrderValue> orderValues = new ArrayList<>();
      if (!orders.isEmpty()) {
        for (LinQOrder order : orders) {
          orderValues.add(
              new OrderValue(
                  apply(order.getFunction(), variables, EMPTY_PARAMETER), order.getOrder()));
        }
      }
      result.add(new SelectValue(row, orderValues));
      record.removeVariableValue(variables);
    }
    return result;
  }

  private List<Record> processGroup(List<Record> records) {
    if (!groups.isEmpty()) {
      Map<List<Object>, List<Record>> group = new LinkedHashMap<>();
      for (Record record : records) {
        variables.setValue(fromAliasIndex, record.getValue());
        record.setVariableValue(variables);
        List<Object> keys =
            groups.stream()
                .map(field -> apply(field, variables, EMPTY_PARAMETER))
                .collect(Collectors.toList());
        group.computeIfAbsent(keys, k -> new ArrayList<>()).add(record);
      }
      records = new ArrayList<>();
      for (Map.Entry<List<Object>, List<Record>> entry : group.entrySet()) {
        List<Record> value = entry.getValue();
        Map<Integer, List<Object>> joinMap = new HashMap<>();
        List<JoinValue> joinValues = new ArrayList<>();
        value.stream()
            .map(Record::getJoinValues)
            .flatMap(Collection::stream)
            .forEach(
                item ->
                    joinMap
                        .computeIfAbsent(item.getIndex(), k -> new ArrayList<>())
                        .add(item.getValue()));
        joinMap.forEach((key, joinValue) -> joinValues.add(new JoinValue(key, joinValue)));
        Record record =
            new Record(
                value.stream().map(Record::getValue).collect(Collectors.toList()), joinValues);
        boolean valid = true;
        if (having != null) {
          variables.setValue(fromAliasIndex, record.getValue());
          record.setVariableValue(variables);
          valid = OperatorHandle.isTrue(apply(having, variables, EMPTY_PARAMETER));
        }
        if (valid) {
          records.add(record);
        }
      }
    }
    return records;
  }

  private List<Record> processWhere(List<Record> records, Object object) {
    if (where != null) {
      variables.setValue(fromAliasIndex, object);
      return records.stream()
          .peek(record -> record.setVariableValue(variables))
          .filter(record -> OperatorHandle.isTrue(apply(where, variables, EMPTY_PARAMETER)))
          .collect(Collectors.toList());
    }
    return records;
  }

  private Object apply(MagicScriptLambdaFunction function, Variables variables, Object[] args) {
    return function.apply(variables, args);
  }

  private boolean processOtherJoin(int index, Map<Integer, List<Object>> container) {
    if (index < joinSize) {
      LinQJoinValue join = joins.get(index);
      boolean matched = false;
      for (Object joinItem : join.getTarget()) {
        // 设置当前关联表所在行的数据
        variables.setValue(join.getAliasIndex(), joinItem);
        // 判断关联条件是否成立
        if (OperatorHandle.isTrue(apply(join.getCondition(), variables, EMPTY_PARAMETER))) {
          matched = true;
          if (processOtherJoin(index + 1, container)) {
            // join关联不上时直接跳过当前行
            continue;
          }
          // 关联上时，把当前结果存放至容器中
          container.get(join.getAliasIndex()).add(joinItem);
        }
      }
      variables.setValue(join.getAliasIndex(), Collections.emptyMap());
      if (!matched) {
        if (!join.isLeftJoin()) {
          return true;
        }
        return processOtherJoin(index + 1, container);
      }
    }
    return false;
  }

  private List<Record> processJoin(Object object) {
    // 当没有join语句时，直接返回该行数据
    if (joinSize == 0) {
      return Collections.singletonList(new Record(object));
    }
    // 定义存放关联结果集的容器并初始化
    Map<Integer, List<Object>> map = new HashMap<>(joinSize);
    joins.forEach(join -> map.put(join.getAliasIndex(), new ArrayList<>()));
    if (processOtherJoin(0, map)) {
      return Collections.emptyList();
    }

    // 计算当前行关联到的行数
    int maxSize = map.values().stream().mapToInt(Collection::size).max().getAsInt();
    // 全没关联上，且没有 join
    if (maxSize == 0) {
      return Collections.singletonList(
          new Record(
              object,
              map.keySet().stream()
                  .map(objects -> new JoinValue(objects, Collections.emptyMap()))
                  .collect(Collectors.toList())));
    }
    List<Record> records = new ArrayList<>(maxSize);
    Set<Map.Entry<Integer, List<Object>>> entries = map.entrySet();
    for (int i = 0; i < maxSize; i++) {
      List<JoinValue> joinValues = new ArrayList<>(entries.size());
      for (Map.Entry<Integer, List<Object>> entry : entries) {
        List<Object> item = entry.getValue();
        int size = item.size();
        if (size > 0) {
          if (size > i) {
            joinValues.add(new JoinValue(entry.getKey(), item.get(i)));
          } else {
            joinValues.add(new JoinValue(entry.getKey(), Collections.emptyMap()));
          }
        }
      }
      records.add(new Record(object, joinValues));
    }
    return records;
  }
}
