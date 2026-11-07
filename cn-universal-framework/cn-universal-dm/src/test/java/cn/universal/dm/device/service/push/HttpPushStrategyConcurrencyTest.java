package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.bo.UPPushBO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class HttpPushStrategyConcurrencyTest {

  @Test
  void parallelApplicationsShouldNeverCrossUrls() throws Exception {
    List<String> appABodies = new CopyOnWriteArrayList<>();
    List<String> appBBodies = new CopyOnWriteArrayList<>();
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/app-a", exchange -> respond(exchange, appABodies));
    server.createContext("/app-b", exchange -> respond(exchange, appBBodies));
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.start();

    int count = 100;
    CountDownLatch done = new CountDownLatch(count * 2L > Integer.MAX_VALUE ? 0 : count * 2);
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    ApplicationPushHttpClient httpClient =
        new ApplicationPushHttpClient(
            new SimpleMeterRegistry(), 64, 300_000, 500, 800, 800, 1_000);
    try {
      HttpPushStrategy strategy = new HttpPushStrategy(httpClient);
      UPPushBO.HttpPushConfig appA = config(server, "/app-a");
      UPPushBO.HttpPushConfig appB = config(server, "/app-b");
      for (int i = 0; i < count; i++) {
        int sequence = i;
        executor.execute(
            () -> {
              strategy.execute(request("a-" + sequence), "{\"application\":\"a\"}", appA);
              done.countDown();
            });
        executor.execute(
            () -> {
              strategy.execute(request("b-" + sequence), "{\"application\":\"b\"}", appB);
              done.countDown();
            });
      }

      assertTrue(done.await(10, TimeUnit.SECONDS));
      assertEquals(count, appABodies.size());
      assertEquals(count, appBBodies.size());
      assertTrue(appABodies.stream().allMatch(body -> body.contains("\"a\"")));
      assertTrue(appBBodies.stream().allMatch(body -> body.contains("\"b\"")));
    } finally {
      executor.shutdownNow();
      httpClient.close();
      server.stop(0);
    }
  }

  private UPPushBO.HttpPushConfig config(HttpServer server, String path) {
    return UPPushBO.HttpPushConfig.builder()
        .url("http://127.0.0.1:" + server.getAddress().getPort() + path)
        .header("X-Test-Secret")
        .secret("secret")
        .enable(true)
        .support(true)
        .build();
  }

  private BaseUPRequest request(String deviceId) {
    return BaseUPRequest.builder()
        .iotId("pk-" + deviceId)
        .productKey("pk")
        .deviceId(deviceId)
        .ioTDeviceDTO(IoTDeviceDTO.builder().thirdPlatform("tcp").build())
        .build();
  }

  private void respond(HttpExchange exchange, List<String> bodies) throws IOException {
    bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    exchange.sendResponseHeaders(200, 2);
    exchange.getResponseBody().write("ok".getBytes(StandardCharsets.UTF_8));
    exchange.close();
  }
}
