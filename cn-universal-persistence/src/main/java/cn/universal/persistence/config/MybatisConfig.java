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

package cn.universal.persistence.config;

import cn.universal.persistence.common.inteceptor.PostgreSqlIdentifierInterceptor;
import cn.universal.persistence.common.inteceptor.PerformanceInterceptor;
import cn.universal.persistence.common.typehandler.PostgreSqlNumericBooleanTypeHandler;
import java.util.Properties;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tk.mybatis.spring.annotation.MapperScan;

/** 扫描 */
@Configuration
@MapperScan(value = {"cn.universal.**.mapper","cc.nexiot.**.mapper","cn.nexaiot.**.mapper"})
public class MybatisConfig {

  @Bean
  Interceptor sqlExplainInterceptor() {
    return new PerformanceInterceptor();
  }

  @Bean
  Interceptor postgreSqlIdentifierInterceptor() {
    return new PostgreSqlIdentifierInterceptor();
  }

  @Bean
  DatabaseIdProvider databaseIdProvider() {
    VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
    Properties properties = new Properties();
    properties.setProperty("MySQL", "mysql");
    properties.setProperty("MariaDB", "mysql");
    properties.setProperty("PostgreSQL", "postgresql");
    properties.setProperty("DM DBMS", "dameng");
    properties.setProperty("DM", "dameng");
    provider.setProperties(properties);
    return provider;
  }

  @Bean
  @Profile({"postgresql", "pg", "dameng"})
  ConfigurationCustomizer postgreSqlBooleanCompatibilityCustomizer() {
    return configuration -> {
      configuration
          .getTypeHandlerRegistry()
          .register(Boolean.class, PostgreSqlNumericBooleanTypeHandler.class);
      configuration
          .getTypeHandlerRegistry()
          .register(boolean.class, PostgreSqlNumericBooleanTypeHandler.class);
    };
  }
}
