package cn.universal.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class IoTDeviceExportMapperContractTest {

  private static final String MYSQL_MAPPER = "mybatis/mysql/IoTDeviceMapper.xml";
  private static final String POSTGRESQL_MAPPER =
      "mybatis/postgresql/IoTDeviceMapper-postgresql.xml";
  private static final String DAMENG_MAPPER = "mybatis/dameng/IoTDeviceMapper-dameng.xml";

  @Test
  void exportTimeMappingsShouldUseTheSharedLocalDateTimeContract() throws IOException {
    for (String mapper : new String[] {MYSQL_MAPPER, POSTGRESQL_MAPPER, DAMENG_MAPPER}) {
      String xml = readResource(mapper);
      assertTimeMapping(xml, "createTime", "create_time");
      assertTimeMapping(xml, "registryTime", "registry_time");
      assertTimeMapping(xml, "onlineTime", "online_time");
    }
  }

  @Test
  void postgresqlExportShouldRemoveTimeZoneAfterEpochConversion() throws IOException {
    String xml = readResource(POSTGRESQL_MAPPER);

    assertTrue(
        xml.contains(
            "to_timestamp(d.create_time) AT TIME ZONE 'Asia/Shanghai' AS create_time"));
    assertTrue(
        xml.contains(
            "to_timestamp(d.registry_time) AT TIME ZONE 'Asia/Shanghai' AS registry_time"));
    assertTrue(
        xml.contains(
            "to_timestamp(d.online_time) AT TIME ZONE 'Asia/Shanghai' AS online_time"));
  }

  @Test
  void mysqlAndDamengExportsShouldKeepNativeTimestampConversion() throws IOException {
    for (String mapper : new String[] {MYSQL_MAPPER, DAMENG_MAPPER}) {
      String xml = readResource(mapper);
      assertTrue(xml.contains("FROM_UNIXTIME(d.create_time) AS create_time"));
      assertTrue(xml.contains("FROM_UNIXTIME(d.registry_time) AS registry_time"));
      assertTrue(xml.contains("FROM_UNIXTIME(d.online_time) AS online_time"));
    }
  }

  private static void assertTimeMapping(String xml, String property, String column) {
    String mapping =
        "property=\""
            + property
            + "\" column=\""
            + column
            + "\" javaType=\"java.time.LocalDateTime\" jdbcType=\"TIMESTAMP\"";
    assertTrue(xml.contains(mapping), () -> "Missing explicit time mapping: " + mapping);
  }

  private static String readResource(String resource) throws IOException {
    try (InputStream input =
        IoTDeviceExportMapperContractTest.class.getClassLoader().getResourceAsStream(resource)) {
      if (input == null) {
        throw new IOException("Mapper resource not found: " + resource);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
