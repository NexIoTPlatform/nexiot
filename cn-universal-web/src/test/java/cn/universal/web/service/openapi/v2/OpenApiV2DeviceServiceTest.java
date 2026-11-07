package cn.universal.web.service.openapi.v2;

import static cn.universal.web.dto.openapi.v2.OpenApiV2DeviceModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IDown;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exercises the V2 boundary with the real unified command factory and registered downlink adapter.
 */
class OpenApiV2DeviceServiceTest {
  private final OpenApiV2Access access = mock(OpenApiV2Access.class);
  private final IoTDeviceMapper devices = mock(IoTDeviceMapper.class);
  private final IoTDeviceShadowService shadows = mock(IoTDeviceShadowService.class);
  private final OpenApiV2DeviceService service =
      new OpenApiV2DeviceService(access, devices, shadows);
  private final List<UnifiedDownlinkCommand> commands = new ArrayList<>();
  private final IoTProduct product =
      IoTProduct.builder().productKey("pk").thirdPlatform("mqtt").deviceNode("DEVICE").build();
  private final IoTDeviceDTO device =
      IoTDeviceDTO.builder()
          .iotId("pkdev")
          .productKey("pk")
          .deviceId("dev")
          .deviceName("original-name")
          .thirdPlatform("mqtt")
          .metadata("{\"functions\":[{\"id\":\"switch\",\"name\":\"switch\"}]}")
          .build();
  private R<?> protocolResult;

  @BeforeEach
  void setUp() {
    var app =
        IoTUserApplication.builder().appUniqueId("app").unionId("owner").tenantId("tenant").build();
    when(access.application()).thenReturn(app);
    when(access.tenantId(app)).thenReturn("tenant");
    when(access.department(app)).thenReturn(42L);
    when(access.product("pk")).thenReturn(product);
    when(access.device("pk", "dev")).thenReturn(device);
    protocolResult =
        R.ok(
            Map.of(
                "iotId",
                "pkactual-device",
                "deviceSecret",
                "private-secret",
                "configuration",
                "private-configuration"));
    IoTDownlFactory.registerDown(
        new IDown() {
          public String code() {
            return "mqtt";
          }

          public String name() {
            return "V2 test adapter";
          }

          public R<?> doAction(UnifiedDownlinkCommand command) {
            commands.add(command);
            return protocolResult;
          }
        });
  }

  @AfterEach
  void cleanUp() {
    IoTDownlFactory.getEnabledDownServices().remove("mqtt");
    IoTDownlFactory.getEnabledDownServices().remove("ezviz");
    IoTDownlFactory.getEnabledDownServices().remove("ttlock");
  }

  @Test
  void creationUsesTrustedIdentityAndReturnsActualSafePersistedIdentity() throws Exception {
    when(devices.selectOne(any(IoTDevice.class)))
        .thenReturn(
            IoTDevice.builder()
                .iotId("pkactual-device")
                .productKey("pk")
                .deviceId("actual-device")
                .deviceSecret("private-secret")
                .configuration("private-configuration")
                .build());
    var result = service.create("pk", create(Map.of("serial", "SN1")));
    var command = commands.getFirst();
    assertScope(command);
    assertEquals(DownCmd.DEV_ADD, command.getCmd());
    assertEquals(42L, command.getDeptId());
    assertFalse(command.isAdmin());
    assertFalse(command.isAllowInsert());
    assertFalse(command.isDeviceReuse());
    assertEquals("SN1", command.getExtensions().get("serial"));
    assertEquals("dev-name", command.getExtensions().get("deviceName"));
    assertEquals("actual-device", result.device().deviceId());
    assertEquals(command.getMsgId(), result.messageId());
    var query = ArgumentCaptor.forClass(IoTDevice.class);
    verify(devices).selectOne(query.capture());
    assertEquals("pkactual-device", query.getValue().getIotId());
    assertEquals("owner", query.getValue().getCreatorId());
    assertEquals("tenant", query.getValue().getTenantId());
    assertEquals("app", query.getValue().getApplication());
    String json = new ObjectMapper().writeValueAsString(result);
    assertFalse(json.contains("private-"));
    assertFalse(json.contains("deviceSecret"));
    assertFalse(json.contains("configuration"));
  }

