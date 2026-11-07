package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Opt-in, strictly SELECT-only validation against the TDengine configured by Spring. */
@EnabledIfSystemProperty(named = "nexiot.test.tdengine.read-only", matches = "true")
class TDengineHistoryReadOnlyIntegrationTest {

  @Test
  void nativeWindowsAndCursorMatchExistingRows() throws Exception {
    Path root = Path.of("").toAbsolutePath();
    while (root != null
        && !Files.exists(
            root.resolve(
                "cn-universal-web/src/main/resources/application-postgresql.properties"))) {
      root = root.getParent();
    }
    if (root == null) {
      throw new IllegalStateException("Spring database configuration not found");
    }
    Properties config = new Properties();
    try (var reader =
        Files.newBufferedReader(
            root.resolve(
                "cn-universal-web/src/main/resources/application-postgresql.properties"))) {
      config.load(reader);
    }
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(
            config.getProperty("tdengine.url"),
            config.getProperty("tdengine.username"),
            config.getProperty("tdengine.password"));
    dataSource.setDriverClassName("com.taosdata.jdbc.rs.RestfulDriver");
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.setQueryTimeout(8);
    String database = config.getProperty("tdengine.database");
    String metadataTable = config.getProperty("tdengine.table.iot_device_log_metadata");
    String table = database + "." + metadataTable;
    String iotId = System.getProperty("nexiot.test.tdengine.iot-id");
    if (iotId == null || iotId.isBlank()) {
      List<String> devices =
          jdbc.query(
              "SELECT DISTINCT iot_id FROM " + table + " LIMIT 1", (rs, index) -> rs.getString(1));
      assertFalse(devices.isEmpty(), "Read-only integration requires existing metadata");
      iotId = devices.getFirst();
    }
    String device = escape(iotId);
    String selectedProperty = System.getProperty("nexiot.test.tdengine.property");
    String propertyFilter =
        selectedProperty == null || selectedProperty.isBlank()
            ? ""
            : " AND property='" + escape(selectedProperty) + "'";
    List<Map<String, Object>> latest =
        jdbc.queryForList(
            "SELECT ts, product_key, device_id, property FROM "
                + table
                + " WHERE iot_id = '"
                + device
                + "' AND message_type='PROPERTIES'"
                + propertyFilter
                + " ORDER BY ts DESC LIMIT 1");
    assertFalse(latest.isEmpty());
    Map<String, Object> record = latest.getFirst();
    String selectedBegin = System.getProperty("nexiot.test.tdengine.begin");
    String selectedEnd = System.getProperty("nexiot.test.tdengine.end");
    assertEquals(
        selectedBegin == null, selectedEnd == null, "Set both begin and end epoch seconds");
    long end =
        selectedEnd == null
            ? (((Timestamp) record.get("ts")).getTime() + 999L) / 1000L
            : Long.parseLong(selectedEnd);
    long begin = selectedBegin == null ? end - 3600L : Long.parseLong(selectedBegin);
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setSamplingMode("exact");
    query.setIotId(iotId);
    query.setProductKey(String.valueOf(record.get("product_key")));
    query.setDeviceId(String.valueOf(record.get("device_id")));
    query.setProperty(String.valueOf(record.get("property")));
    query.setBeginCreateTime(begin);
    query.setEndCreateTime(end);
    query.setMaxPoints(20);

    TDengineDeviceLogService service = new TDengineDeviceLogService();
    set(service, "databaseName", database);
    set(service, "devLogMetaTableName", metadataTable);
    set(service, "devLogTableName", config.getProperty("tdengine.table.iot_device_log"));
    set(service, "tdengineVersion", Integer.parseInt(config.getProperty("tdengine.version")));
    set(service, "historyJdbcTemplate", jdbc);
    // Do not call initDb(): integration checks must not execute CREATE/ALTER/INSERT.
    PropertyTrendVO trend = service.queryPropertyTrend(query);
    String where =
        " WHERE iot_id='"
            + device
            + "' AND product_key='"
            + escape(query.getProductKey())
            + "' AND device_id='"
            + escape(query.getDeviceId())
            + "' AND property='"
            + escape(query.getProperty())
            + "' AND message_type='PROPERTIES' AND ts >= "
            + query.getBeginCreateTime() * 1000L
            + " AND ts <= "
            + query.getEndCreateTime() * 1000L;
    long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + where, Long.class);
    assertEquals(count, trend.getTotalCount());
    assertTrue(count > 0);
    assertTrue(trend.getBuckets().size() <= 20);
    assertTrue(trend.getNumericCount() <= trend.getTotalCount());
    set(service, "tdengineVersion", 2);
    assertEquals(
        trend,
        service.queryPropertyTrend(query),
        "Compatibility branch must preserve all window counts and numeric aggregates");
    set(service, "tdengineVersion", 3);
    assertTrue(
        trend.getBuckets().stream()
            .allMatch(
                bucket ->
                    bucket.getTime() >= query.getBeginCreateTime() * 1000L
                        && bucket.getTime() <= query.getEndCreateTime() * 1000L));

    LogQuery pageQuery = query;
    pageQuery.setCursorMode(true);
    pageQuery.setPageSize(2);
    var first = service.queryLogMeta(pageQuery);
    assertTrue(first.getList().size() <= 2);
    if (count > 2) {
      var last = first.getList().getLast();
      pageQuery.setBeforeTime(last.getCreateTime());
      pageQuery.setBeforeId(last.getId());
      var second = service.queryLogMeta(pageQuery);
      assertFalse(second.getList().isEmpty());
      assertTrue(second.getList().stream().noneMatch(row -> row.getId().equals(last.getId())));
    }
    System.out.println(
        "TDengine SELECT-only history verified: iotId="
            + iotId
            + ", property="
            + query.getProperty()
            + ", begin="
            + begin
            + ", end="
            + end
            + ", records="
            + count
            + ", windows="
            + trend.getBuckets().size()
            + ", native cursor rows="
            + first.getList().size());
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("'", "\\'");
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = TDengineDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
