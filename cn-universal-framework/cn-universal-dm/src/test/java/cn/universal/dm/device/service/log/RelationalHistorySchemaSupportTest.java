package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

class RelationalHistorySchemaSupportTest {
  static class Fixture implements AutoCloseable {
    final RelationalHistorySchemaSupport service = new RelationalHistorySchemaSupport();
    final Connection connection = mock(Connection.class);
    final Statement statement = mock(Statement.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final AtomicBoolean temporal = new AtomicBoolean(false);
    final AtomicBoolean bigintId = new AtomicBoolean(true);
    final AtomicBoolean guard = new AtomicBoolean(false);
    final List<String> ddl = new ArrayList<>();
    boolean rows;
    boolean guardRejects;
    boolean lockAvailable = true;
    boolean indexFails;

    @SuppressWarnings("unchecked")
    Fixture(String dialect) throws Exception {
      Field field = RelationalHistorySchemaSupport.class.getDeclaredField("jdbcTemplate");
      field.setAccessible(true);
      field.set(service, jdbc);
      DatabaseMetaData db = mock(DatabaseMetaData.class);
      when(connection.getMetaData()).thenReturn(db);
      when(db.getDatabaseProductName()).thenReturn(dialect);
      when(db.getDatabaseProductVersion()).thenReturn("8.4.7");
      when(connection.getAutoCommit()).thenReturn(true);
      when(connection.getSchema()).thenReturn("UNIV");
      when(connection.getCatalog()).thenReturn("univ");
      when(connection.createStatement()).thenReturn(statement);
      when(jdbc.execute(any(ConnectionCallback.class)))
          .thenAnswer(
              invocation ->
                  ((ConnectionCallback<Object>) invocation.getArgument(0))
                      .doInConnection(connection));
      when(statement.executeQuery(anyString()))
          .thenAnswer(
              invocation -> {
                String sql = invocation.getArgument(0);
                ResultSet result = mock(ResultSet.class);
                if (sql.contains("GET_LOCK") || sql.contains("DBMS_LOCK.REQUEST")) {
                  when(result.next()).thenReturn(true, false);
                  when(result.getInt(1))
                      .thenReturn(
                          dialect.equals("MySQL")
                              ? (lockAvailable ? 1 : 0)
                              : (lockAvailable ? 0 : 1));
                } else if (sql.contains("WHERE 1=0")) {
                  ResultSetMetaData metadata = mock(ResultSetMetaData.class);
                  when(result.getMetaData()).thenReturn(metadata);
                  when(metadata.getColumnCount()).thenReturn(2);
                  when(metadata.getColumnName(1)).thenReturn("id");
                  when(metadata.getColumnName(2)).thenReturn("create_time");
                  when(metadata.getColumnType(1))
                      .thenReturn(bigintId.get() ? Types.BIGINT : Types.INTEGER);
                  when(metadata.getColumnType(2))
                      .thenReturn(temporal.get() ? Types.TIMESTAMP : Types.BIGINT);
                  when(metadata.getColumnTypeName(1))
                      .thenReturn(bigintId.get() ? "BIGINT" : "INT UNSIGNED");
                  when(metadata.getColumnTypeName(2))
                      .thenReturn(temporal.get() ? "TIMESTAMP" : "BIGINT");
                  when(metadata.isNullable(anyInt())).thenReturn(ResultSetMetaData.columnNoNulls);
                } else {
                  when(result.next()).thenReturn(rows);
                }
                return result;
              });
      when(statement.execute(anyString()))
          .thenAnswer(
              invocation -> {
                String sql = invocation.getArgument(0);
                ddl.add(sql);
                if (indexFails && (sql.contains("ADD INDEX") || sql.contains("CREATE INDEX"))) {
                  throw new SQLException("online index creation unavailable");
                }
                if (sql.contains("CHECK (1=0)")) {
                  if (guardRejects) throw new SQLException("a competing writer inserted a row");
                  guard.set(true);
                }
                if (sql.contains("MODIFY") && sql.contains("create_time")) temporal.set(true);
                if (sql.contains("MODIFY") && sql.contains("`id`")) bigintId.set(true);
                if (sql.contains(" DROP ")) guard.set(false);
                return true;
              });
      when(connection.prepareStatement(anyString()))
          .thenAnswer(
              invocation -> {
                PreparedStatement prepared = mock(PreparedStatement.class);
                String sql = invocation.getArgument(0);
                when(prepared.executeQuery())
                    .thenAnswer(
                        call -> {
                          ResultSet result = mock(ResultSet.class);
                          when(result.next())
                              .thenReturn(sql.contains("constraints") && guard.get());
                          return result;
                        });
                return prepared;
              });
    }

    void migrate() throws Exception {
      Method method =
          RelationalHistorySchemaSupport.class.getDeclaredMethod(
              "migrateEmptyTable",
              Connection.class,
              RelationalHistorySchemaSupport.Table.class,
              Map.class);
      method.setAccessible(true);
      var dialect = RelationalHistorySchemaSupport.dialect(connection.getMetaData());
      method.invoke(
          service,
          connection,
          new RelationalHistorySchemaSupport.Table("UNIV", "logs", dialect),
          Map.of(
              "id",
              new RelationalHistorySchemaSupport.Column(
                  bigintId.get() ? Types.BIGINT : Types.INTEGER,
                  bigintId.get() ? "BIGINT" : "INT UNSIGNED",
                  false,
                  false),
              "create_time",
              new RelationalHistorySchemaSupport.Column(Types.BIGINT, "BIGINT", false, false)));
    }

    @Override
    public void close() {
      service.close();
    }
  }

