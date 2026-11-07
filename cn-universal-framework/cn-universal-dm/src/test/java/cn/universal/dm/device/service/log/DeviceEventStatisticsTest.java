package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.consistent.DeviceLogHashSharding;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.vo.EventCountAndTimeVO;
import cn.universal.persistence.mapper.IoTDeviceLogShardMapper;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.Session;
import org.apache.iotdb.tsfile.read.common.RowRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;

class DeviceEventStatisticsTest {
  private static final String PRODUCT = "product";
  private static final String IOT_ID = "productdevice";
  private static final long LATEST_TIME = 1_790_215_666_000L;

  @Test
  void clickHouseReadsAtMostHundredRowsAndUsesNewestTimestamp() throws Exception {
    for (int count : new int[] {0, 1, 99, 100}) {
      JdbcTemplate jdbc = mock(JdbcTemplate.class);
      when(jdbc.queryForList(anyString(), eq("alarm"), eq(IOT_ID), eq(PRODUCT)))
          .thenReturn(
              eventRows(count, "create_time", "2026-09-24 10:00:00", "2026-09-23 10:00:00"));

      var page = clickHouse(jdbc, event("alarm")).queryEventTotal(PRODUCT, IOT_ID);

      assertEquals(count >= 100 ? "99+" : Long.toString(count), page.getList().getFirst().getQty());
      assertEquals(count == 0 ? null : "2026-09-24 10:00:00", page.getList().getFirst().getTime());
      ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
      verify(jdbc).queryForList(sql.capture(), eq("alarm"), eq(IOT_ID), eq(PRODUCT));
      assertTrue(sql.getValue().endsWith("ORDER BY create_time DESC LIMIT 100"));
      assertFalse(sql.getValue().contains("COUNT("));
      assertTrue(sql.getValue().contains("product_key = ?"));
      assertTrue(sql.getValue().contains("message_type = 'EVENT'"));
    }
  }

  @Test
  void clickHouseFailureDoesNotReturnPartialStatisticsOrReplaceTheFailureWithZero()
      throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    QueryTimeoutException failure = new QueryTimeoutException("query deadline exceeded");
    when(jdbc.queryForList(anyString(), eq("alarm"), eq(IOT_ID), eq(PRODUCT)))
        .thenReturn(eventRows(5, "create_time", "2026-09-24 10:00:00", "2026-09-23 10:00:00"));
    when(jdbc.queryForList(anyString(), eq("offline"), eq(IOT_ID), eq(PRODUCT))).thenThrow(failure);
    IoTDeviceEvents failedEvent = event("offline");
    CkDeviceLogService service = clickHouse(jdbc, event("alarm"), failedEvent);

    IoTException error =
        assertThrows(IoTException.class, () -> service.queryEventTotal(PRODUCT, IOT_ID));

