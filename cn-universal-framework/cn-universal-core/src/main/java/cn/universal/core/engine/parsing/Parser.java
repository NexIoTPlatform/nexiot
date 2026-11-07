package cn.universal.core.engine.parsing;

import static cn.universal.core.engine.parsing.TokenType.And;
import static cn.universal.core.engine.parsing.TokenType.Assignment;
import static cn.universal.core.engine.parsing.TokenType.Asterisk;
import static cn.universal.core.engine.parsing.TokenType.AsteriskEqual;
import static cn.universal.core.engine.parsing.TokenType.BitAnd;
import static cn.universal.core.engine.parsing.TokenType.BitAndEqual;
import static cn.universal.core.engine.parsing.TokenType.BitNot;
import static cn.universal.core.engine.parsing.TokenType.BitOr;
import static cn.universal.core.engine.parsing.TokenType.BitOrEqual;
import static cn.universal.core.engine.parsing.TokenType.BooleanLiteral;
import static cn.universal.core.engine.parsing.TokenType.ByteLiteral;
import static cn.universal.core.engine.parsing.TokenType.Colon;
import static cn.universal.core.engine.parsing.TokenType.ColonColon;
import static cn.universal.core.engine.parsing.TokenType.Comma;
import static cn.universal.core.engine.parsing.TokenType.DecimalLiteral;
import static cn.universal.core.engine.parsing.TokenType.DoubleLiteral;
import static cn.universal.core.engine.parsing.TokenType.Equal;
import static cn.universal.core.engine.parsing.TokenType.EqualEqualEqual;
import static cn.universal.core.engine.parsing.TokenType.FloatLiteral;
import static cn.universal.core.engine.parsing.TokenType.ForwardSlash;
import static cn.universal.core.engine.parsing.TokenType.ForwardSlashEqual;
import static cn.universal.core.engine.parsing.TokenType.Greater;
import static cn.universal.core.engine.parsing.TokenType.GreaterEqual;
import static cn.universal.core.engine.parsing.TokenType.Identifier;
import static cn.universal.core.engine.parsing.TokenType.InstanceOf;
import static cn.universal.core.engine.parsing.TokenType.IntegerLiteral;
import static cn.universal.core.engine.parsing.TokenType.LShift;
import static cn.universal.core.engine.parsing.TokenType.LShiftEqual;
import static cn.universal.core.engine.parsing.TokenType.Lambda;
import static cn.universal.core.engine.parsing.TokenType.Language;
import static cn.universal.core.engine.parsing.TokenType.LeftBracket;
import static cn.universal.core.engine.parsing.TokenType.LeftCurly;
import static cn.universal.core.engine.parsing.TokenType.LeftParantheses;
import static cn.universal.core.engine.parsing.TokenType.Less;
import static cn.universal.core.engine.parsing.TokenType.LessEqual;
import static cn.universal.core.engine.parsing.TokenType.LongLiteral;
import static cn.universal.core.engine.parsing.TokenType.Minus;
import static cn.universal.core.engine.parsing.TokenType.MinusEqual;
import static cn.universal.core.engine.parsing.TokenType.MinusMinus;
import static cn.universal.core.engine.parsing.TokenType.Not;
import static cn.universal.core.engine.parsing.TokenType.NotEqual;
import static cn.universal.core.engine.parsing.TokenType.NotEqualEqual;
import static cn.universal.core.engine.parsing.TokenType.NullLiteral;
import static cn.universal.core.engine.parsing.TokenType.Or;
import static cn.universal.core.engine.parsing.TokenType.PercentEqual;
import static cn.universal.core.engine.parsing.TokenType.Percentage;
import static cn.universal.core.engine.parsing.TokenType.Period;
import static cn.universal.core.engine.parsing.TokenType.Plus;
import static cn.universal.core.engine.parsing.TokenType.PlusEqual;
import static cn.universal.core.engine.parsing.TokenType.PlusPlus;
import static cn.universal.core.engine.parsing.TokenType.QuestionMark;
import static cn.universal.core.engine.parsing.TokenType.QuestionPeriod;
import static cn.universal.core.engine.parsing.TokenType.RShift;
import static cn.universal.core.engine.parsing.TokenType.RShift2Equal;
import static cn.universal.core.engine.parsing.TokenType.RShiftEqual;
import static cn.universal.core.engine.parsing.TokenType.RegexpLiteral;
import static cn.universal.core.engine.parsing.TokenType.RightBracket;
import static cn.universal.core.engine.parsing.TokenType.RightCurly;
import static cn.universal.core.engine.parsing.TokenType.RightParantheses;
import static cn.universal.core.engine.parsing.TokenType.Rshift2;
import static cn.universal.core.engine.parsing.TokenType.ShortLiteral;
import static cn.universal.core.engine.parsing.TokenType.Spread;
import static cn.universal.core.engine.parsing.TokenType.SqlAnd;
import static cn.universal.core.engine.parsing.TokenType.SqlNotEqual;
import static cn.universal.core.engine.parsing.TokenType.SqlOr;
import static cn.universal.core.engine.parsing.TokenType.StringLiteral;
import static cn.universal.core.engine.parsing.TokenType.Xor;
import static cn.universal.core.engine.parsing.TokenType.XorEqual;

