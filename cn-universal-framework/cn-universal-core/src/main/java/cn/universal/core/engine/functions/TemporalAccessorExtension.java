package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class TemporalAccessorExtension {

  @Comment("日期格式化")
  public static String format(
      TemporalAccessor source,
      @Comment(name = "pattern", value = "格式，如yyyy-MM-dd") String pattern) {
    return DateTimeFormatter.ofPattern(pattern).format(source);
  }
}
