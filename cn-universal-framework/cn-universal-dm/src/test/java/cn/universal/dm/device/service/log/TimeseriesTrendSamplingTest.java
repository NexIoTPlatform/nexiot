package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.PropertyTrendQuery;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.session.Session;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.RowRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class TimeseriesTrendSamplingTest {
  private PropertyTrendQuery query() {
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setProductKey("product");
    query.setDeviceId("device");
    query.setIotId("productdevice");
    query.setProperty("speed");
    query.setBeginCreateTime(1700000000L);
    query.setEndCreateTime(1700000100L);
    query.setMaxPoints(20);
    query.setMaxScanRows(1000);
    return query;
  }

  private void set(Object object, String name, Object value) throws Exception {
    Field field = object.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(object, value);
  }

  @Test
  @SuppressWarnings("unchecked")
  void clickhouseSeeksEveryWindowBeforeParsingAndRetainsInclusiveEnd() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    set(service, "historyJdbcTemplate", jdbc);
    set(service, "devLogMetaTableName", "metadata");
    PropertyTrendQuery query = query();
    query.setProperty("speed' OR 1=1");
    when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
        .thenAnswer(
            call -> {
              String sql = call.getArgument(0);
              Object[] args = (Object[]) call.getRawArguments()[2];
              assertEquals(
                  query.getSampleWindowCount(),
                  sql.split("ORDER BY create_time DESC LIMIT", -1).length - 1);
              assertTrue(sql.contains("property = ?"));
              assertFalse(sql.contains(query.getProperty()));
              assertFalse(sql.contains("GROUP BY"));
              assertFalse(sql.contains("toFloat"));
              assertTrue(sql.contains("max_execution_time="));
              assertTrue(
                  List.of(args).contains(new Timestamp(query.getEndCreateTime() * 1000 + 1)));
              return List.of(
                  new PropertyTrendSampleVO(0, "0"),
                  new PropertyTrendSampleVO(0, "bad"),
                  new PropertyTrendSampleVO(query.getSampleWindowCount() - 1, "8"));
            });
    PropertyTrendVO trend = service.queryPropertyTrend(query);
    assertEquals(3, trend.getTotalCount());
    assertEquals(2, trend.getNumericCount());
    assertEquals(2, trend.getBuckets().size());
    assertEquals(1700000000000L, trend.getBuckets().getFirst().getTime());
    assertFalse(trend.isSampled());
    assertTrue(trend.isCountExact());
    verify(jdbc, times(1)).query(anyString(), any(RowMapper.class), any(Object[].class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void tdengineSampleDetectsTruncationWithoutCallingNativeTextAggregates() throws Exception {
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    set(service, "historyJdbcTemplate", jdbc);
    set(service, "databaseName", "iot_log");
    set(service, "devLogMetaTableName", "metadata");
    PropertyTrendQuery query = query();
    List<PropertyTrendSampleVO> samples = new ArrayList<>();
    for (int index = 0; index < query.getSampleFetchSize(); index++) {
      samples.add(new PropertyTrendSampleVO(0, "5"));
    }
    samples.add(new PropertyTrendSampleVO(query.getSampleWindowCount() - 1, "bad"));
    when(jdbc.query(anyString(), any(RowMapper.class)))
        .thenAnswer(
            call -> {
              String sql = call.getArgument(0);
              assertEquals(
                  query.getSampleWindowCount(), sql.split("ORDER BY ts DESC LIMIT", -1).length - 1);
              assertTrue(sql.contains("AND ts < " + (query.getEndCreateTime() * 1000 + 1)));
              assertTrue(sql.contains("SUBSTR(content,1,1025)"));
              assertFalse(sql.contains("CAST"));
              assertFalse(sql.contains("INTERVAL("));
              return samples;
            });
    PropertyTrendVO trend = service.queryPropertyTrend(query);
    assertTrue(trend.isSampled());
    assertFalse(trend.isCountExact());
    assertEquals(query.getSamplesPerWindow() + 1, trend.getTotalCount());
    assertEquals(samples.size(), trend.getScannedCount());
    assertNull(trend.getBuckets().getLast().getAvg());
    assertEquals(0, BigDecimal.valueOf(5).compareTo(trend.getBuckets().getFirst().getAvg()));
  }

  @Test
  void influxSamplesEachRangeWithLimitBeforeNumericWorkAndMergesOneRequest() throws Exception {
    InfluxDBDeviceLogService service = new InfluxDBDeviceLogService();
    InfluxDBClient client = mock(InfluxDBClient.class);
    QueryApi api = mock(QueryApi.class);
    when(client.getQueryApi()).thenReturn(api);
    set(service, "historyQueryClient", client);
    set(service, "bucket", "device_logs");
    PropertyTrendQuery query = query();
    FluxRecord first = mock(FluxRecord.class);
    when(first.getValueByKey("window_index")).thenReturn(0L);
    when(first.getValue()).thenReturn("1e999");
    FluxRecord last = mock(FluxRecord.class);
    when(last.getValueByKey("window_index")).thenReturn(query.getSampleWindowCount() - 1);
    when(last.getValue()).thenReturn("0");
    FluxTable table = mock(FluxTable.class);
    when(table.getRecords()).thenReturn(List.of(first, last));
    when(api.query(anyString()))
        .thenAnswer(
            call -> {
              String flux = call.getArgument(0);
              assertEquals(
                  query.getSampleWindowCount(), flux.split("range\\(start:", -1).length - 1);
              assertTrue(flux.contains("r.iotId == \"productdevice\""));
              assertTrue(flux.contains("r._field == \"content\""));
              assertTrue(flux.contains("limit(n: " + query.getSampleFetchSize() + ")"));
              assertTrue(flux.contains("union(tables:"));
              assertTrue(flux.contains("strings.strlen(v:r._value)<=1024"));
              assertTrue(flux.contains("stop: 2023-11-14T22:15:00.000000001Z"));
              assertFalse(flux.contains("sort("));
              assertFalse(flux.contains("reduce("));
              assertFalse(flux.contains("finiteNumber"));
              return List.of(table);
            });
    PropertyTrendVO trend = service.queryPropertyTrend(query);
    assertEquals(2, trend.getTotalCount());
    assertEquals(1, trend.getNumericCount());
    assertNull(trend.getBuckets().getFirst().getAvg());
    assertEquals(0, BigDecimal.ZERO.compareTo(trend.getBuckets().getLast().getAvg()));
    verify(api, times(1)).query(anyString());
  }

  @Test
  void iotdbUsesOneNativeSubwindowQueryWithExactSeriesAndRequestDeadline() throws Exception {
    IoTDBIoTDeviceLogService service = new IoTDBIoTDeviceLogService();
    Session session = mock(Session.class);
    set(service, "session", session);
    set(service, "storageGroup", "root.device");
    PropertyTrendQuery query = query();
    when(session.executeQueryStatement(anyString(), anyLong()))
        .thenAnswer(
            call -> {
              SessionDataSet data = mock(SessionDataSet.class);
              when(data.getColumnNames())
                  .thenReturn(List.of("Time", "content", "original_time", "source_count"));
              RowRecord row = new RowRecord(1700000000000L);
              row.addField(0D, TSDataType.DOUBLE);
              row.addField(1700000000000L, TSDataType.INT64);
              row.addField(1L, TSDataType.INT64);
              when(data.hasNext()).thenReturn(true, false);
              when(data.next()).thenReturn(row);
              return data;
            });
    PropertyTrendVO trend = service.queryPropertyTrend(query);
    assertEquals(1, trend.getTotalCount());
    assertEquals(trend.getTotalCount(), trend.getNumericCount());
    assertTrue(trend.isCountExact());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
    verify(session).executeQueryStatement(sql.capture(), timeout.capture());
    assertTrue(
        sql.getValue()
            .matches(
                ".*root[.]device[.]product_[a-f0-9]+[.]device_[a-f0-9]+[.]property[.]speed_[a-f0-9]+.*"));
    assertTrue(sql.getValue().contains("SUBSTRING(last_value(content),1,1025)"));
    assertTrue(sql.getValue().contains("max_time(content) AS original_time"));
    assertTrue(sql.getValue().contains("GROUP BY ([1700000000000,1700000100001),"));
    assertTrue(sql.getValue().endsWith("ORDER BY time DESC"));
    assertFalse(sql.getValue().contains("CAST"));
    assertTrue(timeout.getValue() > 0 && timeout.getValue() <= 8000);
  }

  @Test
  @SuppressWarnings("unchecked")
  void clickhouseLargeTrendsBoundPlanSizeAndRetainEveryWindow() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    set(service, "historyJdbcTemplate", jdbc);
    set(service, "devLogMetaTableName", "metadata");
    PropertyTrendQuery query = query();
    query.setEndCreateTime(query.getBeginCreateTime() + 86400);
    query.setMaxPoints(600);
    query.setMaxScanRows(10000);
    when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
        .thenAnswer(
            call -> {
              String sql = call.getArgument(0);
              var matches =
                  java.util.regex.Pattern.compile("SELECT ([0-9]+) AS window_index").matcher(sql);
              List<PropertyTrendSampleVO> samples = new ArrayList<>();
              int windows = 0;
              while (matches.find()) {
                windows++;
                int index = Integer.parseInt(matches.group(1));
                for (int row = 0; row < query.getSampleFetchSize(); row++) {
                  samples.add(new PropertyTrendSampleVO(index, "5"));
                }
              }
              assertTrue(windows > 0 && windows <= 100);
              return samples;
            });
    PropertyTrendVO trend = service.queryPropertyTrend(query);
    assertEquals(query.getSampleWindowCount(), trend.getBuckets().size());
    assertEquals(
        (long) query.getSampleWindowCount() * query.getSamplesPerWindow(), trend.getTotalCount());
    assertEquals(
        (long) query.getSampleWindowCount() * query.getSampleFetchSize(), trend.getScannedCount());
    assertTrue(trend.isSampled());
    assertFalse(trend.isCountExact());
    verify(jdbc, times((query.getSampleWindowCount() + 99) / 100))
        .query(anyString(), any(RowMapper.class), any(Object[].class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void clickhouseBatchDeadlineStopsRemainingWindowsAndRejectsPartialResults() throws Exception {
    CkDeviceLogService service = new CkDeviceLogService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    set(service, "historyJdbcTemplate", jdbc);
    set(service, "historyQueryTimeoutSeconds", 1);
    PropertyTrendQuery query = query();
    query.setEndCreateTime(query.getBeginCreateTime() + 86400);
    query.setMaxPoints(600);
    when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
        .thenAnswer(
            call -> {
              Thread.sleep(1100);
              return List.of(new PropertyTrendSampleVO(0, "5"));
            });
    IoTException failure =
        assertThrows(IoTException.class, () -> service.queryPropertyTrend(query));
    assertTrue(failure.getMessage().contains("超时"));
    verify(jdbc, times(1)).query(anyString(), any(RowMapper.class), any(Object[].class));
  }

  @Test
  void sampledTimeoutsRemainExplicitFailures() throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.query(anyString(), any(RowMapper.class)))
        .thenThrow(new QueryTimeoutException("timeout"));
    when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
        .thenThrow(new QueryTimeoutException("timeout"));
    CkDeviceLogService clickhouse = new CkDeviceLogService();
    set(clickhouse, "historyJdbcTemplate", jdbc);
    TDengineDeviceLogService tdengine = new TDengineDeviceLogService();
    set(tdengine, "historyJdbcTemplate", jdbc);
    assertThrows(IoTException.class, () -> clickhouse.queryPropertyTrend(query()));
    assertThrows(IoTException.class, () -> tdengine.queryPropertyTrend(query()));
  }
}
