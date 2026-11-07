package cn.universal.core.engine.parsing.ast.literal;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.RegexpToken;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Literal;
import java.util.regex.Pattern;

/** 正则常量 */
public class RegexpLiteral extends Literal {

  private int flag;

  public RegexpLiteral(Span span, Object value) {
    super(span);
    this.flag = ((RegexpToken) value).getFlag();
    int i = flag & Pattern.CASE_INSENSITIVE;
    int m = flag & Pattern.MULTILINE;
    int s = flag & Pattern.DOTALL;
    int u = flag & Pattern.UNICODE_CHARACTER_CLASS;

    int f = 0;
    f |= i;
    f |= m;
    f |= s;
    f |= u;
    this.flag = f;
    setValue(
        Pattern.compile(span.getText().replaceAll("^/", "").replaceAll("/[gismuy]*?$", ""), f));
  }

  @Override
  public void compile(MagicScriptCompiler context) {
    String regex = getSpan().getText().replaceAll("^/", "").replaceAll("/[gismuy]*?$", "");
    context
        .ldc(regex)
        .visitInt(this.flag)
        .invoke(INVOKESTATIC, Pattern.class, "compile", Pattern.class, String.class, int.class);
  }
}
