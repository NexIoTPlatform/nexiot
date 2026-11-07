package cn.universal.web.websocket;

import cn.universal.common.config.InstanceIdProvider;
import cn.universal.persistence.entity.bo.WebSocketPushSettingsBO;
import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClusterWebSocketSubscriptionRegistry {

  private static final String CLUSTER_COUNTER_KEY = "iot:websocket:device:subscriptions";
  private static final long COUNTER_TTL_SECONDS = 90L;
  private static final long CACHE_MILLIS = 1000L;

  @Resource private RedissonClient redissonClient;

  @Resource private InstanceIdProvider instanceIdProvider;

  private final Map<String, Integer> localCounters = new ConcurrentHashMap<>();

  private volatile Map<String, Integer> cachedClusterCounters = new LinkedHashMap<>();

  private volatile long cacheAt = 0L;

  public synchronized void syncLocalCounts(Map<String, Integer> counters) {
    localCounters.clear();
    localCounters.putAll(counters);
    RMapCache<String, Integer> counterMap = redissonClient.getMapCache(CLUSTER_COUNTER_KEY);
    String nodeId = instanceIdProvider.getInstanceId();
    for (String category : WebSocketPushSettingsBO.categories()) {
      String key = buildKey(nodeId, category);
      Integer count = counters.getOrDefault(category, 0);
      if (count != null && count > 0) {
        counterMap.put(key, count, COUNTER_TTL_SECONDS, TimeUnit.SECONDS);
      } else {
        counterMap.fastRemove(key);
      }
    }
    invalidateCache();
  }

  public boolean hasRemoteSubscribers(String category) {
    int total = getClusterSubscriberCount(category);
    int local = localCounters.getOrDefault(category, 0);
    return Math.max(total - local, 0) > 0;
  }

  public int getClusterSubscriberCount(String category) {
    return getClusterCounters().getOrDefault(category, 0);
  }

  @Scheduled(fixedDelay = 30000)
  public void refreshLocalPresence() {
    syncLocalCounts(new LinkedHashMap<>(localCounters));
  }

  private Map<String, Integer> getClusterCounters() {
    long now = System.currentTimeMillis();
    if (now - cacheAt < CACHE_MILLIS) {
      return cachedClusterCounters;
    }
    synchronized (this) {
      if (now - cacheAt < CACHE_MILLIS) {
        return cachedClusterCounters;
      }
      Map<String, Integer> aggregated = new LinkedHashMap<>();
      WebSocketPushSettingsBO.categories().forEach(category -> aggregated.put(category, 0));

      RMapCache<String, Integer> counterMap = redissonClient.getMapCache(CLUSTER_COUNTER_KEY);
      Map<String, Integer> clusterCounters = counterMap.readAllMap();
      clusterCounters.forEach(
          (key, count) -> {
            if (count == null || count <= 0) {
              return;
            }
            String category = extractCategory(key);
            if (aggregated.containsKey(category)) {
              aggregated.computeIfPresent(category, (ignored, value) -> value + count);
            }
          });
      cachedClusterCounters = aggregated;
      cacheAt = now;
      return cachedClusterCounters;
    }
  }

  private void invalidateCache() {
    cacheAt = 0L;
  }

  private String buildKey(String nodeId, String category) {
    return nodeId + ":" + category;
  }

  private String extractCategory(String key) {
    int index = key.lastIndexOf(':');
    return index < 0 ? key : key.substring(index + 1);
  }
}
