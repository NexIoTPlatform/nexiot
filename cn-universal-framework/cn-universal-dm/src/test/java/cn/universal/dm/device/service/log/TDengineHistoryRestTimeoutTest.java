package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import com.taosdata.jdbc.rs.RestfulConnection;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;

class TDengineHistoryRestTimeoutTest {
  private void set(TDengineDeviceLogService service, String name, Object value) throws Exception {
    Field field = TDengineDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }

  @Test
  void historyUsesAnActualCallDeadlineEvenAfterWriteDriverResetsItsGlobalSocketTimeout()
      throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    ExecutorService executor = Executors.newCachedThreadPool();
    server.setExecutor(executor);
    server.createContext(
        "/rest/sql",
        exchange -> {
          assertEquals(
              Credentials.basic("fixture-user", "fixture-secret"),
              exchange.getRequestHeaders().getFirst("Authorization"));
          try {
            Thread.sleep(2200);
            byte[] body =
                "{\"code\":0,\"column_meta\":[[\"value\",\"BIGINT\",8]],\"data\":[[1]],\"rows\":1}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    set(service, "jdbcUrl", "jdbc:TAOS-RS://127.0.0.1:" + server.getAddress().getPort() + "/");
    set(service, "databaseName", "fixture");
    set(service, "username", "fixture-user");
    set(service, "password", "fixture-secret");
    set(service, "historyQueryTimeoutSeconds", 1);
    OkHttpClient client = service.historyHttpClient();
    try {
      // TAOS-RS 3.3.0 stores HTTP timeouts in JVM-global static fields on every connection.
      // Initializing a write connection must never replace the independent history deadline.
      Class.forName("com.taosdata.jdbc.rs.RestfulDriver");
      Properties writes = new Properties();
      writes.setProperty("user", "fixture-user");
      writes.setProperty("password", "fixture-secret");
      writes.setProperty("httpSocketTimeout", "60000");
      try (var writeConnection =
          DriverManager.getConnection(
              "jdbc:TAOS-RS://127.0.0.1:" + server.getAddress().getPort() + "/", writes)) {
        assertNotNull(writeConnection);
        JdbcTemplate history = new JdbcTemplate(service.historyRestDataSource(client));
        history.setQueryTimeout(1);
        long begin = System.nanoTime();
        assertThrows(
            QueryTimeoutException.class, () -> history.queryForObject("SELECT 1", Long.class));
        double elapsed = (System.nanoTime() - begin) / 1e9;
        assertTrue(elapsed >= .7 && elapsed < 2, "Actual history deadline took " + elapsed + "s");
      }
    } finally {
      client.dispatcher().cancelAll();
      client.dispatcher().executorService().shutdown();
      client.connectionPool().evictAll();
      server.stop(0);
      executor.shutdownNow();
    }
  }

  @Test
  void sdkResultSetMappingKeepsNumbersNullsAndTimestampsAndHasSeparateClientLifecycle()
      throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/rest/sql",
        exchange -> {
          byte[] body =
              "{\"code\":0,\"column_meta\":[[\"ts\",\"TIMESTAMP\",8],[\"content\",\"NCHAR\",64],[\"count\",\"BIGINT\",8],[\"number\",\"DOUBLE\",8]],\"data\":[[\"2026-10-12T10:46:00.000Z\",null,3000000001,-1.25]],\"rows\":1}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    set(service, "jdbcUrl", "jdbc:TAOS-RS://127.0.0.1:" + server.getAddress().getPort() + "/");
    set(service, "databaseName", "fixture");
    set(service, "username", "fixture-user");
    set(service, "password", "fixture-secret");
    set(service, "maxPoolSize", 2);
    set(service, "connectionTimeout", 1000L);
    OkHttpClient client = service.historyHttpClient();
    set(service, "historyHttpClient", client);
    HikariDataSource pool = service.historyRestPool(client);
    set(service, "historyDataSource", pool);
    try {
      // Exercise the same SELECT 1 startup validation and Hikari connection proxy as initDb().
      assertFalse(pool.isClosed());
      JdbcTemplate jdbc = new JdbcTemplate(pool);
      jdbc.query(
          "SELECT ts, content, count, number",
          rs -> {
            assertTrue(rs.next());
            assertEquals(1791801960000L, rs.getTimestamp("ts").getTime());
            assertNull(rs.getString("content"));
            assertEquals(3000000001L, rs.getLong("count"));
            assertEquals(-1.25, rs.getDouble("number"));
            assertFalse(rs.next());
            return null;
          });
    } finally {
      service.destroy();
      server.stop(0);
    }
    assertTrue(pool.isClosed());
    assertTrue(client.dispatcher().executorService().isShutdown());
    assertEquals(0, client.connectionPool().connectionCount());
  }

  @Test
  void inheritedJdbcOverloadsCannotBypassHistoryClientAndIpv6SslUrlIsPreserved() throws Exception {
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    set(service, "jdbcUrl", "jdbc:TAOS-RS://[::1]:6041/fixture?useSSL=true");
    set(service, "databaseName", "fixture");
    set(service, "username", "fixture-user");
    set(service, "password", "fixture-secret");
    OkHttpClient client = service.historyHttpClient();
    set(service, "historyHttpClient", client);
    try (var connection = service.historyRestDataSource(client).getConnection()) {
      assertEquals(
          Credentials.basic("fixture-user", "fixture-secret"),
          ((RestfulConnection) connection).getAuth());
      assertThrows(
          SQLFeatureNotSupportedException.class, () -> connection.prepareStatement("SELECT ?"));
      assertThrows(
          SQLFeatureNotSupportedException.class,
          () ->
              connection.prepareStatement(
                  "SELECT ?",
                  java.sql.ResultSet.TYPE_FORWARD_ONLY,
                  java.sql.ResultSet.CONCUR_READ_ONLY));
      assertThrows(
          SQLFeatureNotSupportedException.class,
          () -> connection.prepareStatement("SELECT ?", Statement.NO_GENERATED_KEYS));
      try (Statement statement =
          connection.createStatement(
              java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY)) {
        Field endpoint = statement.getClass().getDeclaredField("endpoint");
        endpoint.setAccessible(true);
        assertEquals("https://[::1]:6041/rest/sql", endpoint.get(statement));
      }
    } finally {
      service.destroy();
    }
  }

  @Test
  void serverSqlErrorsAndMalformedJsonKeepSqlCause() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/rest/sql",
        exchange -> {
          String sql = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          byte[] body =
              (sql.equals("ERROR")
                      ? "{\"code\":12345,\"desc\":\"fixture SQL error\"}"
                      : "invalid JSON")
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    set(service, "jdbcUrl", "jdbc:TAOS-RS://127.0.0.1:" + server.getAddress().getPort() + "/");
    set(service, "databaseName", "fixture");
    set(service, "username", "fixture-user");
    set(service, "password", "fixture-secret");
    OkHttpClient client = service.historyHttpClient();
    set(service, "historyHttpClient", client);
    try (var connection = service.historyRestDataSource(client).getConnection();
        var statement = connection.createStatement()) {
      SQLException sql = assertThrows(SQLException.class, () -> statement.executeQuery("ERROR"));
      assertEquals(12345, sql.getErrorCode());
      assertEquals("fixture SQL error", sql.getMessage());
      SQLException malformed =
          assertThrows(SQLException.class, () -> statement.executeQuery("MALFORMED"));
      assertNotNull(malformed.getCause());
    } finally {
      service.destroy();
      server.stop(0);
    }
  }

  @Test
  void statementAndNetworkTimeoutsBoundCallsUsingTheSameEightSecondClient() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    ExecutorService executor = Executors.newCachedThreadPool();
    server.setExecutor(executor);
    server.createContext(
        "/rest/sql",
        exchange -> {
          try {
            Thread.sleep(2200);
            byte[] body =
                "{\"code\":0,\"column_meta\":[[\"value\",\"BIGINT\",8]],\"data\":[[1]],\"rows\":1}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    set(service, "jdbcUrl", "jdbc:TAOS-RS://127.0.0.1:" + server.getAddress().getPort() + "/");
    set(service, "databaseName", "fixture");
    set(service, "username", "fixture-user");
    set(service, "password", "fixture-secret");
    set(service, "historyQueryTimeoutSeconds", 8);
    OkHttpClient client = service.historyHttpClient();
    set(service, "historyHttpClient", client);
    try (var connection = service.historyRestDataSource(client).getConnection()) {
      for (boolean network : new boolean[] {false, true}) {
        try (Statement statement = connection.createStatement()) {
          if (network) {
            connection.setNetworkTimeout(Runnable::run, 1000);
            assertEquals(1000, connection.getNetworkTimeout());
            statement.setQueryTimeout(8);
          } else {
            statement.setQueryTimeout(1);
            assertEquals(1, statement.getQueryTimeout());
          }
          long started = System.nanoTime();
          assertThrows(SQLTimeoutException.class, () -> statement.executeQuery("SELECT 1"));
          double elapsed = (System.nanoTime() - started) / 1e9;
          assertTrue(elapsed >= .7 && elapsed < 2, "Per-call deadline took " + elapsed + "s");
        }
      }
    } finally {
      service.destroy();
      server.stop(0);
      executor.shutdownNow();
    }
  }

  @Test
  void idleHikariCheckoutValidatesHealthWithOneSecondNetworkTimeout() throws Exception {
    AtomicBoolean slow = new AtomicBoolean();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    ExecutorService executor = Executors.newCachedThreadPool();
    server.setExecutor(executor);
    server.createContext(
        "/rest/sql",
        exchange -> {
          try {
            if (slow.get()) Thread.sleep(2200);
            byte[] body =
                "{\"code\":0,\"column_meta\":[[\"value\",\"BIGINT\",8]],\"data\":[[1]],\"rows\":1}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    set(service, "jdbcUrl", "jdbc:TAOS-RS://127.0.0.1:" + server.getAddress().getPort() + "/");
    set(service, "databaseName", "fixture");
    set(service, "username", "fixture-user");
    set(service, "password", "fixture-secret");
    set(service, "historyQueryTimeoutSeconds", 8);
    set(service, "maxPoolSize", 1);
    set(service, "connectionTimeout", 500L);
    OkHttpClient client = service.historyHttpClient();
    set(service, "historyHttpClient", client);
    HikariDataSource pool = service.historyRestPool(client);
    set(service, "historyDataSource", pool);
    try {
      assertEquals(1L, new JdbcTemplate(pool).queryForObject("SELECT 1", Long.class));
      slow.set(true);
      Thread.sleep(600); // Hikari revalidates after its 500ms idle bypass window.
      long started = System.nanoTime();
      assertThrows(SQLTransientConnectionException.class, pool::getConnection);
      double elapsed = (System.nanoTime() - started) / 1e9;
      assertTrue(elapsed >= .7 && elapsed < 2, "Idle health validation took " + elapsed + "s");
    } finally {
      service.destroy();
      server.stop(0);
      executor.shutdownNow();
    }
  }
}
