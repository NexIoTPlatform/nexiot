package cn.universal.dm.device.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.IoTDeviceFenceRelMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTDeviceShadowMapper;
import cn.universal.persistence.mapper.IoTDeviceSubscribeMapper;
import cn.universal.persistence.mapper.IoTDeviceTagsMapper;
import cn.universal.persistence.service.device.IoTDeviceDeleteContext;
import cn.universal.persistence.service.device.IoTDeviceDeleteParticipant;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class IoTDeviceServiceDeleteTest {

  @Test
  void shouldInvokeDeleteParticipantsBeforeCoreRowsAreDeleted() throws Exception {
    IoTDeviceService service = new IoTDeviceService();
    IoTDevice device =
        IoTDevice.builder()
            .id(1L)
            .iotId("iot-1")
            .tenantId("tenant-1")
            .productKey("product-1")
            .deviceId("device-1")
            .deviceName("PLC-1")
            .creatorId("user-1")
            .build();
    AtomicReference<String> phase = new AtomicReference<>("loaded");
    AtomicReference<IoTDeviceDeleteContext> captured = new AtomicReference<>();
    AtomicReference<String> evictedShadowIotId = new AtomicReference<>();

    IoTDeviceMapper deviceMapper =
        proxy(
            IoTDeviceMapper.class,
            (proxy, method, args) -> {
              if ("selectOne".equals(method.getName())
                  || "selectForUpdateByProductKeyAndDeviceId".equals(method.getName())) {
                return device;
              }
              if ("delete".equals(method.getName())) {
                assertEquals("participant", phase.get());
                phase.set("deleted");
                return 1;
              }
              return defaultValue(method.getReturnType());
            });
    IoTDeviceDeleteParticipant participant =
        context -> {
          assertEquals("loaded", phase.get());
          captured.set(context);
          phase.set("participant");
        };

    setField(service, "ioTDeviceMapper", deviceMapper);
    setField(service, "ioTDeviceTagsMapper", zeroMapper(IoTDeviceTagsMapper.class));
    setField(service, "ioTDeviceShadowMapper", zeroMapper(IoTDeviceShadowMapper.class));
    setField(service, "ioTDeviceSubscribeMapper", zeroMapper(IoTDeviceSubscribeMapper.class));
    setField(service, "ioTDeviceFenceRelMapper", zeroMapper(IoTDeviceFenceRelMapper.class));
    setField(
        service,
        "ioTDeviceShadowService",
        new IoTDeviceShadowService() {
          @Override
          public void evictDeviceShadowCache(String iotId) {
            evictedShadowIotId.set(iotId);
          }
        });
    setField(service, "iotCacheRemoveService", new IoTCacheRemoveService());
    setField(service, "deviceDeleteParticipants", List.of(participant));

    assertEquals(1, service.delDevInstance("iot-1"));
    assertEquals("deleted", phase.get());
    assertSame(device.getId(), captured.get().id());
    assertEquals("tenant-1", captured.get().tenantId());
    assertEquals("product-1", captured.get().productKey());
    assertEquals("device-1", captured.get().deviceId());
    assertEquals("iot-1", evictedShadowIotId.get());
  }

  @Test
  void shouldBeIdempotentWhenDeviceIsAlreadyMissing() throws Exception {
    IoTDeviceService service = new IoTDeviceService();
    setField(service, "ioTDeviceMapper", zeroMapper(IoTDeviceMapper.class));
    setField(
        service,
        "deviceDeleteParticipants",
        List.of(
            (IoTDeviceDeleteParticipant)
                context -> {
                  throw new AssertionError("must not run");
                }));

    assertEquals(0, service.delDevInstance("missing"));
  }

  private static <T> T zeroMapper(Class<T> type) {
    return proxy(type, (proxy, method, args) -> defaultValue(method.getReturnType()));
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  private static Object defaultValue(Class<?> type) {
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    if (type == boolean.class) {
      return false;
    }
    return null;
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
