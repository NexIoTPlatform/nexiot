package cn.universal.http.access.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import cn.universal.http.access.config.HttpConfigParser;
import java.time.ZoneId;

/** Optional product-driver schedule stored at product.configuration.httpAccess.poll. */
record HttpDriverPollingConfig(
    boolean enabled, String cron, long fixedDelayMs, long initialDelayMs, ZoneId zone) {

  static HttpDriverPollingConfig from(String productConfiguration, HttpConfigParser parser) {
    if (productConfiguration == null || productConfiguration.isBlank()) {
      return disabled();
    }
    try {
      JsonNode poll = parser.readTree(productConfiguration).path("httpAccess").path("poll");
      if (!poll.path("enabled").asBoolean(false)) {
        return disabled();
      }
      String cron = text(poll, "cron");
      long fixedDelayMs = poll.path("fixedDelayMs").asLong(60000);
      long initialDelayMs = poll.path("initialDelayMs").asLong(5000);
      if (fixedDelayMs < 1000 || initialDelayMs < 0) {
        throw new IllegalArgumentException("httpAccess.poll delay is invalid");
      }
      String zoneId = text(poll, "zone");
      return new HttpDriverPollingConfig(true, cron, fixedDelayMs, initialDelayMs,
          zoneId == null ? ZoneId.of("Asia/Shanghai") : ZoneId.of(zoneId));
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid product httpAccess.poll configuration", e);
    }
  }

  private static HttpDriverPollingConfig disabled() {
    return new HttpDriverPollingConfig(false, null, 60000, 5000, ZoneId.of("Asia/Shanghai"));
  }

  private static String text(JsonNode node, String key) {
    return node.hasNonNull(key) && !node.get(key).asText().isBlank() ? node.get(key).asText() : null;
  }
}
