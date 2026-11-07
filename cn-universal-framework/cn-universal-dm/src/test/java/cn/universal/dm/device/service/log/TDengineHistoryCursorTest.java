package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.query.LogQuery;
import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

class TDengineHistoryCursorTest {
  private static final long TIMESTAMP = 1791801960000L;

  private void set(Object service, String name, Object value) throws Exception {
    Field field = TDengineDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }

  private LogQuery query() {
    LogQuery query = new LogQuery();
    query.setProductKey("fixture");
    query.setIotId("fixture-device");
    query.setProperty("M200");
    query.setBeginCreateTime(TIMESTAMP / 1000 - 1);
    query.setEndCreateTime(TIMESTAMP / 1000 + 1);
    query.setPageSize(2);
    query.setCursorMode(true);
    return query;
  }

  private String response(List<Long> identifiers) {
    String meta =
        "[[\"ts\",\"TIMESTAMP\",8],[\"id\",\"BIGINT\",8],"
            + "[\"device_name\",\"NCHAR\",64],[\"message_type\",\"NCHAR\",32],"
            + "[\"event\",\"NCHAR\",128],[\"property\",\"NCHAR\",64],"
            + "[\"content\",\"NCHAR\",4096],[\"ext1\",\"NCHAR\",512],"
            + "[\"ext2\",\"NCHAR\",1024],[\"ext3\",\"NCHAR\",512],"
            + "[\"iot_id\",\"NCHAR\",256],[\"device_id\",\"NCHAR\",128],"
            + "[\"product_key\",\"NCHAR\",128]]";
    List<String> rows = new ArrayList<>();
    for (long id : identifiers) {
      rows.add(
          "[\"2026-10-12T10:46:00.000Z\","
              + id
              + ",\"fixture\",\"PROPERTIES\",null,\"M200\",\""
              + id
              + "\",null,null,null,\"fixture-device\",\"fixture\",\"fixture\"]");
    }
    return "{\"code\":0,\"column_meta\":"
        + meta
        + ",\"data\":["
        + String.join(",", rows)
        + "],\"rows\":"
        + rows.size()
        + "}";
  }

  private TDengineDeviceLogService service(HttpServer server, int seconds) throws Exception {
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    set(service, "jdbcUrl", "jdbc:TAOS-RS://127.0.0.1:" + server.getAddress().getPort() + "/");
    set(service, "databaseName", "fixture");
    set(service, "devLogMetaTableName", "metadata");
    set(service, "username", "fixture-user");
    set(service, "password", "fixture-secret");
    set(service, "historyQueryTimeoutSeconds", seconds);
    OkHttpClient client = service.historyHttpClient();
    set(service, "historyHttpClient", client);
    set(service, "historyJdbcTemplate", new JdbcTemplate(service.historyRestDataSource(client)));
    return service;
  }

