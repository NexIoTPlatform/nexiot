package cn.universal.admin.platform.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.platform.service.impl.IoTOtaAdminServiceImpl;
import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTOtaPackage;
import cn.universal.persistence.entity.IoTOtaTask;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTOtaDeviceVersionMapper;
import cn.universal.persistence.mapper.IoTOtaPackageMapper;
import cn.universal.persistence.mapper.IoTOtaTaskDeviceMapper;
import cn.universal.persistence.mapper.IoTOtaTaskMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IoTOtaProfileTest {

  @Mock IoTProductMapper mapper;
  @Mock IoTOtaPackageMapper packageMapper;
  @Mock IoTOtaTaskMapper taskMapper;
  @Mock IoTOtaTaskDeviceMapper taskDeviceMapper;
  @Mock IoTOtaDeviceVersionMapper deviceVersionMapper;
  @Mock IoTUserService userService;
  @Mock IIoTDeviceService deviceService;
  @Mock IoTOtaRuntimeService runtimeService;
  @InjectMocks IoTOtaAdminServiceImpl service;
  private IoTProduct product;

  @BeforeEach void setUp() {
    product = new IoTProduct();
    product.setId(12L);
    product.setProductKey("demo-product");
    product.setConfiguration("{\"tcpPort\":12345,\"privateOption\":{\"enabled\":true}}");
    when(mapper.getProductByProductKey("demo-product")).thenReturn(product);
  }

  @Test void legacyProductUsesUrlWithoutRewritingConfiguration() {
    assertEquals("URL", service.getTransferProfile("demo-product").get("mode"));
    verify(mapper, never()).updateDevProduct(any());
  }

  @Test void savesOnlyProfileAndPreservesConnectionConfiguration() {
    when(mapper.selectProductForUpdate("demo-product")).thenReturn(product);
    when(mapper.updateDevProduct(any())).thenReturn(1);
    Map<String, Object> result = service.saveTransferProfile("demo-product", Map.of("mode", "PUSH", "chunkSize", 512));
    assertEquals("PUSH", result.get("mode"));
    ArgumentCaptor<IoTProduct> update = ArgumentCaptor.forClass(IoTProduct.class);
    verify(mapper).updateDevProduct(update.capture());
    assertEquals(12L, update.getValue().getId());
    JSONObject configuration = JSONUtil.parseObj(update.getValue().getConfiguration());
    assertEquals(12345, configuration.getInt("tcpPort"));
    assertTrue(configuration.getJSONObject("privateOption").getBool("enabled"));
    assertEquals(512, configuration.getJSONObject("otaTransfer").getInt("chunkSize"));
  }

  @Test void invalidProfileCannotBeSaved() {
    when(mapper.selectProductForUpdate("demo-product")).thenReturn(product);
    assertThrows(RuntimeException.class,
        () -> service.saveTransferProfile("demo-product", Map.of("mode", "PUSH", "chunkSize", 0)));
    verify(mapper, never()).updateDevProduct(any());
  }

  @Test void malformedConfigurationIsNeverSilentlyOverwritten() {
    product.setConfiguration("broken-json");
    when(mapper.selectProductForUpdate("demo-product")).thenReturn(product);
    assertThrows(RuntimeException.class,
        () -> service.saveTransferProfile("demo-product", Map.of("mode", "URL")));
    verify(mapper, never()).updateDevProduct(any());
  }

  @Test void anotherTenantCannotReadOrChangeManufacturerProfile() {
    product.setTenantId("tenant-b");
    IoTUser user = new IoTUser();
    user.setUnionId("operator-a");
    user.setTenantId("tenant-a");
    user.setIdentity(1);
    when(userService.selectUserByUnionId("operator-a")).thenReturn(user);
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("operator-a");
      assertThrows(RuntimeException.class, () -> service.getTransferProfile("demo-product"));
      assertThrows(RuntimeException.class,
          () -> service.saveTransferProfile("demo-product", Map.of("mode", "PUSH")));
    }
    verify(mapper, never()).selectProductForUpdate(any());
    verify(mapper, never()).updateDevProduct(any());
  }

  @Test void taskSnapshotsTrustedProductProfileAndIgnoresClientOverride() {
    product.setTenantId("tenant-a");
    product.setConfiguration("{\"otaTransfer\":{\"mode\":\"PULL\",\"chunkSize\":512}}");
    IoTOtaPackage firmware = new IoTOtaPackage();
    firmware.setId(31L);
    firmware.setProductKey("demo-product");
    firmware.setPackageName("demo");
    firmware.setVersion("2.0");
    firmware.setStatus("PUBLISHED");
    firmware.setFileUrl("https://storage.example/firmware.bin");
    firmware.setFileSize(2048L);
    firmware.setChecksumAlgorithm("SHA256");
    firmware.setChecksum("ab".repeat(32));
    when(packageMapper.selectPackageById(31L)).thenReturn(firmware);
    when(deviceService.selectDevInstanceList(any(), isNull()))
        .thenReturn(List.of(IoTDevice.builder().deviceId("device-1").iotId("iot-1").build()));
    doAnswer(invocation -> {
      ((IoTOtaTask) invocation.getArgument(0)).setId(71L);
      return 1;
    }).when(taskMapper).insertTask(any());
    IoTOtaTask task = new IoTOtaTask();
    task.setPackageId(31L);
    task.setDeviceIds(List.of("device-1"));
    task.setStrategyJson("{\"transferProfile\":{\"mode\":\"URL\"}}");

    assertEquals(71L, service.createTask(task, "operator-a"));
    product.setConfiguration("{\"otaTransfer\":{\"mode\":\"PUSH\",\"chunkSize\":1024}}");
    JSONObject snapshot = JSONUtil.parseObj(task.getStrategyJson()).getJSONObject("transferProfile");
    assertEquals("PULL", snapshot.getStr("mode"));
    assertEquals(512, snapshot.getInt("chunkSize"));
    verify(taskDeviceMapper).insertTaskDeviceBatch(argThat(devices -> devices.size() == 1
        && devices.get(0).getTaskId().equals(71L)));
    verify(runtimeService, never()).dispatchTask(any());
  }
}