    assertEquals(503, error.getCode());
    assertSame(failure, error.getCause());
    assertNull(failedEvent.getQty());
  }

  @Test
  void tdengineReadsAtMostHundredRowsAndUsesNewestTimestamp() throws Exception {
    LocalDateTime latest = LocalDateTime.of(2026, 9, 24, 10, 0);
    for (int count : new int[] {0, 1, 99, 100}) {
      JdbcTemplate jdbc = mock(JdbcTemplate.class);
      when(jdbc.queryForList(anyString()))
          .thenReturn(
              eventRows(
                  count, "ts", Timestamp.valueOf(latest), Timestamp.valueOf(latest.minusDays(1))));

      var page = tdengine(jdbc, event("alarm")).queryEventTotal(PRODUCT, IOT_ID);

      assertEquals(count >= 100 ? "99+" : Long.toString(count), page.getList().getFirst().getQty());
      assertEquals(count == 0 ? null : latest.toString(), page.getList().getFirst().getTime());
      ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
      verify(jdbc).queryForList(sql.capture());
      assertTrue(sql.getValue().strip().endsWith("ORDER BY ts DESC LIMIT 100"));
      assertFalse(sql.getValue().contains("COUNT("));
      assertTrue(sql.getValue().contains("product_key = '" + PRODUCT + "'"));
      assertTrue(sql.getValue().contains("FROM iot_log.iot_device_log"));
      assertTrue(sql.getValue().contains("event = 'alarm'"));
      assertTrue(sql.getValue().contains("message_type = 'EVENT'"));
      assertTrue(sql.getValue().contains("iot_id = '" + IOT_ID + "'"));
    }
  }

  @Test
  void tdengineFailureIsAnExplicitServiceErrorWithItsOriginalCause() throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    QueryTimeoutException failure = new QueryTimeoutException("query deadline exceeded");
    when(jdbc.queryForList(anyString())).thenThrow(failure);
    IoTDeviceEvents event = event("alarm");
    TDengineDeviceLogService service = tdengine(jdbc, event);

    IoTException error =
        assertThrows(IoTException.class, () -> service.queryEventTotal(PRODUCT, IOT_ID));

    assertEquals(503, error.getCode());
    assertSame(failure, error.getCause());
    assertNull(event.getQty());
  }

  @Test
  void iotdbReadsOnlyLatestHundredToPreserveDisplayThresholdAndLatestTimestamp() throws Exception {
    for (int count : new int[] {0, 99, 100}) {
      Session session = mock(Session.class);
      SessionDataSet data = descendingEvents(count);
      when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(data);

      var page = iotdb(session, event("alarm")).queryEventTotal(PRODUCT, IOT_ID);

      assertEquals(
          count == 100 ? "99+" : Integer.toString(count), page.getList().getFirst().getQty());
      assertEquals(
          count == 0 ? "" : Instant.ofEpochMilli(LATEST_TIME).toString(),
          page.getList().getFirst().getTime());
      ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
      verify(session)
          .executeQueryStatement(
              sql.capture(), longThat(timeout -> timeout > 0 && timeout <= 8000));
      assertTrue(sql.getValue().startsWith("SELECT content FROM root.device."));
      assertTrue(sql.getValue().contains(".event."));
      assertTrue(sql.getValue().contains("product_key='" + PRODUCT + "'"));
      assertTrue(sql.getValue().contains("iot_id='" + IOT_ID + "'"));
      assertTrue(sql.getValue().endsWith("ORDER BY time DESC LIMIT 100"));
      assertFalse(sql.getValue().contains("COUNT("));
      assertFalse(sql.getValue().contains("OFFSET"));
      assertFalse(sql.getValue().contains("*"));
      verify(data, times(count)).next();
      verify(data).close();
      verifyNoMoreInteractions(session);
    }
  }

  @Test
  void iotdbQueryFailureIsNotAnEmptyEventStatistic() throws Exception {
    Session session = mock(Session.class);
    when(session.executeQueryStatement(anyString(), anyLong()))
        .thenThrow(new StatementExecutionException("query deadline exceeded"));
    IoTDeviceEvents event = event("alarm");
    IoTDBIoTDeviceLogService service = iotdb(session, event);

    IoTException error =
        assertThrows(IoTException.class, () -> service.queryEventTotal(PRODUCT, IOT_ID));

    assertTrue(error.getMessage().contains("事件统计"));
    assertNull(event.getQty());
  }

  @Test
  void iotdbReadFailureClosesTheDatasetAndDoesNotExposeAnIncompleteCount() throws Exception {
    Session session = mock(Session.class);
    SessionDataSet data = mock(SessionDataSet.class);
    when(data.hasNext()).thenReturn(true).thenThrow(new StatementExecutionException("read failed"));
    when(data.next()).thenReturn(new RowRecord(LATEST_TIME));
    when(session.executeQueryStatement(anyString(), anyLong())).thenReturn(data);
    IoTDeviceEvents event = event("alarm");
    IoTDBIoTDeviceLogService service = iotdb(session, event);

    assertThrows(IoTException.class, () -> service.queryEventTotal(PRODUCT, IOT_ID));

    assertNull(event.getQty());
    verify(data).close();
  }

  @Test
  void iotdbRejectsAnUnresolvedDeviceInsteadOfScanningAllDeviceEventPaths() throws Exception {
    Session session = mock(Session.class);
    IoTDBIoTDeviceLogService service = iotdb(session, event("alarm"));

    IoTException error =
        assertThrows(IoTException.class, () -> service.queryEventTotal(PRODUCT, "unknown"));

    assertEquals(400, error.getCode());
    verifyNoInteractions(session);
  }

  @Test
  void relationalStatsScopeEachEventAndCapLargeCountsWithoutOverflow() throws Exception {
    IoTDeviceLogShardMapper mapper = mock(IoTDeviceLogShardMapper.class);
    DeviceLogHashSharding sharding = mock(DeviceLogHashSharding.class);
    when(sharding.generateTableName("iot_device_log", IOT_ID)).thenReturn("iot_device_log_17");
    EventCountAndTimeVO stats = new EventCountAndTimeVO();
    stats.setEvent("alarm");
    stats.setTotalCount(4_294_967_296L);
    stats.setLatestCreateTime(new Date(LATEST_TIME));
    EventCountAndTimeVO offline = new EventCountAndTimeVO();
    offline.setEvent("offline");
    offline.setTotalCount(99L);
    when(mapper.queryHistoryEventSummaries(eq(IOT_ID), eq(PRODUCT), anyList()))
        .thenReturn(List.of(stats, offline));
    MysqlDeviceLogService service =
        relational(mapper, sharding, event("alarm"), event("offline"), event("missing"));

    var page = service.queryEventTotal(PRODUCT, IOT_ID);

    assertEquals(
        List.of("99+", "99", "0"), page.getList().stream().map(IoTDeviceEvents::getQty).toList());
    assertEquals(
        cn.hutool.core.date.DateUtil.formatDateTime(new Date(LATEST_TIME)),
        page.getList().getFirst().getTime());
    assertNull(page.getList().get(1).getTime());
    verify(mapper)
        .queryHistoryEventSummaries(IOT_ID, PRODUCT, List.of("alarm", "offline", "missing"));
    verify(sharding).generateTableName("iot_device_log", IOT_ID);
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void relationalStatsPreserveDisplayThresholdAndEmptyTimestamp() throws Exception {
    for (Long count : new Long[] {null, 0L, 99L, 100L}) {
      IoTDeviceLogShardMapper mapper = mock(IoTDeviceLogShardMapper.class);
      DeviceLogHashSharding sharding = mock(DeviceLogHashSharding.class);
      EventCountAndTimeVO stats = new EventCountAndTimeVO();
      stats.setEvent("alarm");
      stats.setTotalCount(count);
      when(mapper.queryHistoryEventSummaries(eq(IOT_ID), eq(PRODUCT), anyList()))
          .thenReturn(List.of(stats));
      var result = relational(mapper, sharding, event("alarm")).queryEventTotal(PRODUCT, IOT_ID);
      assertEquals(
          count == null ? "0" : count >= 100 ? "99+" : count.toString(),
          result.getList().getFirst().getQty());
      assertNull(result.getList().getFirst().getTime());
    }
  }

  @Test
  void relationalQueryFailureIsNotTurnedIntoZeroEvents() throws Exception {
    IoTDeviceLogShardMapper mapper = mock(IoTDeviceLogShardMapper.class);
    DeviceLogHashSharding sharding = mock(DeviceLogHashSharding.class);
    when(sharding.generateTableName("iot_device_log", IOT_ID)).thenReturn("iot_device_log_17");
    QueryTimeoutException failure = new QueryTimeoutException("query deadline exceeded");
    when(mapper.queryHistoryEventSummaries(eq(IOT_ID), eq(PRODUCT), anyList())).thenThrow(failure);
    IoTDeviceEvents event = event("alarm");
    MysqlDeviceLogService service = relational(mapper, sharding, event);

    assertSame(
        failure,
        assertThrows(QueryTimeoutException.class, () -> service.queryEventTotal(PRODUCT, IOT_ID)));
    assertNull(event.getQty());
  }

  private List<Map<String, Object>> eventRows(
      int count, String column, Object newest, Object older) {
    return LongStream.range(0, count)
        .mapToObj(index -> Map.of(column, index == 0 ? newest : older))
        .toList();
  }

  private IoTDeviceEvents event(String id) {
    return IoTDeviceEvents.builder().id(id).name(id).build();
  }

  private CkDeviceLogService clickHouse(JdbcTemplate jdbc, IoTDeviceEvents... events)
      throws Exception {
    CkDeviceLogService service =
        new CkDeviceLogService() {
          @Override
          List<IoTDeviceEvents> selectDevEvents(String productKey) {
            return List.of(events);
          }
        };
    set(service, "historyJdbcTemplate", jdbc);
    set(service, "devLogTableName", "iot_device_log");
    return service;
  }

  private TDengineDeviceLogService tdengine(JdbcTemplate jdbc, IoTDeviceEvents... events)
      throws Exception {
    TDengineDeviceLogService service =
        new TDengineDeviceLogService() {
          @Override
          List<IoTDeviceEvents> selectDevEvents(String productKey) {
            return List.of(events);
          }
        };
    set(service, "historyJdbcTemplate", jdbc);
    set(service, "databaseName", "iot_log");
    set(service, "devLogTableName", "iot_device_log");
    return service;
  }

  private IoTDBIoTDeviceLogService iotdb(Session session, IoTDeviceEvents... events)
      throws Exception {
    IoTDBIoTDeviceLogService service =
        new IoTDBIoTDeviceLogService() {
          @Override
          List<IoTDeviceEvents> selectDevEvents(String productKey) {
            return List.of(events);
          }
        };
    set(service, "session", session);
    set(service, "storageGroup", "root.device");
    return service;
  }

  private MysqlDeviceLogService relational(
      IoTDeviceLogShardMapper mapper, DeviceLogHashSharding sharding, IoTDeviceEvents... events)
      throws Exception {
    MysqlDeviceLogService service =
        new MysqlDeviceLogService() {
          @Override
          List<IoTDeviceEvents> selectDevEvents(String productKey) {
            return List.of(events);
          }
        };
    set(service, "ioTDeviceLogShardMapper", mapper);
    set(service, "deviceLogHashSharding", sharding);
    return service;
  }

  private SessionDataSet descendingEvents(int count) throws Exception {
    SessionDataSet data = mock(SessionDataSet.class);
    var rows =
        LongStream.range(0, count)
            .mapToObj(index -> new RowRecord(LATEST_TIME - index * 1000))
            .iterator();
    when(data.hasNext()).thenAnswer(invocation -> rows.hasNext());
    when(data.next()).thenAnswer(invocation -> rows.next());
    return data;
  }

  private void set(Object target, String name, Object value) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
      try {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
        return;
      } catch (NoSuchFieldException ignored) {
        // Test services override the metadata lookup; fields belong to their parent services.
      }
    }
    throw new NoSuchFieldException(name);
  }
}
