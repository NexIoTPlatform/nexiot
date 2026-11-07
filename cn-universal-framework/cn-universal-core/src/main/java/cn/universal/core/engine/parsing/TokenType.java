package cn.universal.core.engine.parsing;

import java.util.Arrays;

/** Token类型 */
public enum TokenType {
  // @off
  Spread("...", "..."),
  Period(".", "."),
  QuestionPeriod("?.", "?."),
  Comma(",", ","),
  Semicolon(";", ";"),
  Colon(":", ":"),
  Plus("+", "+"),
  Minus("-", "-"),
  Asterisk("*", "*"),
  ForwardSlash("/", "/"),
  PostSlash("\\", "\\"),
  Percentage("%", "%"),
  LeftParantheses("(", ")"),
  RightParantheses(")", ")"),
  LeftBracket("[", "["),
  RightBracket("]", "]"),
  LeftCurly("{", "{"),
  RightCurly("}", "{"),
  Less("<", "<"),
  Greater(">", ">"),
  LessEqual("<=", "<="),
  GreaterEqual(">=", ">="),
  Equal("==", "=="),
  NotEqual("!=", "!="),
  Assignment(true, "=", "="),
  // 1.3.0
  PlusPlus(true, "++", "++"),
  MinusMinus(true, "--", "--"),
  PlusEqual(true, "+=", "+="),
  MinusEqual(true, "-=", "-="),
  AsteriskEqual(true, "*=", "*="),
  ForwardSlashEqual(true, "/=", "/="),
  PercentEqual(true, "%=", "%="),
  // 1.3.0 end
  // 1.3.9
  ColonColon("::", "::"),
  EqualEqualEqual("===", "==="),
  NotEqualEqual("!==", "!=="),
  // 1.3.9 end
  And("&&", "&&"),
  Or("||", "||"),
  Xor("^", "^"),
  Not("!", "!"),
  // 1.5.0 start
  BitAnd("&", "&"),
  BitOr("|", "|"),
  BitNot("~", "~"),
  LShift("<<", "<<"),
  RShift(">>", ">>"),
  Rshift2(">>>", ">>>"),

  XorEqual(true, "^=", "^="),
  BitAndEqual(true, "&=", "&="),
  BitOrEqual(true, "|=", "|="),
  LShiftEqual(true, "<<=", "<<="),
  RShiftEqual(true, ">>=", ">>="),
  RShift2Equal(true, ">>>=", ">>>="),
  // 1.5.0 end
  SqlAnd("and", "and"),
  SqlOr("or", "or"),
  SqlNotEqual("<>", "<>", true),
  // 1.8.0 start
  InstanceOf("instanceof", "instanceof"),
  // 1.8.0 end

  QuestionMark("?", "?"),
  DoubleQuote("\"", "\""),
  TripleQuote("\"\"\"", "\"\"\""),
  SingleQuote("'", "'"),
  Lambda("=>"),
  RegexpLiteral("a regexp"),
  BooleanLiteral("true or false"),
  DoubleLiteral("a double floating point number"),
  DecimalLiteral("a decimal point number"),
  FloatLiteral("a floating point number"),
  LongLiteral("a long integer number"),
  IntegerLiteral("an integer number"),
  ShortLiteral("a short integer number"),
  ByteLiteral("a byte integer number"),
  CharacterLiteral("a character"),
  StringLiteral("a string"),
  NullLiteral("null"),
  Language("language"),
  Comment("comment"),
  Identifier("an identifier");
  // @on

  private static final TokenType[] VALUES;

  static {
    VALUES = TokenType.values();
    // 根据字符长度排序
    Arrays.sort(
        VALUES,
        (o1, o2) -> {
          if (o1.literal == null && o2.literal == null) {
            return 0;
          }
          if (o1.literal == null && o2.literal != null) {
            return 1;
          }
          if (o1.literal != null && o2.literal == null) {
            return -1;
          }
          return o2.literal.length() - o1.literal.length();
        });
  }

  private final String literal;

  private final String error;

  private final boolean inLinq;

  private final boolean modifiable;

  TokenType(String error) {
    this(null, error);
  }

  TokenType(String literal, String error) {
    this(literal, error, false);
  }

  TokenType(boolean modifiable, String literal, String error) {
    this(literal, error, false, modifiable);
  }

  TokenType(String literal, String error, boolean inLinq) {
    this(literal, error, inLinq, false);
  }

  TokenType(String literal, String error, boolean inLinq, boolean modifiable) {
    this.literal = literal;
    this.error = error;
    this.inLinq = inLinq;
    this.modifiable = modifiable;
  }

  public static TokenType[] getSortedValues() {
    return VALUES;
  }

  public boolean isInLinq() {
    return inLinq;
  }

  public boolean isModifiable() {
    return modifiable;
  }

  public String getLiteral() {
    return literal;
  }

  public String getError() {
    return error;
  }
}
