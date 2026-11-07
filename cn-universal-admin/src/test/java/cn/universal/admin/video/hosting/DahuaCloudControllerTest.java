package cn.universal.admin.video.hosting;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import cn.universal.admin.platform.service.IIoTProductService;
import cn.universal.admin.system.permission.SysPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class DahuaCloudControllerTest {
  @Test
  void listCreateAndModelUpdateRequirePermissionsBeforeAccessingProducts() {
    var products = mock(IIoTProductService.class);
    var permissions = mock(SysPermissionService.class);
    var controller = new DahuaCloudController(products, null, null, null, permissions);
    assertThrows(AccessDeniedException.class, controller::products);
    assertThrows(
        AccessDeniedException.class,
        () ->
            controller.create(
                new DahuaCloudController.ProductRequest("video", "123", "456", "fixture-secret")));
    assertThrows(AccessDeniedException.class, () -> controller.syncMetadata("p"));
    verifyNoInteractions(products);
  }
}
