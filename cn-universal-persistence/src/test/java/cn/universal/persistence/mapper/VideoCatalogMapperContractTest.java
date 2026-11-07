package cn.universal.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.vo.VideoCatalogDeviceRow;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class VideoCatalogMapperContractTest {
  private Configuration mapper(String database) throws Exception {
    String suffix = "mysql".equals(database) ? "" : "-" + database;
    String resource = "mybatis/" + database + "/IoTDeviceMapper" + suffix + ".xml";
    Configuration result = new Configuration();
    result.setDatabaseId(database);
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
      assertNotNull(input, resource);
      new XMLMapperBuilder(input, result, resource, result.getSqlFragments()).parse();
    }
    return result;
  }

  private String sql(Configuration mapper, String method, Map<String, Object> args) {
    return mapper.getMappedStatement(IoTDeviceMapper.class.getName() + "." + method)
        .getBoundSql(args).getSql().replaceAll("\\s+", " ").toLowerCase();
  }

  @Test void everyDialectProvidesMatchingScopedCountAndLightweightProjections() throws Exception {
    for (String dialect : List.of("postgresql", "mysql", "dameng")) {
      var config = mapper(dialect);
      var device = IoTDevice.builder().tenantId("tenant-a").build();
      device.setParams(new HashMap<>(Map.of("scopeDenied", true)));
      var args = new HashMap<String, Object>();
      args.put("ioTDevice", device);
      args.put("keyword", "camera");
      args.put("groupId", "group-a");
      String rows = sql(config, "selectVideoCatalogDevices", args);
      String count = sql(config, "selectVideoCatalogDevices_COUNT", args);
      for (String statement : List.of(rows, count)) {
        assertTrue(statement.contains("d.tenant_id = ?"), dialect);
        assertTrue(statement.contains("and 1 = 0"), dialect);
        assertTrue(statement.contains("iot_device_tags"), dialect);
        assertTrue(statement.contains("d.device_name like"), dialect);
      }
      assertFalse(count.contains("configuration"), dialect);
      assertFalse(rows.contains("productconfiguration"), dialect);
      assertFalse(rows.contains("d.detail"), dialect);
      assertTrue(rows.contains("order by d.create_time desc, d.id desc"), dialect);
    }
  }

  @Test void nativeRowsAndCountsUseSameTenantEnabledAndChannelFilters() throws Exception {
    for (String dialect : List.of("postgresql", "mysql", "dameng")) {
      var config = mapper(dialect);
      Map<String, Object> args = Map.of("instanceKey", "p", "deviceId", "serial",
          "tenantId", "tenant-a", "channelId", "0");
      for (String id : List.of("selectVideoCatalogNativeChannels", "selectVideoCatalogNativeChannels_COUNT")) {
        String statement = sql(config, id, args);
        assertTrue(statement.contains("c.tenant_id = ?"), dialect);
        assertTrue(statement.contains("c.platform_device_id = ?"), dialect);
        assertTrue(statement.contains("c.enabled = 1 or c.enabled is null"), dialect);
        assertTrue(statement.contains("c.channel_id = ?"), dialect);
      }
      var device = new VideoCatalogDeviceRow();
      device.setTenantId("tenant-a"); device.setInstanceKey("p"); device.setDeviceId("serial");
      String count = sql(config, "selectVideoCatalogChannelCounts", Map.of("devices", List.of(device)));
      assertTrue(count.contains("c.tenant_id = ?"), dialect);
    }
  }

  @Test void configurationChannelsAreBoundDatabaseRowsetsWithSeparateCountQueries() throws Exception {
    for (String dialect : List.of("postgresql", "mysql", "dameng")) {
      var config = mapper(dialect);
      var args = Map.<String, Object>of("channelsJson", "[]");
      String rows = sql(config, "selectVideoCatalogConfigurationChannels", args);
      String count = sql(config, "selectVideoCatalogConfigurationChannels_COUNT", args);
      String function = "postgresql".equals(dialect) ? "json_to_recordset" : "json_table";
      assertTrue(rows.contains(function), dialect);
      assertTrue(count.contains(function), dialect);
      assertTrue(rows.contains("?"), dialect);
      assertFalse(rows.contains("limit"), "Pagination must be applied by PageHelper: " + dialect);
    }
  }
}
