package cn.universal.dm.device.service.ota.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.dm.device.service.ota.support.OtaReportExtractor;
import cn.universal.persistence.entity.IoTOtaTask;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.mapper.*;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class IoTOtaRuntimeTimeoutTest {
  @Test void timeoutCandidateIsRecheckedAfterLockAndRecentAckPreventsTimeout() {
    verifyRecentActivityProtectsRetry(false);
  }

  @Test void recentRedispatchPreventsOldReportTimestampFromTimingOutRetry() {
    verifyRecentActivityProtectsRetry(true);
  }

  private void verifyRecentActivityProtectsRetry(boolean redispatched) {
    IoTOtaTaskMapper tasks = mock(IoTOtaTaskMapper.class);
    IoTOtaTaskDeviceMapper devices = mock(IoTOtaTaskDeviceMapper.class);
    OtaTransferService transfer = mock(OtaTransferService.class);
    IoTOtaRuntimeServiceImpl runtime = new IoTOtaRuntimeServiceImpl(tasks, devices,
        mock(IoTOtaPackageMapper.class), mock(IoTOtaDeviceVersionMapper.class),
        mock(IoTProductMapper.class), mock(IoTDeviceMapper.class), new OtaReportExtractor(), transfer);
    IoTOtaTask task = IoTOtaTask.builder().id(2L).status(OtaConstants.TaskStatus.RUNNING)
        .strategyJson("{\"timeoutSeconds\":300}").build();
    IoTOtaTaskDevice stale = IoTOtaTaskDevice.builder().id(1L).taskId(2L)
        .status(OtaConstants.DeviceStatus.DOWNLOADING).lastReportTime(new Date(0)).build();
    IoTOtaTaskDevice fresh = IoTOtaTaskDevice.builder().id(1L).taskId(2L)
        .status(OtaConstants.DeviceStatus.DOWNLOADING)
        .lastReportTime(redispatched ? new Date(0) : new Date())
        .pushedTime(redispatched ? new Date() : new Date(0)).build();
    when(tasks.selectTaskForUpdate(2L)).thenReturn(task);
    when(devices.selectTimedOutActiveByTaskId(2L, 300)).thenReturn(List.of(stale));
    when(devices.selectTaskDeviceForUpdate(1L)).thenReturn(fresh);
    when(devices.selectTaskDeviceById(1L)).thenReturn(fresh);
    when(devices.selectByTaskIdForUpdate(2L)).thenReturn(List.of(fresh));

    runtime.processTaskTimeouts(2L);

    var order = inOrder(tasks, devices);
    order.verify(tasks).selectTaskForUpdate(2L);
    order.verify(devices).selectTimedOutActiveByTaskId(2L, 300);
    order.verify(tasks).selectTaskForUpdate(2L);
    order.verify(devices).selectTaskDeviceForUpdate(1L);
    verify(devices, never()).updateTaskDeviceIfActive(any());
  }
}
