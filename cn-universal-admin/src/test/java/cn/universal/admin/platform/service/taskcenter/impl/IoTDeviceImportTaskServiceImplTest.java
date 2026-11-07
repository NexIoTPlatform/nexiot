package cn.universal.admin.platform.service.taskcenter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.IoTTaskCenterDispatchService;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.vo.IoTTaskCenterStatisticsVO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("设备导入任务测试")
class IoTDeviceImportTaskServiceImplTest {

  @Test
  @DisplayName("导入任务rawData应保留经纬度")
  void shouldKeepLongitudeAndLatitudeInRawData() throws Exception {
    IoTDevice device =
        IoTDevice.builder().deviceId("DL299NEXIOT").deviceName("导入设备").detail("备注").build();
    device.setLongitude("81.221842");
    device.setLatitude("33.358114");

    String rawData = buildDeviceRawData(device);
    JSONObject rawJson = JSONUtil.parseObj(rawData);
    IoTDevice restored = parseImportDeviceRawData(rawData);

    assertEquals("81.221842", rawJson.getStr("longitude"));
    assertEquals("33.358114", rawJson.getStr("latitude"));
    assertEquals("81.221842", restored.getLongitude());
    assertEquals("33.358114", restored.getLatitude());
  }

  @Test
  @DisplayName("开源版应拒绝历史工业模板任务，避免部分导入")
  void shouldRejectLegacyIndustrialTemplateTaskBeforeCreatingDevices() {
    DeviceImportRowTransactionService rowService = mock(DeviceImportRowTransactionService.class);
    DeviceImportTaskExecutor executor = new DeviceImportTaskExecutor(null, null, null, rowService);
    IoTTaskCenter taskCenter = new IoTTaskCenter();
    taskCenter.setTenantId("tenant-1");
    taskCenter.setRequestSnapshot(
        JSONUtil.toJsonStr(Map.of("productKey", "product-1", "industrialTemplateId", 9L)));

    BaseException failure = assertThrows(BaseException.class, () -> executor.execute(taskCenter));

    assertEquals("开源版本不支持工业模板导入", failure.getMessage());
    verify(rowService, never()).create(any());
  }

  @Test
  @DisplayName("开源版应在创建任务前拒绝工业模板参数")
  void shouldRejectIndustrialTemplateBeforeTaskCreation() {
    IIoTTaskCenterService taskCenterService = mock(IIoTTaskCenterService.class);
    IoTTaskCenterDispatchService dispatchService = mock(IoTTaskCenterDispatchService.class);
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    IoTDeviceImportTaskServiceImpl service =
        new IoTDeviceImportTaskServiceImpl(
            taskCenterService, dispatchService, productMapper, mock(IoTDeviceMapper.class));
    when(productMapper.getProductByProductKey("product-1"))
        .thenReturn(product("product-1", "tenant-1"));

    BaseException failure =
        assertThrows(
            BaseException.class,
            () ->
                service.submitImportTask(
                    List.of(importDevice()),
                    product("product-1", "tenant-1"),
                    "devices.xlsx",
                    null,
                    null,
                    creator("tenant-1"),
                    "tenant-1",
                    9L,
                    2));

    assertEquals("开源版本不支持工业模板导入", failure.getMessage());
    verify(taskCenterService, never()).createTask(any(), anyList());
    verify(dispatchService, never()).dispatch(any());
  }

  @Test
  @DisplayName("提交导入任务时应拒绝其他租户产品")
  void shouldRejectProductFromAnotherTenantBeforeTaskCreation() {
    IIoTTaskCenterService taskCenterService = mock(IIoTTaskCenterService.class);
    IoTTaskCenterDispatchService dispatchService = mock(IoTTaskCenterDispatchService.class);
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    IoTDeviceMapper deviceMapper = mock(IoTDeviceMapper.class);
    IoTDeviceImportTaskServiceImpl service =
        new IoTDeviceImportTaskServiceImpl(
            taskCenterService, dispatchService, productMapper, deviceMapper);
    IoTProduct requested = product("product-1", "tenant-1");
    when(productMapper.getProductByProductKey("product-1"))
        .thenReturn(product("product-1", "tenant-2"));

    assertThrows(
        BaseException.class,
        () ->
            service.submitImportTask(
                List.of(importDevice()),
                requested,
                "devices.xlsx",
                null,
                null,
                creator("tenant-1"),
                "tenant-1",
                null,
                null));

    verify(taskCenterService, never()).createTask(any(), anyList());
    verify(dispatchService, never()).dispatch(any());
  }

