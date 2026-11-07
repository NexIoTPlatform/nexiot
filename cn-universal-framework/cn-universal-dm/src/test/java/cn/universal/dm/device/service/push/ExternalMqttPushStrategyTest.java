package cn.universal.dm.device.service.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.bo.UPPushBO;
import org.junit.jupiter.api.Test;

class ExternalMqttPushStrategyTest {

  @Test
  void shouldExpandTopicPlaceholdersBeforePublishing() {
    CapturingClientManager clientManager = new CapturingClientManager();
    ExternalMqttPushStrategy strategy = new ExternalMqttPushStrategy(clientManager);
    BaseUPRequest request =
        BaseUPRequest.builder()
            .applicationId("app-1")
            .productKey("pk-1")
            .deviceId("dev-1")
            .iotId("iot-1")
            .messageType(MessageType.PROPERTIES)
            .build();
    UPPushBO.ExternalMqttPushConfig config =
        UPPushBO.ExternalMqttPushConfig.builder()
            .enable(true)
            .support(true)
            .brokerUrl("tcp://localhost:1883")
            .topic("up/${applicationId}/${productKey}/${deviceId}/${messageType}")
            .build();

    assertTrue(strategy.execute(request, "{}", config).isOk());
    assertEquals("app-1", clientManager.appUniqueId);
    assertEquals("up/app-1/pk-1/dev-1/PROPERTIES", clientManager.topic);
    assertEquals("{}", clientManager.payload);
  }

  private static final class CapturingClientManager extends ExternalMqttClientManager {
    private String appUniqueId;
    private String topic;
    private String payload;

    @Override
    public void publish(
        String appUniqueId,
        UPPushBO.ExternalMqttPushConfig config,
        String topic,
        String payload) {
      this.appUniqueId = appUniqueId;
      this.topic = topic;
      this.payload = payload;
    }
  }
}
