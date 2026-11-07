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

import cn.hutool.core.util.StrUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * TimescaleDB 日志存储。
 *
 * <p>实现策略：
 *
 * <ul>
 *   <li>沿用 PostgreSQL/MyBatis 查询链路，同时允许 Timescale 使用独立日志表名，保证接入平滑。
 *   <li>启动时自动检查 TimescaleDB 扩展、日志表和 hypertable 状态，尽量做到开箱即用。
 *   <li>当当前主库不是 PostgreSQL 时，不注册该 Bean，避免误启用。
 * </ul>
 */
@Component
@Slf4j
@Conditional(PostgreSqlDataSourceCondition.class)
@ConditionalOnProperty(
    prefix = "timescaledb",
    name = "enable",
    havingValue = "true",
    matchIfMissing = true)
public class TimescaleDBDeviceLogService extends MysqlDeviceLogService {

  private static final String STORE_POLICY = IoTConstant.TIMESCALEDB;
  private static final String DICT_TYPE = "log_store_policy";


  @Resource private JdbcTemplate jdbcTemplate;
  @Resource private SysDictDataMapper sysDictDataMapper;

  @Value("${timescaledb.auto-init:true}")
  private boolean autoInit;

  @Value("${timescaledb.ensure-dict:true}")
  private boolean ensureDictData;

  @Value("${timescaledb.chunk-time-interval:7 days}")
  private String chunkTimeInterval;

  @Value("${timescaledb.retention-period:}")
  private String retentionPeriod;

  @Value("${timescaledb.compression-after:}")
  private String compressionAfter;

  @Value("${timescaledb.schema:}")
  private String configuredSchemaName;

  @Value("${timescaledb.table.iot_device_log:ts_iot_device_log}")
  private String logTableName;

  @Value("${timescaledb.table.iot_device_log_metadata:ts_iot_device_log_metadata}")
  private String logMetaTableName;

  @Value("${shard.log.enable:true}")
  private boolean logShardingEnabled;

  @Value("${shard.log.table.number:24}")
  private int logTableCount;

  @Value("${shard.logMeta.enable:true}")
  private boolean logMetaShardingEnabled;

  @Value("${shard.logMeta.table.number:24}")
  private int logMetaTableCount;

  private String schemaName = "public";

  @Value("${timescaledb.index-upgrade.enabled:true}")
  private boolean indexUpgradeEnabled = true;

  @Value("${timescaledb.index-upgrade.statement-timeout-seconds:300}")
  private int indexStatementTimeoutSeconds = 300;

  @Value("${timescaledb.index-upgrade.lock-timeout-ms:30000}")
  private int indexLockTimeoutMs = 30000;

  @Value("${timescaledb.schema-lock-timeout-ms:1500}")
  private int schemaLockTimeoutMs = 1500;

  private volatile JdbcTemplate schemaQueryTemplate;

  private final Set<String> createdTables = new HashSet<>();
  private final AtomicBoolean upgradeSubmitted = new AtomicBoolean();
  private final ExecutorService schemaExecutor =
      Executors.newSingleThreadExecutor(
          Thread.ofPlatform().name("history-schema-upgrade").daemon(true).factory());

  @Override
  public String getPolicy() {
    return STORE_POLICY;
  }

