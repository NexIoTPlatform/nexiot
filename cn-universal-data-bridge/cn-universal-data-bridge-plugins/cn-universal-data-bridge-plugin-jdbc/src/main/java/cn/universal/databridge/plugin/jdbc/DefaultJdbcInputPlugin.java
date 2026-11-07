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

package cn.universal.databridge.plugin.jdbc;

import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.plugin.AbstractDataInputPlugin;
import cn.universal.databridge.plugin.SourceScope;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

/**
 * 默认JDBC数据输入插件 - 输入方向 用户可以通过实现自己的插件来覆盖此默认实现
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Component("defaultJdbcInputPlugin")
@ConditionalOnMissingBean(name = "jdbcInputPlugin")
@Slf4j
public class DefaultJdbcInputPlugin extends AbstractDataInputPlugin {

  @Override
  public PluginInfo getPluginInfo() {
    return PluginInfo.builder()
        .name("默认JDBC数据输入插件")
        .version("2.0.0")
        .description("默认的JDBC数据输入实现，支持从MySQL、PostgreSQL等关系型数据库读取数据并发送到IoT平台")
        .author("gitee.com/NexIoT")
        .pluginType("JDBC")
        .supportedResourceTypes(List.of("MYSQL", "POSTGRESQL", "H2", "ORACLE", "SQLSERVER"))
        .dataDirection(PluginInfo.DataDirection.INPUT)
        .category("数据库")
        .icon("database")
        .build();
  }

  @Override
  public Boolean testConnection(ResourceConnection connection) {
    try {
      DataSource dataSource = createDataSource(connection);
      try (Connection conn = dataSource.getConnection()) {
        return conn.isValid(5);
      }
    } catch (Exception e) {
      log.error("JDBC连接测试失败: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public Boolean validateConfig(DataBridgeConfig config) {
    if (config == null) {
      return false;
    }

    // 验证输入配置
    //    if (config.getConfig() == null || config.getConfig().trim().isEmpty()) {
    //      return false;
    //    }

    return true;
  }

  @Override
  public List<SourceScope> getSupportedSourceScopes() {
    return List.of(
        SourceScope.ALL_PRODUCTS, SourceScope.SPECIFIC_PRODUCTS, SourceScope.APPLICATION);
  }

  @Override
  protected void sendToIoTPlatform(Object processedData, DataBridgeConfig config) {
    try {
      // 这里需要实现将处理后的数据发送到IoT平台的逻辑
      // 例如：调用IoT平台的API、发送到消息队列等
      log.debug("发送数据到IoT平台: {}", processedData);

      // 示例实现：这里可以调用IoT平台的服务
      // iotPlatformService.sendData(processedData, config.getConfig());

    } catch (Exception e) {
      log.error("发送数据到IoT平台失败: {}", e.getMessage(), e);
      throw new RuntimeException("发送数据到IoT平台失败: " + e.getMessage(), e);
    }
  }

  /** 创建数据源 */
  private DataSource createDataSource(ResourceConnection connection) {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();

    // 根据数据库类型设置驱动
    String driverClassName = getDriverClassName(connection.getType().name());
    dataSource.setDriverClassName(driverClassName);

    // 构建连接URL
    String url = buildConnectionUrl(connection);
    dataSource.setUrl(url);
    dataSource.setUsername(connection.getUsername());
    dataSource.setPassword(connection.getPassword());

    return dataSource;
  }

  /** 获取驱动类名 */
  private String getDriverClassName(String dbType) {
    return switch (dbType.toUpperCase()) {
      case "MYSQL" -> "com.mysql.cj.jdbc.Driver";
      case "POSTGRESQL" -> "org.postgresql.Driver";
      case "H2" -> "org.h2.Driver";
      case "ORACLE" -> "oracle.jdbc.driver.OracleDriver";
      case "SQLSERVER" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
      default -> throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
    };
  }

  /** 构建连接URL */
  private String buildConnectionUrl(ResourceConnection connection) {
    String dbType = connection.getType().name().toLowerCase();
    String host = connection.getHost();
    Integer port = connection.getPort();
    String databaseName = connection.getDatabaseName();

    return switch (dbType) {
      case "mysql" ->
          String.format(
              "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
              host, port, databaseName);
      case "postgresql" -> String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
      case "h2" -> String.format("jdbc:h2:tcp://%s:%d/%s", host, port, databaseName);
      case "oracle" -> String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
      case "sqlserver" ->
          String.format(
              "jdbc:sqlserver://%s:%d;databaseName=%s;characterEncoding=UTF-8",
              host, port, databaseName);
      default -> throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
    };
  }
}
