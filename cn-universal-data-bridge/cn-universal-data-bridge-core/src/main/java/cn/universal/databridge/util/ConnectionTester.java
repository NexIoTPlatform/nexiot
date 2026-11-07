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

package cn.universal.databridge.util;

import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.json.JSONUtil;
import cn.universal.databridge.entity.ResourceConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.stereotype.Component;

/**
 * 连接测试器 支持多种类型的连接测试：MySQL、Kafka、MQTT、HTTP等
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component
@Slf4j
public class ConnectionTester {

  /**
   * 测试连接
   *
   * @param connection 资源连接配置
   * @return 测试结果
   */
  public ConnectionTestResult testConnection(ResourceConnection connection) {
    if (connection == null) {
      return ConnectionTestResult.failure("连接配置不能为空");
    }
    try {
      switch (connection.getType()) {
        case MYSQL:
          return testMySQLConnection(connection);
        case POSTGRESQL:
          return testPostgreSQLConnection(connection);
        case H2:
          return testH2Connection(connection);
        case ORACLE:
          return testOracleConnection(connection);
        case SQLSERVER:
          return testSQLServerConnection(connection);
        case KAFKA:
          return testKafkaConnection(connection);
        case MQTT:
          return testMqttConnection(connection);
        case HTTP:
          return testHttpConnection(connection);
        case REDIS:
          return testRedisConnection(connection);
        case IOTDB:
          return testIoTDBConnection(connection);
        case INFLUXDB:
          return testInfluxDBConnection(connection);
        case ELASTICSEARCH:
          return testElasticsearchConnection(connection);
        default:
          return ConnectionTestResult.failure("不支持的连接类型: " + connection.getType());
      }
    } catch (Exception e) {
      log.error(
          "测试连接失败，类型: {}, 主机: {}:{}",
          connection.getType(),
          connection.getHost(),
          connection.getPort(),
          e);
      return ConnectionTestResult.failure("连接测试异常: " + e.getMessage());
    }
  }

  /** 测试MySQL连接 */
  private ConnectionTestResult testMySQLConnection(ResourceConnection connection) {
    try {
      // 构建JDBC URL
      String jdbcUrl =
          String.format(
              "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
              connection.getHost(), connection.getPort(), connection.getDatabaseName());

      // 创建数据源
      try (SimpleDataSource dataSource =
          new SimpleDataSource(jdbcUrl, connection.getUsername(), connection.getPassword())) {
        // 测试连接
        try (Connection conn = dataSource.getConnection()) {
          // 执行简单查询验证连接
          conn.createStatement().executeQuery("SELECT 1");
          return ConnectionTestResult.success("MySQL连接测试成功");
        }
      }
    } catch (SQLException e) {
      log.error("MySQL连接测试失败", e);
      return ConnectionTestResult.failure("MySQL连接失败: " + e.getMessage());
    }
  }

  /** 测试PostgreSQL连接 */
  private ConnectionTestResult testPostgreSQLConnection(ResourceConnection connection) {
    try {
      String jdbcUrl =
          String.format(
              "jdbc:postgresql://%s:%d/%s",
              connection.getHost(), connection.getPort(), connection.getDatabaseName());

      try (SimpleDataSource dataSource =
          new SimpleDataSource(jdbcUrl, connection.getUsername(), connection.getPassword())) {
        try (Connection conn = dataSource.getConnection()) {
          conn.createStatement().executeQuery("SELECT 1");
          return ConnectionTestResult.success("PostgreSQL连接测试成功");
        }
      }
    } catch (SQLException e) {
      log.error("PostgreSQL连接测试失败", e);
      return ConnectionTestResult.failure("PostgreSQL连接失败: " + e.getMessage());
    }
  }

  /** 测试H2连接 */
  private ConnectionTestResult testH2Connection(ResourceConnection connection) {
    try {
      String jdbcUrl =
          String.format(
              "jdbc:h2:tcp://%s:%d/%s",
              connection.getHost(), connection.getPort(), connection.getDatabaseName());

      try (SimpleDataSource dataSource =
          new SimpleDataSource(jdbcUrl, connection.getUsername(), connection.getPassword())) {
        try (Connection conn = dataSource.getConnection()) {
          conn.createStatement().executeQuery("SELECT 1");
          return ConnectionTestResult.success("H2连接测试成功");
        }
      }
    } catch (SQLException e) {
      log.error("H2连接测试失败", e);
      return ConnectionTestResult.failure("H2连接失败: " + e.getMessage());
    }
  }

  /** 测试Oracle连接 */
  private ConnectionTestResult testOracleConnection(ResourceConnection connection) {
    try {
      // Oracle使用SID或Service Name
      String jdbcUrl =
          String.format(
              "jdbc:oracle:thin:@%s:%d:%s",
              connection.getHost(), connection.getPort(), connection.getDatabaseName());

      try (SimpleDataSource dataSource =
          new SimpleDataSource(jdbcUrl, connection.getUsername(), connection.getPassword())) {
        try (Connection conn = dataSource.getConnection()) {
          conn.createStatement().executeQuery("SELECT 1 FROM DUAL");
          return ConnectionTestResult.success("Oracle连接测试成功");
        }
      }
    } catch (SQLException e) {
      log.error("Oracle连接测试失败", e);
      return ConnectionTestResult.failure("Oracle连接失败: " + e.getMessage());
    }
  }

  /** 测试SQL Server连接 */
  private ConnectionTestResult testSQLServerConnection(ResourceConnection connection) {
    try {
      // SQL Server连接URL，添加encrypt=false和trustServerCertificate=true以避免SSL证书验证问题
      String jdbcUrl =
          String.format(
              "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true;characterEncoding=UTF-8",
              connection.getHost(), connection.getPort(), connection.getDatabaseName());

      try (SimpleDataSource dataSource =
          new SimpleDataSource(jdbcUrl, connection.getUsername(), connection.getPassword())) {
        try (Connection conn = dataSource.getConnection()) {
          conn.createStatement().executeQuery("SELECT 1");
          return ConnectionTestResult.success("SQL Server连接测试成功");
        }
      }
    } catch (SQLException e) {
      log.error("SQL Server连接测试失败", e);
      return ConnectionTestResult.failure("SQL Server连接失败: " + e.getMessage());
    }
  }

  /** 测试Kafka连接 */
  private ConnectionTestResult testKafkaConnection(ResourceConnection connection) {
    Properties props = new Properties();
    props.put(
        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
        connection.getHost() + ":" + connection.getPort());
    props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
    props.put(AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 10000);

    // 如果有扩展配置，解析并应用
    if (connection.getExtraConfig() != null && !connection.getExtraConfig().trim().isEmpty()) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> extraConfig = JSONUtil.toBean(connection.getExtraConfig(), Map.class);
        extraConfig.forEach((key, value) -> props.put(key, value.toString()));
      } catch (Exception e) {
        log.warn("解析Kafka扩展配置失败", e);
      }
    }

    try (AdminClient adminClient = AdminClient.create(props)) {
      // 尝试列出topics来验证连接
      ListTopicsResult result = adminClient.listTopics();
      result.names().get(5, TimeUnit.SECONDS);
      return ConnectionTestResult.success("Kafka连接测试成功");
    } catch (Exception e) {
      log.error("Kafka连接测试失败", e);
      return ConnectionTestResult.failure("Kafka连接失败: " + e.getMessage());
    }
  }

  /** 测试MQTT连接 */
  private ConnectionTestResult testMqttConnection(ResourceConnection connection) {
    try {
      String brokerUrl = "tcp://" + connection.getHost() + ":" + connection.getPort();
      String clientId = "test_client_" + System.currentTimeMillis();

      MqttClient client = new MqttClient(brokerUrl, clientId);

      MqttConnectionOptions options = new MqttConnectionOptions();
      options.setCleanStart(true);
      options.setConnectionTimeout(10);
      options.setKeepAliveInterval(60);

      // 如果有用户名密码
      if (connection.getUsername() != null && !connection.getUsername().trim().isEmpty()) {
        options.setUserName(connection.getUsername());
        options.setPassword(connection.getPassword().getBytes());
      }

      // 如果有扩展配置，解析并应用
      if (connection.getExtraConfig() != null && !connection.getExtraConfig().trim().isEmpty()) {
        try {
          @SuppressWarnings("unchecked")
          Map<String, Object> extraConfig = JSONUtil.toBean(connection.getExtraConfig(), Map.class);
          if (extraConfig.containsKey("cleanSession")) {
            options.setCleanStart((Boolean) extraConfig.get("cleanSession"));
          }
          if (extraConfig.containsKey("keepAliveInterval")) {
            options.setKeepAliveInterval(
                ((Number) extraConfig.get("keepAliveInterval")).intValue());
          }
        } catch (Exception e) {
          log.warn("解析MQTT扩展配置失败", e);
        }
      }

      client.connect(options);
      client.disconnect();
      client.close();

      return ConnectionTestResult.success("MQTT连接测试成功");
    } catch (MqttException e) {
      log.error("MQTT连接测试失败", e);
      return ConnectionTestResult.failure("MQTT连接失败: " + e.getMessage());
    }
  }

  /** 测试HTTP连接 */
  private ConnectionTestResult testHttpConnection(ResourceConnection connection) {
    try {
      String url = "http://" + connection.getHost() + ":" + connection.getPort();

      // 如果有数据库名（实际是请求路径），添加到URL
      if (connection.getDatabaseName() != null && !connection.getDatabaseName().trim().isEmpty()) {
        if (!connection.getDatabaseName().startsWith("/")) {
          url += "/";
        }
        url += connection.getDatabaseName();
      }

      @SuppressWarnings("deprecation")
      HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);

      // 如果有扩展配置，解析并应用headers
      if (connection.getExtraConfig() != null && !connection.getExtraConfig().trim().isEmpty()) {
        try {
          @SuppressWarnings("unchecked")
          Map<String, Object> extraConfig = JSONUtil.toBean(connection.getExtraConfig(), Map.class);
          if (extraConfig.containsKey("headers")) {
            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) extraConfig.get("headers");
            headers.forEach(conn::setRequestProperty);
          }
        } catch (Exception e) {
          log.warn("解析HTTP扩展配置失败", e);
        }
      }

      int responseCode = conn.getResponseCode();
      conn.disconnect();

      // HTTP状态码200-299表示成功
      if (responseCode >= 200 && responseCode < 300) {
        return ConnectionTestResult.success("HTTP连接测试成功，状态码: " + responseCode);
      } else {
        return ConnectionTestResult.failure("HTTP连接失败，状态码: " + responseCode);
      }
    } catch (IOException e) {
      log.error("HTTP连接测试失败", e);
      return ConnectionTestResult.failure("HTTP连接失败: " + e.getMessage());
    }
  }

  /** 测试Redis连接 */
  private ConnectionTestResult testRedisConnection(ResourceConnection connection) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(connection.getHost(), connection.getPort()), 5000);

      // 简单的Redis PING命令测试
      socket.getOutputStream().write("PING\r\n".getBytes());
      byte[] buffer = new byte[1024];
      int bytesRead = socket.getInputStream().read(buffer);
      String response = new String(buffer, 0, bytesRead);

      if (response.startsWith("+PONG")) {
        return ConnectionTestResult.success("Redis连接测试成功");
      } else {
        return ConnectionTestResult.failure("Redis连接失败，响应: " + response);
      }
    } catch (IOException e) {
      log.error("Redis连接测试失败", e);
      return ConnectionTestResult.failure("Redis连接失败: " + e.getMessage());
    }
  }

  /** 测试IoTDB连接 */
  private ConnectionTestResult testIoTDBConnection(ResourceConnection connection) {
    try {
      // IoTDB使用JDBC连接，类似MySQL
      String jdbcUrl =
          String.format(
              "jdbc:iotdb://%s:%d/%s",
              connection.getHost(), connection.getPort(), connection.getDatabaseName());

      try (SimpleDataSource dataSource =
          new SimpleDataSource(jdbcUrl, connection.getUsername(), connection.getPassword())) {
        try (Connection conn = dataSource.getConnection()) {
          conn.createStatement().executeQuery("SHOW DATABASES");
          return ConnectionTestResult.success("IoTDB连接测试成功");
        }
      }
    } catch (SQLException e) {
      log.error("IoTDB连接测试失败", e);
      return ConnectionTestResult.failure("IoTDB连接失败: " + e.getMessage());
    }
  }

  /** 测试InfluxDB连接 */
  private ConnectionTestResult testInfluxDBConnection(ResourceConnection connection) {
    try {
      // InfluxDB使用HTTP API
      String url = "http://" + connection.getHost() + ":" + connection.getPort() + "/ping";

      @SuppressWarnings("deprecation")
      HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);

      int responseCode = conn.getResponseCode();
      conn.disconnect();

      if (responseCode == 204) { // InfluxDB ping返回204
        return ConnectionTestResult.success("InfluxDB连接测试成功");
      } else {
        return ConnectionTestResult.failure("InfluxDB连接失败，状态码: " + responseCode);
      }
    } catch (IOException e) {
      log.error("InfluxDB连接测试失败", e);
      return ConnectionTestResult.failure("InfluxDB连接失败: " + e.getMessage());
    }
  }

  /** 测试Elasticsearch连接 */
  private ConnectionTestResult testElasticsearchConnection(ResourceConnection connection) {
    try {
      String url =
          "http://" + connection.getHost() + ":" + connection.getPort() + "/_cluster/health";

      @SuppressWarnings("deprecation")
      HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);
      conn.setRequestProperty("Content-Type", "application/json");

      int responseCode = conn.getResponseCode();
      conn.disconnect();

      if (responseCode == 200) {
        return ConnectionTestResult.success("Elasticsearch连接测试成功");
      } else {
        return ConnectionTestResult.failure("Elasticsearch连接失败，状态码: " + responseCode);
      }
    } catch (IOException e) {
      log.error("Elasticsearch连接测试失败", e);
      return ConnectionTestResult.failure("Elasticsearch连接失败: " + e.getMessage());
    }
  }

  /** 连接测试结果 */
  public static class ConnectionTestResult {
    private final boolean success;
    private final String message;
    private final long timestamp;

    private ConnectionTestResult(boolean success, String message) {
      this.success = success;
      this.message = message;
      this.timestamp = System.currentTimeMillis();
    }

    public static ConnectionTestResult success(String message) {
      return new ConnectionTestResult(true, message);
    }

    public static ConnectionTestResult failure(String message) {
      return new ConnectionTestResult(false, message);
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }
}
