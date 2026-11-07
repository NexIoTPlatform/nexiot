package cn.universal.dm.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

class IoTCacheInvalidationListenerTest {

  @Test
  void shouldPreferKeyEvictionWhenCacheAlsoRequestsFullClear() throws Exception {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("devices");
    Cache cache = cacheManager.getCache("devices");
    assertNotNull(cache);
    cache.put("device-1", "one");
    cache.put("device-2", "two");

    IoTCacheInvalidationListener listener = listener(cacheManager);
    listener.onCacheInvalidation(
        new IoTCacheInvalidationEvent(
            "device-update",
            "pk-1",
            "device-1",
            "iot-1",
            List.of("devices"),
            Map.of("devices", List.of("device-1")),
            Collections.emptySet()));

    assertNull(cache.get("device-1"));
    assertEquals("two", cache.get("device-2", String.class));
  }

  @Test
  void shouldClearOnlyCachesWithoutConcreteKeys() throws Exception {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("aggregate");
    Cache cache = cacheManager.getCache("aggregate");
    assertNotNull(cache);
    cache.put("scope-1", "one");
    cache.put("scope-2", "two");

    listener(cacheManager)
        .onCacheInvalidation(
            new IoTCacheInvalidationEvent(
                "aggregate-change",
                null,
                null,
                null,
                List.of("aggregate"),
                Collections.emptyMap(),
                Collections.emptySet()));

    assertNull(cache.get("scope-1"));
    assertNull(cache.get("scope-2"));
  }

  private IoTCacheInvalidationListener listener(ConcurrentMapCacheManager cacheManager)
      throws Exception {
    IoTCacheInvalidationListener listener = new IoTCacheInvalidationListener();
    setField(listener, "cacheManager", cacheManager);
    setField(listener, "stringRedisTemplate", new StringRedisTemplate());
    return listener;
  }

  private void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
