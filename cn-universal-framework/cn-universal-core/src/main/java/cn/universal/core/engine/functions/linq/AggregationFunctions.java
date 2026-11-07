package cn.universal.core.engine.functions.linq;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import cn.universal.core.engine.functions.ObjectConvertExtension;
import cn.universal.core.engine.functions.StreamExtension;
import cn.universal.core.engine.parsing.ast.BinaryOperation;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.BinaryOperator;

/** 聚合函数 */
public class AggregationFunctions {

  @Function
  @Comment("聚合函数-count")
  public int count(@Comment(name = "value", value = "集合") Object target) {
    if (target == null) {
      return 0;
    } else if (target instanceof Map) {
      return 1;
    }
    try {
      return StreamExtension.arrayLikeToList(target).size();
    } catch (Exception e) {
      return 1;
    }
  }

  @Function
  @Comment("聚合函数-max")
  public Object max(@Comment(name = "value", value = "集合") Object target) {
    if (target == null) {
      return null;
    } else if (target instanceof Map) {
      return target;
    }
    try {
      return StreamExtension.arrayLikeToList(target).stream()
          .reduce(BinaryOperator.maxBy(BinaryOperation::compare))
          .orElse(null);
    } catch (Exception e) {
      return null;
    }
  }

  @Function
  @Comment("聚合函数-sum")
  public Number sum(@Comment(name = "value", value = "集合") Object target) {
    if (target == null) {
      return null;
    } else if (target instanceof Map) {
      return null;
    }
    try {
      OptionalDouble value =
          StreamExtension.arrayLikeToList(target).stream()
              .mapToDouble(v -> ObjectConvertExtension.asDouble(v, Double.NaN))
              .filter(v -> !Double.isNaN(v))
              .reduce(Double::sum);
      return value.isPresent() ? value.getAsDouble() : null;
    } catch (Exception e) {
      return null;
    }
  }

  @Function
  @Comment("聚合函数-min")
  public Object min(@Comment(name = "value", value = "集合") Object target) {
    if (target == null) {
      return null;
    } else if (target instanceof Map) {
      return target;
    }
    try {
      return StreamExtension.arrayLikeToList(target).stream()
          .reduce(BinaryOperator.minBy(BinaryOperation::compare))
          .orElse(null);
    } catch (Exception e) {
      return null;
    }
  }

  @Function
  @Comment("聚合函数-avg")
  public Object avg(@Comment(name = "value", value = "集合") Object target) {
    if (target == null) {
      return null;
    } else if (target instanceof Map) {
      return target;
    }
    try {
      OptionalDouble average =
          StreamExtension.arrayLikeToList(target).stream()
              .mapToDouble(v -> ObjectConvertExtension.asDouble(v, Double.NaN))
              .filter(v -> !Double.isNaN(v))
              .average();
      return average.isPresent() ? average.getAsDouble() : null;
    } catch (Exception e) {
      return null;
    }
  }

  @Function
  @Comment("分组后按指定字符串拼接")
  public String group_concat(
      @Comment(name = "target", value = "列，如t.a") Object target,
      @Comment(name = "separator", value = "分隔符，如`|`") String separator) {
    if (target == null) {
      return null;
    }
    return StreamExtension.join(target, separator);
  }

  @Function
  @Comment("分组后使用`,`拼接")
  public String group_concat(@Comment(name = "target", value = "列，如t.a") Object target) {
    return group_concat(target, ",");
  }
}
