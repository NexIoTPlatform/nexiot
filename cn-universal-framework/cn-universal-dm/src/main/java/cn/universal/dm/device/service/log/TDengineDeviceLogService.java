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

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UPRequest;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.log.writer.DeviceLogEntityWrite;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRequest;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTDeviceLogMetadata;
import cn.universal.persistence.entity.IoTDeviceLogMetadata.IoTDeviceLogMetadataBuilder;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO.Bucket;
import cn.universal.persistence.query.DeviceLogQuery;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import com.taosdata.jdbc.rs.RestfulConnection;
import com.taosdata.jdbc.rs.RestfulResultSet;
import com.taosdata.jdbc.rs.RestfulStatement;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * TDengine 存储日志服务,20260507紧急修复严重BUg
 *
 * <p>使用 HikariCP + Spring JdbcTemplate 实现，提供高性能、稳定的时序数据库访问
 *
 * <p>TDengine 特性：
 *
 * <ul>
 *   <li>超级表(STable) + 子表模式：按 iot_id 自动分区
 *   <li>支持高并发写入，针对时序数据优化
 *   <li>支持 TDengine 2.x 和 3.x 版本
 *   <li>批量写入优化，提升写入性能
 * </ul>
 *
 * <p>通过配置文件动态加载
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/28 16:10
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "tdengine", name = "enable", havingValue = "true")
public class TDengineDeviceLogService extends AbstractIoTDeviceLogService {

  private static final String STORE_POLICY = "tdengine";
  private static final int METADATA_SERIES_KEY_LENGTH = 192;
  private static final String NUMERIC_PATTERN =
      "^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)([eE][+-]?[0-9]+)?$";
  private static final Pattern NUMERIC_CONTENT = Pattern.compile(NUMERIC_PATTERN);

  /** 超级表名称 - 设备日志 */
  @Value("${tdengine.table.iot_device_log:iot_device_log}")
  private String devLogTableName;

  /** 超级表名称 - 设备日志元数据 */
  @Value("${tdengine.table.iot_device_log_metadata:iot_device_log_metadata}")
  private String devLogMetaTableName;

  /** 数据库名称 */
  @Value("${tdengine.database:iot_log}")
  private String databaseName;

  /** TDengine JDBC URL */
  @Value("${tdengine.url:jdbc:TAOS://127.0.0.1:6030/}")
  private String jdbcUrl;

  /** TDengine 用户名 */
  @Value("${tdengine.username:root}")
  private String username;

  /** TDengine 密码 */
  @Value("${tdengine.password:taosdata}")
  private String password;

  /** 连接池最大连接数 */
  @Value("${tdengine.pool.max-size:20}")
  private int maxPoolSize;

  /** 连接池最小空闲连接数 */
  @Value("${tdengine.pool.min-idle:5}")
  private int minIdle;

  /** 连接超时时间(毫秒) */
  @Value("${tdengine.pool.connection-timeout:30000}")
  private long connectionTimeout;

  /** 批量写入大小 */
  @Value("${tdengine.batch.size:1000}")
  private int batchSize;

  /** 数据保留天数（0表示不限制） */
  @Value("${tdengine.keep.days:365}")
  private int keepDays;

  /** TDengine 版本（2 或 3） */
  @Value("${tdengine.version:3}")
  private int tdengineVersion;

  @Resource
  private IoTDeviceService iotDeviceService;

  private HikariDataSource dataSource;
  private HikariDataSource historyDataSource;
  private OkHttpClient historyHttpClient;
  private JdbcTemplate jdbcTemplate;
  private JdbcTemplate historyJdbcTemplate;

  @Value("${tdengine.history.query-timeout-seconds:${nexiot.log.history.query-timeout-seconds:8}}")
  private int historyQueryTimeoutSeconds = 8;

  @Value("${tdengine.history.auto-upgrade-numeric-columns:true}")
  private boolean autoUpgradeNumericColumns = true;

  /** Old rows retain NULL markers and use the guarded content conversion. */
  private volatile boolean numericColumnsAvailable;

  /** 子表缓存，避免重复创建子表 */
  private final ConcurrentHashMap<String, Boolean> subTableCache = new ConcurrentHashMap<>();

  /** 子表创建锁，防止并发创建同一子表 */
  private final ConcurrentHashMap<String, ReentrantLock> subTableLocks = new ConcurrentHashMap<>();

  /**
   * 获取完整的设备日志表名（带数据库前缀）
   *
   * <p>连接池中不同连接的数据库上下文可能不同，必须使用完整表名
   */
  private String getFullLogTableName() {
    return databaseName + "." + devLogTableName;
  }

  /** 获取完整的元数据表名（带数据库前缀） */
  private String getFullMetaTableName() {
    return databaseName + "." + devLogMetaTableName;
  }

  @PostConstruct
  public void initDb() {
    try {
      // 创建 HikariCP 数据源配置
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(jdbcUrl);
      config.setUsername(username);
      config.setPassword(password);

      // TDengine JDBC 驱动类名（根据 URL 类型自动选择）
      // jdbc:TAOS:// → 原生驱动（需安装 TDengine 客户端）
      // jdbc:TAOS-RS:// → RESTful/WebSocket 驱动（无需安装客户端，推荐）
      String driverClass;
      if (jdbcUrl.toLowerCase().contains("taos-rs")) {
        driverClass = "com.taosdata.jdbc.rs.RestfulDriver";
        log.info("TDengine 使用 RESTful/WebSocket 驱动");
      } else {
        driverClass = "com.taosdata.jdbc.TSDBDriver";
        log.info("TDengine 使用原生驱动");
      }
      config.setDriverClassName(driverClass);

      // 连接池配置 - 针对 TDengine 优化
      config.setMaximumPoolSize(maxPoolSize);
      config.setMinimumIdle(minIdle);
      config.setConnectionTimeout(connectionTimeout);
      config.setIdleTimeout(600000); // 10分钟空闲超时
      config.setMaxLifetime(1800000); // 30分钟最大生命周期
      config.setValidationTimeout(5000); // 5秒验证超时
      config.setLeakDetectionThreshold(60000); // 60秒连接泄漏检测

      // 连接池名称
      config.setPoolName("TDengine-Pool");

      // TDengine 特定配置
      config.setAutoCommit(true);
      config.setRegisterMbeans(false); // 关闭JMX监控

      // 创建数据源
      dataSource = new HikariDataSource(config);
      jdbcTemplate = new JdbcTemplate(dataSource);
      if (jdbcUrl.toLowerCase(java.util.Locale.ROOT).startsWith("jdbc:taos-rs:")) {
        historyHttpClient = historyHttpClient();
        historyDataSource = historyRestPool(historyHttpClient);
      }
      historyJdbcTemplate =
          new JdbcTemplate(historyDataSource != null ? historyDataSource : dataSource);
      historyJdbcTemplate.setQueryTimeout(Math.max(1, historyQueryTimeoutSeconds));

      // 创建数据库和表结构
      createDatabaseIfNotExist();
      createTablesIfNotExist();
      upgradeMetadataNumericColumns();

      log.info(
          "初始化 TDengine 数据源成功，连接池配置: maxSize={}, minIdle={}, batchSize={}, version={}",
          maxPoolSize,
          minIdle,
          batchSize,
          tdengineVersion);

    } catch (Exception e) {
      log.error("初始化 TDengine 数据源失败", e);
      destroy();
      throw new RuntimeException("初始化 TDengine 数据源失败", e);
    }
  }

