/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.rule.parser;

import cn.universal.rule.enums.ParserFormat;
import cn.universal.rule.express.ExpressTemplate;
import cn.universal.rule.model.RuleParserResult;
import cn.universal.rule.model.RuleParserResult.RuleField;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLArrayExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * sql解析策略 @Author gitee.com/NexIoT
 *
 * @since 2025/12/3 9:16
 */
@Component
public class SqlModelParserStrategy implements RuleModelParserStrategy {

  @Override
  public ParserFormat getFormat() {
    return ParserFormat.sql;
  }

  @Override
  public RuleParserResult parse(String modelDefineString) {
    RuleParserResult ruleParserResult = new RuleParserResult();

    SQLStatement sqlStatement = SQLUtils.parseSingleStatement(modelDefineString, DbType.mock);
    SQLSelectStatement statement = (SQLSelectStatement) sqlStatement;
    SQLSelectQueryBlock query = (SQLSelectQueryBlock) statement.getSelect().getQuery();

    List<RuleField> ruleFields =
        query.getSelectList().stream()
            .map(
                item ->
                    new RuleField(
                        item.getExpr().toString().trim(),
                        Objects.isNull(item.getAlias()) ? null : item.getAlias().trim()))
            .collect(Collectors.toList());
    ruleParserResult.setFields(ruleFields);

    List<String> topics =
        Stream.of(query.getFrom().toString().split(","))
            .map(String::trim)
            .collect(Collectors.toList());
    ruleParserResult.setTopics(topics);

    String condition = convertWhereSql(query.getWhere());
    ruleParserResult.setCondition(condition);

    return ruleParserResult;
  }

  public static String convertWhereSql(SQLExpr expr) {
    String sql = "";
    if (expr instanceof SQLBinaryOpExpr) {
      SQLBinaryOpExpr binaryOpExpr = (SQLBinaryOpExpr) expr;
      if (binaryOpExpr.getOperator().equals(SQLBinaryOperator.Equality)) {
        sql = operand(binaryOpExpr.getLeft()) + " == " + operand(binaryOpExpr.getRight());
      } else if (binaryOpExpr.getOperator().equals(SQLBinaryOperator.Is)) {
        sql = operand(binaryOpExpr.getLeft()) + " == null";
      } else if (binaryOpExpr.getOperator().equals(SQLBinaryOperator.IsNot)) {
        sql = operand(binaryOpExpr.getLeft()) + " != null";
      } else if (binaryOpExpr.getOperator().equals(SQLBinaryOperator.LessThanOrGreater)) {
        sql = operand(binaryOpExpr.getLeft()) + " != " + operand(binaryOpExpr.getRight());
      } else if (binaryOpExpr.getOperator().equals(SQLBinaryOperator.BooleanAnd)) {
        sql = getAndOrSql(binaryOpExpr, SQLBinaryOperator.BooleanAnd);
      } else if (binaryOpExpr.getOperator().equals(SQLBinaryOperator.BooleanOr)) {
        sql = getAndOrSql(binaryOpExpr, SQLBinaryOperator.BooleanOr);
      } else {
        sql =
            String.format(
                "%s %s %s",
                operand(binaryOpExpr.getLeft()),
                binaryOpExpr.getOperator().name.toLowerCase(),
                operand(binaryOpExpr.getRight()));
      }
    } else {
      if (expr instanceof SQLInListExpr) {
        SQLInListExpr inListExpr = (SQLInListExpr) expr;
        sql = operand(inListExpr.getExpr()) + " in "
            + inListExpr.getTargetList().stream().map(SqlModelParserStrategy::operand)
                .collect(Collectors.joining(",", "[", "]"));
        if (inListExpr.isNot()) sql = "!(" + sql + ")";
      } else {
        if (Objects.nonNull(expr)) {
          sql = operand(expr);
        }
      }
    }

    return sql;
  }

  /** QLExpress 的原生数组操作不支持 JSON List；所有字段读取都走与 SELECT 相同的路径解析器。 */
  private static String operand(SQLExpr expr) {
    String path = fieldPath(expr);
    if (path != null) {
      return ExpressTemplate.FIELD_VALUE_FUNCTION + "(" + ExpressTemplate.SOURCE_VARIABLE + ", \""
          + path.replace("\\", "\\\\").replace("\"", "\\\"") + "\")";
    }
    if (expr instanceof SQLMethodInvokeExpr method && method.getOwner() == null
        && method.getFrom() == null && method.getUsing() == null && method.getFor() == null) {
      return method.getMethodName() + method.getArguments().stream()
          .map(SqlModelParserStrategy::operand).collect(Collectors.joining(",", "(", ")"));
    }
    if (expr instanceof SQLBinaryOpExpr) return "(" + convertWhereSql(expr) + ")";
    return expr.toString();
  }

  private static String fieldPath(SQLExpr expr) {
    if (expr instanceof SQLIdentifierExpr identifier) return identifier.normalizedName();
    if (expr instanceof SQLPropertyExpr property) {
      String owner = fieldPath(property.getOwner());
      return owner == null ? null : owner + "." + SQLUtils.normalize(property.getName());
    }
    if (expr instanceof SQLArrayExpr array) {
      String owner = fieldPath(array.getExpr());
      if (owner == null || array.getValues().stream().anyMatch(value -> !(value instanceof SQLIntegerExpr))) return null;
      return owner + array.getValues().stream().map(value -> "[" + value + "]")
          .collect(Collectors.joining());
    }
    return null;
  }

  private static String getAndOrSql(
      SQLBinaryOpExpr binaryOpExpr, SQLBinaryOperator binaryOperator) {
    String sql;
    String leftSql = convertWhereSql(binaryOpExpr.getLeft());
    String rightSql = convertWhereSql(binaryOpExpr.getRight());
    String baseSql = StringUtils.trimToEmpty(binaryOpExpr.toString());
    if (baseSql.startsWith("(")) {
      leftSql = String.format("(%s)", leftSql);
    }
    if (baseSql.endsWith(")")) {
      rightSql = String.format("(%s)", rightSql);
    }
    sql = String.format("%s %s %s", leftSql, binaryOperator.name.toLowerCase(), rightSql);
    return sql;
  }

  public static void main(String[] args) {
    String sql = "select a,abc as aaa,b,c,d from 'app:*',c where qwe is null ";
    RuleParserResult parse = new SqlModelParserStrategy().parse(sql);
    System.out.println(parse);
  }
}