import cn.universal.core.engine.MagicScriptError;
import cn.universal.core.engine.parsing.ast.BinaryOperation;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.LanguageExpression;
import cn.universal.core.engine.parsing.ast.Literal;
import cn.universal.core.engine.parsing.ast.Node;
import cn.universal.core.engine.parsing.ast.TernaryOperation;
import cn.universal.core.engine.parsing.ast.UnaryOperation;
import cn.universal.core.engine.parsing.ast.VariableSetter;
import cn.universal.core.engine.parsing.ast.binary.AssigmentOperation;
import cn.universal.core.engine.parsing.ast.linq.LinqExpression;
import cn.universal.core.engine.parsing.ast.linq.LinqField;
import cn.universal.core.engine.parsing.ast.linq.LinqJoin;
import cn.universal.core.engine.parsing.ast.linq.LinqOrder;
import cn.universal.core.engine.parsing.ast.linq.LinqSelect;
import cn.universal.core.engine.parsing.ast.linq.WholeLiteral;
import cn.universal.core.engine.parsing.ast.literal.BigDecimalLiteral;
import cn.universal.core.engine.parsing.ast.literal.BooleanLiteral;
import cn.universal.core.engine.parsing.ast.literal.ByteLiteral;
import cn.universal.core.engine.parsing.ast.literal.DoubleLiteral;
import cn.universal.core.engine.parsing.ast.literal.FloatLiteral;
import cn.universal.core.engine.parsing.ast.literal.IntegerLiteral;
import cn.universal.core.engine.parsing.ast.literal.ListLiteral;
import cn.universal.core.engine.parsing.ast.literal.LongLiteral;
import cn.universal.core.engine.parsing.ast.literal.MapLiteral;
import cn.universal.core.engine.parsing.ast.literal.NullLiteral;
import cn.universal.core.engine.parsing.ast.literal.RegexpLiteral;
import cn.universal.core.engine.parsing.ast.literal.ShortLiteral;
import cn.universal.core.engine.parsing.ast.literal.StringLiteral;
import cn.universal.core.engine.parsing.ast.statement.Assert;
import cn.universal.core.engine.parsing.ast.statement.AsyncCall;
import cn.universal.core.engine.parsing.ast.statement.Break;
import cn.universal.core.engine.parsing.ast.statement.ClassicForStatement;
import cn.universal.core.engine.parsing.ast.statement.ClassConverter;
import cn.universal.core.engine.parsing.ast.statement.Continue;
import cn.universal.core.engine.parsing.ast.statement.Exit;
import cn.universal.core.engine.parsing.ast.statement.ForStatement;
import cn.universal.core.engine.parsing.ast.statement.FunctionCall;
import cn.universal.core.engine.parsing.ast.statement.IfStatement;
import cn.universal.core.engine.parsing.ast.statement.Import;
import cn.universal.core.engine.parsing.ast.statement.LambdaFunction;
import cn.universal.core.engine.parsing.ast.statement.MapOrArrayAccess;
import cn.universal.core.engine.parsing.ast.statement.MemberAccess;
import cn.universal.core.engine.parsing.ast.statement.MethodCall;
import cn.universal.core.engine.parsing.ast.statement.NewStatement;
import cn.universal.core.engine.parsing.ast.statement.Return;
import cn.universal.core.engine.parsing.ast.statement.Spread;
import cn.universal.core.engine.parsing.ast.statement.SwitchStatement;
import cn.universal.core.engine.parsing.ast.statement.Throw;
import cn.universal.core.engine.parsing.ast.statement.TryStatement;
import cn.universal.core.engine.parsing.ast.statement.VariableAccess;
import cn.universal.core.engine.parsing.ast.statement.VariableDefine;
import cn.universal.core.engine.parsing.ast.statement.VariableDestructuringDefine;
import cn.universal.core.engine.parsing.ast.statement.WhileStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 语法解析器 */
public class Parser {

  public static final String ANONYMOUS_VARIABLE = "-anonymous";
  private static final TokenType[][] BINARY_OPERATOR_PRECEDENCE =
      new TokenType[][] {
        new TokenType[] {Assignment},
        new TokenType[] {
          RShift2Equal,
          RShiftEqual,
          LShiftEqual,
          XorEqual,
          BitOrEqual,
          BitAndEqual,
          PercentEqual,
          ForwardSlashEqual,
          AsteriskEqual,
          MinusEqual,
          PlusEqual
        },
        new TokenType[] {Or, SqlOr},
        new TokenType[] {And, SqlAnd},
        new TokenType[] {BitOr},
        new TokenType[] {Xor},
        new TokenType[] {BitAnd},
        new TokenType[] {EqualEqualEqual, Equal, NotEqualEqual, NotEqual, SqlNotEqual},
        new TokenType[] {Less, LessEqual, Greater, GreaterEqual, InstanceOf},
        new TokenType[] {Plus, Minus},
        new TokenType[] {LShift, RShift, Rshift2},
        new TokenType[] {Asterisk, ForwardSlash, Percentage}
      };
  private static final TokenType[][] LINQ_BINARY_OPERATOR_PRECEDENCE =
      new TokenType[][] {
        new TokenType[] {
          RShift2Equal,
          RShiftEqual,
          LShiftEqual,
          XorEqual,
          BitOrEqual,
          BitAndEqual,
          PercentEqual,
          ForwardSlashEqual,
          AsteriskEqual,
          MinusEqual,
          PlusEqual
        },
        new TokenType[] {Or, SqlOr},
        new TokenType[] {And, SqlAnd},
        new TokenType[] {BitOr},
        new TokenType[] {Xor},
        new TokenType[] {BitAnd},
        new TokenType[] {Assignment, EqualEqualEqual, Equal, NotEqualEqual, NotEqual, SqlNotEqual},
        new TokenType[] {Less, LessEqual, Greater, GreaterEqual, InstanceOf},
        new TokenType[] {Plus, Minus},
        new TokenType[] {LShift, RShift, Rshift2},
        new TokenType[] {ForwardSlash, Asterisk, Percentage}
      };
  private static final TokenType[] UNARY_OPERATORS =
      new TokenType[] {
        MinusMinus, PlusPlus, BitNot,
        Minus, Plus, Not
      };
  private static final List<String> KEYWORDS =
      Arrays.asList(
          "import",
          "as",
          "var",
          "let",
          "const",
          "return",
          "break",
          "continue",
          "if",
          "for",
          "in",
          "new",
          "true",
          "false",
          "null",
          "else",
          "try",
          "catch",
          "finally",
          "async",
          "while",
          "exit",
          "and",
          "or",
          "throw" /*, "assert"*/);
  private static final List<String> LINQ_KEYWORDS =
      Arrays.asList(
          "from", "join", "left", "group", "by", "as", "having", "and", "or", "in", "where", "on",
          "limit", "offset");
  private VarScope varNames = new VarScope();
  private final VarScope rootvarNames = varNames;
  private final List<Span> spans = new ArrayList<>();
  private final Set<VarIndex> varIndices = new LinkedHashSet<>();
  private int varCount = 0;
  private int linqLevel = 0;
  private boolean requiredNew = true;
  private TokenStream stream;
  private final List<String> defines = new ArrayList<>();

  public Set<VarIndex> getVarIndices() {
    return varIndices;
  }

  public List<Node> parse(String source) {
    List<Node> nodes = new ArrayList<>();
    push();
    stream = Tokenizer.tokenize(source);
    while (stream.hasMore()) {
      Node node = parseStatement();
      if (node != null) {
        validateNode(node);
        nodes.add(node);
      }
    }
    pop();
    return nodes;
  }

  private void validateNode(Node node) {
    if (node instanceof Literal) {
      MagicScriptError.error("literal cannot be used alone", node.getSpan());
    }
  }

