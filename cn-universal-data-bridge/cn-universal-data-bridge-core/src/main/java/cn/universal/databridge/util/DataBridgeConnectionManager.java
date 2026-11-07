/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 数据桥接连接池管理器
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 */

package cn.universal.databridge.util;

import cn.universal.databridge.entity.ResourceConnection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据桥接连接池管理器 独立管理数据桥接的数据库连接池，避免与框架的tk.mybatis冲突
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Slf4j
public class DataBridgeConnectionManager {

  // 连接池缓存 - 使用独立的命名空间避免冲突
  private final ConcurrentMap<String, DataSource> connectionPoolCache = new ConcurrentHashMap<>();

  // 保活调度器
  private final ScheduledExecutorService keepAliveScheduler = Executors.newScheduledThreadPool(1);

  // 连接池配置 - 针对数据桥接优化
  private static final int MAX_POOL_SIZE = 3; // 数据桥接不需要太多连接
  private static final int MIN_IDLE = 1;
  private static final long CONNECTION_TIMEOUT = 20000L; // 20秒连接超时
  private static final long IDLE_TIMEOUT = 300000L; // 5分钟空闲超时
  private static final long MAX_LIFETIME = 1800000L; // 30分钟最大生命周期
  private static final long VALIDATION_TIMEOUT = 2000L; // 2秒验证超时

  // 保活配置
  private static final long KEEP_ALIVE_INTERVAL = 300000L; // 5分钟保活间隔

  public DataBridgeConnectionManager() {
    // 启动保活任务
    startKeepAliveTask();
  }

  /**
   * 获取或创建数据源
   *
   * @param connection 连接配置
   * @return 数据源
   */
  public DataSource getOrCreateDataSource(ResourceConnection connection) {
    String cacheKey = generateCacheKey(connection);

    // 先检查缓存中是否已有连接池
    DataSource existingDataSource = connectionPoolCache.get(cacheKey);
    if (existingDataSource != null) {
      // 检查连接池是否健康
      if (isDataSourceHealthy(existingDataSource)) {
        log.debug("复用现有数据桥接连接池: {}", cacheKey);
        return existingDataSource;
      } else {
        log.warn("连接池不健康，移除并重新创建: {}", cacheKey);
        removeDataSource(connection);
      }
    }

    // 创建新的连接池
    return connectionPoolCache.computeIfAbsent(
        cacheKey,
        key -> {
          log.info("创建数据桥接连接池: {}", key);
          return createDataBridgeDataSource(connection);
        });
  }

  /**
   * 创建数据桥接专用的数据源
   *
   * @param connection 连接配置
   * @return 数据源
   */
  private DataSource createDataBridgeDataSource(ResourceConnection connection) {
    try {
      log.info("创建数据桥接连接池: {}:{}", connection.getHost(), connection.getPort());

      // 测试网络连接
      testNetworkConnection(connection);

      HikariConfig config = new HikariConfig();

      // 基本连接信息
      String driverClassName = getDriverClassName(connection.getType().name());
      String jdbcUrl = buildConnectionUrl(connection);

      config.setDriverClassName(driverClassName);
      config.setJdbcUrl(jdbcUrl);
      config.setUsername(connection.getUsername());
      config.setPassword(connection.getPassword());

      // 数据桥接专用连接池配置
      config.setMaximumPoolSize(MAX_POOL_SIZE);
      config.setMinimumIdle(MIN_IDLE);
      config.setConnectionTimeout(CONNECTION_TIMEOUT);
      config.setIdleTimeout(IDLE_TIMEOUT);
      config.setMaxLifetime(MAX_LIFETIME);
      config.setValidationTimeout(VALIDATION_TIMEOUT);

      // 连接池名称 - 使用独立前缀避免冲突
      config.setPoolName("DataBridge-" + connection.getId());

      // 连接测试配置 - 根据数据库类型设置
      String dbType = connection.getType().name().toUpperCase();
      config.setConnectionTestQuery(getConnectionTestQuery(dbType));
      String initSql = getConnectionInitSql(dbType);
      if (initSql != null && !initSql.isEmpty()) {
        config.setConnectionInitSql(initSql);
      }

      // 数据桥接专用配置
      config.setInitializationFailTimeout(15000); // 15秒初始化超时
      config.setRegisterMbeans(false); // 关闭JMX监控，避免冲突
      config.setLeakDetectionThreshold(30000); // 30秒连接泄漏检测
      config.setAutoCommit(true);
      config.setKeepaliveTime(30000); // 30秒保活

      // 数据库特定属性配置
      addDatabaseSpecificProperties(config, dbType);

      // 创建数据源
      HikariDataSource dataSource = new HikariDataSource(config);

      // 测试连接
      testDataSource(dataSource);

      log.info("数据桥接连接池创建成功");
      return dataSource;

    } catch (Exception e) {
      log.error(
          "创建数据桥接连接池失败: {}:{} - {}", connection.getHost(), connection.getPort(), e.getMessage());

      // 根据错误类型提供解决建议
      if (e.getMessage().contains("Access denied")) {
        log.error(
            "解决建议: 检查MySQL用户权限，确保用户 '{}' 有访问数据库 '{}' 的权限",
            connection.getUsername(),
            getDatabaseName(connection));
      } else if (e.getMessage().contains("Could not create connection")) {
        log.error("解决建议: 检查网络连接和MySQL服务，确保端口 {} 可访问", connection.getPort());
      }

      throw new RuntimeException("数据桥接连接池创建失败: " + e.getMessage(), e);
    }
  }

