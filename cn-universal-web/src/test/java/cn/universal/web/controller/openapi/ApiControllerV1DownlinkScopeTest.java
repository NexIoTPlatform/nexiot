package cn.universal.web.controller.openapi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IDown;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.query.IoTAPIQuery;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ApiControllerV1DownlinkScopeTest {
  private static final String PROTOCOL = "openapi-scope-test";
  private final ScopedController controller = new ScopedController();
  private final IoTDeviceService devices = mock(IoTDeviceService.class);
  private final IoTProductDeviceService products = mock(IoTProductDeviceService.class);
  private final List<UnifiedDownlinkCommand> commands = new ArrayList<>();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(controller, "iotDeviceService", devices);
    ReflectionTestUtils.setField(controller, "iotProductDeviceService", products);
    IoTDeviceDTO device =
        IoTDeviceDTO.builder()
            .productKey("product-owned")
            .deviceId("phone-owned")
            .tenantId("tenant-owned")
            .iotId("iot-owned")
            .thirdPlatform(PROTOCOL)
            .build();
    when(devices.selectDevInstanceBO(anyMap())).thenReturn(device);
    when(products.getProduct("product-owned"))
        .thenReturn(
            IoTProduct.builder()
                .productKey("product-owned")
                .tenantId("tenant-owned")
                .thirdPlatform(PROTOCOL)
                .build());
    IoTDownlFactory.registerDown(
        new IDown() {
          public String code() {
            return PROTOCOL;
          }

          public String name() {
            return PROTOCOL;
          }

          public R<?> doAction(UnifiedDownlinkCommand command) {
            assertTrue(controller.checked, "authorization must precede dispatch");
            commands.add(command);
            return R.ok();
          }
        });
  }

  @AfterEach
  void cleanUp() {
    IoTDownlFactory.getEnabledDownServices().remove(PROTOCOL);
  }

  @Test
  void functionUsesAuthorizedDeviceScopeEvenWhenPayloadContainsOtherIdentifiers() {
    var body =
        JSONUtil.parseObj(
            "{\"function\":\"playAudio\",\"tenantId\":\"other-tenant\","
                + "\"productKey\":\"other-product\",\"deviceId\":\"other-phone\",\"data\":{\"audioFile\":\"hello.wav\"}}");
    assertTrue(controller.actionFunction("product-owned", "phone-owned", body).isSuccess());
    assertScope(commands.getFirst());
    assertEquals("playAudio", commands.getFirst().getFunction().get("function"));
  }

  @Test
  void updateAndDeletePropagateTheAuthorizedDeviceTenant() {
    IoTDeviceBO body = new IoTDeviceBO();
    body.setProductKey("other-product");
    body.setDeviceId("other-phone");
    body.setTenantId("other-tenant");
    assertTrue(controller.updateIoTDevice("product-owned", "phone-owned", body).isSuccess());
    assertScope(commands.getFirst());
    controller.checked = false;
    assertTrue(controller.deleteIoTDevice("product-owned", "phone-owned").isSuccess());
    assertScope(commands.getLast());
    assertEquals(2, commands.size());
  }

  @Test
  void rejectedAuthorizationNeverReachesAProtocolAdapter() {
    controller.allowed = false;
    assertThrows(
        IoTException.class,
        () -> controller.actionFunction("product-owned", "phone-owned", JSONUtil.createObj()));
    assertThrows(
        IoTException.class,
        () -> controller.updateIoTDevice("product-owned", "phone-owned", new IoTDeviceBO()));
    assertThrows(
        IoTException.class, () -> controller.deleteIoTDevice("product-owned", "phone-owned"));
    assertTrue(commands.isEmpty());
  }

  private void assertScope(UnifiedDownlinkCommand command) {
    assertEquals("tenant-owned", command.getTenantId());
    assertEquals("product-owned", command.getProductKey());
    assertEquals("phone-owned", command.getDeviceId());
    assertEquals("trusted-actor", command.getAppUnionId());
  }

  private static final class ScopedController extends ApiControllerV1 {
    boolean allowed = true;
    boolean checked;

    protected String iotUnionId() {
      return "trusted-actor";
    }

    protected String iotApplicationId() {
      return "trusted-app";
    }

    protected boolean checkDevSelf(IoTAPIQuery query) {
      check();
      return true;
    }

    protected void checkDevOrProductSelf(IoTAPIQuery query) {
      check();
    }

    private void check() {
      if (!allowed) throw new IoTException("denied");
      checked = true;
    }
  }
}
