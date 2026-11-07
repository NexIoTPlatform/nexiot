package cn.universal.http.access.down;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.core.message.DownRequest;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.service.IoTDeviceAreaService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HttpDeviceLifecycleServiceTest {
  private final IoTDeviceMapper devices = mock(IoTDeviceMapper.class);
  private final IoTDeviceService deviceService = mock(IoTDeviceService.class);
  private final IoTDeviceAreaService areas = mock(IoTDeviceAreaService.class);
  private final IoTDeviceLifeCycle lifecycle = mock(IoTDeviceLifeCycle.class);
  private final TestLifecycleService service = new TestLifecycleService();

  @Test
  void newDeviceIsInsertedWithScopeBeforeAfterCreateCallback() {
    HttpDownRequest request = request(DownCmd.DEV_ADD);
    when(devices.insertUseGeneratedKeys(any())).thenReturn(1);

    assertTrue(service.process(request).isSuccess());

    var stored = ArgumentCaptor.forClass(IoTDevice.class);
    var order = inOrder(devices, lifecycle);
    order.verify(devices).selectCount(any(IoTDevice.class));
    order.verify(devices).insertUseGeneratedKeys(stored.capture());
    order.verify(lifecycle).create("product-1", "device-1", request);
    assertEquals(1, service.beforeCalls);
    assertEquals("tenant-a", stored.getValue().getTenantId());
    assertEquals(5L, stored.getValue().getDeptId());
    assertEquals("creator-a", stored.getValue().getCreatorId());
    assertEquals("HTTP meter", stored.getValue().getDeviceName());
    assertEquals("product-1device-1", stored.getValue().getIotId());
    assertTrue(stored.getValue().getDeviceSecret().length() > 0);
    verifyNoInteractions(deviceService);
  }

  @Test
  void sharedPermissionOrValidationRejectionPreventsPersistence() {
    service.beforeResult = R.error("permission denied");

    assertSame(service.beforeResult, service.process(request(DownCmd.DEV_ADD)));

    verifyNoInteractions(devices, deviceService, lifecycle);
  }

  @Test
  void duplicateDeviceDoesNotInsertOrPublishCreate() {
    when(devices.selectCount(any(IoTDevice.class))).thenReturn(1);

    assertTrue(service.process(request(DownCmd.DEV_ADD)).isError());

    verify(devices, never()).insertUseGeneratedKeys(any());
    verifyNoInteractions(lifecycle);
  }

  @Test
  void updateUsesSharedWrapperAndAfterUpdateWithoutReassigningTenant() {
    HttpDownRequest request = request(DownCmd.DEV_UPDATE);
    IoTDevice existing = IoTDevice.builder().id(7L).productKey("product-1")
        .deviceId("device-1").iotId("product-1device-1").tenantId("tenant-a").build();
    when(devices.selectOne(any(IoTDevice.class))).thenReturn(existing);
    when(devices.updateByPrimaryKey(existing)).thenReturn(1);

    assertTrue(service.process(request).isSuccess());

    assertEquals(1, service.beforeCalls);
    assertEquals("HTTP meter", existing.getDeviceName());
    assertEquals("tenant-a", existing.getTenantId());
    var order = inOrder(devices, lifecycle);
    order.verify(devices).selectOne(any(IoTDevice.class));
    order.verify(devices).updateByPrimaryKey(existing);
    order.verify(lifecycle).update("product-1", "device-1", request);
  }

  @Test
  void deleteUsesCommonCleanupAndOnlyThenPublishesDelete() {
    HttpDownRequest request = request(DownCmd.DEV_DEL);
    IoTDevice existing = IoTDevice.builder().productKey("product-1").deviceId("device-1")
        .iotId("product-1device-1").build();
    IoTDeviceDTO snapshot = new IoTDeviceDTO();
    when(devices.selectOne(any(IoTDevice.class))).thenReturn(existing);
    when(deviceService.selectDevInstanceBO("product-1", "device-1")).thenReturn(snapshot);
    when(deviceService.delDevInstance(existing.getIotId())).thenReturn(1);

    assertTrue(service.process(request).isSuccess());

    assertEquals(1, service.beforeCalls);
    var order = inOrder(deviceService, lifecycle);
    order.verify(deviceService).selectDevInstanceBO("product-1", "device-1");
    order.verify(deviceService).delDevInstance(existing.getIotId());
    order.verify(lifecycle).delete(snapshot, request);
  }

  private HttpDownRequest request(DownCmd cmd) {
    HttpDownRequest request = new HttpDownRequest();
    request.setCmd(cmd);
    request.setProductKey("product-1");
    request.setDeviceId("device-1");
    request.setAppUnionId("creator-a");
    request.setTenantId("tenant-a");
    request.setDeptId(5L);
    request.setProduct(IoTProduct.builder().productKey("product-1").thirdPlatform("http")
        .tenantId("tenant-a").name("HTTP product").deviceNode("DEVICE").build());
    request.setData(new JSONObject(Map.of("deviceName", "HTTP meter")));
    return request;
  }

  private class TestLifecycleService extends HttpDeviceLifecycleService {
    private R<?> beforeResult;
    private int beforeCalls;

    TestLifecycleService() {
      super(HttpDeviceLifecycleServiceTest.this.devices,
          HttpDeviceLifecycleServiceTest.this.deviceService,
          HttpDeviceLifecycleServiceTest.this.areas,
          HttpDeviceLifecycleServiceTest.this.lifecycle);
    }

    @Override
    protected R beforeDownAction(IoTProduct product, Object data, DownRequest request) {
      beforeCalls++;
      return beforeResult;
    }
  }
}