  /**
   * 添加MySQL特定属性
   *
   * @param config HikariCP配置
   */
  private void addMySQLProperties(HikariConfig config) {
    config.addDataSourceProperty("useSSL", "false");
    config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
    config.addDataSourceProperty("autoReconnect", "true");
    config.addDataSourceProperty("failOverReadOnly", "false");
    config.addDataSourceProperty("maxReconnects", "2"); // 减少重连次数
    config.addDataSourceProperty("initialTimeout", "1");
    config.addDataSourceProperty("connectTimeout", "20000");
    config.addDataSourceProperty("socketTimeout", "20000");
    config.addDataSourceProperty("useLocalSessionState", "true");
    config.addDataSourceProperty("rewriteBatchedStatements", "true");
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("useServerPrepStmts", "true");
    config.addDataSourceProperty("maintainTimeStats", "false");
    config.addDataSourceProperty("elideSetAutoCommits", "true");
    config.addDataSourceProperty("useLocalTransactionState", "true");
    config.addDataSourceProperty("alwaysSendSetIsolation", "false");
    config.addDataSourceProperty("cacheCallableStmts", "true");
    config.addDataSourceProperty("cacheResultSetMetadata", "true");
    config.addDataSourceProperty("cacheServerConfiguration", "true");
  }

  /**
   * 添加PostgreSQL特定属性
   *
   * @param config HikariCP配置
   */
  private void addPostgreSQLProperties(HikariConfig config) {
    config.addDataSourceProperty("tcpKeepAlive", "true");
    config.addDataSourceProperty("socketTimeout", "20");
    config.addDataSourceProperty("loginTimeout", "20");
  }