  private Node parseStatement() {
    return parseStatement(false);
  }

  private Node parseStatement(boolean expectRightCurly) {
    Node result = null;
    if (stream.match("import", false)) {
      result = parseImport();
    } else if (matchVarDefine()) {
      result = parseVarDefine();
    } else if (stream.match("if", false)) {
      result = parseIfStatement();
    } else if (stream.match("return", false)) {
      result = parseReturn();
    } else if (stream.match("for", false)) {
      result = parseForStatement();
    } else if (isSwitchStatement()) {
      result = parseSwitchStatement();
    } else if (stream.match("while", false)) {
      result = parseWhileStatement();
    } else if (stream.match("continue", false)) {
      result = new Continue(stream.consume().getSpan());
    } else if (stream.match("async", false)) {
      result = parseAsync();
    } else if (stream.match("try", false)) {
      result = parseTryStatement();
    } else if (stream.match("break", false)) {
      result = new Break(stream.consume().getSpan());
    } else if (stream.match("exit", false)) {
      result = parseExit();
    } else if (stream.match("assert", false)) {
      result = parseAssert();
    } else if (stream.match("throw", false)) {
      result = parseThrow();
    } else {
      int index = stream.makeIndex();
      if (matchTypeDefine()) {
        stream.resetIndex(index);
        result = parseVarDefine();
      }
      if (result == null) {
        stream.resetIndex(index);
        result = parseExpression(expectRightCurly);
      }
    }
    // consume semi-colons as statement delimiters
    while (stream.match(";", true)) {}
    return result;
  }

  private boolean matchTypeDefine() {
    boolean typeDefine = stream.match(Identifier, true);
    if (!typeDefine) {
      return false;
    }
    int index = stream.makeIndex();
    try {
      if ("new".equals(stream.getPrev().getText())) {
        return false;
      }
      if (stream.match(Identifier, false)) {
        return true;
      }
      int end = stream.getPrev().getSpan().getEnd();
      if (stream.hasMore() && stream.consume().getSpan().getStart() == end) {
        return false;
      }
    } finally {
      stream.resetIndex(index);
    }
    // destructuring support
    boolean isMapAccess;
    if ((isMapAccess = stream.match(true, LeftCurly)) || stream.match(true, LeftBracket)) {
      do {
        if (!stream.match(true, Identifier)) {
          return false;
        }
      } while (stream.match(true, Comma));
      if (isMapAccess) {
        return stream.match(true, RightCurly);
      } else {
        return stream.match(true, RightBracket);
      }
    }
    return false;
  }

  private boolean matchVarDefine() {
    return stream.match(false, "var", "let", "const");
  }

  private VarIndex add(String name) {
    VarScope varIndices = varNames;
    do {
      for (int j = varIndices.size() - 1; j >= 0; j--) {
        VarIndex varIndex = varIndices.get(j);
        if (varIndex.getName().equals(name)) {
          return defines.contains(name) ? varIndex.scoped() : varIndex;
        }
      }
    } while ((varIndices = varIndices.getParent()) != null);
    return add(new VarIndex(name, varCount++, true), true);
  }

  private VarIndex add(VarIndex varIndex) {
    return add(varIndex, false);
  }

  private VarIndex add(VarIndex varIndex, boolean isRoot) {
    if (defines.contains(varIndex.getName())) {
      varIndex = varIndex.scoped();
    }
    if (isRoot) {
      rootvarNames.add(varIndex);
    } else {
      varNames.add(varIndex);
    }
    varIndices.add(varIndex);
    return varIndex;
  }

  private VarIndex forceAdd(String name) {
    return forceAdd(name, false);
  }

  private VarIndex forceAdd(String name, boolean isConst) {
    return add(new VarIndex(name, varCount++, false, isConst));
  }

  private void push() {
    varNames = varNames.push();
  }

  private void pop() {
    varNames = varNames.pop();
  }

  private Span addSpan(Span opening, Span ending) {
    addSpan(opening);
    addSpan(ending);
    return addSpan(new Span(opening, ending));
  }

  private Span addSpan(String source, int start, int end) {
    return addSpan(new Span(source, start, end));
  }

  private Span addSpan(Span span) {
    this.spans.add(span);
    return span;
  }

  private Node parseExit() {
    Span opening = stream.expect("exit").getSpan();
    List<Expression> expressionList = new ArrayList<>();
    do {
      expressionList.add(parseExpression());
    } while (stream.match(Comma, true));
    return new Exit(addSpan(opening, stream.getPrev().getSpan()), expressionList);
  }

  private Node parseThrow() {
    Span opening = stream.consume().getSpan();
    Expression expression = parseExpression();
    return new Throw(addSpan(opening, stream.getPrev().getSpan()), expression);
  }

  private Node parseAssert() {
    int index = stream.makeIndex();
    try {
      Span opening = stream.expect("assert").getSpan();
      Expression condition = parseExpression();
      stream.expect(Colon);
      List<Expression> expressionList = new ArrayList<>();
      do {
        expressionList.add(parseExpression());
      } while (stream.match(Comma, true));
      return new Assert(addSpan(opening, stream.getPrev().getSpan()), condition, expressionList);
    } catch (Exception e) {
      stream.resetIndex(index);
      return parseExpression();
    }
  }

  private Expression parseAsync() {
    Span opening = stream.expect("async").getSpan();
    requiredNew = false;
    Expression expression = parseExpression();
    requiredNew = true;
    if (expression instanceof MethodCall
        || expression instanceof FunctionCall
        || expression instanceof LambdaFunction) {
      return new AsyncCall(addSpan(opening, stream.getPrev().getSpan()), expression);
    }
    MagicScriptError.error(
        "Expected MethodCall or FunctionCall or LambdaFunction", stream.getPrev().getSpan());
    return null;
  }

