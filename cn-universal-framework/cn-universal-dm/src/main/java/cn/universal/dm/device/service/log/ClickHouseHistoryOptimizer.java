package cn.universal.dm.device.service.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Online, resumable history optimization. A projection belongs to its source part, so ClickHouse
 * combines materialized and original parts correctly while historical materialization is in
 * progress. No watermark, dual-write, or approximate result is needed.
 */
@Slf4j
final class ClickHouseHistoryOptimizer implements AutoCloseable {
  static final String PROJECTION = "history_minute_v1";
  static final String NUMERIC_EXPRESSION =
      "if(isFinite(toFloat64OrNull(content)),toFloat64OrNull(content),NULL)";
  private static final String PROJECTION_SELECT =
      "SELECT iot_id,product_key,device_id,property,message_type,toStartOfMinute(create_time) AS"
          + " minute,count(),count(numeric_value),min(numeric_value),max(numeric_value),sum(numeric_value)"
          + " GROUP BY iot_id,product_key,device_id,property,message_type,minute";
  private static final Set<String> PROJECTION_GROUPS =
      Set.of(
          "iot_id",
          "product_key",
          "device_id",
          "property",
          "message_type",
          "tostartofminute(create_time)");
  private static final Set<String> PROJECTION_VALUES =
      Set.of(
          "count()",
          "count(numeric_value)",
          "min(numeric_value)",
          "max(numeric_value)",
          "sum(numeric_value)");
  private static final Pattern PROJECTION_QUERY =
      Pattern.compile("(?is)^\\s*SELECT\\s+(.+?)\\s+GROUP\\s+BY\\s+(.+?)\\s*;?\\s*$");
  private static final Pattern ALIAS =
      Pattern.compile(
          "(?is)^(.+?)\\s+AS\\s+([a-zA-Z_][a-zA-Z0-9_]*|`[a-zA-Z_][a-zA-Z0-9_]*`|\"[a-zA-Z_][a-zA-Z0-9_]*\")\\s*$");
  private final JdbcTemplate jdbc;
  private final String table;
  private final boolean backfill;
  private final long maxPartitionBytes;
  private final ScheduledExecutorService worker =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread thread = new Thread(r, "clickhouse-history-upgrade");
            thread.setDaemon(true);
            return thread;
          });
  private volatile boolean numericReady;
  private volatile boolean projectionReady;
  private volatile String status = "pending";
  private volatile String lastError;

  ClickHouseHistoryOptimizer(
      JdbcTemplate jdbc, String table, boolean backfill, long maxPartitionBytes) {
    if (table == null || !table.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
      throw new IllegalArgumentException("ClickHouse history table must be a simple identifier");
    }
    this.jdbc = jdbc;
    this.table = table;
    this.backfill = backfill;
    this.maxPartitionBytes = Math.max(0, maxPartitionBytes);
  }

  void start(Runnable guardedUpgrade) {
    worker.scheduleWithFixedDelay(
        () -> {
          try {
            guardedUpgrade.run();
          } catch (Exception e) {
            status = "retry-pending";
            lastError = e.getClass().getSimpleName();
            log.warn("ClickHouse history upgrade coordinator unavailable; retrying later", e);
          }
        },
        5,
        30,
        TimeUnit.SECONDS);
  }

  boolean numericReady() {
    return numericReady;
  }

  boolean projectionReady() {
    return projectionReady;
  }

  void upgrade() {
    try {
      if (!ensureCompatibleSchema()) return;
      if (!backfill) {
        status = "new-parts-only";
        return;
      }
      // Serialize with all existing mutations, including those initiated by another application
      // node.
      List<Map<String, Object>> pending =
          jdbc.queryForList(
              "SELECT mutation_id,latest_fail_reason FROM system.mutations WHERE"
                  + " database=currentDatabase() AND table=? AND NOT is_done"
                  + " ORDER BY notEmpty(latest_fail_reason) DESC,create_time LIMIT 1",
              table);
      if (!pending.isEmpty()) {
        Map<String, Object> mutation = pending.getFirst();
        String failure = Objects.toString(mutation.get("latest_fail_reason"), "").trim();
        status = failure.isEmpty() ? "materializing" : "materialization-failed";
        lastError =
            failure.isEmpty()
                ? null
                : Objects.toString(mutation.get("mutation_id"), "unknown mutation")
                    + ": "
                    + failure;
        return;
      }
      List<Map<String, Object>> parts =
          jdbc.queryForList(
              "SELECT partition_id, sum(bytes_on_disk) AS bytes FROM system.parts WHERE active AND"
                  + " database=currentDatabase() AND table=? AND NOT has(projections, ?) GROUP BY"
                  + " partition_id ORDER BY partition_id DESC",
              table,
              PROJECTION);
      if (parts.isEmpty()) {
        status = "ready";
        lastError = null;
        return;
      }
      Map<String, Object> part =
          parts.stream()
              .filter(p -> ((Number) p.get("bytes")).longValue() <= maxPartitionBytes)
              .findFirst()
              .orElse(null);
      if (part == null) {
        status = "partition-budget-exceeded";
        return;
      }
      long bytes = ((Number) part.get("bytes")).longValue();
      List<Map<String, Object>> disks =
          jdbc.queryForList(
              "SELECT free_space,total_space FROM system.disks WHERE total_space > 0");
      if (disks.isEmpty()
          || disks.stream()
              .anyMatch(
                  d ->
                      ((Number) d.get("free_space")).longValue()
                          < Math.max(
                              2L * bytes, ((Number) d.get("total_space")).longValue() / 10))) {
        status = "insufficient-disk-headroom";
        return;
      }
      String partition = String.valueOf(part.get("partition_id"));
      if (!partition.matches("[a-zA-Z0-9_-]+"))
        throw new IllegalStateException("Unexpected partition identifier");
      // Async mutation: never hold startup or an HTTP request while rewriting historical parts.
      jdbc.execute(
          "ALTER TABLE "
              + table
              + " MATERIALIZE PROJECTION "
              + PROJECTION
              + " IN PARTITION ID '"
              + partition
              + "' SETTINGS mutations_sync=0, max_threads=2, max_memory_usage=536870912");
      status = "materializing:" + partition;
      lastError = null;
      log.info(
          "ClickHouse history projection queued: table={}, partition={}, sourceBytes={}",
          table,
          partition,
          bytes);
    } catch (Exception e) {
      status = "retry-pending";
      lastError = e.getClass().getSimpleName();
      log.warn(
          "ClickHouse history optimization deferred; raw queries remain available: table={},"
              + " error={}",
          table,
          e.getMessage());
    }
  }

  static String projectionDdl(String table) {
    return "ALTER TABLE "
        + table
        + " ADD PROJECTION IF NOT EXISTS "
        + PROJECTION
        + " ("
        + PROJECTION_SELECT
        + ")";
  }

  private boolean ensureCompatibleSchema() {
    try {
      Map<String, Object> column = numericColumn();
      Map<String, Object> projection = projection();
      // IF NOT EXISTS does not validate an object owned by another application or an older release.
      if (column != null && !compatibleNumericColumn(column))
        return schemaConflict("numeric_value has an incompatible type or materialized expression");
      if (projection != null && !compatibleProjection(projection))
        return schemaConflict(PROJECTION + " has an incompatible aggregate definition");
      if (column == null) {
        jdbc.execute(
            "ALTER TABLE "
                + table
                + " ADD COLUMN IF NOT EXISTS numeric_value Nullable(Float64) MATERIALIZED "
                + NUMERIC_EXPRESSION);
        column = numericColumn();
        if (column == null)
          throw new IllegalStateException("numeric_value metadata is not visible");
        if (!compatibleNumericColumn(column))
          return schemaConflict(
              "numeric_value changed while the history schema was being installed");
      }
      if (projection == null) {
        jdbc.execute(projectionDdl(table));
        projection = projection();
        if (projection == null)
          throw new IllegalStateException("history projection metadata is not visible");
        if (!compatibleProjection(projection))
          return schemaConflict(
              PROJECTION + " changed while the history schema was being installed");
      }
      numericReady = true;
      projectionReady = true;
      lastError = null;
      return true;
    } catch (RuntimeException e) {
      numericReady = false;
      projectionReady = false;
      throw e;
    }
  }

  private Map<String, Object> numericColumn() {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT type,default_kind,default_expression FROM system.columns WHERE"
                + " database=currentDatabase() AND table=? AND name='numeric_value'",
            table);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  private Map<String, Object> projection() {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT type,query FROM system.projections WHERE database=currentDatabase()"
                + " AND table=? AND name=?",
            table,
            PROJECTION);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  private boolean schemaConflict(String reason) {
    numericReady = false;
    projectionReady = false;
    status = "schema-conflict";
    if (!reason.equals(lastError))
      log.warn(
          "ClickHouse history schema conflict; retaining raw queries: table={}, reason={}",
          table,
          reason);
    lastError = reason;
    return false;
  }

  static boolean compatibleNumericColumn(Map<String, Object> column) {
    return "Nullable(Float64)".equals(String.valueOf(column.get("type")).replaceAll("\\s+", ""))
        && "MATERIALIZED".equalsIgnoreCase(String.valueOf(column.get("default_kind")))
        && canonicalExpression(NUMERIC_EXPRESSION)
            .equals(canonicalExpression(String.valueOf(column.get("default_expression"))));
  }

  static boolean compatibleProjection(Map<String, Object> projection) {
    if (!"Aggregate".equalsIgnoreCase(String.valueOf(projection.get("type")))) return false;
    Matcher query = PROJECTION_QUERY.matcher(String.valueOf(projection.get("query")));
    if (!query.matches()) return false;
    Map<String, String> aliases = new HashMap<>();
    Set<String> selected = new HashSet<>();
    for (String item : splitExpressions(query.group(1))) {
      Matcher alias = ALIAS.matcher(item);
      String expression = canonicalExpression(alias.matches() ? alias.group(1) : item);
      if (expression.isEmpty() || !selected.add(expression)) return false;
      if (alias.matches()
          && aliases.putIfAbsent(canonicalExpression(alias.group(2)), expression) != null)
        return false;
    }
    Set<String> expected = new HashSet<>(PROJECTION_GROUPS);
    expected.addAll(PROJECTION_VALUES);
    if (!selected.equals(expected)) return false;
    Set<String> groups = new HashSet<>();
    for (String item : splitExpressions(query.group(2))) {
      String expression = canonicalExpression(item);
      if (!groups.add(aliases.getOrDefault(expression, expression))) return false;
    }
    return groups.equals(PROJECTION_GROUPS);
  }

  /**
   * A narrow expression grammar: reject predicates, literals and unknown SQL instead of guessing.
   */
  private static String canonicalExpression(String expression) {
    String text = expression.trim();
    if (!text.matches("[a-zA-Z0-9_\\s(),`\"]+")) return "";
    text = text.replaceAll("[`\"]([a-zA-Z_][a-zA-Z0-9_]*)[`\"]", "$1");
    if (text.indexOf('`') >= 0 || text.indexOf('"') >= 0) return "";
    // Preserve identifier case; ClickHouse column names are case-sensitive.
    text = text.replaceAll("\\s+", "");
    Matcher function = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*(?=\\()").matcher(text);
    StringBuffer result = new StringBuffer();
    while (function.find())
      function.appendReplacement(result, function.group().toLowerCase(Locale.ROOT));
    function.appendTail(result);
    return result.toString().replaceAll("(?i)\\bnull\\b", "NULL");
  }

  private static List<String> splitExpressions(String expressions) {
    List<String> items = new ArrayList<>();
    int depth = 0;
    int start = 0;
    for (int index = 0; index < expressions.length(); index++) {
      char ch = expressions.charAt(index);
      if (ch == '(') depth++;
      else if (ch == ')' && --depth < 0) return List.of();
      else if (ch == ',' && depth == 0) {
        items.add(expressions.substring(start, index));
        start = index + 1;
      }
    }
    if (depth != 0) return List.of();
    items.add(expressions.substring(start));
    return items;
  }

  Map<String, Object> metrics() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", status);
    result.put("numericReady", numericReady);
    result.put("projectionReady", projectionReady);
    result.put("backfillEnabled", backfill);
    result.put("maxPartitionBytes", maxPartitionBytes);
    result.put("lastError", lastError);
    return result;
  }

  @Override
  public void close() {
    worker.shutdownNow();
  }
}
