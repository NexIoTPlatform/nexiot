package cn.universal.core.engine.parsing.ast.linq;

import cn.universal.core.engine.MagicScriptError;
import cn.universal.core.engine.compile.MagicScriptCompiler;
import cn.universal.core.engine.parsing.Span;
import cn.universal.core.engine.parsing.ast.Expression;
import cn.universal.core.engine.parsing.ast.Literal;
import cn.universal.core.engine.runtime.Variables;
import cn.universal.core.engine.runtime.function.MagicScriptLambdaFunction;
import cn.universal.core.engine.runtime.linq.LinQBuilder;
import java.util.List;

public class LinqSelect extends Expression {

  private final List<LinqField> fields;

  private final LinqField from;

  private final List<LinqJoin> joins;

  private final LinqExpression where;

  private final List<LinqField> groups;

  private final LinqExpression having;

  private final List<LinqOrder> orders;

  private final Expression limit;

  private final Expression offset;

  public LinqSelect(
      Span span,
      List<LinqField> fields,
      LinqField from,
      List<LinqJoin> joins,
      LinqExpression where,
      List<LinqField> groups,
      LinqExpression having,
      List<LinqOrder> orders,
      Expression limit,
      Expression offset) {
    super(span);
    this.fields = fields;
    this.from = from;
    this.joins = joins;
    this.where = where;
    this.groups = groups;
    this.having = having;
    this.orders = orders;
    this.limit = limit;
    this.offset = offset;
    if (from.getVarIndex() == null) {
      MagicScriptError.error("LINQ中，别名是必须的", from.getSpan());
    }
  }

  @Override
  public void visitMethod(MagicScriptCompiler compiler) {
    from.visitMethod(compiler);
    fields.forEach(it -> it.visitMethod(compiler));
    joins.forEach(it -> it.visitMethod(compiler));
    groups.forEach(it -> it.visitMethod(compiler));
    orders.forEach(it -> it.visitMethod(compiler));
    if (where != null) {
      where.visitMethod(compiler);
    }
    if (having != null) {
      having.visitMethod(compiler);
    }
    if (limit != null) {
      limit.visitMethod(compiler);
    }
    if (offset != null) {
      offset.visitMethod(compiler);
    }
  }

  @Override
  public void compile(MagicScriptCompiler compiler) {
    compiler
        .load2()
        .invoke(INVOKESTATIC, LinQBuilder.class, "create", LinQBuilder.class, Variables.class)
        .visit(from.getExpression())
        .visitInt(from.getVarIndex().getIndex())
        .invoke(
            INVOKEVIRTUAL, LinQBuilder.class, "from", LinQBuilder.class, Object.class, int.class);
    if (where != null) {
      compiler
          .visit(where)
          .invoke(
              INVOKEVIRTUAL,
              LinQBuilder.class,
              "where",
              LinQBuilder.class,
              MagicScriptLambdaFunction.class);
    }
    if (having != null) {
      compiler
          .visit(having)
          .invoke(
              INVOKEVIRTUAL,
              LinQBuilder.class,
              "having",
              LinQBuilder.class,
              MagicScriptLambdaFunction.class);
    }
    groups.forEach(
        group ->
            compiler
                .visit(group)
                .invoke(
                    INVOKEVIRTUAL,
                    LinQBuilder.class,
                    "group",
                    LinQBuilder.class,
                    MagicScriptLambdaFunction.class));
    joins.forEach(compiler::visit);
    fields.forEach(
        field ->
            compiler
                .visit(field)
                .ldc(field.getAlias())
                .visitInt(field.getVarIndex() == null ? -1 : field.getVarIndex().getIndex())
                .invoke(
                    INVOKEVIRTUAL,
                    LinQBuilder.class,
                    "select",
                    LinQBuilder.class,
                    MagicScriptLambdaFunction.class,
                    String.class,
                    int.class));
    orders.forEach(
        order ->
            compiler
                .visit(order)
                .visitInt(order.getOrder())
                .invoke(
                    INVOKEVIRTUAL,
                    LinQBuilder.class,
                    "order",
                    LinQBuilder.class,
                    MagicScriptLambdaFunction.class,
                    int.class));
    if (limit != null) {
      if (offset == null && limit instanceof Literal && "1".equals(limit.getSpan().getText())) {
        compiler.invoke(INVOKEVIRTUAL, LinQBuilder.class, "executeAndFetchFirst", Object.class);
        return;
      }
      compiler
          .visit(limit)
          .invoke(INVOKEVIRTUAL, LinQBuilder.class, "limit", LinQBuilder.class, Object.class);
      if (offset != null) {
        compiler
            .visit(offset)
            .invoke(INVOKEVIRTUAL, LinQBuilder.class, "offset", LinQBuilder.class, Object.class);
      }
    }
    compiler.invoke(INVOKEVIRTUAL, LinQBuilder.class, "execute", List.class);
  }
}
