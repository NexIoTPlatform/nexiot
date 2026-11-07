package cn.universal.dm.device.service.log;

import cn.universal.common.exception.IoTException;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * MySQL/DM history schema readiness. PostgreSQL is owned by TimescaleDBDeviceLogService. Query
 * checks never run DDL. Startup upgrades run serially after the application is ready.
 */
@Component
@Slf4j
public class RelationalHistorySchemaSupport {
  private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,127}");
  @Resource private JdbcTemplate jdbcTemplate;

  @Value("${nexiot.log.history.schema.auto-upgrade:true}")
  private boolean autoUpgrade = true;

  @Value("${nexiot.log.history.schema.query-timeout-seconds:8}")
  private int queryTimeoutSeconds = 8;

  @Value("${nexiot.log.history.schema.index-timeout-seconds:300}")
  private int indexTimeoutSeconds = 300;

  @Value("${shard.log.enable:true}")
  private boolean logSharding = true;

  @Value("${shard.log.table.number:24}")
  private int logShardCount = 24;

  @Value("${shard.log.table.prefix:${timescaledb.table.iot_device_log:iot_device_log}}")
  private String logPrefix = "iot_device_log";

  @Value("${shard.log.table.namespace:${timescaledb.schema:${mysql.log.database:}}}")
  private String logNamespace = "";

  @Value("${shard.logMeta.enable:true}")
  private boolean metaSharding = true;

  @Value("${shard.logMeta.table.number:24}")
  private int metaShardCount = 24;

  @Value(
      "${shard.logMeta.table.prefix:${timescaledb.table.iot_device_log_metadata:iot_device_log_metadata}}")
  private String metaPrefix = "iot_device_log_metadata";

  @Value("${shard.logMeta.table.namespace:${timescaledb.schema:${mysql.log.database:}}}")
  private String metaNamespace = "";

  private final ConcurrentHashMap<String, Readiness> readiness = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> maintenanceWarnings = new ConcurrentHashMap<>();
  private final AtomicLong migrationRequired = new AtomicLong();
  private final AtomicLong migratedTables = new AtomicLong();
  private final AtomicLong createdIndexes = new AtomicLong();
  private final AtomicBoolean submitted = new AtomicBoolean();
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          Thread.ofPlatform().daemon(true).name("relational-history-schema").factory());

  public record Readiness(boolean ready, String reason, long checkedAtMillis) {}

  public record Metrics(long migrationRequired, long migratedTables, long createdIndexes) {}

  record Column(int jdbcType, String typeName, boolean nullable, boolean autoIncrement) {}

  enum Dialect {
    MYSQL,
    DM,
    OTHER
  }

  record Table(String schema, String name, Dialect dialect) {
    String quoted() {
      return quote(schema, dialect) + "." + quote(name, dialect);
    }
  }

  /** Call before a history read using the already resolved physical shard table name. */
  public void requireReady(String physicalTable) {
    validateTableName(physicalTable);
    Readiness cached = readiness.get(physicalTable);
    long now = System.currentTimeMillis();
    if (cached != null && now - cached.checkedAtMillis() < (cached.ready() ? 60000 : 5000)) {
      if (!cached.ready()) throw new IoTException(cached.reason());
      return;
    }
    try {
      jdbcTemplate.execute(
          (ConnectionCallback<Void>)
              connection -> {
                Dialect dialect = dialect(connection.getMetaData());
                if (dialect == Dialect.OTHER) return null;
                Table table = table(connection, physicalTable, dialect);
                Map<String, Column> columns = columns(connection, table);
                String problem = readProblem(columns);
                if (hasGuard(connection, table))
                  throw new IoTException("历史表 " + table.quoted() + " 空表迁移尚未完成，请等待后台检查或联系管理员");
                if (problem != null)
                  throw new IoTException(
                      "历史表 " + table.quoted() + " " + problem + "，需要完成存储迁移后查询；原始记录未被改写");
                return null;
              });
      readiness.put(physicalTable, new Readiness(true, "ready", now));
    } catch (Exception error) {
      String reason =
          error instanceof IoTException ? error.getMessage() : "历史表结构检查失败，请检查数据库或迁移状态后重试";
      readiness.put(physicalTable, new Readiness(false, reason, now));
      migrationRequired.incrementAndGet();
      log.error("历史表尚未就绪: table={}, reason={}", physicalTable, reason);
      throw new IoTException(reason);
    }
  }

  /** Explicit/background maintenance entry point; never called from requireReady. */
  public void upgradeTables(List<String> physicalTables, boolean metadata) {
    for (String physicalTable : physicalTables) {
      if (Thread.currentThread().isInterrupted()) return;
      validateTableName(physicalTable);
      try {
        jdbcTemplate.execute(
            (ConnectionCallback<Void>)
                connection -> {
                  Dialect dialect = dialect(connection.getMetaData());
                  if (dialect == Dialect.OTHER) return null;
                  if (!connection.getAutoCommit()) throw new SQLException("历史表升级要求独立自动提交连接");
                  Table table = table(connection, physicalTable, dialect);
                  if (!acquireMaintenanceLock(connection, table)) {
                    maintenanceWarnings.put(physicalTable, "另一个实例正在升级历史存储，请等待下一次检查");
                    return null;
                  }
                  try {
                    maintenanceWarnings.remove(physicalTable);
                    Map<String, Column> existing = columns(connection, table);
                    if (schemaProblem(existing) != null) {
                      if (readProblem(existing) == null && hasRows(connection, table)) {
                        migrationRequired.incrementAndGet();
                        maintenanceWarnings.put(
                            physicalTable, "id写入容量不足，需要在线影子表或维护窗口升级BIGINT；历史读取不受影响");
                        log.warn(
                            "历史表读取兼容，但id写入容量不足，需要在线影子表或维护窗口升级BIGINT: table={}", table.quoted());
                      } else {
                        migrateEmptyTable(connection, table, existing);
                      }
                    }
                    if (readProblem(columns(connection, table)) != null) {
                      throw new IoTException("历史表 " + table.quoted() + " 需要计划迁移，保留原数据");
                    }
                    cleanupGuard(connection, table);
                    if (metadata) {
                      ensureIndex(
                          connection, table, List.of("iot_id", "property", "create_time", "id"));
                      ensureIndex(
                          connection, table, List.of("iot_id", "event", "create_time", "id"));
                    } else {
                      ensureIndex(connection, table, List.of("iot_id", "create_time", "id"));
                    }
                  } finally {
                    releaseMaintenanceLock(connection, table);
                  }
                  return null;
                });
        readiness.remove(physicalTable);
      } catch (Exception error) {
        String reason = "历史表 " + physicalTable + " 自动升级未完成：" + error.getMessage();
        // Index availability affects performance, not whether existing records can be read.
        // A subsequent requireReady performs its own schema-only check.
        readiness.remove(physicalTable);
        maintenanceWarnings.put(physicalTable, reason);
        migrationRequired.incrementAndGet();
        log.error("{}；未猜测历史时间单位或重写非空表", reason);
      }
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void upgradeAfterStartup() {
    if (!autoUpgrade || !submitted.compareAndSet(false, true)) return;
    executor.submit(
        () -> {
          upgradeTables(managedTables(logNamespace, logPrefix, logSharding, logShardCount), false);
          upgradeTables(
              managedTables(metaNamespace, metaPrefix, metaSharding, metaShardCount), true);
        });
  }

  public Map<String, Readiness> readinessSnapshot() {
    return Map.copyOf(readiness);
  }

  public Map<String, String> maintenanceWarnings() {
    return Map.copyOf(maintenanceWarnings);
  }

  public Metrics metrics() {
    return new Metrics(migrationRequired.get(), migratedTables.get(), createdIndexes.get());
  }

  @PreDestroy
  public void close() {
    executor.shutdownNow();
  }

  static List<String> managedTables(String namespace, String prefix, boolean sharded, int count) {
    String base = namespace == null || namespace.isBlank() ? prefix : namespace + "." + prefix;
    validateTableName(base);
    if (count < 0 || count > 1024) throw new IllegalArgumentException("日志分片数必须在0到1024之间");
    List<String> tables = new ArrayList<>();
    tables.add(base);
    if (sharded) for (int index = 0; index < count; index++) tables.add(base + "_" + index);
    return tables;
  }

  private Map<String, Column> columns(Connection connection, Table table) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.setQueryTimeout(queryTimeoutSeconds);
      try (ResultSet rows =
          statement.executeQuery("SELECT * FROM " + table.quoted() + " WHERE 1=0")) {
        ResultSetMetaData metadata = rows.getMetaData();
        Map<String, Column> columns = new LinkedHashMap<>();
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
          columns.put(
              metadata.getColumnName(index).toLowerCase(Locale.ROOT),
              new Column(
                  metadata.getColumnType(index),
                  metadata.getColumnTypeName(index),
                  metadata.isNullable(index) != ResultSetMetaData.columnNoNulls,
                  metadata.isAutoIncrement(index)));
        }
        return columns;
      }
    }
  }

  static String readProblem(Map<String, Column> columns) {
    Column time = columns.get("create_time");
    if (time == null || !isTemporal(time)) return "create_time 不是 TIMESTAMP/DATETIME";
    Column id = columns.get("id");
    if (id == null || !isInteger(id)) return "id 不是整数类型，无法稳定翻页";
    return null;
  }

  static String schemaProblem(Map<String, Column> columns) {
    Column time = columns.get("create_time");
    if (time == null || !isTemporal(time)) return "create_time 不是 TIMESTAMP/DATETIME";
    Column id = columns.get("id");
    if (id == null || id.jdbcType() != Types.BIGINT) return "id 不是 BIGINT，无法保存完整日志标识";
    return null;
  }

  static boolean isTemporal(Column column) {
    return column.jdbcType() == Types.TIMESTAMP
        || column.jdbcType() == Types.TIMESTAMP_WITH_TIMEZONE;
  }

  private void migrateEmptyTable(Connection connection, Table table, Map<String, Column> existing)
      throws SQLException {
    if (hasRows(connection, table)) {
      throw new IoTException(
          "非空历史表 "
              + table.quoted()
              + " "
              + schemaProblem(existing)
              + "；请确认 epoch 秒/毫秒、时区和ID映射后执行离线或影子表迁移");
    }
    Column time = existing.get("create_time");
    Column id = existing.get("id");
    if (time == null || id == null || (!isTemporal(time) && !isInteger(time)) || !isInteger(id)) {
      throw new IoTException("不支持自动转换的历史列类型，需人工确认迁移方案");
    }
    requireEnforcedCheck(connection.getMetaData(), table.dialect());
    // DM/MySQL DDL commits implicitly. An exclusive transaction lock alone has a race.
    // This enforced check atomically validates emptiness, then blocks inserts across DDLs.
    // A competing writer either wins before the check (upgrade fails) or is rejected.
    String guard = guardName(table);
    if (!hasGuard(connection, table)) {
      executeDdl(
          connection,
          "ALTER TABLE "
              + table.quoted()
              + " ADD CONSTRAINT "
              + quote(guard, table.dialect())
              + " CHECK (1=0)",
          queryTimeoutSeconds);
    }
    if (hasRows(connection, table)) throw new IoTException("空表迁移保护检查失败，拒绝修改");
    Map<String, Column> current = columns(connection, table);
    if (!isTemporal(current.get("create_time"))) {
      String target = table.dialect() == Dialect.MYSQL ? "DATETIME(6)" : "TIMESTAMP(6)";
      modifyColumn(connection, table, "create_time", target, current.get("create_time"));
    }
    if (current.get("id").jdbcType() != Types.BIGINT) {
      String target =
          table.dialect() == Dialect.MYSQL
                  && current.get("id").typeName().toUpperCase(Locale.ROOT).contains("UNSIGNED")
              ? "BIGINT UNSIGNED"
              : "BIGINT";
      modifyColumn(connection, table, "id", target, current.get("id"));
    }
    if (schemaProblem(columns(connection, table)) != null) {
      throw new IoTException("历史表转换后验证失败，保留空表迁移保护约束");
    }
    cleanupGuard(connection, table);
    migratedTables.incrementAndGet();
    log.info("空历史表结构迁移完成: table={}", table.quoted());
  }

  private boolean hasRows(Connection connection, Table table) throws SQLException {
    String sql =
        table.dialect() == Dialect.MYSQL
            ? "SELECT 1 FROM " + table.quoted() + " LIMIT 1"
            : "SELECT TOP 1 1 FROM " + table.quoted();
    try (Statement statement = connection.createStatement()) {
      statement.setQueryTimeout(queryTimeoutSeconds);
      try (ResultSet rows = statement.executeQuery(sql)) {
        return rows.next();
      }
    }
  }

  private void modifyColumn(
      Connection connection, Table table, String column, String target, Column original)
      throws SQLException {
    String attributes = original.nullable() ? " NULL" : " NOT NULL";
    if (table.dialect() == Dialect.MYSQL && original.autoIncrement())
      attributes += " AUTO_INCREMENT";
    executeDdl(
        connection,
        "ALTER TABLE "
            + table.quoted()
            + " MODIFY "
            + quote(column, table.dialect())
            + " "
            + target
            + attributes,
        queryTimeoutSeconds);
  }

  private boolean hasGuard(Connection connection, Table table) throws SQLException {
    String sql =
        table.dialect() == Dialect.MYSQL
            ? "SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=? AND"
                  + " table_name=? AND constraint_name=? AND constraint_type='CHECK'"
            : "SELECT 1 FROM all_constraints WHERE owner=? AND table_name=? AND constraint_name=?"
                  + " AND constraint_type='C'";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setQueryTimeout(queryTimeoutSeconds);
      statement.setString(1, table.schema());
      statement.setString(2, table.name());
      statement.setString(3, guardName(table));
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next();
      }
    }
  }

  private void cleanupGuard(Connection connection, Table table) throws SQLException {
    if (hasGuard(connection, table))
      executeDdl(
          connection,
          "ALTER TABLE "
              + table.quoted()
              + " DROP "
              + (table.dialect() == Dialect.MYSQL ? "CHECK " : "CONSTRAINT ")
              + quote(guardName(table), table.dialect()),
          queryTimeoutSeconds);
  }

  private String guardName(Table table) {
    return "nx_hist_empty_" + Integer.toUnsignedString(table.quoted().hashCode(), 16);
  }

  private static boolean isInteger(Column column) {
    return column.jdbcType() == Types.BIGINT
        || column.jdbcType() == Types.INTEGER
        || column.jdbcType() == Types.SMALLINT
        || column.jdbcType() == Types.TINYINT;
  }

  private void ensureIndex(Connection connection, Table table, List<String> wanted)
      throws SQLException {
    if (hasIndex(connection, table, wanted)) return;
    String name =
        "nx_hist_"
            + (wanted.size() == 3 ? "device_" : wanted.get(1) + "_")
            + Integer.toUnsignedString(table.quoted().hashCode(), 16);
    String columns =
        wanted.stream()
            .map(
                column ->
                    quote(column, table.dialect())
                        + (column.equals("create_time") || column.equals("id") ? " DESC" : ""))
            .collect(java.util.stream.Collectors.joining(","));
    String ddl =
        table.dialect() == Dialect.MYSQL
            ? "ALTER TABLE "
                + table.quoted()
                + " ADD INDEX "
                + quote(name, table.dialect())
                + " ("
                + columns
                + "), ALGORITHM=INPLACE, LOCK=NONE"
            : "CREATE INDEX "
                + quote(table.schema(), table.dialect())
                + "."
                + quote(name, table.dialect())
                + " ON "
                + table.quoted()
                + " ("
                + columns
                + ") ONLINE";
    try {
      executeDdl(connection, ddl, indexTimeoutSeconds);
    } catch (SQLException failure) {
      if (!hasIndex(connection, table, wanted)) throw failure;
    }
    if (!hasIndex(connection, table, wanted)) throw new SQLException("索引创建后不可用: " + name);
    createdIndexes.incrementAndGet();
  }

  private boolean hasIndex(Connection connection, Table table, List<String> wanted)
      throws SQLException {
    String mysqlVisibility =
        table.dialect() == Dialect.MYSQL && supportsMysqlIndexVisibility(connection.getMetaData())
            ? " AND is_visible='YES'"
            : "";
    String sql =
        table.dialect() == Dialect.MYSQL
            ? "SELECT index_name,column_name,seq_in_index AS position,collation AS direction FROM"
                  + " information_schema.statistics WHERE table_schema=? AND table_name=? AND"
                  + " sub_part IS NULL"
                + mysqlVisibility
                + " ORDER BY index_name,seq_in_index"
            : "SELECT c.index_name,c.column_name,c.column_position AS position,c.descend AS"
                  + " direction FROM all_ind_columns c JOIN all_indexes i ON i.owner=c.index_owner"
                  + " AND i.index_name=c.index_name WHERE c.index_owner=? AND c.table_name=? AND"
                  + " i.status='VALID' ORDER BY c.index_name,c.column_position";
    Map<String, List<String>> indexes = new LinkedHashMap<>();
    Map<String, List<String>> directions = new LinkedHashMap<>();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setQueryTimeout(queryTimeoutSeconds);
      statement.setString(1, table.schema());
      statement.setString(2, table.name());
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          String name = rows.getString(1);
          List<String> keys = indexes.computeIfAbsent(name, ignored -> new ArrayList<>());
          keys.add(
              rows.getInt(3) == keys.size() + 1
                  ? rows.getString(2).toLowerCase(Locale.ROOT)
                  : "<invalid-prefix>");
          directions.computeIfAbsent(name, ignored -> new ArrayList<>()).add(rows.getString(4));
        }
      }
    }
    for (Map.Entry<String, List<String>> index : indexes.entrySet()) {
      List<String> keys = index.getValue();
      if (keys.size() >= wanted.size() && keys.subList(0, wanted.size()).equals(wanted)) {
        List<String> order = directions.get(index.getKey());
        // Equality columns may use either order; both seek columns must have the same order.
        if (java.util.Objects.equals(order.get(wanted.size() - 2), order.get(wanted.size() - 1)))
          return true;
      }
    }
    return false;
  }

  private static boolean supportsMysqlIndexVisibility(DatabaseMetaData metadata)
      throws SQLException {
    if (metadata == null) return false;
    String product = metadata.getDatabaseProductName();
    if (product == null || !product.toLowerCase(Locale.ROOT).contains("mysql")) return false;
    String version = metadata.getDatabaseProductVersion();
    java.util.regex.Matcher parts =
        Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)").matcher(version == null ? "" : version);
    if (!parts.find()) return false;
    int major = Integer.parseInt(parts.group(1));
    return major >= 8;
  }

  private boolean acquireMaintenanceLock(Connection connection, Table table) throws SQLException {
    int lockId = table.quoted().hashCode() & 0x3fffffff;
    String sql =
        table.dialect() == Dialect.MYSQL
            ? "SELECT GET_LOCK('nexiot.history." + lockId + "', 0)"
            : "SELECT DBMS_LOCK.REQUEST(" + lockId + ", 6, 0, FALSE) FROM DUAL";
    try (Statement statement = connection.createStatement()) {
      statement.setQueryTimeout(queryTimeoutSeconds);
      try (ResultSet result = statement.executeQuery(sql)) {
        if (!result.next()) throw new SQLException("数据库未返回历史存储升级锁结果");
        int status = result.getInt(1);
        return !result.wasNull()
            && (table.dialect() == Dialect.MYSQL ? status == 1 : status == 0 || status == 4);
      }
    }
  }

  private void releaseMaintenanceLock(Connection connection, Table table) throws SQLException {
    int lockId = table.quoted().hashCode() & 0x3fffffff;
    String sql =
        table.dialect() == Dialect.MYSQL
            ? "SELECT RELEASE_LOCK('nexiot.history." + lockId + "')"
            : "SELECT DBMS_LOCK.RELEASE(" + lockId + ") FROM DUAL";
    try (Statement statement = connection.createStatement()) {
      statement.setQueryTimeout(queryTimeoutSeconds);
      statement.execute(sql);
    }
  }

  static void requireEnforcedCheck(DatabaseMetaData metadata, Dialect dialect) throws SQLException {
    if (dialect != Dialect.MYSQL) return;
    String version = metadata.getDatabaseProductVersion();
    java.util.regex.Matcher parts =
        Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)").matcher(version == null ? "" : version);
    if (!parts.find()) throw new SQLException("无法验证MySQL CHECK约束支持，拒绝自动列类型迁移");
    int major = Integer.parseInt(parts.group(1)),
        minor = Integer.parseInt(parts.group(2)),
        patch = Integer.parseInt(parts.group(3));
    if (major < 8 || (major == 8 && minor == 0 && patch < 16)) {
      throw new SQLException("MySQL 8.0.16以下不强制CHECK约束，旧历史列类型需维护窗口迁移");
    }
  }

  private void executeDdl(Connection connection, String sql, int timeoutSeconds)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.setQueryTimeout(Math.max(1, timeoutSeconds));
      statement.execute(sql);
    }
  }

  static Dialect dialect(DatabaseMetaData metadata) throws SQLException {
    String product = metadata.getDatabaseProductName().toLowerCase(Locale.ROOT);
    if (product.contains("mysql")) return Dialect.MYSQL;
    if (product.startsWith("dm") || product.contains("dameng")) return Dialect.DM;
    return Dialect.OTHER;
  }

  private Table table(Connection connection, String name, Dialect dialect) throws SQLException {
    String[] parts = name.split("\\.");
    String schema =
        parts.length == 2
            ? parts[0]
            : dialect == Dialect.MYSQL ? connection.getCatalog() : connection.getSchema();
    if (schema == null || !IDENTIFIER.matcher(schema).matches())
      throw new SQLException("无效的日志数据库命名空间");
    return new Table(schema, parts[parts.length - 1], dialect);
  }

  static void validateTableName(String table) {
    if (table == null) throw new IllegalArgumentException("需要日志表名");
    String[] parts = table.split("\\.", -1);
    if (parts.length > 2) throw new IllegalArgumentException("无效的日志表名");
    for (String part : parts)
      if (!IDENTIFIER.matcher(part).matches()) throw new IllegalArgumentException("无效的日志表名");
  }

  private static String quote(String value, Dialect dialect) {
    return dialect == Dialect.MYSQL ? "`" + value + "`" : "\"" + value + "\"";
  }
}
