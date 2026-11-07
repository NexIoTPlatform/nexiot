package cn.universal.dm.device.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import cn.universal.persistence.mapper.IoTDeviceSubscribeMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;
import org.junit.jupiter.api.Test;

class DevicePushSubscriptionIndexTest {
  @Test
  void loadsOnceAndMatchesProductAndDeviceWithoutMessageTimeQueries() {
    var mapper = mock(IoTDeviceSubscribeMapper.class);
    var product =
        IoTDeviceSubscribe.builder().productKey("p").subType("PRODUCT").msgType("ALL").build();
    var device =
        IoTDeviceSubscribe.builder()
            .productKey("p")
            .subType("DEVICE")
            .iotId("PD")
            .msgType("PROPERTIES")
            .build();
    when(mapper.select(any())).thenReturn(List.of(product, device));
    try (var index = new DevicePushSubscriptionIndex(mapper, new SimpleMeterRegistry())) {
      index.refresh();
      for (int i = 0; i < 1000; i++)
        assertEquals(
            List.of(product, device).size(), index.find("p", "pd", MessageType.PROPERTIES).size());
      assertEquals(List.of(product), index.find("p", "other", MessageType.PROPERTIES));
      assertEquals(List.of(product), index.find("p", "pd", MessageType.EVENT));
      assertTrue(index.find("unknown", "pd", MessageType.PROPERTIES).isEmpty());
      verify(mapper, times(1)).select(any());
      when(mapper.select(any())).thenReturn(List.of());
      index.refresh();
      assertTrue(index.find("p", "pd", MessageType.PROPERTIES).isEmpty());
    }
  }
}
