package cn.universal.core.engine.parsing.ast.literal;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Literal;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/** boolean常量 */
public class BooleanLiteral extends Literal {

  public BooleanLiteral(Span literal) {
    super(literal, Boolean.parseBoolean(literal.getText()));
  }

  public static boolean isTrue(Object object) {
    if (object == null) {
      return false;
    }
    if (object instanceof Boolean) {
      return (Boolean) object;
    }
    if (object instanceof CharSequence) { // 非空字符串
      return ((CharSequence) object).length() != 0;
    }
    if (object instanceof Number) { // 非0 为 true
      return ((Number) object).doubleValue() != 0;
    }
    if (object instanceof Collection) { // 非空集合
      return !((Collection) object).isEmpty();
    }
    if (object.getClass().isArray()) { // 非空数组
      return Array.getLength(object) > 0;
    }
    if (object instanceof Map) { // 非空Map
      return !((Map) object).isEmpty();
    }
    // 其它情况全视作为true
    return true;
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler.insn((Boolean) this.value ? ICONST_1 : ICONST_0).asBoolean();
  }
}