  /**
   * 添加Oracle特定属性
   *
   * @param config HikariCP配置
   */
  private void addOracleProperties(HikariConfig config) {
    config.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "20000");
    config.addDataSourceProperty("oracle.jdbc.ReadTimeout", "20000");
  }

  /**
   * 添加SQL Server特定属性
   *
   * @param config HikariCP配置
   */
  private void addSQLServerProperties(HikariConfig config) {
    config.addDataSourceProperty("loginTimeout", "20");
    config.addDataSourceProperty("socketTimeout", "20000");
    config.addDataSourceProperty("encrypt", "false");
    config.addDataSourceProperty("trustServerCertificate", "true");
  }

  /**
   * 添加H2特定属性
   *
   * @param config HikariCP配置
   */
  private void addH2Properties(HikariConfig config) {
    // H2数据库通常不需要特殊配置
    config.addDataSourceProperty("AUTO_SERVER", "true");
  }

  /**
   * 测试网络连接
   *
   * @param connection 连接配置
   */
  private void testNetworkConnection(ResourceConnection connection) {
    try {
      java.net.Socket socket = new java.net.Socket();
      socket.connect(
          new java.net.InetSocketAddress(connection.getHost(), connection.getPort()), 5000);
      socket.close();
    } catch (Exception e) {
      log.error("网络连接测试失败: {}:{} - {}", connection.getHost(), connection.getPort(), e.getMessage());
      throw new RuntimeException("无法连接到数据库服务器: " + e.getMessage(), e);
    }
  }

  /**
   * 测试数据源连接
   *
   * @param dataSource 数据源
   */
  private void testDataSource(DataSource dataSource) {
    try (java.sql.Connection conn = dataSource.getConnection()) {
      if (!conn.isValid(2)) {
        throw new RuntimeException("连接无效");
      }
    } catch (Exception e) {
      log.error("数据桥接连接测试失败: {}", e.getMessage());
      throw new RuntimeException("数据桥接连接测试失败: " + e.getMessage(), e);
    }
  }

  /**
   * 检查数据源是否健康
   *
   * @param dataSource 数据源
   * @return 是否健康
   */
  private boolean isDataSourceHealthy(DataSource dataSource) {
    try {
      if (dataSource instanceof HikariDataSource) {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

        // 检查连接池是否已关闭
        if (hikariDataSource.isClosed()) {
          return false;
        }

        // 检查连接池状态
        try (java.sql.Connection conn = hikariDataSource.getConnection()) {
          return conn.isValid(2);
        }
      }

      // 对于非HikariCP数据源，尝试获取连接
      try (java.sql.Connection conn = dataSource.getConnection()) {
        return conn.isValid(2);
      }
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 构建连接URL
   *
   * @param connection 连接配置
   * @return 连接URL
   */
  private String buildConnectionUrl(ResourceConnection connection) {
    String dbType = connection.getType().name().toUpperCase();
    String host = connection.getHost();
    Integer port = connection.getPort();
    String databaseName = getDatabaseName(connection);

    return switch (dbType) {
      case "MYSQL" ->
          String.format(
              "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true&failOverReadOnly=false&maxReconnects=2&initialTimeout=1&connectTimeout=20000&socketTimeout=20000&useLocalSessionState=true&rewriteBatchedStatements=true&cachePrepStmts=true&useServerPrepStmts=true&maintainTimeStats=false&elideSetAutoCommits=true&useLocalTransactionState=true&alwaysSendSetIsolation=false&cacheCallableStmts=true&cacheResultSetMetadata=true&cacheServerConfiguration=true",
              host, port, databaseName);
      case "POSTGRESQL" -> String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
      case "H2" -> String.format("jdbc:h2:tcp://%s:%d/%s", host, port, databaseName);
      case "ORACLE" -> String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
      case "SQLSERVER" ->
          String.format(
              "jdbc:sqlserver://%s:%d;databaseName=%s;characterEncoding=UTF-8",
              host, port, databaseName);
      default -> throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
    };
  }

  /**
   * 获取数据库名称
   *
   * @param connection 连接配置
   * @return 数据库名称
   */
  private String getDatabaseName(ResourceConnection connection) {
    // 使用数据库名称字段，如果为空则根据数据库类型使用默认值
    String databaseName = connection.getDatabaseName();
    if (databaseName != null && !databaseName.trim().isEmpty()) {
      return databaseName;
    }
    // 根据数据库类型返回默认数据库名
    String dbType = connection.getType().name().toUpperCase();
    return switch (dbType) {
      case "MYSQL" -> "mysql";
      case "POSTGRESQL" -> "postgres";
      case "H2" -> "test";
      case "ORACLE" -> "ORCL";
      case "SQLSERVER" -> "master";
      default -> "mysql";
    };
  }

  /**
   * 获取驱动类名
   *
   * @param dbType 数据库类型
   * @return 驱动类名
   */
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

  /**
   * 获取连接测试SQL - 根据数据库类型返回不同的测试SQL
   *
   * @param dbType 数据库类型
   * @return 测试SQL
   */
  private String getConnectionTestQuery(String dbType) {
    return switch (dbType) {
      case "ORACLE" -> "SELECT 1 FROM DUAL";
      case "SQLSERVER" -> "SELECT 1";
      default -> "SELECT 1";
    };
  }

  /**
   * 获取连接初始化SQL - 根据数据库类型返回不同的初始化SQL
   *
   * @param dbType 数据库类型
   * @return 初始化SQL，如果不需要则返回null
   */
  private String getConnectionInitSql(String dbType) {
    return switch (dbType) {
      case "MYSQL" -> "SET SESSION wait_timeout=28800";
      default -> null; // 其他数据库不需要初始化SQL
    };
  }

  /**
   * 添加数据库特定属性
   *
   * @param config HikariCP配置
   * @param dbType 数据库类型
   */
  private void addDatabaseSpecificProperties(HikariConfig config, String dbType) {
    switch (dbType) {
      case "MYSQL":
        addMySQLProperties(config);
        break;
      case "POSTGRESQL":
        addPostgreSQLProperties(config);
        break;
      case "ORACLE":
        addOracleProperties(config);
        break;
      case "SQLSERVER":
        addSQLServerProperties(config);
        break;
      case "H2":
        addH2Properties(config);
        break;
      default:
        // 其他数据库使用默认配置
        break;
    }
  }

  /**
   * 生成缓存键 - 使用独立前缀避免冲突
   *
   * @param connection 连接配置
   * @return 缓存键
   */
  private String generateCacheKey(ResourceConnection connection) {
    return String.format(
        "DataBridge_%s_%s_%s_%s_%s",
        connection.getType().name(),
        connection.getHost(),
        connection.getPort(),
        connection.getUsername(),
        getDatabaseName(connection));
  }

  /**
   * 移除指定连接池
   *
   * @param connection 连接配置
   */
  public void removeDataSource(ResourceConnection connection) {
    String cacheKey = generateCacheKey(connection);
    DataSource dataSource = connectionPoolCache.remove(cacheKey);
    if (dataSource instanceof HikariDataSource) {
      try {
        ((HikariDataSource) dataSource).close();
        log.info("已关闭数据桥接连接池: {}", cacheKey);
      } catch (Exception e) {
        log.warn("关闭数据桥接连接池失败: {} - {}", cacheKey, e.getMessage());
      }
    }
  }

  /** 启动保活任务 */
  private void startKeepAliveTask() {
    keepAliveScheduler.scheduleWithFixedDelay(
        () -> {
          try {
            connectionPoolCache.forEach(
                (key, dataSource) -> {
                  if (!isDataSourceHealthy(dataSource)) {
                    log.warn("检测到不健康的连接池: {}", key);
                  }
                });
          } catch (Exception e) {
            log.error("连接池保活检查失败: {}", e.getMessage());
          }
        },
        KEEP_ALIVE_INTERVAL,
        KEEP_ALIVE_INTERVAL,
        TimeUnit.MILLISECONDS);

    log.info("数据桥接连接池保活任务已启动，检查间隔: {}秒", KEEP_ALIVE_INTERVAL / 1000);
  }

  /** 关闭所有连接池 */
  public void shutdown() {
    log.info("开始关闭所有数据桥接连接池...");

    // 关闭保活调度器
    keepAliveScheduler.shutdown();
    try {
      if (!keepAliveScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        keepAliveScheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      keepAliveScheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // 关闭所有连接池
    connectionPoolCache.forEach(
        (key, dataSource) -> {
          try {
            if (dataSource instanceof HikariDataSource) {
              ((HikariDataSource) dataSource).close();
            }
          } catch (Exception e) {
            log.warn("关闭数据桥接连接池失败: {} - {}", key, e.getMessage());
          }
        });
    connectionPoolCache.clear();
    log.info("所有数据桥接连接池已关闭");
  }

  /**
   * 获取连接池状态
   *
   * @return 连接池状态信息
   */
  public String getPoolStatus() {
    StringBuilder status = new StringBuilder();
    status.append("数据桥接连接池状态:\n");
    connectionPoolCache.forEach(
        (key, dataSource) -> {
          if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            status.append(
                String.format(
                    "  %s: 活跃=%d, 空闲=%d, 等待=%d\n",
                    key,
                    hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                    hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                    hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()));
          }
        });
    return status.toString();
  }

  /**
   * 获取连接池数量
   *
   * @return 连接池数量
   */
  public int getPoolCount() {
    return connectionPoolCache.size();
  }
}
