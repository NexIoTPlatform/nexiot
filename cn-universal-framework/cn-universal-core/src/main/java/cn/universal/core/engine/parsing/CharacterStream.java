package cn.universal.core.engine.parsing;

/** 对字符串进行封装，提供匹配、跳过方法，方便词法解析。 */
public class CharacterStream {

  private final String source;

  private final int end;

  private int index;

  private int spanStart = 0;

  public CharacterStream(String source) {
    this(source, 0, source.length());
  }

  public CharacterStream(String source, int start, int end) {
    if (start > end) {
      throw new IllegalArgumentException("Start must be <= end.");
    }
    if (start < 0) {
      throw new IndexOutOfBoundsException("Start must be >= 0.");
    }
    if (start > Math.max(0, source.length() - 1)) {
      throw new IndexOutOfBoundsException("Start outside of string.");
    }
    if (end > source.length()) {
      throw new IndexOutOfBoundsException("End outside of string.");
    }
    this.source = source;
    this.index = start;
    this.end = end;
  }

  /**
   * 截取字符串
   *
   * @param startIndex 开始位置
   * @param endIndex 结束位置
   */
  public String substring(int startIndex, int endIndex) {
    return this.source.substring(startIndex, endIndex);
  }

  /** 是否有下一个字符 */
  public boolean hasMore() {
    return index < end;
  }

  /** 根据开始位置、结束位置返回Span */
  public Span getSpan(int start, int end) {
    return new Span(this.source, start, end);
  }

  /** 返回下一个字符 */
  public char consume() {
    if (!hasMore()) {
      throw new RuntimeException("No more characters in stream.");
    }
    return source.charAt(index++);
  }

  /** 返回是否是以给定的字符串开头 */
  public boolean match(String needle, boolean consume) {
    int needleLength = needle.length();
    if (needleLength + index > end) {
      return false;
    }
    for (int i = 0, j = index; i < needleLength; i++, j++) {
      if (index >= end) {
        return false;
      }
      if (needle.charAt(i) != source.charAt(j)) {
        return false;
      }
    }
    if (consume) {
      index += needleLength;
    }
    return true;
  }

  /**
   * 匹配任意字符串
   *
   * @param strs 任意字符串
   * @return
   */
  public boolean matchAny(boolean consume, String... strs) {
    for (String str : strs) {
      if (match(str, consume)) {
        return true;
      }
    }
    return false;
  }

  /** 返回是否是数字 */
  public boolean matchDigit(boolean consume) {
    if (index >= end) {
      return false;
    }
    char c = source.charAt(index);
    if (Character.isDigit(c)) {
      if (consume) {
        index++;
      }
      return true;
    }
    return false;
  }

  /** 返回是否以标识符开头 */
  public boolean matchIdentifierStart(boolean consume) {
    if (index >= end) {
      return false;
    }
    char c = source.charAt(index);
    if (Character.isJavaIdentifierStart(c) || c == '@') {
      if (consume) {
        index++;
      }
      return true;
    }
    return false;
  }

  /**
   * 返回是否是标识符部分
   *
   * @param consume 是否消耗
   */
  public boolean matchIdentifierPart(boolean consume) {
    if (index >= end) {
      return false;
    }
    char c = source.charAt(index);
    if (Character.isJavaIdentifierPart(c)) {
      if (consume) {
        index++;
      }
      return true;
    }
    return false;
  }

  /** 跳过一行 */
  public void skipLine() {
    while (index < end) {
      if (source.charAt(index++) == '\n') {
        break;
      }
    }
  }

  /** 直到给定的字符串之前全部跳过 */
  public boolean skipUntil(String chars) {
    while (index < end) {
      boolean matched = true;
      for (int i = 0, len = chars.length(); i < len && index + i < end; i++) {
        if (chars.charAt(i) != source.charAt(index + i)) {
          matched = false;
          break;
        }
      }
      this.index += matched ? chars.length() : 1;
      if (matched) {
        return true;
      }
    }
    return false;
  }

  /** 跳过空白字符 */
  public void skipWhiteSpace() {
    while (index < end) {
      char c = source.charAt(index);
      if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
        index++;
      } else {
        break;
      }
    }
  }

  /** 记录当前位置为Span的开始位置 */
  public void startSpan() {
    spanStart = index;
  }

  /** 根据当前位置返回Span */
  public Span endSpan() {
    return new Span(source, spanStart, index);
  }

  /** 根据当前位置 - offset 返回 Span */
  public Span endSpan(int offset) {
    return new Span(source, spanStart, index + offset);
  }

  public Span endSpan(int start, int end) {
    return new Span(source, start, end);
  }

  /** 返回当前位置 */
  public int getPosition() {
    return index;
  }

  /** 重置当前位置 */
  public void reset(int position) {
    index = position;
  }
}
