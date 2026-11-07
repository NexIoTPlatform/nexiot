package cn.universal.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.universal.common.exception.IoTException;
import org.junit.jupiter.api.Test;

class BaseControllerTest {

  @Test
  void rejectsSuperAdminCreate() {
    TestController controller = new TestController(true);

    IoTException error =
        assertThrows(IoTException.class, () -> controller.checkCreate("产品"));

    assertEquals("超级管理员不允许创建产品", error.getMessage());
  }

  @Test
  void allowsTenantUserCreate() {
    TestController controller = new TestController(false);

    assertDoesNotThrow(() -> controller.checkCreate("产品"));
  }

  private static class TestController extends BaseController {

    private final boolean admin;

    private TestController(boolean admin) {
      this.admin = admin;
    }

    @Override
    public boolean isAdmin() {
      return admin;
    }

    private void checkCreate(String resourceName) {
      rejectSuperAdminCreate(resourceName);
    }
  }
}