  private Import parseImport() {
    Span opening = stream.expect("import").getSpan();
    if (stream.hasMore()) {
      Token expected = stream.consume();
      String packageName = null;
      boolean isStringLiteral = expected.getType() == StringLiteral;
      if (isStringLiteral) {
        packageName = createStringLiteral(expected).getValue();
      } else if (expected.getType() == Identifier) {
        Span startSpan = expected.getSpan();
        packageName = startSpan.getText();
        while (stream.match(true, Period)) {
          isStringLiteral = true;
          if (stream.match(false, Asterisk)) {
            expected = stream.consume();
            break;
          }
          expected = stream.expect(Identifier);
        }
        if (isStringLiteral) {
          packageName = new Span(startSpan, expected.getSpan()).getText();
        }
      } else {
        MagicScriptError.error(
            "Expected identifier or string, but got stream is " + expected.getType().getError(),
            stream.getPrev().getSpan());
      }
      String varName = packageName;
      if (isStringLiteral) {
        if (stream.match("as", true)) {
          expected = stream.expect(Identifier);
          checkKeyword(expected.getSpan());
          varName = expected.getSpan().getText();
        } else {
          String temp = packageName;
          if (!temp.startsWith("@")) {
            int index = temp.lastIndexOf(".");
            if (index != -1) {
              temp = temp.substring(index + 1);
            }
          } else {
            MagicScriptError.error("Expected as", stream);
          }
          varName = temp;
        }
      }
      // 发现是危险包，则删除
      //      if (SafeRule.filter(packageName)) {
      //        MagicScriptError.error("只允许导入[cn.hutool]的部分工具包,请查阅(https://hutool.cn/)",
      //            stream.getPrev().getSpan());
      //      }
      return new Import(
          addSpan(opening, expected.getSpan()), packageName, forceAdd(varName), !isStringLiteral);
    }
    MagicScriptError.error(
        "Expected identifier or string, but got stream is EOF", stream.getPrev().getSpan());
    return null;
  }

  private TryStatement parseTryStatement() {
    Token opening = stream.expect("try");
    push();
    List<VariableDefine> tryResources = new ArrayList<>();
    if (stream.match("(", true)) {
      if (stream.match(")", false)) {
        // 空的 try-with-resource
      } else {
        while (!stream.match(")", false)) {
          if (stream.match(";", true)) {
            continue;
          }
          VariableDefine result = null;
          if (matchVarDefine()) {
            result = parseVarDefine();
          } else {
            if (stream.match(false, KEYWORDS.toArray(new String[0]))) {
              MagicScriptError.error("try 括号中只允许写赋值语句", stream.consume().getSpan());
            }
            int index = stream.makeIndex();
            if (matchTypeDefine()) {
              stream.resetIndex(index);
              result = parseVarDefine();
            }
            if (result == null) {
              stream.resetIndex(index);
              MagicScriptError.error("try 括号中只允许写赋值语句", stream.consume().getSpan());
            }
          }
          tryResources.add(result);
        }
      }
      stream.expect(")");
    }
    List<Node> tryBlocks = parseFunctionBody();
    pop();
    List<Node> catchBlocks = new ArrayList<>();
    List<Node> finallyBlocks = new ArrayList<>();
    VarIndex exceptionVarNode = null;
    if (stream.match("catch", true)) {
      push();
      if (stream.match("(", true)) {
        exceptionVarNode = add(stream.expect(Identifier).getText());
        defines.add(exceptionVarNode.getName());
        stream.expect(")");
      }
      catchBlocks.addAll(parseFunctionBody());
      pop();
    }
    if (stream.match("finally", true)) {
      push();
      finallyBlocks.addAll(parseFunctionBody());
      pop();
    }
    return new TryStatement(
        addSpan(opening.getSpan(), stream.getPrev().getSpan()),
        exceptionVarNode,
        tryBlocks,
        tryResources,
        catchBlocks,
        finallyBlocks);
  }

  private List<Node> parseFunctionBody() {
    stream.expect("{");
    List<Node> blocks = new ArrayList<>();
    while (stream.hasMore() && !stream.match("}", false)) {
      Node node = parseStatement(true);
      if (node != null) {
        validateNode(node);
        blocks.add(node);
      }
    }
    expectCloseing();
    return blocks;
  }

  private Expression parseNewExpression(Span opening) {
    Expression expression = parseAccessOrCall(Identifier, true);
    if (expression instanceof MethodCall) {
      MethodCall call = (MethodCall) expression;
      Span span =
          addSpan(opening.getSource(), opening.getStart(), stream.getPrev().getSpan().getEnd());
      return parseAccessOrCall(new NewStatement(span, call.getMethod(), call.getArguments()));
    } else if (expression instanceof FunctionCall) {
      FunctionCall call = (FunctionCall) expression;
      Span span =
          addSpan(opening.getSource(), opening.getStart(), stream.getPrev().getSpan().getEnd());
      return parseAccessOrCall(new NewStatement(span, call.getFunction(), call.getArguments()));
    }
    MagicScriptError.error(
        "Expected MethodCall or FunctionCall or LambdaFunction", stream.getPrev().getSpan());
    return null;
  }

  private VariableDefine parseVarDefine() {
    Span opening = stream.consume().getSpan();
    boolean isMapAccess;
    if ((isMapAccess = stream.match(false, LeftCurly)) || stream.match(false, LeftBracket)) {
      stream.expect(LeftCurly, LeftBracket);
      List<Token> tokens = new ArrayList<>();
      do {
        Token token = stream.expect(Identifier);
        tokens.add(token);
      } while (stream.match(true, Comma));
      if (isMapAccess) {
        stream.expect(RightCurly);
      } else {
        stream.expect(RightBracket);
      }
      stream.match(Assignment, true);
      boolean isConst = "const".equals(opening.getText());
      VariableDestructuringDefine destructuring =
          new VariableDestructuringDefine(
              addSpan(opening, stream.getPrev().getSpan()),
              tokens.size(),
              parseExpression(),
              isMapAccess);
      for (Token token : tokens) {
        String variableName = token.getSpan().getText();
        VarIndex varIndex = forceAdd(variableName, isConst);
        defines.add(variableName);
        VariableDefine variableDefine =
            new VariableDefine(
                addSpan(token.getSpan(), stream.getPrev().getSpan()), varIndex, null);
        destructuring.add(variableDefine);
      }
      return destructuring;
    }
    Token token = stream.expect(Identifier);
    boolean isConst = "const".equals(opening.getText());
    checkKeyword(token.getSpan());
    String variableName = token.getSpan().getText();
    if (stream.match(Assignment, true)) {
      VarIndex varIndex = forceAdd(variableName, isConst);
      defines.add(variableName);
      return new VariableDefine(
          addSpan(opening, stream.getPrev().getSpan()), varIndex, parseExpression());
    } else if (isConst) {
      MagicScriptError.error("const修饰的变量需要给初始值", stream.getPrev().getSpan());
    }
    return new VariableDefine(
        addSpan(opening, stream.getPrev().getSpan()), forceAdd(variableName), null);
  }

  private void checkKeyword(Span span) {
    if (KEYWORDS.contains(span.getText())) {
      MagicScriptError.error("变量名不能定义为关键字", span);
    }
  }

