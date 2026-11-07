package cn.universal.rule.scene.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.universal.persistence.entity.SceneLinkage;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** 显式开启的只读三库验收：执行真实 MyBatis BoundSql，CTE 测试数据不写入业务表。 */
@EnabledIfSystemProperty(named = "scene.database.verify", matches = "true")
class SceneLinkageDatabaseTest {
  @Test
  void postgresql() throws Exception {
    verify(
        "postgresql",
        "application-postgresql.properties",
        "org/postgresql/postgresql",
        "org.postgresql.Driver");
  }

  @Test
  void mysql() throws Exception {
    verify(
        "mysql",
        "application-dev.properties",
        "com/mysql/mysql-connector-j",
        "com.mysql.cj.jdbc.Driver");
  }

  @Test
  void dameng() throws Exception {
    verify(
        "dameng",
        "application-dameng.properties",
        "com/dameng/DmJdbcDriver18",
        "dm.jdbc.driver.DmDriver");
  }

  private void verify(String dialect, String config, String jarFolder, String driverClass)
      throws Exception {
    var properties = new Properties();
    try (var reader =
        Files.newBufferedReader(Path.of("../cn-universal-web/src/main/resources", config))) {
      properties.load(reader);
    }
    Path jar;
    try (var files =
        Files.walk(Path.of(System.getProperty("user.home"), ".m2/repository", jarFolder))) {
      jar =
          files
              .filter(
                  p ->
                      p.toString().endsWith(".jar")
                          && !p.toString().contains("-sources")
                          && !p.toString().contains("-javadoc"))
              .sorted()
              .reduce((a, b) -> b)
              .orElseThrow();
    }
    try (var loader =
        new URLClassLoader(new java.net.URL[] {jar.toUri().toURL()}, getClass().getClassLoader())) {
      Driver driver =
          (Driver) Class.forName(driverClass, true, loader).getConstructor().newInstance();
      Properties credentials = new Properties();
      credentials.setProperty(
          "user", resolve(properties.getProperty("spring.datasource.username")));
      credentials.setProperty(
          "password", resolve(properties.getProperty("spring.datasource.password")));
      credentials.setProperty("connectTimeout", "10000");
      try (Connection db =
          driver.connect(resolve(properties.getProperty("spring.datasource.url")), credentials)) {
        db.setAutoCommit(false);
        try {
          var configuration = new Configuration();
          configuration.setDatabaseId(dialect);
          String resource =
              "mybatis/"
                  + dialect
                  + "/SceneLinkageMapper"
                  + ("mysql".equals(dialect) ? "" : "-" + dialect)
                  + ".xml";
          try (var xml = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(xml, configuration, resource, configuration.getSqlFragments())
                .parse();
          }
          var args = new HashMap<String, Object>();
          args.put("productKey", "p");
          args.put("deviceId", "d");
          args.put("tenantId", "t");
          args.put("creatorId", "u");
          assertEquals(
              List.of(1L, 4L),
              query(
                  db,
                  configuration,
                  dialect,
                  "selectSceneLinkageListByProductKeyAndDeviceId",
                  args));
          assertEquals(
              List.of(1L, 2L, 6L),
              query(db, configuration, dialect, "selectEnabledSceneLinkageScope", args));
          args.put("tenantId", null);
          assertEquals(
              List.of(4L),
              query(db, configuration, dialect, "selectEnabledSceneLinkageScope", args));
          var filter = new SceneLinkage();
          filter.setTenantId("t");
          filter.setCreateBy("u");
          filter.setDevId("d");
          assertEquals(
              List.of(1L, 2L, 4L, 5L),
              query(db, configuration, dialect, "selectSceneLinkageList", filter));
          filter.setDevId(null);
          filter.setTriggerCondition("timer");
          assertEquals(
              List.of(6L), query(db, configuration, dialect, "selectSceneLinkageList", filter));
          assertEquals(
              List.of(1L, 2L, 3L, 4L),
              query(db, configuration, dialect, "selectTriggerByDevId", Map.of("deviceId", "d")));
          assertEquals(
              List.of(1L, 2L, 3L, 4L),
              query(db, configuration, dialect, "selectTriggerByType", Map.of("deviceId", "d")));
        } finally {
          db.rollback();
        }
      }
    }
  }

  private List<Long> query(
      Connection db, Configuration config, String dialect, String method, Object args)
      throws Exception {
    var sql =
        config
            .getMappedStatement("cn.universal.persistence.mapper.SceneLinkageMapper." + method)
            .getBoundSql(args);
    try (var statement = db.prepareStatement(fixture(dialect) + sql.getSql())) {
      statement.setQueryTimeout(15);
      var meta = config.newMetaObject(args);
      for (int i = 0; i < sql.getParameterMappings().size(); i++)
        statement.setObject(i + 1, meta.getValue(sql.getParameterMappings().get(i).getProperty()));
      List<Long> ids = new ArrayList<>();
      try (var results = statement.executeQuery()) {
        while (results.next()) ids.add(results.getLong("id"));
      }
      ids.sort(Long::compareTo);
      return ids;
    }
  }

  private String fixture(String dialect) {
    String same = "[{\"trigger\":\"device\",\"deviceId\":\"d\",\"productKey\":\"p\"}]";
    String split =
        "[{\"trigger\":\"device\",\"deviceId\":\"d\",\"productKey\":\"other\"},{\"trigger\":\"device\",\"deviceId\":\"other\",\"productKey\":\"p\"}]";
    List<String> rows = new ArrayList<>();
    for (int id = 1; id <= 6; id++) {
      String json = id == 2 ? split : id == 6 ? "[{\"trigger\":\"timer\"}]" : same;
      String tenant = id == 3 ? "other" : id == 4 ? "" : "t";
      rows.add(
          "select "
              + id
              + " as id, 'test' as scene_name, 'one' as touch, '"
              + json
              + "' as trigger_condition, '[]' as exec_action, 0 as sleep_cycle, 'u' as create_by, '"
              + tenant
              + "' as tenant_id, null as create_time, null as update_by, null as update_time, "
              + (id == 5 ? 1 : 0)
              + " as status, null as dev_id"
              + ("dameng".equals(dialect) ? " from dual" : ""));
    }
    return "with scene_linkage as (" + String.join(" union all ", rows) + ") ";
  }

  private String resolve(String value) {
    var pattern = Pattern.compile("\\$\\{([^:{}]+):([^{}]*)}");
    for (int i = 0; i < 10; i++) {
      var matcher = pattern.matcher(value);
      var resolved = new StringBuffer();
      boolean changed = false;
      while (matcher.find()) {
        changed = true;
        matcher.appendReplacement(
            resolved,
            java.util.regex.Matcher.quoteReplacement(
                System.getenv().getOrDefault(matcher.group(1), matcher.group(2))));
      }
      matcher.appendTail(resolved);
      value = resolved.toString();
      if (!changed) return value;
    }
    return value;
  }
}
