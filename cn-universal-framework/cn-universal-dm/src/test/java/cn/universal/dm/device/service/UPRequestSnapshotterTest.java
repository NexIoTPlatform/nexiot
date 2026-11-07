package cn.universal.dm.device.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import cn.universal.persistence.entity.IoTProduct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UPRequestSnapshotterTest {

  @Test
  void shouldDetachMutableRequestState() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("temperature", 20);
    IoTDeviceDTO device =
        IoTDeviceDTO.builder().iotId("iot-1").deviceId("dev-1").applicationId("app-1").build();
    IoTProduct product = IoTProduct.builder().productKey("pk-1").name("product-1").build();
    IoTDeviceSubscribe subscribe =
        IoTDeviceSubscribe.builder().url("http://one").enabled(true).build();
    List<IoTDeviceSubscribe> subscriptions = new ArrayList<>(List.of(subscribe));
    byte[] payload = new byte[] {1, 2, 3};
    BaseUPRequest source =
        BaseUPRequest.builder()
            .iotId("iot-1")
            .deviceId("dev-1")
            .productKey("pk-1")
            .payloadRaw(payload)
            .properties(properties)
            .ioTDeviceDTO(device)
            .ioTProduct(product)
            .devSubscribe(subscriptions)
            .debug(true)
            .build();

    BaseUPRequest snapshot = new UPRequestSnapshotter().snapshot(source);
    properties.put("temperature", 99);
    device.setDeviceId("changed");
    product.setName("changed");
    subscribe.setUrl("http://changed");
    payload[0] = 9;

    assertNotSame(source, snapshot);
    assertNotSame(source.getIoTDeviceDTO(), snapshot.getIoTDeviceDTO());
    assertNotSame(source.getIoTProduct(), snapshot.getIoTProduct());
    assertEquals(20, snapshot.getProperties().get("temperature"));
    assertEquals("dev-1", snapshot.getIoTDeviceDTO().getDeviceId());
    assertEquals("product-1", snapshot.getIoTProduct().getName());
    assertEquals(
        "http://one",
        ((List<IoTDeviceSubscribe>) snapshot.getDevSubscribe()).get(0).getUrl());
    assertArrayEquals(new byte[] {1, 2, 3}, snapshot.getPayloadRaw());
    assertEquals(true, snapshot.isDebug());
  }
}
