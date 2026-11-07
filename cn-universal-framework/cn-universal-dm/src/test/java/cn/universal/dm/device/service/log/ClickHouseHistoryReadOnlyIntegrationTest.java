package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "NEXIOT_CLICKHOUSE_LIVE_TEST", matches = "true")
class ClickHouseHistoryReadOnlyIntegrationTest {
  private static void set(Object target, String name, Object value) throws Exception {
    var field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  void realMillionSampleTrendMatchesRawValuesAndCursorHasNoDuplicate() throws Exception {
    Path root = Path.of(System.getProperty("user.dir"));
    while (!Files.exists(
        root.resolve("cn-universal-web/src/main/resources/application-postgresql.properties")))
      root = root.getParent();
    Properties props = new Properties();
    try (var reader =
        Files.newBufferedReader(
            root.resolve(
                "cn-universal-web/src/main/resources/application-postgresql.properties"))) {
      props.load(reader);
    }
    var ds = new DriverManagerDataSource();
    ds.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
    ds.setUrl(
        props.getProperty("clickhouse.address")
            + "?readonly=1&max_execution_time=8&socket_timeout=10000");
    ds.setUsername(props.getProperty("clickhouse.username"));
    ds.setPassword(props.getProperty("clickhouse.password"));
    JdbcTemplate jdbc = new JdbcTemplate(ds);
    jdbc.setQueryTimeout(8);
    String table =
        props.getProperty("clickhouse.table.iot_device_log_metadata", "iot_device_log_metadata");
    CkDeviceLogService service = new CkDeviceLogService();
    set(service, "jdbcTemplate", jdbc);
    set(service, "devLogMetaTableName", table);
    // Inspect only; the test never runs the upgrade or writes data.
    boolean optimized =
        jdbc.queryForObject(
                "SELECT count() FROM system.projections WHERE database=currentDatabase() AND"
                    + " table=? AND name=?",
                Long.class,
                table,
                ClickHouseHistoryOptimizer.PROJECTION)
            > 0;
    assertTrue(optimized, "Run the reviewed additive history migration before this verification");
    ClickHouseHistoryOptimizer optimizer = new ClickHouseHistoryOptimizer(jdbc, table, false, 0);
    try {
      set(optimizer, "numericReady", true);
      set(optimizer, "projectionReady", true);
      for (String[] device :
          List.of(
              new String[] {"jt808", "jt808069509769807", "069509769807", "currentSpeed"},
              new String[] {
                "tGO00TmKRByI",
                "tGO00TmKRByImodbus_slave3_20260628",
                "modbus_slave3_20260628",
                "slave3Hr1"
              })) {
        PropertyTrendQuery query = new PropertyTrendQuery();
        query.setSamplingMode("exact");
        query.setProductKey(device[0]);
        query.setIotId(device[1]);
        query.setDeviceId(device[2]);
        query.setProperty(device[3]);
        query.setBeginCreateTime(1788192000L); // fixed September interval, no moving writes
        query.setEndCreateTime(1790179200L);
        set(service, "historyOptimizer", null);
        long start = System.nanoTime();
        PropertyTrendVO raw = service.queryPropertyTrend(query);
        long rawMs = (System.nanoTime() - start) / 1000000;
        set(service, "historyOptimizer", optimizer);
        start = System.nanoTime();
        PropertyTrendVO aggregated = service.queryPropertyTrend(query);
        long aggregateMs = (System.nanoTime() - start) / 1000000;
        assertEquals(raw.getTotalCount(), aggregated.getTotalCount());
        assertEquals(raw.getNumericCount(), aggregated.getNumericCount());
        assertEquals(raw.getBuckets().size(), aggregated.getBuckets().size());
        for (int i = 0; i < raw.getBuckets().size(); i++) {
          var left = raw.getBuckets().get(i);
          var right = aggregated.getBuckets().get(i);
          assertEquals(left.getTime(), right.getTime());
          assertEquals(left.getCount(), right.getCount());
          assertEquals(left.getNumericCount(), right.getNumericCount());
          assertEquals(left.getMin(), right.getMin());
          assertEquals(left.getMax(), right.getMax());
          if (left.getAvg() != null)
            assertEquals(
                left.getAvg().doubleValue(),
                right.getAvg().doubleValue(),
                Math.max(1e-9, Math.abs(left.getAvg().doubleValue()) * 1e-12));
        }
        System.out.printf(
            "CLICKHOUSE history samples=%d buckets=%d rawMs=%d projectionMs=%d%n",
            raw.getTotalCount(), raw.getBuckets().size(), rawMs, aggregateMs);
        LogQuery page = query;
        page.setPageSize(50);
        page.setCursorMode(true);
        HistoryPageSupport.prepare(page, true);
        var first = service.queryLogMeta(page);
        assertTrue(first.isHasMore());
        page.setCursor(first.getNextCursor());
        HistoryPageSupport.prepare(page, true);
        var second = service.queryLogMeta(page);
        var last = first.getList().getLast();
        var next = second.getList().getFirst();
        assertTrue(
            next.getCreateTime().isBefore(last.getCreateTime())
                || (next.getCreateTime().equals(last.getCreateTime())
                    && next.getId() < last.getId()));
        assertEquals(-1, second.getTotalCount());
      }
    } finally {
      optimizer.close();
    }
  }
}
