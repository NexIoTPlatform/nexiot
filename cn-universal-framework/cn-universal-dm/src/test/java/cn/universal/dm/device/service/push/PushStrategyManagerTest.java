package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.dm.device.service.push.transform.PushTransformService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import cn.universal.persistence.entity.bo.UPPushBO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PushStrategyManagerTest {

  private ApplicationPushHttpClient httpClient;

  @BeforeEach
  void setUp() {
    httpClient =
        new ApplicationPushHttpClient(
            new SimpleMeterRegistry(), 4, 60_000, 500, 800, 800, 1_000);
  }

  @AfterEach
  void tearDown() {
    httpClient.close();
  }

  @Test
  void shouldSkipSerializationWhenNoChannelIsEnabled() {
    AtomicInteger transformCount = new AtomicInteger();
    PushStrategyManager manager =
        manager(new CapturingTransform(transformCount), new Subscribe(httpClient));
    BaseUPRequest request = BaseUPRequest.builder().iotId("iot-1").build();

    assertEquals(List.of(), manager.executePush(request, UPPushBO.builder().build()));
    assertEquals(0, transformCount.get());
  }

  @Test
  void shouldExecuteEnabledDeviceSubscription() {
    Subscribe subscribe = new Subscribe(httpClient);
    PushStrategyManager manager = manager(new CapturingTransform(new AtomicInteger()), subscribe);
    BaseUPRequest request =
        BaseUPRequest.builder()
            .iotId("iot-1")
            .productKey("pk-1")
            .ioTDeviceDTO(IoTDeviceDTO.builder().thirdPlatform("tcp").build())
            .devSubscribe(
                List.of(IoTDeviceSubscribe.builder().url("http://localhost").enabled(true).build()))
            .build();
    UPPushBO config = UPPushBO.builder().build();

    assertEquals(1, manager.executePush(request, config).size());
    assertEquals(1, subscribe.executeCount.get());
  }

  private PushStrategyManager manager(
      PushTransformService transformService, SubscribePushStrategy subscribeStrategy) {
    PushStrategyManager manager = new PushStrategyManager();
    setField(manager, "httpPushStrategy", new DisabledHttp(httpClient));
    setField(manager, "mqttPushStrategy", new DisabledMqtt());
    setField(manager, "externalMqttPushStrategy", new DisabledExternalMqtt());
    setField(manager, "tcpPushStrategy", new DisabledTcp());
    setField(manager, "kafkaPushStrategy", new DisabledKafka());
    setField(manager, "rocketMQPushStrategy", new DisabledRocket());
    setField(manager, "subscribePushStrategy", subscribeStrategy);
    setField(manager, "pushTransformService", transformService);
    return manager;
  }

  private void setField(Object target, String name, Object value) {
    try {
      Field field = PushStrategyManager.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }

  private static final class CapturingTransform extends PushTransformService {
    private final AtomicInteger calls;

    private CapturingTransform(AtomicInteger calls) {
      this.calls = calls;
    }

    @Override
    public String transformPayload(
        BaseUPRequest request, UPPushBO config, String originalPayload) {
      calls.incrementAndGet();
      return originalPayload;
    }
  }

  private static final class Subscribe extends SubscribePushStrategy {
    private final AtomicInteger executeCount = new AtomicInteger();

    private Subscribe(ApplicationPushHttpClient httpClient) {
      super(httpClient);
    }

    @Override
    public IoTPushResult execute(BaseUPRequest request, String messageJson, Void config) {
      executeCount.incrementAndGet();
      return IoTPushResult.success("tcp", "pk-1", "iot-1", "Subscribe", messageJson, 1L);
    }
  }

  private static final class DisabledHttp extends HttpPushStrategy {
    private DisabledHttp(ApplicationPushHttpClient httpClient) {
      super(httpClient);
    }

    @Override
    public boolean isSupported(UPPushBO.HttpPushConfig config) {
      return false;
    }
  }

  private static final class DisabledMqtt extends MqttPushStrategy {
    @Override
    public boolean isSupported(UPPushBO.MqttPushConfig config) {
      return false;
    }
  }

  private static final class DisabledExternalMqtt extends ExternalMqttPushStrategy {
    private DisabledExternalMqtt() {
      super(null);
    }

    @Override
    public boolean isSupported(UPPushBO.ExternalMqttPushConfig config) {
      return false;
    }
  }

  private static final class DisabledTcp extends TcpPushStrategy {
    private DisabledTcp() {
      super(null);
    }

    @Override
    public boolean isSupported(UPPushBO.TcpPushConfig config) {
      return false;
    }
  }

  private static final class DisabledKafka extends KafkaPushStrategy {
    @Override
    public boolean isSupported(UPPushBO.KafkaPushConfig config) {
      return false;
    }
  }

  private static final class DisabledRocket extends RocketMQPushStrategy {
    @Override
    public boolean isSupported(UPPushBO.RocketMQPushConfig config) {
      return false;
    }
  }
}
