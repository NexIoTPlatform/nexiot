package cn.universal.dm.device.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTDeviceShadowMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

class ShadowFlushSchedulerTest {

  @Test
  void shouldRemoveDueMemberWhenShadowCacheIsMissing() throws Exception {
    ShadowFlushScheduler scheduler = new ShadowFlushScheduler();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.putDue("shadow:flush", "offline-device");

    configureFlushTask(scheduler, redisTemplate);

    int processed = invokeExecuteFlushTask(scheduler);

    assertEquals(0, processed);
    assertFalse(redisTemplate.containsZsetMember("shadow:flush", "offline-device"));
  }

  @Test
  void shouldRequeueDueMemberWhenShadowCacheReadFails() throws Exception {
    ShadowFlushScheduler scheduler = new ShadowFlushScheduler();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.putDue("shadow:flush", "unavailable-device");
    redisTemplate.failRead("shadow:unavailable-device");

    configureFlushTask(scheduler, redisTemplate);

    int processed = invokeExecuteFlushTask(scheduler);

    assertEquals(0, processed);
    assertTrue(redisTemplate.containsZsetMember("shadow:flush", "unavailable-device"));
  }

  @Test
  void shouldFlushCachedMemberAndRemoveMissingMemberInSameBatch() throws Exception {
    ShadowFlushScheduler scheduler = new ShadowFlushScheduler();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.putDue("shadow:flush", "offline-device");
    redisTemplate.putDue("shadow:flush", "online-device");
    redisTemplate.putValue("shadow:online-device", "{\"version\":2}");
    AtomicInteger updateCount = new AtomicInteger();

    IoTDeviceShadow existing =
        IoTDeviceShadow.builder().iotId("online-device").metadata("{\"version\":1}").build();
    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectByIotIds".equals(method.getName())) {
                return List.of(existing);
              }
              if ("batchUpdate".equals(method.getName())) {
                updateCount.incrementAndGet();
                return 1;
              }
              return defaultValue(method.getReturnType());
            });

    configureFlushTask(scheduler, redisTemplate);
    setField(scheduler, "ioTDeviceShadowMapper", shadowMapper);

    int processed = invokeExecuteFlushTask(scheduler);

    assertEquals(1, processed);
    assertEquals(1, updateCount.get());
    assertFalse(redisTemplate.containsZsetMember("shadow:flush", "offline-device"));
    assertFalse(redisTemplate.containsZsetMember("shadow:flush", "online-device"));
  }

  @Test
  void shouldRequeueInsteadOfInsertWhenDeviceIdentityIsMissing() throws Exception {
    ShadowFlushScheduler scheduler = new ShadowFlushScheduler();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    AtomicInteger insertCount = new AtomicInteger();

    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectOne".equals(method.getName())) {
                return null;
              }
              if ("insertSelective".equals(method.getName())) {
                insertCount.incrementAndGet();
                return 1;
              }
              return defaultValue(method.getReturnType());
            });

    IoTDeviceMapper deviceMapper =
        proxy(
            IoTDeviceMapper.class,
            (method, args) -> {
              if ("getOneByIotId".equals(method.getName())) {
                return IoTDevice.builder().iotId("pkDid").build();
              }
              return defaultValue(method.getReturnType());
            });

    setField(scheduler, "stringRedisTemplate", redisTemplate);
    setField(scheduler, "ioTDeviceShadowMapper", shadowMapper);
    setField(scheduler, "ioTDeviceMapper", deviceMapper);
    setField(scheduler, "shadowFlushZsetKey", "shadow:flush");
    setField(scheduler, "missingDeviceRetryDelayMs", 5000L);

    boolean processed = invokeProcessShadow(scheduler, "pkDid", "{\"version\":1}");

    assertFalse(processed);
    assertEquals(0, insertCount.get());
    assertTrue(redisTemplate.wasRequeued("shadow:flush", "pkDid"));
  }

  @Test
  void shouldDiscardCachedShadowWhenDeviceWasDeleted() throws Exception {
    ShadowFlushScheduler scheduler = new ShadowFlushScheduler();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.putDue("shadow:flush", "deleted-device");
    redisTemplate.putValue("shadow:deleted-device", "{\"version\":1}");

    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectByIotIds".equals(method.getName())) {
                return List.of();
              }
              return defaultValue(method.getReturnType());
            });
    IoTDeviceMapper deviceMapper =
        proxy(
            IoTDeviceMapper.class,
            (method, args) -> {
              if ("getOneByIotId".equals(method.getName())) {
                return null;
              }
              return defaultValue(method.getReturnType());
            });

    configureFlushTask(scheduler, redisTemplate);
    setField(scheduler, "ioTDeviceShadowMapper", shadowMapper);
    setField(scheduler, "ioTDeviceMapper", deviceMapper);

    assertEquals(0, invokeExecuteFlushTask(scheduler));
    assertFalse(redisTemplate.containsZsetMember("shadow:flush", "deleted-device"));
    assertFalse(redisTemplate.containsValue("shadow:deleted-device"));
  }

  @Test
  void shouldBuildNewShadowWithResolvedDeviceIdentity() throws Exception {
    ShadowFlushScheduler scheduler = new ShadowFlushScheduler();

    IoTDeviceMapper deviceMapper =
        proxy(
            IoTDeviceMapper.class,
            (method, args) -> {
              if ("getOneByIotId".equals(method.getName())) {
                return IoTDevice.builder()
                    .iotId("pkDid")
                    .productKey("pk")
                    .deviceId("did")
                    .extDeviceId("ext")
                    .application("app")
                    .onlineTime(1710000000L)
                    .build();
              }
              return defaultValue(method.getReturnType());
            });

    setField(scheduler, "ioTDeviceMapper", deviceMapper);

    IoTDeviceShadow shadow =
        invokeBuildNewShadow(scheduler, "pkDid", "{\"version\":1}", new Date(0L));

    assertNotNull(shadow);
    assertEquals("pkDid", shadow.getIotId());
    assertEquals("pk", shadow.getProductKey());
    assertEquals("did", shadow.getDeviceId());
    assertEquals("ext", shadow.getExtDeviceId());
    assertEquals("app", shadow.getInstance());
    assertEquals(1L, shadow.getVersion());
    assertNotNull(shadow.getOnlineTime());
  }

  @Test
  void shouldSetVersionWhenUpdatingShadowWithBlankExistingMetadata() throws Exception {
    ShadowFlushScheduler scheduler = new ShadowFlushScheduler();
    IoTDeviceShadow existed = IoTDeviceShadow.builder().iotId("pkDid").metadata("").build();

    IoTDeviceShadow updated =
        invokeBuildUpdatedShadow(scheduler, existed, "{\"version\":7}", new Date(0L));

    assertEquals(7L, updated.getVersion());
    assertEquals("{\"version\":7}", updated.getMetadata());
  }

  private boolean invokeProcessShadow(ShadowFlushScheduler scheduler, String iotId, String cacheJson)
      throws Exception {
    Method method =
        ShadowFlushScheduler.class.getDeclaredMethod("processShadow", String.class, String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(scheduler, iotId, cacheJson);
  }

  private IoTDeviceShadow invokeBuildNewShadow(
      ShadowFlushScheduler scheduler, String iotId, String cacheJson, Date now) throws Exception {
    Method method =
        ShadowFlushScheduler.class.getDeclaredMethod(
            "buildNewShadow", String.class, String.class, Date.class);
    method.setAccessible(true);
    return (IoTDeviceShadow) method.invoke(scheduler, iotId, cacheJson, now);
  }

  private IoTDeviceShadow invokeBuildUpdatedShadow(
      ShadowFlushScheduler scheduler, IoTDeviceShadow existed, String cacheJson, Date now)
      throws Exception {
    Method method =
        ShadowFlushScheduler.class.getDeclaredMethod(
            "buildUpdatedShadow", IoTDeviceShadow.class, String.class, Date.class);
    method.setAccessible(true);
    return (IoTDeviceShadow) method.invoke(scheduler, existed, cacheJson, now);
  }

  private int invokeExecuteFlushTask(ShadowFlushScheduler scheduler) throws Exception {
    Method method = ShadowFlushScheduler.class.getDeclaredMethod("executeFlushTask");
    method.setAccessible(true);
    return (int) method.invoke(scheduler);
  }

  private void configureFlushTask(
      ShadowFlushScheduler scheduler, RecordingStringRedisTemplate redisTemplate) throws Exception {
    setField(scheduler, "stringRedisTemplate", redisTemplate);
    setField(scheduler, "instanceIdProvider", new cn.universal.common.config.InstanceIdProvider());
    setField(scheduler, "shadowKeyPrefix", "shadow");
    setField(scheduler, "shadowFlushZsetKey", "shadow:flush");
    setField(scheduler, "batchSize", 1000);
    setField(scheduler, "missingDeviceRetryDelayMs", 5000L);
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @SuppressWarnings("unchecked")
  private <T> T proxy(Class<T> type, MethodHandler handler) {
    return (T)
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, args) -> {
              if ("toString".equals(method.getName())) {
                return type.getSimpleName() + "Proxy";
              }
              if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
              }
              if ("equals".equals(method.getName())) {
                return proxy == args[0];
              }
              return handler.invoke(method, args);
            });
  }

  private Object defaultValue(Class<?> returnType) {
    if (returnType == null || !returnType.isPrimitive()) {
      return null;
    }
    if (boolean.class.equals(returnType)) {
      return false;
    }
    if (char.class.equals(returnType)) {
      return '\0';
    }
    return 0;
  }

  @FunctionalInterface
  private interface MethodHandler {
    Object invoke(Method method, Object[] args) throws Throwable;
  }

  private static class RecordingStringRedisTemplate extends StringRedisTemplate {

    private final Map<String, String> values = new HashMap<>();
    private final Map<String, Double> requeued = new HashMap<>();
    private final Set<String> failedReads = new LinkedHashSet<>();

    void putValue(String key, String value) {
      values.put(key, value);
    }

    void putDue(String key, String value) {
      requeued.put(key + "|" + value, 0D);
    }

    void failRead(String key) {
      failedReads.add(key);
    }

    boolean containsZsetMember(String key, String value) {
      return requeued.containsKey(key + "|" + value);
    }

    boolean containsValue(String key) {
      return values.containsKey(key);
    }

    @Override
    public Boolean delete(String key) {
      return values.remove(key) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValueOperations<String, String> opsForValue() {
      return (ValueOperations<String, String>)
          Proxy.newProxyInstance(
              ValueOperations.class.getClassLoader(),
              new Class<?>[] {ValueOperations.class},
              (proxy, method, args) -> {
                if ("get".equals(method.getName())) {
                  if (failedReads.contains(args[0])) {
                    throw new IllegalStateException("redis unavailable");
                  }
                  return values.get(args[0]);
                }
                if ("toString".equals(method.getName())) {
                  return "RecordingValueOperations";
                }
                return null;
              });
    }

    @Override
    @SuppressWarnings("unchecked")
    public ZSetOperations<String, String> opsForZSet() {
      return (ZSetOperations<String, String>)
          Proxy.newProxyInstance(
              ZSetOperations.class.getClassLoader(),
              new Class<?>[] {ZSetOperations.class},
              (proxy, method, args) -> {
                if ("add".equals(method.getName())) {
                  requeued.put(args[0] + "|" + args[1], (Double) args[2]);
                  return true;
                }
                if ("count".equals(method.getName())) {
                  String keyPrefix = args[0] + "|";
                  return requeued.keySet().stream().filter(key -> key.startsWith(keyPrefix)).count();
                }
                if ("rangeByScore".equals(method.getName())) {
                  String keyPrefix = args[0] + "|";
                  long limit = (Long) args[4];
                  Set<String> result = new LinkedHashSet<>();
                  requeued.keySet().stream()
                      .filter(key -> key.startsWith(keyPrefix))
                      .limit(limit)
                      .map(key -> key.substring(keyPrefix.length()))
                      .forEach(result::add);
                  return result;
                }
                if ("remove".equals(method.getName())) {
                  String key = (String) args[0];
                  Object[] members = (Object[]) args[1];
                  long removed = 0;
                  for (Object member : members) {
                    if (requeued.remove(key + "|" + member) != null) {
                      removed++;
                    }
                  }
                  return removed;
                }
                if ("toString".equals(method.getName())) {
                  return "RecordingZSetOperations";
                }
                return null;
              });
    }

    boolean wasRequeued(String key, String value) {
      return requeued.containsKey(key + "|" + value);
    }
  }
}
