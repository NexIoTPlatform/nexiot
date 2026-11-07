package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** 类型转换 */
public class ObjectConvertExtension {

  /**
   * 转int
   *
   * @param defaultValue 默认值
   */
  @Comment("将值转换为int类型")
  public static int asInt(
      Object val, @Comment(name = "defaultValue", value = "转换失败时的默认值") int defaultValue) {
    try {
      return asDecimal(val).intValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * 转double
   *
   * @param defaultValue 默认值
   */
  @Comment("将对象转为double类型")
  public static double asDouble(
      Object val, @Comment(name = "defaultValue", value = "转换失败时的默认值") double defaultValue) {
    try {
      return asDecimal(val).doubleValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * 转long
   *
   * @param defaultValue 默认值
   */
  @Comment("将对象转为long类型")
  public static long asLong(
      Object val, @Comment(name = "defaultValue", value = "转换失败时的默认值") long defaultValue) {
    try {
      return asDecimal(val).longValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /** 转String */
  @Comment("将对象转为String类型")
  public static String asString(Object val) {
    return asString(val, null);
  }

  /** 转Date */
  @Comment("将对象转为Date类型，默认字符串格式为yyyy-MM-dd HH:mm:ss")
  public static Date asDate(Object val) {
    return asDate(val, "yyyy-MM-dd HH:mm:ss");
  }

  /** 转BigDecimal */
  @Comment("将对象转为BigDecimal类型")
  public static BigDecimal asDecimal(Object val) {
    if (val instanceof BigDecimal) {
      return (BigDecimal) val;
    }
    return new BigDecimal(asString(val));
  }

  /** 转BigDecimal */
  @Comment("将对象转为BigDecimal类型")
  public static BigDecimal asDecimal(
      Object val, @Comment(name = "defaultVal", value = "转换失败时的默认值") BigDecimal defaultVal) {
    if (val instanceof BigDecimal) {
      return (BigDecimal) val;
    }
    try {
      return new BigDecimal(asString(val));
    } catch (Exception e) {
      return defaultVal;
    }
  }

  /** 转Date */
  @Comment("将对象转为Date类型,支持String、10位、13位时间戳")
  public static Date asDate(
      Object val, @Comment(name = "format", value = "日期格式，如yyyy-MM-dd HH:mm:ss") String format) {
    if (val == null) {
      return null;
    }
    if (val instanceof String) {
      try {
        return new SimpleDateFormat(format).parse(val.toString());
      } catch (ParseException e) {
        long longVal = asLong(val, -1);
        if (longVal > 0) {
          return asDate(longVal, format);
        }
      }
    } else if (val instanceof Date) {
      return (Date) val;
    } else if (val instanceof Number) {
      Number number = (Number) val;
      if (val.toString().length() == 10) { // 10位时间戳
        return new Date(number.longValue() * 1000L);
      } else if (val.toString().length() == 13) { // 13位时间戳
        return new Date(number.longValue());
      }
    }
    return null;
  }

  /**
   * 转String
   *
   * @param defaultValue 默认值
   */
  @Comment("将对象转为String类型")
  public static String asString(
      Object val, @Comment(name = "defaultValue", value = "转换失败时的默认值") String defaultValue) {
    return val == null ? defaultValue : val.toString();
  }

  /** 转int */
  @Comment("将值转换为int类型，转换失败时为0")
  public int asInt(Object val) {
    return asInt(val, 0);
  }

  /** 转double */
  @Comment("将对象转为double类型，转换失败时为0.0")
  public double asDouble(Object val) {
    return asDouble(val, 0.0);
  }

  /** 转long */
  @Comment("将对象转为long类型，转换失败时为0L")
  public long asLong(Object val) {
    return asLong(val, 0L);
  }

  /** 转byte */
  @Comment("将对象转为byte类型，转换失败时默认为0")
  public byte asByte(Object val) {
    return asByte(val, (byte) 0);
  }

  /**
   * 转byte
   *
   * @param defaultValue 默认值
   */
  @Comment("将对象转为byte类型")
  public byte asByte(
      Object val, @Comment(name = "defaultValue", value = "转换失败时的默认值") byte defaultValue) {
    try {
      return asDecimal(val).byteValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /** 转short */
  @Comment("将对象转为short类型，转换失败时默认为0")
  public short asShort(Object val) {
    return asShort(val, (short) 0);
  }

  /**
   * 转short
   *
   * @param defaultValue 默认值
   */
  @Comment("将对象转为short类型")
  public short asShort(
      Object val, @Comment(name = "defaultValue", value = "转换失败时的默认值") short defaultValue) {
    try {
      return asDecimal(val).shortValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /** 转float */
  @Comment("将对象转为float类型，转换失败默认为0.0f")
  public float asFloat(Object val) {
    return asFloat(val, 0.0f);
  }

  /**
   * 转float
   *
   * @param defaultValue 默认值
   */
  @Comment("将对象转为float类型")
  public float asFloat(
      Object val, @Comment(name = "defaultValue", value = "转换失败时的默认值") float defaultValue) {
    try {
      return asDecimal(val).floatValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }
}
