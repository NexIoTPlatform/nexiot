package cn.universal.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.query.IoTProductQuery;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class IoTProductOptionsMapperTest {

  private static final String STATEMENT = IoTProductMapper.class.getName() + ".selectProductOptions";
  private static final String[] DATABASES = {"mysql", "mariadb", "postgresql", "dameng"};

  @Test
  void defaultSelectionIsLightweightEnabledAndDeterministicallyOrderedForEveryDatabase()
      throws Exception {
    for (String database : DATABASES) {
      MappedStatement statement = statement(database);
      BoundSql bound = statement.getBoundSql(new IoTProductQuery());
      String sql = normalize(bound.getSql());
      String projection = sql.substring("select ".length(), sql.indexOf(" from "));
      Set<String> columns = Arrays.stream(projection.split(","))
          .map(String::trim)
          .map(column -> column.replaceFirst("\\s+as\\s+\\w+$", ""))
          .collect(Collectors.toSet());

      assertEquals(3, projection.split(",").length, database + ": " + sql);
      assertEquals(Set.of("a.id", "a.name", "a.product_key"), columns, database);
      assertEquals("cn.universal.persistence.entity.vo.IoTProductOptionVO",
          statement.getResultMaps().getFirst().getType().getName(), database);
      assertTrue(sql.contains("from iot_product a"), database + ": " + sql);
      assertTrue(sql.contains("left join iot_user au on a.creator_id = au.union_id"),
          database + ": " + sql);
      assertTrue(sql.contains("a.is_deleted = 0"), database + ": " + sql);
      assertTrue(sql.contains("a.state = 0"), database + ": " + sql);
      assertFalse(sql.contains("iot_device"), database + ": " + sql);
      assertFalse(sql.contains("metadata"), database + ": " + sql);
      assertFalse(sql.matches(".*\\bcount\\s*\\(.*"), database + ": " + sql);
      assertTrue(compact(sql).endsWith("orderbya.create_timedesc,a.iddesc"),
          database + ": " + sql);
      assertTrue(bound.getParameterMappings().isEmpty(), database + ": " + sql);
    }
  }

  @Test
  void tenantFilterIncludesLegacyProductsThroughTheirCreatorForEveryDatabase() throws Exception {
    for (String database : DATABASES) {
      IoTProductQuery query = new IoTProductQuery();
      query.setTenantId("tenant-a");
      BoundSql bound = statement(database).getBoundSql(query);
      String sql = compact(bound.getSql());

      assertTrue(sql.contains("and(a.tenant_id=?or((a.tenant_idisnullora.tenant_id='')"
          + "andau.tenant_id=?))"), database + ": " + bound.getSql());
      assertEquals(List.of("tenantId", "tenantId"), parameters(bound), database);
      assertFalse(sql.contains("a.product_keylike"), database);
    }
  }

  @Test
  void departmentAndUserScopesAreGroupedWithOrAndDeniedScopeCannotBeBypassed()
      throws Exception {
    for (String database : DATABASES) {
      MappedStatement statement = statement(database);
      IoTProductQuery query = new IoTProductQuery();
      query.setTenantId("tenant-a");
      query.setScopeDeptIds(List.of(10L, 11L));
      query.setScopeUnionIds(List.of("user-a", "user-b"));
      BoundSql allowed = statement.getBoundSql(query);
      String sql = compact(allowed.getSql());
      assertTrue(sql.contains("and(au.dept_idin(?,?)ora.creator_idin(?,?))"),
          database + ": " + allowed.getSql());
      assertFalse(sql.contains("and1=0"), database);

      query.setScopeDenied(true);
      BoundSql denied = statement.getBoundSql(query);
      String deniedSql = compact(denied.getSql());
      assertTrue(deniedSql.contains("and1=0"), database + ": " + denied.getSql());
      assertFalse(deniedSql.contains("au.dept_idin"), database);
      assertFalse(deniedSql.contains("a.creator_idin"), database);
      assertEquals(List.of("tenantId", "tenantId"), parameters(denied), database);

      query.setScopeDenied(false);
      query.setScopeUnionIds(List.of());
      assertTrue(compact(statement.getBoundSql(query).getSql())
          .contains("and(au.dept_idin(?,?))"), database);
      query.setScopeDeptIds(List.of());
      query.setScopeUnionIds(List.of("user-a"));
      assertTrue(compact(statement.getBoundSql(query).getSql())
          .contains("and(a.creator_idin(?))"), database);
    }
  }

  @Test
  void oneNameSearchMatchesBothNameAndKeyWhileRestoringAKeyUsesExactEquality()
      throws Exception {
    for (String database : DATABASES) {
      MappedStatement statement = statement(database);
      IoTProductQuery query = new IoTProductQuery();
      query.setTenantId("tenant-a");
      query.setCreatorId("self-user");
      query.setName("pump");
      query.setProductKey("pump-exact");
      BoundSql searched = statement.getBoundSql(query);
      String sql = compact(searched.getSql());

      assertTrue(sql.contains("and(a.namelikeconcat('%',?,'%')"
          + "ora.product_keylikeconcat('%',?,'%'))"), database + ": " + searched.getSql());
      assertTrue(sql.contains("anda.product_key=?"), database + ": " + searched.getSql());
      assertTrue(sql.contains("anda.creator_id=?"), database + ": " + searched.getSql());
      assertEquals(2, parameters(searched).stream().filter("name"::equals).count(), database);
      assertEquals(1, parameters(searched).stream().filter("productKey"::equals).count(),
          database);
      assertEquals(2, parameters(searched).stream().filter("tenantId"::equals).count(), database);

      query.setName("");
      BoundSql restore = statement.getBoundSql(query);
      String restoreSql = compact(restore.getSql());
      assertTrue(restoreSql.contains("anda.product_key=?"), database);
      assertFalse(restoreSql.contains("like"), database + ": " + restore.getSql());
      assertFalse(parameters(restore).contains("name"), database);
      assertTrue(parameters(restore).contains("tenantId"), database);
      assertTrue(parameters(restore).contains("creatorId"), database);

      query.setProductKey("");
      BoundSql noSearch = statement.getBoundSql(query);
      assertFalse(compact(noSearch.getSql()).contains("anda.product_key=?"), database);
      assertFalse(parameters(noSearch).contains("productKey"), database);
    }
  }

  private MappedStatement statement(String database) throws Exception {
    // MariaDB uses the mysql mapper and databaseId in MybatisConfig.
    String mapperDatabase = "mariadb".equals(database) ? "mysql" : database;
    String suffix = "mysql".equals(mapperDatabase) ? "" : "-" + mapperDatabase;
    String resource = "mybatis/" + mapperDatabase + "/IoTProductMapper" + suffix + ".xml";
    Configuration configuration = new Configuration();
    configuration.setDatabaseId(mapperDatabase);
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
      assertNotNull(input, resource);
      new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
    }
    return configuration.getMappedStatement(STATEMENT);
  }

  private List<String> parameters(BoundSql boundSql) {
    return boundSql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList();
  }

  private String normalize(String sql) {
    return sql.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
  }

  private String compact(String sql) {
    return normalize(sql).replaceAll("\\s+", "");
  }
}
