package cn.universal.dm.device.service.push;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Shared connection-pooled HTTP client for application and device-subscription push. */
@Slf4j
@Component
public class ApplicationPushHttpClient {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private final ConnectionPool connectionPool;
  private final OkHttpClient client;

  public ApplicationPushHttpClient(
      MeterRegistry meterRegistry,
      @Value("${nexiot.push.http.pool.max-idle-connections:64}") int maxIdleConnections,
      @Value("${nexiot.push.http.pool.keep-alive-ms:300000}") long keepAliveMs,
      @Value("${nexiot.push.http.connect-timeout-ms:500}") long connectTimeoutMs,
      @Value("${nexiot.push.http.read-timeout-ms:800}") long readTimeoutMs,
      @Value("${nexiot.push.http.write-timeout-ms:800}") long writeTimeoutMs,
      @Value("${nexiot.push.http.call-timeout-ms:1000}") long callTimeoutMs) {
    int normalizedMaxIdle = Math.max(1, maxIdleConnections);
    long normalizedKeepAliveMs = Math.max(1000L, keepAliveMs);
    this.connectionPool =
        new ConnectionPool(normalizedMaxIdle, normalizedKeepAliveMs, TimeUnit.MILLISECONDS);
    this.client =
        new OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .connectTimeout(Math.max(100L, connectTimeoutMs), TimeUnit.MILLISECONDS)
            .readTimeout(Math.max(100L, readTimeoutMs), TimeUnit.MILLISECONDS)
            .writeTimeout(Math.max(100L, writeTimeoutMs), TimeUnit.MILLISECONDS)
            .callTimeout(Math.max(100L, callTimeoutMs), TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build();
    Gauge.builder(
            "nexiot.push.http.connections", connectionPool, ConnectionPool::connectionCount)
        .tag("state", "total")
        .register(meterRegistry);
    Gauge.builder(
            "nexiot.push.http.connections", connectionPool, ConnectionPool::idleConnectionCount)
        .tag("state", "idle")
        .register(meterRegistry);
    log.info(
        "Application push HTTP pool started maxIdle={}, keepAliveMs={}, connectTimeoutMs={}, readTimeoutMs={}, writeTimeoutMs={}, callTimeoutMs={}",
        normalizedMaxIdle,
        normalizedKeepAliveMs,
        Math.max(100L, connectTimeoutMs),
        Math.max(100L, readTimeoutMs),
        Math.max(100L, writeTimeoutMs),
        Math.max(100L, callTimeoutMs));
  }

  public int postJson(String url, String body, Map<String, String> headers) throws IOException {
    Request.Builder request =
        new Request.Builder().url(url).post(RequestBody.create(body == null ? "" : body, JSON));
    if (headers != null) {
      headers.forEach(
          (name, value) -> {
            if (name != null && value != null) {
              request.header(name, value);
            }
          });
    }
    try (Response response = client.newCall(request.build()).execute()) {
      return response.code();
    }
  }

  int connectionCount() {
    return connectionPool.connectionCount();
  }

  int idleConnectionCount() {
    return connectionPool.idleConnectionCount();
  }

  @PreDestroy
  public void close() {
    connectionPool.evictAll();
    client.dispatcher().executorService().shutdown();
  }
}
