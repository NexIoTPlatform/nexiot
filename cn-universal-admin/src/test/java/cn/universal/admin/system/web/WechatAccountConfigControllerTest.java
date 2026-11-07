package cn.universal.admin.system.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.security.wechat.WechatAccountConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WechatAccountConfigControllerTest {
  @Test
  void dedicatedRequestAcceptsSecretsAndKeepsThemOutOfStringRepresentation() throws Exception {
    WechatAccountConfigService service = mock(WechatAccountConfigService.class);
    when(service.safeConfig("tenant-a")).thenReturn(Map.of("appSecretConfigured", true));
    WechatAccountConfigController controller = controller(service, user(1, "TENANT_ADMIN"));
    WechatAccountConfigController.ConfigRequest request = new ObjectMapper().readValue("""
        {"accountName":"account","appId":"wx0000000000000000","appSecret":"private-secret",
         "callbackToken":"private-token","encodingAESKey":"private-aes","enabled":true}
        """, WechatAccountConfigController.ConfigRequest.class);
    controller.save(null, request);
    ArgumentCaptor<WechatLoginConfig> captured = ArgumentCaptor.forClass(WechatLoginConfig.class);
    verify(service).save(eq("tenant-a"), captured.capture(), eq(false));
    assertEquals("private-secret", captured.getValue().getAppSecret());
    assertEquals("private-token", captured.getValue().getCallbackToken());
    assertEquals("private-aes", captured.getValue().getEncodingAESKey());
    assertFalse(request.toString().contains("private-"));
  }

  @Test
  void tenantAdministratorCannotManageAnotherTenantAndOrdinaryUsersCannotManageConfig() {
    WechatAccountConfigService service = mock(WechatAccountConfigService.class);
    WechatAccountConfigController admin = controller(service, user(1, "ADMIN"));
    assertThrows(IoTException.class, () -> admin.config("tenant-b"));
    WechatAccountConfigController ordinary = controller(service, user(1, "USER"));
    assertThrows(IoTException.class, () -> ordinary.config(null));
    verifyNoInteractions(service);
  }

  @Test
  void platformAdministratorMustExplicitlySelectTheManagedTenant() {
    WechatAccountConfigService service = mock(WechatAccountConfigService.class);
    WechatAccountConfigController platform = controller(service, user(0, "PLATFORM_ADMIN"));
    assertThrows(IoTException.class, () -> platform.config(null));
    when(service.safeConfig("tenant-b")).thenReturn(Map.of());
    platform.config("tenant-b");
    verify(service).safeConfig("tenant-b");
  }

  private IoTUser user(int identity, String role) {
    return IoTUser.builder().identity(identity).tenantId("tenant-a").tenantRole(role)
        .status(0).deleted(0).build();
  }

  private WechatAccountConfigController controller(WechatAccountConfigService service, IoTUser user) {
    return new WechatAccountConfigController(service) {
      @Override protected IoTUser currentTenantUser() { return user; }
    };
  }
}