  @Test
  void rejectsAllReservedCustomControlsAndValidatedPresentationFields() {
    for (String key :
        List.of(
            "tenantId",
            "APPLICATIONID",
            "isAdmin",
            "deviceReuse",
            "cmd",
            "gwProductKey",
            "extDeviceId",
            "slaveAddress",
            "longitude",
            "latitude",
            "deviceName",
            "detail",
            "deviceAddress",
            "msgId",
            "function",
            "extensions",
            "metadata")) {
      assertThrows(
          ResponseStatusException.class,
          () -> service.create("pk", create(Map.of(key, "bad"))),
          key);
    }
    assertTrue(commands.isEmpty());
  }

  @Test
  void creationNeverReusesAnExistingDevice() {
    when(devices.selectCount(any(IoTDevice.class))).thenReturn(1);
    var error =
        assertThrows(ResponseStatusException.class, () -> service.create("pk", create(null)));
    assertEquals(409, error.getStatusCode().value());
    assertTrue(commands.isEmpty());
  }

  @Test
  void gatewayCreationCannotAuthorizeOneGatewayAndRouteToAnother() {
    gatewayProduct();
    when(access.device("gateway-pk", "gateway-a")).thenReturn(gateway());
    assertThrows(
        ResponseStatusException.class, () -> service.create("pk", child("gateway-a", "gateway-b")));
    verify(access).device("gateway-pk", "gateway-a");
    assertTrue(commands.isEmpty());
  }

  @Test
  void authorizedGatewayIsTheOnlyParentForwardedToProtocol() {
    gatewayProduct();
    when(access.device("gateway-pk", "gateway-a")).thenReturn(gateway());
    service.create("pk", child("gateway-a", null));
    var command = commands.getFirst();
    assertEquals("gateway-pk", command.getGwProductKey());
    assertEquals("gateway-a", command.getGwDeviceId());
    assertEquals("gateway-a", command.getExtDeviceId());
    assertEquals("1", command.getSlaveAddress());
  }

  @Test
  void nonGatewayParentsAndGatewayFieldsOnOrdinaryDevicesAreRejected() {
    assertThrows(
        ResponseStatusException.class, () -> service.create("pk", child("gateway-a", null)));
    gatewayProduct();
    when(access.device("gateway-pk", "gateway-a")).thenReturn(device);
    assertThrows(
        ResponseStatusException.class, () -> service.create("pk", child("gateway-a", null)));
    assertTrue(commands.isEmpty());
  }

  @Test
  void invokingHistoricalChildRechecksActualPersistedGatewayScope() {
    device.setDeviceNode(DeviceNode.GATEWAY_SUB_DEVICE);
    device.setGwProductKey("gateway-pk");
    device.setExtDeviceId("foreign-gateway");
    when(access.device("gateway-pk", "foreign-gateway"))
        .thenThrow(new AccessDeniedException("denied"));
    assertThrows(
        AccessDeniedException.class,
        () -> service.invoke("pk", "dev", "switch", new FunctionInvocation(Map.of())));
    assertTrue(commands.isEmpty());
  }

  @Test
  void invocationKeepsFunctionPayloadInsideFunctionAndPreservesTrustedCommandScope() {
    protocolResult = R.ok(Map.of("value", 1));
    var result =
        service.invoke(
            "pk", "dev", "switch", new FunctionInvocation(Map.of("tenantId", "payload-value")));
    var command = commands.getFirst();
    assertScope(command);
    assertEquals(DownCmd.DEV_FUNCTION, command.getCmd());
    assertEquals("switch", command.getFunction().get("function"));
    assertEquals(Map.of("tenantId", "payload-value"), command.getFunction().get("data"));
    assertTrue(result.accepted());
    assertEquals(Map.of("value", 1), result.result());
  }

  @Test
  void rejectsUndefinedFunctionsBeforeDispatch() {
    assertThrows(
        ResponseStatusException.class,
        () -> service.invoke("pk", "dev", "missing", new FunctionInvocation(null)));
    assertTrue(commands.isEmpty());
  }

