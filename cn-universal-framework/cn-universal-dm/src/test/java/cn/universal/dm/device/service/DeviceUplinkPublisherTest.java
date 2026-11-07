package cn.universal.dm.device.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.event.*;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import java.util.*;
import org.junit.jupiter.api.Test;

class DeviceUplinkPublisherTest {
  @Test void retainsNativeCorrelationWithoutReplacingUplinkTraceIdentity() {
    List<DeviceUplinkEvent> events=new ArrayList<>();
    var request=BaseUPRequest.builder().messageType(MessageType.EVENT).event("motion")
        .data(Map.of("sourceEventId","native","eventId","sdk-event","receiptId","receipt"))
        .ioTDeviceDTO(IoTDeviceDTO.builder().tenantId("t").productKey("isup").deviceId("nvr").build()).build();
    new DeviceUplinkPublisher(List.of((identity,snapshot)->events.add(snapshot.get())),new io.micrometer.core.instrument.simple.SimpleMeterRegistry()).publish(List.of(request));
    assertEquals("native",events.getFirst().payload().get("sourceEventId"));assertEquals("receipt",events.getFirst().payload().get("receiptId"));
    assertNotEquals("native",events.getFirst().payload().get("eventId"));assertNotEquals("sdk-event",events.getFirst().payload().get("eventId"));
  }
  @Test
  void capturesDetachedTrustedIdentityWithoutApplicationAndSharesOneLazySnapshot() {
    Map<String, Object> props = new LinkedHashMap<>(Map.of("temperature", 21));
    var request =
        BaseUPRequest.builder()
            .messageType(MessageType.PROPERTIES)
            .productKey("forged")
            .deviceId("forged")
            .properties(props)
            .ioTDeviceDTO(
                IoTDeviceDTO.builder()
                    .tenantId("t")
                    .productKey("p")
                    .deviceId("d")
                    .iotId("pd")
                    .productName("产品")
                    .deviceName("设备")
                    .devGroupId(List.of("g"))
                    .build())
            .build();
    List<DeviceUplinkEvent> events = new ArrayList<>();
    DeviceUplinkSink sink = (identity, snapshot) -> events.add(snapshot.get());
    new DeviceUplinkPublisher(
            List.of(sink, sink), new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
        .publish(List.of(request));
    assertSame(events.get(0), events.get(1));
    var event = events.getFirst();
    assertEquals("p", event.identity().productKey());
    assertNull(event.identity().applicationId());
    props.put("temperature", 100);
    assertEquals(21, ((Map<?, ?>) event.payload().get("properties")).get("temperature"));
    assertEquals("产品", event.payload().get("productName"));
    assertThrows(UnsupportedOperationException.class, () -> event.payload().put("changed", true));
  }

  @Test
  void unmatchedSinkDoesNotSerializeUnsupportedPayload() {
    var request =
        BaseUPRequest.builder()
            .messageType(MessageType.PROPERTIES)
            .properties(Map.of("bad", new Object()))
            .ioTDeviceDTO(
                IoTDeviceDTO.builder().userUnionId("t").productKey("p").deviceId("d").build())
            .build();
    new DeviceUplinkPublisher(
            List.of((identity, snapshot) -> assertEquals("t", identity.tenantId())),
            new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
        .publish(List.of(request));
  }

  @Test
  void oversizedAndMutableScalarSnapshotsAreRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DeviceUplinkEvent.snapshot(null, Map.of("v", "x".repeat(3 * 1024 * 1024))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DeviceUplinkEvent.snapshot(
                null, Map.of("v", new java.util.concurrent.atomic.AtomicInteger())));
  }

  enum Quality {
    GOOD
  }

  record Sample(String propertyKey, Object value, Quality quality) {}

  @Test
  void industrialRecordsQualityCodesNullsAndDriverArraysBecomeDetachedJson() {
    int[] raw = {1, 2};
    var snapshot =
        DeviceUplinkEvent.snapshot(
            null,
            Map.of(
                "samples",
                List.of(new Sample("temperature", raw, Quality.GOOD)),
                "null",
                cn.hutool.json.JSONNull.NULL));
    raw[0] = 99;
    var sample = (Map<?, ?>) ((List<?>) snapshot.payload().get("samples")).getFirst();
    assertEquals("GOOD", sample.get("quality"));
    assertEquals(List.of(1, 2), sample.get("value"));
    assertNull(snapshot.payload().get("null"));
  }
}
