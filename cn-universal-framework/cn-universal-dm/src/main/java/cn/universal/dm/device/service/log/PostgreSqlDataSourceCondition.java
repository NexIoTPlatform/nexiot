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

package cn.universal.dm.device.service.log;

import java.util.Arrays;
import java.util.Locale;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 仅在当前主数据源为 PostgreSQL 时启用。
 *
 * <p>TimescaleDB 依赖 PostgreSQL 能力，因此这里同时兼容：
 *
 * <ul>
 *   <li>PG/TimescaleDB profile</li>
 *   <li>显式配置 PostgreSQL JDBC URL / Driver</li>
 * </ul>
 */
public class PostgreSqlDataSourceCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    Environment environment = context.getEnvironment();
    if (environment == null) {
      return false;
    }
    if (Arrays.stream(environment.getActiveProfiles()).anyMatch(this::isPostgreSqlProfile)) {
      return true;
    }
    return containsPostgreSql(environment.getProperty("spring.datasource.driver-class-name"))
        || containsPostgreSql(environment.getProperty("spring.datasource.url"));
  }

  private boolean isPostgreSqlProfile(String profile) {
    String normalized = normalize(profile);
    return "postgresql".equalsIgnoreCase(normalized) || "pg".equalsIgnoreCase(normalized);
  }

  private boolean containsPostgreSql(String value) {
    return normalize(value).contains("postgresql");
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
