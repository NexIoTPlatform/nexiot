package cn.universal.admin.platform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IDown;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.mapper.IoTDeviceFenceRelMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

/** Exercises the existing management entry points, where SIP identity/config can be lost. */
class SipDeviceManagementTest {
  private final IoTUser user = IoTUser.builder().unionId("operator").tenantId("tenant-a").build();
  private final IoTDevice device =
      IoTDevice.builder()
          .id(1L)
          .iotId("pk1001")
          .productKey("pk")
          .deviceId("1001")
          .tenantId("tenant-a")
          .creatorId("operator")
          .configuration("{\"sipUsername\":\"1001\",\"sipPassword\":\"stored-secret\"}")
          .build();
  private final IoTDeviceMapper devices = mock(IoTDeviceMapper.class);
  private final IoTProductMapper products = mock(IoTProductMapper.class);
  private final IDown down = mock(IDown.class);
  private final IoTDeviceServiceImpl service =
      new IoTDeviceServiceImpl() {
        @Override
        public IoTUser queryIotUser(String unionId) {
          return user;
        }

        @Override
        protected boolean canAccessDeptResource(
            String tenant, Long dept, String creator, IoTUser actor) {
          return actor != null && Objects.equals(tenant, actor.getTenantId());
        }
      };
  private MockedStatic<SecurityUtils> security;
  private MockedStatic<IoTDownlFactory> factory;

  @BeforeEach
  void setup() {
    security = mockStatic(SecurityUtils.class);
    security.when(SecurityUtils::getUnionIdQuietly).thenReturn("operator");
    factory = mockStatic(IoTDownlFactory.class);
    factory.when(() -> IoTDownlFactory.getIDown("sip")).thenReturn(down);
    IoTUserService users = mock(IoTUserService.class);
    when(users.selectUserByUnionId("operator")).thenReturn(user);
    ReflectionTestUtils.setField(service, "ioTUserService", users);
    ReflectionTestUtils.setField(service, "ioTDeviceMapper", devices);
    ReflectionTestUtils.setField(service, "ioTProductMapper", products);
    ReflectionTestUtils.setField(
        service, "ioTDeviceFenceRelMapper", mock(IoTDeviceFenceRelMapper.class));
    when(products.getProductByProductKey("pk"))
        .thenReturn(
            IoTProduct.builder()
                .productKey("pk")
                .thirdPlatform("sip")
                .tenantId("tenant-a")
                .build());
    when(devices.selectByPrimaryKey(1L)).thenReturn(device);
    when(devices.selectDevInstanceById("1")).thenReturn(device);
    when(devices.selectIoTDevice("pk", "1001")).thenReturn(device);
    when(down.doAction(any(UnifiedDownlinkCommand.class))).thenReturn(R.ok());
  }

  @AfterEach
  void close() {
    factory.close();
    security.close();
  }

  @Test
  void createForwardsSipConfigurationWithoutCustomFieldMetadata() {
    assertTrue(
        service
            .insertDevInstance(
                IoTDeviceBO.builder()
                    .productKey("pk")
                    .deviceId("1001")
                    .creatorId("operator")
                    .configuration(
                        "{\"sipUsername\":\"1001\",\"sipPassword\":\"new-secret\",\"sipAutoAnswer\":true}")
                    .build())
            .isSuccess());
    UnifiedDownlinkCommand command = command();
    assertEquals("tenant-a", command.getTenantId());
    assertEquals("1001", command.getExtensions().get("sipUsername"));
    assertEquals("new-secret", command.getExtensions().get("sipPassword"));
    assertEquals(true, command.getExtensions().get("sipAutoAnswer"));
  }

  @Test
  void updateForwardsTenantAndDoesNotOverwritePluginPasswordWithMask() {
    assertTrue(
        service
            .updateDevInstance(
                IoTDeviceBO.builder()
                    .id(1L)
                    .configuration("{\"sipUsername\":\"1001\",\"sipPassword\":\"******\"}")
                    .build())
            .isSuccess());
    assertEquals("tenant-a", command().getTenantId());
    assertEquals(
        "stored-secret", JSONUtil.parseObj(device.getConfiguration()).getStr("sipPassword"));
    verify(devices, never()).updateByPrimaryKeySelective(any(IoTDevice.class));
  }

  @Test
  void functionUsesAuthorizedDeviceIdentityInsteadOfBodyIdentity() {
    assertTrue(
        service
            .iotFunctionsDown(
                "pk",
                "{\"productKey\":\"other-product\",\"deviceId\":\"1001\","
                    + "\"tenantId\":\"other-tenant\",\"iotId\":\"other-device\",\"cmd\":\"DEV_FUNCTION\","
                    + "\"function\":{\"function\":\"queryStatus\"}}")
            .isSuccess());
    UnifiedDownlinkCommand command = command();
    assertEquals("pk", command.getProductKey());
    assertEquals("pk1001", command.getIotId());
    assertEquals("tenant-a", command.getTenantId());
  }

  @Test
  void unauthorizedDeviceCannotReachProtocol() {
    device.setTenantId("tenant-b");
    assertThrows(
        IoTException.class,
        () -> service.iotFunctionsDown("pk", "{\"deviceId\":\"1001\",\"cmd\":\"DEV_FUNCTION\"}"));
    verifyNoInteractions(down);
  }

  @Test
  void singleDeleteForwardsTenant() {
    assertTrue(service.deleteDevInstanceById("1").isSuccess());
    assertEquals("tenant-a", command().getTenantId());
  }

  @Test
  void batchDeleteForwardsTenant() {
    assertTrue(service.deleteDevInstanceByIds(new String[] {"1"}).isSuccess());
    assertEquals("tenant-a", command().getTenantId());
  }

  private UnifiedDownlinkCommand command() {
    ArgumentCaptor<UnifiedDownlinkCommand> captor =
        ArgumentCaptor.forClass(UnifiedDownlinkCommand.class);
    verify(down).doAction(captor.capture());
    return captor.getValue();
  }
}
