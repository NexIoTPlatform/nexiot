package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.bo.UPPushBO;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MqttPushStrategyTest {

  @Test
  void shouldNotReportSuccessWhenSystemMqttRejectsPublish() {
    MqttPushStrategy strategy = new MqttPushStrategy();
    setField(strategy, "thingPrefix", "$thing");
    AtomicInteger publishedQos = new AtomicInteger(-1);
    setField(
        strategy,
        "mqttPushService",
        (MQTTPushService)
            (topic, payload, qos, retained) -> {
              publishedQos.set(qos);
              return false;
            });
    BaseUPRequest request =
        BaseUPRequest.builder()
            .iotId("iot-1")
            .deviceId("dev-1")
            .productKey("pk-1")
            .ioTDeviceDTO(IoTDeviceDTO.builder().appId("app-1").thirdPlatform("mqtt").build())
            .build();

    assertFalse(
        strategy
            .execute(request, "{}", UPPushBO.MqttPushConfig.builder().enable(true).support(true).build())
            .isOk());
    assertEquals(0, publishedQos.get());
  }

  private void setField(Object target, String name, Object value) {
    try {
      Field field = MqttPushStrategy.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }
}