  @Test
  void ezvizFunctionCannotRedirectToAnotherCamera() {
    useProtocol("ezviz");
    assertEquals(
        400,
        assertThrows(
                ResponseStatusException.class,
                () ->
                    service.invoke(
                        "pk",
                        "dev",
                        "switch",
                        new FunctionInvocation(Map.of("deviceSerial", "foreign-camera"))))
            .getStatusCode()
            .value());
    assertTrue(commands.isEmpty());
  }

  @Test
  void ezvizAlwaysReceivesTrustedSerialWithoutMutatingCallerPayload() {
    useProtocol("ezviz");
    Map<String, Object> original = new LinkedHashMap<>();
    original.put("channelNo", 1);
    service.invoke("pk", "dev", "switch", new FunctionInvocation(original));
    assertEquals("dev", functionData().get("deviceSerial"));
    assertEquals(1, functionData().get("channelNo"));
    assertFalse(original.containsKey("deviceSerial"));
    assertScope(commands.getFirst());
  }

  @Test
  void ezvizCreationAlsoRejectsMismatchedSerial() {
    useProtocol("ezviz");
    assertThrows(
        ResponseStatusException.class,
        () -> service.create("pk", create(Map.of("deviceSerial", "foreign-camera"))));
    assertTrue(commands.isEmpty());
    service.create("pk", create(null));
    assertEquals("dev", commands.getFirst().getExtensions().get("deviceSerial"));
  }

  @Test
  void ttlockUsesPersistedExternalLockIdAndRejectsAnotherLock() {
    useProtocol("ttlock");
    function("unlock");
    device.setExtDeviceId("123");
    assertThrows(
        ResponseStatusException.class,
        () -> service.invoke("pk", "dev", "unlock", new FunctionInvocation(Map.of("lockId", 999))));
    assertTrue(commands.isEmpty());
    service.invoke("pk", "dev", "unlock", new FunctionInvocation(Map.of()));
    assertEquals(123, functionData().get("lockId"));
    assertScope(commands.getFirst());
  }

  @Test
  void ttlockResolvesNestedAndLegacyDeviceConfigurationWithoutTrustingInput() {
    useProtocol("ttlock");
    function("lockdetail");
    for (String config : List.of("{\"ttlock\":{\"lockId\":456}}", "{\"lockId\":456}")) {
      device.setDevConfiguration(config);
      service.invoke("pk", "dev", "lockdetail", new FunctionInvocation(Map.of("lockId", "456")));
      assertEquals(456, functionData().get("lockId"));
      commands.clear();
    }
  }

  @Test
  void ttlockMissingTrustedIdentityCannotUseCallerSuppliedLockId() {
    useProtocol("ttlock");
    function("unlock");
    assertEquals(
        409,
        assertThrows(
                ResponseStatusException.class,
                () ->
                    service.invoke(
                        "pk", "dev", "unlock", new FunctionInvocation(Map.of("lockId", 999))))
            .getStatusCode()
            .value());
    assertTrue(commands.isEmpty());
  }

  @Test
  void ttlockAccountLifecycleAndUnknownFunctionsAreNotDeviceGrants() {
    useProtocol("ttlock");
    device.setExtDeviceId("123");
    for (String name :
        List.of(
            "locklist",
            "userlist",
            "userdelete",
            "userregister",
            "dev_add",
            "dev_update",
            "dev_del",
            "futureAccountOperation")) {
      function(name);
      assertEquals(
          409,
          assertThrows(
                  ResponseStatusException.class,
                  () -> service.invoke("pk", "dev", name, new FunctionInvocation(Map.of())))
              .getStatusCode()
              .value(),
          name);
    }
    assertTrue(commands.isEmpty());
  }