  @Test
  void tiesUseOneTimestampSortAndCursorPagesDoNotSkipOrDuplicateRows() throws Exception {
    List<String> sqls = new java.util.concurrent.CopyOnWriteArrayList<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/rest/sql",
        exchange -> {
          String sql = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          sqls.add(sql);
          var matcher = Pattern.compile("AND id < ([0-9]+)").matcher(sql);
          long before = matcher.find() ? Long.parseLong(matcher.group(1)) : Long.MAX_VALUE;
          List<Long> candidates = new ArrayList<>();
          for (long id = 1; id <= 5; id++) if (id < before) candidates.add(id);
          if (sql.contains("ORDER BY id DESC"))
            candidates.sort(java.util.Comparator.reverseOrder());
          byte[] body =
              response(candidates.subList(0, Math.min(3, candidates.size())))
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    TDengineDeviceLogService service = service(server, 8);
    try {
      LogQuery query = query();
      List<Long> seen = new ArrayList<>();
      for (int page = 0; page < 3; page++) {
        HistoryPageSupport.prepare(query, true);
        var result = service.queryLogMeta(query);
        seen.addAll(result.getList().stream().map(IoTDeviceLogMetadataVO::getId).toList());
        assertEquals(-1L, result.getTotalCount());
        assertEquals(page < 2, result.isHasMore());
        query.setCursor(result.getNextCursor());
      }
      assertEquals(List.of(5L, 4L, 3L, 2L, 1L), seen);
      query.setCursor(null);
      HistoryPageSupport.prepare(query, true);
      assertEquals(
          List.of(5L, 4L),
          service.queryLogMeta(query).getList().stream()
              .map(IoTDeviceLogMetadataVO::getId)
              .toList());
      assertEquals(7, sqls.size());
      for (String sql : sqls) {
        assertFalse(sql.contains("COUNT(") || sql.contains("OFFSET"));
        if (sql.contains("ORDER BY id DESC")) assertTrue(sql.contains("AND ts = " + TIMESTAMP));
        else assertTrue(sql.endsWith("ORDER BY ts DESC LIMIT 3"));
      }
    } finally {
      service.destroy();
      server.stop(0);
    }
  }

  @Test
  void bothCursorReadsShareOneDeadlineAndDoNotReturnPartialRows() throws Exception {
    List<String> sqls = new java.util.concurrent.CopyOnWriteArrayList<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var executor = Executors.newCachedThreadPool();
    server.setExecutor(executor);
    server.createContext(
        "/rest/sql",
        exchange -> {
          String sql = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          sqls.add(sql);
          try {
            Thread.sleep(sql.contains("ORDER BY id DESC") ? 1000 : 650);
            byte[] body = response(List.of(1L, 2L, 3L)).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    TDengineDeviceLogService service = service(server, 1);
    try {
      long started = System.nanoTime();
      IoTException failure = assertThrows(IoTException.class, () -> service.queryLogMeta(query()));
      double elapsed = (System.nanoTime() - started) / 1e9;
      assertEquals(503, failure.getCode());
      assertInstanceOf(QueryTimeoutException.class, failure.getCause());
      assertEquals(2, sqls.size());
      assertTrue(elapsed >= .8 && elapsed < 1.4, "Combined cursor deadline took " + elapsed + "s");
    } finally {
      service.destroy();
      server.stop(0);
      executor.shutdownNow();
    }
  }

  @Test
  void nativeDriversWithoutNetworkTimeoutStillUseStatementTimeout() throws Exception {
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet result = mock(ResultSet.class);
    when(connection.getNetworkTimeout()).thenThrow(new SQLFeatureNotSupportedException());
    doThrow(new SQLFeatureNotSupportedException())
        .when(connection)
        .setNetworkTimeout(any(), anyInt());
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(result);
    doAnswer(
            invocation ->
                ((ConnectionCallback<?>) invocation.getArgument(0)).doInConnection(connection))
        .when(jdbc)
        .execute(any(ConnectionCallback.class));
    set(service, "historyJdbcTemplate", jdbc);
    set(service, "databaseName", "fixture");
    set(service, "devLogMetaTableName", "metadata");
    assertTrue(service.queryLogMeta(query()).getList().isEmpty());
    verify(statement).setQueryTimeout(intThat(seconds -> seconds > 0 && seconds <= 8));
  }

  @Test
  void hikariEvictionAfterNetworkFailureKeepsTheOriginalConnectionCause() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/rest/sql",
        exchange -> {
          String sql = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          if (!sql.contains("ORDER BY id DESC")) {
            byte[] body = response(List.of(1L, 2L, 3L)).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          }
          // EOF before headers on the second query causes SQLSTATE 08006 and Hikari eviction.
          exchange.close();
        });
    server.start();
    TDengineDeviceLogService service = service(server, 8);
    set(service, "maxPoolSize", 1);
    set(service, "connectionTimeout", 1000L);
    Field clientField = TDengineDeviceLogService.class.getDeclaredField("historyHttpClient");
    clientField.setAccessible(true);
    OkHttpClient client = (OkHttpClient) clientField.get(service);
    HikariDataSource pool = service.historyRestPool(client);
    set(service, "historyDataSource", pool);
    set(service, "historyJdbcTemplate", new JdbcTemplate(pool));
    try {
      IoTException failure = assertThrows(IoTException.class, () -> service.queryLogMeta(query()));
      assertEquals(503, failure.getCode());
      var sql =
          assertInstanceOf(
              java.sql.SQLTransientConnectionException.class, failure.getCause().getCause());
      assertEquals("08006", sql.getSQLState());
      assertInstanceOf(java.io.IOException.class, sql.getCause());
      assertFalse(sql.getMessage().contains("Connection is closed"));
    } finally {
      service.destroy();
      server.stop(0);
    }
    assertTrue(pool.isClosed());
    assertEquals(0, client.dispatcher().runningCallsCount());
    assertEquals(0, client.dispatcher().queuedCallsCount());
    assertFalse(
        Thread.getAllStackTraces().keySet().stream()
            .anyMatch(
                thread ->
                    thread.isAlive() && thread.getName().startsWith("TDengine-History-Pool")));
  }
}