  private WhileStatement parseWhileStatement() {
    Span openingWhile = stream.expect("while").getSpan();
    requiredNew = false;
    Expression condition = parseExpression();
    requiredNew = true;
    push();
    List<Node> trueBlock = parseFunctionBody();
    Span closingEnd = stream.getPrev().getSpan();
    pop();
    return new WhileStatement(addSpan(openingWhile, closingEnd), condition, trueBlock);
  }

  private Node parseForStatement() {
    Span openingFor = stream.expect("for").getSpan();
    stream.expect("(");
    if (isClassicForStatement()) {
      return parseClassicForStatement(openingFor);
    }
    return parseIteratorForStatement(openingFor);
  }

  private ForStatement parseIteratorForStatement(Span openingFor) {
    push();
    Span index = null;
    Span value = stream.expect(Identifier).getSpan();
    checkKeyword(value);
    defines.add(value.getText());
    if (stream.match(Comma, true)) {
      index = value;
      value = stream.expect(Identifier).getSpan();
      defines.add(value.getText());
      checkKeyword(value);
    }
    VarIndex indexOrKeyNode = null;
    if (index != null) {
      indexOrKeyNode = forceAdd(index.getText());
    }
    VarIndex valueNode = forceAdd(value.getText());
    stream.expect("in");
    Expression mapOrArray = parseExpression();
    stream.expect(")");
    List<Node> body = parseFunctionBody();
    VarIndex anonymous = forceAdd(ANONYMOUS_VARIABLE);
    pop();
    return new ForStatement(
        addSpan(openingFor, stream.getPrev().getSpan()),
        indexOrKeyNode,
        valueNode,
        anonymous,
        mapOrArray,
        body);
  }

  private ClassicForStatement parseClassicForStatement(Span openingFor) {
    push();
    Node initializer = null;
    Expression condition = null;
    Expression update = null;
    if (!stream.match(";", false)) {
      initializer = parseForInitializer();
    }
    stream.expect(";");
    if (!stream.match(";", false)) {
      condition = parseExpression();
    }
    stream.expect(";");
    if (!stream.match(")", false)) {
      update = parseExpression();
    }
    stream.expect(")");
    List<Node> body = parseFunctionBody();
    pop();
    return new ClassicForStatement(
        addSpan(openingFor, stream.getPrev().getSpan()), initializer, condition, update, body);
  }

  private Node parseForInitializer() {
    if (matchVarDefine()) {
      return parseVarDefine();
    }
    int index = stream.makeIndex();
    if (matchTypeDefine()) {
      stream.resetIndex(index);
      return parseVarDefine();
    }
    stream.resetIndex(index);
    return parseExpression();
  }

  private boolean isClassicForStatement() {
    int index = stream.makeIndex();
    int depth = 0;
    try {
      while (stream.hasMore()) {
        Token token = stream.consume();
        switch (token.getType()) {
          case LeftParantheses:
          case LeftBracket:
          case LeftCurly:
            depth++;
            break;
          case RightParantheses:
            if (depth == 0) {
              return false;
            }
            depth--;
            break;
          case RightBracket:
          case RightCurly:
            if (depth > 0) {
              depth--;
            }
            break;
          default:
            if (";".equals(token.getText()) && depth == 0) {
              return true;
            }
            break;
        }
      }
      return false;
    } finally {
      stream.resetIndex(index);
    }
  }

  private boolean isSwitchStatement() {
    if (!stream.match("switch", false)) {
      return false;
    }
    int index = stream.makeIndex();
    try {
      stream.consume();
      return stream.match(LeftParantheses, false);
    } finally {
      stream.resetIndex(index);
    }
  }

  private SwitchStatement parseSwitchStatement() {
    Span openingSwitch = stream.expect("switch").getSpan();
    requiredNew = false;
    Expression target = parseExpression();
    requiredNew = true;
    stream.expect("{");
    List<SwitchStatement.SwitchCase> switchCases = new ArrayList<>();
    boolean hasDefault = false;
    while (stream.hasMore() && !stream.match("}", false)) {
      boolean defaultCase = false;
      Expression condition = null;
      Span caseSpan;
      if (stream.match("case", true)) {
        caseSpan = stream.getPrev().getSpan();
        condition = parseExpression();
      } else if (stream.match("default", true)) {
        caseSpan = stream.getPrev().getSpan();
        defaultCase = true;
        if (hasDefault) {
          MagicScriptError.error("switch 中只允许一个 default", caseSpan);
        }
        hasDefault = true;
      } else {
        MagicScriptError.error("switch 中只允许 case 或 default", stream.consume().getSpan());
        return null;
      }
      stream.expect(Colon);
      List<Node> statements = new ArrayList<>();
      while (stream.hasMore()
          && !stream.match("}", false)
          && !stream.match("case", false)
          && !stream.match("default", false)) {
        Node node = parseStatement(true);
        if (node != null) {
          validateNode(node);
          statements.add(node);
        }
      }
      Span clauseEnd =
          statements.isEmpty() ? stream.getPrev().getSpan() : statements.get(statements.size() - 1).getSpan();
      switchCases.add(
          new SwitchStatement.SwitchCase(
              addSpan(caseSpan, clauseEnd), condition, statements, defaultCase));
    }
    Span closingEnd = expectCloseing();
    return new SwitchStatement(addSpan(openingSwitch, closingEnd), target, switchCases);
  }

  private Span expectCloseing() {
    if (!stream.hasMore()) {
      MagicScriptError.error("Did not find closing }.", stream.prev().getSpan());
    }
    return stream.expect("}").getSpan();
  }

  private Node parseIfStatement() {
    Span openingIf = stream.expect("if").getSpan();
    requiredNew = false;
    Expression condition = parseExpression();
    requiredNew = true;
    push();
    List<Node> trueBlock = parseFunctionBody();
    pop();
    List<IfStatement> elseIfs = new ArrayList<>();
    List<Node> falseBlock = new ArrayList<>();
    while (stream.hasMore() && stream.match("else", true)) {
      if (stream.hasMore() && stream.match("if", false)) {
        Span elseIfOpening = stream.expect("if").getSpan();
        Expression elseIfCondition = parseExpression();
        push();
        List<Node> elseIfBlock = parseFunctionBody();
        Span elseIfSpan =
            addSpan(
                elseIfOpening,
                elseIfBlock.size() > 0
                    ? elseIfBlock.get(elseIfBlock.size() - 1).getSpan()
                    : elseIfOpening);
        pop();
        elseIfs.add(
            new IfStatement(
                elseIfSpan, elseIfCondition, elseIfBlock, new ArrayList<>(), new ArrayList<>()));
      } else {
        push();
        falseBlock.addAll(parseFunctionBody());
        pop();
        break;
      }
    }
    Span closingEnd = stream.getPrev().getSpan();

    return new IfStatement(
        addSpan(openingIf, closingEnd), condition, trueBlock, elseIfs, falseBlock);
  }

