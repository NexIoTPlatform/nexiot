package cn.universal.core.engine.parsing;

public class RegexpToken extends Token {

  private final int flag;

  public RegexpToken(TokenType type, Span span, int regFlag) {
    super(type, span);
    this.flag = regFlag;
  }

  public int getFlag() {
    return flag;
  }
}
