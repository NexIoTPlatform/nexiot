package cn.universal.core.engine.parsing;

public class Token {

  private final TokenType type;

  private final Span span;
  private final Object value;
  private TokenStream tokenStream;

  public Token(TokenType type, Span span) {
    this.type = type;
    this.span = span;
    this.value = null;
  }

  public Token(TokenType type, Span span, Object value) {
    this.type = type;
    this.span = span;
    this.value = value;
  }

  public Token(TokenType type, Span span, TokenStream tokenStream) {
    this.type = type;
    this.span = span;
    this.tokenStream = tokenStream;
    this.value = null;
  }

  public TokenType getType() {
    return type;
  }

  public Span getSpan() {
    return span;
  }

  public String getText() {
    return span.getText();
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "Token [type=" + type + ", span=" + span + "]";
  }

  public TokenStream getTokenStream() {
    return tokenStream;
  }
}
