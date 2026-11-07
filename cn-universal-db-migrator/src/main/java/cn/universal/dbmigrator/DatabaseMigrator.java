package cn.universal.dbmigrator;

import java.util.Locale;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.ClassicConfiguration;

/**
 * Explicit production migration entry point. Business applications must not run migrations on startup.
 */
public final class DatabaseMigrator {

  private DatabaseMigrator() {}

  public static void main(String[] args) {
    Arguments arguments = Arguments.parse(args);
    String url = required("DB_MIGRATOR_URL");
    String user = required("DB_MIGRATOR_USERNAME");
    String password = required("DB_MIGRATOR_PASSWORD");
    String vendor = vendor(url);
    if (!("mysql".equals(vendor) || "postgresql".equals(vendor))) {
      throw new IllegalArgumentException("Only MySQL and PostgreSQL are enabled; detected: " + vendor);
    }

    ClassicConfiguration configuration = new ClassicConfiguration();
    configuration.setDataSource(url, user, password);
    if ("postgresql".equals(vendor)) {
      // Concurrent index builds must not wait on a transaction held by Flyway's own lock.
      configuration.getPluginRegister()
          .getPlugin(org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension.class)
          .setTransactionalLock(false);
    }
    configuration.setLocations(
        new Location("classpath:db/migration/common"),
        new Location("classpath:db/migration/" + vendor));
    configuration.setTable(System.getenv().getOrDefault("DB_MIGRATOR_HISTORY_TABLE", "flyway_schema_history"));
    String baselineVersion = "baseline".equals(arguments.command())
        ? required("DB_MIGRATOR_BASELINE_VERSION")
        : System.getenv().getOrDefault("DB_MIGRATOR_BASELINE_VERSION", "1.5.0");
    configuration.setBaselineVersionAsString(baselineVersion);
    configuration.setBaselineDescription("NexIoT current-version baseline");
    configuration.setBaselineOnMigrate(false);
    configuration.setCleanDisabled(true);
    configuration.setOutOfOrder(false);
    // Bound a release to its reviewed version without applying later pending features.
    String target = System.getenv("DB_MIGRATOR_TARGET");
    if (target != null && !target.isBlank()) configuration.setTargetAsString(target);
    configuration.setMixed(false);
    // Keep transactional and non-transactional migrations (for example PostgreSQL CONCURRENTLY indexes) separate.
    configuration.setGroup(false);
    configuration.setLockRetryCount(30);
    configuration.setConnectRetries(3);
    configuration.setValidateMigrationNaming(true);
    // 已 baseline 的生产库允许存在待执行版本；migrate 会在锁内按顺序执行它们。
    configuration.setIgnoreMigrationPatterns("*:pending");
    configuration.setFailOnMissingLocations(true);
    configuration.setPlaceholderReplacement(false);

    Flyway flyway = new Flyway(configuration);
    switch (arguments.command()) {
      case "validate" -> flyway.validate();
      case "baseline" -> {
        if (!"YES".equals(System.getenv("DB_MIGRATOR_CONFIRM_BASELINE"))) {
          throw new IllegalArgumentException(
              "Baseline requires a verified schema/release; set DB_MIGRATOR_CONFIRM_BASELINE=YES explicitly");
        }
        if ("postgresql".equals(vendor)) {
          baselinePostgreSql(flyway, configuration);
        } else {
          flyway.baseline();
        }
      }
      case "migrate" -> {
        if ("postgresql".equals(vendor)) requirePostgreSqlHistory(configuration);
        flyway.validate();
        flyway.migrate();
      }
      default -> throw new IllegalArgumentException("Unsupported command: " + arguments.command());
    }
    System.out.printf("Database migration command '%s' completed for %s%n", arguments.command(), vendor);
  }

