package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import java.util.regex.Pattern;

/** Pattern 扩展 */
public class PatternExtension {

  @Comment("校验文本是否符合正则")
  public boolean test(Pattern pattern, @Comment(name = "source", value = "目标字符串") String source) {
    return source != null && pattern.matcher(source).find();
  }
}