  @Test
  void postgresSkipsThisComponentsSchemaAndDdl() throws Exception {
    try (Fixture f = new Fixture("PostgreSQL")) {
      f.service.requireReady("logs");
      f.service.upgradeTables(List.of("logs"), false);
      verify(f.connection, never()).createStatement();
      assertTrue(f.ddl.isEmpty());
    }
  }

  @Test
  void queryGuardNeverSilentlyReadsLegacyEpochSchemaOrRunsDdl() throws Exception {
    try (Fixture f = new Fixture("DM DBMS")) {
      assertThrows(IoTException.class, () -> f.service.requireReady("logs"));
      assertFalse(f.service.readinessSnapshot().get("logs").ready());
      assertEquals(1, f.service.metrics().migrationRequired());
      assertTrue(f.ddl.isEmpty());
    }
  }

  @Test
  void nonemptyLegacyRowsCannotBeMigratedOrAssignedGuessedTimeUnits() throws Exception {
    try (Fixture f = new Fixture("DM DBMS")) {
      f.rows = true;
      assertThrows(Exception.class, f::migrate);
      assertTrue(f.ddl.isEmpty());
      assertFalse(f.temporal.get());
    }
  }

  @Test
  void emptyDmMigrationKeepsEnforcedWriteGuardAcrossImplicitDdlCommits() throws Exception {
    try (Fixture f = new Fixture("DM DBMS")) {
      f.migrate();
      assertTrue(f.temporal.get());
      assertFalse(f.guard.get());
      assertEquals(3, f.ddl.size());
      assertTrue(f.ddl.get(0).contains("CHECK (1=0)"));
      assertTrue(f.ddl.get(1).contains("MODIFY \"create_time\" TIMESTAMP(6) NOT NULL"));
      assertTrue(f.ddl.get(2).contains("DROP CONSTRAINT"));
      assertEquals(1, f.service.metrics().migratedTables());
    }
  }

  @Test
  void racingWriterMakesTheAtomicEmptyGuardFailBeforeColumnConversion() throws Exception {
    try (Fixture f = new Fixture("DM DBMS")) {
      f.guardRejects = true;
      assertThrows(Exception.class, f::migrate);
      assertFalse(f.temporal.get());
      assertEquals(1, f.ddl.size());
      assertFalse(f.ddl.getFirst().contains("MODIFY"));
    }
  }

  @Test
  void emptyMysqlMigrationWidensSnowflakeIdAndTimestampTogether() throws Exception {
    try (Fixture f = new Fixture("MySQL")) {
      f.bigintId.set(false);
      f.migrate();
      assertTrue(f.temporal.get());
      assertTrue(f.bigintId.get());
      assertTrue(
          f.ddl.stream().anyMatch(sql -> sql.contains("MODIFY `id` BIGINT UNSIGNED NOT NULL")));
      assertTrue(f.ddl.stream().anyMatch(sql -> sql.contains("MODIFY `create_time` DATETIME(6)")));
      assertFalse(f.guard.get());
    }
  }

