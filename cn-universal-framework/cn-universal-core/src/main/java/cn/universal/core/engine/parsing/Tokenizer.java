package cn.universal.core.engine.parsing;

import cn.universal.core.engine.MagicScriptError;
import cn.universal.core.engine.exception.StringLiteralException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Tokenizer {

  public static TokenStream tokenize(String source) {
    return tokenize(source, false);
  }

  public static TokenStream tokenize(String source, boolean matchComment) {
    CharacterStream stream = new CharacterStream(source, 0, source.length());
    List<Token> tokens = new ArrayList<>();
    tokenizer(stream, tokens, matchComment, null);
    return new TokenStream(tokens);
  }

  private static List<Token> tokenizer(
      CharacterStream stream, List<Token> tokens, boolean matchComment, String except) {
    int leftCount = 0;
    int rightCount = 0;
    outer:
    while (stream.hasMore()) {
      stream.skipWhiteSpace();
      stream.startSpan();
      if (except != null && stream.match(except, true)) {
        return tokens;
      }
      // // /* */
      if (tokenizerComment(stream, tokens, matchComment)) {
        continue;
      }
      // int double long float byte decimal
      if (tokenizerNumber(stream, tokens)) {
        continue;
      }
      // '' """ """ ""
      if (tokenizerString(stream, TokenType.SingleQuote, tokens)
          || tokenizerString(stream, TokenType.TripleQuote, tokens)
          || tokenizerString(stream, TokenType.DoubleQuote, tokens)) {
        continue;
      }

      // regexp
      if (regexpToken(stream, tokens)) {
        continue;
      }
      // ``` ```
      if (tokenizerLanguage(stream, tokens)) {
        continue;
      }
      // template string
      if (tokenizerTemplateString(stream, tokens, matchComment)) {
        continue;
      }
      // Identifier, keyword, boolean literal, or null literal
      if (tokenizerIdentifier(stream, tokens)) {
        continue;
      }
      // lambda
      if (stream.match("=>", true) || stream.match("->", true)) {
        tokens.add(
            new Token(
                TokenType.Lambda, stream.getSpan(stream.getPosition() - 2, stream.getPosition())));
        continue;
      }
      // Simple tokens
      for (TokenType t : TokenType.getSortedValues()) {
        if (t.getLiteral() != null) {
          if (stream.match(t.getLiteral(), true)) {
            if (t == TokenType.LeftCurly) {
              leftCount++;
            }
            tokens.add(
                new Token(
                    t,
                    stream.getSpan(
                        stream.getPosition() - t.getLiteral().length(), stream.getPosition())));
            continue outer;
          }
        }
      }
      if (leftCount != rightCount && stream.match("}", true)) {
        rightCount++;
        tokens.add(
            new Token(
                TokenType.RightCurly,
                stream.getSpan(stream.getPosition() - 1, stream.getPosition())));
        continue outer;
      }
      if (stream.hasMore()) {
        MagicScriptError.error(
            "Unknown token", stream.getSpan(stream.getPosition(), stream.getPosition() + 1));
      }
    }
    return tokens;
  }

  private static boolean tokenizerLanguage(CharacterStream stream, List<Token> tokens) {
    // TODO exception
    if (stream.match("```", true)) {
      stream.startSpan();
      if (stream.matchIdentifierStart(true)) {
        while (stream.matchIdentifierPart(true)) {}
        Span language = stream.endSpan();
        tokens.add(new Token(TokenType.Language, language));
        stream.startSpan();
        if (!stream.skipUntil("```")) {
          MagicScriptError.error("```需要以```结尾", stream.endSpan(), new StringLiteralException());
        }
        tokens.add(new Token(TokenType.Language, stream.endSpan(-3)));
        return true;
      } else {
        MagicScriptError.error("```后需要标识语言类型", stream.endSpan(), new StringLiteralException());
      }
    }
    return false;
  }

  private static boolean tokenizerTemplateString(
      CharacterStream stream, List<Token> tokens, boolean matchComment) {
    if (stream.match("`", true)) {
      int begin = stream.getPosition();
      int start = begin;
      boolean matchedEndQuote = false;
      List<Token> subTokens = new ArrayList<>();
      while (stream.hasMore()) {
        if (stream.match("\\", true)) {
          stream.consume();
          continue;
        }
        if (stream.match("`", true)) {
          matchedEndQuote = true;
          break;
        }
        if (stream.match("${", true)) {
          int end = stream.getPosition();
          if (start < end - 2) {
            subTokens.add(
                new LiteralToken(TokenType.StringLiteral, stream.endSpan(start, end - 2)));
          }
          subTokens.addAll(tokenizer(stream, new ArrayList<>(), matchComment, "}"));
          start = stream.getPosition();
          continue;
        }
        stream.consume();
      }
      if (!matchedEndQuote) {
        MagicScriptError.error("模板字符串没有结束符`", stream.endSpan(), new StringLiteralException());
      }
      Span stringSpan = stream.endSpan(begin, stream.getPosition());
      int end = stream.getPosition() - 1;
      if (end - start > 0) {
        subTokens.add(new LiteralToken(TokenType.StringLiteral, stream.endSpan(start, end)));
      }
      stringSpan = stream.getSpan(stringSpan.getStart() - 1, stringSpan.getEnd());
      tokens.add(new LiteralToken(TokenType.StringLiteral, stringSpan, new TokenStream(subTokens)));
      return true;
    }
    return false;
  }

  private static boolean tokenizerIdentifier(CharacterStream stream, List<Token> tokens) {
    if (stream.matchIdentifierStart(true)) {
      stream.startSpan();
      while (stream.matchIdentifierPart(true)) {}
      Span identifierSpan = stream.endSpan();
      identifierSpan = stream.getSpan(identifierSpan.getStart() - 1, identifierSpan.getEnd());
      if ("true".equals(identifierSpan.getText()) || "false".equals(identifierSpan.getText())) {
        tokens.add(new LiteralToken(TokenType.BooleanLiteral, identifierSpan));
      } else if ("null".equals(identifierSpan.getText())) {
        tokens.add(new LiteralToken(TokenType.NullLiteral, identifierSpan));
      } else if ("instanceof".equals(identifierSpan.getText())) {
        tokens.add(new Token(TokenType.InstanceOf, identifierSpan));
      } else if (TokenType.SqlAnd.getLiteral().equalsIgnoreCase(identifierSpan.getText())) {
        tokens.add(new Token(TokenType.SqlAnd, identifierSpan));
      } else if (TokenType.SqlOr.getLiteral().equalsIgnoreCase(identifierSpan.getText())) {
        tokens.add(new Token(TokenType.SqlOr, identifierSpan));
      } else {
        tokens.add(new Token(TokenType.Identifier, identifierSpan));
      }
      return true;
    }
    return false;
  }

  private static boolean tokenizerComment(
      CharacterStream stream, List<Token> tokens, boolean matchComment) {
    if (stream.match("//", true)) { // 注释
      stream.skipLine();
      if (matchComment) {
        tokens.add(new Token(TokenType.Comment, stream.endSpan()));
      }
      return true;
    }
    stream.startSpan();
    if (stream.match("/*", true)) { // 多行注释
      stream.skipUntil("*/");
      if (matchComment) {
        tokens.add(new Token(TokenType.Comment, stream.endSpan()));
      }
      return true;
    }
    return false;
  }

  private static boolean tokenizerNumber(CharacterStream stream, List<Token> tokens) {
    if (stream.match("0", false)) {
      int index = stream.getPosition();
      stream.startSpan();
      stream.consume();
      if (stream.matchAny(true, "x", "X")) { // 0x 16进制
        while (stream.matchDigit(true)
            || stream.matchAny(
                true, "A", "B", "C", "D", "E", "F", "a", "b", "c", "d", "e", "f", "_")) {}
        if (stream.matchAny(true, "L", "l")) {
          Span span = stream.endSpan();
          String text = span.getText();
          tokens.add(
              new LiteralToken(
                  TokenType.LongLiteral,
                  span,
                  Long.parseLong(text.substring(2, text.length() - 1).replace("_", ""), 16)));
          return true;
        }
        tokens.add(autoNumberType(stream.endSpan(), 16));
        return true;
      } else if (stream.matchAny(true, "b", "B")) { // 二进制
        while (stream.matchAny(true, "0", "1", "_")) {}
        if (stream.matchAny(true, "L", "l")) {
          Span span = stream.endSpan();
          String text = span.getText();
          tokens.add(
              new LiteralToken(
                  TokenType.LongLiteral,
                  span,
                  Long.parseLong(text.substring(2, text.length() - 1).replace("_", ""), 2)));
          return true;
        }
        tokens.add(autoNumberType(stream.endSpan(), 2));
        return true;
      }
      stream.reset(index);
    }
    if (stream.matchDigit(false)) {
      TokenType type = TokenType.IntegerLiteral;
      stream.startSpan();
      while (stream.matchDigit(true) || stream.match("_", true)) {}
      if (stream.match(TokenType.Period.getLiteral(), true)) {
        type = TokenType.DoubleLiteral;
        while (stream.matchDigit(true) || stream.match("_", true)) {}
      }
      if (stream.matchAny(true, "b", "B")) {
        if (type == TokenType.DoubleLiteral) {
          MagicScriptError.error("Byte literal can not have a decimal point.", stream.endSpan());
        }
        type = TokenType.ByteLiteral;
      } else if (stream.matchAny(true, "s", "S")) {
        if (type == TokenType.DoubleLiteral) {
          MagicScriptError.error("Short literal can not have a decimal point.", stream.endSpan());
        }
        type = TokenType.ShortLiteral;
      } else if (stream.matchAny(true, "L", "l")) {
        if (type == TokenType.DoubleLiteral) {
          MagicScriptError.error("Long literal can not have a decimal point.", stream.endSpan());
        }
        type = TokenType.LongLiteral;
      } else if (stream.matchAny(true, "f", "F")) {
        type = TokenType.FloatLiteral;
      } else if (stream.matchAny(true, "d", "D")) {
        type = TokenType.DoubleLiteral;
      } else if (stream.matchAny(true, "m", "M")) {
        type = TokenType.DecimalLiteral;
      }
      Span numberSpan = stream.endSpan();
      tokens.add(new LiteralToken(type, numberSpan));
      return true;
    }
    return false;
  }

  private static LiteralToken autoNumberType(Span span, int radix) {
    long value = Long.parseLong(span.getText().substring(2).replace("_", ""), radix);
    if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
      return new LiteralToken(TokenType.LongLiteral, span, value);
    } else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
      return new LiteralToken(TokenType.IntegerLiteral, span, (int) value);
    } else {
      return new LiteralToken(TokenType.ByteLiteral, span, (byte) value);
    }
  }

  private static boolean tokenizerString(
      CharacterStream stream, TokenType tokenType, List<Token> tokens) {
    // String literal
    if (stream.match(tokenType.getLiteral(), true)) {
      stream.startSpan();
      boolean matchedEndQuote = false;
      while (stream.hasMore()) {
        // Note: escape sequences like \n are parsed in StringLiteral
        if (stream.match("\\", true)) {
          stream.consume();
          continue;
        }
        if (stream.match(tokenType.getLiteral(), true)) {
          matchedEndQuote = true;
          break;
        }
        char ch = stream.consume();
        if (tokenType != TokenType.TripleQuote && (ch == '\r' || ch == '\n')) {
          MagicScriptError.error(
              tokenType.getError() + tokenType.getError() + "定义的字符串不能换行",
              stream.endSpan(),
              new StringLiteralException());
        }
      }
      if (!matchedEndQuote) {
        MagicScriptError.error(
            "字符串没有结束符" + tokenType.getError(), stream.endSpan(), new StringLiteralException());
      }
      Span stringSpan = stream.endSpan();
      stringSpan =
          stream.getSpan(
              stringSpan.getStart(), stringSpan.getEnd() - tokenType.getLiteral().length());
      tokens.add(new LiteralToken(TokenType.StringLiteral, stringSpan));
      return true;
    }
    return false;
  }

  private static boolean regexpToken(CharacterStream stream, List<Token> tokens) {
    if (tokens.size() > 0) {
      Token token = tokens.get(tokens.size() - 1);
      if (token instanceof LiteralToken) {
        return false;
      }
      switch (token.getType()) {
        case Comma: // ,
        case Semicolon: // ;
        case Colon: // :
        case RightCurly: // }
        case LeftBracket: // [
        case LeftParantheses: // (
        case Assignment: // =
        case NotEqual: // !=
        case EqualEqualEqual: // ===
        case NotEqualEqual: // !==
        case Equal: // ==
        case And: // &&
        case Or: // ||
        case SqlAnd: // and
        case SqlOr: // or
        case SqlNotEqual: // <>
        case QuestionMark: // ?
        case Lambda: // => ->
        case Not: // !
          break;
        default:
          return false;
      }
    }
    if (stream.match("/", false)) {
      int mark = stream.getPosition();
      stream.consume();
      stream.startSpan();
      boolean matchedEndQuote = false;
      int deep = 0;
      int expFlag = 0;
      int maybeMissForwardSlash = 0;
      int maybeMissForwardSlashEnd = 0;
      while (stream.hasMore()) {
        // Note: escape sequences like \n are parsed in StringLiteral
        if (stream.match("\\", true)) {
          stream.consume();
          continue;
        }
        if (stream.match("[", false)) {
          deep++;
          maybeMissForwardSlash = stream.getPosition();
        } else if (deep > 0 && stream.match("]", false)) {
          deep--;
        } else if (stream.match(TokenType.ForwardSlash.getLiteral(), true)) {
          if (deep == 0) {
            if (stream.match("g", true)) {
              expFlag |= 1;
            }
            if (stream.match("i", true)) {
              expFlag |= Pattern.CASE_INSENSITIVE;
            }
            if (stream.match("m", true)) {
              expFlag |= Pattern.MULTILINE;
            }
            if (stream.match("s", true)) {
              expFlag |= Pattern.DOTALL;
            }
            if (stream.match("u", true)) {
              expFlag |= Pattern.UNICODE_CHARACTER_CLASS;
            }
            if (stream.match("y", true)) {
              expFlag |= 16;
            }
            matchedEndQuote = true;
            break;
          } else {
            maybeMissForwardSlashEnd = stream.getPosition();
          }
        }
        char ch = stream.consume();
        if (ch == '\r' || ch == '\n') {
          stream.reset(mark);
          return false;
        }
      }
      if (deep != 0) {
        MagicScriptError.error(
            "Missing ']'", stream.getSpan(maybeMissForwardSlash, maybeMissForwardSlashEnd - 1));
      }
      if (!matchedEndQuote) {
        stream.reset(mark);
        return false;
      }
      Span regexpSpan = stream.endSpan();
      regexpSpan = stream.getSpan(regexpSpan.getStart() - 1, regexpSpan.getEnd());
      tokens.add(new RegexpToken(TokenType.RegexpLiteral, regexpSpan, expFlag));
      return true;
    }
    return false;
  }
}
