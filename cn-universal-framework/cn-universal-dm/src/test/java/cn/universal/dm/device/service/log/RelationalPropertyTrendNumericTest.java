package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.entity.vo.PropertyTrendVO;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RelationalPropertyTrendNumericTest {
  private static final String STATEMENT =
      "cn.universal.persistence.mapper.IoTDeviceLogMetadataShardMapper.selectPropertyTrend";

  private PropertyTrendQuery query() {
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setProductKey("p");
    query.setIotId("i");
    query.setProperty("speed");
    query.setBeginCreateTime(1700000000L);
    query.setEndCreateTime(1700000060L);
    query.setMaxPoints(20);
    return query;
  }

  private Configuration mapper(String dialect) throws Exception {
    Configuration configuration = new Configuration();
    configuration.setDatabaseId(dialect);
    Path path =
        root()
            .resolve(
                "cn-universal-persistence/src/main/resources/mybatis/"
                    + dialect
                    + "/IoTDeviceLogMetadataShardMapper"
                    + (dialect.equals("mysql") ? "" : "-" + dialect)
                    + ".xml");
    try (Reader reader = Files.newBufferedReader(path)) {
      new XMLMapperBuilder(reader, configuration, path.toString(), configuration.getSqlFragments())
          .parse();
    }
    return configuration;
  }

  @ParameterizedTest
  @ValueSource(strings = {"mysql", "postgresql", "dameng"})
  void mappedSqlBoundsNumericParsingAndUsesTheSameTimestampAsStoredRows(String dialect)
      throws Exception {
    Configuration configuration = mapper(dialect);
    MappedStatement statement = configuration.getMappedStatement(STATEMENT);
    BoundSql bound = statement.getBoundSql(query());
    assertEquals(8, statement.getTimeout());
    assertTrue(bound.getSql().contains("BETWEEN 1 AND 1024"));
    assertTrue(bound.getSql().contains("decimal_order"));
    assertTrue(bound.getSql().contains("numeric_value"));
    assertTrue(
        bound.getParameterMappings().stream().anyMatch(p -> p.getProperty().equals("beginTime")));
    assertTrue(
        bound.getParameterMappings().stream().anyMatch(p -> p.getProperty().equals("endTime")));
    assertFalse(bound.getSql().contains("to_timestamp"));
    assertFalse(bound.getSql().contains("FROM_UNIXTIME"));
    assertFalse(bound.getSql().contains("DECIMAL(65,20)"));
    if (dialect.equals("mysql")) {
      assertFalse(bound.getSql().contains("REGEXP_REPLACE"));
      assertFalse(bound.getSql().contains("OVER ("));
      assertFalse(bound.getSql().contains("AS DOUBLE"));
    }
  }

  @Test
  void everyAdapterFailsExplicitlyIfAnAggregateExceedsTheFiniteDoubleDomain() {
    for (String column : List.of("min", "max", "avg")) {
      PropertyTrendVO.Bucket bucket = new PropertyTrendVO.Bucket();
      if (column.equals("min")) bucket.setMin(new BigDecimal("-1e999"));
      if (column.equals("max")) bucket.setMax(new BigDecimal("1e999"));
      if (column.equals("avg")) bucket.setAvg(new BigDecimal("1e999"));
      assertEquals(
          503,
          assertThrows(IoTException.class, () -> PropertyTrendVO.of(query(), List.of(bucket)))
              .getCode());
    }
    PropertyTrendVO.Bucket boundary = new PropertyTrendVO.Bucket();
    boundary.setMax(new BigDecimal("1.7976931348623158e308"));
    assertDoesNotThrow(() -> PropertyTrendVO.of(query(), List.of(boundary)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"mysql", "postgresql", "dameng"})
  @EnabledIfEnvironmentVariable(named = "NEXIOT_RELATIONAL_LIVE_TEST", matches = "true")
  void liveSamplingUsesBoundWindowSeeksAndExcludesFractionalTimeAfterEnd(String dialect)
      throws Exception {
    Configuration configuration = mapper(dialect);
    MappedStatement statement = configuration.getMappedStatement(STATEMENT + "Samples");
    PropertyTrendQuery query = query();
    query.validateRange();
    BoundSql bound = statement.getBoundSql(query);
    assertTrue(
        statement.getResultMaps().getFirst().getResultMappings().stream()
            .anyMatch(mapping -> mapping.getProperty().equals("windowIndex")));
    StringBuilder fixture = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      if (i > 0) fixture.append(" UNION ALL ");
      LocalDateTime time = query.getBeginTime().plusSeconds(i == 3 ? 60 : i);
      if (i == 3)
        time = time.plusNanos(500000); // outside inclusive end, within 1ms sentinel boundary
      fixture
          .append("SELECT ")
          .append(i)
          .append(" AS id,TIMESTAMP '")
          .append(Timestamp.valueOf(time))
          .append("' AS create_time,'p' AS product_key,'i' AS iot_id,'PROPERTIES' AS message_type,")
          .append("'speed' AS property,'")
          .append(i == 2 ? "bad" : i * 10)
          .append("' AS content");
    }
    String sql =
        bound.getSql().replace("FROM iot_device_log_metadata", "FROM (" + fixture + ") fixture");
    try (Connection connection = connect(dialect);
        PreparedStatement prepared = connection.prepareStatement(sql)) {
      prepared.setQueryTimeout(8);
      configuration.newParameterHandler(statement, query, bound).setParameters(prepared);
      List<PropertyTrendSampleVO> samples = new ArrayList<>();
      try (ResultSet rows = prepared.executeQuery()) {
        while (rows.next())
          samples.add(
              new PropertyTrendSampleVO(rows.getInt("window_index"), rows.getString("content")));
      }
      var result = TrendSamplingSupport.aggregate(query, samples);
      assertEquals(3, result.getTotalCount());
      assertEquals(2, result.getNumericCount());
      assertEquals(1, result.getBuckets().size());
      assertTrue(result.isCountExact());
      assertEquals(0, BigDecimal.valueOf(5).compareTo(result.getBuckets().getFirst().getAvg()));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"mysql", "postgresql", "dameng"})
  @EnabledIfEnvironmentVariable(named = "NEXIOT_RELATIONAL_LIVE_TEST", matches = "true")
  void liveReadOnlyFixturesValidateTheActualMappedSql(String dialect) throws Exception {
    Configuration configuration = mapper(dialect);
    MappedStatement statement = configuration.getMappedStatement(STATEMENT);
    BoundSql bound = statement.getBoundSql(query());
    String sql = bound.getSql();
    int parsedEnd =
        dialect.equals("postgresql")
            ? sql.indexOf("SELECT (", sql.indexOf("bounded_values"))
            : sql.indexOf(") finite_values");
    String parsedNumbers = sql.substring(sql.indexOf("SELECT create_time,"), parsedEnd).trim();
    if (dialect.equals("postgresql")) {
      parsedNumbers = parsedNumbers.substring(0, parsedNumbers.lastIndexOf(')')).trim();
    }
    List<String> values =
        List.of(
            "0",
            "-1.25",
            "2.5e1",
            "abc",
            "NaN",
            "Infinity",
            "1e999",
            "1e50",
            "1e308",
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
            "0x123",
            "1e000000001",
            "9".repeat(1025),
            "1" + "0".repeat(500) + "e-500");
    List<Integer> invalid = List.of(3, 4, 5, 6, 10, 11, 19, 21);
    StringBuilder fixture = new StringBuilder();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) fixture.append(" UNION ALL ");
      fixture
          .append("SELECT ")
          .append(i)
          .append(" AS create_time, '")
          .append(values.get(i))
          .append("' AS content");
    }
    String numericSql =
        parsedNumbers.replaceAll(
                "FROM iot_device_log_metadata\\s+WHERE[\\s\\S]+?(?=\\) raw_values)",
                Matcher.quoteReplacement("FROM (" + fixture + ") fixture"))
            + " ORDER BY create_time";
    try (Connection connection = connect(dialect);
        Statement probe = connection.createStatement()) {
      probe.setQueryTimeout(8);
      try (ResultSet rows = probe.executeQuery(numericSql)) {
        int index = 0;
        while (rows.next()) {
          assertEquals(index, rows.getInt("create_time"));
          Double actual =
              rows.getObject("numeric_value") == null ? null : rows.getDouble("numeric_value");
          if (invalid.contains(index)) {
            assertNull(actual, values.get(index));
          } else {
            assertNotNull(actual, values.get(index));
            assertTrue(Double.isFinite(actual), values.get(index));
            double expected = Double.parseDouble(values.get(index));
            // DM 8.1 native DOUBLE flushes subnormal values to zero; this is not an invalid sample.
            if (dialect.equals("dameng") && Math.abs(expected) < Double.MIN_NORMAL) expected = 0;
            assertEquals(expected, actual, Math.abs(expected) * 1e-14, values.get(index));
          }
          index++;
        }
        assertEquals(values.size(), index);
      }
      verifyWholeTrend(
          connection,
          configuration,
          statement,
          bound,
          List.of("0", "10", "20", "bad", "1e999", "-2", "7", "1e308", "1e308"),
          9,
          7);
      // The old MySQL native DOUBLE AVG actually returned zero for these two values.
      verifyWholeTrend(
          connection, configuration, statement, bound, List.of("1e308", "1e308"), 2, 2);
      verifyWholeTrend(
          connection,
          configuration,
          statement,
          bound,
          List.of("1.7976931348623158e308", "1.7976931348623158e308"),
          2,
          2);
    }
  }

  private void verifyWholeTrend(
      Connection connection,
      Configuration configuration,
      MappedStatement statement,
      BoundSql bound,
      List<String> values,
      long expectedTotal,
      long expectedNumeric)
      throws Exception {
    StringBuilder fixture = new StringBuilder();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) fixture.append(" UNION ALL ");
      LocalDateTime time =
          LocalDateTime.ofInstant(Instant.ofEpochSecond(1700000000L + i), ZoneId.systemDefault());
      fixture
          .append("SELECT TIMESTAMP '")
          .append(Timestamp.valueOf(time))
          .append("' AS create_time,")
          .append(
              "'p' AS product_key,'i' AS iot_id,'PROPERTIES' AS message_type,'speed' AS property,'")
          .append(values.get(i))
          .append("' AS content");
    }
    String sql =
        bound.getSql().replace("FROM iot_device_log_metadata", "FROM (" + fixture + ") fixture");
    try (PreparedStatement prepared = connection.prepareStatement(sql)) {
      prepared.setQueryTimeout(8);
      configuration.newParameterHandler(statement, query(), bound).setParameters(prepared);
      try (ResultSet rows = prepared.executeQuery()) {
        long count = 0;
        long numeric = 0;
        int bucket = 0;
        while (rows.next()) {
          count += rows.getLong("count");
          numeric += rows.getLong("numeric_count");
          assertEquals(1700000000000L + bucket * 4000L, rows.getLong("time"));
          assertTrue(Double.isFinite(rows.getDouble("min")));
          assertTrue(Double.isFinite(rows.getDouble("max")));
          assertTrue(Double.isFinite(rows.getDouble("avg")));
          if (values.size() == 2)
            assertEquals(
                Double.parseDouble(values.get(0)),
                rows.getDouble("avg"),
                Double.parseDouble(values.get(0)) * 1e-14);
          if (values.size() == 9 && bucket == 0) assertEquals(10, rows.getDouble("avg"), 1e-14);
          bucket++;
        }
        assertEquals(expectedTotal, count);
        assertEquals(expectedNumeric, numeric);
      }
    }
  }

  private Connection connect(String dialect) throws Exception {
    String profile = dialect.equals("mysql") ? "dev" : dialect;
    Properties properties = new Properties();
    try (Reader reader =
        Files.newBufferedReader(
            root()
                .resolve(
                    "cn-universal-web/src/main/resources/application-"
                        + profile
                        + ".properties"))) {
      properties.load(reader);
    }
    String driverName = resolve(properties.getProperty("spring.datasource.driver-class-name"));
    Class<?> driverClass;
    try {
      driverClass = Class.forName(driverName);
    } catch (ClassNotFoundException e) {
      String jar =
          dialect.equals("dameng")
              ? "com/dameng/DmJdbcDriver18/8.1.3.140/DmJdbcDriver18-8.1.3.140.jar"
              : "org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar";
      Path path = Path.of(System.getProperty("user.home"), ".m2/repository", jar);
      assertTrue(Files.isRegularFile(path), "请先安装对应JDBC驱动以运行显式启用的只读验收");
      driverClass =
          Class.forName(
              driverName,
              true,
              new URLClassLoader(
                  new java.net.URL[] {path.toUri().toURL()}, getClass().getClassLoader()));
    }
    Properties credentials = new Properties();
    credentials.setProperty("user", resolve(properties.getProperty("spring.datasource.username")));
    credentials.setProperty(
        "password", resolve(properties.getProperty("spring.datasource.password")));
    return ((Driver) driverClass.getDeclaredConstructor().newInstance())
        .connect(resolve(properties.getProperty("spring.datasource.url")), credentials);
  }

  private String resolve(String value) {
    Pattern pattern = Pattern.compile("\\$\\{([^:{}]+):([^{}]*)}");
    for (int iteration = 0; iteration < 5; iteration++) {
      Matcher matcher = pattern.matcher(value);
      StringBuffer result = new StringBuffer();
      boolean found = false;
      while (matcher.find()) {
        found = true;
        String env = System.getenv(matcher.group(1));
        matcher.appendReplacement(
            result, Matcher.quoteReplacement(env == null ? matcher.group(2) : env));
      }
      matcher.appendTail(result);
      value = result.toString();
      if (!found) return value;
    }
    return value;
  }

  private Path root() {
    Path directory = Path.of("").toAbsolutePath();
    while (directory != null
        && !Files.isDirectory(directory.resolve("cn-universal-web/src/main/resources")))
      directory = directory.getParent();
    assertNotNull(directory);
    return directory;
  }
}
