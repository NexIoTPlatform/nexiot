package cn.universal.http.access.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class HttpRateLimiter {

  private final StringRedisTemplate redisTemplate;
  private final ConcurrentHashMap<String, LocalWindow> localWindows = new ConcurrentHashMap<>();
  private final AtomicLong localRequests = new AtomicLong();

  public HttpRateLimiter(ObjectProvider<StringRedisTemplate> redisTemplate) {
    this.redisTemplate = redisTemplate.getIfAvailable();
  }

  public boolean allow(Long networkId, String remoteAddress, int limit) {
    if (limit <= 0) {
      return true;
    }
    long minute = Instant.now().getEpochSecond() / 60;
    String key = "http:rate:" + networkId + ':' + safe(remoteAddress) + ':' + minute;
    if (redisTemplate != null) {
      try {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
          redisTemplate.expire(key, Duration.ofSeconds(70));
        }
        return count != null && count <= limit;
      } catch (Exception ignored) {
        // Local fallback protects the node when Redis is temporarily unavailable.
      }
    }
    LocalWindow window = localWindows.compute(key, (k, current) -> {
      if (current == null || current.minute != minute) {
        return new LocalWindow(minute);
      }
      return current;
    });
    if ((localRequests.incrementAndGet() & 1023) == 0) {
      localWindows.entrySet().removeIf(entry -> entry.getValue().minute < minute - 1);
    }
    return window.count.incrementAndGet() <= limit;
  }

  private String safe(String value) {
    return value == null ? "unknown" : Integer.toHexString(value.hashCode());
  }

  private static final class LocalWindow {
    private final long minute;
    private final AtomicInteger count = new AtomicInteger();

    private LocalWindow(long minute) {
      this.minute = minute;
    }
  }
}
