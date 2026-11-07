package cn.universal.core.engine.functions.linq;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import cn.universal.core.engine.functions.NumberExtension;

public class MathFunctions {

  @Comment("四舍五入保留N为小数")
  @Function
  public double round(
      @Comment(name = "target", value = "目标值") Number target,
      @Comment(name = "len", value = "保留的小数位数") int len) {
    return NumberExtension.round(target, len);
  }

  @Comment("四舍五入保留N为小数")
  @Function
  public double round(@Comment(name = "target", value = "目标值") Number target) {
    return NumberExtension.round(target, 0);
  }

  @Comment("向上取整")
  @Function
  public Number ceil(@Comment(name = "target", value = "目标值") Number target) {
    return NumberExtension.ceil(target);
  }

  @Comment("向下取整")
  @Function
  public Number floor(@Comment(name = "target", value = "目标值") Number target) {
    return NumberExtension.floor(target);
  }

  @Comment("求百分比")
  @Function
  public String percent(
      @Comment(name = "target", value = "目标值") Number target,
      @Comment(name = "len", value = "保留的小数位数") int len) {
    return NumberExtension.asPercent(target, len);
  }

  @Comment("求百分比")
  @Function
  public String percent(@Comment(name = "target", value = "目标值") Number target) {
    return NumberExtension.asPercent(target, 0);
  }
}