  @Test
  void managedVideoCannotBypassDedicatedChannelAndOpaqueTaskFacade() {
    for (String protocol : List.of("wvp", "hik_sdk", "dahua_sdk", "tiandy_sdk", "hik_isup")) {
      device.setThirdPlatform(protocol);
      for (String name :
          List.of("cameraSnapshot", "queryCloudRecords", "snapshotStatus", "stopPlayback")) {
        function(name);
        assertEquals(
            409,
            assertThrows(
                    ResponseStatusException.class,
                    () -> service.invoke("pk", "dev", name, new FunctionInvocation(Map.of())))
                .getStatusCode()
                .value(),
            protocol + ":" + name);
      }
    }
    assertTrue(commands.isEmpty());
  }

  @Test
  void sipCallRequiresBothDevicesToBelongToCurrentApplication() {
    device.setThirdPlatform("sip");
    function("call");
    when(access.device("pk", "callee")).thenThrow(new AccessDeniedException("not bound"));
    assertThrows(
        AccessDeniedException.class,
        () ->
            service.invoke(
                "pk", "dev", "call", new FunctionInvocation(Map.of("targetDeviceId", "callee"))));
    verify(access).device("pk", "callee");
    assertTrue(commands.isEmpty());
  }

  @Test
  void sipCallUsesExplicitTargetProductForBindingCheck() {
    device.setThirdPlatform("sip");
    function("call");
    when(access.device("target-pk", "callee")).thenThrow(new AccessDeniedException("not bound"));
    assertThrows(
        AccessDeniedException.class,
        () ->
            service.invoke(
                "pk",
                "dev",
                "call",
                new FunctionInvocation(
                    Map.of("targetProductKey", "target-pk", "targetDeviceId", "callee"))));
    verify(access).device("target-pk", "callee");
    assertTrue(commands.isEmpty());
  }

  @Test
  void updatePreservesOmittedNameAndForwardsCoordinatesThroughExistingData() {
    var result =
        service.update("pk", "dev", new UpdateDevice(null, "new-detail", null, "121.47", "31.23"));
    var command = commands.getFirst();
    assertScope(command);
    assertEquals(DownCmd.DEV_UPDATE, command.getCmd());
    assertEquals("original-name", command.getExtensions().get("deviceName"));
    assertEquals("121.47", command.getExtensions().get("longitude"));
    assertEquals("31.23", command.getExtensions().get("latitude"));
    assertEquals("new-detail", command.getDetail());
    assertNull(result.result());
  }

  @Test
  void rejectsEmptyUpdateBlankFieldsAndInvalidCoordinates() {
    for (UpdateDevice body :
        List.of(
            new UpdateDevice(null, null, null, null, null),
            new UpdateDevice(" ", null, null, null, null),
            new UpdateDevice(null, "", null, null, null),
            new UpdateDevice(null, null, "", null, null),
            new UpdateDevice(null, null, null, "1", null),
            new UpdateDevice(null, null, null, "181", "0"),
            new UpdateDevice(null, null, null, "0", "91"),
            new UpdateDevice(null, null, null, "NaN", "0"))) {
      assertThrows(ResponseStatusException.class, () -> service.update("pk", "dev", body));
    }
    assertTrue(commands.isEmpty());
  }

  @Test
  void httpLifecycleCommandsAreRejectedBeforeCallingControlOnlyProtocol() {
    product.setThirdPlatform("http");
    device.setThirdPlatform("http");
    assertThrows(ResponseStatusException.class, () -> service.create("pk", create(null)));
    assertThrows(
        ResponseStatusException.class,
        () -> service.update("pk", "dev", new UpdateDevice("name", null, null, null, null)));
    assertThrows(ResponseStatusException.class, () -> service.delete("pk", "dev"));
    assertTrue(commands.isEmpty());
  }

  @Test
  void unsupportedLifecycleProtocolsCannotFallThroughToFunctionExecutors() {
    for (String protocol :
        List.of(
            "http",
            "wvp",
            "modbus",
            "onenet",
            "ctaiot",
            "ttlock",
            "imoulife",
            "hik_sdk",
            "dahua_sdk",
            "hik_isup",
            "unknown-future-protocol")) {
      product.setThirdPlatform(protocol);
      device.setThirdPlatform(protocol);
      assertEquals(
          409,
          assertThrows(ResponseStatusException.class, () -> service.create("pk", create(null)))
              .getStatusCode()
              .value());
      assertEquals(
          409,
          assertThrows(
                  ResponseStatusException.class,
                  () ->
                      service.update("pk", "dev", new UpdateDevice("name", null, null, null, null)))
              .getStatusCode()
              .value());
    }
    assertTrue(commands.isEmpty());
  }

