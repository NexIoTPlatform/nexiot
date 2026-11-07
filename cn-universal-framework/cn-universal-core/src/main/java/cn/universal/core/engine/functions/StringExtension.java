package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** String 扩展 */
public class StringExtension {

  @Comment("校验文本是否符合正则")
  public boolean match(String source, @Comment(name = "pattern", value = "正则表达式") Pattern pattern) {
    return pattern.matcher(source).find();
  }

  @Comment("正则匹配所有")
  public List<String> matcher(
      String source, @Comment(name = "pattern", value = "正则表达式") String regex) {
    // 创建 Pattern 对象
    // 现在创建 matcher 对象
    List<String> list = new ArrayList<>();
    Pattern pattern = Pattern.compile(regex);
    Matcher m = pattern.matcher(source);
    while (m.find()) {
      list.add(m.group().substring(1, m.group().length() - 1));
    }
    return list;
  }

  @Comment("正则替换字符串")
  public String replace(
      String source,
      @Comment(name = "pattern", value = "正则表达式") Pattern pattern,
      @Comment(name = "replacement", value = "替换字符串") String replacement) {
    return pattern.matcher(source).replaceAll(replacement);
  }
}
