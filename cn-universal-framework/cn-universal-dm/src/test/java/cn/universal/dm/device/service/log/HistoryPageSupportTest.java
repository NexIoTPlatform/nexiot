package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.query.LogQuery;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryPageSupportTest {
  record Row(LocalDateTime time, Long id) {}

  private LogQuery query() {
    LogQuery query =
        LogQuery.builder()
            .productKey("p")
            .iotId("i")
            .property("speed")
            .beginCreateTime(1700000000L)
            .endCreateTime(1700086400L)
            .pageSize(2)
            .build();
    query.setCursorMode(true);
    return query;
  }

  @Test
  void cursorPreservesMillisecondTiesAndDoesNotCount() {
    LogQuery query = query();
    HistoryPageSupport.prepare(query, true);
    LocalDateTime time = LocalDateTime.of(2023, 11, 15, 0, 0, 0, 123000000);
    var page =
        HistoryPageSupport.page(
            List.of(new Row(time, 9L), new Row(time, 8L), new Row(time, 7L)),
            query,
            Row::time,
            Row::id);
    assertEquals(-1, page.getTotalCount());
    assertEquals(2, page.getList().size());
    assertTrue(page.isHasMore());
    query.setCursor(page.getNextCursor());
    HistoryPageSupport.prepare(query, true);
    assertEquals(time, query.getBeforeTime());
    assertEquals(8L, query.getBeforeId());
  }

  @Test
  void cursorCannotCrossWindowsDevicesOrPageSizes() {
    LogQuery query = query();
    String cursor =
        HistoryPageSupport.page(
                List.of(
                    new Row(LocalDateTime.now(), 9L),
                    new Row(LocalDateTime.now(), 8L),
                    new Row(LocalDateTime.now(), 7L)),
                query,
                Row::time,
                Row::id)
            .getNextCursor();
    for (String change : List.of("time", "device", "page")) {
      LogQuery other = query();
      other.setCursor(cursor);
      if (change.equals("time")) other.setEndCreateTime(other.getEndCreateTime() + 1);
      if (change.equals("device")) other.setIotId("other");
      if (change.equals("page")) other.setPageSize(100);
      assertThrows(IoTException.class, () -> HistoryPageSupport.prepare(other, true));
    }
  }

  @Test
  void rejectsUnboundedFiltersAndDoesNotTrustClientSeekKeys() {
    LogQuery query = query();
    query.setBeforeTime(LocalDateTime.now());
    query.setBeforeId(1L);
    HistoryPageSupport.prepare(query, true);
    assertNull(query.getBeforeTime());
    assertNull(query.getBeforeId());
    query.setEndCreateTime(query.getBeginCreateTime() + 32 * 86400);
    assertThrows(IoTException.class, () -> HistoryPageSupport.prepare(query, true));
    query.setEndCreateTime(null);
    assertThrows(IoTException.class, () -> HistoryPageSupport.prepare(query, true));
  }

  @Test
  void finalPageHasNoNextCursor() {
    var page =
        HistoryPageSupport.page(
            List.of(new Row(LocalDateTime.now(), 1L)), query(), Row::time, Row::id);
    assertFalse(page.isHasMore());
    assertNull(page.getNextCursor());
  }

  @Test
  void missingIdAtCursorBoundaryFailsInsteadOfUsingSyntheticZero() {
    LogQuery query = query();
    LocalDateTime time = LocalDateTime.of(2023, 11, 15, 0, 0, 0, 123000000);
    assertThrows(
        IoTException.class,
        () ->
            HistoryPageSupport.page(
                List.of(
                    new Row(time, 3L), new Row(time, null), new Row(time, 1L)),
                query,
                Row::time,
                Row::id));
  }
}
