package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

class TimescaleHistorySchemaTest {
  private static final List<String> COLUMNS = List.of("iot_id", "property", "create_time", "id");

  private void set(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private TimescaleDBDeviceLogService service(JdbcTemplate jdbc) throws Exception {
    TimescaleDBDeviceLogService service = new TimescaleDBDeviceLogService();
    set(service, "jdbcTemplate", jdbc);
    set(service, "schemaName", "history");
    return service;
  }

  private ResultSet stringResult(String value) throws Exception {
    ResultSet result = mock(ResultSet.class);
    when(result.next()).thenReturn(true, false);
    when(result.getString(1)).thenReturn(value);
    return result;
  }

  @SuppressWarnings("unchecked")
  private Statement ddlConnection(JdbcTemplate jdbc) throws Exception {
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    when(connection.getAutoCommit()).thenReturn(true);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery("SHOW lock_timeout")).thenAnswer(invocation -> stringResult("0"));
    when(statement.executeQuery("SHOW statement_timeout"))
        .thenAnswer(invocation -> stringResult("0"));
    when(statement.executeQuery(startsWith("SELECT pg_try_advisory_lock")))
        .thenAnswer(
            invocation -> {
              ResultSet result = mock(ResultSet.class);
              when(result.next()).thenReturn(true);
              when(result.getBoolean(1)).thenReturn(true);
              return result;
            });
    ResultSet absent = mock(ResultSet.class);
    when(absent.next()).thenReturn(false);
    when(statement.executeQuery(startsWith("SELECT indisvalid"))).thenReturn(absent);
    when(jdbc.execute(any(ConnectionCallback.class)))
        .thenAnswer(
            invocation ->
                ((ConnectionCallback<Object>) invocation.getArgument(0))
                    .doInConnection(connection));
    return statement;
  }

  @Test
  void newCursorIndexIsConcurrentAndRepeatedUpgradeReusesIt() throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    Statement statement = ddlConnection(jdbc);
    when(jdbc.queryForObject(
            contains("from pg_index i"), eq(Boolean.class), eq("history"), eq("logs")))
        .thenReturn(false, true, true);
    when(jdbc.queryForObject(
            contains("timescaledb_information"), eq(Boolean.class), eq("history"), eq("logs")))
        .thenReturn(false);
    TimescaleDBDeviceLogService service = service(jdbc);
    service.createHistoryIndex("logs", "history_property_v2", COLUMNS);
    service.createHistoryIndex("logs", "history_property_v2", COLUMNS);
    verify(statement, times(1)).execute(startsWith("CREATE INDEX CONCURRENTLY IF NOT EXISTS"));
    verify(statement).execute("SET lock_timeout = '30000ms'");
    verify(statement).execute("SET statement_timeout = '300000ms'");
    verify(statement).execute("SET lock_timeout = '0'");
    verify(statement).execute("SET statement_timeout = '0'");
    verify(statement).execute(startsWith("SELECT pg_advisory_unlock"));
    service.stopSchemaUpgrade();
  }

  @Test
  void equivalentOperatorIndexIsReusedWithoutAnyDdl() throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(
            contains("from pg_index i"), eq(Boolean.class), eq("history"), eq("logs")))
        .thenReturn(true);
    TimescaleDBDeviceLogService service = service(jdbc);
    service.createHistoryIndex("logs", "history_property_v2", COLUMNS);
    verify(jdbc, never()).execute(any(ConnectionCallback.class));
    service.stopSchemaUpgrade();
  }

  @Test
  void existingHypertableRequiresChunkMaintenanceInsteadOfUnsupportedConcurrentDdl()
      throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(
            contains("timescaledb_information"), eq(Boolean.class), eq("history"), eq("logs")))
        .thenReturn(true);
    when(jdbc.queryForObject(
            eq("select to_regclass('timescaledb_information.hypertables') is not null"),
            eq(Boolean.class)))
        .thenReturn(true);
    TimescaleDBDeviceLogService service = service(jdbc);
    service.createHistoryIndex("logs", "history_property_v2", COLUMNS);
    verify(jdbc, never()).execute(any(ConnectionCallback.class));
    service.stopSchemaUpgrade();
  }

  @Test
  void existingTablesAreNeverRewrittenOrGivenInventedTimestampsOnStartup() throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    Statement statement = ddlConnection(jdbc);
    when(jdbc.queryForObject(
            eq("select to_regclass(?) is not null"), eq(Boolean.class), anyString()))
        .thenReturn(true);
    TimescaleDBDeviceLogService service = service(jdbc);
    set(service, "autoInit", true);
    set(service, "configuredSchemaName", "history");
    set(service, "logTableName", "logs");
    set(service, "logMetaTableName", "metadata");
    service.initTimescale();
    Method convert =
        TimescaleDBDeviceLogService.class.getDeclaredMethod("ensureHypertable", String.class);
    convert.setAccessible(true);
    convert.invoke(service, "logs");
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(statement, atLeastOnce()).execute(sql.capture());
    assertTrue(
        sql.getAllValues().stream()
            .noneMatch(
                value -> {
                  String normalized = value.toLowerCase();
                  return normalized.contains("alter table")
                      || normalized.contains("update ")
                      || normalized.contains("create_hypertable")
                      || normalized.contains("create index");
                }));
    service.stopSchemaUpgrade();
  }

  @Test
  void failureReleasesAdvisoryLockAndRestoresPooledConnectionSettings() throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    Statement statement = ddlConnection(jdbc);
    when(statement.execute(startsWith("CREATE INDEX CONCURRENTLY")))
        .thenThrow(new SQLException("lock timeout"));
    TimescaleDBDeviceLogService service = service(jdbc);
    assertThrows(
        Exception.class, () -> service.createHistoryIndex("logs", "history_property_v2", COLUMNS));
    verify(statement).execute(startsWith("SELECT pg_advisory_unlock"));
    verify(statement).execute("SET lock_timeout = '0'");
    verify(statement).execute("SET statement_timeout = '0'");
    service.stopSchemaUpgrade();
  }
}
