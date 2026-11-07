package cn.universal.http.access.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "nexiot.http-access")
public class HttpAccessProperties {

  private boolean enabled = true;
  private int schedulerThreads = 2;
  private int workerThreads = Math.max(4, Runtime.getRuntime().availableProcessors());
  private int workerQueueCapacity = 1000;
  private int hardMaxInboundBytes = 4 * 1024 * 1024;
  private int hardMaxOutboundRequestBytes = 4 * 1024 * 1024;
  private int hardMaxOutboundResponseBytes = 16 * 1024 * 1024;
  private long reconciliationIntervalMs = 30000;
}