  private Node parseReturn() {
    Span returnSpan = stream.expect("return").getSpan();
    if (stream.match(false, ";", "}")) {
      return new Return(returnSpan, null);
    }
    Expression returnValue = parseExpression();
    return new Return(addSpan(returnSpan, returnValue.getSpan()), returnValue);
  }

  public Expression parseExpression() {
    return parseTernaryOperator();
  }

  public Expression parseExpression(boolean expectRightCurly) {
    return parseTernaryOperator(expectRightCurly);
  }

  private Expression parseTernaryOperator(boolean expectRightCurly) {
    Expression condition = parseBinaryOperator(0, expectRightCurly);
    if (stream.match(QuestionMark, true)) {
      Expression trueExpression = parseTernaryOperator(expectRightCurly);
      stream.expect(Colon);
      Expression falseExpression = parseTernaryOperator(expectRightCurly);
      if (condition instanceof AssigmentOperation) {
        AssigmentOperation operation = (AssigmentOperation) condition;
        operation.setRightOperand(
            new TernaryOperation(operation.getRightOperand(), trueExpression, falseExpression));
        return operation;
      }
      return new TernaryOperation(condition, trueExpression, falseExpression);
    } else {
      return condition;
    }
  }

  private Expression parseTernaryOperator() {
    return parseTernaryOperator(false);
  }

  private Expression parseBinaryOperator(
      TokenType[][] precedence, int level, boolean expectRightCurly) {
    int nextLevel = level + 1;
    Expression left =
        nextLevel == precedence.length
            ? parseUnaryOperator(expectRightCurly)
            : parseBinaryOperator(nextLevel, expectRightCurly);

    TokenType[] operators = precedence[level];
    while (stream.hasMore() && stream.match(false, operators)) {
      Token operator = stream.consume();
      if (operator.getType().isInLinq() && linqLevel == 0) {
        MagicScriptError.error(operator.getText() + " 只能在Linq中使用", stream);
      }
      Expression right =
          nextLevel == precedence.length
              ? parseUnaryOperator(expectRightCurly)
              : parseBinaryOperator(nextLevel, expectRightCurly);
      left = BinaryOperation.create(left, operator, right, linqLevel);
    }
    addSpan(left.getSpan());
    return left;
  }

  private Expression parseBinaryOperator(int level, boolean expectRightCurly) {
    if (linqLevel > 0) {
      return parseBinaryOperator(LINQ_BINARY_OPERATOR_PRECEDENCE, level, expectRightCurly);
    }
    return parseBinaryOperator(BINARY_OPERATOR_PRECEDENCE, level, expectRightCurly);
  }

  private Expression parseUnaryOperator(boolean expectRightCurly) {
    if (stream.match(false, UNARY_OPERATORS)) {
      return new UnaryOperation(stream.consume(), parseUnaryOperator(expectRightCurly));
    } else {
      if (stream.match(LeftParantheses, false)) { // (
        Span openSpan = stream.expect(LeftParantheses).getSpan();
        int index = stream.makeIndex();
        List<VarIndex> parameters = new ArrayList<>();
        push();
        try {
          while (stream.match(Identifier, false)) {
            Token identifier = stream.expect(Identifier);
            checkKeyword(identifier.getSpan());
            if (requiredNew) {
              parameters.add(forceAdd(identifier.getSpan().getText()));
            } else {
              parameters.add(add(identifier.getSpan().getText()));
            }
            if (stream.match(Comma, true)) { // ,
              continue;
            }
            if (stream.match(RightParantheses, true)) { // )
              if (stream.match(Lambda, true)) { // =>
                return parseLambdaBody(openSpan, parameters);
              }
              break;
            }
          }
          if (stream.match(RightParantheses, true) && stream.match(Lambda, true)) {
            return parseLambdaBody(openSpan, parameters);
          }
        } finally {
          pop();
        }
        stream.resetIndex(index);
        Expression expression = parseExpression();
        stream.expect(RightParantheses);
        return parseAccessOrCall(expression);
      } else {
        Expression expression = parseAccessOrCallOrLiteral(expectRightCurly);
        if (expression instanceof VariableSetter) {
          if (stream.match(false, PlusPlus, MinusMinus)) {
            return new UnaryOperation(stream.consume(), expression, true);
          }
        }

        return expression;
      }
    }
  }

  private Expression parseLambdaBody(Span openSpan, List<VarIndex> parameters) {
    defines.clear();
    int index = stream.makeIndex();
    List<Node> childNodes = new ArrayList<>();
    try {
      Expression expression = parseExpression();
      childNodes.add(new Return(new Span("return", 0, 6), expression));
      return new LambdaFunction(addSpan(openSpan, expression.getSpan()), parameters, childNodes);
    } catch (Exception e) {
      stream.resetIndex(index);
      if (stream.match(LeftCurly, true)) {
        while (stream.hasMore() && !stream.match(false, "}")) {
          Node node = parseStatement(true);
          validateNode(node);
          childNodes.add(node);
        }
        Span closeSpan = expectCloseing();
        return new LambdaFunction(addSpan(openSpan, closeSpan), parameters, childNodes);
      } else {
        Node node = parseStatement();
        childNodes.add(new Return(addSpan("return", 0, 6), node));
        return new LambdaFunction(addSpan(openSpan, node.getSpan()), parameters, childNodes);
      }
    } finally {
      defines.clear();
    }
  }

  private Expression parseSpreadAccess(Token spread) {
    Expression target = parseExpression();
    return new Spread(addSpan(spread.getSpan(), target.getSpan()), target);
  }

  private Expression parseSpreadAccess() {
    Token spread = stream.expect(Spread);
    return parseSpreadAccess(spread);
  }

