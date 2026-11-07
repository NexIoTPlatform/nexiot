package cn.universal.http.access.client;

import cn.universal.http.access.config.HttpIntegrationConfig.HttpOutboundPolicy;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HttpClientManager {

  private final Executor executor;
  private final Map<Long, HttpClient> clients = new ConcurrentHashMap<>();

  public HttpClientManager(@Qualifier("httpAccessWorkerExecutor") Executor executor) {
    this.executor = executor;
  }

  public HttpClient build(Long networkId, HttpOutboundPolicy policy) {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(policy.getConnectTimeoutMs()))
        .executor(executor)
        .followRedirects(HttpClient.Redirect.NEVER)
        .version(HttpClient.Version.HTTP_2)
        .build();
    clients.put(networkId, client);
    return client;
  }

  public void remove(Long networkId) {
    clients.remove(networkId);
  }
}
