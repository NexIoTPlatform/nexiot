package cn.universal.admin.platform.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.LogQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DeviceHistoryAccessTest {
  @Test
  void rejectsForeignTenantAndMismatchedDeviceBeforeReadingHistory() {
    var guard = new DeviceHistoryAccess();
    var products = mock(IoTProductMapper.class);
    var devices = mock(IoTDeviceMapper.class);
    ReflectionTestUtils.setField(guard, "productMapper", products);
    ReflectionTestUtils.setField(guard, "deviceMapper", devices);
    when(products.getProductByProductKey("p"))
        .thenReturn(IoTProduct.builder().productKey("p").tenantId("t").build());
    var query = LogQuery.builder().productKey("p").iotId("i").deviceId("d").build();
    assertThrows(IoTException.class, () -> guard.requireAccess("other", false, query));
    verifyNoInteractions(devices);
    when(devices.selectOne(any()))
        .thenReturn(
            IoTDevice.builder().iotId("i").productKey("p").deviceId("d").tenantId("t").build());
    assertDoesNotThrow(() -> guard.requireAccess("t", false, query));
    assertDoesNotThrow(() -> guard.requireAccess("admin", true, query));
    query.setDeviceId("other");
    assertThrows(IoTException.class, () -> guard.requireAccess("t", false, query));
    assertThrows(IoTException.class, () -> guard.requireAccess(null, false, query));
  }
}