  private Expression parseSelect() {
    Span opeing = stream.expect("select", true).getSpan();
    linqLevel++;
    List<LinqField> fields = parseLinqFields();
    stream.expect("from", true);
    LinqField from = parseLinqField();
    List<LinqJoin> joins = parseLinqJoins();
    LinqExpression where = null;
    if (stream.match("where", true, true)) {
      where = new LinqExpression(parseExpression());
    }
    List<LinqField> groups = parseGroup();
    LinqExpression having = null;
    if (stream.match("having", true, true)) {
      having = new LinqExpression(parseExpression());
    }
    List<LinqOrder> orders = parseLinqOrders();
    linqLevel--;
    Expression limit = null;
    Expression offset = null;
    if (stream.match("limit", true, true)) {
      limit = parseExpression();
      if (stream.match("offset", true, true)) {
        offset = parseExpression();
      }
    }
    Span close = stream.getPrev().getSpan();
    return new LinqSelect(
        addSpan(opeing, close), fields, from, joins, where, groups, having, orders, limit, offset);
  }

  private List<LinqField> parseGroup() {
    List<LinqField> groups = new ArrayList<>();
    if (stream.match("group", true, true)) {
      stream.expect("by", true);
      do {
        Expression expression = parseExpression();
        groups.add(new LinqField(expression.getSpan(), expression, null));
      } while (stream.match(Comma, true));
    }
    return groups;
  }

  private List<LinqOrder> parseLinqOrders() {
    List<LinqOrder> orders = new ArrayList<>();
    if (stream.match("order", true, true)) {
      stream.expect("by", true);
      do {
        Expression expression = parseExpression();
        int order = 1;
        if (stream.match(false, true, "desc", "asc")) {
          if ("desc".equalsIgnoreCase(stream.consume().getText())) {
            order = -1;
          }
        }
        orders.add(
            new LinqOrder(
                addSpan(expression.getSpan(), stream.getPrev().getSpan()),
                expression,
                null,
                order));
      } while (stream.match(Comma, true));
    }
    return orders;
  }

  private List<LinqField> parseLinqFields() {
    List<LinqField> fields = new ArrayList<>();
    do {
      Expression expression = parseExpression();

      if (stream.match(Identifier, false) && !stream.match(LINQ_KEYWORDS, false, true)) {
        if (expression instanceof WholeLiteral) {
          MagicScriptError.error("* 后边不能跟别名", stream);
        } else if (expression instanceof MemberAccess && ((MemberAccess) expression).isWhole()) {
          MagicScriptError.error(expression.getSpan().getText() + " 后边不能跟别名", stream);
        }
        Span alias = stream.consume().getSpan();
        fields.add(
            new LinqField(addSpan(expression.getSpan(), alias), expression, add(alias.getText())));
      } else {
        fields.add(new LinqField(expression.getSpan(), expression, null));
      }
    } while (stream.match(Comma, true)); // ,
    if (fields.isEmpty()) {
      MagicScriptError.error("至少要查询一个字段", stream);
    }
    return fields;
  }

  private List<LinqJoin> parseLinqJoins() {
    List<LinqJoin> joins = new ArrayList<>();
    do {
      boolean isLeft = stream.match("left", false, true);
      Span opeing = isLeft ? stream.consume().getSpan() : null;
      if (stream.match("join", true, true)) {
        opeing = isLeft ? opeing : stream.getPrev().getSpan();
        LinqField target = parseLinqField();
        stream.expect("on", true);
        Expression condition = parseExpression();
        joins.add(
            new LinqJoin(addSpan(opeing, stream.getPrev().getSpan()), isLeft, target, condition));
      }
    } while (stream.match(false, true, "left", "join"));
    return joins;
  }

  private LinqField parseLinqField() {
    Expression expression = parseExpression();
    if (stream.match(Identifier, false) && !stream.match(LINQ_KEYWORDS, false, true)) {
      Span alias = stream.expect(Identifier).getSpan();
      return new LinqField(addSpan(expression.getSpan(), alias), expression, add(alias.getText()));
    }
    return new LinqField(expression.getSpan(), expression, null);
  }

  private Expression parseAccessOrCallOrLiteral(boolean expectRightCurly) {
    Expression expression = null;
    if (expectRightCurly && stream.match("}", false)) {
      return null;
    } else if (stream.match(Spread, false)) {
      expression = parseSpreadAccess();
    } else if (stream.match(Identifier, false)) {
      if (stream.match("async", false)) {
        expression = parseAsync();
      } else if (stream.match("select", false, true)) {
        expression = parseSelect();
      } else {
        expression = parseAccessOrCall(TokenType.Identifier, false);
      }
    } else if (stream.match(LeftCurly, false)) {
      expression = parseMapLiteral();
    } else if (stream.match(LeftBracket, false)) {
      expression = parseListLiteral();
    } else if (stream.match(StringLiteral, false)) {
      expression = createStringLiteral(stream.expect(StringLiteral));
    } else if (stream.match(BooleanLiteral, false)) {
      expression = new BooleanLiteral(stream.expect(BooleanLiteral).getSpan());
    } else if (stream.match(DoubleLiteral, false)) {
      expression = new DoubleLiteral(stream.expect(DoubleLiteral).getSpan());
    } else if (stream.match(FloatLiteral, false)) {
      expression = new FloatLiteral(stream.expect(FloatLiteral).getSpan());
    } else if (stream.match(ByteLiteral, false)) {
      Token token = stream.expect(ByteLiteral);
      expression =
          token.getValue() != null
              ? new ByteLiteral(token.getSpan(), token.getValue())
              : new ByteLiteral(token.getSpan());
    } else if (stream.match(ShortLiteral, false)) {
      expression = new ShortLiteral(stream.expect(ShortLiteral).getSpan());
    } else if (stream.match(IntegerLiteral, false)) {
      Token token = stream.expect(IntegerLiteral);
      expression =
          token.getValue() != null
              ? new IntegerLiteral(token.getSpan(), token.getValue())
              : new IntegerLiteral(token.getSpan());
    } else if (stream.match(LongLiteral, false)) {
      Token token = stream.expect(LongLiteral);
      expression =
          token.getValue() != null
              ? new LongLiteral(token.getSpan(), token.getValue())
              : new LongLiteral(token.getSpan());
    } else if (stream.match(DecimalLiteral, false)) {
      expression = new BigDecimalLiteral(stream.expect(DecimalLiteral).getSpan());
    } else if (stream.match(RegexpLiteral, false)) {
      Token token = stream.expect(RegexpLiteral);
      expression = new RegexpLiteral(token.getSpan(), token);
    } else if (stream.match(NullLiteral, false)) {
      expression = new NullLiteral(stream.expect(NullLiteral).getSpan());
    } else if (linqLevel > 0 && stream.match(Asterisk, false)) {
      expression = new WholeLiteral(stream.expect(Asterisk).getSpan());
    } else if (stream.match(Language, false)) {
      expression = new LanguageExpression(stream.consume().getSpan(), stream.consume().getSpan());
    }
    if (expression == null) {
      MagicScriptError.error(
          "Expected a variable, field, map, array, function or method call, or literal.", stream);
    }
    return parseAccessOrCall(expression);
  }

