package cn.universal.dm.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.common.event.RedisEventPublisher;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("IoTCacheInvalidationPublisher 测试")
class IoTCacheInvalidationPublisherTest {

  @Test
  @DisplayName("产品存储策略变更应同时清理产品对象缓存和策略缓存")
  void shouldEvictProductCacheWhenStorePolicyChanges() throws Exception {
    IoTCacheInvalidationPublisher publisher = new IoTCacheInvalidationPublisher();
    CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
    setField(publisher, "applicationEventPublisher", eventPublisher);
    setField(publisher, "redisEventPublisher", new NoopRedisEventPublisher());

    publisher.publishProductStorePolicyChange("pk-1", "product-store-policy-update");

    IoTCacheInvalidationEvent event = eventPublisher.last(IoTCacheInvalidationEvent.class);
    assertEquals("product-store-policy-update", event.getReason());
    assertEquals("pk-1", event.getProductKey());
    assertTrue(event.getCacheNames().isEmpty());
    assertEquals(
        Set.of("pk-1"), event.getCacheKeys().get("iot_product_log_store_policy"));
    assertEquals(Set.of("pk-1"), event.getCacheKeys().get("iot_product_device"));
  }

  @Test
  @DisplayName("设备下线只应提交该设备的精细缓存键")
  void shouldEvictOnlyOfflineDeviceKeys() throws Exception {
    IoTCacheInvalidationPublisher publisher = publisher();

    publisher.publishDeviceChange(
        IoTDevicePostProcessor.Operation.OFFLINE,
        IoTDeviceDTO.builder()
            .iotId("iot-1")
            .productKey("pk-1")
            .deviceId("dev-1")
            .thirdPlatform("mqtt")
            .extDeviceId("ext-1")
            .build());

    IoTCacheInvalidationEvent event =
        ((CapturingEventPublisher) getField(publisher, "applicationEventPublisher"))
            .last(IoTCacheInvalidationEvent.class);
    assertEquals("iot-1", event.getIotId());
    assertTrue(event.getCacheNames().isEmpty());
    assertEquals(Set.of("pk-1:dev-1"), event.getCacheKeys().get("selectDevInstanceBOPD"));
    assertEquals(Set.of("iot-1"), event.getCacheKeys().get("selectDevInstanceBOIoTId"));
    assertTrue(event.getCacheKeys().get("selectDevInstanceBOMap").contains("pd:pk-1:dev-1"));
    assertTrue(event.getCacheKeys().get("iot_dev_action").contains("tpe:mqtt:ext-1"));
    assertFalse(event.getCacheKeys().containsKey("iot_dev_instance_bo"));
  }

  private static IoTCacheInvalidationPublisher publisher() throws Exception {
    IoTDeviceCacheKeyResolver deviceResolver = new IoTDeviceCacheKeyResolver();
    IoTDevActionCacheKeyResolver actionResolver = new IoTDevActionCacheKeyResolver();
    setField(actionResolver, "deviceCacheKeyResolver", deviceResolver);

    IoTCacheInvalidationPublisher publisher = new IoTCacheInvalidationPublisher();
    setField(publisher, "applicationEventPublisher", new CapturingEventPublisher());
    setField(publisher, "redisEventPublisher", new NoopRedisEventPublisher());
    setField(publisher, "iotDeviceCacheKeyResolver", deviceResolver);
    setField(publisher, "iotDevActionCacheKeyResolver", actionResolver);
    return publisher;
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Object getField(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  private static class CapturingEventPublisher implements ApplicationEventPublisher {
    private final List<Object> events = new ArrayList<>();

    @Override
    public void publishEvent(ApplicationEvent event) {
      events.add(event);
    }

    @Override
    public void publishEvent(Object event) {
      events.add(event);
    }

    private <T> T last(Class<T> eventType) {
      return eventType.cast(events.get(events.size() - 1));
    }
  }

  private static class NoopRedisEventPublisher extends RedisEventPublisher {
    @Override
    public void publishEvent(String topic, Object event) {}
  }
}