  @Test
  void readableIntegerIdsAreNotConfusedWithSnowflakeWriteCapacity() throws Exception {
    try (Fixture f = new Fixture("MySQL")) {
      f.temporal.set(true);
      f.bigintId.set(false);
      f.service.requireReady("logs");
      assertTrue(f.service.readinessSnapshot().get("logs").ready());
      assertTrue(f.ddl.isEmpty());
    }
  }

  @Test
  void mysqlBeforeEnforcedCheckVersionCannotAutoMigrate() throws Exception {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    for (String version : List.of("5.7.44", "8.0.15", "unknown")) {
      when(metadata.getDatabaseProductVersion()).thenReturn(version);
      assertThrows(
          SQLException.class,
          () ->
              RelationalHistorySchemaSupport.requireEnforcedCheck(
                  metadata, RelationalHistorySchemaSupport.Dialect.MYSQL));
    }
    when(metadata.getDatabaseProductVersion()).thenReturn("8.0.16");
    RelationalHistorySchemaSupport.requireEnforcedCheck(
        metadata, RelationalHistorySchemaSupport.Dialect.MYSQL);
  }

  @Test
  void indexFailureReleasesMaintenanceLockAndDoesNotBlockCompatibleReads() throws Exception {
    try (Fixture f = new Fixture("MySQL")) {
      f.temporal.set(true);
      f.indexFails = true;
      f.service.upgradeTables(List.of("logs"), false);
      assertTrue(
          f.service
              .maintenanceWarnings()
              .get("logs")
              .contains("online index creation unavailable"));
      assertTrue(f.ddl.stream().anyMatch(sql -> sql.contains("RELEASE_LOCK")));
      assertFalse(f.ddl.stream().anyMatch(sql -> sql.contains("MODIFY")));
      f.service.requireReady("logs");
      assertTrue(f.service.readinessSnapshot().get("logs").ready());
    }
  }

  @Test
  void concurrentMaintenanceDoesNotRunDdlOrPreventReads() throws Exception {
    try (Fixture f = new Fixture("DM DBMS")) {
      f.temporal.set(true);
      f.lockAvailable = false;
      f.service.upgradeTables(List.of("logs"), false);
      assertTrue(f.ddl.isEmpty());
      assertTrue(f.service.maintenanceWarnings().containsKey("logs"));
      f.service.requireReady("logs");
      assertTrue(f.service.readinessSnapshot().get("logs").ready());
    }
  }

  @Test
  void mysql57IndexCatalogDoesNotReferenceMysql8VisibilityColumn() throws Exception {
    try (Fixture f = new Fixture("MySQL")) {
      when(f.connection.getMetaData().getDatabaseProductVersion()).thenReturn("5.7.44");
      f.temporal.set(true);
      f.indexFails = true;
      f.service.upgradeTables(List.of("logs"), false);
      verify(f.connection, atLeastOnce())
          .prepareStatement(
              argThat(
                  sql ->
                      sql.contains("information_schema.statistics")
                          && !sql.contains("is_visible")));
      verify(f.connection, never()).prepareStatement(contains("is_visible"));
    }
  }

  @Test
  void aPreviousCompletedMigrationGuardIsRecoveredBeforeIndexMaintenance() throws Exception {
    try (Fixture f = new Fixture("MySQL")) {
      f.temporal.set(true);
      f.guard.set(true);
      f.indexFails = true;
      assertThrows(IoTException.class, () -> f.service.requireReady("logs"));
      f.service.upgradeTables(List.of("logs"), false);
      assertFalse(f.guard.get());
      assertTrue(f.ddl.stream().anyMatch(sql -> sql.contains("DROP CHECK")));
      assertFalse(f.ddl.stream().anyMatch(sql -> sql.contains("MODIFY")));
      f.service.requireReady("logs");
      assertTrue(f.service.readinessSnapshot().get("logs").ready());
    }
  }

  @Test
  void malformedIdentifiersAndUnboundedShardCountsAreRejected() {
    for (String name : List.of("logs;DROP TABLE x", "logs'", "public.logs.x", "logs.", "logs--")) {
      assertThrows(
          IllegalArgumentException.class,
          () -> RelationalHistorySchemaSupport.validateTableName(name));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> RelationalHistorySchemaSupport.managedTables("UNIV", "logs", true, 5000));
    assertEquals(
        List.of("UNIV.logs", "UNIV.logs_0", "UNIV.logs_1"),
        RelationalHistorySchemaSupport.managedTables("UNIV", "logs", true, 2));
  }
}
