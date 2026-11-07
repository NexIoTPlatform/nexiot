package cn.universal.core.engine.parsing.ast.literal;

import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Literal;
import cn.universal.core.engine.parsing.ast.statement.Spread;
import java.util.List;
import java.util.Objects;

/** map常量 */
public class MapLiteral extends Literal {

  private final List<Expression> keys;
  private final List<Expression> values;

  public MapLiteral(Span span, List<Expression> keys, List<Expression> values) {
    super(span);
    this.keys = keys;
    this.values = values;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    values.forEach(it -> it.visitMethod(compiler));
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    int size = keys.size();
    compiler
        .insn(values.stream().anyMatch(it -> it instanceof Spread) ? ICONST_1 : ICONST_0)
        .asBoolean()
        .visitInt((int) (size * 2 - keys.stream().filter(Objects::isNull).count()))
        .typeInsn(ANEWARRAY, Object.class);
    int index = 0;
    for (int i = 0; i < size; i++) {
      Expression key = keys.get(i);
      Expression expression = values.get(i);
      compiler.insn(DUP).visitInt(index++);
      if (expression instanceof Spread) {
        compiler.visit(expression);
      } else {
        compiler.visit(key).insn(AASTORE).insn(DUP).visitInt(index++).visit(expression);
      }
      compiler.insn(AASTORE);
    }
    compiler.call("newLinkedHashMap", 2);
  }
}
