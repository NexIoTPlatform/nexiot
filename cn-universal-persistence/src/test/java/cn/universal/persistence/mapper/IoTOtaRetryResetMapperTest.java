package cn.universal.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.entity.IoTOtaTaskDevice;
import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class IoTOtaRetryResetMapperTest {

  private static final String NAMESPACE = IoTOtaTaskDeviceMapper.class.getName() + ".";

  @Test
  void retryExplicitlyClearsErrorsWithoutChangingOrdinaryPatchSemanticsForEveryDatabase()
      throws Exception {
    // MariaDB shares the mysql mapper and databaseId, as configured by MybatisConfig.
    for (String database : new String[] {"mysql", "mariadb", "postgresql", "dameng"}) {
      String mapperDatabase = "mariadb".equals(database) ? "mysql" : database;
      String suffix = "mysql".equals(mapperDatabase) ? "" : "-" + mapperDatabase;
      String resource = "mybatis/" + mapperDatabase + "/IoTOtaTaskDeviceMapper" + suffix + ".xml";
      Configuration configuration = new Configuration();
      configuration.setDatabaseId(mapperDatabase);
      try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
        assertNotNull(input, resource);
        new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
      }
      BoundSql reset = configuration.getMappedStatement(NAMESPACE + "resetRetryFields")
          .getBoundSql(Map.of("id", 42L));
      String sql = normalize(reset.getSql());
      assertTrue(sql.contains("error_code = null"), database + ": " + sql);
      assertTrue(sql.contains("error_message = null"), database + ": " + sql);
      assertTrue(sql.endsWith("where id = ?"), database + ": " + sql);
      assertEquals(1, reset.getParameterMappings().size(), database);
      assertEquals("id", reset.getParameterMappings().getFirst().getProperty(), database);

      IoTOtaTaskDevice patch = IoTOtaTaskDevice.builder().id(42L).progress(50).build();
      for (String method : new String[] {"updateTaskDevice", "updateTaskDeviceIfActive"}) {
        String patchSql = normalize(configuration.getMappedStatement(NAMESPACE + method)
            .getBoundSql(patch).getSql());
        assertFalse(patchSql.contains("error_code ="), database + ": " + method);
        assertFalse(patchSql.contains("error_message ="), database + ": " + method);
        assertTrue(patchSql.contains("progress = ?"), database + ": " + method);
      }
    }
  }

  private String normalize(String sql) {
    return sql.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
  }
}
