package cn.universal.core.engine.functions;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;

public class StringFunctions {

  @Function
  @Comment("判断字符串是否不是空")
  public boolean not_blank(@Comment(name = "str", value = "目标字符串") CharSequence cs) {
    return !is_blank(cs);
  }

  @Function
  @Comment("判断字符串是否不是空")
  public boolean is_blank(@Comment(name = "str", value = "目标字符串") CharSequence cs) {
    if (cs == null) {
      return true;
    }
    int strLen = cs.length();
    if (strLen == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
