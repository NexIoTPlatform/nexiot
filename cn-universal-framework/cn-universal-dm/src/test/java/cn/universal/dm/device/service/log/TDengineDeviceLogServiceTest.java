package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.exception.IoTException;
import cn.universal.core.message.UPRequest;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.entity.IoTDeviceLogMetadata;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class TDengineDeviceLogServiceTest {

  @Test
  void shouldUseSeriesKeyCompositeKeyForTdengine3MetadataSchema() throws Exception {
    TDengineDeviceLogService service = newService();

    String logStableSql = invokeStringMethod(service, "buildCreateDeviceLogSTableSql");
    String metaStableSql = invokeStringMethod(service, "buildCreateMetadataSTableSql");

    assertTrue(logStableSql.contains("id BIGINT"));
    assertTrue(metaStableSql.contains("series_key VARCHAR(192) COMPOSITE KEY"));
    assertTrue(metaStableSql.indexOf("ts TIMESTAMP") < metaStableSql.indexOf("series_key"));
    assertTrue(metaStableSql.indexOf("series_key") < metaStableSql.indexOf("id BIGINT"));
  }

  @Test
  void shouldUseSeriesKeyWithSameTimestampForTdengine3MetadataRows() throws Exception {
    TDengineDeviceLogService service = newService();
    LocalDateTime createTime = LocalDateTime.of(2026, 4, 23, 10, 0);
    IoTDeviceLogMetadata temperature = metadata(1L, "temperature", createTime);
    IoTDeviceLogMetadata humidity = metadata(2L, "humidity", createTime);

    String subTableName = invokeMetadataSubTableName(service, temperature);
    String sql = invokeBuildMetadataBatchSql(service, subTableName, List.of(temperature, humidity));
    String epochMillis = String.valueOf(toEpochMilli(createTime));

    assertTrue(sql.contains("INSERT INTO iot_log.t_meta_"));
    assertTrue(
        sql.contains(
            "(ts, series_key, id, device_name, message_type, event, property, content, ext1, ext2,"
                + " ext3) VALUES"));
    assertTrue(sql.contains("(" + epochMillis + ", 'p:temperature', 1,"));
    assertTrue(sql.contains("(" + epochMillis + ", 'p:humidity', 2,"));
    assertEquals(2, countOccurrences(sql, epochMillis));
    assertEquals(createTime, temperature.getCreateTime());
    assertEquals(createTime, humidity.getCreateTime());
  }

  @Test
  void shouldSplitTdengine2MetadataSubTablesBySeriesKey() throws Exception {
    TDengineDeviceLogService service = newService();
    setField(service, "tdengineVersion", 2);
    LocalDateTime createTime = LocalDateTime.of(2026, 4, 23, 10, 0);
    IoTDeviceLogMetadata temperature = metadata(1L, "temperature", createTime);
    IoTDeviceLogMetadata humidity = metadata(2L, "humidity", createTime);

    String temperatureSubTable = invokeMetadataSubTableName(service, temperature);
    String humiditySubTable = invokeMetadataSubTableName(service, humidity);
    String sql = invokeBuildMetadataBatchSql(service, temperatureSubTable, List.of(temperature));
    String epochMillis = String.valueOf(toEpochMilli(createTime));

    assertTrue(invokeStringMethod(service, "buildCreateMetadataSTableSql").contains("id BIGINT"));
    assertTrue(
        !invokeStringMethod(service, "buildCreateMetadataSTableSql").contains("COMPOSITE KEY"));
    assertTrue(!temperatureSubTable.equals(humiditySubTable));
    assertTrue(
        sql.contains(
            "(ts, id, device_name, message_type, event, property, content, ext1, ext2, ext3)"
                + " VALUES"));
    assertTrue(sql.contains("(" + epochMillis + ", 1,"));
  }

  @Test
  void shouldIgnoreEmptyPropertyPolicyWithoutTouchingTdengine() throws Exception {
    TDengineDeviceLogService service = newService();
    UPRequest request = new UPRequest();
    request.setMessageType(MessageType.PROPERTIES);
    request.setIotId("iot-001");
    request.setProductKey("pk001");
    request.setDeviceId("dev001");
    request.setDeviceName("device001");
    request.setProperties(Map.of("temperature", 26.5));
    IoTProduct product = IoTProduct.builder().metadata("{}").build();

    Method method =
        TDengineDeviceLogService.class.getDeclaredMethod(
            "saveLogStorePolicy", LogStorePolicyDTO.class, UPRequest.class, IoTProduct.class);
    method.setAccessible(true);

    assertDoesNotThrow(() -> method.invoke(service, null, request, product));
  }

  @Test
  void shouldIgnoreEventWhenEventPolicyIsEmpty() throws Exception {
    TDengineDeviceLogService service = newService();
    UPRequest request = new UPRequest();
    request.setMessageType(MessageType.EVENT);
    request.setIotId("iot-001");
    request.setProductKey("pk001");
    request.setDeviceId("dev001");
    request.setDeviceName("device001");
    request.setEvent("alarm");
    request.setEventName("alarm name");
    IoTProduct product = IoTProduct.builder().metadata("{}").build();

    Method method =
        TDengineDeviceLogService.class.getDeclaredMethod(
            "saveLogStorePolicy", LogStorePolicyDTO.class, UPRequest.class, IoTProduct.class);
    method.setAccessible(true);

    assertDoesNotThrow(() -> method.invoke(service, null, request, product));
  }

  @Test
  void trendUsesNativeAlignedWindowsAndDoesNotTurnInvalidContentIntoZero() throws Exception {
    TDengineDeviceLogService service = newService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    setField(service, "jdbcTemplate", jdbc);
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp("bucket_time")).thenReturn(new Timestamp(100_000L));
    when(rs.getLong("total_count")).thenReturn(1_000_000L);
    when(rs.getLong("numeric_count")).thenReturn(999_998L);
    when(rs.getBigDecimal("min_value")).thenReturn(new BigDecimal("-12.5"));
    when(rs.getBigDecimal("max_value")).thenReturn(new BigDecimal("51.5"));
    when(rs.getBigDecimal("avg_value")).thenReturn(new BigDecimal("19.5"));
    AtomicReference<String> executed = new AtomicReference<>();
    doAnswer(
            invocation -> {
              executed.set(invocation.getArgument(0));
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(rs, 0));
            })
        .when(jdbc)
        .query(anyString(), any(RowMapper.class));

    PropertyTrendVO result = service.queryPropertyTrend(trendQuery());

    assertEquals(1_000_000L, result.getTotalCount());
    assertEquals(999_998L, result.getNumericCount());
    assertEquals(new BigDecimal("-12.5"), result.getBuckets().getFirst().getMin());
    assertEquals(new BigDecimal("51.5"), result.getBuckets().getFirst().getMax());
    assertTrue(executed.get().contains("CASE WHEN TRIM(content) MATCH"));
    assertTrue(executed.get().contains("ELSE NULL END"));
    assertTrue(executed.get().contains("iot_id = 'iot-001'"));
    assertTrue(executed.get().contains("message_type = 'PROPERTIES'"));
    assertTrue(executed.get().contains("INTERVAL(6s, 4s)"));
    assertTrue(executed.get().endsWith("LIMIT 21"));
    assertFalse(executed.get().contains("OFFSET"));
  }

  @Test
  void upgradedTrendReadsTypedNewValuesAndFallsBackForHistoricalRows() throws Exception {
    TDengineDeviceLogService service = newService();
    setField(service, "numericColumnsAvailable", true);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    setField(service, "jdbcTemplate", jdbc);
    AtomicReference<String> executed = new AtomicReference<>();
    doAnswer(
            invocation -> {
              executed.set(invocation.getArgument(0));
              return List.of();
            })
        .when(jdbc)
        .query(anyString(), any(RowMapper.class));

    service.queryPropertyTrend(trendQuery());

    assertTrue(executed.get().contains("WHEN numeric_valid = 1 THEN numeric_value"));
    assertTrue(executed.get().contains("WHEN numeric_valid = 0 THEN NULL ELSE CASE"));
  }

  @Test
  void typedWritesExcludeNonFiniteAndMalformedNumbers() throws Exception {
    TDengineDeviceLogService service = newService();
    setField(service, "numericColumnsAvailable", true);
    LocalDateTime time = LocalDateTime.of(2026, 9, 1, 12, 0);
    IoTDeviceLogMetadata valid = metadata(1L, "temperature", time);
    valid.setContent("1.2e3");
    IoTDeviceLogMetadata invalid = metadata(2L, "temperature", time.plusSeconds(1));
    invalid.setContent("oops");
    IoTDeviceLogMetadata infinite = metadata(3L, "temperature", time.plusSeconds(2));
    infinite.setContent("1e999");
    String sql =
        invokeBuildMetadataBatchSql(service, "test_table", List.of(valid, invalid, infinite));

    assertTrue(sql.contains("ext3, numeric_value, numeric_valid)"));
    assertTrue(sql.contains(", 1200.0, 1)"));
    assertEquals(2, countOccurrences(sql, ", NULL, 0)"));
  }

  @Test
  void queryFailuresRemainVisibleInsteadOfReturningEmptyHistory() throws Exception {
    TDengineDeviceLogService service = newService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    setField(service, "jdbcTemplate", jdbc);
    doAnswer(
            invocation -> {
              throw new QueryTimeoutException("timeout");
            })
        .when(jdbc)
        .query(anyString(), any(RowMapper.class));
    LogQuery query = trendQuery();
    query.setCursorMode(true);
    assertThrows(IoTException.class, () -> service.queryPropertyTrend(trendQuery()));
    assertThrows(IoTException.class, () -> service.queryLogMeta(query));
    assertThrows(IoTException.class, () -> service.pageList(query));
  }

  @Test
  void cursorReadsUseStableTimeIdOrderingWithoutCountOrOffset() throws Exception {
    TDengineDeviceLogService service = newService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    setField(service, "jdbcTemplate", jdbc);
    List<String> queries = new ArrayList<>();
    java.sql.Connection connection = mock(java.sql.Connection.class);
    java.sql.Statement statement = mock(java.sql.Statement.class);
    ResultSet rows = mock(ResultSet.class);
    when(connection.createStatement()).thenReturn(statement);
    doAnswer(
            invocation -> {
              queries.add(invocation.getArgument(0));
              return rows;
            })
        .when(statement)
        .executeQuery(anyString());
    doAnswer(
            invocation ->
                ((org.springframework.jdbc.core.ConnectionCallback<?>) invocation.getArgument(0))
                    .doInConnection(connection))
        .when(jdbc)
        .execute(any(org.springframework.jdbc.core.ConnectionCallback.class));
    doAnswer(
            invocation -> {
              queries.add(invocation.getArgument(0));
              return List.of();
            })
        .when(jdbc)
        .query(anyString(), any(RowMapper.class));
    LogQuery query = trendQuery();
    query.setCursorMode(true);
    query.setPageSize(50);
    query.setBeforeTime(LocalDateTime.of(2026, 9, 1, 12, 0));
    query.setBeforeId(99L);
    query.setId("95");

    service.queryLogMeta(query);
    service.pageList(query);

    assertEquals(2, queries.size());
    for (String sql : queries) {
      assertTrue(sql.contains("AND id < 99)"));
      assertTrue(sql.contains("AND id = 95"));
      assertFalse(sql.contains("COUNT("));
      assertFalse(sql.contains("OFFSET"));
    }
    assertTrue(queries.get(0).endsWith("ORDER BY ts DESC LIMIT 51"));
    assertTrue(queries.get(1).endsWith("ORDER BY ts DESC, id DESC LIMIT 51"));
    verify(jdbc, never()).queryForObject(anyString(), any(Class.class));
  }

  @Test
  void queryByIdAcceptsStringIdAndAppliesOptionalScopeFilters() throws Exception {
    TDengineDeviceLogService service = newService();
    JdbcTemplate normal = mock(JdbcTemplate.class);
    JdbcTemplate history = mock(JdbcTemplate.class);
    setField(service, "jdbcTemplate", normal);
    setField(service, "historyJdbcTemplate", history);
    IoTDeviceLogVO expected = new IoTDeviceLogVO();
    expected.setId(123L);
    AtomicReference<String> executed = new AtomicReference<>();
    doAnswer(
            invocation -> {
              executed.set(invocation.getArgument(0));
              return List.of(expected);
            })
        .when(history)
        .query(anyString(), any(RowMapper.class));

    LogQuery query =
        LogQuery.builder()
            .id("123")
            .productKey("pk001")
            .iotId("iot-001")
            .deviceId("dev001")
            .beginCreateTime(100L)
            .endCreateTime(200L)
            .build();

    assertSame(expected, service.queryById(query));
    assertTrue(executed.get().contains("id = 123"));
    assertTrue(executed.get().contains("product_key = 'pk001'"));
    assertTrue(executed.get().contains("iot_id = 'iot-001'"));
    assertTrue(executed.get().contains("device_id = 'dev001'"));
    assertTrue(executed.get().contains("ts >= 100000"));
    assertTrue(executed.get().contains("ts <= 200000"));
    assertTrue(executed.get().endsWith("LIMIT 1"));
    verify(history).query(anyString(), any(RowMapper.class));
    verifyNoInteractions(normal);
  }

  @Test
  void queryByIdKeepsIdOnlyCompatibilityAndReturnsNullOnlyForMissingRows() throws Exception {
    TDengineDeviceLogService service = newService();
    JdbcTemplate history = mock(JdbcTemplate.class);
    setField(service, "historyJdbcTemplate", history);
    when(history.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

    assertNull(service.queryById(LogQuery.builder().id("123").build()));
    verify(history).query(anyString(), any(RowMapper.class));
  }

  @Test
  void queryByIdRejectsMissingOrInvalidIdsBeforeQuerying() throws Exception {
    TDengineDeviceLogService service = newService();
    JdbcTemplate history = mock(JdbcTemplate.class);
    setField(service, "historyJdbcTemplate", history);

    assertEquals(400, assertThrows(IoTException.class, () -> service.queryById(null)).getCode());
    assertEquals(
        400, assertThrows(IoTException.class, () -> service.queryById(new LogQuery())).getCode());
    for (String id : List.of("", "  ", "not-a-number", "123 OR 1=1", "9223372036854775808")) {
      IoTException failure =
          assertThrows(
              IoTException.class, () -> service.queryById(LogQuery.builder().id(id).build()));
      assertEquals(400, failure.getCode());
    }
    verifyNoInteractions(history);
  }

  @Test
  void queryByIdSurfacesHistoryTimeoutsAsServiceFailures() throws Exception {
    TDengineDeviceLogService service = newService();
    JdbcTemplate history = mock(JdbcTemplate.class);
    setField(service, "historyJdbcTemplate", history);
    QueryTimeoutException timeout = new QueryTimeoutException("timeout");

    doAnswer(
            invocation -> {
              throw timeout;
            })
        .when(history)
        .query(anyString(), any(RowMapper.class));
    IoTException failure =
        assertThrows(
            IoTException.class,
            () -> service.queryById(LogQuery.builder().id("123").build()));
    assertEquals(503, failure.getCode());
    assertSame(timeout, failure.getCause());
  }

  @Test
  void cursorRowsPreserveSqlNullIdsAndFailInsteadOfIssuingAnUnsafeCursor() throws Exception {
    TDengineDeviceLogService service = newService();
    JdbcTemplate history = mock(JdbcTemplate.class);
    setField(service, "historyJdbcTemplate", history);
    ResultSet rs = mock(ResultSet.class);
    when(rs.getTimestamp("ts")).thenReturn(new Timestamp(100_123L));
    when(rs.getLong("id")).thenReturn(0L);
    when(rs.wasNull()).thenReturn(true);
    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(rs, 0), mapper.mapRow(rs, 1));
            })
        .when(history)
        .query(anyString(), any(RowMapper.class));
    LogQuery query = trendQuery();
    query.setCursorMode(true);
    query.setPageSize(1);

    assertThrows(IoTException.class, () -> service.pageList(query));
    assertThrows(IoTException.class, () -> service.queryLogMeta(query));
  }

  @Test
  void numericSchemaUpgradeAddsOnlyMissingColumnsAndIsIdempotent() throws Exception {
    TDengineDeviceLogService service = newService();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    setField(service, "jdbcTemplate", jdbc);
    List<String> columns = new ArrayList<>(List.of("ts", "numeric_value"));
    doAnswer(invocation -> new ArrayList<>(columns))
        .when(jdbc)
        .query(anyString(), any(RowMapper.class));
    doAnswer(
            invocation -> {
              columns.add("numeric_valid");
              return null;
            })
        .when(jdbc)
        .execute("ALTER STABLE iot_log.iot_device_log_metadata ADD COLUMN numeric_valid TINYINT");
    Method upgrade =
        TDengineDeviceLogService.class.getDeclaredMethod("upgradeMetadataNumericColumns");
    upgrade.setAccessible(true);

    upgrade.invoke(service);
    upgrade.invoke(service);

    verify(jdbc)
        .execute("ALTER STABLE iot_log.iot_device_log_metadata ADD COLUMN numeric_valid TINYINT");
    verify(jdbc, never())
        .execute("ALTER STABLE iot_log.iot_device_log_metadata ADD COLUMN numeric_value DOUBLE");
    assertTrue(invokeStringMethod(service, "buildMetadataInsertColumns").contains("numeric_valid"));
  }

  private static PropertyTrendQuery trendQuery() {
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setSamplingMode("exact");
    query.setProductKey("pk001");
    query.setIotId("iot-001");
    query.setProperty("temperature");
    query.setBeginCreateTime(100L);
    query.setEndCreateTime(200L);
    query.setMaxPoints(20);
    return query;
  }

  private static TDengineDeviceLogService newService() throws Exception {
    TDengineDeviceLogService service = new TDengineDeviceLogService();
    setField(service, "databaseName", "iot_log");
    setField(service, "devLogTableName", "iot_device_log");
    setField(service, "devLogMetaTableName", "iot_device_log_metadata");
    setField(service, "tdengineVersion", 3);
    return service;
  }

  private static IoTDeviceLogMetadata metadata(Long id, String property, LocalDateTime createTime) {
    return IoTDeviceLogMetadata.builder()
        .id(id)
        .iotId("iot-001")
        .productKey("pk001")
        .deviceId("dev001")
        .deviceName("device001")
        .messageType(MessageType.PROPERTIES.name())
        .property(property)
        .content("1")
        .createTime(createTime)
        .build();
  }

  private static String invokeBuildMetadataBatchSql(
      TDengineDeviceLogService service,
      String subTableName,
      List<IoTDeviceLogMetadata> metadataRecords)
      throws Exception {
    Method method =
        TDengineDeviceLogService.class.getDeclaredMethod(
            "buildMetadataBatchSql", String.class, java.util.List.class);
    method.setAccessible(true);
    return (String) method.invoke(service, subTableName, metadataRecords);
  }

  private static String invokeMetadataSubTableName(
      TDengineDeviceLogService service, IoTDeviceLogMetadata metadata) throws Exception {
    Method method =
        TDengineDeviceLogService.class.getDeclaredMethod(
            "generateMetadataSubTableName", IoTDeviceLogMetadata.class);
    method.setAccessible(true);
    return (String) method.invoke(service, metadata);
  }

  private static String invokeStringMethod(TDengineDeviceLogService service, String methodName)
      throws Exception {
    Method method = TDengineDeviceLogService.class.getDeclaredMethod(methodName);
    method.setAccessible(true);
    return (String) method.invoke(service);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = TDengineDeviceLogService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static long toEpochMilli(LocalDateTime time) {
    return time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  private static int countOccurrences(String source, String value) {
    int count = 0;
    int index = 0;
    while ((index = source.indexOf(value, index)) >= 0) {
      count++;
      index += value.length();
    }
    return count;
  }
}