  @Test
  @DisplayName("提交导入任务时应在当前租户解析并冻结唯一网关")
  void shouldResolveAndFreezeGatewayWithinTenant() {
    IIoTTaskCenterService taskCenterService = mock(IIoTTaskCenterService.class);
    IoTTaskCenterDispatchService dispatchService = mock(IoTTaskCenterDispatchService.class);
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    IoTDeviceMapper deviceMapper = mock(IoTDeviceMapper.class);
    IoTDeviceImportTaskServiceImpl service =
        new IoTDeviceImportTaskServiceImpl(
            taskCenterService, dispatchService, productMapper, deviceMapper);
    when(productMapper.getProductByProductKey("product-1"))
        .thenReturn(product("product-1", "tenant-1"));
    when(productMapper.getProductByProductKey("gateway-product"))
        .thenReturn(product("gateway-product", "tenant-1"));
    when(deviceMapper.select(any(IoTDevice.class)))
        .thenReturn(
            List.of(
                IoTDevice.builder()
                    .tenantId("tenant-1")
                    .productKey("gateway-product")
                    .deviceId("gateway-1")
                    .build()));
    when(taskCenterService.createTask(any(), anyList())).thenReturn(101L);

    service.submitImportTask(
        List.of(importDevice()),
        product("product-1", "tenant-1"),
        "devices.xlsx",
        "gateway-1",
        null,
        creator("tenant-1"),
        "tenant-1",
        null,
        null);

    ArgumentCaptor<IoTDevice> gatewayQuery = ArgumentCaptor.forClass(IoTDevice.class);
    verify(deviceMapper).select(gatewayQuery.capture());
    assertEquals("tenant-1", gatewayQuery.getValue().getTenantId());
    assertEquals("gateway-1", gatewayQuery.getValue().getDeviceId());
    ArgumentCaptor<IoTTaskCenter> task = ArgumentCaptor.forClass(IoTTaskCenter.class);
    verify(taskCenterService).createTask(task.capture(), anyList());
    JSONObject snapshot = JSONUtil.parseObj(task.getValue().getRequestSnapshot());
    assertEquals("gateway-product", snapshot.getStr("gwProductKey"));
    assertEquals("tenant-1", task.getValue().getTenantId());
    assertFalse(snapshot.containsKey("industrialTemplateId"));
    assertFalse(snapshot.containsKey("industrialTemplateVersion"));
  }

  @Test
  @DisplayName("异步执行时应按任务租户和完整身份复核网关")
  void executorShouldRevalidateGatewayWithTaskTenantAndIdentity() {
    IIoTTaskCenterService taskCenterService = mock(IIoTTaskCenterService.class);
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    IoTDeviceMapper deviceMapper = mock(IoTDeviceMapper.class);
    DeviceImportTaskExecutor executor =
        new DeviceImportTaskExecutor(taskCenterService, productMapper, deviceMapper, null);
    when(productMapper.getProductByProductKey("product-1"))
        .thenReturn(product("product-1", "tenant-1"));
    when(productMapper.getProductByProductKey("gateway-product"))
        .thenReturn(product("gateway-product", "tenant-1"));
    when(deviceMapper.selectOne(any(IoTDevice.class)))
        .thenReturn(
            IoTDevice.builder()
                .tenantId("tenant-1")
                .productKey("gateway-product")
                .deviceId("gateway-1")
                .build());
    when(taskCenterService.selectPendingItems(10L)).thenReturn(List.of());
    when(taskCenterService.refreshTaskStatistics(10L)).thenReturn(new IoTTaskCenterStatisticsVO());
    IoTTaskCenter task = new IoTTaskCenter();
    task.setId(10L);
    task.setTenantId("tenant-1");
    task.setRequestSnapshot(
        JSONUtil.toJsonStr(
            Map.of(
                "productKey", "product-1",
                "gwDeviceId", "gateway-1",
                "gwProductKey", "gateway-product")));

    executor.execute(task);

    ArgumentCaptor<IoTDevice> gatewayQuery = ArgumentCaptor.forClass(IoTDevice.class);
    verify(deviceMapper).selectOne(gatewayQuery.capture());
    assertEquals("tenant-1", gatewayQuery.getValue().getTenantId());
    assertEquals("gateway-product", gatewayQuery.getValue().getProductKey());
    assertEquals("gateway-1", gatewayQuery.getValue().getDeviceId());
  }

