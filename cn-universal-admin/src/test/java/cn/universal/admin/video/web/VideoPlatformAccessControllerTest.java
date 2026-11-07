package cn.universal.admin.video.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class VideoPlatformAccessControllerTest {
  private final VideoPlatformInstanceService instances = mock(VideoPlatformInstanceService.class);
  private final TestController controller = new TestController(instances);

  private static class TestController extends VideoPlatformAccessController {
    IoTUser user;
    TestController(VideoPlatformInstanceService instances) {
      super(instances, new ObjectMapper());
      user = new IoTUser();
      user.setUnionId("member");
      user.setTenantId("tenant-a");
    }
    @Override protected IoTUser currentTenantUser() { return user; }
    @Override protected String resolveTenantId(String creatorId) { return "tenant-a"; }
  }

  private VideoPlatformInstance instance(String type, String auth) {
    VideoPlatformInstance instance = VideoPlatformInstance.builder()
        .instanceKey("camera").platformType(type).tenantId("tenant-a").creatorId("owner")
        .auth(auth).options("{}").build();
    when(instances.getByInstanceKey("camera")).thenReturn(instance);
    return instance;
  }

  @Test void onlyReturnsRegistrationKeyAndDisablesCaching() {
    instance("hik_isup", "{\"isupKey\":\"DeviceKey123\",\"gatewayToken\":\"private-gateway\",\"callbackToken\":\"private-callback\"}");
    var response = controller.accessCredentials("camera");
    assertEquals(Map.of("isupKey", "DeviceKey123"), response.getBody().getData());
    assertEquals("no-store", response.getHeaders().getCacheControl());
  }

  @Test void supportsLegacyIsupKeyWithoutReturningOtherOptions() {
    var instance = instance("hik_isup", "{}");
    instance.setOptions("{\"isupKey\":\"LegacyKey123\",\"gatewayToken\":\"private\"}");
    assertEquals(Map.of("isupKey", "LegacyKey123"), controller.accessCredentials("camera").getBody().getData());
  }

  @Test void dahuaReturnsOnlyActiveRegistrationDeviceCredentials() {
    instance("dahua_sdk", "{\"activeRegistrationUsername\":\"admin\",\"activeRegistrationPassword\":\"device-password\",\"password\":\"gateway-password\",\"gatewayToken\":\"private\"}");
    assertEquals(Map.of("username", "admin", "password", "device-password"),
        controller.accessCredentials("camera").getBody().getData());
  }

  @Test void rejectsOtherTenantsAndUnauthenticatedRequests() {
    var instance = instance("hik_isup", "{\"isupKey\":\"DeviceKey123\"}");
    instance.setTenantId("tenant-b");
    assertThrows(BaseException.class, () -> controller.accessCredentials("camera"));
    controller.user = null;
    assertThrows(BaseException.class, () -> controller.accessCredentials("camera"));
  }

  @Test void legacyInstancesUseTheirCreatorsTenant() {
    var instance = instance("hik_isup", "{}");
    instance.setTenantId(null);
    assertEquals(Map.of("isupKey", ""), controller.accessCredentials("camera").getBody().getData());
    controller.user.setTenantId("tenant-b");
    assertThrows(BaseException.class, () -> controller.accessCredentials("camera"));
  }

  @Test void rejectsUnsupportedPlatformsAndRequiresEditPermission() throws Exception {
    instance("hik_sdk", "{}");
    assertThrows(BaseException.class, () -> controller.accessCredentials("camera"));
    var authorization = VideoPlatformAccessController.class
        .getMethod("accessCredentials", String.class).getAnnotation(PreAuthorize.class);
    assertEquals("@ss.hasPermi('video:platform:edit')", authorization.value());
  }
}
