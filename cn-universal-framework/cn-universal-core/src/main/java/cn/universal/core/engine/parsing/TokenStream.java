package cn.universal.core.engine.parsing;

import cn.universal.core.engine.MagicScriptError;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 对List<Token>进行封装，提供匹配相关方法，方便语法解析 */
public class TokenStream {

  private final List<Token> tokens;

  private final int end;

  private int index;

  public TokenStream(List<Token> tokens) {
    this.tokens = tokens;
    this.index = 0;
    this.end = tokens.size();
  }

  /** 当前是否可读 */
  public boolean hasMore() {
    return index < end;
  }

  /** 是否有下一个Token */
  public boolean hasNext() {
    return index + 1 < end;
  }

  /** 是否有前一个Token */
  public boolean hasPrev() {
    return index > 0;
  }

  /** 标记当前位置，和resetIndex搭配使用。 */
  public int makeIndex() {
    return index;
  }

  /** 重置当前位置，和makeIndex搭配使用 */
  public void resetIndex(int index) {
    this.index = index;
  }

  /** 无条件消耗掉当前Token */
  public Token consume() {
    if (!hasMore()) {
      throw new RuntimeException("流已经遍历完毕");
    }
    return tokens.get(index++);
  }

  /** 获取下一个Token并改变当前位置 */
  public Token next() {
    if (!hasMore()) {
      throw new RuntimeException("流已经遍历完毕");
    }
    return tokens.get(++index);
  }

  /** 获取前一个Token并改变当前位置 */
  public Token prev() {
    if (index == 0) {
      throw new RuntimeException("流已经遍历完毕");
    }
    return tokens.get(--index);
  }

  /** 获取前一个Token，不改变当前位置 */
  public Token getPrev() {
    if (index == 0) {
      throw new RuntimeException("流已经遍历完毕");
    }
    return tokens.get(index - 1);
  }

  /** 期待下一个Token是给定的类型中之一 */
  public Token expect(TokenType... types) {
    if (!match(true, types)) {
      Token token = index < tokens.size() ? tokens.get(index) : null;
      Span span = token != null ? token.getSpan() : null;
      if (span == null) {
        MagicScriptError.error(
            "期待 '"
                + Stream.of(types).map(TokenType::getError).collect(Collectors.joining("','"))
                + "', 但是流已经遍历完毕",
            this);
      } else {
        MagicScriptError.error(
            "期待 '"
                + Stream.of(types).map(TokenType::getError).collect(Collectors.joining("','"))
                + "', 获得 '"
                + token.getText()
                + "'",
            span);
      }
      return null; // 执行不到这里
    } else {
      return tokens.get(index - 1);
    }
  }

  /** 获取全部注释 */
  public List<Span> comments() {
    return tokens.stream()
        .filter(it -> it.getType() == TokenType.Comment)
        .map(Token::getSpan)
        .collect(Collectors.toList());
  }

  /** 期待下一个Token为指定类型 */
  public Token expect(TokenType type) {
    if (!match(type, true)) {
      Token token = index < tokens.size() ? tokens.get(index) : null;
      Span span = token != null ? token.getSpan() : null;
      if (span == null) {
        MagicScriptError.error("期待 '" + type.getError() + "', 但是流已经遍历完毕", this);
      } else {
        MagicScriptError.error("期待 '" + type.getError() + "', 获得 '" + token.getText() + "'", span);
      }
      return null; // 执行不到这里
    } else {
      return tokens.get(index - 1);
    }
  }

  /** 期待匹配字符串 */
  public Token expect(String text) {
    return expect(text, false);
  }

  /**
   * 期待匹配字符串
   *
   * @param ignoreCase 是否忽略大小写
   */
  public Token expect(String text, boolean ignoreCase) {
    boolean result = match(text, true, ignoreCase);
    if (!result) {
      Token token = index < tokens.size() ? tokens.get(index) : null;
      Span span = token != null ? token.getSpan() : null;
      if (span == null) {
        MagicScriptError.error("期待 '" + text + "', 但是流已经遍历完毕", this);
      } else {
        MagicScriptError.error("期待 '" + text + "', 获得 '" + token.getText() + "'", span);
      }
      return null; // never reached
    } else {
      return tokens.get(index - 1);
    }
  }

  /**
   * 匹配指定类型Token
   *
   * @param consume 匹配成功后是否改变当前位置
   */
  public boolean match(TokenType type, boolean consume) {
    if (index >= end) {
      return false;
    }
    if (tokens.get(index).getType() == type) {
      if (consume) {
        index++;
      }
      return true;
    }
    return false;
  }

  /**
   * 匹配指定类型Token
   *
   * @param consume 匹配成功后是否改变当前位置
   */
  public boolean match(List<String> texts, boolean consume) {
    return match(texts, consume, false);
  }

  /**
   * 匹配指定字符串
   *
   * @param consume 匹配成功后是否改变当前位置
   * @param ignoreCase 是否忽略大小写
   */
  public boolean match(List<String> texts, boolean consume, boolean ignoreCase) {
    for (String text : texts) {
      if (match(text, consume, ignoreCase)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 匹配指定字符串
   *
   * @param consume 匹配成功后是否改变当前位置
   * @param ignoreCase 是否忽略大小写
   */
  public boolean match(String text, boolean consume, boolean ignoreCase) {
    if (index >= end) {
      return false;
    }
    String matchText = tokens.get(index).getText();
    if (ignoreCase ? matchText.equalsIgnoreCase(text) : matchText.equals(text)) {
      if (consume) {
        index++;
      }
      return true;
    }
    return false;
  }

  /**
   * 匹配指定字符串
   *
   * @param consume 匹配成功后是否改变当前位置
   */
  public boolean match(String text, boolean consume) {
    return match(text, consume, false);
  }

  /**
   * 匹配指定Token
   *
   * @param consume 匹配成功后是否改变当前位置
   */
  public boolean match(boolean consume, TokenType... types) {
    for (TokenType type : types) {
      if (match(type, consume)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 匹配指定字符串
   *
   * @param consume 匹配成功后是否改变当前位置
   */
  public boolean match(boolean consume, String... tokenTexts) {
    return match(consume, false, tokenTexts);
  }

  /**
   * 匹配指定字符串
   *
   * @param consume 匹配成功后是否改变当前位置
   * @param ignoreCase 是否忽略大小写
   */
  public boolean match(boolean consume, boolean ignoreCase, String... tokenTexts) {
    for (String text : tokenTexts) {
      if (match(text, consume, ignoreCase)) {
        return true;
      }
    }
    return false;
  }
}