  @Test
  @DisplayName("单行事务内应锁定网关并拒绝跨租户身份变化")
  void rowTransactionShouldLockAndRejectGatewayTenantChange() {
    IIoTDeviceService deviceService = mock(IIoTDeviceService.class);
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    IoTDeviceMapper deviceMapper = mock(IoTDeviceMapper.class);
    DeviceImportRowTransactionService rowService =
        new DeviceImportRowTransactionService(deviceService, productMapper, deviceMapper);
    when(productMapper.getProductByProductKey("product-1"))
        .thenReturn(product("product-1", "tenant-1"));
    when(deviceMapper.selectForUpdateByProductKeyAndDeviceId(
            "gateway-product", "gateway-1"))
        .thenReturn(
            IoTDevice.builder()
                .tenantId("tenant-2")
                .productKey("gateway-product")
                .deviceId("gateway-1")
                .build());
    IoTDeviceBO device = new IoTDeviceBO();
    device.setTenantId("tenant-1");
    device.setProductKey("product-1");
    device.setDeviceId("device-1");
    device.setGwProductKey("gateway-product");
    device.setGwDeviceId("gateway-1");

    assertThrows(BaseException.class, () -> rowService.create(device));

    verify(deviceMapper)
        .selectForUpdateByProductKeyAndDeviceId("gateway-product", "gateway-1");
    verify(deviceService, never()).insertDevInstance(any());
  }

  @Test
  @DisplayName("普通设备导入应在租户校验后调用设备创建")
  void rowTransactionShouldCreateGenericDeviceWithoutIndustrialHooks() {
    IIoTDeviceService deviceService = mock(IIoTDeviceService.class);
    IoTProductMapper productMapper = mock(IoTProductMapper.class);
    DeviceImportRowTransactionService rowService =
        new DeviceImportRowTransactionService(
            deviceService, productMapper, mock(IoTDeviceMapper.class));
    when(productMapper.getProductByProductKey("product-1"))
        .thenReturn(product("product-1", "tenant-1"));
    IoTDeviceBO device = new IoTDeviceBO();
    device.setTenantId("tenant-1");
    device.setProductKey("product-1");
    device.setDeviceId("device-1");
    when(deviceService.insertDevInstance(device)).thenReturn(R.ok());

    rowService.create(device);

    verify(deviceService).insertDevInstance(device);
  }

  private String buildDeviceRawData(IoTDevice device) throws Exception {
    IoTDeviceImportTaskServiceImpl service =
        new IoTDeviceImportTaskServiceImpl(null, null, null, null);
    Method method = IoTDeviceImportTaskServiceImpl.class.getDeclaredMethod("buildDeviceRawData", IoTDevice.class);
    method.setAccessible(true);
    return (String) method.invoke(service, device);
  }

  private IoTDevice parseImportDeviceRawData(String rawData) throws Exception {
    DeviceImportTaskExecutor executor = new DeviceImportTaskExecutor(null, null, null, null);
    Method method = DeviceImportTaskExecutor.class.getDeclaredMethod("parseImportDeviceRawData", String.class);
    method.setAccessible(true);
    return (IoTDevice) method.invoke(executor, rawData);
  }

  private IoTDevice importDevice() {
    return IoTDevice.builder().deviceId("device-1").deviceName("设备1").build();
  }

  private IoTProduct product(String productKey, String tenantId) {
    return IoTProduct.builder().productKey(productKey).name(productKey).tenantId(tenantId).build();
  }

  private IoTUser creator(String tenantId) {
    return IoTUser.builder()
        .unionId("operator-1")
        .username("operator")
        .tenantId(tenantId)
        .build();
  }
}
