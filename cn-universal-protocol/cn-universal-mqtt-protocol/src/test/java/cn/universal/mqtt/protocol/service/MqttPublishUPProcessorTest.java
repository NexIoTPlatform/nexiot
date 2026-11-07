package cn.universal.mqtt.protocol.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.entity.ProcessingStage;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor.ProcessorResult;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MqttPublishUPProcessorTest {
  @Test
  void unboundDeviceWithoutLegacySubscriptionReachesNativeUplink() {
    var device =
        IoTDeviceDTO.builder().tenantId("tenant").productKey("product").deviceId("device").build();
    var decoded =
        BaseUPRequest.builder()
            .ioTDeviceDTO(device)
            .messageType(MessageType.PROPERTIES)
            .properties(Map.of("temperature", 21))
            .build();
    var request = new MQTTUPRequest();
    request.setIoTDeviceDTO(device);
    request.setUpRequestList(List.of(decoded));
    var processor = new CapturingProcessor();

    assertNull(device.getApplicationId());
    assertNull(request.getDevSubscribe());
    assertTrue(processor.supports(request));
    assertEquals(ProcessorResult.CONTINUE, processor.process(request));
    assertSame(decoded, processor.submitted.getFirst());
    assertSame(device, processor.submitted.getFirst().getIoTDeviceDTO());
    assertEquals(ProcessingStage.PUBLISH_PROCESSED, request.getStage());
  }

  @Test
  void unresolvedDeviceCannotReachUplink() {
    assertFalse(new CapturingProcessor().supports(new MQTTUPRequest()));
  }

  @Test
  void emptyDecodedBatchDoesNotSubmitWork() {
    var request = new MQTTUPRequest();
    request.setIoTDeviceDTO(IoTDeviceDTO.builder().applicationId("app").build());
    var processor = new CapturingProcessor();
    assertTrue(processor.supports(request));
    assertEquals(ProcessorResult.CONTINUE, processor.process(request));
    assertNull(processor.submitted);
  }

  private static class CapturingProcessor extends MqttPublishUPProcessor {
    List<BaseUPRequest> submitted;

    @Override
    protected boolean submitUp(List<BaseUPRequest> requests) {
      submitted = requests;
      return true;
    }
  }
}
