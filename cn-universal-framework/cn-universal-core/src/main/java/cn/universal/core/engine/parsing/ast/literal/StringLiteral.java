package cn.universal.core.engine.parsing.ast.literal;

import cn.universal.core.engine.MagicScriptError;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.CharacterStream;
import cn.universal.core.engine.parsing.Token;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Literal;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/** String 常量 */
public class StringLiteral extends Literal {

  private final Token token;

  private final List<Expression> expressionList;

  public StringLiteral(Token token) {
    this(token, null);
  }

  public StringLiteral(Token token, List<Expression> expressionList) {
    super(token.getSpan());
    this.token = token;
    this.expressionList = expressionList;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    if (expressionList != null) {
      expressionList.forEach(expr -> expr.visitMethod(compiler));
    }
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    if (isTemplateString()) {
      if (expressionList.isEmpty()) {
        compiler.ldc("");
      } else {
        compiler
            .typeInsn(NEW, StringJoiner.class)
            .insn(DUP)
            .ldc("")
            .invoke(INVOKESPECIAL, StringJoiner.class, "<init>", void.class, CharSequence.class);
        expressionList.forEach(
            expression ->
                compiler
                    .visit(expression)
                    .ldc("")
                    .invoke(
                        INVOKESTATIC,
                        Objects.class,
                        "toString",
                        String.class,
                        Object.class,
                        String.class)
                    .invoke(
                        INVOKEVIRTUAL,
                        StringJoiner.class,
                        "add",
                        StringJoiner.class,
                        CharSequence.class));
        compiler.invoke(INVOKEVIRTUAL, StringJoiner.class, "toString", String.class);
      }
    } else {
      compiler.ldc(getValue());
    }
  }

  public boolean isTemplateString() {
    return expressionList != null;
  }

  private String transferString(String text) {
    StringBuilder builder = new StringBuilder();

    CharacterStream stream = new CharacterStream(text);
    // 处理转义符
    while (stream.hasMore()) {
      if (stream.match("\\\\", true)) {
        builder.append('\\');
      } else if (stream.match("\\n", true)) {
        builder.append('\n');
      } else if (stream.match("\\r", true)) {
        builder.append('\r');
      } else if (stream.match("\\t", true)) {
        builder.append('\t');
      } else if (stream.match("\\\"", true)) {
        builder.append('"');
      } else if (stream.match("\\'", true)) {
        builder.append("'");
      } else {
        builder.append(stream.consume());
      }
    }
    return builder.toString();
  }

  public String getValue() {
    if (token.getTokenStream() != null) {
      MagicScriptError.error("此处不支持模板字符串", getSpan());
    }
    return transferString(getSpan().getText());
  }
}
