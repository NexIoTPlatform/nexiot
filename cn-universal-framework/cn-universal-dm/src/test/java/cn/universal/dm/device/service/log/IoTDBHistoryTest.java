package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.Session;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.RowRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IoTDBHistoryTest {
  private static final long BEGIN = 1700000000L;
  private static final List<String> TYPED_COLUMNS =
      List.of(
          "Time",
          "total_count",
          "typed_count",
          "valid_count",
          "numeric_count",
          "numeric_min",
          "numeric_max",
          "numeric_avg");

  private PropertyTrendQuery query() {
    PropertyTrendQuery q = new PropertyTrendQuery();
    q.setSamplingMode("exact");
    q.setProductKey("product");
    q.setDeviceId("device");
    q.setIotId("productdevice");
    q.setProperty("speed");
    q.setBeginCreateTime(BEGIN);
    q.setEndCreateTime(BEGIN + 86400);
    q.setPageSize(2);
    return q;
  }

  private IoTDBIoTDeviceLogService service(Session session) throws Exception {
    IoTDBIoTDeviceLogService service = new IoTDBIoTDeviceLogService();
    set(service, "session", session);
    set(service, "storageGroup", "root.device");
    return service;
  }

  private void set(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private RowRecord row(long timestamp, Number... values) {
    RowRecord record = new RowRecord(timestamp);
    for (Number value : values) {
      record.addField(value, value instanceof Double ? TSDataType.DOUBLE : TSDataType.INT64);
    }
    return record;
  }

  private SessionDataSet dataset(List<String> columns, RowRecord... records) throws Exception {
    SessionDataSet data = mock(SessionDataSet.class);
    when(data.getColumnNames()).thenReturn(columns);
    var iterator = List.of(records).iterator();
    when(data.hasNext()).thenAnswer(invocation -> iterator.hasNext());
    when(data.next()).thenAnswer(invocation -> iterator.next());
    return data;
  }

  @Test
  void nativeTypedWindowRetainsLargeCountsAndExtremesWithoutReadingRawRows() throws Exception {
    Session session = mock(Session.class);
    SessionDataSet data =
        dataset(
            TYPED_COLUMNS,
            row(BEGIN * 1000, 100000000L, 100000000L, 100000000D, 100000000L, -13D, 81D, 22.5D));
    when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(data);
    var trend = service(session).queryPropertyTrend(query());
    assertEquals(100000000, trend.getTotalCount());
    assertEquals(100000000, trend.getNumericCount());
    assertEquals(new BigDecimal("-13.0"), trend.getBuckets().getFirst().getMin());
    assertEquals(new BigDecimal("81.0"), trend.getBuckets().getFirst().getMax());
    assertEquals(new BigDecimal("22.5"), trend.getBuckets().getFirst().getAvg());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session)
        .executeQueryStatement(sql.capture(), longThat(timeout -> timeout > 0 && timeout <= 8000));
    assertTrue(sql.getValue().contains("GROUP BY ([1700000000000,1700086400001)"));
    assertFalse(sql.getValue().contains("OFFSET"));
    assertFalse(sql.getValue().contains("CAST"));
    verify(data).close();
  }

  @Test
  void oldTextSeriesUsesTwoBoundedDatabaseAggregatesAndPreservesInvalidCount() throws Exception {
    Session session = mock(Session.class);
    SessionDataSet counts = dataset(List.of("Time", "total_count"), row(BEGIN * 1000, 103L));
    SessionDataSet numeric =
        dataset(
            List.of("Time", "numeric_count", "numeric_min", "numeric_max", "numeric_avg"),
            row(BEGIN * 1000, 100L, 0D, 8D, 4D));
    when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(counts, numeric);
    var trend = service(session).queryPropertyTrend(query());
    assertEquals(103, trend.getTotalCount());
    assertEquals(100, trend.getNumericCount());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session, times(2)).executeQueryStatement(sql.capture(), anyLong());
    assertTrue(sql.getAllValues().get(1).contains("WHERE content REGEXP"));
    assertTrue(sql.getAllValues().stream().allMatch(value -> value.contains("GROUP BY")));
  }

  @Test
  void typedNonnumericOverwriteDoesNotReuseOldNumericCell() throws Exception {
    Session session = mock(Session.class);
    SessionDataSet nativeData =
        dataset(TYPED_COLUMNS, row(BEGIN * 1000, 4L, 4L, 3D, 4L, -99D, 10D, -21D));
    SessionDataSet filtered =
        dataset(
            List.of("Time", "numeric_count", "numeric_min", "numeric_max", "numeric_avg"),
            row(BEGIN * 1000, 3L, 1D, 10D, 5D));
    when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(nativeData, filtered);
    var trend = service(session).queryPropertyTrend(query());
    assertEquals(4, trend.getTotalCount());
    assertEquals(3, trend.getNumericCount());
    assertEquals(new BigDecimal("1.0"), trend.getBuckets().getFirst().getMin());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session, times(2)).executeQueryStatement(sql.capture(), anyLong());
    assertTrue(sql.getAllValues().get(1).contains("WHERE value_schema = 1"));
    assertFalse(sql.getAllValues().get(1).contains("CAST"));
  }

  @Test
  void overflowIsExplicitAndNaNInfinityAreNeverWrittenAsNumeric() throws Exception {
    for (String value : List.of("NaN", "Infinity", "-Infinity", "1e999", "bad", "")) {
      assertNull(IoTDBIoTDeviceLogService.finiteNumericValue(value));
    }
    assertEquals(0D, IoTDBIoTDeviceLogService.finiteNumericValue("0"));
    assertEquals(-0.0012D, IoTDBIoTDeviceLogService.finiteNumericValue("-1.2e-3"));
    Session session = mock(Session.class);
    SessionDataSet overflow =
        dataset(
            TYPED_COLUMNS,
            row(
                BEGIN * 1000,
                1L,
                1L,
                1D,
                1L,
                1D,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY));
    when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(overflow);
    assertThrows(IoTException.class, () -> service(session).queryPropertyTrend(query()));
  }

  @Test
  void queryFailureIsNotPresentedAsEmptyHistory() throws Exception {
    Session session = mock(Session.class);
    when(session.executeQueryStatement(anyString(), anyLong()))
        .thenThrow(new StatementExecutionException("timeout"));
    IoTDBIoTDeviceLogService service = service(session);
    assertThrows(IoTException.class, () -> service.queryPropertyTrend(query()));
    PropertyTrendQuery query = query();
    query.setCursorMode(true);
    assertThrows(IoTException.class, () -> service.queryLogMeta(query));
    assertThrows(IoTException.class, () -> service.pageList(query));
  }

  @Test
  void metadataCursorSeeksTimestampWithoutCountOffsetOrDeepPageAllocation() throws Exception {
    Session session = mock(Session.class);
    SessionDataSet data =
        dataset(
            List.of("Time"),
            row(BEGIN * 1000 + 300),
            row(BEGIN * 1000 + 200),
            row(BEGIN * 1000 + 100));
    when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(data);
    PropertyTrendQuery query = query();
    query.setCursorMode(true);
    query.setPageNum(1000000);
    query.setBeforeTime(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(BEGIN * 1000 + 400), ZoneId.systemDefault()));
    var page = service(session).queryLogMeta(query);
    assertEquals(2, page.getList().size());
    assertEquals(-1L, page.getTotalCount());
    assertTrue(page.isHasMore());
    assertNotNull(page.getNextCursor());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session).executeQueryStatement(sql.capture(), anyLong());
    assertTrue(sql.getValue().contains("time < 1700000000400"));
    assertTrue(sql.getValue().endsWith("LIMIT 3"));
    assertFalse(sql.getValue().contains("COUNT"));
    assertFalse(sql.getValue().contains("OFFSET"));
  }
}
