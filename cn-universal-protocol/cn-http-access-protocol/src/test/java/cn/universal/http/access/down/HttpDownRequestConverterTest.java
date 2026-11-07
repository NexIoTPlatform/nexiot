package cn.universal.http.access.down;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.downlink.DownlinkInterceptorChain;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.ICodecService;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.http.access.client.HttpInteractionExecutor;
import cn.universal.http.access.runtime.HttpOperationRegistry;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpDownRequestConverterTest {
  private final IoTProductDeviceService products = mock(IoTProductDeviceService.class);
  private final IoTDeviceService devices = mock(IoTDeviceService.class);
  private final ICodecService codec = mock(ICodecService.class);
  private final HttpDownRequestConverter converter = new HttpDownRequestConverter(products, devices, codec);

  @Test
  void lifecycleConversionDoesNotRequirePersistedDeviceOrInvokeEncode() {
    IoTProduct product = product();
    when(products.getProduct("product-1")).thenReturn(product);
    for (DownCmd cmd : List.of(DownCmd.DEV_ADD, DownCmd.DEV_DEL, DownCmd.DEV_UPDATE)) {
      UnifiedDownlinkCommand command = command(cmd);
      HttpDownRequest request = converter.convert(command, new DownlinkContext<>());
      assertEquals(cmd, request.getCmd());
      assertEquals("tenant-a", request.getTenantId());
      assertEquals("creator-a", request.getAppUnionId());
      assertEquals("HTTP meter", request.getData().getStr("deviceName"));
      assertSame(product, request.getProduct());
      assertNull(request.getEncodedPayload());
    }
    verifyNoInteractions(devices, codec);
  }

  @Test
  void productLifecycleDoesNotRequireDeviceIdentity() {
    when(products.getProduct("product-1")).thenReturn(product());
    UnifiedDownlinkCommand command = command(DownCmd.PRO_ADD);
    command.setDeviceId(null);
    assertEquals(DownCmd.PRO_ADD, converter.convert(command, new DownlinkContext<>()).getCmd());
    verifyNoInteractions(devices, codec);
  }

  @Test
  void controlStillRequiresExistingDeviceAndUsesProductEncode() {
    IoTProduct product = product();
    IoTDeviceDTO device = new IoTDeviceDTO();
    when(products.getProduct("product-1")).thenReturn(product);
    when(devices.selectDevInstanceBO("product-1", "device-1")).thenReturn(device);
    when(codec.encode(eq("product-1"), anyString(), any())).thenReturn("driver-control-payload");
    UnifiedDownlinkCommand command = command(DownCmd.DEV_FUNCTION);
    command.setProperties(Map.of("targetTemperature", 21));

    HttpDownRequest request = converter.convert(command, new DownlinkContext<>());

    assertSame(device, request.getDevice());
    assertEquals("driver-control-payload", request.getEncodedPayload());
    verify(codec).encode(eq("product-1"), eq(command.toJson().toString()), any());
  }

  @Test
  void missingControlDeviceFailsBeforeEncode() {
    when(products.getProduct("product-1")).thenReturn(product());
    assertThrows(IllegalArgumentException.class,
        () -> converter.convert(command(DownCmd.DEV_FUNCTION), new DownlinkContext<>()));
    verifyNoInteractions(codec);
  }

  @Test
  void localLifecycleRunsBeforeHttpNetworkLookupOrDriverInteraction() {
    HttpOperationRegistry registry = mock(HttpOperationRegistry.class);
    HttpInteractionExecutor interaction = mock(HttpInteractionExecutor.class);
    HttpDeviceLifecycleService lifecycle = mock(HttpDeviceLifecycleService.class);
    HttpAccessDownService service = new HttpAccessDownService(
        registry, interaction, mock(DownlinkInterceptorChain.class), lifecycle);
    HttpDownRequest request = new HttpDownRequest();
    request.setCmd(DownCmd.DEV_ADD);
    DownlinkContext<HttpDownRequest> context = new DownlinkContext<>();
    context.setDownRequest(request);
    R<?> expected = R.ok("created-locally");
    doReturn(expected).when(lifecycle).process(request);

    assertSame(expected, service.doProcess(context));

    verifyNoInteractions(registry, interaction);
  }

  private UnifiedDownlinkCommand command(DownCmd cmd) {
    return UnifiedDownlinkCommand.fromJson(JSONUtil.parseObj(Map.of(
        "cmd", cmd.name(), "productKey", "product-1", "deviceId", "device-1",
        "tenantId", "tenant-a", "appUnionId", "creator-a", "deptId", 5,
        "data", Map.of("deviceName", "HTTP meter"))));
  }

  private IoTProduct product() {
    return IoTProduct.builder().productKey("product-1").thirdPlatform("http").build();
  }
}
