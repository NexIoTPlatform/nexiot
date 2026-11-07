package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTProduct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class CkDeviceLogServiceTest {

  @Test
  void shouldFlushDeviceLogsWithBatchUpdate() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate();
    setField(service, "jdbcTemplate", jdbcTemplate);
    setField(service, "devLogTableName", "iot_device_log");
    setField(service, "batchEnabled", true);
    setField(service, "batchSize", 1000);
    setField(service, "batchFlushIntervalMs", 10L);
    setField(service, "batchQueueCapacity", 1000);
    setField(service, "deviceLogQueue", new ArrayBlockingQueue<>(1000));

    IoTProduct product =
        IoTProduct.builder().productKey("pk001").storePolicy("clickhouse").metadata("{}").build();
    IoTDeviceDTO device =
        IoTDeviceDTO.builder()
            .iotId("pk001dev001")
            .productKey("pk001")
            .deviceId("dev001")
            .deviceName("dev001")
            .build();

    service.saveDeviceLog(upRequest(device, 1), device, product);
    service.saveDeviceLog(upRequest(device, 2), device, product);
    invoke(service, "flushPendingDeviceLogs", new Class<?>[] {int.class}, 1000);

    assertEquals(1, jdbcTemplate.batchUpdateCalls);
    assertNotNull(jdbcTemplate.lastSql);
    assertEquals(2, jdbcTemplate.lastBatchArgs.size());
  }

  @Test
  void shouldQueueMetadataLogsBeforeBatchUpdate() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate();
    setField(service, "jdbcTemplate", jdbcTemplate);
    setField(service, "devLogMetaTableName", "iot_device_log_metadata");
    setField(service, "batchEnabled", true);
    setField(service, "batchSize", 1000);
    setField(service, "batchFlushIntervalMs", 10L);
    setField(service, "batchQueueCapacity", 1000);
    setField(service, "deviceLogMetadataQueue", new ArrayBlockingQueue<>(1000));

    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk001")
            .storePolicy("clickhouse")
            .metadata(
                "{\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"double\"}}]}")
            .build();
    IoTDeviceDTO device =
        IoTDeviceDTO.builder()
            .iotId("pk001dev001")
            .productKey("pk001")
            .deviceId("dev001")
            .deviceName("dev001")
            .build();
    LogStorePolicyDTO policy =
        LogStorePolicyDTO.builder()
            .properties(
                new HashMap<>(
                    Map.of(
                        "temperature",
                        new LogStorePolicyDTO.StorePolicy("temperature", 1000))))
            .build();

    invoke(
        service,
        "saveLogStorePolicy",
        new Class<?>[] {
          LogStorePolicyDTO.class, cn.universal.core.message.UPRequest.class, IoTProduct.class
        },
        policy,
        upRequest(device, 1),
        product);

    assertEquals(0, jdbcTemplate.batchUpdateCalls);

    invoke(service, "flushPendingDeviceLogMetadata", new Class<?>[] {int.class}, 1000);

    assertEquals(1, jdbcTemplate.batchUpdateCalls);
    assertNotNull(jdbcTemplate.lastSql);
    assertEquals(1, jdbcTemplate.lastBatchArgs.size());
  }

  @Test
  void shouldResolveLogStorePolicyFromLoadedProductConfiguration() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk001")
            .storePolicyConfiguration(
                "{\"properties\":[{\"id\":\"temperature\",\"maxStorage\":123}],\"event\":[{\"id\":\"alarm\",\"maxStorage\":7}]}")
            .build();

    LogStorePolicyDTO policy =
        (LogStorePolicyDTO)
            invoke(
                service,
                "resolveLogStorePolicy",
                new Class<?>[] {IoTProduct.class},
                product);

    assertEquals(123, policy.getProperties().get("temperature").getMaxStorage());
    assertEquals(7, policy.getEvent().get("alarm").getMaxStorage());
  }

  @Test
  void shouldClearLocalPolicyCacheWhenProductStorePolicyInvalidated() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk001")
            .storePolicyConfiguration(
                "{\"properties\":[{\"id\":\"temperature\",\"maxStorage\":123}],\"event\":[]}")
            .build();

    invoke(
        service,
        "resolveLogStorePolicy",
        new Class<?>[] {IoTProduct.class},
        product);

    Map<?, ?> cache = getSuperclassMapField(service, "localLogStorePolicyCache");
    assertEquals(1, cache.size());

    service.onLogCacheInvalidation(
        new IoTCacheInvalidationEvent(
            "product-store-policy-update",
            "pk001",
            null,
            List.of("iot_product_log_store_policy"),
            Collections.emptyMap(),
            Collections.emptySet()));

    assertEquals(0, cache.size());
  }

  @Test
  void shouldKeepLocalPolicyCacheWhenDeviceGoesOffline() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    IoTProduct product =
        IoTProduct.builder()
            .productKey("pk001")
            .storePolicyConfiguration(
                "{\"properties\":[{\"id\":\"temperature\",\"maxStorage\":123}],\"event\":[]}")
            .build();
    invoke(
        service,
        "resolveLogStorePolicy",
        new Class<?>[] {IoTProduct.class},
        product);

    Map<?, ?> cache = getSuperclassMapField(service, "localLogStorePolicyCache");
    service.onLogCacheInvalidation(
        new IoTCacheInvalidationEvent(
            "device-offline",
            "pk001",
            "dev001",
            "iot001",
            Collections.emptySet(),
            Map.of("selectDevInstanceBOPD", List.of("pk001:dev001")),
            Collections.emptySet()));

    assertEquals(1, cache.size());
  }

  @Test
  void shouldStartSeparateBatchersForDeviceAndMetadataLogs() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    setField(service, "batchEnabled", true);
    setField(service, "batchSize", 1000);
    setField(service, "batchFlushIntervalMs", 10L);
    setField(service, "batchQueueCapacity", 1000);
    setField(service, "batchShutdownTimeoutMs", 1000L);

    invoke(service, "startDeviceLogBatcher", new Class<?>[] {});

    try {
      ExecutorService deviceExecutor =
          (ExecutorService) getField(service, "deviceLogBatchExecutor");
      ExecutorService metadataExecutor =
          (ExecutorService) getField(service, "deviceLogMetadataBatchExecutor");

      assertNotNull(deviceExecutor);
      assertNotNull(metadataExecutor);
    } finally {
      invoke(service, "stopDeviceLogBatcher", new Class<?>[] {});
    }
  }

  private static BaseUPRequest upRequest(IoTDeviceDTO device, int value) {
    BaseUPRequest request = new BaseUPRequest();
    request.setMessageType(MessageType.PROPERTIES);
    request.setIotId(device.getIotId());
    request.setProductKey(device.getProductKey());
    request.setDeviceId(device.getDeviceId());
    request.setDeviceName(device.getDeviceName());
    request.setTime(System.currentTimeMillis());
    request.setIoTDeviceDTO(device);
    request.setProperties(java.util.Map.of("temperature", value));
    request.setData(request.getProperties());
    request.setPayload("{\"temperature\":" + value + "}");
    return request;
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = CkDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Map<?, ?> getSuperclassMapField(Object target, String name) throws Exception {
    Field field = AbstractIoTDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    return (Map<?, ?>) field.get(target);
  }

  private static Object getField(Object target, String name) throws Exception {
    Field field = CkDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args)
      throws Exception {
    Method method = findMethod(target.getClass(), name, parameterTypes);
    method.setAccessible(true);
    return method.invoke(target, args);
  }

  private static Method findMethod(Class<?> type, String name, Class<?>[] parameterTypes)
      throws NoSuchMethodException {
    Class<?> current = type;
    while (current != null) {
      try {
        return current.getDeclaredMethod(name, parameterTypes);
      } catch (NoSuchMethodException ignored) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchMethodException(name);
  }

  private static class CapturingJdbcTemplate extends JdbcTemplate {
    private int batchUpdateCalls;
    private String lastSql;
    private List<Object[]> lastBatchArgs = List.of();

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
      batchUpdateCalls++;
      lastSql = sql;
      lastBatchArgs = new ArrayList<>(batchArgs);
      int[] counts = new int[batchArgs.size()];
      java.util.Arrays.fill(counts, 1);
      return counts;
    }
  }
}
