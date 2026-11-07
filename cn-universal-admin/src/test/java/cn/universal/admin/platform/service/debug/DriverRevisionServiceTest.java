package cn.universal.admin.platform.service.debug;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.entity.IoTProtocolRevision;
import cn.universal.persistence.mapper.IoTProtocolRevisionMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DriverRevisionServiceTest {
  @Test
  void firstSaveKeepsOriginalAndNewSourceAndRejectsStaleHash() {
    IoTProtocolRevisionMapper mapper = mock(IoTProtocolRevisionMapper.class);
    when(mapper.list("driver")).thenReturn(List.of());
    DriverRevisionService service = new DriverRevisionService(mapper);
    IoTDeviceProtocol protocol =
        IoTDeviceProtocol.builder()
            .id("driver")
            .version("1.0")
            .configuration("{\"location\":\"before\"}")
            .build();
    service.checkExpected(protocol, DriverRevisionService.hash("before"));
    assertThrows(
        IoTException.class,
        () -> service.checkExpected(protocol, DriverRevisionService.hash("stale")));
    service.recordChange(protocol, "after", "1.1", "user");
    ArgumentCaptor<IoTProtocolRevision> revisions =
        ArgumentCaptor.forClass(IoTProtocolRevision.class);
    verify(mapper, times(2)).insert(revisions.capture());
    assertEquals(
        List.of("before", "after"),
        revisions.getAllValues().stream().map(IoTProtocolRevision::getSource).toList());
  }

  @Test
  void unchangedSourceDoesNotCreateDuplicateAndRetentionRemovesOnlyOldest() {
    IoTProtocolRevisionMapper mapper = mock(IoTProtocolRevisionMapper.class);
    List<IoTProtocolRevision> history = new ArrayList<>();
    for (int i = 0; i < 31; i++) {
      IoTProtocolRevision row = new IoTProtocolRevision();
      row.setId("rev-" + i);
      history.add(row);
    }
    when(mapper.list("driver")).thenReturn(history);
    DriverRevisionService service = new DriverRevisionService(mapper);
    service.recordChange(
        IoTDeviceProtocol.builder().id("driver").configuration("{\"location\":\"same\"}").build(),
        "same",
        "1",
        "user");
    verify(mapper, never()).insert(any());
    verify(mapper).delete("rev-30", "driver");
  }
}