  /** Never treat a bootstrapped application schema with empty metadata as version zero. */
  private static void requirePostgreSqlHistory(ClassicConfiguration configuration) {
    try (Connection connection = configuration.getDataSource().getConnection()) {
      String qualified = quoteIdentifier(connection.getSchema()) + "." + quoteIdentifier(configuration.getTable());
      boolean exists;
      try (PreparedStatement query = connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
        query.setString(1, qualified);
        try (ResultSet result = query.executeQuery()) { result.next(); exists = result.getBoolean(1); }
      }
      boolean hasRecords = false;
      if (exists) {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT EXISTS (SELECT 1 FROM " + qualified + ")")) {
          result.next(); hasRecords = result.getBoolean(1);
        }
      }
      if (!hasRecords) {
        throw new IllegalStateException("Migration history is missing or empty. Verify the installed schema/release, set DB_MIGRATOR_BASELINE_VERSION to that installed version, and run baseline with DB_MIGRATOR_CONFIRM_BASELINE=YES before migrate; never replay historical migrations on a bootstrap schema");
      }
    } catch (SQLException error) {
      throw new IllegalStateException("Could not verify PostgreSQL migration history", error);
    }
  }

  /** The Docker schema snapshot contains an official, empty Flyway history table. */
  private static void baselinePostgreSql(Flyway flyway, ClassicConfiguration configuration) {
    try (Connection connection = configuration.getDataSource().getConnection()) {
      String schema = connection.getSchema();
      String table = configuration.getTable();
      String qualified = quoteIdentifier(schema) + "." + quoteIdentifier(table);
      String lockKey = "nexiot.baseline:" + schema + "." + table;
      // Bound both the advisory lock and metadata table lock to a short wait.
      try (Statement statement = connection.createStatement()) {
        statement.execute("SET lock_timeout = '10s'");
      }
      // Coordinate explicit baseline invocations before examining/recreating empty metadata.
      try (PreparedStatement lock = connection.prepareStatement("SELECT pg_advisory_lock(hashtextextended(?, 0))")) {
        lock.setString(1, lockKey);
        lock.execute();
      }
      try {
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
          statement.execute("SET LOCAL lock_timeout = '10s'");
          boolean exists;
          try (PreparedStatement query = connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
            query.setString(1, qualified);
            try (ResultSet result = query.executeQuery()) { result.next(); exists = result.getBoolean(1); }
          }
          if (exists) {
            statement.execute("LOCK TABLE " + qualified + " IN ACCESS EXCLUSIVE MODE");
            boolean empty;
            try (ResultSet result = statement.executeQuery("SELECT NOT EXISTS (SELECT 1 FROM " + qualified + ")")) {
              result.next(); empty = result.getBoolean(1);
            }
            if (empty) {
              // Only the known default bootstrap metadata shape is eligible. Never remove rows,
              // custom history tables, extra columns, triggers or dependent objects.
              if (!"flyway_schema_history".equals(table) || !isOfficialEmptyHistory(connection, qualified)) {
                throw new IllegalStateException("Existing empty migration history is not the official bootstrap shape; review it before baseline");
              }
              statement.execute("DROP TABLE " + qualified);
              System.out.println("Recreated empty Docker bootstrap Flyway metadata before confirmed baseline; no migration records removed");
            }
          }
          connection.commit();
        } catch (SQLException | RuntimeException error) {
          connection.rollback();
          throw error;
        }
        flyway.baseline();
      } finally {
        connection.setAutoCommit(true);
        try (PreparedStatement unlock = connection.prepareStatement("SELECT pg_advisory_unlock(hashtextextended(?, 0))")) {
          unlock.setString(1, lockKey);
          unlock.execute();
        }
      }
    } catch (SQLException error) {
      throw new IllegalStateException("Could not prepare PostgreSQL migration baseline", error);
    }
  }

  private static boolean isOfficialEmptyHistory(Connection connection, String qualified) throws SQLException {
    String query = """
        WITH expected(name,type_name,required) AS (VALUES
          ('installed_rank','integer',true),('version','character varying(50)',false),
          ('description','character varying(200)',true),('type','character varying(20)',true),
          ('script','character varying(1000)',true),('checksum','integer',false),
          ('installed_by','character varying(100)',true),('installed_on','timestamp without time zone',true),
          ('execution_time','integer',true),('success','boolean',true))
        SELECT (SELECT count(*) FROM pg_attribute WHERE attrelid=to_regclass(?) AND attnum>0 AND NOT attisdropped)=10
          AND NOT EXISTS (SELECT 1 FROM expected e LEFT JOIN pg_attribute a
             ON a.attrelid=to_regclass(?) AND a.attname=e.name AND a.attnum>0 AND NOT a.attisdropped
             WHERE format_type(a.atttypid,a.atttypmod) IS DISTINCT FROM e.type_name OR a.attnotnull IS DISTINCT FROM e.required)
          AND NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgrelid=to_regclass(?) AND NOT tgisinternal)
          AND EXISTS (SELECT 1 FROM pg_class WHERE oid=to_regclass(?) AND relkind='r' AND relpersistence='p'
             AND relacl IS NULL AND reloptions IS NULL AND NOT relrowsecurity AND NOT relforcerowsecurity)
          AND NOT EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid=to_regclass(?)
             AND (attacl IS NOT NULL OR attidentity<>'' OR attgenerated<>''))
          AND (SELECT count(*) FROM pg_attrdef WHERE adrelid=to_regclass(?))=1
          AND EXISTS (SELECT 1 FROM pg_attrdef d JOIN pg_attribute a ON a.attrelid=d.adrelid AND a.attnum=d.adnum
             WHERE d.adrelid=to_regclass(?) AND a.attname='installed_on' AND pg_get_expr(d.adbin,d.adrelid)='now()')
          AND (SELECT count(*) FROM pg_constraint WHERE conrelid=to_regclass(?) AND contype<>'n')=1
          AND EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid=to_regclass(?) AND contype='p'
             AND convalidated AND conname='flyway_schema_history_pk' AND pg_get_constraintdef(oid)='PRIMARY KEY (installed_rank)')
          AND (SELECT count(*) FROM pg_index WHERE indrelid=to_regclass(?))=2
          AND (SELECT count(*) FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid WHERE i.indrelid=to_regclass(?)
             AND i.indisvalid AND i.indpred IS NULL AND i.indexprs IS NULL AND (
               c.relname='flyway_schema_history_pk' AND i.indisunique AND pg_get_indexdef(i.indexrelid) LIKE '% USING btree (installed_rank)'
               OR c.relname='flyway_schema_history_s_idx' AND NOT i.indisunique AND pg_get_indexdef(i.indexrelid) LIKE '% USING btree (success)'))=2
        """;
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      for (int i = 1; i <= 11; i++) statement.setString(i, qualified);
      try (ResultSet result = statement.executeQuery()) { result.next(); return result.getBoolean(1); }
    }
  }

  private static String quoteIdentifier(String name) {
    if (name == null || name.isBlank()) throw new IllegalStateException("Database schema/history identifier is missing");
    return "\"" + name.replace("\"", "\"\"") + "\"";
  }

  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must be set; credentials are never read from source files");
    }
    return value;
  }

  private static String vendor(String url) {
    String normalized = url.toLowerCase(Locale.ROOT);
    if (normalized.startsWith("jdbc:postgresql:")) return "postgresql";
    if (normalized.startsWith("jdbc:mysql:")) return "mysql";
    return "unknown";
  }

  private record Arguments(String command) {
    private static Arguments parse(String[] args) {
      if (args.length != 1 || !("validate".equals(args[0]) || "baseline".equals(args[0]) || "migrate".equals(args[0]))) {
        throw new IllegalArgumentException("Usage: java -jar cn-universal-db-migrator.jar <validate|baseline|migrate>");
      }
      return new Arguments(args[0]);
    }
  }
}
