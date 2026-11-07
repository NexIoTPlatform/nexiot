package cn.universal.dm.device.service.log.writer.mq;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.dm.device.service.log.DefaultIoTDeviceLogService;
import cn.universal.dm.device.service.log.writer.DeviceLogWriteRecord;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class DeviceLogMqMessageHandlerTest {

  @Test
  void shouldPassPollRecordsToStorageAsOneBatchWhenDedupIsDisabled() {
    CapturingLogService logService = new CapturingLogService();
    ObjectProvider<DefaultIoTDeviceLogService> logServiceProvider =
        new ObjectProvider<>() {
          @Override
          public DefaultIoTDeviceLogService getObject() {
            return logService;
          }

          @Override
          public DefaultIoTDeviceLogService getObject(Object... args) {
            return logService;
          }
        };
    DeviceLogMqMessageHandler handler =
        new DeviceLogMqMessageHandler(
            logServiceProvider,
            null,
            false,
            "test:",
            300,
            7);

    handler.handleBatch(List.of(record("event-1"), record("event-2")));

    assertEquals(List.of("event-1", "event-2"), logService.eventIds);
    assertEquals(1, logService.batchCount);
  }

  private DeviceLogWriteRecord record(String eventId) {
    return new DeviceLogWriteRecord(1, eventId, "mysql", "device-1", "{}", null, "{}", "{}", 1L);
  }

  private static final class CapturingLogService extends DefaultIoTDeviceLogService {
    private final List<String> eventIds = new ArrayList<>();
    private int batchCount;

    @Override
    public void persistMqRecords(List<DeviceLogWriteRecord> records) {
      batchCount++;
      records.forEach(record -> eventIds.add(record.eventId()));
    }
  }
}
