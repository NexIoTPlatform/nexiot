package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PropertyTrendQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Real service + Flux + Java reduction over an isolated durable ten-million-point bucket. */
@EnabledIfEnvironmentVariable(named = "NEXIOT_INFLUX_MILLION_TEST", matches = "true")
class InfluxDBHistoryMillionIntegrationTest {
  private static final Path PRIVATE = Path.of("/tmp/nexiot-history-influx-20261013.private.json");
  private static final Path EVIDENCE = Path.of("/tmp/nexiot-history-influx-20261013-preparation.json");
  private static final Path RESULTS = Path.of("/tmp/nexiot-history-influx-20261013-results.json");
  private static final long START_MS = 1790630400000L;
  private static final long LAST_MS = START_MS + 9999999L * 100L;

  public static void main(String[] args) throws Exception {
    if (!"true".equals(System.getenv("NEXIOT_INFLUX_MILLION_TEST"))) {
      throw new IllegalStateException("Explicit isolated benchmark enablement is required");
    }
    new InfluxDBHistoryMillionIntegrationTest().completeServiceQueryAcrossPointsBudgetsAndInvalidAttributes();
  }

  @Test
  @SuppressWarnings("unchecked")
  void completeServiceQueryAcrossPointsBudgetsAndInvalidAttributes() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> preparation = mapper.readValue(Files.readString(EVIDENCE), Map.class);
    assertEquals(10000000, ((Number) preparation.get("numericPointsVerifiedAfterRestart")).intValue());
    Map<String, String> credentials = mapper.readValue(Files.readString(PRIVATE), Map.class);
    assertEquals("http://127.0.0.1:18925", credentials.get("url"));
    List<Map<String, Object>> results = new ArrayList<>();
    try (InfluxDBClient client = InfluxDBClientFactory.create(
        InfluxDBClientOptions.builder().url(credentials.get("url"))
            .authenticateToken(credentials.get("token").toCharArray())
            .org(credentials.get("org")).bucket(credentials.get("bucket"))
            .okHttpClient(new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS).callTimeout(8, TimeUnit.SECONDS))
            .build())) {
      InfluxDBDeviceLogService service = new InfluxDBDeviceLogService();
      field(service, "historyQueryClient", client);
      field(service, "bucket", credentials.get("bucket"));
      if ("boundary".equals(System.getenv("NEXIOT_INFLUX_CASES"))) {
        probe(service, results, mapper, "endpoint", LAST_MS / 1000, LAST_MS / 1000,
            100, 10000, "inclusive-nanosecond-boundary");
        assertTrue(results.stream().allMatch(r -> Boolean.TRUE.equals(r.get("success"))));
        return;
      }
      if ("detail".equals(System.getenv("NEXIOT_INFLUX_CASES"))) {
        for (int pageSize : new int[] {50, 200}) detailProbe(service, results, mapper, pageSize);
        assertTrue(results.stream().allMatch(r -> Boolean.TRUE.equals(r.get("success"))));
        return;
      }
      if ("detail-edge".equals(System.getenv("NEXIOT_INFLUX_CASES"))) {
        detailEdgeProbe(service, results, mapper);
        assertTrue(results.stream().allMatch(r -> Boolean.TRUE.equals(r.get("success"))));
        return;
      }
      for (int points : new int[] {100, 300, 600}) {
        for (int budget : new int[] {1000, 10000, 100000}) {
          probe(service, results, mapper, "numeric", START_MS / 1000, LAST_MS / 1000,
              points, budget, "million-range");
        }
      }
      // The 31-day range contains all ten million real points plus empty windows.
      probe(service, results, mapper, "numeric", START_MS / 1000 - 86400,
          START_MS / 1000 - 86400 + 31L * 86400, 100, 10000, "31-day-range");
      probe(service, results, mapper, "numeric", LAST_MS / 1000 - 3600,
          LAST_MS / 1000, 100, 10000, "last-hour");
      probe(service, results, mapper, "missing", START_MS / 1000,
          LAST_MS / 1000, 100, 10000, "empty-attribute");
      probe(service, results, mapper, "mixed", START_MS / 1000,
          LAST_MS / 1000, 100, 10000, "mixed-numeric-text");
      probe(service, results, mapper, "text", START_MS / 1000,
          LAST_MS / 1000, 100, 10000, "nonnumeric-attribute");
      // Writer precision is milliseconds; deliberate +0.5ms fixture must be excluded.
      long end = (LAST_MS / 1000) + 1;
      probe(service, results, mapper, "endpoint", end - 1, end - 1,
          100, 10000, "inclusive-nanosecond-boundary");
      probe(service, results, mapper, "exact-boundaries", START_MS / 1000,
          START_MS / 1000 + 60, 100, 10000, "exact-boundaries");
      probe(service, results, mapper, "numeric", START_MS / 1000,
          LAST_MS / 1000, 100, 10000, "million-range-exact-8s-probe");
      for (int pageSize : new int[] {50, 200}) detailProbe(service, results, mapper, pageSize);
    }
    assertTrue(results.stream().allMatch(r -> Boolean.TRUE.equals(r.get("success"))
            || (r.get("case").equals("million-range-exact-8s-probe")
                && "IoTException".equals(r.get("errorType"))
                && ((Number) r.get("elapsedMillis")).doubleValue() >= 7500
                && String.valueOf(r.get("error")).contains("超时"))),
        "Some genuine million-point service cases failed; inspect the sanitized results artifact");
  }

  private void probe(InfluxDBDeviceLogService service, List<Map<String, Object>> results,
      ObjectMapper mapper, String property, long begin, long end, int points, int budget,
      String caseName) throws Exception {
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setProductKey("historyMillion");
    query.setIotId("historyMilliondevice1");
    query.setDeviceId("device1");
    query.setProperty(property);
    query.setBeginCreateTime(begin);
    query.setEndCreateTime(end);
    query.setMaxPoints(points);
    query.setMaxScanRows(budget);
    query.setSamplingMode(caseName.contains("exact") ? "exact" : "sample");
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("case", caseName);
    result.put("property", property);
    result.put("begin", begin);
    result.put("end", end);
    result.put("maxPoints", points);
    result.put("maxScanRows", budget);
    result.put("samplingMode", query.getSamplingMode());
    result.put("windowCount", query.getSampleWindowCount());
    result.put("sampleFetchSize", query.getSampleFetchSize());
    long started = System.nanoTime();
    try {
      PropertyTrendVO trend = service.queryPropertyTrend(query);
      result.put("success", true);
      result.put("elapsedMillis", (System.nanoTime() - started) / 1000000.0);
      result.put("bucketCount", trend.getBuckets().size());
      result.put("totalCount", trend.getTotalCount());
      result.put("numericCount", trend.getNumericCount());
      result.put("scannedCount", trend.getScannedCount());
      result.put("sampled", trend.isSampled());
      result.put("countExact", trend.isCountExact());
      assertTrue(trend.getBuckets().size() <= points);
      if (query.isSampledQuery()) {
        assertTrue(trend.getTotalCount() <= budget);
        assertTrue(trend.getScannedCount() <= budget + query.getSampleWindowCount());
      }
      assertTrue(trend.getBuckets().stream().allMatch(b ->
          b.getTime() >= begin * 1000 && b.getTime() <= end * 1000));
      if (property.equals("numeric")) {
        if (query.isSampledQuery()) {
          assertTrue(trend.isSampled());
          assertFalse(trend.isCountExact());
          long expectedRetained = 0;
          long expectedFetched = 0;
          for (PropertyTrendQuery.SampleWindow window : query.getSampleWindows()) {
            long firstIndex = Math.max(0, Math.floorDiv(window.getBeginMillis() - START_MS + 99, 100));
            long lastIncludedMillis = Math.min(window.getEndExclusiveMillis() - 1, end * 1000);
            long lastIndex = Math.min(9999999, Math.floorDiv(lastIncludedMillis - START_MS, 100));
            long actualRows = Math.max(0, lastIndex - firstIndex + 1);
            expectedRetained += Math.min(query.getSamplesPerWindow(), actualRows);
            expectedFetched += Math.min(query.getSampleFetchSize(), actualRows);
          }
          result.put("expectedRetainedCount", expectedRetained);
          result.put("expectedFetchedCount", expectedFetched);
          assertEquals(expectedRetained, trend.getTotalCount());
          assertEquals(expectedFetched, trend.getScannedCount());
        } else {
          assertTrue(trend.isCountExact());
          assertEquals(9999991, trend.getTotalCount());
        }
        assertTrue(trend.getNumericCount() > 0);
        assertEquals(trend.getTotalCount(), trend.getNumericCount());
        assertTrue(trend.getBuckets().stream().allMatch(b -> b.getMin().doubleValue() >= 0
            && b.getMax().doubleValue() <= 999 && Double.isFinite(b.getAvg().doubleValue())));
      } else if (property.equals("missing")) {
        assertEquals(0, trend.getTotalCount());
        assertTrue(trend.isCountExact());
      } else if (property.equals("text")) {
        assertTrue(trend.getTotalCount() > 0);
        assertEquals(0, trend.getNumericCount());
        assertTrue(trend.getBuckets().stream().allMatch(b -> b.getAvg() == null));
      } else if (property.equals("mixed")) {
        assertTrue(trend.getNumericCount() > 0);
        assertTrue(trend.getNumericCount() < trend.getTotalCount());
      } else if (property.equals("endpoint")) {
        assertEquals(1, trend.getTotalCount());
        assertTrue(trend.isCountExact());
        assertEquals(7, trend.getBuckets().getFirst().getAvg().doubleValue());
      } else if (property.equals("exact-boundaries")) {
        assertTrue(trend.isCountExact());
        assertEquals(20, trend.getTotalCount());
        assertEquals(12, trend.getNumericCount());
        assertTrue(trend.getBuckets().stream().filter(b -> b.getNumericCount() == 0)
            .allMatch(b -> b.getMin() == null && b.getMax() == null && b.getAvg() == null));
      }
    } catch (Throwable failure) {
      result.put("success", false);
      result.put("elapsedMillis", (System.nanoTime() - started) / 1000000.0);
      result.put("errorType", failure.getClass().getSimpleName());
      result.put("error", failure.getMessage());
    }
    results.add(result);
    mapper.writerWithDefaultPrettyPrinter().writeValue(RESULTS.toFile(), results);
    System.out.println("INFLUX_MILLION_CASE " + mapper.writeValueAsString(result));
  }

  private void detailProbe(InfluxDBDeviceLogService service, List<Map<String, Object>> results,
      ObjectMapper mapper, int pageSize) throws Exception {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("case", "cursor-detail-" + pageSize);
    result.put("pageSize", pageSize);
    long started = System.nanoTime();
    try {
      List<Double> elapsed = new ArrayList<>();
      PageBean<IoTDeviceLogMetadataVO> first = detailPage(service, pageSize, null, elapsed);
      PageBean<IoTDeviceLogMetadataVO> next = detailPage(service, pageSize, first.getNextCursor(), elapsed);
      PageBean<IoTDeviceLogMetadataVO> back = detailPage(service, pageSize, null, elapsed);
      assertEquals(pageSize, first.getList().size());
      assertEquals(pageSize, next.getList().size());
      assertTrue(first.isHasMore());
      assertTrue(next.isHasMore());
      assertEquals(-1L, first.getTotalCount());
      List<Long> firstIds = first.getList().stream().map(IoTDeviceLogMetadataVO::getId).toList();
      List<Long> nextIds = next.getList().stream().map(IoTDeviceLogMetadataVO::getId).toList();
      long newestId = Math.floorDiv((LAST_MS / 1000) * 1000 - START_MS, 100) + 1;
      for (int index = 0; index < pageSize; index++) {
        assertEquals(newestId - index, firstIds.get(index));
        assertEquals(newestId - pageSize - index, nextIds.get(index));
      }
      HashSet<Long> seen = new HashSet<>(firstIds);
      assertTrue(nextIds.stream().noneMatch(seen::contains));
      assertEquals(firstIds, back.getList().stream().map(IoTDeviceLogMetadataVO::getId).toList());
      assertTrue(next.getList().getFirst().getCreateTime().isBefore(first.getList().getLast().getCreateTime()));
      result.put("success", true);
      result.put("pageElapsedMillis", elapsed);
      result.put("returnedRows", first.getList().size() + next.getList().size() + back.getList().size());
      result.put("noDuplicates", true);
      result.put("backPageStable", true);
    } catch (Throwable failure) {
      result.put("success", false);
      result.put("errorType", failure.getClass().getSimpleName());
      result.put("error", failure.getMessage());
    }
    result.put("elapsedMillis", (System.nanoTime() - started) / 1000000.0);
    results.add(result);
    mapper.writerWithDefaultPrettyPrinter().writeValue(RESULTS.toFile(), results);
    System.out.println("INFLUX_MILLION_CASE " + mapper.writeValueAsString(result));
  }

  private void detailEdgeProbe(InfluxDBDeviceLogService service, List<Map<String, Object>> results,
      ObjectMapper mapper) throws Exception {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("case", "cursor-detail-small-boundary-fixture");
    try {
      LogQuery query = new LogQuery();
      query.setProductKey("historyMillion");
      query.setIotId("historyMilliondevice1");
      query.setDeviceId("device1");
      query.setMessageType("PROPERTIES");
      query.setProperty("exact-boundaries");
      query.setBeginCreateTime(START_MS / 1000);
      query.setEndCreateTime(START_MS / 1000 + 60);
      query.setPageSize(5);
      query.setCursorMode(true);
      List<Long> ids = new ArrayList<>();
      do {
        HistoryPageSupport.prepare(query, true);
        PageBean<IoTDeviceLogMetadataVO> page = service.queryLogMeta(query);
        ids.addAll(page.getList().stream().map(IoTDeviceLogMetadataVO::getId).toList());
        if (!page.isHasMore()) break;
        assertNotNull(page.getNextCursor());
        query.setCursor(page.getNextCursor());
        assertTrue(ids.size() <= 20);
      } while (true);
      assertEquals(20, ids.size());
      for (int i = 0; i < 20; i++) assertEquals(20000019L - i, ids.get(i));
      query.setProperty("cursor-ties");
      query.setCursor(null);
      List<Long> tiedIds = new ArrayList<>();
      do {
        HistoryPageSupport.prepare(query, true);
        PageBean<IoTDeviceLogMetadataVO> page = service.queryLogMeta(query);
        tiedIds.addAll(page.getList().stream().map(IoTDeviceLogMetadataVO::getId).toList());
        if (!page.isHasMore()) break;
        assertNotNull(page.getNextCursor());
        query.setCursor(page.getNextCursor());
        assertTrue(tiedIds.size() <= 12);
      } while (true);
      assertEquals(12, tiedIds.size());
      for (int i = 0; i < 12; i++) assertEquals(30000011L - i, tiedIds.get(i));
      query.setProperty("endpoint");
      query.setBeginCreateTime(LAST_MS / 1000);
      query.setEndCreateTime(LAST_MS / 1000);
      query.setCursor(null);
      HistoryPageSupport.prepare(query, true);
      PageBean<IoTDeviceLogMetadataVO> endpoint = service.queryLogMeta(query);
      assertEquals(1, endpoint.getList().size());
      assertEquals("7", endpoint.getList().getFirst().getContent());
      assertFalse(endpoint.isHasMore());
      result.put("success", true);
      result.put("exactIdsInDescendingOrder", ids);
      result.put("sameTimestampMultipleSeriesIds", tiedIds);
      result.put("inclusiveEndpointCount", endpoint.getList().size());
      result.put("excludedPointAfterEndpoint", true);
    } catch (Throwable failure) {
      result.put("success", false);
      result.put("errorType", failure.getClass().getSimpleName());
      result.put("error", failure.getMessage());
    }
    results.add(result);
    mapper.writerWithDefaultPrettyPrinter().writeValue(RESULTS.toFile(), results);
    System.out.println("INFLUX_MILLION_CASE " + mapper.writeValueAsString(result));
  }

  private PageBean<IoTDeviceLogMetadataVO> detailPage(InfluxDBDeviceLogService service,
      int pageSize, String cursor, List<Double> elapsed) {
    LogQuery query = new LogQuery();
    query.setProductKey("historyMillion");
    query.setIotId("historyMilliondevice1");
    query.setDeviceId("device1");
    query.setProperty("numeric");
    query.setMessageType("PROPERTIES");
    query.setBeginCreateTime(START_MS / 1000);
    query.setEndCreateTime(LAST_MS / 1000);
    query.setPageSize(pageSize);
    query.setCursorMode(true);
    query.setCursor(cursor);
    HistoryPageSupport.prepare(query, true);
    long started = System.nanoTime();
    PageBean<IoTDeviceLogMetadataVO> result = service.queryLogMeta(query);
    elapsed.add((System.nanoTime() - started) / 1000000.0);
    return result;
  }

  private static void field(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