  @PostConstruct
  public void initTimescale() {
    schemaName = resolveSchemaName();
    if (ensureDictData) {
      ensureStorePolicyDictData();
    }
    if (!autoInit) {
      log.info("TimescaleDB 自动初始化已关闭");
      return;
    }
    try {
      // Bootstrap only missing tables. Existing data, timestamps, column types and primary
      // keys are never rewritten on an application startup.
      executeMaintenanceDdl("create schema if not exists " + quotedIdentifier(schemaName), 8, null);
      ensureLogTables(resolveManagedTables(logTableName, logShardingEnabled, logTableCount));
      ensureMetadataTables(
          resolveManagedTables(logMetaTableName, logMetaShardingEnabled, logMetaTableCount));
    } catch (Exception e) {
      log.error("日志表检查失败；不会自动重写既有日志，请检查数据库连接与权限", e);
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void upgradeHistorySchemaAsync() {
    if (!autoInit || !indexUpgradeEnabled || !upgradeSubmitted.compareAndSet(false, true)) {
      return;
    }
    schemaExecutor.submit(
        () -> {
          try {
            List<String> logTables =
                resolveManagedTables(logTableName, logShardingEnabled, logTableCount);
            List<String> metaTables =
                resolveManagedTables(logMetaTableName, logMetaShardingEnabled, logMetaTableCount);
            boolean timescale = ensureTimescaleExtension();
            for (boolean metadata : new boolean[] {false, true}) {
              for (String table : metadata ? metaTables : logTables) {
                if (Thread.currentThread().isInterrupted()) {
                  return;
                }
                try {
                  validateExistingSchema(table);
                  if (timescale) {
                    ensureHypertable(table);
                  }
                  ensurePlainIndexes(List.of(table), metadata);
                  if (timescale && isHypertable(table)) {
                    ensureRetentionPolicy(table);
                    ensureCompressionPolicy(table);
                  }
                } catch (Exception e) {
                  log.warn("日志存储后台升级未完成，保留原表继续服务: table={}, error={}", table, e.getMessage());
                }
              }
            }
            log.info("日志存储后台检查完成，schema={}；未重写历史数据", schemaName);
          } catch (Exception e) {
            log.error("日志存储后台升级失败，保留原表继续服务", e);
          }
        });
  }

  @PreDestroy
  public void stopSchemaUpgrade() {
    schemaExecutor.shutdownNow();
  }

  private void ensureStorePolicyDictData() {
    try {
      List<SysDictData> dictList = sysDictDataMapper.selectDictDataByType(DICT_TYPE);
      boolean exists =
          dictList.stream()
              .map(SysDictData::getDictValue)
              .filter(StrUtil::isNotBlank)
              .map(value -> value.trim().toLowerCase(Locale.ROOT))
              .anyMatch(STORE_POLICY::equals);
      if (exists) {
        return;
      }
      Date now = new Date();
      SysDictData dictData =
          SysDictData.builder()
              .dictSort(resolveNextDictSort(dictList))
              .dictLabel("TimescaleDB")
              .dictValue(STORE_POLICY)
              .dictType(DICT_TYPE)
              .status("0")
              .isDefault("N")
              .createBy("system")
              .createTime(now)
              .updateBy("system")
              .updateTime(now)
              .remark("PostgreSQL/TimescaleDB 日志存储")
              .build();
      sysDictDataMapper.insertSelective(dictData);
      log.info("已自动注册 TimescaleDB 日志存储策略字典，dictType={}", DICT_TYPE);
    } catch (Exception e) {
      log.warn("自动注册 TimescaleDB 存储策略字典失败，不影响日志功能: {}", e.getMessage());
    }
  }

  private int resolveNextDictSort(List<SysDictData> dictList) {
    return dictList.stream()
        .map(SysDictData::getDictSort)
        .filter(sort -> sort != null)
        .max(Integer::compareTo)
        .map(sort -> sort + 1)
        .orElse(10);
  }

  private String resolveSchemaName() {
    return StrUtil.isNotBlank(configuredSchemaName)
        ? configuredSchemaName.trim()
        : StrUtil.blankToDefault(resolveCurrentSchema(), "public");
  }

  private String resolveCurrentSchema() {
    try {
      String currentSchema =
          schemaQueryJdbc().queryForObject("select current_schema()", String.class);
      return StrUtil.blankToDefault(currentSchema, "public");
    } catch (DataAccessException e) {
      log.warn("获取当前 PostgreSQL schema 失败，默认使用 public: {}", e.getMessage());
      return "public";
    }
  }

  private boolean ensureTimescaleExtension() {
    Boolean installed =
        schemaQueryJdbc()
            .queryForObject(
                "select exists(select 1 from pg_extension where extname = 'timescaledb')",
                Boolean.class);
    if (!Boolean.TRUE.equals(installed)) {
      log.info("TimescaleDB 扩展未安装，按普通 PostgreSQL 在线索引方案运行；扩展安装由数据库管理员管理");
    }
    return Boolean.TRUE.equals(installed);
  }

  private List<String> resolveManagedTables(
      String baseTableName, boolean shardingEnabled, int shardTableCount) {
    List<String> tableNames = new ArrayList<>();
    String normalizedBaseTableName = StrUtil.blankToDefault(baseTableName, "").trim();
    if (StrUtil.isBlank(normalizedBaseTableName)) {
      return tableNames;
    }

    tableNames.add(normalizedBaseTableName);
    if (!shardingEnabled) {
      return tableNames;
    }

    int tableCount = Math.min(Math.max(shardTableCount, 0), 1024);
    for (int index = 0; index < tableCount; index++) {
      tableNames.add(normalizedBaseTableName + "_" + index);
    }
    return tableNames;
  }

  private void ensureLogTables(List<String> tableNames) {
    for (String tableName : tableNames) {
      ensureLogTable(tableName);
    }
  }

  private void ensureMetadataTables(List<String> tableNames) {
    for (String tableName : tableNames) {
      ensureMetadataTable(tableName);
    }
  }

  private void ensureLogTable(String tableName) {
    if (tableExists(tableName)) {
      return;
    }
    executeMaintenanceDdl(
        """
        CREATE TABLE IF NOT EXISTS %s (
          id BIGINT NOT NULL,
          iot_id VARCHAR(128),
          device_id VARCHAR(64),
          product_key VARCHAR(64),
          device_name VARCHAR(64),
          message_type VARCHAR(32),
          command_id VARCHAR(128),
          command_status INTEGER,
          event VARCHAR(128),
          point VARCHAR(255),
          content TEXT,
          create_time TIMESTAMP NOT NULL,
          PRIMARY KEY (id, create_time)
        )
        """
            .formatted(qualifiedTable(tableName)),
        8,
        null);
    createdTables.add(tableName);
  }

  private void ensureMetadataTable(String tableName) {
    if (tableExists(tableName)) {
      return;
    }
    executeMaintenanceDdl(
        """
        CREATE TABLE IF NOT EXISTS %s (
          id BIGINT NOT NULL,
          iot_id VARCHAR(128),
          product_key VARCHAR(64),
          device_name VARCHAR(64),
          device_id VARCHAR(64),
          message_type VARCHAR(32),
          event VARCHAR(128),
          property VARCHAR(128),
          content TEXT,
          ext1 VARCHAR(255),
          ext2 VARCHAR(1024),
          ext3 VARCHAR(255),
          create_time TIMESTAMP NOT NULL,
          PRIMARY KEY (id, create_time)
        )
        """
            .formatted(qualifiedTable(tableName)),
        8,
        null);
    createdTables.add(tableName);
  }

  private boolean tableExists(String tableName) {
    return Boolean.TRUE.equals(
        schemaQueryJdbc()
            .queryForObject(
                "select to_regclass(?) is not null", Boolean.class, qualifiedTable(tableName)));
  }

  private void validateExistingSchema(String tableName) {
    List<String> problems =
        schemaQueryJdbc()
            .query(
                "select column_name, data_type, is_nullable from information_schema.columns where"
                    + " table_schema = ? and table_name = ? and column_name in"
                    + " ('id','create_time')",
                (rs, row) ->
                    "id".equals(rs.getString("column_name"))
                            && !"bigint".equals(rs.getString("data_type"))
                        ? "id 类型为 " + rs.getString("data_type")
                        : "YES".equals(rs.getString("is_nullable"))
                            ? rs.getString("column_name") + " 允许 NULL"
                            : "",
                schemaName,
                tableName);
    problems.stream()
        .filter(StrUtil::isNotBlank)
        .forEach(
            problem -> log.warn("日志表需要计划迁移，启动不会自动改写: table={}, reason={}", tableName, problem));
  }

  private void ensureHypertable(String tableName) {
    if (isHypertable(tableName)) {
      return;
    }
    if (!createdTables.contains(tableName)) {
      log.info("保留已有 PostgreSQL 表 {}；转换 hypertable 需离线迁移计划", qualifiedTable(tableName));
      return;
    }
    // migrate_data=false also protects against a writer winning the race after CREATE TABLE.
    // Never drop a primary key or invent a timestamp for pre-existing records.
    executeMaintenanceDdl(
        """
        SELECT create_hypertable('%s', 'create_time', if_not_exists => TRUE,
          migrate_data => FALSE, chunk_time_interval => INTERVAL '%s')
        """
            .formatted(
                escapeSqlLiteral(qualifiedTable(tableName)), escapeSqlLiteral(chunkTimeInterval)),
        8,
        null);
  }

  private void ensureRetentionPolicy(String tableName) {
    if (StrUtil.isBlank(retentionPeriod)) {
      return;
    }
    try {
      executeMaintenanceDdl(
          """
          SELECT add_retention_policy(
            '%s',
            INTERVAL '%s',
            if_not_exists => TRUE
          )
          """
              .formatted(
                  escapeSqlLiteral(qualifiedTable(tableName)), escapeSqlLiteral(retentionPeriod)),
          8,
          null);
    } catch (Exception e) {
      log.warn("TimescaleDB 添加保留策略失败: table={}, retention={}", tableName, retentionPeriod, e);
    }
  }

  private void ensureCompressionPolicy(String tableName) {
    if (StrUtil.isBlank(compressionAfter)) {
      return;
    }
    try {
      executeMaintenanceDdl(
          """
          ALTER TABLE %s SET (
            timescaledb.compress,
            timescaledb.compress_segmentby = 'iot_id'
          )
          """
              .formatted(qualifiedTable(tableName)),
          8,
          null);
      executeMaintenanceDdl(
          """
          SELECT add_compression_policy(
            '%s',
            INTERVAL '%s',
            if_not_exists => TRUE
          )
          """
              .formatted(
                  escapeSqlLiteral(qualifiedTable(tableName)), escapeSqlLiteral(compressionAfter)),
          8,
          null);
    } catch (Exception e) {
      log.warn(
          "TimescaleDB 添加压缩策略失败: table={}, compressionAfter={}",
          tableName,
          compressionAfter,
          e);
    }
  }

  private boolean isHypertable(String tableName) {
    Boolean catalogExists =
        schemaQueryJdbc()
            .queryForObject(
                "select to_regclass('timescaledb_information.hypertables') is not null",
                Boolean.class);
    if (!Boolean.TRUE.equals(catalogExists)) {
      return false;
    }
    // If the catalog exists but the check fails, abort this table's upgrade. A timeout or
    // missing privilege must never be interpreted as permission to use ordinary-table DDL.
    return Boolean.TRUE.equals(
        schemaQueryJdbc()
            .queryForObject(
                "select exists(select 1 from timescaledb_information.hypertables "
                    + "where hypertable_schema = ? and hypertable_name = ?)",
                Boolean.class,
                schemaName,
                tableName));
  }

  private void ensurePlainIndexes(List<String> tableNames, boolean metadataTable) {
    for (String tableName : tableNames) {
      if (metadataTable) {
        createHistoryIndex(
            tableName, "history_property_v2", List.of("iot_id", "property", "create_time", "id"));
        createHistoryIndex(
            tableName, "history_event_v2", List.of("iot_id", "event", "create_time", "id"));
      } else {
        createHistoryIndex(tableName, "history_device_v2", List.of("iot_id", "create_time", "id"));
      }
    }
  }

  /** Accept equivalent existing indexes regardless of their names, including longer prefixes. */
  boolean hasSupportingIndex(String table, List<String> columns) {
    String columnArray =
        columns.stream()
            .map(value -> "'" + value + "'")
            .collect(java.util.stream.Collectors.joining(","));
    String sql =
        """
        select exists (
          select 1 from pg_index i
          join pg_class t on t.oid=i.indrelid
          join pg_namespace n on n.oid=t.relnamespace
          join pg_class ix on ix.oid=i.indexrelid
          join pg_am am on am.oid=ix.relam
          where n.nspname=? and t.relname=? and am.amname='btree'
            and i.indisvalid and i.indisready and i.indpred is null and i.indexprs is null
            and i.indnkeyatts >= %d
            and (select array_agg(a.attname::text order by k.ord)
                 from unnest(i.indkey) with ordinality k(attnum,ord)
                 join pg_attribute a on a.attrelid=t.oid and a.attnum=k.attnum
                 where k.ord <= %d) = ARRAY[%s]::text[]
            and (i.indoption[%d] & 1) = (i.indoption[%d] & 1)
        )
        """
            .formatted(
                columns.size(),
                columns.size(),
                columnArray,
                columns.size() - 2,
                columns.size() - 1);
    return Boolean.TRUE.equals(
        schemaQueryJdbc().queryForObject(sql, Boolean.class, schemaName, table));
  }

  void createHistoryIndex(String table, String suffix, List<String> columns) {
    if (hasSupportingIndex(table, columns)) {
      return;
    }
    String indexName = buildIndexName(table, suffix);
    String columnsSql =
        "("
            + columns.stream()
                .map(
                    column ->
                        quotedIdentifier(column)
                            + ("create_time".equals(column) || "id".equals(column) ? " DESC" : ""))
                .collect(java.util.stream.Collectors.joining(", "))
            + ")";
    if (isHypertable(table)) {
      if (createdTables.contains(table)) {
        executeMaintenanceDdl(
            "CREATE INDEX IF NOT EXISTS "
                + quotedIdentifier(indexName)
                + " ON "
                + qualifiedTable(table)
                + " "
                + columnsSql,
            8,
            null);
        return;
      }
      // TimescaleDB rejects CONCURRENTLY. transaction_per_chunk still locks each chunk;
      // do not automatically take those locks on a running, populated hypertable.
      log.warn(
          "hypertable {} 需要分块索引计划；维护窗口执行: CREATE INDEX {} ON {} {} WITH"
              + " (timescaledb.transaction_per_chunk)",
          qualifiedTable(table),
          quotedIdentifier(indexName),
          qualifiedTable(table),
          columnsSql);
      return;
    }
    String ddl =
        "CREATE INDEX CONCURRENTLY IF NOT EXISTS "
            + quotedIdentifier(indexName)
            + " ON "
            + qualifiedTable(table)
            + " "
            + columnsSql;
    executeMaintenanceDdl(ddl, Math.max(1, indexStatementTimeoutSeconds), indexName);
    if (!hasSupportingIndex(table, columns)) {
      log.warn("索引 {} 尚不可用，请检查 pg_index.indisvalid 后安排重试", indexName);
    }
  }

  /** A dedicated autocommit connection is required for CREATE INDEX CONCURRENTLY. */
  private void executeMaintenanceDdl(String sql, int timeoutSeconds, String indexName) {
    jdbcTemplate.execute(
        (ConnectionCallback<Void>)
            connection -> {
              if (!connection.getAutoCommit()) {
                throw new SQLException("历史表升级必须使用独立的自动提交连接");
              }
              try (Statement statement = connection.createStatement()) {
                String oldLockTimeout = sessionSetting(statement, "lock_timeout");
                String oldStatementTimeout = sessionSetting(statement, "statement_timeout");
                boolean locked = false;
                String lockKey = "nexiot.history.index." + schemaName + "." + indexName;
                try {
                  statement.setQueryTimeout(Math.max(1, timeoutSeconds));
                  statement.execute(
                      "SET lock_timeout = '"
                          + Math.max(
                              1, indexName == null ? schemaLockTimeoutMs : indexLockTimeoutMs)
                          + "ms'");
                  statement.execute(
                      "SET statement_timeout = '" + (Math.max(1L, timeoutSeconds) * 1000) + "ms'");
                  if (indexName != null) {
                    try (ResultSet result =
                        statement.executeQuery(
                            "SELECT pg_try_advisory_lock(hashtextextended('"
                                + escapeSqlLiteral(lockKey)
                                + "', 0))")) {
                      locked = result.next() && result.getBoolean(1);
                    }
                    if (!locked) {
                      log.info("另一个实例正在创建索引 {}，本实例跳过", indexName);
                      return null;
                    }
                    // A canceled concurrent build can leave an invalid index. Only repair our own
                    // generated index, never drop existing usable/operator-owned indexes.
                    String qualifiedIndex =
                        quotedIdentifier(schemaName) + "." + quotedIdentifier(indexName);
                    boolean invalidIndex;
                    try (ResultSet result =
                        statement.executeQuery(
                            "SELECT indisvalid FROM pg_index WHERE indexrelid=to_regclass('"
                                + escapeSqlLiteral(qualifiedIndex)
                                + "')")) {
                      invalidIndex = result.next() && !result.getBoolean(1);
                    }
                    if (invalidIndex) {
                      statement.execute("DROP INDEX CONCURRENTLY IF EXISTS " + qualifiedIndex);
                    }
                  }
                  statement.execute(sql);
                } finally {
                  try {
                    if (locked) {
                      statement.execute(
                          "SELECT pg_advisory_unlock(hashtextextended('"
                              + escapeSqlLiteral(lockKey)
                              + "', 0))");
                    }
                  } finally {
                    try {
                      statement.execute(
                          "SET lock_timeout = '" + escapeSqlLiteral(oldLockTimeout) + "'");
                    } finally {
                      statement.execute(
                          "SET statement_timeout = '"
                              + escapeSqlLiteral(oldStatementTimeout)
                              + "'");
                    }
                  }
                }
              }
              return null;
            });
  }

  private JdbcTemplate schemaQueryJdbc() {
    JdbcTemplate current = schemaQueryTemplate;
    if (current == null && jdbcTemplate.getDataSource() != null) {
      current = new JdbcTemplate(jdbcTemplate.getDataSource());
      current.setQueryTimeout(8);
      schemaQueryTemplate = current;
    }
    return current == null ? jdbcTemplate : current;
  }

  private String sessionSetting(Statement statement, String name) throws SQLException {
    try (ResultSet result = statement.executeQuery("SHOW " + name)) {
      if (!result.next()) {
        throw new SQLException("Unable to read PostgreSQL session setting " + name);
      }
      return result.getString(1);
    }
  }

  private String buildIndexName(String tableName, String suffix) {
    String normalized = ("idx_" + tableName + "_" + suffix).replaceAll("[^a-zA-Z0-9_]", "_");
    normalized = normalized.toLowerCase(Locale.ROOT);
    if (normalized.length() <= 63) {
      return normalized;
    }

    String hash = Integer.toHexString(normalized.hashCode());
    int prefixLength = Math.max(1, 63 - hash.length() - 1);
    return normalized.substring(0, prefixLength) + "_" + hash;
  }

  private String qualifiedTable(String tableName) {
    return quotedIdentifier(schemaName) + "." + quotedIdentifier(tableName);
  }

  private String quotedIdentifier(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  private String escapeSqlLiteral(String value) {
    return StrUtil.blankToDefault(value, "7 days").replace("'", "''");
  }

}
