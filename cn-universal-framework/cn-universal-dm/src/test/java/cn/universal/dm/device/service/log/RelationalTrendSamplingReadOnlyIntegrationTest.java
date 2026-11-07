package cn.universal.dm.device.service.log;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.entity.vo.PropertyTrendSampleVO;
import cn.universal.persistence.query.PropertyTrendQuery;
import java.io.StringReader;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Explicitly enabled, SELECT-only verification of the actual mapped result and reducer. */
@EnabledIfEnvironmentVariable(named = "NEXIOT_HISTORY_SAMPLE_CONFIG", matches = ".+")
class RelationalTrendSamplingReadOnlyIntegrationTest {
  private static String resolve(String value) {
    Pattern pattern = Pattern.compile("\\$\\{([^:{}]+):([^{}]*)}");
    for (int i = 0; i < 20; i++) {
      Matcher matcher = pattern.matcher(value);
      StringBuffer result = new StringBuffer();
      boolean found = false;
      while (matcher.find()) {
        found = true;
        String environment = System.getenv(matcher.group(1));
        matcher.appendReplacement(
            result, Matcher.quoteReplacement(environment == null ? matcher.group(2) : environment));
      }
      matcher.appendTail(result);
      value = result.toString();
      if (!found) return value;
    }
    throw new IllegalArgumentException("Unresolved database properties");
  }

  private String env(String key, String fallback) {
    return System.getenv().getOrDefault(key, fallback);
  }

  @Test
  void realBoundMapperAndNumericReducerCompleteWithinTheHistoryDeadline() throws Exception {
    Properties config = new Properties();
    try (var reader =
        Files.newBufferedReader(Path.of(System.getenv("NEXIOT_HISTORY_SAMPLE_CONFIG")))) {
      config.load(reader);
    }
    String table =
        env("NEXIOT_HISTORY_SAMPLE_TABLE", "unix_tenant_202605.ts_iot_device_log_metadata_6");
    assertTrue(table.matches("[A-Za-z_][A-Za-z0-9_]*\\.[A-Za-z_][A-Za-z0-9_]*"));
    Path root = Path.of("").toAbsolutePath();
    while (!Files.isDirectory(root.resolve("cn-universal-web/src/main/resources")))
      root = root.getParent();
    Path mapper =
        root.resolve(
            "cn-universal-persistence/src/main/resources/mybatis/postgresql/IoTDeviceLogMetadataShardMapper-postgresql.xml");
    Configuration mybatis = new Configuration();
    mybatis.setDatabaseId("postgresql");
    String xml = Files.readString(mapper).replace("FROM iot_device_log_metadata", "FROM " + table);
    new XMLMapperBuilder(
            new StringReader(xml), mybatis, mapper.toString(), mybatis.getSqlFragments())
        .parse();
    PropertyTrendQuery query = new PropertyTrendQuery();
    query.setProductKey(env("NEXIOT_HISTORY_SAMPLE_PRODUCT", "SuTfBzvbVCBp"));
    query.setIotId(env("NEXIOT_HISTORY_SAMPLE_IOT_ID", "SuTfBzvbVCBpS7001"));
    query.setDeviceId(env("NEXIOT_HISTORY_SAMPLE_DEVICE", "S7001"));
    query.setProperty(env("NEXIOT_HISTORY_SAMPLE_PROPERTY", "M200"));
    query.setBeginCreateTime(1791801960L);
    query.setEndCreateTime(1791888360L);
    query.validateRange();
    Properties credentials = new Properties();
    credentials.setProperty("user", resolve(config.getProperty("spring.datasource.username")));
    credentials.setProperty("password", resolve(config.getProperty("spring.datasource.password")));
    var loader =
        new URLClassLoader(
            new java.net.URL[] {
              Path.of(
                      System.getProperty("user.home"),
                      ".m2/repository/org/postgresql/postgresql/42.7.5/postgresql-42.7.5.jar")
                  .toUri()
                  .toURL()
            },
            getClass().getClassLoader());
    Driver driver =
        (Driver)
            Class.forName("org.postgresql.Driver", true, loader)
                .getDeclaredConstructor()
                .newInstance();
    try (loader;
        var connection =
            driver.connect(resolve(config.getProperty("spring.datasource.url")), credentials)) {
      connection.setReadOnly(true);
      try (var session = new SqlSessionFactoryBuilder().build(mybatis).openSession(connection)) {
        for (int points : new int[] {100, 600}) {
          query.setMaxPoints(points);
          long start = System.nanoTime();
          List<PropertyTrendSampleVO> samples =
              session.selectList(
                  "cn.universal.persistence.mapper.IoTDeviceLogMetadataShardMapper.selectPropertyTrendSamples",
                  query);
          var result = TrendSamplingSupport.aggregate(query, samples);
          long millis = (System.nanoTime() - start) / 1000000;
          assertTrue(result.getNumericCount() > 0);
          assertTrue(
              result.getBuckets().size() > 1, "Explicit result map must preserve window_index");
          assertTrue(result.getBuckets().size() <= points);
          assertTrue(result.getTotalCount() <= query.getMaxScanRows());
          assertTrue(samples.size() <= query.getMaxScanRows() + points);
          assertTrue(millis < 8000, "Mapped query and reducer exceeded native history deadline");
          System.out.printf(
              "history-sample-readonly points=%d windows=%d rows=%d samples=%d numeric=%d"
                  + " millis=%d%n",
              points,
              result.getBuckets().size(),
              samples.size(),
              result.getTotalCount(),
              result.getNumericCount(),
              millis);
          session.clearCache();
        }
      }
    }
  }
}
