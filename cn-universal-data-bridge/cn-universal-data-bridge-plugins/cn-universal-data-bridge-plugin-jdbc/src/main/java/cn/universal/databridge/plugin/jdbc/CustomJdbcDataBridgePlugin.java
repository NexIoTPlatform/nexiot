/// *
// *
// * Copyright (c) 2025, NexIoT. All Rights Reserved.
// *
// * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
// * @Author: gitee.com/NexIoT
// * @Email: wo8335224@gmail.com
// * @Wechat: outlookFil
// *
// *
// */
//
// package cn.universal.databridge.plugin.jdbc;
//
// import cn.universal.databridge.entity.DataBridgeConfig;
// import cn.universal.databridge.entity.PluginInfo;
// import cn.universal.databridge.entity.ResourceConnection;
// import cn.universal.databridge.enums.PluginStatus;
// import cn.universal.databridge.plugin.DataBridgePlugin;
// import cn.universal.databridge.plugin.SourceScope;
// import java.sql.Connection;
// import java.util.List;
// import javax.sql.DataSource;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.jdbc.datasource.DriverManagerDataSource;
// import org.springframework.stereotype.Component;
//
/// **
// * 用户自定义JDBC数据桥接插件示例 这个插件会覆盖默认的JDBC插件实现
// *
// * <p>使用方式： 1. 在配置文件中设置：databridge.plugins.jdbc.custom.enabled=true 2.
// * 或者直接使用@Component("jdbcDataBridgePlugin")来覆盖默认实现
// *
// * @version 2.0 @Author gitee.com/NexIoT
// * @since 2025/1/15
// */
// @Component("jdbcDataBridgePlugin")
// @ConditionalOnProperty(
//    prefix = "databridge.plugins.jdbc.custom",
//    name = "enabled",
//    havingValue = "true",
//    matchIfMissing = false)
// @Slf4j
// public class CustomJdbcDataBridgePlugin implements DataBridgePlugin {
//
//  private PluginStatus status = PluginStatus.INITIALIZING;
//
//  @Override
//  public PluginInfo getPluginInfo() {
//    return PluginInfo.builder()
//        .name("自定义JDBC数据桥接插件")
//        .version("2.0.0")
//        .description("用户自定义的JDBC数据桥接实现，支持更复杂的业务逻辑")
//        .author("用户")
//        .pluginType("JDBC")
//        .supportedResourceTypes(List.of("MYSQL", "POSTGRESQL", "H2", "ORACLE", "SQLSERVER"))
//        .dataDirection(PluginInfo.DataDirection.BIDIRECTIONAL)
//        .category("数据库")
//        .icon("database")
//        .build();
//  }
//
//  @Override
//  public Boolean testConnection(ResourceConnection connection) {
//    try {
//      DataSource dataSource = createDataSource(connection);
//      try (Connection conn = dataSource.getConnection()) {
//        return conn.isValid(5);
//      }
//    } catch (Exception e) {
//      log.error("自定义JDBC连接测试失败: {}", e.getMessage(), e);
//      return false;
//    }
//  }
//
//  @Override
//  public Boolean validateConfig(DataBridgeConfig config) {
//    return Boolean.TRUE;
//  }
//
//  @Override
//  public List<SourceScope> getSupportedSourceScopes() {
//    return List.of(
//        SourceScope.ALL_PRODUCTS, SourceScope.SPECIFIC_PRODUCTS, SourceScope.APPLICATION);
//  }
//
//  /** 创建数据源 */
//  private DataSource createDataSource(ResourceConnection connection) {
//    DriverManagerDataSource dataSource = new DriverManagerDataSource();
//
//    // 根据数据库类型设置驱动
//    String driverClassName = getDriverClassName(connection.getType().name());
//    dataSource.setDriverClassName(driverClassName);
//
//    // 构建连接URL
//    String url = buildConnectionUrl(connection);
//    dataSource.setUrl(url);
//    dataSource.setUsername(connection.getUsername());
//    dataSource.setPassword(connection.getPassword());
//
//    return dataSource;
//  }
//
//  /** 获取驱动类名 */
//  private String getDriverClassName(String dbType) {
//    return switch (dbType.toUpperCase()) {
//      case "MYSQL" -> "com.mysql.cj.jdbc.Driver";
//      case "POSTGRESQL" -> "org.postgresql.Driver";
//      case "H2" -> "org.h2.Driver";
//      case "ORACLE" -> "oracle.jdbc.driver.OracleDriver";
//      case "SQLSERVER" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
//      default -> throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
//    };
//  }
//
//  /** 构建连接URL */
//  private String buildConnectionUrl(ResourceConnection connection) {
//    String dbType = connection.getType().name().toLowerCase();
//    String host = connection.getHost();
//    Integer port = connection.getPort();
//    String databaseName = connection.getDatabaseName();
//
//    return switch (dbType) {
//      case "mysql" ->
//          String.format(
//
// "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
//              host, port, databaseName);
//      case "postgresql" -> String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
//      case "h2" -> String.format("jdbc:h2:tcp://%s:%d/%s", host, port, databaseName);
//      case "oracle" -> String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
//      case "sqlserver" ->
//          String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, databaseName);
//      default -> throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
//    };
//  }
// }
