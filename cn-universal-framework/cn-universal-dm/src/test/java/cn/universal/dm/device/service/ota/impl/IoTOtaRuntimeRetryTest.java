package cn.universal.dm.device.service.ota.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.dm.device.service.ota.support.OtaReportExtractor;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.mapper.*;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class IoTOtaRuntimeRetryTest {
  IoTOtaTaskMapper tasks = mock(IoTOtaTaskMapper.class);
  IoTOtaTaskDeviceMapper devices = mock(IoTOtaTaskDeviceMapper.class);
  IoTOtaRuntimeServiceImpl runtime = spy(new IoTOtaRuntimeServiceImpl(tasks, devices,
      mock(IoTOtaPackageMapper.class), mock(IoTOtaDeviceVersionMapper.class),
      mock(IoTProductMapper.class), mock(IoTDeviceMapper.class), new OtaReportExtractor(),
      mock(OtaTransferService.class)));

  @Test
  void clearsPreviousFailureInDatabaseBeforeRedispatch() {
    IoTOtaTaskDevice device = failedDevice();
    when(devices.selectTaskDeviceById(2L)).thenReturn(device);
    when(devices.selectTaskDeviceForUpdate(2L)).thenReturn(device);
    doReturn(OtaServiceResult.SUCCESS).when(runtime).dispatchTaskDevice(2L);

    assertEquals(OtaServiceResult.SUCCESS, runtime.retryTaskDevice(2L));

    InOrder order = inOrder(devices, runtime);
    order.verify(devices).updateTaskDevice(device);
    order.verify(devices).resetRetryFields(2L);
    order.verify(runtime).dispatchTaskDevice(2L);
    assertEquals(OtaConstants.DeviceStatus.WAITING_PUSH, device.getStatus());
    assertEquals(0, device.getProgress());
    assertEquals("", device.getTransferState());
    assertNull(device.getErrorCode());
    assertNull(device.getErrorMessage());
  }

  @Test
  void successfulDeviceCannotBeResetOrRetried() {
    IoTOtaTaskDevice device = failedDevice();
    device.setStatus(OtaConstants.DeviceStatus.SUCCESS);
    when(devices.selectTaskDeviceById(2L)).thenReturn(device);
    when(devices.selectTaskDeviceForUpdate(2L)).thenReturn(device);

    assertEquals(OtaServiceResult.UNSUPPORTED_OPERATION, runtime.retryTaskDevice(2L));
    verify(devices, never()).resetRetryFields(anyLong());
    verify(devices, never()).updateTaskDevice(any());
    verify(runtime, never()).dispatchTaskDevice(anyLong());
  }

  @Test
  void exhaustedRetryBudgetKeepsFailureEvidence() {
    IoTOtaTaskDevice device = failedDevice();
    device.setRetryCount(3);
    when(devices.selectTaskDeviceById(2L)).thenReturn(device);
    when(devices.selectTaskDeviceForUpdate(2L)).thenReturn(device);

    assertEquals(OtaServiceResult.FAILED, runtime.retryTaskDevice(2L));
    verify(devices, never()).resetRetryFields(anyLong());
    verify(devices, never()).updateTaskDevice(any());
    verify(runtime, never()).dispatchTaskDevice(anyLong());
    assertEquals("previous failure", device.getErrorMessage());
  }

  private IoTOtaTaskDevice failedDevice() {
    return IoTOtaTaskDevice.builder().id(2L).taskId(20L)
        .status(OtaConstants.DeviceStatus.FAILED).progress(60).retryCount(1).maxRetryCount(3)
        .errorCode("TIMEOUT").errorMessage("previous failure")
        .transferState("{\"sessionId\":\"old-session\"}").build();
  }
}
