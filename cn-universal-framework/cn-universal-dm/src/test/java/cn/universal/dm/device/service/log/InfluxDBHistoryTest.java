package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PropertyTrendQuery;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class InfluxDBHistoryTest {
  private InfluxDBDeviceLogService service;
  private QueryApi queryApi;

  @BeforeEach
  void setup() throws Exception {
    service = new InfluxDBDeviceLogService();
    InfluxDBClient client = mock(InfluxDBClient.class);
    queryApi = mock(QueryApi.class);
    when(client.getQueryApi()).thenReturn(queryApi);
    setField("influxDBClient", client);
    setField("historyQueryClient", client);
    setField("bucket", "device_logs");
  }

  private void setField(String name, Object value) throws Exception {
    Field field = InfluxDBDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }

  private PropertyTrendQuery trendQuery() {
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setSamplingMode("exact");
    query.setProductKey("product");
    query.setIotId("device");
    query.setProperty("speed");
    query.setBeginCreateTime(1700000000L);
    query.setEndCreateTime(1700000060L);
    query.setMaxPoints(20);
    return query;
  }

  @Test
  void eventStatisticsUseOneBudgetedQueryAndMergeAllSeries() throws Exception {
    service = spy(service);
    IoTDeviceEvents event = IoTDeviceEvents.builder().id("alarm").build();
    IoTDeviceEvents missing = IoTDeviceEvents.builder().id("missing").build();
    doReturn(List.of(event, missing)).when(service).selectDevEvents("product");
    InfluxDBClient historyClient = mock(InfluxDBClient.class);
    QueryApi historyApi = mock(QueryApi.class);
    when(historyClient.getQueryApi()).thenReturn(historyApi);
    setField("historyQueryClient", historyClient);
    FluxTable first = mock(FluxTable.class);
    FluxTable second = mock(FluxTable.class);
    List<FluxRecord> firstRecords =
        List.of(
            eventRecord("event_counts", "alarm", 60L, null),
            eventRecord("event_latest", "alarm", "first", Instant.ofEpochSecond(1700000000L)));
    List<FluxRecord> secondRecords =
        List.of(
            eventRecord("event_counts", "alarm", 50L, null),
            eventRecord("event_latest", "alarm", "last", Instant.ofEpochSecond(1700000060L)));
    when(first.getRecords()).thenReturn(firstRecords);
    when(second.getRecords()).thenReturn(secondRecords);
    when(historyApi.query(anyString()))
        .thenAnswer(
            call -> {
              String flux = call.getArgument(0);
              assertTrue(flux.contains("range(start: 1970-01-01T00:00:00Z)"));
              assertTrue(flux.contains("r.productKey == \"product\" and r.iotId == \"device\""));
              assertTrue(flux.contains("limit(n: 100) |> count()"));
              assertTrue(flux.contains("source |> last()"));
              return List.of(first, second);
            });
    assertEquals(2L, service.queryEventTotal("product", "device").getTotalCount());
    assertEquals("99+", event.getQty());
    assertEquals("2023-11-14T22:14:20Z", event.getTime());
    assertEquals("0", missing.getQty());
    assertEquals("", missing.getTime());
    verify(historyApi, times(1)).query(anyString());
    verifyNoInteractions(queryApi);
  }

  @Test
  void eventStatisticsFailuresAreNotReportedAsZero() {
    service = spy(service);
    doReturn(List.of(IoTDeviceEvents.builder().id("alarm").build()))
        .when(service)
        .selectDevEvents("product");
    when(queryApi.query(anyString())).thenThrow(new RuntimeException("query timeout"));
    assertThrows(IoTException.class, () -> service.queryEventTotal("product", "device"));
  }

  @Test
  void historyDoesNotFallBackToUnboundedWriteClient() throws Exception {
    setField("historyQueryClient", null);
    assertThrows(IoTException.class, () -> service.queryPropertyTrend(trendQuery()));
    verifyNoInteractions(queryApi);
  }

  private FluxRecord eventRecord(String result, String event, Object value, Instant time) {
    FluxRecord record = mock(FluxRecord.class);
    when(record.getValueByKey("result")).thenReturn(result);
    when(record.getValueByKey("event")).thenReturn(event);
    when(record.getValue()).thenReturn(value);
    when(record.getTime()).thenReturn(time);
    return record;
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "NEXIOT_INFLUX_LIVE_TEST", matches = "true")
  void liveFluxEventStatisticsReturnExactSmallCountsAndLatestAcrossSeries() throws Exception {
    service = spy(service);
    IoTDeviceEvents event = IoTDeviceEvents.builder().id("alarm").build();
    doReturn(List.of(event)).when(service).selectDevEvents("product");
    try (InfluxDBClient client = liveClient()) {
      when(queryApi.query(anyString()))
          .thenAnswer(
              call -> {
                String flux = call.getArgument(0);
                String fixture =
                    "import \"array\"\n"
                        + "source = array.from(rows: ["
                        + "{_time:2023-11-14T22:13:20Z,_value:\"a\",event:\"alarm\",series:\"a\"},"
                        + "{_time:2023-11-14T22:14:20Z,_value:\"b\",event:\"alarm\",series:\"b\"},"
                        + "{_time:2023-11-14T22:14:00Z,_value:\"c\",event:\"alarm\",series:\"b\"}])"
                        + " |> group(columns: [\"event\",\"series\"]) |> sort(columns:"
                        + " [\"_time\"])\n";
                return client
                    .getQueryApi()
                    .query(fixture + flux.substring(flux.indexOf("source |>")));
              });
      service.queryEventTotal("product", "device");
      assertEquals("3", event.getQty());
      assertEquals("2023-11-14T22:14:20Z", event.getTime());
    }
  }

  @Test
  void historyRangeKeepsEpochZeroAndLargeEpochSeconds() {
    PropertyTrendQuery query = trendQuery();
    for (long start : List.of(0L, 10_000_000_001L)) {
      query.setBeginCreateTime(start);
      query.setEndCreateTime(start + 60);
      doAnswer(
              call -> {
                String flux = call.getArgument(0);
                assertTrue(flux.contains("range(start: " + Instant.ofEpochSecond(start)));
                assertTrue(
                    flux.contains("stop: " + Instant.ofEpochSecond(start + 60).plusNanos(1)));
                return List.of();
              })
          .when(queryApi)
          .query(anyString());
      service.queryPropertyTrend(query);
    }
  }

  @Test
  void trendPushesTagFiltersAndWindowsToDatabaseAndIncludesExactEnd() {
    when(queryApi.query(anyString()))
        .thenAnswer(
            call -> {
              String flux = call.getArgument(0);
              assertTrue(flux.contains("r.iotId == \"device\""));
              assertTrue(flux.contains("r.property == \"speed\""));
              assertTrue(flux.contains("stop: 2023-11-14T22:14:20.000000001Z"));
              assertTrue(flux.contains("window(every: 4s, offset: 0s"));
              assertTrue(flux.contains("group(columns: [\"_start\"])"));
              assertTrue(flux.contains("accumulator.count + r.count"));
              assertTrue(flux.contains("accumulator.numericCount + r.numericCount"));
              assertTrue(flux.contains("limit(n: 21)"));
              assertFalse(flux.contains("pivot("));
              assertFalse(flux.contains("offset:" + " 50"));
              return List.of();
            });
    assertEquals(0, service.queryPropertyTrend(trendQuery()).getTotalCount());
    verify(queryApi, times(1)).query(anyString());
  }

  @Test
  void trendDistinguishesMissingNumericValuesFromZeroAndKeepsLongCounts() {
    FluxRecord record = mock(FluxRecord.class);
    when(record.getTime()).thenReturn(Instant.ofEpochSecond(1700000000L));
    when(record.getValueByKey("count")).thenReturn(3_000_000_001L);
    when(record.getValueByKey("numericCount")).thenReturn(3_000_000_000L);
    when(record.getValueByKey("min")).thenReturn(-2.0);
    when(record.getValueByKey("max")).thenReturn(2.0);
    when(record.getValueByKey("avg")).thenReturn(0.0);
    FluxRecord invalid = mock(FluxRecord.class);
    when(invalid.getTime()).thenReturn(Instant.ofEpochSecond(1700000004L));
    when(invalid.getValueByKey("count")).thenReturn(3L);
    when(invalid.getValueByKey("numericCount")).thenReturn(0L);
    FluxTable table = mock(FluxTable.class);
    when(table.getRecords()).thenReturn(List.of(record, invalid));
    when(queryApi.query(anyString())).thenReturn(List.of(table));
    PropertyTrendVO result = service.queryPropertyTrend(trendQuery());
    assertEquals(3_000_000_004L, result.getTotalCount());
    assertEquals(3_000_000_000L, result.getNumericCount());
    assertEquals(0, BigDecimal.ZERO.compareTo(result.getBuckets().get(0).getAvg()));
    assertNull(result.getBuckets().get(1).getMin());
  }

  @Test
  void allHistoryFailuresRemainFailures() {
    when(queryApi.query(anyString())).thenThrow(new RuntimeException("query timeout"));
    assertThrows(IoTException.class, () -> service.queryPropertyTrend(trendQuery()));
    assertThrows(IoTException.class, () -> service.queryLogMeta(trendQuery()));
    assertThrows(IoTException.class, () -> service.pageList(trendQuery()));
    assertThrows(
        IoTException.class, () -> new InfluxDBDeviceLogService().queryPropertyTrend(trendQuery()));
  }

  @Test
  void cursorPagesDoNotCountOrOffsetAndUseTimeAndId() {
    LogQuery query = trendQuery();
    query.setCursorMode(true);
    query.setPageSize(50);
    query.setPageNum(9000);
    query.setBeforeTime(
        LocalDateTime.ofInstant(Instant.ofEpochSecond(1700000050L), ZoneId.systemDefault()));
    query.setBeforeId(12345L);
    when(queryApi.query(anyString()))
        .thenAnswer(
            call -> {
              String flux = call.getArgument(0);
              assertTrue(flux.contains("stop: 2023-11-14T22:14:10.000000001Z"));
              assertTrue(flux.contains("r.id < 12345"));
              assertTrue(flux.contains("sort(columns: [\"_time\", \"id\"], desc: true)"));
              assertTrue(flux.contains("tail(n: 52)"));
              assertTrue(flux.contains("limit(n: 51)"));
              assertFalse(flux.contains("offset:"));
              assertFalse(flux.contains("count()"));
              return List.of();
            });
    assertEquals(-1L, service.queryLogMeta(query).getTotalCount());
    assertEquals(-1L, service.pageList(query).getTotalCount());
    verify(queryApi, times(2)).query(anyString());
  }

  @Test
  void cursorRefusesLegacyRowsWithoutStableIdentifiers() {
    LogQuery query = trendQuery();
    query.setCursorMode(true);
    FluxRecord record = mock(FluxRecord.class);
    when(record.getTime()).thenReturn(Instant.ofEpochSecond(1700000000L));
    FluxTable table = mock(FluxTable.class);
    when(table.getRecords()).thenReturn(List.of(record));
    when(queryApi.query(anyString())).thenReturn(List.of(table));
    assertThrows(IoTException.class, () -> service.queryLogMeta(query));
    assertThrows(IoTException.class, () -> service.pageList(query));
  }

  @Test
  void validatesAndEscapesSelectorsBeforeExecutingFlux() {
    PropertyTrendQuery query = trendQuery();
    query.setProperty("speed\"\n|> limit(n:1)");
    when(queryApi.query(anyString()))
        .thenAnswer(
            call -> {
              String flux = call.getArgument(0);
              assertTrue(flux.contains("r.property == \"speed\\\"\\n|> limit(n:1)\""));
              return List.of();
            });
    service.queryPropertyTrend(query);
    query.setEndCreateTime(query.getBeginCreateTime() + 32L * 86400);
    assertThrows(IoTException.class, () -> service.queryPropertyTrend(query));
    verify(queryApi, times(1)).query(anyString());
  }

  @Test
  void newWritesStoreFiniteTypedValuesWithoutDiscardingOriginalContent() throws Exception {
    Method parser =
        InfluxDBDeviceLogService.class.getDeclaredMethod("finiteNumericValue", String.class);
    parser.setAccessible(true);
    assertEquals(0.0, parser.invoke(service, "0"));
    assertEquals(-120.0, parser.invoke(service, " -1.2e2 "));
    assertEquals(Double.MAX_VALUE, parser.invoke(service, "1.7976931348623158e308"));
    for (String value :
        List.of("NaN", "Infinity", "1e999", "1.7976931348623159e308", "0x1.0p1", "{}", "true")) {
      assertNull(parser.invoke(service, value));
    }
    BaseUPRequest up = new BaseUPRequest();
    up.setProductKey("product");
    up.setDeviceId("device");
    up.setIotId("device");
    up.setTime(1700000000000L);
    Method builder =
        InfluxDBDeviceLogService.class.getDeclaredMethod(
            "buildPropertyMetadataPoint",
            BaseUPRequest.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class);
    builder.setAccessible(true);
    Point point = (Point) builder.invoke(service, up, "speed", "1.25", "Speed", "1.25", "km/h");
    String line = point.toLineProtocol();
    assertTrue(line.contains("content=\"1.25\""));
    assertTrue(line.contains("numericValue=1.25"));
    assertTrue(line.contains("numericCount=1i"));
    assertTrue(line.contains("sampleCount=1i"));
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "NEXIOT_INFLUX_LIVE_TEST", matches = "true")
  void liveFluxCombinesSeriesFiltersInvalidValuesAndKeepsInclusiveEnd() throws Exception {
    // 仅在真实引擎执行array.from查询，不写入/删除生产bucket数据。
    try (InfluxDBClient client = liveClient()) {
      PropertyTrendQuery query = trendQuery();
      query.validateRange();
      when(queryApi.query(anyString()))
          .thenAnswer(
              call -> {
                String flux = call.getArgument(0);
                String source = "import \"array\"\n";
                int from = flux.indexOf("from(bucket:");
                int afterFilters =
                    flux.indexOf("  |> filter(fn: (r) => r._field == \"content\")", from);
                source += flux.substring(0, from);
                source +=
                    "array.from(rows:["
                        + arrayRow(0, "0", "a")
                        + ","
                        + arrayRow(0, "10", "b")
                        + ","
                        + arrayRow(1, "20", "b")
                        + ","
                        + arrayRow(2, "bad", "a")
                        + ","
                        + arrayRow(3, "1e999", "a")
                        + ","
                        + arrayRow(4, "-2", "a")
                        + ","
                        + arrayRow(60, "7", "a")
                        + ","
                        + arrayRow(61, "100", "a")
                        + "]) |> group(columns:[\"series\",\"_field\"]) |>"
                        + " range(start:2023-11-14T22:13:20Z,stop:2023-11-14T22:14:20.000000001Z)\n";
                return client.getQueryApi().query(source + flux.substring(afterFilters));
              });
      PropertyTrendVO result = service.queryPropertyTrend(query);
      assertEquals(7, result.getTotalCount());
      assertEquals(5, result.getNumericCount());
      assertEquals(3, result.getBuckets().size());
      assertEquals(5, result.getBuckets().get(0).getCount());
      assertEquals(3, result.getBuckets().get(0).getNumericCount());
      assertEquals(0, BigDecimal.TEN.compareTo(result.getBuckets().get(0).getAvg()));
      assertEquals(0, BigDecimal.ZERO.compareTo(result.getBuckets().get(0).getMin()));
      assertEquals(1700000060000L, result.getBuckets().get(2).getTime());
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "NEXIOT_INFLUX_LIVE_TEST", matches = "true")
  void liveFluxSafelyConvertsEveryFiniteDoubleBoundaryAndDoesNotOverflowMeans() throws Exception {
    try (InfluxDBClient client = liveClient()) {
      Field field = InfluxDBDeviceLogService.class.getDeclaredField("FINITE_NUMBER_FLUX");
      field.setAccessible(true);
      String functions = (String) field.get(null);
      List<String> values =
          List.of(
              "0",
              "-1.25",
              " 2.5e1 ",
              "abc",
              "NaN",
              "Infinity",
              "1e999",
              "1.7976931348623158e308",
              "1.7976931348623159e308",
              "-1.7976931348623159e308",
              "1e-999",
              "0e9999999999999999",
              "5e-324",
              ".5",
              "1.",
              "01",
              "+1",
              "0x123");
      List<Boolean> expected =
          List.of(
              true, true, true, false, false, false, false, true, false, false, true, true, true,
              true, true, true, true, false);
      String rows =
          values.stream()
              .map(v -> "{_value:\"" + v + "\"}")
              .collect(java.util.stream.Collectors.joining(","));
      List<FluxTable> tables =
          client
              .getQueryApi()
              .query(
                  "import \"array\"\n"
                      + functions
                      + "array.from(rows:["
                      + rows
                      + "]) |> map(fn: (r) => { n = finiteNumber(text:r._value) return"
                      + " {valid:n.valid, value:n.value} })");
      List<FluxRecord> records = tables.stream().flatMap(t -> t.getRecords().stream()).toList();
      assertEquals(values.size(), records.size());
      for (int i = 0; i < values.size(); i++) {
        assertEquals(expected.get(i), records.get(i).getValueByKey("valid"), values.get(i));
        assertTrue(Double.isFinite(((Number) records.get(i).getValueByKey("value")).doubleValue()));
      }
      List<FluxTable> means =
          client
              .getQueryApi()
              .query(
                  "import \"array\"\n"
                      + functions
                      + "array.from(rows:[{_value:float(v:\"1.7976931348623157e308\")},{_value:float(v:\"1.7976931348623157e308\")},{_value:float(v:\"1.7976931348623157e308\")},{_value:float(v:\"-1.7976931348623157e308\")}])"
                      + " |> reduce(identity:{count:0,avg:0.0},fn:(r,accumulator)=>({count:accumulator.count+1,avg:weightedMean(a:accumulator.avg,b:r._value,left:accumulator.count,right:1)}))");
      double average =
          ((Number) means.get(0).getRecords().get(0).getValueByKey("avg")).doubleValue();
      assertEquals(0.5, average / Double.MAX_VALUE, 1e-14);
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "NEXIOT_INFLUX_LIVE_TEST", matches = "true")
  void liveExistingDeviceAndEventLogsPageWithoutDuplicates() throws Exception {
    try (InfluxDBClient client = liveClient()) {
      setField("historyQueryClient", client);
      for (String measurement : List.of("device_log", "event_metadata")) {
        List<FluxRecord> samples =
            client
                .getQueryApi()
                .query(
                    "from(bucket: \"device_logs\")"
                        + " |> range(start:-31d) |> filter(fn:(r)=>r._measurement==\""
                        + measurement
                        + "\" and r._field==\"id\") |> last() |> group() |> limit(n:1)")
                .stream()
                .flatMap(t -> t.getRecords().stream())
                .toList();
        assertFalse(samples.isEmpty(), "测试实例缺少" + measurement + "带id记录");
        FluxRecord sample = samples.get(0);
        LogQuery query = new LogQuery();
        query.setProductKey((String) sample.getValueByKey("productKey"));
        query.setIotId((String) sample.getValueByKey("iotId"));
        query.setDeviceId((String) sample.getValueByKey("deviceId"));
        query.setBeginCreateTime(Instant.now().minusSeconds(31L * 86400).getEpochSecond());
        query.setEndCreateTime(Instant.now().getEpochSecond());
        query.setPageSize(10);
        query.setPageNum(1);
        query.setCursorMode(true);
        if (measurement.equals("event_metadata")) {
          query.setEvent((String) sample.getValueByKey("event"));
          query.setMessageType("EVENT");
          var first = service.queryLogMeta(query);
          assertEquals(-1, first.getTotalCount());
          assertFalse(first.getList().isEmpty());
          var tail = first.getList().get(first.getList().size() - 1);
          query.setBeforeTime(tail.getCreateTime());
          query.setBeforeId(tail.getId());
          var second = service.queryLogMeta(query);
          var ids = first.getList().stream().map(v -> v.getId()).toList();
          assertTrue(second.getList().stream().noneMatch(v -> ids.contains(v.getId())));
        } else {
          var first = service.pageList(query);
          assertEquals(-1, first.getTotalCount());
          assertFalse(first.getList().isEmpty());
          var tail = first.getList().get(first.getList().size() - 1);
          query.setBeforeTime(tail.getCreateTime());
          query.setBeforeId(tail.getId());
          var second = service.pageList(query);
          var ids = first.getList().stream().map(v -> v.getId()).toList();
          assertTrue(second.getList().stream().noneMatch(v -> ids.contains(v.getId())));
        }
      }
    }
  }

  private String arrayRow(int seconds, String value, String series) {
    return "{_time:time(v:"
        + (1700000000L + seconds)
        + "000000000),_field:\"content\",_value:\""
        + value
        + "\",series:\""
        + series
        + "\"}";
  }

  private InfluxDBClient liveClient() throws Exception {
    Path directory = Path.of("").toAbsolutePath();
    while (directory != null
        && !Files.isRegularFile(
            directory.resolve(
                "cn-universal-web/src/main/resources/application-postgresql.properties"))) {
      directory = directory.getParent();
    }
    assertNotNull(directory, "Spring配置文件缺失");
    Properties properties = new Properties();
    try (Reader reader =
        Files.newBufferedReader(
            directory.resolve(
                "cn-universal-web/src/main/resources/application-postgresql.properties"))) {
      properties.load(reader);
    }
    return InfluxDBClientFactory.create(
        properties.getProperty("influxdb.url"),
        properties.getProperty("influxdb.token").toCharArray(),
        properties.getProperty("influxdb.org"),
        properties.getProperty("influxdb.bucket"));
  }
}
