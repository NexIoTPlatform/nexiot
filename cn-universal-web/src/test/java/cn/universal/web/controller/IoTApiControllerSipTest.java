package cn.universal.web.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.web.context.IoTCustomAuthContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class IoTApiControllerSipTest {
  @Test
  void legacyEnvelopeCannotSupplySipTenantAuthority() {
    IoTApiController controller = new IoTApiController();
    IoTProductDeviceService products = mock(IoTProductDeviceService.class);
    IoTCustomAuthContext legacyAuth = mock(IoTCustomAuthContext.class);
    ReflectionTestUtils.setField(controller, "iotProductDeviceService", products);
    ReflectionTestUtils.setField(controller, "ioTCustomAuthContext", legacyAuth);
    when(products.getProduct("pk")).thenReturn(IoTProduct.builder().thirdPlatform("sip").build());

    assertFalse(
        controller
            .iotDown("pk", "{\"tenantId\":\"forged\"}", new MockHttpServletRequest())
            .isSuccess());
    verifyNoInteractions(legacyAuth);
  }
}
