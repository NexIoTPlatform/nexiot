package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ClickHouseHistoryOptimizerTest {
  // Format observed in the configured ClickHouse system.columns/system.projections, using SELECT
  // only.
  private static final String LIVE_EXPRESSION =
      "if(isFinite(toFloat64OrNull(content)), toFloat64OrNull(content), NULL)";
  private static final String LIVE_PROJECTION =
      "SELECT iot_id, product_key, device_id, property, message_type,"
          + " toStartOfMinute(create_time) AS minute, count(), count(numeric_value),"
          + " min(numeric_value), max(numeric_value), sum(numeric_value)"
          + " GROUP BY iot_id, product_key, device_id, property, message_type, minute";

  @Test
  void acceptsObservedSchemaWithoutIssuingDdl() {
    CapturingJdbc jdbc = new CapturingJdbc();
    try (var optimizer = optimizer(jdbc, false)) {
      optimizer.upgrade();
      assertTrue(optimizer.numericReady());
      assertTrue(optimizer.projectionReady());
      assertEquals("new-parts-only", optimizer.metrics().get("status"));
      assertTrue(jdbc.statements.isEmpty());
    }
  }

  @Test
  void acceptsFormattingQuotedIdentifiersAliasesAndEquivalentExpressionOrder() {
    String equivalent =
        "select SUM(`numeric_value`) as total, COUNT() as samples,"
            + " MIN(\"numeric_value\") as low, max(numeric_value) as high,"
            + " count(numeric_value) as numbers, `property`, device_id, iot_id, product_key,"
            + " message_type, toStartOfMinute(`create_time`) AS `minute_bucket`"
            + " group by message_type, minute_bucket, product_key, iot_id, device_id, property;";
    assertTrue(ClickHouseHistoryOptimizer.compatibleProjection(projection(equivalent)));
    assertTrue(
        ClickHouseHistoryOptimizer.compatibleProjection(
            projection(LIVE_PROJECTION.replace(", minute", ", toStartOfMinute(create_time)"))));
    assertTrue(
        ClickHouseHistoryOptimizer.compatibleNumericColumn(
            column(
                "Nullable(Float64)",
                "MATERIALIZED",
                LIVE_EXPRESSION.replace("content", "`content`"))));
  }

  @Test
  void rejectsWrongColumnTypeKindAndCalculationWithoutDdlOrBackfill() {
    for (Map<String, Object> column :
        List.of(
            column("Float64", "MATERIALIZED", LIVE_EXPRESSION),
            column("Nullable(Float64)", "DEFAULT", LIVE_EXPRESSION),
            column("Nullable(Float64)", "", ""),
            column("Nullable(Float64)", "MATERIALIZED", "toFloat64OrZero(content)"),
            column(
                "Nullable(Float64)",
                "MATERIALIZED",
                LIVE_EXPRESSION.replace("content", "CONTENT")))) {
      CapturingJdbc jdbc = new CapturingJdbc();
      jdbc.column = column;
      try (var optimizer = optimizer(jdbc, true)) {
        optimizer.upgrade();
        assertConflict(optimizer, jdbc);
      }
    }
  }

  @Test
  void rejectsWrongAggregateGroupingAndUnexpectedSql() {
    for (String query :
        List.of(
            LIVE_PROJECTION.replace("sum(numeric_value)", "avg(numeric_value)"),
            LIVE_PROJECTION.replace("count(numeric_value)", "count(content)"),
            LIVE_PROJECTION.replace("toStartOfMinute", "toStartOfHour"),
            LIVE_PROJECTION.replace(" AS minute", " AS iot_id"),
            LIVE_PROJECTION.replace(" GROUP BY", " WHERE numeric_value > 0 GROUP BY"),
            LIVE_PROJECTION + " HAVING count() > 1",
            LIVE_PROJECTION.replace("iot_id", "IOT_ID"),
            LIVE_PROJECTION.replace("sum(numeric_value)", "sum(numeric_value"))) {
      CapturingJdbc jdbc = new CapturingJdbc();
      jdbc.projection = projection(query);
      try (var optimizer = optimizer(jdbc, true)) {
        optimizer.upgrade();
        assertConflict(optimizer, jdbc);
      }
    }
    assertFalse(
        ClickHouseHistoryOptimizer.compatibleProjection(
            Map.of("type", "Normal", "query", LIVE_PROJECTION)));
  }

  @Test
  void checksAnExistingProjectionBeforeAddingAnyMissingColumn() {
    CapturingJdbc jdbc = new CapturingJdbc();
    jdbc.column = null;
    jdbc.projection = projection("SELECT content");
    try (var optimizer = optimizer(jdbc, true)) {
      optimizer.upgrade();
      assertConflict(optimizer, jdbc);
    }
  }

  @Test
  void validatesObjectsAfterIdempotentCreationBeforeDeclaringReady() {
    CapturingJdbc jdbc = new CapturingJdbc();
    jdbc.column = null;
    jdbc.projection = null;
    try (var optimizer = optimizer(jdbc, false)) {
      optimizer.upgrade();
      assertTrue(optimizer.numericReady());
      assertTrue(optimizer.projectionReady());
      assertEquals(2, jdbc.statements.size());
      assertEquals(2, jdbc.columnReads);
      assertEquals(2, jdbc.projectionReads);
    }
  }

  @Test
  void refusesIncompatibleObjectCreatedByAnotherProcessBetweenInspectAndAdd() {
    CapturingJdbc jdbc = new CapturingJdbc();
    jdbc.column = null;
    jdbc.columnAfterAdd = column("Nullable(Float64)", "DEFAULT", "0");
    try (var optimizer = optimizer(jdbc, true)) {
      optimizer.upgrade();
      assertEquals("schema-conflict", optimizer.metrics().get("status"));
      assertFalse(optimizer.numericReady());
      assertFalse(optimizer.projectionReady());
      assertEquals(1, jdbc.statements.size());
      assertFalse(jdbc.mutationInspected);
    }
  }

  @Test
  void retriesDdlFailureAndRecoversWithoutRestart() {
    CapturingJdbc jdbc = new CapturingJdbc();
    jdbc.projection = null;
    jdbc.failDdl = true;
    try (var optimizer = optimizer(jdbc, false)) {
      optimizer.upgrade();
      assertEquals("retry-pending", optimizer.metrics().get("status"));
      assertFalse(optimizer.numericReady());
      assertFalse(optimizer.projectionReady());
      jdbc.failDdl = false;
      optimizer.upgrade();
      assertTrue(optimizer.numericReady());
      assertTrue(optimizer.projectionReady());
      assertEquals("new-parts-only", optimizer.metrics().get("status"));
      assertEquals(null, optimizer.metrics().get("lastError"));
    }
  }

  @Test
  void rechecksSchemaAndRecoversAfterExternalRepair() {
    CapturingJdbc jdbc = new CapturingJdbc();
    try (var optimizer = optimizer(jdbc, false)) {
      optimizer.upgrade();
      assertTrue(optimizer.projectionReady());
      jdbc.projection = projection(LIVE_PROJECTION.replace("sum(numeric_value)", "sum(content)"));
      optimizer.upgrade();
      assertConflict(optimizer, jdbc);
      jdbc.projection = projection(LIVE_PROJECTION);
      optimizer.upgrade();
      assertTrue(optimizer.numericReady());
      assertTrue(optimizer.projectionReady());
      assertEquals("new-parts-only", optimizer.metrics().get("status"));
    }
  }

  @Test
  void metadataReadFailureClearsStaleReadiness() {
    CapturingJdbc jdbc = new CapturingJdbc();
    try (var optimizer = optimizer(jdbc, false)) {
      optimizer.upgrade();
      jdbc.failMetadata = true;
      optimizer.upgrade();
      assertFalse(optimizer.numericReady());
      assertFalse(optimizer.projectionReady());
      assertEquals("retry-pending", optimizer.metrics().get("status"));
    }
  }

  @Test
  void reportsFailedPendingMutationWithoutSubmittingAnother() {
    CapturingJdbc jdbc = new CapturingJdbc();
    jdbc.mutations =
        List.of(Map.of("mutation_id", "mutation_42.txt", "latest_fail_reason", "Not enough space"));
    try (var optimizer = optimizer(jdbc, true)) {
      optimizer.upgrade();
      assertEquals("materialization-failed", optimizer.metrics().get("status"));
      assertEquals("mutation_42.txt: Not enough space", optimizer.metrics().get("lastError"));
      assertTrue(optimizer.numericReady());
      assertTrue(optimizer.projectionReady());
      assertTrue(jdbc.statements.isEmpty());
      optimizer.upgrade();
      assertEquals("materialization-failed", optimizer.metrics().get("status"));
      assertTrue(jdbc.statements.isEmpty());
      jdbc.mutations = List.of(Map.of("mutation_id", "mutation_42.txt", "latest_fail_reason", ""));
      optimizer.upgrade();
      assertEquals("materializing", optimizer.metrics().get("status"));
      assertEquals(null, optimizer.metrics().get("lastError"));
      assertTrue(jdbc.statements.isEmpty());
    }
  }

  private static void assertConflict(ClickHouseHistoryOptimizer optimizer, CapturingJdbc jdbc) {
    assertEquals("schema-conflict", optimizer.metrics().get("status"));
    assertFalse(optimizer.numericReady());
    assertFalse(optimizer.projectionReady());
    assertTrue(jdbc.statements.isEmpty());
    assertFalse(jdbc.mutationInspected);
  }

  private static ClickHouseHistoryOptimizer optimizer(CapturingJdbc jdbc, boolean backfill) {
    return new ClickHouseHistoryOptimizer(jdbc, "iot_device_log_metadata", backfill, 536870912L);
  }

  private static Map<String, Object> column(String type, String kind, String expression) {
    return Map.of("type", type, "default_kind", kind, "default_expression", expression);
  }

  private static Map<String, Object> projection(String query) {
    return Map.of("type", "Aggregate", "query", query);
  }

  private static class CapturingJdbc extends JdbcTemplate {
    Map<String, Object> column = column("Nullable(Float64)", "MATERIALIZED", LIVE_EXPRESSION);
    Map<String, Object> projection = projection(LIVE_PROJECTION);
    Map<String, Object> columnAfterAdd = column;
    List<String> statements = new ArrayList<>();
    List<Map<String, Object>> mutations = List.of();
    int columnReads;
    int projectionReads;
    boolean failDdl;
    boolean failMetadata;
    boolean mutationInspected;

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object... args) {
      if (failMetadata) throw new IllegalStateException("metadata unavailable");
      if (sql.contains("system.columns")) {
        columnReads++;
        return column == null ? List.of() : List.of(column);
      }
      if (sql.contains("system.projections")) {
        projectionReads++;
        return projection == null ? List.of() : List.of(projection);
      }
      if (sql.contains("system.mutations")) {
        mutationInspected = true;
        return mutations;
      }
      throw new AssertionError("Unexpected query: " + sql);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
      mutationInspected = true;
      throw new AssertionError("Should not inspect mutations for incompatible schema: " + sql);
    }

    @Override
    public void execute(String sql) {
      statements.add(sql);
      if (failDdl) throw new IllegalStateException("DDL temporarily unavailable");
      if (sql.contains("ADD COLUMN")) column = columnAfterAdd;
      else if (sql.contains("ADD PROJECTION")) projection = projection(LIVE_PROJECTION);
      else throw new AssertionError("Unexpected DDL: " + sql);
    }
  }
}