  @PreDestroy
  public void destroy() {
    if (historyHttpClient != null) {
      historyHttpClient.dispatcher().cancelAll();
    }
    if (historyDataSource != null && !historyDataSource.isClosed()) {
      historyDataSource.close();
    }
    if (historyHttpClient != null) {
      historyHttpClient.dispatcher().executorService().shutdown();
      historyHttpClient.connectionPool().evictAll();
    }
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      log.info("TDengine 数据源已关闭");
    }
  }

  OkHttpClient historyHttpClient() {
    int seconds = Math.max(1, historyQueryTimeoutSeconds);
    return new OkHttpClient.Builder()
        .connectTimeout(Math.min(3, seconds), java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(seconds, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(seconds, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build();
  }

  AbstractDataSource historyRestDataSource(OkHttpClient client) {
    // The bundled JDBC driver's REST setQueryTimeout() is a no-op and its socket timeout is
    // JVM-global. An independent client makes the history deadline reliable and leaves writes
    // alone.
    URI address = URI.create(jdbcUrl.replaceFirst("(?i)^jdbc:taos-rs:", "http:"));
    boolean ssl =
        address.getQuery() != null
            && java.util.regex.Pattern.compile("(?i)(^|&)useSSL=true(&|$)")
                .matcher(address.getQuery())
                .find();
    String scheme = ssl ? "https" : "http";
    int port = address.getPort() < 0 ? 6041 : address.getPort();
    if (address.getHost() == null) {
      throw new IllegalArgumentException("TDengine REST URL requires a host");
    }
    String endpoint;
    try {
      endpoint = new URI(scheme, null, address.getHost(), port, "/rest/sql", null, null).toString();
    } catch (java.net.URISyntaxException invalid) {
      throw new IllegalArgumentException("Invalid TDengine REST URL", invalid);
    }
    String authorization = Credentials.basic(username, password);
    return new AbstractDataSource() {
      private volatile int loginTimeout = 3;

      @Override
      public void setLoginTimeout(int seconds) throws SQLException {
        if (seconds < 0) throw new SQLException("Login timeout cannot be negative");
        // Creating this connection has no I/O; its SELECT 1 validation has a real network deadline.
        // Hikari also uses this value when waiting for its connection creator to stop on close.
        loginTimeout = seconds;
      }

      @Override
      public int getLoginTimeout() {
        return loginTimeout;
      }

      @Override
      public java.sql.Connection getConnection() {
        return connection(authorization);
      }

      @Override
      public java.sql.Connection getConnection(String user, String secret) {
        return connection(Credentials.basic(user, secret));
      }

      private java.sql.Connection connection(String auth) {
        return new RestfulConnection(
            address.getHost(),
            Integer.toString(port),
            new Properties(),
            databaseName,
            jdbcUrl,
            auth.substring("Basic ".length()),
            ssl,
            null,
            null) {
          private volatile int networkTimeout;

          @Override
          public Statement createStatement() throws SQLException {
            if (isClosed()) throw new SQLException("Connection is closed");
            return new HistoryRestStatement(this, client, endpoint, auth);
          }

          @Override
          public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds)
              throws SQLException {
            if (milliseconds < 0) throw new SQLException("Network timeout cannot be negative");
            networkTimeout = milliseconds;
          }

          @Override
          public int getNetworkTimeout() {
            return networkTimeout;
          }

          @Override
          public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
            // All history SQL is generated with validated/escaped literals and uses Statement.
            // Never allow the SDK's inherited prepared path to bypass the independent deadline.
            throw new SQLFeatureNotSupportedException("TDengine history supports Statement only");
          }
        };
      }
    };
  }

  HikariDataSource historyRestPool(OkHttpClient client) {
    HikariConfig config = new HikariConfig();
    config.setDataSource(historyRestDataSource(client));
    config.setPoolName("TDengine-History-Pool");
    config.setMaximumPoolSize(Math.max(1, Math.min(4, maxPoolSize)));
    config.setMinimumIdle(0);
    config.setConnectionTimeout(Math.min(3000, Math.max(250, connectionTimeout)));
    config.setValidationTimeout(1000);
    config.setConnectionTestQuery("SELECT 1");
    return new HikariDataSource(config);
  }

  private static final class HistoryRestStatement extends RestfulStatement {
    private final RestfulConnection connection;
    private final OkHttpClient client;
    private final String endpoint;
    private final String authorization;
    private volatile Call active;
    private ResultSet result;
    private int updateCount = -1;
    private int queryTimeoutSeconds;

    private HistoryRestStatement(
        RestfulConnection connection, OkHttpClient client, String endpoint, String authorization) {
      super(connection, connection.getDatabase());
      this.connection = connection;
      this.client = client;
      this.endpoint = endpoint;
      this.authorization = authorization;
    }

    private com.alibaba.fastjson.JSONObject request(String sql) throws SQLException {
      if (isClosed()) throw new SQLException("Statement is closed");
      Request request =
          new Request.Builder()
              .url(endpoint)
              .header("Authorization", authorization)
              .post(RequestBody.create(sql, MediaType.get("text/plain; charset=utf-8")))
              .build();
      Call call = client.newCall(request);
      long timeout = client.callTimeoutMillis();
      if (connection.getNetworkTimeout() > 0) {
        timeout = Math.min(timeout, connection.getNetworkTimeout());
      }
      if (queryTimeoutSeconds > 0) {
        timeout = Math.min(timeout, queryTimeoutSeconds * 1000L);
      }
      call.timeout().timeout(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
      active = call;
      try (Response response = call.execute()) {
        if (!response.isSuccessful() || response.body() == null) {
          throw new SQLException("TDengine历史请求失败，HTTP " + response.code());
        }
        com.alibaba.fastjson.JSONObject data =
            com.alibaba.fastjson.JSON.parseObject(response.body().string());
        if (data == null || !data.containsKey("code")) {
          throw new SQLException("TDengine历史请求返回无效结果");
        }
        if (data.getIntValue("code") != 0) {
          throw new SQLException(data.getString("desc"), null, data.getIntValue("code"));
        }
        return data;
      } catch (InterruptedIOException failure) {
        throw new SQLTimeoutException("TDengine历史请求超过执行时限", "HYT00", failure);
      } catch (IOException failure) {
        throw new SQLTransientConnectionException("TDengine历史请求连接失败", "08006", failure);
      } catch (com.alibaba.fastjson.JSONException failure) {
        throw new SQLException("TDengine历史请求返回无效结果", failure);
      } finally {
        active = null;
      }
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
      result = new RestfulResultSet(connection.getDatabase(), this, request(sql));
      updateCount = -1;
      return result;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
      updateCount = request(sql).getIntValue("rows");
      result = null;
      return updateCount;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
      com.alibaba.fastjson.JSONObject data = request(sql);
      if (data.containsKey("column_meta") && !data.getJSONArray("column_meta").isEmpty()) {
        result = new RestfulResultSet(connection.getDatabase(), this, data);
        updateCount = -1;
        return true;
      }
      updateCount = data.getIntValue("rows");
      result = null;
      return false;
    }

    @Override
    public ResultSet getResultSet() {
      return result;
    }

    @Override
    public int getUpdateCount() {
      return updateCount;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
      if (seconds < 0) throw new SQLException("Query timeout cannot be negative");
      queryTimeoutSeconds = seconds;
    }

    @Override
    public int getQueryTimeout() {
      return queryTimeoutSeconds;
    }

    @Override
    public void cancel() {
      Call call = active;
      if (call != null) call.cancel();
    }

    @Override
    public void close() throws SQLException {
      cancel();
      if (result != null) result.close();
      super.close();
    }
  }

  /** 创建数据库（如果不存在） */
  private void createDatabaseIfNotExist() {
    try {
      String createDbSql;
      if (tdengineVersion >= 3) {
        // TDengine 3.x 语法
        createDbSql =
            String.format(
                "CREATE DATABASE IF NOT EXISTS %s PRECISION 'ms' KEEP %d BUFFER 256",
                databaseName, keepDays > 0 ? keepDays : 3650);
      } else {
        // TDengine 2.x 语法
        createDbSql =
            String.format(
                "CREATE DATABASE IF NOT EXISTS %s KEEP %d UPDATE 1",
                databaseName, keepDays > 0 ? keepDays : 3650);
      }
      jdbcTemplate.execute(createDbSql);
      log.info("TDengine 数据库 {} 检查/创建成功", databaseName);

      // 切换到目标数据库
      jdbcTemplate.execute("USE " + databaseName);
      log.info("TDengine 切换到数据库 {}", databaseName);

    } catch (Exception e) {
      log.error("TDengine 创建数据库失败: {}", databaseName, e);
      throw new RuntimeException("TDengine 创建数据库失败", e);
    }
  }

  /** 创建超级表（如果不存在） */
  private void createTablesIfNotExist() {
    try {
      // 创建设备日志超级表
      // TDengine 超级表：普通列存储经常变化的数据，标签列存储不经常变化的数据
      jdbcTemplate.execute(buildCreateDeviceLogSTableSql());
      log.info("TDengine 超级表 {} 检查/创建成功", getFullLogTableName());

      // 创建设备日志元数据超级表
      jdbcTemplate.execute(buildCreateMetadataSTableSql());
      log.info("TDengine 超级表 {} 检查/创建成功", getFullMetaTableName());

    } catch (Exception e) {
      log.error("TDengine 创建超级表失败", e);
      throw new RuntimeException("TDengine 创建超级表失败", e);
    }
  }

  private String buildCreateDeviceLogSTableSql() {
    return String.format(
        """
        CREATE STABLE IF NOT EXISTS %s (
            ts TIMESTAMP,
            id BIGINT,
            device_name NCHAR(64),
            message_type NCHAR(32),
            command_id NCHAR(64),
            command_status TINYINT,
            event NCHAR(128),
            content NCHAR(4096),
            point NCHAR(256)
        ) TAGS (
            iot_id NCHAR(256),
            device_id NCHAR(128),
            product_key NCHAR(128)
        )
        """,
        getFullLogTableName());
  }

  private String buildCreateMetadataSTableSql() {
    String seriesKeyColumn =
        supportsMetadataCompositeKey()
            ? String.format("series_key VARCHAR(%d) COMPOSITE KEY,", METADATA_SERIES_KEY_LENGTH)
            : "";
    return String.format(
        """
        CREATE STABLE IF NOT EXISTS %s (
            ts TIMESTAMP,
            %s
            id BIGINT,
            device_name NCHAR(64),
            message_type NCHAR(32),
            event NCHAR(128),
            property NCHAR(64),
            content NCHAR(4096),
            ext1 NCHAR(512),
            ext2 NCHAR(1024),
            ext3 NCHAR(512),
            numeric_value DOUBLE,
            numeric_valid TINYINT
        ) TAGS (
            iot_id NCHAR(256),
            device_id NCHAR(128),
            product_key NCHAR(128)
        )
        """,
        getFullMetaTableName(), seriesKeyColumn);
  }

  /**
   * Add nullable columns only: existing rows are neither rewritten nor synchronously backfilled. If
   * a read-only account cannot upgrade, legacy reads/writes remain compatible. A second DESCRIBE
   * also handles another application instance adding the same column concurrently.
   */
  private void upgradeMetadataNumericColumns() {
    try {
      List<String> columns = metadataColumns();
      if (autoUpgradeNumericColumns) {
        for (String definition : List.of("numeric_value DOUBLE", "numeric_valid TINYINT")) {
          String name = definition.substring(0, definition.indexOf(' '));
          if (!columns.contains(name)) {
            try {
              historyJdbc()
                  .execute("ALTER STABLE " + getFullMetaTableName() + " ADD COLUMN " + definition);
            } catch (RuntimeException failure) {
              if (!metadataColumns().contains(name)) {
                throw failure;
              }
            }
          }
        }
        columns = metadataColumns();
      }
      numericColumnsAvailable =
          columns.contains("numeric_value") && columns.contains("numeric_valid");
      log.info(
          "TDengine metadata numeric columns ready={}, automaticUpgrade={}",
          numericColumnsAvailable,
          autoUpgradeNumericColumns);
    } catch (RuntimeException failure) {
      numericColumnsAvailable = false;
      log.warn("TDengine 数值列自动升级未完成，保留历史兼容查询/写入；请检查DDL权限", failure);
    }
  }

  private List<String> metadataColumns() {
    return historyJdbc()
        .query(
            "DESCRIBE " + getFullMetaTableName(),
            (rs, rowNum) -> rs.getString("field").toLowerCase(java.util.Locale.ROOT));
  }

  private JdbcTemplate historyJdbc() {
    return historyJdbcTemplate != null ? historyJdbcTemplate : jdbcTemplate;
  }

  private boolean supportsMetadataCompositeKey() {
    return tdengineVersion >= 3;
  }

  /**
   * 获取或创建子表
   *
   * <p>子表命名规则：{超级表名}_{iot_id的短哈希}
   *
   * @param stableName 超级表名称
   * @param iotId IoT ID
   * @param deviceId 设备ID
   * @param productKey 产品Key
   * @return 子表名称
   */
  private String getOrCreateSubTable(
      String stableName, String iotId, String deviceId, String productKey) {
    // 生成子表名称（使用 iotId 的哈希值，确保合法命名）
    String subTableName = generateSubTableName(stableName, iotId);

    // 检查缓存
    if (subTableCache.containsKey(subTableName)) {
      return subTableName;
    }

    // 获取或创建锁
    ReentrantLock lock =
        subTableLocks.computeIfAbsent(subTableName, k -> new ReentrantLock());
    lock.lock();
    try {
      // 双重检查
      if (subTableCache.containsKey(subTableName)) {
        return subTableName;
      }

      // 创建子表（使用 USING 语法自动创建）
      // TDengine 的 INSERT 语句可以自动创建子表，所以这里只需要标记缓存
      subTableCache.put(subTableName, Boolean.TRUE);
      log.debug("TDengine 子表 {} 已标记（将在写入时自动创建）", subTableName);

      return subTableName;
    } finally {
      lock.unlock();
    }
  }

  /**
   * 生成子表名称
   *
   * <p>TDengine 表名规则：只能包含字母、数字、下划线，不能以数字开头
   *
   * <p>重要：不同超级表的子表名必须唯一，使用简短前缀区分
   */
  private String generateSubTableName(String stableName, String iotId) {
    String hash = shortStableHash(stableName + "::" + iotId);
    // 根据超级表名称生成不同前缀，避免子表名冲突
    String prefix;
    if (stableName.contains("metadata")) {
      prefix = "meta";
    } else {
      prefix = "log";
    }
    // 确保不以数字开头，格式: t_{prefix}_{hash}
    return "t_" + prefix + "_" + hash;
  }

  private String shortStableHash(String value) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(24);
      for (int index = 0; index < digest.length && builder.length() < 24; index++) {
        builder.append(String.format("%02x", digest[index]));
      }
      return builder.toString();
    } catch (Exception e) {
      return Integer.toHexString(Math.abs(value.hashCode()));
    }
  }

  /**
   * 转义字符串，防止SQL注入
   *
   * <p>TDengine 使用单引号包裹字符串
   */
  private String escapeString(String value) {
    if (value == null) {
      return "";
    }
    // 转义单引号和反斜杠
    return value.replace("\\", "\\\\").replace("'", "\\'");
  }

  @Override
  @Async("taskExecutor")
  public void saveDeviceLog(BaseUPRequest upRequest, IoTDeviceDTO noUse, IoTProduct ioTProduct) {
    saveDeviceLogDirect(upRequest, noUse, ioTProduct);
  }

  @Override
  public void saveDeviceLogDirect(
      BaseUPRequest upRequest, IoTDeviceDTO noUse, IoTProduct ioTProduct) {
    // 产品数据存储策略，不为空则保存日志
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      IoTDeviceDTO ioTDeviceDTO = resolveDevice(upRequest, noUse, iotDeviceService);
      try {
        if (ioTDeviceDTO == null) {
          log.warn(
              "TDengine保存设备日志跳过，设备不存在 productKey={}, deviceId={}",
              upRequest.getProductKey(),
              upRequest.getDeviceId());
          return;
        }
        IoTDeviceLog ioTDeviceLog = build(upRequest, ioTDeviceDTO);
        insertDeviceLog(ioTDeviceLog);
          log.debug("TDengine 插入设备日志成功, iotId={}", upRequest.getIotId());
      } catch (Exception e) {
        log.error("TDengine 保存设备日志报错", e);
      }

      // 处理存储策略配置
      String storePolicyConfiguration = ioTProduct.getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)) {
          LogStorePolicyDTO productLogStorePolicy = resolveLogStorePolicy(ioTProduct);
          saveLogStorePolicy(productLogStorePolicy, upRequest, ioTProduct);
        }
      } catch (Exception e) {
        log.error("TDengine 保存设备属性扩展日志报错", e);
      }
    }
  }

  @Override
  public void flushDeviceLogRequests(List<DeviceLogWriteRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return;
    }
    List<IoTDeviceLog> logs = new ArrayList<>(requests.size());
    List<DeviceLogWriteRequest> metadataRequests = new ArrayList<>(requests.size());
    for (DeviceLogWriteRequest request : requests) {
      if (request == null || request.upRequest() == null || request.ioTProduct() == null) {
        continue;
      }
      IoTProduct product = request.ioTProduct();
      if (StrUtil.isBlank(product.getStorePolicy())) {
        continue;
      }
      IoTDeviceDTO device = resolveDevice(request.upRequest(), request.ioTDeviceDTO(), iotDeviceService);
      if (device == null) {
        log.warn(
            "TDengine批量保存设备日志跳过，设备不存在 productKey={}, deviceId={}",
            request.upRequest().getProductKey(),
            request.upRequest().getDeviceId());
        continue;
      }
      logs.add(build(request.upRequest(), device));
      metadataRequests.add(request);
    }
    batchInsertDeviceLogs(logs);
    for (DeviceLogWriteRequest request : metadataRequests) {
      String storePolicyConfiguration = request.ioTProduct().getStorePolicyConfiguration();
      try {
        if (StrUtil.isNotBlank(storePolicyConfiguration)) {
          LogStorePolicyDTO productLogStorePolicy = resolveLogStorePolicy(request.ioTProduct());
          saveLogStorePolicy(productLogStorePolicy, request.upRequest(), request.ioTProduct());
        }
      } catch (Exception e) {
        log.error("TDengine 保存设备属性扩展日志报错", e);
      }
    }
  }

  @Override
  public void saveDeviceLog(
      IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {
    // 产品数据存储策略，不为空则保存日志
    if (StrUtil.isNotBlank(ioTProduct.getStorePolicy())) {
      try {
        insertDeviceLog(ioTDeviceLog);
        log.debug("TDengine 插入设备日志成功");
      } catch (Exception e) {
        log.error("TDengine 保存设备日志报错", e);
      }
    }
  }

  @Override
  public void flushDeviceLogEntities(List<DeviceLogEntityWrite> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    List<IoTDeviceLog> logs = new ArrayList<>(entities.size());
    for (DeviceLogEntityWrite entity : entities) {
      if (entity == null || entity.ioTDeviceLog() == null || entity.ioTProduct() == null) {
        continue;
      }
      if (StrUtil.isBlank(entity.ioTProduct().getStorePolicy())) {
        continue;
      }
      logs.add(ensureLogIdentity(entity.ioTDeviceLog()));
    }
    batchInsertDeviceLogs(logs);
  }

  /** 插入设备日志到 TDengine */
  private void insertDeviceLog(IoTDeviceLog deviceLog) {
    String subTableName =
        generateSubTableName(devLogTableName, deviceLog.getIotId());

    // TDengine INSERT 语法（自动创建子表）
    // INSERT INTO <数据库.子表名> USING <数据库.超级表名> TAGS (...) VALUES (...)
    String sql =
        String.format(
            """
            INSERT INTO %s.%s USING %s TAGS ('%s', '%s', '%s')
            VALUES (%d, %d, '%s', '%s', '%s', %d, '%s', '%s', '%s')
            """,
            databaseName,
            subTableName,
            getFullLogTableName(),
            escapeString(deviceLog.getIotId()),
            escapeString(deviceLog.getDeviceId()),
            escapeString(deviceLog.getProductKey()),
            // VALUES
            deviceLog.getCreateTime() != null
                ? deviceLog
                    .getCreateTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                : System.currentTimeMillis(),
            deviceLog.getId() != null ? deviceLog.getId() : IdUtil.getSnowflake().nextId(),
            escapeString(deviceLog.getDeviceName()),
            escapeString(deviceLog.getMessageType()),
            escapeString(deviceLog.getCommandId()),
            deviceLog.getCommandStatus() != null ? deviceLog.getCommandStatus() : 0,
            escapeString(deviceLog.getEvent()),
            escapeString(deviceLog.getContent()),
            escapeString(deviceLog.getPoint()));

    try {
      debugRawInsertSql(STORE_POLICY, sql);
      jdbcTemplate.execute(sql);
    } catch (Exception e) {
      log.error("TDengine 插入设备日志失败: sql={}", sql, e);
      throw e;
    }
  }

  /** 根据存储策略保存元数据日志 */
  private void saveLogStorePolicy(
      LogStorePolicyDTO logStorePolicyDTO, UPRequest up, IoTProduct ioTProduct) {
    LogStorePolicyDTO effectivePolicy =
        logStorePolicyDTO == null ? LogStorePolicyDTO.builder().build() : logStorePolicyDTO;
    List<IoTDeviceLogMetadata> metadataRecords = new ArrayList<>();
    DeviceMetadata deviceMetadata = getDeviceMetadata(ioTProduct.getMetadata());

    // 处理属性消息
    if (MessageType.PROPERTIES.equals(up.getMessageType())
        && up.getProperties() != null
        && MapUtil.isNotEmpty(effectivePolicy.getProperties())) {
      up.getProperties()
          .forEach(
              (key, value) -> {
                if (effectivePolicy.getProperties().containsKey(key)) {
                  AbstractPropertyMetadata propertyOrNull =
                      deviceMetadata.getPropertyOrNull(key);
                  IoTDevicePropertiesBO ioTDevicePropertiesBO = new IoTDevicePropertiesBO();
                  ioTDevicePropertiesBO.withValue(propertyOrNull, value);

                  IoTDeviceLogMetadataBuilder builder = builder(up);
                  builder.id(IdUtil.getSnowflake().nextId());
                  builder.property(key);
                  builder.content(metadataValueText(value));
                  builder.ext1(ioTDevicePropertiesBO.getPropertyName());
                  builder.ext2(ioTDevicePropertiesBO.getFormatValue());
                  builder.ext3(ioTDevicePropertiesBO.getSymbol());

                  metadataRecords.add(builder.build());
                }
              });
    }

    // 处理事件消息
    if (MessageType.EVENT.equals(up.getMessageType())
        && MapUtil.isNotEmpty(effectivePolicy.getEvent())
        && effectivePolicy.getEvent().containsKey(up.getEvent())) {

      IoTDeviceLogMetadataBuilder builder = builder(up);
      builder.id(IdUtil.getSnowflake().nextId());
      builder.event(up.getEvent());
      builder.content(up.getEventName());

      metadataRecords.add(builder.build());
    }

    if (metadataRecords.isEmpty()) {
      return;
    }

    try {
      batchInsertMetadataRecords(metadataRecords);
      log.debug("TDengine 批量插入元数据成功，数量={}", metadataRecords.size());
    } catch (Exception e) {
      log.warn("TDengine 批量插入元数据失败，回退逐条执行: iotId={}", up.getIotId(), e);
      for (IoTDeviceLogMetadata metadata : metadataRecords) {
        try {
          insertMetadataRecord(metadata);
        } catch (Exception ex) {
          log.error(
              "TDengine 逐条插入元数据失败: iotId={}, property={}, event={}",
              metadata.getIotId(),
              metadata.getProperty(),
              metadata.getEvent(),
              ex);
        }
      }
    }
  }

  private void batchInsertMetadataRecords(List<IoTDeviceLogMetadata> metadataRecords) {
    if (metadataRecords == null || metadataRecords.isEmpty()) {
      return;
    }

    Map<String, List<IoTDeviceLogMetadata>> recordsBySubTable = new LinkedHashMap<>();
    for (IoTDeviceLogMetadata metadata : metadataRecords) {
      recordsBySubTable
          .computeIfAbsent(generateMetadataSubTableName(metadata), ignored -> new ArrayList<>())
          .add(metadata);
    }

    int effectiveBatchSize = batchSize > 0 ? batchSize : metadataRecords.size();
    for (Map.Entry<String, List<IoTDeviceLogMetadata>> entry : recordsBySubTable.entrySet()) {
      List<IoTDeviceLogMetadata> groupRecords = entry.getValue();
      for (int start = 0; start < groupRecords.size(); start += effectiveBatchSize) {
        int end = Math.min(start + effectiveBatchSize, groupRecords.size());
        List<IoTDeviceLogMetadata> batch = groupRecords.subList(start, end);
        String batchSql = buildMetadataBatchSql(entry.getKey(), batch);
        debugRawInsertSql(STORE_POLICY, batchSql);
        jdbcTemplate.execute(batchSql);
      }
    }
  }

  private String buildMetadataBatchSql(
      String subTableName, List<IoTDeviceLogMetadata> metadataRecords) {
    IoTDeviceLogMetadata firstMetadata = metadataRecords.get(0);
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ")
        .append(databaseName)
        .append(".")
        .append(subTableName)
        .append(" USING ")
        .append(getFullMetaTableName())
        .append(" TAGS ('")
        .append(escapeString(firstMetadata.getIotId()))
        .append("', '")
        .append(escapeString(firstMetadata.getDeviceId()))
        .append("', '")
        .append(escapeString(firstMetadata.getProductKey()))
        .append("') ")
        .append(buildMetadataInsertColumns())
        .append(" VALUES ");

    boolean first = true;
    for (IoTDeviceLogMetadata metadata : metadataRecords) {
      if (!first) {
        sql.append(" ");
      }
      first = false;
      appendMetadataValueTuple(sql, metadata);
    }
    return sql.toString();
  }

  private void insertMetadataRecord(IoTDeviceLogMetadata metadata) {
    String subTableName = generateMetadataSubTableName(metadata);
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ")
        .append(databaseName)
        .append(".")
        .append(subTableName)
        .append(" USING ")
        .append(getFullMetaTableName())
        .append(" TAGS ('")
        .append(escapeString(metadata.getIotId()))
        .append("', '")
        .append(escapeString(metadata.getDeviceId()))
        .append("', '")
        .append(escapeString(metadata.getProductKey()))
        .append("') ")
        .append(buildMetadataInsertColumns())
        .append(" VALUES ");
    appendMetadataValueTuple(sql, metadata);
    String insertSql = sql.toString();
    debugRawInsertSql(STORE_POLICY, insertSql);
    jdbcTemplate.execute(insertSql);
  }

  private String buildMetadataInsertColumns() {
    return "(ts, "
        + (supportsMetadataCompositeKey() ? "series_key, " : "")
        + "id, device_name, message_type, event, property, content, ext1, ext2, ext3"
        + (numericColumnsAvailable ? ", numeric_value, numeric_valid" : "")
        + ")";
  }

  private String generateMetadataSubTableName(IoTDeviceLogMetadata metadata) {
    String seriesKey =
        supportsMetadataCompositeKey()
            ? StrUtil.nullToEmpty(metadata.getIotId())
            : buildMetadataSubTableKey(metadata);
    return generateSubTableName(devLogMetaTableName, seriesKey);
  }

  private String buildMetadataSubTableKey(IoTDeviceLogMetadata metadata) {
    return StrUtil.nullToEmpty(metadata.getIotId()) + "::" + buildMetadataSeriesKey(metadata);
  }

  private String buildMetadataSeriesKey(IoTDeviceLogMetadata metadata) {
    if (StrUtil.isNotBlank(metadata.getProperty())) {
      return StrUtil.sub("p:" + metadata.getProperty(), 0, METADATA_SERIES_KEY_LENGTH);
    }
    if (StrUtil.isNotBlank(metadata.getEvent())) {
      return StrUtil.sub("e:" + metadata.getEvent(), 0, METADATA_SERIES_KEY_LENGTH);
    }
    return StrUtil.sub(
        "m:" + StrUtil.nullToEmpty(metadata.getMessageType()), 0, METADATA_SERIES_KEY_LENGTH);
  }

  private void appendMetadataValueTuple(StringBuilder sql, IoTDeviceLogMetadata metadata) {
    sql.append("(")
        .append(
            metadata.getCreateTime() != null
                ? metadata.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis())
        .append(", ");
    if (supportsMetadataCompositeKey()) {
      sql.append("'")
          .append(escapeString(buildMetadataSeriesKey(metadata)))
          .append("', ");
    }
    sql.append(metadata.getId())
        .append(", '")
        .append(escapeString(metadata.getDeviceName()))
        .append("', '")
        .append(escapeString(metadata.getMessageType()))
        .append("', '")
        .append(escapeString(metadata.getEvent()))
        .append("', '")
        .append(escapeString(metadata.getProperty()))
        .append("', '")
        .append(escapeString(metadata.getContent()))
        .append("', '")
        .append(escapeString(metadata.getExt1()))
        .append("', '")
        .append(escapeString(metadata.getExt2()))
        .append("', '")
        .append(escapeString(metadata.getExt3()))
        .append("'");
    if (numericColumnsAvailable) {
      Double number = numericContent(metadata.getContent());
      sql.append(", ")
          .append(number == null ? "NULL" : number.toString())
          .append(", ")
          .append(number == null ? 0 : 1);
    }
    sql.append(")");
  }

  private Double numericContent(String content) {
    if (content == null || !NUMERIC_CONTENT.matcher(content.trim()).matches()) {
      return null;
    }
    try {
      double number = Double.parseDouble(content.trim());
      return Double.isFinite(number) ? number : null;
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  @Override
  public PageBean<IoTDeviceLogVO> pageList(LogQuery query) {
    String where = historyWhere(query, false);
    String projection =
        "SELECT ts, id, device_name, message_type, command_id, command_status, "
            + "event, content, point, iot_id, device_id, product_key FROM "
            + getFullLogTableName();
    try {
      if (query.isCursorMode()) {
        List<IoTDeviceLogVO> rows =
            historyJdbc()
                .query(
                    projection
                        + where
                        + cursorPredicate(query)
                        + " ORDER BY ts DESC, id DESC LIMIT "
                        + (query.getPageSize() + 1),
                    new DeviceLogRowMapper());
        return HistoryPageSupport.page(
            rows, query, IoTDeviceLogVO::getCreateTime, IoTDeviceLogVO::getId);
      }
      Long total =
          historyJdbc()
              .queryForObject("SELECT COUNT(*) FROM " + getFullLogTableName() + where, Long.class);
      long offset = (long) (query.getPageNum() - 1) * query.getPageSize();
      List<IoTDeviceLogVO> rows =
          historyJdbc()
              .query(
                  projection
                      + where
                      + " ORDER BY ts DESC, id DESC LIMIT "
                      + query.getPageSize()
                      + " OFFSET "
                      + offset,
                  new DeviceLogRowMapper());
      return new PageBean<>(
          rows, total == null ? 0L : total, query.getPageSize(), query.getPageNum());
    } catch (RuntimeException failure) {
      log.warn("TDengine 日志查询失败 iotId={}", query.getIotId(), failure);
      throw new IoTException("TDengine日志查询失败或超时，请缩小时间范围后重试");
    }
  }

  /**
   * The first TAG (iot_id) uses TDengine's tag index to prune child tables, including legacy names.
   */
  private String historyWhere(LogQuery query, boolean metadata) {
    StringBuilder sql = new StringBuilder(" WHERE 1=1");
    appendEquals(sql, "iot_id", query.getIotId());
    appendEquals(sql, "product_key", query.getProductKey());
    appendEquals(sql, "device_id", query.getDeviceId());
    appendEquals(sql, "message_type", query.getMessageType());
    appendEquals(sql, "event", query.getEvent());
    if (StrUtil.isNotBlank(query.getId())) {
      try {
        sql.append(" AND id = ").append(Long.parseLong(query.getId()));
      } catch (NumberFormatException failure) {
        throw new IoTException("消息ID格式不正确", 400);
      }
    }
    if (metadata) {
      appendEquals(sql, "property", query.getProperty());
    }
    if (StrUtil.isNotBlank(query.getDeviceName())) {
      sql.append(" AND device_name LIKE '%")
          .append(escapeString(query.getDeviceName()))
          .append("%'");
    }
    if (query.getBeginCreateTime() != null) {
      sql.append(" AND ts >= ").append(Math.multiplyExact(query.getBeginCreateTime(), 1000L));
    }
    if (query.getEndCreateTime() != null) {
      sql.append(" AND ts <= ").append(Math.multiplyExact(query.getEndCreateTime(), 1000L));
    }
    if (!metadata && MapUtil.isNotEmpty(query.getParams())) {
      if (query.getParams().get("properties") != null) {
        sql.append(" AND content LIKE '%")
            .append(escapeString(String.valueOf(query.getParams().get("properties"))))
            .append("%'");
      }
      if (query.getParams().get("event") != null) {
        appendEquals(sql, "event", String.valueOf(query.getParams().get("event")));
      }
    }
    return sql.toString();
  }

  private void appendEquals(StringBuilder sql, String column, String value) {
    if (StrUtil.isNotBlank(value)) {
      sql.append(" AND ").append(column).append(" = '").append(escapeString(value)).append("'");
    }
  }

  private String cursorPredicate(LogQuery query) {
    if (query.getBeforeTime() == null) {
      return "";
    }
    long beforeMillis =
        query.getBeforeTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    return " AND (ts < "
        + beforeMillis
        + " OR (ts = "
        + beforeMillis
        + " AND id < "
        + query.getBeforeId()
        + "))";
  }

  @Override
  public IoTDeviceLogVO queryById(LogQuery logQuery) {
    if (logQuery == null || StrUtil.isBlank(logQuery.getId())) {
      throw new IoTException("消息ID不能为空", 400);
    }
    try {
      Long.parseLong(logQuery.getId());
    } catch (NumberFormatException failure) {
      throw new IoTException("消息ID格式不正确", 400, failure);
    }
    String sql =
        "SELECT ts, id, device_name, message_type, command_id, command_status, "
            + "event, content, point, iot_id, device_id, product_key FROM "
            + getFullLogTableName()
            + historyWhere(logQuery, false)
            + " LIMIT 1";

    try {
      List<IoTDeviceLogVO> list = historyJdbc().query(sql, new DeviceLogRowMapper());
      return list.isEmpty() ? null : list.get(0);
    } catch (RuntimeException failure) {
      log.warn("TDengine 根据ID查询日志失败或超时 id={}", logQuery.getId(), failure);
      throw new IoTException("TDengine根据ID查询日志失败或超时，请稍后重试", 503, failure);
    }
  }

  @Override
  public PageBean<IoTDeviceEvents> queryEventTotal(String productKey, String iotId) {
    List<IoTDeviceEvents> list = selectDevEvents(productKey);
    long deadline =
        System.nanoTime()
            + java.util.concurrent.TimeUnit.SECONDS.toNanos(
                Math.max(1, historyQueryTimeoutSeconds));

    for (IoTDeviceEvents devEvent : list) {
      if (System.nanoTime() >= deadline) throw new IoTException("事件统计查询超时，请稍后重试", 503);
      String sql =
          String.format(
              """
              SELECT ts
              FROM %s
              WHERE event = '%s' AND message_type = 'EVENT' AND iot_id = '%s' AND product_key = '%s'
              ORDER BY ts DESC LIMIT 100
              """,
              getFullLogTableName(),
              escapeString(devEvent.getId()),
              escapeString(iotId),
              escapeString(productKey));

      try {
        List<Map<String, Object>> rows = historyJdbc().queryForList(sql);
        int qty = rows.size();
        devEvent.setQty(qty >= 100 ? "99+" : String.valueOf(qty));
        devEvent.setTime(rows.isEmpty() ? null : formatEventTime(rows.getFirst().get("ts")));
      } catch (Exception e) {
        log.warn("TDengine 查询事件统计错误: event={}", devEvent.getId(), e);
        throw new IoTException("事件统计查询失败，请稍后重试", 503, e);
      }
    }

    return new PageBean<>(list, (long) list.size(), 1, 100);
  }

  private String formatEventTime(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof LocalDateTime dateTime) {
      return dateTime.toString();
    }

    if (value instanceof Timestamp timestamp) {
      return timestamp.toLocalDateTime().toString();
    }

    if (value instanceof Date date) {
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toString();
    }

    if (value instanceof Number number) {
      return LocalDateTime.ofInstant(
              Instant.ofEpochMilli(number.longValue()), ZoneId.systemDefault())
          .toString();
    }

    if (value instanceof CharSequence text) {
      return text.toString();
    }

    return String.valueOf(value);
  }

  @Override
  public PageBean<IoTDeviceLogMetadataVO> queryLogMeta(LogQuery query) {
    String where = historyWhere(query, true);
    String projection =
        "SELECT ts, id, device_name, message_type, event, property, "
            + "content, ext1, ext2, ext3, iot_id, device_id, product_key FROM "
            + getFullMetaTableName();
    try {
      if (query.isCursorMode()) {
        long deadline =
            System.nanoTime()
                + java.util.concurrent.TimeUnit.SECONDS.toNanos(
                    Math.max(1, historyQueryTimeoutSeconds));
        List<IoTDeviceLogMetadataVO> rows =
            historyJdbc()
                .execute(
                    (org.springframework.jdbc.core.ConnectionCallback<List<IoTDeviceLogMetadataVO>>)
                        connection -> {
                          int originalTimeout;
                          try {
                            originalTimeout = connection.getNetworkTimeout();
                          } catch (SQLFeatureNotSupportedException unsupported) {
                            originalTimeout = -1;
                          }
                          Throwable primaryFailure = null;
                          try {
                            return cursorMetadataRows(
                                connection,
                                projection + where + cursorPredicate(query),
                                query,
                                deadline);
                          } catch (SQLException | RuntimeException failure) {
                            primaryFailure = failure;
                            throw failure;
                          } finally {
                            try {
                              if (originalTimeout >= 0 && !connection.isClosed()) {
                                setHistoryNetworkTimeout(connection, originalTimeout);
                              }
                            } catch (SQLException restoreFailure) {
                              if (primaryFailure == null) throw restoreFailure;
                              primaryFailure.addSuppressed(restoreFailure);
                            }
                          }
                        });
        return HistoryPageSupport.page(
            rows, query, IoTDeviceLogMetadataVO::getCreateTime, IoTDeviceLogMetadataVO::getId);
      }
      Long total =
          historyJdbc()
              .queryForObject("SELECT COUNT(*) FROM " + getFullMetaTableName() + where, Long.class);
      long offset = (long) (query.getPageNum() - 1) * query.getPageSize();
      List<IoTDeviceLogMetadataVO> rows =
          historyJdbc()
              .query(
                  projection
                      + where
                      + " ORDER BY ts DESC, id DESC LIMIT "
                      + query.getPageSize()
                      + " OFFSET "
                      + offset,
                  new DeviceLogMetadataRowMapper());
      return new PageBean<>(
          rows, total == null ? 0L : total, query.getPageSize(), query.getPageNum());
    } catch (RuntimeException failure) {
      log.warn("TDengine 属性日志查询失败 iotId={}", query.getIotId(), failure);
      throw new IoTException("TDengine属性记录查询失败或超时，请缩小时间范围后重试", 503, failure);
    }
  }

  private List<IoTDeviceLogMetadataVO> cursorMetadataRows(
      Connection connection, String sql, LogQuery query, long deadline) throws SQLException {
    int fetch = query.getPageSize() + 1;
    // An id secondary sort disables TDengine's native descending timestamp scan. First seek the
    // latest timestamps; only sort rows at the final timestamp in the engine, preserving all ties.
    List<IoTDeviceLogMetadataVO> rows =
        boundedMetadataRows(connection, sql + " ORDER BY ts DESC LIMIT " + fetch, deadline, fetch);
    if (rows.size() == fetch) {
      LocalDateTime boundary = rows.getLast().getCreateTime();
      long millis = boundary.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
      rows.removeIf(row -> boundary.equals(row.getCreateTime()));
      rows.addAll(
          boundedMetadataRows(
              connection,
              sql + " AND ts = " + millis + " ORDER BY id DESC LIMIT " + fetch,
              deadline,
              fetch));
    }
    rows.sort(
        Comparator.comparing(IoTDeviceLogMetadataVO::getCreateTime)
            .thenComparing(IoTDeviceLogMetadataVO::getId)
            .reversed());
    return rows.size() > fetch ? new ArrayList<>(rows.subList(0, fetch)) : rows;
  }

  private List<IoTDeviceLogMetadataVO> boundedMetadataRows(
      Connection connection, String sql, long deadline, int fetch) throws SQLException {
    long remaining =
        java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
    if (remaining <= 0) {
      throw new SQLTimeoutException("TDengine属性记录查询超过执行时限", "HYT00");
    }
    setHistoryNetworkTimeout(connection, (int) Math.min(Integer.MAX_VALUE, remaining));
    try (Statement statement = connection.createStatement()) {
      statement.setQueryTimeout((int) Math.max(1, (remaining + 999) / 1000));
      try (ResultSet result = statement.executeQuery(sql)) {
        List<IoTDeviceLogMetadataVO> rows = new ArrayList<>();
        DeviceLogMetadataRowMapper mapper = new DeviceLogMetadataRowMapper();
        while (result.next()) {
          if (rows.size() >= fetch) {
            throw new SQLException("TDengine属性记录返回超过请求上限");
          }
          rows.add(mapper.mapRow(result, rows.size()));
        }
        if (System.nanoTime() >= deadline) {
          throw new SQLTimeoutException("TDengine属性记录查询超过执行时限", "HYT00");
        }
        return rows;
      }
    }
  }

  private void setHistoryNetworkTimeout(Connection connection, int milliseconds)
      throws SQLException {
    try {
      connection.setNetworkTimeout(Runnable::run, milliseconds);
    } catch (SQLFeatureNotSupportedException unsupported) {
      // Native/future JDBC drivers may support only the Statement timeout; REST overrides both.
    }
  }

  @Override
  public PropertyTrendVO queryPropertyTrend(PropertyTrendQuery query) {
    query.validateRange();
    if (query.isSampledQuery()) {
      return querySampledTrend(query);
    }
    String where = historyWhere(query, true);
    long interval = query.getIntervalSeconds();
    long offset = Math.floorMod(query.getBeginCreateTime(), interval);
    String window =
        " INTERVAL("
            + interval
            + "s"
            + (offset == 0 ? "" : ", " + offset + "s")
            + ") ORDER BY _wstart ASC LIMIT "
            + (query.getMaxPoints() + 1);
    try {
      List<Bucket> buckets;
      if (tdengineVersion >= 3) {
        String legacyNumber =
            "CASE WHEN TRIM(content) MATCH '"
                + NUMERIC_PATTERN
                + "' THEN CAST(TRIM(content) AS DOUBLE) ELSE NULL END";
        String number =
            numericColumnsAvailable
                ? "CASE WHEN numeric_valid = 1 THEN numeric_value WHEN numeric_valid = 0 THEN NULL"
                      + " ELSE "
                    + legacyNumber
                    + " END"
                : legacyNumber;
        String sql =
            "SELECT _wstart AS bucket_time, COUNT(*) AS total_count, COUNT("
                + number
                + ") AS numeric_count, MIN("
                + number
                + ") AS min_value, MAX("
                + number
                + ") AS max_value, AVG("
                + number
                + ") AS avg_value FROM "
                + getFullMetaTableName()
                + where
                + window;
        buckets = historyJdbc().query(sql, (rs, rowNum) -> trendBucket(rs, true));
      } else {
        // TDengine 2.x lacks CASE support. Both bounded result sets are aggregated in the engine;
        // only window summaries are merged here, never raw records.
        Map<Long, Bucket> windows = new TreeMap<>();
        for (Bucket bucket :
            historyJdbc()
                .query(
                    "SELECT _wstart AS bucket_time, COUNT(*) AS total_count FROM "
                        + getFullMetaTableName()
                        + where
                        + window,
                    (rs, rowNum) -> trendBucket(rs, false))) {
          windows.put(bucket.getTime(), bucket);
        }
        String number = "CAST(TRIM(content) AS DOUBLE)";
        List<Bucket> numericBuckets =
            historyJdbc()
                .query(
                    "SELECT _wstart AS bucket_time, COUNT(*) AS total_count, COUNT("
                        + number
                        + ") AS numeric_count, MIN("
                        + number
                        + ") AS min_value, MAX("
                        + number
                        + ") AS max_value, AVG("
                        + number
                        + ") AS avg_value FROM "
                        + getFullMetaTableName()
                        + where
                        + " AND TRIM(content) MATCH '"
                        + NUMERIC_PATTERN
                        + "'"
                        + window,
                    (rs, rowNum) -> trendBucket(rs, true));
        for (Bucket numeric : numericBuckets) {
          Bucket bucket = windows.get(numeric.getTime());
          if (bucket != null) {
            bucket.setNumericCount(numeric.getNumericCount());
            bucket.setMin(numeric.getMin());
            bucket.setMax(numeric.getMax());
            bucket.setAvg(numeric.getAvg());
          }
        }
        buckets = new ArrayList<>(windows.values());
      }
      // Never return a silently truncated aggregate if an engine's window semantics differ.
      if (buckets.size() > query.getMaxPoints()) {
        throw new IllegalStateException("TDengine returned more time windows than requested");
      }
      return PropertyTrendVO.of(query, buckets);
    } catch (RuntimeException failure) {
      log.warn(
          "TDengine 全时段趋势查询失败 iotId={}, property={}",
          query.getIotId(),
          query.getProperty(),
          failure);
      throw new IoTException("TDengine趋势查询失败或超时，请缩小时间范围后重试", 503, failure);
    }
  }

  private PropertyTrendVO querySampledTrend(PropertyTrendQuery query) {
    String where = historyWhere(query, true);
    List<String> windows = new ArrayList<>();
    for (PropertyTrendQuery.SampleWindow window : query.getSampleWindows()) {
      windows.add(
          "SELECT "
              + window.getIndex()
              + " AS window_index, SUBSTR(content,1,1025) AS content FROM (SELECT content FROM "
              + getFullMetaTableName()
              + where
              + " AND ts >= "
              + window.getBeginMillis()
              + " AND ts < "
              + window.getEndExclusiveMillis()
              + " ORDER BY ts DESC LIMIT "
              + query.getSampleFetchSize()
              + ")");
    }
    try {
      return TrendSamplingSupport.aggregate(
          query,
          historyJdbc()
              .query(
                  String.join(" UNION ALL ", windows),
                  (rs, rowNum) ->
                      new PropertyTrendSampleVO(
                          rs.getInt("window_index"), rs.getString("content"))));
    } catch (RuntimeException failure) {
      log.warn(
          "TDengine趋势采样失败 iotId={}, property={}", query.getIotId(), query.getProperty(), failure);
      throw new IoTException("TDengine趋势查询失败或超时，请缩小时间范围后重试", 503, failure);
    }
  }

  private Bucket trendBucket(ResultSet rs, boolean numeric) throws SQLException {
    Bucket bucket = new Bucket();
    bucket.setTime(rs.getTimestamp("bucket_time").getTime());
    bucket.setCount(rs.getLong("total_count"));
    if (numeric) {
      bucket.setNumericCount(rs.getLong("numeric_count"));
      bucket.setMin(rs.getBigDecimal("min_value"));
      bucket.setMax(rs.getBigDecimal("max_value"));
      bucket.setAvg(rs.getBigDecimal("avg_value"));
    }
    return bucket;
  }

  @Override
  public String getPolicy() {
    return STORE_POLICY;
  }

  @Override
  public JSONObject configMetadata() {
    JSONObject config = new JSONObject();
    config.put("url", jdbcUrl);
    config.put("database", databaseName);
    config.put("username", username);
    config.put("version", tdengineVersion);
    config.put("keepDays", keepDays);
    return config;
  }

  /** 查询最近的指令记录 */
  @Override
  public IoTDeviceLog getRecentCommand(
      String productKey,
      String deviceId,
      String commandId,
      String messageType,
      Long createTime,
      Integer commandStatus) {
    try {
      StringBuilder sql = new StringBuilder();
      sql.append("SELECT ts, id, device_name, message_type, command_id, command_status, ");
      sql.append("event, content, point, iot_id, device_id, product_key FROM ");
      sql.append(getFullLogTableName());
      sql.append(" WHERE product_key = '").append(escapeString(productKey)).append("'");
      sql.append(" AND device_id = '").append(escapeString(deviceId)).append("'");
      sql.append(" AND message_type = '").append(escapeString(messageType)).append("'");
      sql.append(" AND command_id = '").append(escapeString(commandId)).append("'");
      sql.append(" AND command_status = ").append(commandStatus != null ? commandStatus : 0);

      if (createTime != null) {
        sql.append(" AND ts >= ").append(createTime * 1000);
      }

      sql.append(" ORDER BY ts DESC, id DESC LIMIT 1");

      List<IoTDeviceLog> logs = jdbcTemplate.query(sql.toString(), new IoTDeviceLogRowMapper());
      return logs.isEmpty() ? null : logs.get(0);

    } catch (Exception e) {
      log.error("TDengine 查询指令记录异常: {}", e.getMessage(), e);
      return null;
    }
  }

  /** 通用设备日志查询接口 */
  @Override
  public List<IoTDeviceLog> queryDeviceLogs(DeviceLogQuery query) {
    try {
      StringBuilder sql = new StringBuilder();
      sql.append("SELECT ts, id, device_name, message_type, command_id, command_status, ");
      sql.append("event, content, point, iot_id, device_id, product_key FROM ");
      sql.append(getFullLogTableName());
      sql.append(" WHERE 1=1");

      if (query.getIotId() != null) {
        sql.append(" AND iot_id = '").append(escapeString(query.getIotId())).append("'");
      }
      if (query.getProductKey() != null) {
        sql.append(" AND product_key = '").append(escapeString(query.getProductKey())).append("'");
      }
      if (query.getDeviceId() != null) {
        sql.append(" AND device_id = '").append(escapeString(query.getDeviceId())).append("'");
      }
      if (query.getMessageType() != null) {
        sql.append(" AND message_type = '")
            .append(escapeString(query.getMessageType()))
            .append("'");
      }
      if (query.getLogId() != null) {
        sql.append(" AND id = ").append(query.getLogId());
      }
      if (query.getCommandId() != null) {
        sql.append(" AND command_id = '").append(escapeString(query.getCommandId())).append("'");
      }
      if (query.getCommandStatus() != null) {
        sql.append(" AND command_status = ").append(query.getCommandStatus());
      }
      if (query.getStartTime() != null) {
        sql.append(" AND ts >= ").append(query.getStartTime() * 1000);
      }
      if (query.getEndTime() != null) {
        sql.append(" AND ts <= ").append(query.getEndTime() * 1000);
      }
      if (query.getEventType() != null) {
        sql.append(" AND event = '").append(escapeString(query.getEventType())).append("'");
      }

      sql.append(" ORDER BY ts DESC, id DESC");

      if (query.getLimit() > 0) {
        sql.append(" LIMIT ").append(query.getLimit());
      }

      return jdbcTemplate.query(sql.toString(), new IoTDeviceLogRowMapper());

    } catch (Exception e) {
      log.error("TDengine 通用查询异常: {}", e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  /**
   * 批量插入设备日志（高性能批量写入）
   *
   * <p>TDengine 支持一次 INSERT 多条记录，大幅提升写入性能
   *
   * @param logs 日志列表
   */
  public void batchInsertDeviceLogs(List<IoTDeviceLog> logs) {
    if (logs == null || logs.isEmpty()) {
      return;
    }

    // 按 iotId 分组
    Map<String, List<IoTDeviceLog>> groupedLogs = new java.util.HashMap<>();
    for (IoTDeviceLog log : logs) {
      groupedLogs.computeIfAbsent(log.getIotId(), k -> new ArrayList<>()).add(log);
    }

    // 对每个分组执行批量插入
    for (Map.Entry<String, List<IoTDeviceLog>> entry : groupedLogs.entrySet()) {
      String iotId = entry.getKey();
      List<IoTDeviceLog> groupLogs = entry.getValue();

      if (groupLogs.isEmpty()) {
        continue;
      }

      IoTDeviceLog firstLog = groupLogs.get(0);
      String subTableName = generateSubTableName(devLogTableName, iotId);

      StringBuilder sql = new StringBuilder();
      sql.append("INSERT INTO ").append(databaseName).append(".").append(subTableName);
      sql.append(" USING ").append(getFullLogTableName());
      sql.append(" TAGS ('").append(escapeString(iotId)).append("', '");
      sql.append(escapeString(firstLog.getDeviceId())).append("', '");
      sql.append(escapeString(firstLog.getProductKey())).append("') VALUES ");

      boolean first = true;
      for (IoTDeviceLog deviceLog : groupLogs) {
        if (!first) {
          sql.append(" ");
        }
        first = false;

        sql.append("(");
        sql.append(
            deviceLog.getCreateTime() != null
                ? deviceLog.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis());
        sql.append(", ");
        sql.append(deviceLog.getId() != null ? deviceLog.getId() : IdUtil.getSnowflake().nextId());
        sql.append(", '").append(escapeString(deviceLog.getDeviceName())).append("'");
        sql.append(", '").append(escapeString(deviceLog.getMessageType())).append("'");
        sql.append(", '").append(escapeString(deviceLog.getCommandId())).append("'");
        sql.append(", ")
            .append(deviceLog.getCommandStatus() != null ? deviceLog.getCommandStatus() : 0);
        sql.append(", '").append(escapeString(deviceLog.getEvent())).append("'");
        sql.append(", '").append(escapeString(deviceLog.getContent())).append("'");
        sql.append(", '").append(escapeString(deviceLog.getPoint())).append("'");
        sql.append(")");
      }

      try {
        jdbcTemplate.execute(sql.toString());
        log.debug("TDengine 批量插入成功，iotId={}, 数量={}", iotId, groupLogs.size());
      } catch (Exception e) {
        log.error("TDengine 批量插入失败，iotId={}", iotId, e);
      }
    }
  }

  /** 设备日志行映射器 - 返回 VO */
  private static class DeviceLogRowMapper implements RowMapper<IoTDeviceLogVO> {
    @Override
    public IoTDeviceLogVO mapRow(ResultSet rs, int rowNum) throws SQLException {
      IoTDeviceLogVO vo = new IoTDeviceLogVO();
      long id = rs.getLong("id");
      vo.setId(rs.wasNull() ? null : id);
      vo.setIotId(rs.getString("iot_id"));
      vo.setDeviceId(rs.getString("device_id"));
      vo.setProductKey(rs.getString("product_key"));
      vo.setDeviceName(rs.getString("device_name"));
      vo.setMessageType(rs.getString("message_type"));
      vo.setCommandId(rs.getString("command_id"));
      vo.setCommandStatus(
          rs.getObject("command_status") != null ? rs.getInt("command_status") : null);
      vo.setEvent(rs.getString("event"));
      vo.setContent(rs.getString("content"));
      vo.setPoint(rs.getString("point"));

      // TDengine ts 列返回时间戳
      Timestamp ts = rs.getTimestamp("ts");
      if (ts != null) {
        vo.setCreateTime(ts.toLocalDateTime());
      }

      return vo;
    }
  }

  /** 设备日志行映射器 - 返回实体 */
  private static class IoTDeviceLogRowMapper implements RowMapper<IoTDeviceLog> {
    @Override
    public IoTDeviceLog mapRow(ResultSet rs, int rowNum) throws SQLException {
      IoTDeviceLog log = new IoTDeviceLog();
      log.setId(rs.getLong("id"));
      log.setIotId(rs.getString("iot_id"));
      log.setDeviceId(rs.getString("device_id"));
      log.setProductKey(rs.getString("product_key"));
      log.setDeviceName(rs.getString("device_name"));
      log.setMessageType(rs.getString("message_type"));
      log.setCommandId(rs.getString("command_id"));
      log.setCommandStatus(
          rs.getObject("command_status") != null ? rs.getInt("command_status") : null);
      log.setEvent(rs.getString("event"));
      log.setContent(rs.getString("content"));
      log.setPoint(rs.getString("point"));

      Timestamp ts = rs.getTimestamp("ts");
      if (ts != null) {
        log.setCreateTime(ts.toLocalDateTime());
      }

      return log;
    }
  }

  /** 设备日志元数据行映射器 */
  private static class DeviceLogMetadataRowMapper implements RowMapper<IoTDeviceLogMetadataVO> {
    @Override
    public IoTDeviceLogMetadataVO mapRow(ResultSet rs, int rowNum) throws SQLException {
      IoTDeviceLogMetadataVO vo = new IoTDeviceLogMetadataVO();
      long id = rs.getLong("id");
      vo.setId(rs.wasNull() ? null : id);
      vo.setIotId(rs.getString("iot_id"));
      vo.setProductKey(rs.getString("product_key"));
      vo.setDeviceName(rs.getString("device_name"));
      vo.setDeviceId(rs.getString("device_id"));
      vo.setMessageType(rs.getString("message_type"));
      vo.setEvent(rs.getString("event"));
      vo.setProperty(rs.getString("property"));
      vo.setContent(rs.getString("content"));
      vo.setExt1(rs.getString("ext1"));
      vo.setExt2(rs.getString("ext2"));
      vo.setExt3(rs.getString("ext3"));

      Timestamp ts = rs.getTimestamp("ts");
      if (ts != null) {
        vo.setCreateTime(ts.toLocalDateTime());
      }

      return vo;
    }
  }
}
