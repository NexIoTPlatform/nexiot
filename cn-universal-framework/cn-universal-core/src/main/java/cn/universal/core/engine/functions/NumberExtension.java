package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Number类型扩展 */
public class NumberExtension {

  @Comment("四舍五入保留N位小数")
  public static double round(Number number, @Comment(name = "num", value = "规定小数的位数") int num) {
    return new BigDecimal("" + number.doubleValue())
        .setScale(num, RoundingMode.HALF_UP)
        .doubleValue();
  }

  @Comment("向下取整")
  public static Number floor(Number number) {
    if (number instanceof Double || number instanceof Float) {
      return fixed(Math.floor(number.floatValue()));
    } else if (number instanceof BigDecimal) {
      return ((BigDecimal) number).setScale(0, RoundingMode.FLOOR);
    }
    return number;
  }

  @Comment("向上取整")
  public static Number ceil(Number number) {
    if (number instanceof Double || number instanceof Float) {
      return fixed(Math.ceil(number.doubleValue()));
    } else if (number instanceof BigDecimal) {
      return ((BigDecimal) number).setScale(0, RoundingMode.UP);
    }
    return number;
  }

  @Comment("转为百分比")
  public static String asPercent(Number number, @Comment(name = "num", value = "规定小数的位数") int num) {
    return new BigDecimal(number.doubleValue() * 100).setScale(num, RoundingMode.HALF_UP).toString()
        + "%";
  }

  private static Number fixed(double value) {
    if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
      return (long) value;
    }
    return value;
  }

  @Comment("四舍五入保留N位小数,仿JS的toFixed")
  public String toFixed(Number number, @Comment(name = "num", value = "规定小数的位数") int num) {
    return new BigDecimal("" + number.doubleValue()).setScale(num, RoundingMode.HALF_UP).toString();
  }
}
