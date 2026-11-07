package cn.universal.core.engine.parsing;

public class LiteralToken extends Token {

  public LiteralToken(TokenType type, Span span) {
    super(type, span);
  }

  public LiteralToken(TokenType type, Span span, TokenStream tokenStream) {
    super(type, span, tokenStream);
  }

  public LiteralToken(TokenType type, Span span, Object value) {
    super(type, span, value);
  }
}
