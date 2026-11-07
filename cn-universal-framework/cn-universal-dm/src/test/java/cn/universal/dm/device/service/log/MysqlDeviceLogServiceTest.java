package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.message.UPRequest;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRequest;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.consistent.DeviceLogHashSharding;
import cn.universal.persistence.consistent.DeviceLogMetaHashSharding;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTDeviceLogMetadata;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceLogMetadataShardMapper;
import cn.universal.persistence.mapper.IoTDeviceLogShardMapper;
import cn.universal.persistence.query.DeviceLogQuery;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MysqlDeviceLogServiceTest {

  @Test
  void shouldComposeIotIdWhenQueryingRecentCommandByProductAndDevice() {
    CapturingMysqlDeviceLogService service = new CapturingMysqlDeviceLogService();

    service.getRecentCommand("pk001", "dev001", "cmd001", MessageType.FUNCTIONS.name(), 123L, 0);

    DeviceLogQuery query = service.getLastQuery();
    assertNotNull(query);
    assertEquals("pk001dev001", query.getIotId());
    assertEquals("pk001", query.getProductKey());
    assertEquals("dev001", query.getDeviceId());
    assertEquals("cmd001", query.getCommandId());
    assertEquals(MessageType.FUNCTIONS.name(), query.getMessageType());
    assertEquals(123L, query.getStartTime());
    assertEquals(0, query.getCommandStatus());
    assertEquals(1, query.getLimit());
  }

  @Test
  void shouldLeaveIotIdEmptyWhenProductOrDeviceIsMissing() {
    CapturingMysqlDeviceLogService service = new CapturingMysqlDeviceLogService();

    service.getRecentCommand(null, "dev001", "cmd001", MessageType.FUNCTIONS.name(), 123L, 0);
    assertNull(service.getLastQuery().getIotId());

    service.getRecentCommand("pk001", "", "cmd001", MessageType.FUNCTIONS.name(), 123L, 0);
    assertNull(service.getLastQuery().getIotId());
  }

  @Test
  void shouldUseExplicitIdBatchInsertForSingleMetadataRecord() throws Exception {
    MysqlDeviceLogService service = new MysqlDeviceLogService();
    CapturingMetadataMapper mapper = new CapturingMetadataMapper();
    setField(
        service,
        "ioTDeviceLogMetadataShardMapper",
        mapper.proxy(IoTDeviceLogMetadataShardMapper.class));

    UPRequest request = new UPRequest();
    request.setMessageType(MessageType.EVENT);
    request.setIotId("dahua_icc_0011000008");
    request.setProductKey("dahua_icc_001");
    request.setDeviceId("1000008");
    request.setDeviceName("IPC10001001");
    request.setEvent("offline");
    request.setEventName("设备离线");
    IoTProduct product = IoTProduct.builder().metadata("{}").build();

    invokeSaveLogStorePolicy(service, LogStorePolicyDTO.builder().build(), request, product);

    assertEquals(1, mapper.batchInsertCalls);
    assertEquals(0, mapper.generatedKeysInsertCalls);
    assertEquals("dahua_icc_0011000008", mapper.lastIotId);
    assertNotNull(mapper.lastRecords);
    assertEquals(1, mapper.lastRecords.size());
    IoTDeviceLogMetadata metadata = mapper.lastRecords.get(0);
    assertNotNull(metadata.getId());
    assertEquals("offline", metadata.getEvent());
  }

  @Test
  void shouldTrimLargeEventMetadataExt1ForRelationalStore() throws Exception {
    MysqlDeviceLogService service = new MysqlDeviceLogService();
    CapturingMetadataMapper mapper = new CapturingMetadataMapper();
    setField(
        service,
        "ioTDeviceLogMetadataShardMapper",
        mapper.proxy(IoTDeviceLogMetadataShardMapper.class));

    UPRequest request = new UPRequest();
    request.setMessageType(MessageType.EVENT);
    request.setIotId("hik-sdk-123192.168.31.120");
    request.setProductKey("hik-sdk-123");
    request.setDeviceId("192.168.31.120");
    request.setDeviceName("IPC19216831120");
    request.setEvent("cameraAlarm");
    request.setEventName("摄像机告警");
    request.setData(java.util.Map.of("raw", "x".repeat(1000)));
    IoTProduct product = IoTProduct.builder().metadata("{}").build();

    invokeSaveLogStorePolicy(service, LogStorePolicyDTO.builder().build(), request, product);

    IoTDeviceLogMetadata metadata = mapper.lastRecords.get(0);
    assertEquals("cameraAlarm", metadata.getEvent());
    assertNotNull(metadata.getExt1());
    assertEquals(255, metadata.getExt1().length());
  }

  @Test
  void shouldStoreBigDecimalMetadataContentWithoutScientificNotation() throws Exception {
    MysqlDeviceLogService service = new MysqlDeviceLogService();
    CapturingMetadataMapper mapper = new CapturingMetadataMapper();
    setField(
        service,
        "ioTDeviceLogMetadataShardMapper",
        mapper.proxy(IoTDeviceLogMetadataShardMapper.class));

    UPRequest request = new UPRequest();
    request.setMessageType(MessageType.PROPERTIES);
    request.setIotId("pk001dev001");
    request.setProductKey("pk001");
    request.setDeviceId("dev001");
    request.setDeviceName("dev001");
    request.setProperties(Map.of("temperature", new BigDecimal("7E+1")));
    request.setData(request.getProperties());
    IoTProduct product = IoTProduct.builder().metadata("{}").build();
    LogStorePolicyDTO policy =
        LogStorePolicyDTO.builder()
            .properties(Map.of("temperature", new LogStorePolicyDTO.StorePolicy("temperature", 10)))
            .build();

    invokeSaveLogStorePolicy(service, policy, request, product);

    IoTDeviceLogMetadata metadata = mapper.lastRecords.get(0);
    assertEquals("temperature", metadata.getProperty());
    assertEquals("70", metadata.getContent());
  }

  @Test
  void shouldSerializeFunctionDataAsDeviceLogContent() {
    MysqlDeviceLogService service = new MysqlDeviceLogService();
    IoTDeviceDTO device =
        IoTDeviceDTO.builder()
            .iotId("pk001dev001")
            .productKey("pk001")
            .deviceId("dev001")
            .deviceName("dev001")
            .build();
    BaseUPRequest request = new BaseUPRequest();
    request.setMessageType(MessageType.FUNCTIONS);
    request.setFunction(IoTConstant.SET_DEVICE_DESIRED_PROPERTIES);
    request.setCommandId("cmd001");
    request.setCommandStatus(1);
    request.setData(
        Map.of(
            "source",
            "industrial",
            "desired",
            Map.of("edf", false),
            "points",
            List.of(Map.of("propertyKey", "edf", "value", false))));

    IoTDeviceLog log = service.build(request, device);

    assertEquals(IoTConstant.SET_DEVICE_DESIRED_PROPERTIES, log.getEvent());
    assertEquals("cmd001", log.getCommandId());
    assertEquals(1, log.getCommandStatus());
    assertTrue(JSONUtil.isTypeJSON(log.getContent()));
    assertEquals(false, JSONUtil.parseObj(log.getContent()).getJSONObject("desired").getBool("edf"));
  }

  @Test
  void shouldBatchInsertDeviceLogsWhenFlushingRequests() throws Exception {
    MysqlDeviceLogService service = new MysqlDeviceLogService();
    CapturingLogMapper mapper = new CapturingLogMapper();
    setField(service, "ioTDeviceLogShardMapper", mapper.proxy(IoTDeviceLogShardMapper.class));
    IoTDeviceDTO device =
        IoTDeviceDTO.builder()
            .iotId("pk001dev001")
            .productKey("pk001")
            .deviceId("dev001")
            .deviceName("dev001")
            .build();
    IoTProduct product =
        IoTProduct.builder().productKey("pk001").storePolicy("mysql").metadata("{}").build();

    service.flushDeviceLogRequests(
        List.of(
            new DeviceLogWriteRequest(upRequest(device, 1), device, product),
            new DeviceLogWriteRequest(upRequest(device, 2), device, product)));

    assertEquals(1, mapper.batchInsertCalls);
    assertEquals(0, mapper.insertSelectiveCalls);
    assertEquals("pk001dev001", mapper.lastIotId);
    assertNotNull(mapper.lastRecords);
    assertEquals(2, mapper.lastRecords.size());
  }

  @Test
  void shouldBatchDeviceLogsFromDifferentDevicesOnTheSamePhysicalShard() throws Exception {
    MysqlDeviceLogService service = new MysqlDeviceLogService();
    CapturingLogMapper mapper = new CapturingLogMapper();
    setField(service, "ioTDeviceLogShardMapper", mapper.proxy(IoTDeviceLogShardMapper.class));
    setField(service, "deviceLogHashSharding", fixedLogShard("iot_device_log_7"));
    IoTDeviceDTO first = device("pk001dev001", "dev001");
    IoTDeviceDTO second = device("pk001dev002", "dev002");
    IoTProduct product =
        IoTProduct.builder().productKey("pk001").storePolicy("mysql").metadata("{}").build();

    service.flushDeviceLogRequests(
        List.of(
            new DeviceLogWriteRequest(upRequest(first, 1), first, product),
            new DeviceLogWriteRequest(upRequest(second, 2), second, product)));

    assertEquals(1, mapper.batchInsertCalls);
    assertEquals("pk001dev001", mapper.lastIotId);
    assertEquals(
        List.of("pk001dev001", "pk001dev002"),
        mapper.lastRecords.stream().map(IoTDeviceLog::getIotId).toList());
  }

  @Test
  void shouldBatchMetadataFromDifferentDevicesOnTheSamePhysicalShard() throws Exception {
    MysqlDeviceLogService service = new MysqlDeviceLogService();
    CapturingMetadataMapper mapper = new CapturingMetadataMapper();
    setField(
        service,
        "ioTDeviceLogMetadataShardMapper",
        mapper.proxy(IoTDeviceLogMetadataShardMapper.class));
    setField(service, "deviceLogMetaHashSharding", fixedMetadataShard("iot_device_log_metadata_3"));
    List<IoTDeviceLogMetadata> records =
        List.of(
            IoTDeviceLogMetadata.builder().id(1L).iotId("pk001dev001").build(),
            IoTDeviceLogMetadata.builder().id(2L).iotId("pk001dev002").build());

    invokeInsertMetadataRecords(service, records);

    assertEquals(1, mapper.batchInsertCalls);
    assertEquals("pk001dev001", mapper.lastIotId);
    assertEquals(
        List.of("pk001dev001", "pk001dev002"),
        mapper.lastRecords.stream().map(IoTDeviceLogMetadata::getIotId).toList());
  }

  @Test
  void shouldSplitLargeMetadataShardBatchAtTheRelationalLimit() throws Exception {
    MysqlDeviceLogService service = new MysqlDeviceLogService();
    CapturingMetadataMapper mapper = new CapturingMetadataMapper();
    setField(
        service,
        "ioTDeviceLogMetadataShardMapper",
        mapper.proxy(IoTDeviceLogMetadataShardMapper.class));
    setField(service, "deviceLogMetaHashSharding", fixedMetadataShard("iot_device_log_metadata_3"));
    List<IoTDeviceLogMetadata> records = new ArrayList<>();
    for (int index = 0; index < 1001; index++) {
      records.add(
          IoTDeviceLogMetadata.builder()
              .id((long) index + 1)
              .iotId("pk001dev" + index)
              .build());
    }

    invokeInsertMetadataRecords(service, records);

    assertEquals(2, mapper.batchInsertCalls);
    assertEquals(List.of(1000, 1), mapper.batchSizes);
  }

  @Test
  void shouldWriteDifferentPhysicalShardsConcurrently() throws Exception {
    MysqlDeviceLogService service = new MysqlDeviceLogService();
    BlockingLogMapper mapper = new BlockingLogMapper(2);
    setField(service, "ioTDeviceLogShardMapper", mapper.proxy(IoTDeviceLogShardMapper.class));
    setField(
        service,
        "deviceLogHashSharding",
        new DeviceLogHashSharding() {
          @Override
          public String generateTableName(String tableNamePrefix, Object value) {
            return value.toString().endsWith("1") ? "iot_device_log_1" : "iot_device_log_2";
          }
        });
    setField(service, "relationalShardConcurrency", 2);
    service.startRelationalShardWriter();

    IoTDeviceDTO first = device("pk001dev001", "dev001");
    IoTDeviceDTO second = device("pk001dev002", "dev002");
    IoTProduct product =
        IoTProduct.builder().productKey("pk001").storePolicy("mysql").metadata("{}").build();
    ExecutorService caller = Executors.newSingleThreadExecutor();
    try {
      Future<?> flush =
          caller.submit(
              () ->
                  service.flushDeviceLogRequests(
                      List.of(
                          new DeviceLogWriteRequest(upRequest(first, 1), first, product),
                          new DeviceLogWriteRequest(upRequest(second, 2), second, product))));

      assertTrue(mapper.awaitConcurrentCalls(2, TimeUnit.SECONDS));
      mapper.release();
      flush.get(2, TimeUnit.SECONDS);
      assertEquals(2, mapper.batchInsertCalls.get());
    } finally {
      mapper.release();
      caller.shutdownNow();
      service.stopRelationalShardWriter();
    }
  }

  private static class CapturingMysqlDeviceLogService extends MysqlDeviceLogService {
    private DeviceLogQuery lastQuery;

    @Override
    public List<IoTDeviceLog> queryDeviceLogs(DeviceLogQuery query) {
      this.lastQuery = query;
      return Collections.emptyList();
    }

    private DeviceLogQuery getLastQuery() {
      return lastQuery;
    }
  }

  private static void invokeSaveLogStorePolicy(
      MysqlDeviceLogService service,
      LogStorePolicyDTO policy,
      UPRequest request,
      IoTProduct product)
      throws Exception {
    Method method =
        MysqlDeviceLogService.class.getDeclaredMethod(
            "saveLogStorePolicy", LogStorePolicyDTO.class, UPRequest.class, IoTProduct.class);
    method.setAccessible(true);
    method.invoke(service, policy, request, product);
  }

  private static void invokeInsertMetadataRecords(
      MysqlDeviceLogService service, List<IoTDeviceLogMetadata> records) throws Exception {
    Method method =
        MysqlDeviceLogService.class.getDeclaredMethod("insertMetadataRecords", List.class);
    method.setAccessible(true);
    method.invoke(service, records);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = MysqlDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static BaseUPRequest upRequest(IoTDeviceDTO device, int value) {
    BaseUPRequest request = new BaseUPRequest();
    request.setMessageType(MessageType.PROPERTIES);
    request.setIotId(device.getIotId());
    request.setProductKey(device.getProductKey());
    request.setDeviceId(device.getDeviceId());
    request.setDeviceName(device.getDeviceName());
    request.setTime(System.currentTimeMillis());
    request.setProperties(java.util.Map.of("temperature", value));
    request.setData(request.getProperties());
    request.setPayload("{\"temperature\":" + value + "}");
    return request;
  }

  private static IoTDeviceDTO device(String iotId, String deviceId) {
    return IoTDeviceDTO.builder()
        .iotId(iotId)
        .productKey("pk001")
        .deviceId(deviceId)
        .deviceName(deviceId)
        .build();
  }

  private static DeviceLogHashSharding fixedLogShard(String tableName) {
    return new DeviceLogHashSharding() {
      @Override
      public String generateTableName(String tableNamePrefix, Object value) {
        return tableName;
      }
    };
  }

  private static DeviceLogMetaHashSharding fixedMetadataShard(String tableName) {
    return new DeviceLogMetaHashSharding() {
      @Override
      public String generateTableName(String tableNamePrefix, Object value) {
        return tableName;
      }
    };
  }

  private static class CapturingLogMapper implements InvocationHandler {
    private int batchInsertCalls;
    private int insertSelectiveCalls;
    private String lastIotId;
    private List<IoTDeviceLog> lastRecords;

    private <T> T proxy(Class<T> mapperType) {
      Object proxy =
          Proxy.newProxyInstance(
              mapperType.getClassLoader(), new Class<?>[] {mapperType}, this);
      return mapperType.cast(proxy);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) {
      if ("batchInsertByIotId".equals(method.getName())) {
        batchInsertCalls++;
        lastIotId = (String) args[0];
        lastRecords = (List<IoTDeviceLog>) args[1];
        return lastRecords.size();
      }
      if ("insertSelective".equals(method.getName())) {
        insertSelectiveCalls++;
        return 1;
      }
      if ("toString".equals(method.getName())) {
        return "CapturingLogMapper";
      }
      return defaultValue(method.getReturnType());
    }
  }

  private static class CapturingMetadataMapper implements InvocationHandler {
    private int batchInsertCalls;
    private int generatedKeysInsertCalls;
    private String lastIotId;
    private List<IoTDeviceLogMetadata> lastRecords;
    private final List<Integer> batchSizes = new ArrayList<>();

    private <T> T proxy(Class<T> mapperType) {
      Object proxy =
          Proxy.newProxyInstance(
              mapperType.getClassLoader(), new Class<?>[] {mapperType}, this);
      return mapperType.cast(proxy);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) {
      if ("batchInsertByIotId".equals(method.getName())) {
        batchInsertCalls++;
        lastIotId = (String) args[0];
        lastRecords = (List<IoTDeviceLogMetadata>) args[1];
        batchSizes.add(lastRecords.size());
        return lastRecords.size();
      }
      if ("insertUseGeneratedKeys".equals(method.getName())) {
        generatedKeysInsertCalls++;
        throw new AssertionError("insertUseGeneratedKeys must not be used for metadata shards");
      }
      if ("toString".equals(method.getName())) {
        return "CapturingMetadataMapper";
      }
      return defaultValue(method.getReturnType());
    }
  }

  private static class BlockingLogMapper implements InvocationHandler {
    private final CountDownLatch entered;
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicInteger batchInsertCalls = new AtomicInteger();

    private BlockingLogMapper(int expectedConcurrentCalls) {
      this.entered = new CountDownLatch(expectedConcurrentCalls);
    }

    private <T> T proxy(Class<T> mapperType) {
      Object proxy =
          Proxy.newProxyInstance(
              mapperType.getClassLoader(), new Class<?>[] {mapperType}, this);
      return mapperType.cast(proxy);
    }

    private boolean awaitConcurrentCalls(long timeout, TimeUnit unit) throws InterruptedException {
      return entered.await(timeout, unit);
    }

    private void release() {
      release.countDown();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws InterruptedException {
      if ("batchInsertByIotId".equals(method.getName())) {
        batchInsertCalls.incrementAndGet();
        entered.countDown();
        release.await(2, TimeUnit.SECONDS);
        return ((List<?>) args[1]).size();
      }
      if ("toString".equals(method.getName())) {
        return "BlockingLogMapper";
      }
      return defaultValue(method.getReturnType());
    }
  }

  private static Object defaultValue(Class<?> returnType) {
    if (!returnType.isPrimitive()) {
      return null;
    }
    if (boolean.class.equals(returnType)) {
      return false;
    }
    if (void.class.equals(returnType)) {
      return null;
    }
    return 0;
  }
}
