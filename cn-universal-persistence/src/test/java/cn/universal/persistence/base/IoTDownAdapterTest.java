package cn.universal.persistence.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.core.message.DownRequest;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import org.junit.jupiter.api.Test;

class IoTDownAdapterTest {

  @Test
  void activeRegistrationShouldUseProductTenantFirst() {
    IoTDevice device = new IoTDevice();
    IoTProduct product = IoTProduct.builder().tenantId("product-tenant").build();
    DownRequest request = new DownRequest();
    request.setAllowInsert(true);
    request.setTenantId("request-tenant");
    request.setDeptId(10L);

    IoTDownAdapter.finalDown_Add(device, product, request);

    assertEquals("product-tenant", device.getTenantId());
    assertEquals(10L, device.getDeptId());
  }

  @Test
  void activeRegistrationShouldFallbackToRequestTenant() {
    IoTDevice device = new IoTDevice();
    IoTProduct product = IoTProduct.builder().build();
    DownRequest request = new DownRequest();
    request.setAllowInsert(true);
    request.setTenantId("request-tenant");

    IoTDownAdapter.finalDown_Add(device, product, request);

    assertEquals("request-tenant", device.getTenantId());
  }

  @Test
  void manualAddShouldUseRequestTenant() {
    IoTDevice device = new IoTDevice();
    IoTProduct product = IoTProduct.builder().tenantId("product-tenant").build();
    DownRequest request = new DownRequest();
    request.setTenantId("request-tenant");
    request.setDeptId(20L);

    IoTDownAdapter.finalDown_Add(device, product, request);

    assertEquals("request-tenant", device.getTenantId());
    assertEquals(20L, device.getDeptId());
  }
}