  @Test
  void tcpCreateRejectsAddressThatTheExistingAdapterWouldSilentlyIgnore() {
    product.setThirdPlatform("tcp");
    assertEquals(
        400,
        assertThrows(
                ResponseStatusException.class,
                () ->
                    service.create(
                        "pk",
                        new CreateDevice(
                            "dev", "name", null, null, null, null, null, "address", null, null,
                            null)))
            .getStatusCode()
            .value());
    assertTrue(commands.isEmpty());
  }

  @Test
  void protocolFailuresDoNotBecomeSuccessfulAcceptance() {
    protocolResult = R.error(1234, "internal-secret-detail");
    var failure = assertThrows(IoTException.class, () -> service.delete("pk", "dev"));
    assertFalse(failure.getMessage().contains("internal-secret"));
    protocolResult = null;
    assertEquals(
        502,
        assertThrows(ResponseStatusException.class, () -> service.delete("pk", "dev"))
            .getStatusCode()
            .value());
  }

  @Test
  void safeDeviceProjectionDoesNotSerializeCredentialsAndMissingShadowIsEmpty() throws Exception {
    var entity =
        IoTDevice.builder()
            .iotId("pkdev")
            .deviceId("dev")
            .deviceSecret("private-secret")
            .configuration("private-config")
            .build();
    String json = new ObjectMapper().writeValueAsString(Device.from(entity));
    assertFalse(json.contains("private-"));
    assertFalse(json.contains("deviceSecret"));
    assertFalse(json.contains("configuration"));
    assertEquals(List.of(), service.properties("pk", "dev"));
    verify(shadows).getDevState("pkdev");
  }

  @Test
  void invalidListBoundsNeverReachMapper() {
    for (int[] bounds :
        List.of(new int[] {0, 20}, new int[] {1, 0}, new int[] {1, 201}, new int[] {52, 200}))
      assertThrows(
          ResponseStatusException.class, () -> service.list(null, null, bounds[0], bounds[1]));
    verifyNoInteractions(devices);
  }

  private void assertScope(UnifiedDownlinkCommand command) {
    assertEquals("pk", command.getProductKey());
    assertEquals("dev", command.getDeviceId());
    assertEquals("owner", command.getAppUnionId());
    assertEquals("app", command.getApplicationId());
    assertEquals("tenant", command.getTenantId());
    assertEquals("openapi-v2", command.getMetadata().getSource());
    assertNotNull(command.getMsgId());
  }

  private void function(String name) {
    device.setMetadata("{\"functions\":[{\"id\":\"" + name + "\",\"name\":\"" + name + "\"}]}");
  }

  private Map<?, ?> functionData() {
    return (Map<?, ?>) commands.getFirst().getFunction().get("data");
  }

  private void useProtocol(String protocol) {
    product.setThirdPlatform(protocol);
    device.setThirdPlatform(protocol);
    IoTDownlFactory.registerDown(
        new IDown() {
          public String code() {
            return protocol;
          }

          public String name() {
            return "V2 scope test adapter";
          }

          public R<?> doAction(UnifiedDownlinkCommand command) {
            commands.add(command);
            return protocolResult;
          }
        });
  }

  private CreateDevice create(Map<String, Object> data) {
    return new CreateDevice(
        "dev", "dev-name", null, null, null, null, null, null, null, null, data);
  }

  private CreateDevice child(String parent, String external) {
    return new CreateDevice(
        "dev", "dev-name", null, external, "gateway-pk", parent, "1", null, null, null, null);
  }

  private void gatewayProduct() {
    product.setDeviceNode(DeviceNode.GATEWAY_SUB_DEVICE.name());
    product.setGwProductKey("gateway-pk");
  }

  private IoTDeviceDTO gateway() {
    return IoTDeviceDTO.builder().deviceNode(DeviceNode.GATEWAY).build();
  }
}
