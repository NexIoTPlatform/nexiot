package cn.universal.dm.device.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.message.UPRequest;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTDeviceShadowMapper;
import cn.universal.persistence.shadow.Shadow;
import cn.universal.persistence.shadow.State;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

class IoTDeviceShadowServiceTest {

  @Test
  void shouldReturnLatestShadowFromCacheAndFillDeviceIdentity() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.put(
        "shadow:pkDid",
        "{\"state\":{\"desired\":{},\"reported\":{\"temperature\":12.3}},\"metadata\":{\"desired\":{},\"reported\":{\"temperature\":{\"timestamp\":200}}},\"timestamp\":200,\"version\":2}");

    AtomicReference<String> unexpectedShadowDbCall = new AtomicReference<>();
    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectOne".equals(method.getName()) || "selectByIotIds".equals(method.getName())) {
                unexpectedShadowDbCall.set(method.getName());
              }
              return defaultValue(method.getReturnType());
            });
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
                    .build();
              }
              return defaultValue(method.getReturnType());
            });

    setField(service, "shadowCacheEnabled", true);
    setField(service, "shadowKeyPrefix", "shadow");
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "ioTDeviceShadowMapper", shadowMapper);
    setField(service, "ioTDeviceMapper", deviceMapper);

    IoTDeviceShadow shadow = service.getLatestDeviceShadow("pkDid");

    assertEquals("pkDid", shadow.getIotId());
    assertEquals("pk", shadow.getProductKey());
    assertEquals("did", shadow.getDeviceId());
    assertEquals("ext", shadow.getExtDeviceId());
    assertEquals("app", shadow.getInstance());
    assertEquals(2L, shadow.getVersion());
    assertEquals(12.3, new JSONObject(shadow.getMetadata()).getJSONObject("state").getJSONObject("reported").getDouble("temperature"));
    assertNull(unexpectedShadowDbCall.get());
  }

  @Test
  void shouldOnlyQueryDatabaseForBatchCacheMisses() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.put(
        "shadow:cached-1",
        "{\"state\":{\"desired\":{},\"reported\":{\"temperature\":12.3}},\"metadata\":{\"desired\":{},\"reported\":{\"temperature\":{\"timestamp\":200}}},\"timestamp\":200,\"version\":2}");

    AtomicReference<List<String>> queriedIds = new AtomicReference<>();
    AtomicInteger selectByIotIdsCalls = new AtomicInteger();
    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectByIotIds".equals(method.getName())) {
                selectByIotIdsCalls.incrementAndGet();
                @SuppressWarnings("unchecked")
                List<String> ids = (List<String>) args[0];
                queriedIds.set(ids);
                return List.of(
                    IoTDeviceShadow.builder()
                        .iotId("miss-1")
                        .metadata("{\"state\":{\"reported\":{\"humidity\":66}}}")
                        .version(1L)
                        .build());
              }
              return defaultValue(method.getReturnType());
            });

    setField(service, "shadowCacheEnabled", true);
    setField(service, "shadowKeyPrefix", "shadow");
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "ioTDeviceShadowMapper", shadowMapper);

    List<IoTDeviceShadow> shadows = service.getLatestDeviceShadows(List.of("cached-1", "miss-1"));

    assertEquals(1, selectByIotIdsCalls.get());
    assertEquals(List.of("miss-1"), queriedIds.get());
    assertEquals(2, shadows.size());
    assertEquals("cached-1", shadows.get(0).getIotId());
    assertEquals(2L, shadows.get(0).getVersion());
    assertEquals("miss-1", shadows.get(1).getIotId());
  }

  @Test
  void shouldScheduleFirstFlushWithoutDbVersionCheck() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.put(
        "shadow:pkDid",
        "{\"state\":{\"desired\":{},\"reported\":{\"temperature\":12.3}},\"metadata\":{\"desired\":{},\"reported\":{\"temperature\":{\"timestamp\":200}}},\"timestamp\":200,\"version\":1}");

    AtomicReference<String> unexpectedDbCall = new AtomicReference<>();
    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectOne".equals(method.getName())) {
                unexpectedDbCall.set(method.getName());
              }
              return defaultValue(method.getReturnType());
            });

    setField(service, "shadowKeyPrefix", "shadow");
    setField(service, "shadowFlushZsetKey", "shadow:flush");
    setField(service, "flushBaseIntervalMs", 1_000L);
    setField(service, "flushJitterRate", 0D);
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "ioTDeviceShadowMapper", shadowMapper);

    long before = System.currentTimeMillis();
    Long nextAt = invokeCalcNextFlushAtMs(service, "pkDid");
    long delay = nextAt - before;

    assertTrue(delay >= 900L);
    assertTrue(delay < 2_000L);
    assertNull(unexpectedDbCall.get());
  }

  @Test
  void shouldNotPostponeExistingForcedFlushForHighFrequencyReports() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.put(
        "shadow:pkDid",
        "{\"state\":{\"desired\":{},\"reported\":{\"temperature\":12.3}},\"metadata\":{\"desired\":{},\"reported\":{\"temperature\":{\"timestamp\":200}}},\"timestamp\":200,\"version\":2}");

    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectOne".equals(method.getName())) {
                return null;
              }
              return defaultValue(method.getReturnType());
            });

    setField(service, "shadowKeyPrefix", "shadow");
    setField(service, "shadowFlushZsetKey", "shadow:flush");
    setField(service, "flushBaseIntervalMs", 86_400_000L);
    setField(service, "flushJitterRate", 0D);
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "ioTDeviceShadowMapper", shadowMapper);

    long firstFlushAt = invokeCalcNextFlushAtMs(service, "pkDid");
    redisTemplate.putZsetScore("shadow:flush", "pkDid", firstFlushAt);
    Thread.sleep(5);

    long secondFlushAt = invokeCalcNextFlushAtMs(service, "pkDid");

    assertEquals(firstFlushAt, secondFlushAt);
  }

  @Test
  void shouldKeepOverdueFlushDueWhenHighFrequencyReportsContinue() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();

    setField(service, "shadowFlushZsetKey", "shadow:flush");
    setField(service, "flushBaseIntervalMs", 86_400_000L);
    setField(service, "flushJitterRate", 0.2D);
    setField(service, "stringRedisTemplate", redisTemplate);

    long overdueFlushAt = System.currentTimeMillis() - 1000L;
    redisTemplate.putZsetScore("shadow:flush", "pkDid", overdueFlushAt);

    long nextFlushAt = invokeCalcNextFlushAtMs(service, "pkDid");

    assertEquals(overdueFlushAt, nextFlushAt);
  }

  @Test
  void shouldReadProductDeviceShadowFromResolvedIotIdCache() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.put(
        "shadow:custom-iot-id",
        "{\"state\":{\"desired\":{},\"reported\":{\"pressure\":88}},\"metadata\":{\"desired\":{},\"reported\":{\"pressure\":{\"timestamp\":300}}},\"timestamp\":300,\"version\":3}");

    IoTDeviceMapper deviceMapper =
        proxy(
            IoTDeviceMapper.class,
            (method, args) -> {
              if ("getOneByDeviceId".equals(method.getName())) {
                return IoTDevice.builder()
                    .iotId("custom-iot-id")
                    .productKey("pk")
                    .deviceId("did")
                    .build();
              }
              return defaultValue(method.getReturnType());
            });
    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("getShadowMetadata".equals(method.getName())) {
                return null;
              }
              return defaultValue(method.getReturnType());
            });

    setField(service, "shadowKeyPrefix", "shadow");
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "ioTDeviceMapper", deviceMapper);
    setField(service, "ioTDeviceShadowMapper", shadowMapper);

    JSONObject shadow = service.getDeviceShadowObj("pk", "did");

    assertEquals(88, shadow.getJSONObject("state").getJSONObject("reported").getInt("pressure"));
  }

  @Test
  void shouldFallbackToReportedPropertiesWhenMetadataIsEmpty() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.put(
        "shadow:iot-1",
        "{\"state\":{\"desired\":{},\"reported\":{\"temperature\":12.3,\"pressure\":3.4}},\"metadata\":{\"desired\":{},\"reported\":{\"temperature\":{\"timestamp\":200},\"pressure\":{\"timestamp\":201}}},\"timestamp\":201,\"version\":4}");

    IoTDeviceMapper deviceMapper =
        proxy(
            IoTDeviceMapper.class,
            (method, args) -> {
              if ("selectIoTDeviceBO".equals(method.getName())) {
                return IoTDeviceDTO.builder()
                    .iotId("iot-1")
                    .productKey("pk")
                    .deviceId("did")
                    .metadata("{\"properties\":[]}")
                    .build();
              }
              return defaultValue(method.getReturnType());
            });
    IoTProductDeviceService productDeviceService =
        new IoTProductDeviceService() {
          @Override
          public LogStorePolicyDTO getProductLogStorePolicy(String productKey) {
            return LogStorePolicyDTO.builder()
                .properties(Map.of("temperature", new LogStorePolicyDTO.StorePolicy("temperature", 10)))
                .build();
          }
        };

    setField(service, "shadowCacheEnabled", true);
    setField(service, "shadowKeyPrefix", "shadow");
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "ioTDeviceMapper", deviceMapper);
    setField(service, "iotProductDeviceService", productDeviceService);

    List<cn.universal.dm.device.entity.IoTDevicePropertiesBO> properties =
        service.getDevState("iot-1");
    Map<String, cn.universal.dm.device.entity.IoTDevicePropertiesBO> byProperty =
        new HashMap<>();
    properties.forEach(property -> byProperty.put(property.getProperty(), property));

    assertEquals(2, properties.size());
    assertEquals("temperature", byProperty.get("temperature").getPropertyName());
    assertEquals("did", byProperty.get("temperature").getDeviceId());
    assertEquals(200L, byProperty.get("temperature").getTimestamp());
    assertTrue(byProperty.get("temperature").isStoragePolicy());
    assertEquals("pressure", byProperty.get("pressure").getPropertyName());
    assertEquals(201L, byProperty.get("pressure").getTimestamp());
    assertFalse(byProperty.get("pressure").isStoragePolicy());
  }

  @Test
  void shouldCreateInitialShadowRowWhenCacheModeReceivesFirstReport() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    AtomicReference<IoTDeviceShadow> inserted = new AtomicReference<>();
    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectOne".equals(method.getName())) {
                return null;
              }
              if ("insertSelective".equals(method.getName())) {
                inserted.set((IoTDeviceShadow) args[0]);
                return 1;
              }
              return defaultValue(method.getReturnType());
            });

    setField(service, "shadowCacheEnabled", true);
    setField(service, "shadowKeyPrefix", "shadow");
    setField(service, "shadowCacheTtlSeconds", 604800L);
    setField(service, "shadowFlushZsetKey", "shadow:flush");
    setField(service, "flushBaseIntervalMs", 7_200_000L);
    setField(service, "flushJitterRate", 0.2D);
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "ioTDeviceShadowMapper", shadowMapper);

    IoTDeviceDTO device =
        IoTDeviceDTO.builder()
            .iotId("iot-1")
            .productKey("pk")
            .deviceId("did")
            .extDeviceId("ext")
            .build();
    UPRequest request =
        UPRequest.builder().iotId("iot-1").properties(Map.of("temperature", 12.3)).time(300L).build();

    service.doShadow(request, device);

    IoTDeviceShadow created = inserted.get();
    assertEquals("iot-1", created.getIotId());
    assertEquals("pk", created.getProductKey());
    assertEquals("did", created.getDeviceId());
    assertEquals("ext", created.getExtDeviceId());
    assertEquals(2L, created.getVersion());
    JSONObject metadata = new JSONObject(created.getMetadata());
    assertEquals(12.3, metadata.getJSONObject("state").getJSONObject("reported").getDouble("temperature"));
    assertTrue(redisTemplate.wasRequeued("shadow:flush", "iot-1"));
  }

  @Test
  void shouldCacheAndPersistFirstDesiredAudioProperties() throws Exception {
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    AtomicReference<IoTDeviceShadow> inserted = new AtomicReference<>();
    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("insertSelective".equals(method.getName())) {
                inserted.set((IoTDeviceShadow) args[0]);
                return 1;
              }
              return defaultValue(method.getReturnType());
            });
    IoTDeviceShadowService service = desiredCacheService(redisTemplate, shadowMapper);
    IoTDeviceDTO device =
        IoTDeviceDTO.builder()
            .iotId("iot-1")
            .productKey("pk")
            .deviceId("did")
            .extDeviceId("sip-1001")
            .build();

    service.doDesiredShadow(
        device, Map.of("audioSource", "url", "audioUrl", "https://oss.example/alarm.wav"), 300L);

    JSONObject cached = new JSONObject(redisTemplate.opsForValue().get("shadow:iot-1"));
    JSONObject desired = cached.getJSONObject("state").getJSONObject("desired");
    assertEquals("url", desired.getStr("audioSource"));
    assertEquals("https://oss.example/alarm.wav", desired.getStr("audioUrl"));
    assertTrue(cached.getJSONObject("state").getJSONObject("reported").isEmpty());
    assertEquals(
        300L,
        cached.getJSONObject("metadata").getJSONObject("desired").getJSONObject("audioUrl").getLong("timestamp"));
    IoTDeviceShadow created = inserted.get();
    assertNotNull(created);
    assertEquals("iot-1", created.getIotId());
    assertEquals("sip-1001", created.getExtDeviceId());
    assertEquals(desired, new JSONObject(created.getMetadata()).getJSONObject("state").getJSONObject("desired"));
    assertEquals(cached.getLong("version"), created.getVersion());
    assertTrue(redisTemplate.wasRequeued("shadow:flush", "iot-1"));
  }

  @Test
  void shouldPatchExistingDesiredAudioWithoutReplacingUntouchedProperties() throws Exception {
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.put(
        "shadow:iot-1",
        "{\"state\":{\"desired\":{\"audioUrl\":\"https://oss.example/old.wav\",\"targetDeviceId\":\"1002\"},\"reported\":{\"registered\":true}},\"metadata\":{\"desired\":{\"audioUrl\":{\"timestamp\":100},\"targetDeviceId\":{\"timestamp\":100}},\"reported\":{\"registered\":{\"timestamp\":200}}},\"timestamp\":200,\"version\":4}");
    redisTemplate.put("shadow:persisted:iot-1", "1");
    AtomicReference<String> unexpectedDbCall = new AtomicReference<>();
    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectOne".equals(method.getName()) || "insertSelective".equals(method.getName())) {
                unexpectedDbCall.set(method.getName());
              }
              return defaultValue(method.getReturnType());
            });
    IoTDeviceShadowService service = desiredCacheService(redisTemplate, shadowMapper);

    service.doDesiredShadow(
        IoTDeviceDTO.builder().iotId("iot-1").productKey("pk").deviceId("did").build(),
        Map.of("audioUrl", "https://oss.example/new.wav"),
        1_800_000_000_000L);

    JSONObject cached = new JSONObject(redisTemplate.opsForValue().get("shadow:iot-1"));
    JSONObject desired = cached.getJSONObject("state").getJSONObject("desired");
    assertEquals("https://oss.example/new.wav", desired.getStr("audioUrl"));
    assertEquals("1002", desired.getStr("targetDeviceId"));
    assertEquals(true, cached.getJSONObject("state").getJSONObject("reported").getBool("registered"));
    JSONObject metadata = cached.getJSONObject("metadata").getJSONObject("desired");
    assertEquals(100L, metadata.getJSONObject("targetDeviceId").getLong("timestamp"));
    assertEquals(1_800_000_000L, metadata.getJSONObject("audioUrl").getLong("timestamp"));
    assertEquals(5L, cached.getLong("version"));
    assertTrue(redisTemplate.wasRequeued("shadow:flush", "iot-1"));
    assertNull(unexpectedDbCall.get());
  }

  private IoTDeviceShadowService desiredCacheService(
      RecordingStringRedisTemplate redisTemplate, IoTDeviceShadowMapper shadowMapper)
      throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    setField(service, "shadowCacheEnabled", true);
    setField(service, "shadowKeyPrefix", "shadow");
    setField(service, "shadowCacheTtlSeconds", 604800L);
    setField(service, "shadowFlushZsetKey", "shadow:flush");
    setField(service, "shadowPersistedMarkerKeyPrefix", "shadow:persisted");
    setField(service, "shadowPersistedMarkerTtlSeconds", 604800L);
    setField(service, "flushBaseIntervalMs", 7_200_000L);
    setField(service, "flushJitterRate", 0.2D);
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "ioTDeviceShadowMapper", shadowMapper);
    return service;
  }

  @Test
  void shouldSkipInitialShadowSelectWhenPersistedMarkerExists() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    redisTemplate.put("shadow:persisted:iot-1", "1");
    AtomicReference<String> unexpectedDbCall = new AtomicReference<>();
    IoTDeviceShadowMapper shadowMapper =
        proxy(
            IoTDeviceShadowMapper.class,
            (method, args) -> {
              if ("selectOne".equals(method.getName()) || "insertSelective".equals(method.getName())) {
                unexpectedDbCall.set(method.getName());
              }
              return defaultValue(method.getReturnType());
            });

    setField(service, "shadowCacheEnabled", true);
    setField(service, "shadowKeyPrefix", "shadow");
    setField(service, "shadowCacheTtlSeconds", 604800L);
    setField(service, "shadowFlushZsetKey", "shadow:flush");
    setField(service, "shadowPersistedMarkerKeyPrefix", "shadow:persisted");
    setField(service, "shadowPersistedMarkerTtlSeconds", 604800L);
    setField(service, "flushBaseIntervalMs", 7_200_000L);
    setField(service, "flushJitterRate", 0.2D);
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "ioTDeviceShadowMapper", shadowMapper);

    IoTDeviceDTO device =
        IoTDeviceDTO.builder().iotId("iot-1").productKey("pk").deviceId("did").build();
    UPRequest request =
        UPRequest.builder().iotId("iot-1").properties(Map.of("temperature", 12.3)).time(300L).build();
    Shadow shadow =
        Shadow.builder()
            .state(State.builder().desired(new JSONObject()).reported(new JSONObject()).build())
            .metadata(State.builder().desired(new JSONObject()).reported(new JSONObject()).build())
            .timestamp(300L)
            .version(2L)
            .build();

    invokeEnsureInitialShadowPersisted(service, request, device, shadow);

    assertNull(unexpectedDbCall.get());
  }

  @Test
  void shouldRemoveDesiredWhenPropertyReportIsNewerThanDesired() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    Shadow shadow = buildShadow("openState", "0", 100L);
    shadow.setVersion(1L);

    UPRequest request = UPRequest.builder().properties(Map.of("openState", "1")).time(200L).build();

    invokeDoDesired(service, shadow, request);

    assertEquals("1", shadow.getState().getReported().getStr("openState"));
    assertFalse(shadow.getState().getDesired().containsKey("openState"));
    assertFalse(shadow.getMetadata().getDesired().containsKey("openState"));
    assertEquals(200L, shadow.getMetadata().getReported().getJSONObject("openState").getLong("timestamp"));
    assertEquals(200L, shadow.getTimestamp());
    assertEquals(2L, shadow.getVersion());
  }

  @Test
  void shouldKeepDesiredWhenPropertyReportIsOlderThanDesired() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    Shadow shadow = buildShadow("openState", "0", 300L);

    UPRequest request = UPRequest.builder().properties(Map.of("openState", "1")).time(200L).build();

    invokeDoDesired(service, shadow, request);

    assertEquals("1", shadow.getState().getReported().getStr("openState"));
    assertTrue(shadow.getState().getDesired().containsKey("openState"));
    assertTrue(shadow.getMetadata().getDesired().containsKey("openState"));
    assertEquals(200L, shadow.getMetadata().getReported().getJSONObject("openState").getLong("timestamp"));
  }

  @Test
  void shouldNotRemoveDesiredWhenReplyDoesNotContainReportedValue() throws Exception {
    IoTDeviceShadowService service = new IoTDeviceShadowService();
    Shadow shadow = buildShadow("openState", "0", 100L);

    UPRequest request =
        UPRequest.builder()
            .messageType(MessageType.REPLY)
            .data(Map.of("openState", ""))
            .time(200L)
            .build();

    invokeDoDesired(service, shadow, request);

    assertTrue(shadow.getState().getDesired().containsKey("openState"));
    assertTrue(shadow.getMetadata().getDesired().containsKey("openState"));
    assertFalse(shadow.getState().getReported().containsKey("openState"));
  }

  private void invokeDoDesired(IoTDeviceShadowService service, Shadow shadow, UPRequest request)
      throws Exception {
    Method method =
        IoTDeviceShadowService.class.getDeclaredMethod(
            "doDesired", Shadow.class, cn.universal.persistence.dto.IoTDeviceDTO.class, UPRequest.class);
    method.setAccessible(true);
    method.invoke(service, shadow, null, request);
  }

  private Long invokeCalcNextFlushAtMs(IoTDeviceShadowService service, String iotId)
      throws Exception {
    Method method = IoTDeviceShadowService.class.getDeclaredMethod("calcNextFlushAtMs", String.class);
    method.setAccessible(true);
    return (Long) method.invoke(service, iotId);
  }

  private void invokeEnsureInitialShadowPersisted(
      IoTDeviceShadowService service, UPRequest request, IoTDeviceDTO device, Shadow shadow)
      throws Exception {
    Method method =
        IoTDeviceShadowService.class.getDeclaredMethod(
            "ensureInitialShadowPersisted", UPRequest.class, IoTDeviceDTO.class, Shadow.class);
    method.setAccessible(true);
    method.invoke(service, request, device, shadow);
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
    private final Map<String, Double> zsetScores = new HashMap<>();

    void put(String key, String value) {
      values.put(key, value);
    }

    boolean wasRequeued(String key, String member) {
      return zsetScores.containsKey(key + "|" + member);
    }

    void putZsetScore(String key, String member, double score) {
      zsetScores.put(key + "|" + member, score);
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
                  return values.get(args[0]);
                }
                if ("set".equals(method.getName())) {
                  values.put((String) args[0], (String) args[1]);
                  return null;
                }
                if ("setIfAbsent".equals(method.getName())) {
                  String key = (String) args[0];
                  if (values.containsKey(key)) {
                    return false;
                  }
                  values.put(key, (String) args[1]);
                  return true;
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
                if ("score".equals(method.getName())) {
                  return zsetScores.get(args[0] + "|" + args[1]);
                }
                if ("add".equals(method.getName())) {
                  zsetScores.put(args[0] + "|" + args[1], (Double) args[2]);
                  return true;
                }
                if ("toString".equals(method.getName())) {
                  return "RecordingZSetOperations";
                }
                return null;
              });
    }
  }

  private Shadow buildShadow(String property, Object desiredValue, long desiredTimestamp) {
    JSONObject stateDesired = new JSONObject();
    stateDesired.set(property, desiredValue);

    JSONObject desiredMeta = new JSONObject();
    desiredMeta.set(property, new JSONObject().set("timestamp", desiredTimestamp));

    State state = State.builder().desired(stateDesired).reported(new JSONObject()).build();
    State metadata = State.builder().desired(desiredMeta).reported(new JSONObject()).build();
    return Shadow.builder().state(state).metadata(metadata).timestamp(0L).version(1L).build();
  }
}
