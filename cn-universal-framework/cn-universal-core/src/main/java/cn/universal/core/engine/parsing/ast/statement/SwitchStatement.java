package cn.universal.core.engine.parsing.ast.statement;

import cn.universal.core.engine.asm.Label;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Node;
import cn.universal.core.engine.runtime.handle.OperatorHandle;
import java.util.ArrayList;
import java.util.List;

public class SwitchStatement extends Node {

  private final Expression target;
  private final List<SwitchCase> switchCases;

  public SwitchStatement(Span span, Expression target, List<SwitchCase> switchCases) {
    super(span);
    this.target = target;
    this.switchCases = switchCases;
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    target.visitMethod(compiler);
    switchCases.forEach(it -> it.visitMethod(compiler));
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    int targetSlot = compiler.getTempIndex();
    Label end = new Label();
    List<Label> labels = new ArrayList<>(switchCases.size());
    Label defaultLabel = end;
    compiler.visit(target).store(targetSlot).markBreakLabel(end);
    for (SwitchCase switchCase : switchCases) {
      Label label = new Label();
      labels.add(label);
      if (switchCase.isDefaultCase()) {
        defaultLabel = label;
        continue;
      }
      compiler
          .loadLocal(targetSlot)
          .visit(switchCase.getCondition())
          .operator("equals")
          .invoke(INVOKESTATIC, OperatorHandle.class, "isTrue", boolean.class, Object.class)
          .jump(IFNE, label);
    }
    compiler.jump(GOTO, defaultLabel);
    for (int i = 0; i < switchCases.size(); i++) {
      compiler.label(labels.get(i)).compile(switchCases.get(i).getStatements());
    }
    compiler.label(end).exitLabel();
  }

  public static class SwitchCase extends Node {

    private final Expression condition;
    private final List<Node> statements;
    private final boolean defaultCase;

    public SwitchCase(
        Span span, Expression condition, List<Node> statements, boolean defaultCase) {
      super(span);
      this.condition = condition;
      this.statements = statements;
      this.defaultCase = defaultCase;
    }

    @Override
    public void visitMethod(MagicScriptCompiler compiler) {
      if (condition != null) {
        condition.visitMethod(compiler);
      }
      statements.forEach(it -> it.visitMethod(compiler));
    }

    public Expression getCondition() {
      return condition;
    }

    public List<Node> getStatements() {
      return statements;
    }

    public boolean isDefaultCase() {
      return defaultCase;
    }
  }
}
