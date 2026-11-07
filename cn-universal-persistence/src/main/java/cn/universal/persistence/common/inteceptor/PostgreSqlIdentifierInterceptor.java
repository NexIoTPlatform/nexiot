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

package cn.universal.persistence.common.inteceptor;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * PostgreSQL/达梦下将 MySQL 风格的反引号标识符重写为双引号。
 *
 * <p>主要用于兜底 tk.mybatis 通用 Provider 生成的 SQL，这类 SQL 会直接读取实体上的
 * {@code @Column(name = "`field`")}，在 ANSI 引号数据库下会因为反引号而语法报错。
 */
@Intercepts({
  @Signature(
      type = StatementHandler.class,
      method = "prepare",
      args = {Connection.class, Integer.class})
})
public class PostgreSqlIdentifierInterceptor implements Interceptor {

  private static final Pattern MYSQL_IDENTIFIER_PATTERN = Pattern.compile("`([^`]+)`");

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Connection connection = (Connection) invocation.getArgs()[0];
    if (!isAnsiQuotedDatabase(connection)) {
      return invocation.proceed();
    }

    StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
    BoundSql boundSql = statementHandler.getBoundSql();
    String sql = boundSql.getSql();
    if (sql == null || sql.indexOf('`') < 0) {
      return invocation.proceed();
    }

    String rewrittenSql = rewriteIdentifiers(sql, connection);
    if (!rewrittenSql.equals(sql)) {
      MetaObject metaObject = SystemMetaObject.forObject(boundSql);
      metaObject.setValue("sql", rewrittenSql);
    }
    return invocation.proceed();
  }

  @Override
  public Object plugin(Object target) {
    if (target instanceof StatementHandler) {
      return Plugin.wrap(target, this);
    }
    return target;
  }

  @Override
  public void setProperties(Properties properties) {}

  private String rewriteIdentifiers(String sql, Connection connection) {
    if (sql == null || sql.indexOf('`') < 0) {
      return sql;
    }

    boolean dameng = isDameng(connection);
    Matcher matcher = MYSQL_IDENTIFIER_PATTERN.matcher(sql);
    StringBuffer rewritten = new StringBuffer();
    while (matcher.find()) {
      String identifier = matcher.group(1);
      if (dameng) {
        identifier = identifier.toUpperCase();
      }
      matcher.appendReplacement(rewritten, Matcher.quoteReplacement("\"" + identifier + "\""));
    }
    matcher.appendTail(rewritten);
    return rewritten.toString();
  }

  private boolean isAnsiQuotedDatabase(Connection connection) {
    try {
      DatabaseMetaData metaData = connection.getMetaData();
      String productName = metaData == null ? null : metaData.getDatabaseProductName();
      if (productName == null) {
        return false;
      }
      String normalized = productName.toLowerCase();
      return normalized.contains("postgresql") || isDameng(normalized);
    } catch (Exception ignored) {
      return false;
    }
  }

  private boolean isDameng(Connection connection) {
    try {
      DatabaseMetaData metaData = connection.getMetaData();
      String productName = metaData == null ? null : metaData.getDatabaseProductName();
      return isDameng(productName == null ? null : productName.toLowerCase());
    } catch (Exception ignored) {
      return false;
    }
  }

  private boolean isDameng(String productName) {
    return productName != null
        && (productName.contains("dm dbms")
            || productName.equals("dm")
            || productName.contains("dameng"));
  }
}
