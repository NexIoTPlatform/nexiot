package cn.universal.dm.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.hutool.json.JSONUtil;
import cn.universal.common.event.EventMessage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

class ClusterIoTCacheInvalidationHandlerTest {

  @Test
  void shouldPreserveDeviceIdentityAndConcreteKeysAcrossClusterMessage() throws Exception {
    IoTCacheInvalidationEvent source =
        new IoTCacheInvalidationEvent(
            "device-offline",
            "pk-1",
            "dev-1",
            "iot-1",
            Set.of("aggregate-cache"),
            Map.of(
                "selectDevInstanceBOMap", Set.of("iot:iot-1", "pd:pk-1:dev-1"),
                "selectDevInstanceBOIoTId", Set.of("iot-1")),
            Set.of("device:pk-1dev-1"));

    CapturingEventPublisher publisher = new CapturingEventPublisher();
    ClusterIoTCacheInvalidationHandler handler = new ClusterIoTCacheInvalidationHandler();
    setField(handler, "applicationEventPublisher", publisher);

    handler.handleDeviceCacheInvalidated(
        EventMessage.builder().data(JSONUtil.parseObj(JSONUtil.toJsonStr(source))).build());

    IoTCacheInvalidationEvent restored = publisher.last(IoTCacheInvalidationEvent.class);
    assertEquals(source.getReason(), restored.getReason());
    assertEquals(source.getProductKey(), restored.getProductKey());
    assertEquals(source.getDeviceId(), restored.getDeviceId());
    assertEquals(source.getIotId(), restored.getIotId());
    assertEquals(source.getCacheNames(), restored.getCacheNames());
    assertEquals(source.getCacheKeys(), restored.getCacheKeys());
    assertEquals(source.getRedisKeys(), restored.getRedisKeys());
  }

  private void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
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
}
