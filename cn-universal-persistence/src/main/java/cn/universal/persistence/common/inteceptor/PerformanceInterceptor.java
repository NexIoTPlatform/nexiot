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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MyBatis 性能拦截器，用于输出每条 SQL 语句及其执行时间 */
@Intercepts({
  @Signature(
      type = Executor.class,
      method = "query",
      args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(
      type = Executor.class,
      method = "update",
      args = {MappedStatement.class, Object.class})
})
public class PerformanceInterceptor implements Interceptor {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private final Logger logger = LoggerFactory.getLogger("mybatisPerformanceInterceptor");

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
    Object parameterObject = null;
    if (invocation.getArgs().length > 1) {
      parameterObject = invocation.getArgs()[1];
    }
    String statementId = mappedStatement.getId();
    BoundSql boundSql = mappedStatement.getBoundSql(parameterObject);
    Configuration configuration = mappedStatement.getConfiguration();
    String sql = getSql(boundSql, parameterObject, configuration);
    long start = System.currentTimeMillis();
    Object result = invocation.proceed();
    long end = System.currentTimeMillis();
    long timing = end - start;
    if (logger.isDebugEnabled()) {
      logger.debug("[I D]{}", statementId);
      logger.debug("[SQL]{}", sql);
      logger.debug("[耗时] {} ms", timing);
    }
    return result;
  }

  @Override
  public Object plugin(Object target) {
    if (target instanceof Executor) {
      return Plugin.wrap(target, this);
    }
    return target;
  }

  @Override
  public void setProperties(Properties properties) {}

  private String getSql(BoundSql boundSql, Object parameterObject, Configuration configuration) {
    String sql = "";
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    if (parameterMappings != null) {
      try {
        sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        for (int i = 0; i < parameterMappings.size(); i++) {
          ParameterMapping parameterMapping = parameterMappings.get(i);
          if (parameterMapping.getMode() != ParameterMode.OUT) {
            Object value;
            String propertyName = parameterMapping.getProperty();
            if (boundSql.hasAdditionalParameter(propertyName)) {
              value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
              value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
              value = parameterObject;
            } else {
              MetaObject metaObject = configuration.newMetaObject(parameterObject);
              value = metaObject.getValue(propertyName);
            }
            sql = replacePlaceholder(sql, value);
          }
        }
      } catch (Exception e) {
        logger.error("mybatis intercept error={}", e);
        logger.warn("mybatis intercept error ,sql [{}] propertyValue[{}]", sql, parameterObject);
      }
    }
    return sql;
  }

  private String replacePlaceholder(String sql, Object propertyValue) {
    String result;
    if (propertyValue != null) {
      if (propertyValue instanceof String) {
        // Escape replacement string special characters: $ and \
        String escapedValue = ((String) propertyValue).replace("\\", "\\\\").replace("$", "\\$");
        result = "'" + escapedValue + "'";
      } else if (propertyValue instanceof Date) {
        result = "'" + DATE_FORMAT.format(propertyValue) + "'";
      } else {
        // Escape replacement string special characters: $ and \
        String escapedValue = propertyValue.toString().replace("\\", "\\\\").replace("$", "\\$");
        result = escapedValue;
      }
    } else {
      result = "null";
    }
    return sql.replaceFirst("\\?", result);
  }
}
