package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteBuffer;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class DefaultIoTDeviceLogServiceTest {

  @Test
  void shouldPersistSipServerMessagesWithoutOverridingRegistrationState() throws Exception {
    DefaultIoTDeviceLogService service = new DefaultIoTDeviceLogService();
    AtomicInteger directWrites = new AtomicInteger();
    AtomicInteger onlineFlushes = new AtomicInteger();
    RecordingDeviceLogWriteBuffer writer = new RecordingDeviceLogWriteBuffer();
    configureOnlineRefresh(service, directWrites, onlineFlushes);
    setField(service, "deviceLogWriteBuffer", writer);
    IoTProduct product = IoTProduct.builder().storePolicy("mysql").thirdPlatform("sip").build();
    IoTDeviceDTO device = IoTDeviceDTO.builder().iotId("sip-offline").build();
    BaseUPRequest properties = BaseUPRequest.builder().messageType(MessageType.PROPERTIES).build();
    properties.setProperties(Map.of("registered", false, "registrationState", "offline"));
    BaseUPRequest event =
        BaseUPRequest.builder().messageType(MessageType.EVENT).event("unregistered").build();

    try {
      service.saveDeviceLogDirect(properties, device, product);
      service.saveDeviceLog(event, device, product);
      service.saveDeviceLog(
          IoTDeviceLog.builder().messageType("EVENT").event("connectionLost").build(),
          device,
          product);
      flushOnline(service);
    } finally {
      writer.shutdown();
    }

    assertEquals(1, directWrites.get());
    assertEquals(1, writer.requestWrites);
    assertEquals(1, writer.entityWrites);
    assertEquals(0, onlineFlushes.get());
  }

  @Test
  void shouldStillRefreshOnlineForNormalDeviceReports() throws Exception {
    DefaultIoTDeviceLogService service = new DefaultIoTDeviceLogService();
    AtomicInteger directWrites = new AtomicInteger();
    AtomicInteger onlineFlushes = new AtomicInteger();
    configureOnlineRefresh(service, directWrites, onlineFlushes);

    service.saveDeviceLogDirect(
        BaseUPRequest.builder().messageType(MessageType.PROPERTIES).build(),
        IoTDeviceDTO.builder().iotId("mqtt-device").build(),
        IoTProduct.builder().storePolicy("mysql").thirdPlatform("mqtt").build());
    flushOnline(service);

    assertEquals(1, directWrites.get());
    assertEquals(1, onlineFlushes.get());
  }

  private static void configureOnlineRefresh(
      DefaultIoTDeviceLogService service,
      AtomicInteger directWrites,
      AtomicInteger onlineFlushes)
      throws Exception {
    setField(service, "policies", Map.of("mysql", capturingLogService(directWrites)));
    setField(service, "onlineRefreshIntervalSeconds", 0L);
    setField(
        service,
        "ioTDeviceMapper",
        Proxy.newProxyInstance(
            IoTDeviceMapper.class.getClassLoader(),
            new Class<?>[] {IoTDeviceMapper.class},
            (proxy, method, args) -> {
              if ("batchFlushLog".equals(method.getName())) onlineFlushes.incrementAndGet();
              return defaultValue(method.getReturnType());
            }));
  }

  private static void flushOnline(DefaultIoTDeviceLogService service) throws Exception {
    Method method = DefaultIoTDeviceLogService.class.getDeclaredMethod("doOnline");
    method.setAccessible(true);
    method.invoke(service);
  }

  @Test
  void shouldResolveMysqlDoubleWriteFromLoadedProductConfigurationOnDirectPath() throws Exception {
    DefaultIoTDeviceLogService service = new DefaultIoTDeviceLogService();
    IoTProduct enabled =
        IoTProduct.builder()
            .productKey("pk001")
            .configuration("{\"enableMysqlWrite\":true}")
            .build();
    IoTProduct blank = IoTProduct.builder().productKey("pk002").configuration("").build();

    assertTrue(enableMysqlWrite(service, enabled, true));
    assertFalse(enableMysqlWrite(service, blank, true));
  }

  @Test
  void shouldThrottleOnlineRefreshLocallyBeforeRedis() throws Exception {
    DefaultIoTDeviceLogService service = new DefaultIoTDeviceLogService();
    CountingStringRedisTemplate redisTemplate = new CountingStringRedisTemplate(true);
    setField(service, "stringRedisTemplate", redisTemplate);
    setField(service, "onlineRefreshIntervalSeconds", 60L);
    setField(service, "onlineRefreshKeyPrefix", "online:");

    assertTrue(allowOnlineTimeRefresh(service, "iot-1"));
    assertFalse(allowOnlineTimeRefresh(service, "iot-1"));

    assertEquals(1, redisTemplate.setIfAbsentCalls);
  }

  @Test
  void shouldRespectWriterRejectionWithoutExternalDirectFallback() throws Exception {
    DefaultIoTDeviceLogService service = new DefaultIoTDeviceLogService();
    DeviceLogWriteBuffer writer = new RejectingDeviceLogWriteBuffer();
    AtomicInteger directWriteCalls = new AtomicInteger();
    IIoTDeviceLogService logService = capturingLogService(directWriteCalls);
    BaseUPRequest request = new BaseUPRequest();
    IoTDeviceDTO device = IoTDeviceDTO.builder().iotId("iot-1").build();
    IoTProduct product = IoTProduct.builder().storePolicy("mysql").build();
    setField(service, "deviceLogWriteBuffer", writer);

    try {
      submitSave(service, "mysql", logService, request, device, product);
    } finally {
      writer.shutdown();
    }

    assertEquals(0, directWriteCalls.get());
  }

  private static boolean enableMysqlWrite(
      DefaultIoTDeviceLogService service, IoTProduct product, boolean direct) throws Exception {
    Method method =
        DefaultIoTDeviceLogService.class.getDeclaredMethod(
            "enableMysqlWrite", BaseUPRequest.class, IoTProduct.class, boolean.class);
    method.setAccessible(true);
    BaseUPRequest request = new BaseUPRequest();
    request.setProductKey(product.getProductKey());
    return (Boolean) method.invoke(service, request, product, direct);
  }

  private static boolean allowOnlineTimeRefresh(
      DefaultIoTDeviceLogService service, String iotId) throws Exception {
    Method method =
        DefaultIoTDeviceLogService.class.getDeclaredMethod("allowOnlineTimeRefresh", String.class);
    method.setAccessible(true);
    return (Boolean) method.invoke(service, iotId);
  }

  private static void submitSave(
      DefaultIoTDeviceLogService service,
      String policy,
      IIoTDeviceLogService logService,
      BaseUPRequest request,
      IoTDeviceDTO device,
      IoTProduct product)
      throws Exception {
    Method method =
        DefaultIoTDeviceLogService.class.getDeclaredMethod(
            "submitSave",
            String.class,
            IIoTDeviceLogService.class,
            BaseUPRequest.class,
            IoTDeviceDTO.class,
            IoTProduct.class);
    method.setAccessible(true);
    method.invoke(service, policy, logService, request, device, product);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = DefaultIoTDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static IIoTDeviceLogService capturingLogService(AtomicInteger directWriteCalls) {
    return (IIoTDeviceLogService)
        Proxy.newProxyInstance(
            IIoTDeviceLogService.class.getClassLoader(),
            new Class<?>[] {IIoTDeviceLogService.class},
            (proxy, method, args) -> {
              if ("saveDeviceLogDirect".equals(method.getName())) {
                directWriteCalls.incrementAndGet();
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (void.class.equals(type)) {
      return null;
    }
    if (boolean.class.equals(type)) {
      return false;
    }
    if (char.class.equals(type)) {
      return '\0';
    }
    return 0;
  }

  private static final class RejectingDeviceLogWriteBuffer extends DeviceLogWriteBuffer {
    private RejectingDeviceLogWriteBuffer() {
      super("direct", "kafka", false, false, true, 1, 1, 1, 1, 10, 0, 1000, List.of());
    }

    @Override
    public boolean submitRequest(
        String policy,
        IIoTDeviceLogService service,
        BaseUPRequest upRequest,
        IoTDeviceDTO ioTDeviceDTO,
        IoTProduct ioTProduct) {
      return false;
    }

  }

  private static final class RecordingDeviceLogWriteBuffer extends DeviceLogWriteBuffer {
    private int requestWrites;
    private int entityWrites;

    private RecordingDeviceLogWriteBuffer() {
      super("direct", "kafka", false, false, true, 1, 1, 1, 1, 10, 0, 1000, List.of());
    }

    @Override
    public boolean submitRequest(
        String policy,
        IIoTDeviceLogService service,
        BaseUPRequest upRequest,
        IoTDeviceDTO device,
        IoTProduct product) {
      requestWrites++;
      return true;
    }

    @Override
    public boolean submitEntity(
        String policy,
        IIoTDeviceLogService service,
        IoTDeviceLog deviceLog,
        IoTDeviceDTO device,
        IoTProduct product) {
      entityWrites++;
      return true;
    }
  }

  private static class CountingStringRedisTemplate extends StringRedisTemplate {
    private final Boolean result;
    private int setIfAbsentCalls;

    private CountingStringRedisTemplate(Boolean result) {
      this.result = result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValueOperations<String, String> opsForValue() {
      return (ValueOperations<String, String>)
          Proxy.newProxyInstance(
              ValueOperations.class.getClassLoader(),
              new Class<?>[] {ValueOperations.class},
              (proxy, method, args) -> {
                if ("setIfAbsent".equals(method.getName())) {
                  setIfAbsentCalls++;
                  return result;
                }
                return defaultValue(method.getReturnType());
              });
    }

  }
}