  private StringLiteral createStringLiteral(Token token) {
    if (token.getTokenStream() == null) {
      return new StringLiteral(token);
    }
    TokenStream tempStream = this.stream;
    this.stream = token.getTokenStream();
    List<Expression> expressionList = new ArrayList<>();
    while (this.stream.hasMore()) {
      expressionList.add(parseExpression());
    }
    this.stream = tempStream;
    return new StringLiteral(token, expressionList);
  }

  private Expression parseMapLiteral() {
    Span openCurly = stream.expect(LeftCurly).getSpan();

    List<Expression> keys = new ArrayList<>();
    List<Expression> values = new ArrayList<>();
    while (stream.hasMore() && !stream.match("}", false)) {
      Expression key;
      if (stream.hasPrev()) {
        Token prev = stream.getPrev();
        if (stream.match(Spread, false)
            && (prev.getType() == LeftCurly || prev.getType() == Comma)) {
          Token spread = stream.expect(Spread);
          keys.add(null);
          values.add(parseSpreadAccess(spread));
          if (stream.match(false, Comma, RightCurly)) {
            stream.match(Comma, true);
          }
          continue;
        }
      }
      boolean isStringKey;
      if (isStringKey = stream.match(StringLiteral, false)) { // "key" 'key' """key"""
        key = createStringLiteral(stream.expect(StringLiteral));
      } else if (stream.match(LeftBracket, true)) { // [key]
        key = parseExpression();
        stream.expect(RightBracket);
      } else { // key
        key = createStringLiteral(stream.expect(Identifier));
      }
      keys.add(key);
      if (stream.match(false, Comma, RightCurly)) {
        stream.match(Comma, true);
        if (!isStringKey && !(key instanceof VariableAccess)) {
          values.add(new VariableAccess(key.getSpan(), add(key.getSpan().getText())));
        } else {
          values.add(key);
        }
      } else {
        stream.expect(":");
        values.add(parseExpression());
        if (!stream.match("}", false)) {
          stream.expect(Comma);
        }
      }
    }
    Span closeCurly = stream.expect("}").getSpan();
    return new MapLiteral(addSpan(openCurly, closeCurly), keys, values);
  }

  private Expression parseListLiteral() {
    Span openBracket = stream.expect(LeftBracket).getSpan();

    List<Expression> values = new ArrayList<>();
    while (stream.hasMore() && !stream.match(RightBracket, false)) {
      values.add(parseExpression());
      if (!stream.match(RightBracket, false)) {
        stream.expect(Comma);
      }
    }

    Span closeBracket = stream.expect(RightBracket).getSpan();
    return new ListLiteral(addSpan(openBracket, closeBracket), values);
  }

  private Expression parseAccessOrCall(TokenType tokenType, boolean isNew) {
    Token token = stream.expect(tokenType);
    Span identifier = token.getSpan();
    if (tokenType == Identifier && "new".equals(identifier.getText())) {
      return parseNewExpression(identifier);
    }
    if (tokenType == Identifier && stream.match(Lambda, true)) {
      push();
      String name = identifier.getText();
      Expression expression =
          parseLambdaBody(
              identifier, Collections.singletonList(requiredNew ? forceAdd(name) : add(name)));
      pop();
      return expression;
    }
    Expression result =
        tokenType == StringLiteral
            ? createStringLiteral(token)
            : new VariableAccess(identifier, add(identifier.getText()));
    return parseAccessOrCall(result, isNew);
  }

  private Expression parseAccessOrCall(Expression target) {
    return parseAccessOrCall(target, false);
  }

  private Expression parseAccessOrCall(Expression target, boolean isNew) {
    while (stream.hasMore()
        && stream.match(false, LeftParantheses, LeftBracket, Period, QuestionPeriod, ColonColon)) {
      if (stream.match(ColonColon, false)) {
        Span open = stream.consume().getSpan();
        List<Expression> arguments = Collections.emptyList();
        Token identifier = stream.expect(Identifier);
        Span closing = identifier.getSpan();
        if (stream.match(LeftParantheses, false)) {
          arguments = parseArguments();
          closing = stream.expect(RightParantheses).getSpan();
        }
        target =
            new ClassConverter(addSpan(open, closing), identifier.getText(), target, arguments);
      }
      // function or method call
      else if (stream.match(LeftParantheses, false)) {
        List<Expression> arguments = parseArguments();
        Span closingSpan = stream.expect(RightParantheses).getSpan();
        if (target instanceof VariableAccess || target instanceof MapOrArrayAccess) {
          target =
              new FunctionCall(
                  addSpan(target.getSpan(), closingSpan), target, arguments, linqLevel > 0);
        } else if (target instanceof MemberAccess) {
          target =
              new MethodCall(
                  addSpan(target.getSpan(), closingSpan),
                  (MemberAccess) target,
                  arguments,
                  linqLevel > 0);
        } else {
          MagicScriptError.error("Expected a variable, field or method.", stream);
        }
        if (isNew) {
          break;
        }
      }

      // map or array access
      else if (stream.match(LeftBracket, true)) {
        Expression keyOrIndex = parseExpression();
        Span closingSpan = stream.expect(RightBracket).getSpan();
        target = new MapOrArrayAccess(addSpan(target.getSpan(), closingSpan), target, keyOrIndex);
      }

      // field or method access
      else if (stream.match(false, Period, QuestionPeriod)) {
        boolean optional = stream.consume().getType() == QuestionPeriod;
        if (linqLevel > 0 && stream.match(Asterisk, false)) {
          target = new MemberAccess(target, optional, stream.expect(Asterisk).getSpan(), true);
        } else {
          target =
              new MemberAccess(
                  target, optional, stream.expect(Identifier, SqlAnd, SqlOr).getSpan(), false);
        }
      }
    }
    return target;
  }

  /** Does not consume the closing parentheses. */
  private List<Expression> parseArguments() {
    stream.expect(LeftParantheses);
    List<Expression> arguments = new ArrayList<Expression>();
    while (stream.hasMore() && !stream.match(RightParantheses, false)) {
      arguments.add(parseExpression());
      if (!stream.match(RightParantheses, false)) {
        stream.expect(Comma);
      }
    }
    return arguments;
  }
}
