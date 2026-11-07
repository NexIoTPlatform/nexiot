package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class ApplicationPushHttpClientTest {

  @Test
  void shouldReuseConnectionForSequentialPushes() throws Exception {
    Set<Integer> remotePorts = ConcurrentHashMap.newKeySet();
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/push",
        exchange -> {
          remotePorts.add(exchange.getRemoteAddress().getPort());
          respond(exchange);
        });
    server.start();
    ApplicationPushHttpClient client = client(500, 800, 800, 1_000);
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/push";
      for (int index = 0; index < 10; index++) {
        assertEquals(204, client.postJson(url, "{}", null));
      }

      assertEquals(1, remotePorts.size());
      assertEquals(1, client.connectionCount());
      assertEquals(1, client.idleConnectionCount());
    } finally {
      client.close();
      server.stop(0);
    }
  }

  @Test
  void shouldEnforceTotalCallTimeout() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/slow",
        exchange -> {
          try {
            Thread.sleep(500);
            respond(exchange);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          } catch (IOException ignored) {
            exchange.close();
          }
        });
    server.start();
    ApplicationPushHttpClient client = client(200, 200, 200, 100);
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/slow";
      long startedAt = System.nanoTime();

      assertThrows(IOException.class, () -> client.postJson(url, "{}", null));

      long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
      assertTrue(elapsedMs < 1_000, "call timeout must bound the complete request");
    } finally {
      client.close();
      server.stop(0);
    }
  }

  private ApplicationPushHttpClient client(
      long connectTimeoutMs,
      long readTimeoutMs,
      long writeTimeoutMs,
      long callTimeoutMs) {
    return new ApplicationPushHttpClient(
        new SimpleMeterRegistry(),
        64,
        300_000,
        connectTimeoutMs,
        readTimeoutMs,
        writeTimeoutMs,
        callTimeoutMs);
  }

  private static void respond(HttpExchange exchange) throws IOException {
    exchange.getRequestBody().readAllBytes();
    exchange.sendResponseHeaders(204, -1);
    exchange.close();
  }
}
