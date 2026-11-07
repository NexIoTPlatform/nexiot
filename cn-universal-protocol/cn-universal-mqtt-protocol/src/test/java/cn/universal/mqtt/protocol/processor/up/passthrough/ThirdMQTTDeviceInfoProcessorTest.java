package cn.universal.mqtt.protocol.processor.up.passthrough;

import static cn.universal.mqtt.protocol.config.MqttConstant.TopicCategory.*;
import static cn.universal.mqtt.protocol.processor.MqttMessageProcessor.ProcessorResult.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.core.message.UPRequest;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.third.ThirdMQTTServerManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ThirdMQTTDeviceInfoProcessorTest {
  private final ThirdMQTTServerManager manager = mock(ThirdMQTTServerManager.class);
  private final TestProcessor processor = new TestProcessor();

  ThirdMQTTDeviceInfoProcessorTest() {
    ReflectionTestUtils.setField(processor, "thirdMQTTServerManager", manager);
  }

  @Test
  void preservesAlreadyResolvedCategoryForInFlightMessageAfterStop() {
    var request = request();
    request.setTopicCategory(PASSTHROUGH);
    assertEquals(CONTINUE, processor.process(request));
    assertEquals("FF8A050101005543", request.getDeviceId());
    assertEquals(PASSTHROUGH, request.getContextValue("topicCategory"));
    verifyNoInteractions(manager);
  }

  @Test
  void missingCategoryFallsBackToNetworkAndActualTopic() {
    var request = request();
    request.setTopicCategory(UNKNOWN);
    when(manager.resolveTopicCategory("network1", request.getUpTopic())).thenReturn(THING_MODEL);
    assertEquals(CONTINUE, processor.process(request));
    assertEquals(THING_MODEL, request.getTopicCategory());
    assertEquals(THING_MODEL, request.getContextValue("topicCategory"));
  }

  @Test
  void unresolvedCategoryStopsBeforePreDecode() {
    var request = request();
    when(manager.resolveTopicCategory("network1", request.getUpTopic())).thenReturn(UNKNOWN);
    assertEquals(ERROR, processor.process(request));
    assertNull(request.getDeviceId());
    assertEquals(0, processor.preDecodeCalls);
  }

  private MQTTUPRequest request() {
    return MQTTUPRequest.builder()
        .networkUnionId("network1")
        .productKey("7Gc9jZqGthEz")
        .upTopic("hongfa/FF8A050101005543/upload/")
        .payload("{\"GSN\":\"FF8A050101005543\",\"GW_Respond\":\"73BB01090005FF04020000\"}")
        .build();
  }

  private static class TestProcessor extends ThirdMQTTDeviceInfoProcessor {
    int preDecodeCalls;

    @Override
    protected boolean fillProductInfo(MQTTUPRequest request) {
      return true;
    }

    @Override
    protected boolean fillDeviceInfo(MQTTUPRequest request) {
      return true;
    }

    @Override
    protected UPRequest preDecode(String productKey, String payload, Object context) {
      preDecodeCalls++;
      return UPRequest.builder().deviceId("FF8A050101005543").build();
    }
  }
}
