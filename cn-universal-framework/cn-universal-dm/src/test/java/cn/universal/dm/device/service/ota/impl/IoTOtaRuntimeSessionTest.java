package cn.universal.dm.device.service.ota.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.dm.device.service.ota.support.OtaReportExtractor;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.mapper.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IoTOtaRuntimeSessionTest {
  IoTOtaTaskMapper tasks = mock(IoTOtaTaskMapper.class);
  IoTOtaTaskDeviceMapper devices = mock(IoTOtaTaskDeviceMapper.class);
  OtaTransferService transfer = mock(OtaTransferService.class);
  IoTOtaRuntimeServiceImpl runtime = new IoTOtaRuntimeServiceImpl(tasks, devices,
      mock(IoTOtaPackageMapper.class), mock(IoTOtaDeviceVersionMapper.class),
      mock(IoTProductMapper.class), mock(IoTDeviceMapper.class), new OtaReportExtractor(), transfer);

  @Test void sessionRoutesToCorrectModuleInsteadOfMostRecentActiveTask() {
    IoTOtaTaskDevice another = device(1, 10, "session-a");
    IoTOtaTaskDevice target = device(2, 20, "session-b");
    when(devices.selectActiveCandidatesByDevice("product", "device")).thenReturn(List.of(another, target));
    when(devices.selectTaskDeviceForUpdate(2L)).thenReturn(target);
    when(devices.selectTaskDeviceById(2L)).thenReturn(target);
    when(transfer.matchesSession(eq(target), anyMap())).thenReturn(true);
    runtime.handleUpgradeProgress(Map.of("productKey", "product", "deviceId", "device",
        "sessionId", "session-b", "progress", 40));

    verify(tasks).selectTaskForUpdate(20L);
    verify(tasks, never()).selectTaskForUpdate(10L);
    ArgumentCaptor<IoTOtaTaskDevice> saved = ArgumentCaptor.forClass(IoTOtaTaskDevice.class);
    verify(devices).updateTaskDeviceIfActive(saved.capture());
    assertEquals(2L, saved.getValue().getId());
  }

  @Test void explicitOldSessionCannotConfirmNewAttemptByFirmwareVersion() {
    IoTOtaTaskDevice target = device(2, 20, "current-session");
    when(devices.selectTaskDeviceById(2L)).thenReturn(target);
    when(devices.selectTaskDeviceForUpdate(2L)).thenReturn(target);
    when(transfer.matchesSession(eq(target), anyMap())).thenReturn(false);
    assertEquals(OtaServiceResult.PROTOCOL_ERROR, runtime.handleFirmwareReport(Map.of(
        "productKey", "product", "deviceId", "device", "taskDeviceId", 2L,
        "sessionId", "old-session", "currentVersion", "2.0")));
    verify(devices, never()).updateTaskDeviceIfActive(any());
  }

  private IoTOtaTaskDevice device(long id, long taskId, String sessionId) {
    return IoTOtaTaskDevice.builder().id(id).taskId(taskId).productKey("product").deviceId("device")
        .targetVersion("2.0").status(OtaConstants.DeviceStatus.DOWNLOADING)
        .transferState("{\"sessionId\":\"" + sessionId + "\",\"phase\":\"CHUNK\"}").build();
  }
}
