package cn.universal.admin.platform.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.impl.IoTOtaAdminServiceImpl;
import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IoTOtaGrayTaskTest {
  @Mock IoTProductMapper products;
  @Mock IoTOtaPackageMapper packages;
  @Mock IoTOtaTaskMapper tasks;
  @Mock IoTOtaTaskDeviceMapper taskDevices;
  @Mock IoTOtaDeviceVersionMapper versions;
  @Mock IIoTDeviceService deviceService;
  @Mock IoTOtaRuntimeService runtime;
  @InjectMocks IoTOtaAdminServiceImpl service;

  @BeforeEach void setUp() {
    when(products.getProductByProductKey("product-a")).thenReturn(IoTProduct.builder()
        .id(12L).productKey("product-a").tenantId("tenant-a").build());
    when(packages.selectPackageById(31L)).thenReturn(IoTOtaPackage.builder()
        .id(31L).productKey("product-a").packageName("firmware").version("2.0")
        .status("PUBLISHED").fileUrl("https://storage.example/fw.bin").fileSize(2048L)
        .checksumAlgorithm("SHA256").checksum("ab".repeat(32)).build());
  }

  @Test void grayUsesProductCandidatesWithoutRequiringExplicitDeviceIds() {
    when(deviceService.selectDevInstanceList(any(), isNull())).thenReturn(List.of(
        device("device-c"), device("device-a"), device("device-b")));
    doAnswer(call -> { ((IoTOtaTask) call.getArgument(0)).setId(71L); return 1; })
        .when(tasks).insertTask(any());
    var task = task("GRAY");
    task.setGrayPercent(50);
    task.setGrayDeviceLimit(1);
    assertEquals(71L, service.createTask(task, "operator"));
    assertEquals("GRAY", task.getRolloutMode());
    assertEquals("tenant-a", task.getTenantId());
    assertEquals(3, JSONUtil.parseObj(task.getStrategyJson()).getInt("candidateDeviceCount"));
    assertEquals(1, JSONUtil.parseObj(task.getStrategyJson()).getInt("selectedDeviceCount"));
    verify(deviceService).selectDevInstanceList(argThat(query ->
        "product-a".equals(query.getProductKey())), isNull());
    verify(taskDevices).selectActiveConflictsByDeviceIds("product-a", "APPLICATION", List.of("device-a"));
    verify(taskDevices).insertTaskDeviceBatch(argThat(rows -> rows.size() == 1
        && "device-a".equals(rows.getFirst().getDeviceId())
        && "tenant-a".equals(rows.getFirst().getTenantId())));
  }

  @Test void allStillUsesProductCandidatesWithoutExplicitSelection() {
    when(deviceService.selectDevInstanceList(any(), isNull()))
        .thenReturn(List.of(device("device-a"), device("device-b")));
    doAnswer(call -> { ((IoTOtaTask) call.getArgument(0)).setId(72L); return 1; })
        .when(tasks).insertTask(any());
    assertEquals(72L, service.createTask(task("ALL"), "operator"));
    verify(taskDevices).insertTaskDeviceBatch(argThat(rows -> rows.size() == 2));
  }

  @Test void specifiedStillRequiresExplicitDeviceIds() {
    assertThrows(IoTException.class, () -> service.createTask(task("SPECIFIED"), "operator"));
    verify(deviceService, never()).selectDevInstanceList(any(), any());
    verify(tasks, never()).insertTask(any());
  }

  private IoTOtaTask task(String mode) {
    return IoTOtaTask.builder().packageId(31L).rolloutMode(mode).deviceIds(List.of()).build();
  }

  private IoTDevice device(String id) {
    return IoTDevice.builder().productKey("product-a").deviceId(id).iotId("iot-" + id).build();
  }
}
