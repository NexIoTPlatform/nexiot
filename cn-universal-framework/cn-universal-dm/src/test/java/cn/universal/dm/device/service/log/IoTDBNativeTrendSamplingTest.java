package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.Session;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.RowRecord;
import org.apache.iotdb.tsfile.utils.Binary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IoTDBNativeTrendSamplingTest {
  private static final long BEGIN = 1700000000L;
  private static final List<String> COLUMNS =
      List.of("Time", "content", "original_time", "source_count");

  private PropertyTrendQuery query() {
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setProductKey("product");
    query.setDeviceId("device");
    query.setIotId("productdevice");
    query.setProperty("speed");
    query.setBeginCreateTime(BEGIN);
    query.setEndCreateTime(BEGIN + 100);
    query.setMaxPoints(20);
    query.setMaxScanRows(1000);
    return query;
  }

  private long step(PropertyTrendQuery query) {
    long interval = query.getIntervalSeconds() * 1000;
    return (interval + query.getSamplesPerWindow() - 1) / query.getSamplesPerWindow();
  }

  private int fineWindows(PropertyTrendQuery query) {
    long range = (query.getEndCreateTime() - query.getBeginCreateTime()) * 1000 + 1;
    return Math.toIntExact((range + step(query) - 1) / step(query));
  }

  private IoTDBIoTDeviceLogService service(Session session) throws Exception {
    IoTDBIoTDeviceLogService service = new IoTDBIoTDeviceLogService();
    set(service, "session", session);
    set(service, "storageGroup", "root.device");
    return service;
  }

  private void set(Object object, String name, Object value) throws Exception {
    Field field = object.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(object, value);
  }

  private RowRecord row(long groupTime, String content, Long originalTime, Long sourceCount) {
    RowRecord row = new RowRecord(groupTime);
    row.addField(
        content == null ? null : new Binary(content.getBytes(StandardCharsets.UTF_8)),
        TSDataType.TEXT);
    row.addField(originalTime, TSDataType.INT64);
    row.addField(sourceCount, TSDataType.INT64);
    return row;
  }

  private RowRecord sample(
      PropertyTrendQuery query, long originalTime, String content, long count) {
    long begin = query.getBeginCreateTime() * 1000;
    long groupTime = begin + (originalTime - begin) / step(query) * step(query);
    return row(groupTime, content, originalTime, count);
  }

  private SessionDataSet data(RowRecord... rows) throws Exception {
    SessionDataSet data = mock(SessionDataSet.class);
    when(data.getColumnNames()).thenReturn(COLUMNS);
    var iterator = List.of(rows).iterator();
    when(data.hasNext()).thenAnswer(invocation -> iterator.hasNext());
    when(data.next()).thenAnswer(invocation -> iterator.next());
    return data;
  }

  private PropertyTrendVO trend(PropertyTrendQuery query, SessionDataSet data) throws Exception {
    Session session = mock(Session.class);
    when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(data);
    return service(session).queryPropertyTrend(query);
  }

  @Test
  void sixHundredPointsUseOneNativeAggregateWithInclusiveEndAndReturnedSampleBudget()
      throws Exception {
    PropertyTrendQuery query = query();
    query.setEndCreateTime(BEGIN + 86400);
    query.setMaxPoints(600);
    query.setMaxScanRows(10000);
    Session session = mock(Session.class);
    SessionDataSet data =
        data(
            sample(query, query.getEndCreateTime() * 1000, "8", 1),
            sample(query, BEGIN * 1000, "0", 100000));
    when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(data);

    PropertyTrendVO trend = service(session).queryPropertyTrend(query);

    assertEquals(2, trend.getTotalCount());
    assertEquals(2, trend.getScannedCount());
    assertTrue(trend.isSampled());
    assertFalse(trend.isCountExact());
    assertTrue(fineWindows(query) <= query.getMaxScanRows());
    assertEquals(9601, fineWindows(query));
    assertEquals(query.getEndCreateTime() * 1000, trend.getBuckets().getLast().getTime());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session)
        .executeQueryStatement(sql.capture(), longThat(timeout -> timeout > 0 && timeout <= 8000));
    assertTrue(sql.getValue().contains("SUBSTRING(last_value(content),1,1025)"));
    assertTrue(sql.getValue().contains("max_time(content) AS original_time"));
    assertTrue(sql.getValue().contains("count(content) AS source_count"));
    assertTrue(sql.getValue().contains("GROUP BY ([1700000000000,1700086400001),9000ms)"));
    assertTrue(sql.getValue().endsWith("ORDER BY time DESC"));
    assertFalse(sql.getValue().contains("CAST"));
    assertFalse(sql.getValue().contains("last_value(SUBSTRING"));
    verify(data).close();
  }

  @Test
  void fineGroupCrossingMainWindowUsesOriginalSampleTime() throws Exception {
    PropertyTrendQuery query = query();
    long originalTime = BEGIN * 1000 + query.getIntervalSeconds() * 1000 + 1;
    RowRecord row = sample(query, originalTime, "7", 1);
    assertTrue(row.getTimestamp() < BEGIN * 1000 + query.getIntervalSeconds() * 1000);

    PropertyTrendVO trend = trend(query, data(row));

    assertEquals(
        BEGIN * 1000 + query.getIntervalSeconds() * 1000, trend.getBuckets().getFirst().getTime());
    assertEquals(new BigDecimal("7.0"), trend.getBuckets().getFirst().getAvg());
    assertFalse(trend.isSampled());
    assertTrue(trend.isCountExact());
  }

  @Test
  void sparseOneValueGroupsKeepExactCountsAndRejectNonnumericValuesWithoutTurningThemIntoZero()
      throws Exception {
    PropertyTrendQuery query = query();
    String[] content = {"NaN", "1e999", "bad", "", "0", " 4.5 "};
    List<RowRecord> rows = new ArrayList<>();
    for (int index = 0; index < content.length; index++) {
      rows.add(sample(query, BEGIN * 1000 + index * step(query), content[index], 1));
    }

    PropertyTrendVO trend = trend(query, data(rows.toArray(RowRecord[]::new)));

    assertEquals(6, trend.getTotalCount());
    assertEquals(2, trend.getNumericCount());
    assertEquals(6, trend.getScannedCount());
    assertFalse(trend.isSampled());
    assertTrue(trend.isCountExact());
    assertEquals(0, new BigDecimal("2.25").compareTo(trend.getBuckets().getFirst().getAvg()));
  }

  @Test
  void emptyNativeGroupsProduceEmptyExactHistory() throws Exception {
    PropertyTrendQuery query = query();

    PropertyTrendVO trend = trend(query, data(row(BEGIN * 1000, null, null, 0L)));

    assertEquals(0, trend.getTotalCount());
    assertEquals(0, trend.getScannedCount());
    assertTrue(trend.getBuckets().isEmpty());
    assertTrue(trend.isCountExact());
    assertFalse(trend.isSampled());
  }

  @Test
  void downsampledSourceGroupCannotBeReportedAsCompleteEvenWhenReturnBudgetIsNotFilled()
      throws Exception {
    PropertyTrendQuery query = query();

    PropertyTrendVO trend = trend(query, data(sample(query, BEGIN * 1000, "5", 2000000)));

    assertEquals(1, trend.getTotalCount());
    assertEquals(1, trend.getScannedCount());
    assertTrue(trend.isSampled());
    assertFalse(trend.isCountExact());
  }

  @Test
  void boundaryExtraCandidateUsesCommonSentinelWithoutExceedingMainWindowBudget() throws Exception {
    PropertyTrendQuery query = query();
    long mainStart = BEGIN * 1000 + query.getIntervalSeconds() * 1000;
    long mainEnd = mainStart + query.getIntervalSeconds() * 1000;
    List<RowRecord> rows = new ArrayList<>();
    for (long group = BEGIN * 1000; group < mainEnd; group += step(query)) {
      if (group + step(query) <= mainStart) continue;
      rows.add(row(group, "5", Math.max(group, mainStart), 1L));
    }
    assertEquals(query.getSampleFetchSize(), rows.size());

    PropertyTrendVO trend = trend(query, data(rows.reversed().toArray(RowRecord[]::new)));

    assertEquals(query.getSamplesPerWindow(), trend.getTotalCount());
    assertEquals(query.getSampleFetchSize(), trend.getScannedCount());
    assertTrue(trend.isSampled());
    assertFalse(trend.isCountExact());
    assertEquals(1, trend.getBuckets().size());
  }

  @Test
  void nativeReturnedRowsCannotExceedComputedFineWindowLimit() throws Exception {
    PropertyTrendQuery query = query();
    List<RowRecord> rows = new ArrayList<>();
    for (int index = 0; index <= fineWindows(query); index++) {
      rows.add(row(BEGIN * 1000, null, null, 0L));
    }

    IoTException error =
        assertThrows(IoTException.class, () -> trend(query, data(rows.toArray(RowRecord[]::new))));

    assertEquals(503, error.getCode());
    assertTrue(error.getMessage().contains("样本数超过限制"));
  }

  @Test
  void nonemptyGroupRequiresItsContentAndOriginalTime() throws Exception {
    for (RowRecord invalid :
        List.of(row(BEGIN * 1000, null, BEGIN * 1000, 1L), row(BEGIN * 1000, "5", null, 1L))) {
      IoTException error = assertThrows(IoTException.class, () -> trend(query(), data(invalid)));
      assertEquals(503, error.getCode());
    }
  }

  @Test
  void originalTimeMustBelongToItsNativeGroupAndTheRequestedInclusiveRange() throws Exception {
    PropertyTrendQuery query = query();
    long begin = BEGIN * 1000;
    long endExclusive = query.getEndCreateTime() * 1000 + 1;
    for (RowRecord invalid :
        List.of(
            row(begin, "5", begin - 1, 1L),
            row(begin, "5", begin + step(query), 1L),
            row(endExclusive, "5", endExclusive, 1L),
            row(begin + 1, "5", begin + 1, 1L))) {
      IoTException error = assertThrows(IoTException.class, () -> trend(query, data(invalid)));
      assertEquals(503, error.getCode());
    }
  }

  @Test
  void missingOrNegativeSourceCountCannotLookLikeAnEmptyWindow() throws Exception {
    for (Long count : new Long[] {null, -1L}) {
      IoTException error =
          assertThrows(
              IoTException.class,
              () -> trend(query(), data(row(BEGIN * 1000, "5", BEGIN * 1000, count))));
      assertEquals(503, error.getCode());
    }
  }

  @Test
  void missingNativeColumnsFailExplicitly() throws Exception {
    PropertyTrendQuery query = query();
    SessionDataSet data = data(sample(query, BEGIN * 1000, "5", 1));
    when(data.getColumnNames()).thenReturn(List.of("Time", "content"));

    IoTException error = assertThrows(IoTException.class, () -> trend(query(), data));

    assertEquals(503, error.getCode());
    verify(data).close();
  }

  @Test
  void missingSeriesWithoutColumnsReturnsEmptyExactHistory() throws Exception {
    SessionDataSet data = data();
    when(data.getColumnNames()).thenReturn(List.of("Time"));

    PropertyTrendVO trend = trend(query(), data);

    assertEquals(0, trend.getTotalCount());
    assertEquals(0, trend.getScannedCount());
    assertTrue(trend.getBuckets().isEmpty());
    assertFalse(trend.isSampled());
    assertTrue(trend.isCountExact());
    verify(data).close();
  }

  @Test
  void nativeFailureReturnsRetryableCodeAndPreservesItsCause() throws Exception {
    Session session = mock(Session.class);
    StatementExecutionException timeout = new StatementExecutionException("query timeout");
    when(session.executeQueryStatement(anyString(), anyLong())).thenThrow(timeout);

    IoTException error =
        assertThrows(IoTException.class, () -> service(session).queryPropertyTrend(query()));

    assertEquals(503, error.getCode());
    assertSame(timeout, error.getCause());
    verify(session, times(1)).executeQueryStatement(anyString(), anyLong());
  }

  @Test
  void slowEmptyFinalFetchCannotReturnPartialSuccessAfterDeadline() throws Exception {
    Session session = mock(Session.class);
    SessionDataSet data = data();
    when(data.hasNext())
        .thenAnswer(
            call -> {
              TimeUnit.MILLISECONDS.sleep(80);
              return false;
            });
    when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(data);
    IoTDBIoTDeviceLogService service = service(session);
    set(service, "historyQueryTimeoutMs", 30L);

    IoTException error =
        assertThrows(IoTException.class, () -> service.queryPropertyTrend(query()));

    assertEquals(503, error.getCode());
    assertTrue(error.getMessage().contains("超时"));
    verify(data).close();
  }
}
