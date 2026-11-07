package cn.universal.admin.video.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.security.utils.SecurityUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;
import tk.mybatis.mapper.mapperhelper.MapperHelper;

class WvpDeviceChannelSyncTest {

  private static final String INSTANCE = "inner_wvp_123";
  private static final String SHARED_CHANNEL = "34020000001320000001";

  static Stream<Arguments> sharedChannelCases() {
    return Stream.of("PostgreSQL", "MySQL")
        .flatMap(mode -> Stream.of("", "shared-directory").map(parent -> Arguments.of(mode, parent)));
  }

  @ParameterizedTest
  @MethodSource("sharedChannelCases")
  void sameChannelIdOnTwoDevicesSurvivesRepeatedSyncInEitherOrder(String mode, String parent)
      throws Exception {
    try (var database = new ChannelDatabase(mode);
        var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("test-user");
      var first = channel(SHARED_CHANNEL, "AI监控2", parent, "ON");
      var second = channel(SHARED_CHANNEL, "AI监控3", parent, "OFF");

      assertEquals(1, database.handler.sync(1L, "54020000001180000001", List.of(first)));
      Long firstId = database.channels(1L).getFirst().getId();
      assertEquals(1, database.handler.sync(2L, "34020000001320000001", List.of(second)));
      Long secondId = database.channels(2L).getFirst().getId();
      assertNotEquals(firstId, secondId);

      // The last synced device must never take ownership of another device's channel.
      database.handler.sync(2L, "34020000001320000001", List.of(second));
      database.handler.sync(1L, "54020000001180000001", List.of(first));
      database.handler.sync(1L, "54020000001180000001", List.of(first));
      database.handler.sync(2L, "34020000001320000001", List.of(second));

      assertEquals(2, database.mapper.selectAll().size());
      var firstChannel = database.channels(1L).getFirst();
      var secondChannel = database.channels(2L).getFirst();
      assertEquals(firstId, firstChannel.getId());
      assertEquals(secondId, secondChannel.getId());
      assertEquals("54020000001180000001", firstChannel.getPlatformDeviceId());
      assertEquals("34020000001320000001", secondChannel.getPlatformDeviceId());
      assertEquals("AI监控2", firstChannel.getChannelName());
      assertEquals("AI监控3", secondChannel.getChannelName());
      assertEquals("ON", firstChannel.getChannelStatus());
      assertEquals("OFF", secondChannel.getChannelStatus());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"PostgreSQL", "MySQL"})
  void directoryChangeUpdatesSameRowAndStaleCleanupStaysWithinDevice(String mode) throws Exception {
    try (var database = new ChannelDatabase(mode);
        var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("test-user");
      var shared = channel(SHARED_CHANNEL, "AI监控2", "", "ON");
      database.handler.sync(1L, "first", List.of(shared, channel("stale", "old", "", "OFF")));
      var original = database.channels(1L).stream()
          .filter(ch -> SHARED_CHANNEL.equals(ch.getChannelId())).findFirst().orElseThrow();
      database.handler.sync(2L, "second", List.of(shared));

      database.handler.sync(1L, "first", List.of(channel(SHARED_CHANNEL, "renamed", "new-parent", "OFF")));

      assertEquals(2, database.mapper.selectAll().size());
      assertEquals(1, database.channels(1L).size());
      var updated = database.channels(1L).getFirst();
      assertEquals(original.getId(), updated.getId());
      assertEquals(original.getCreateTime(), updated.getCreateTime());
      assertEquals("new-parent", updated.getParentId());
      assertEquals("renamed", updated.getChannelName());
      assertNull(database.channels(2L).getFirst().getParentId());
      assertEquals("AI监控2", database.channels(2L).getFirst().getChannelName());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"PostgreSQL", "MySQL"})
  void repeatedChannelWithinDeviceDoesNotViolateDeviceUniqueConstraint(String mode) throws Exception {
    try (var database = new ChannelDatabase(mode);
        var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn("test-user");
      assertEquals(1, database.handler.sync(1L, "first", List.of(
          channel(SHARED_CHANNEL, "first occurrence", "parent-a", "ON"),
          channel(SHARED_CHANNEL, "duplicate", "parent-b", "OFF"))));
      assertEquals(1, database.channels(1L).size());
      assertEquals("first occurrence", database.channels(1L).getFirst().getChannelName());
    }
  }

  private static Map<String, Object> channel(String id, String name, String parent, String status) {
    return Map.of("channelId", id, "name", name, "parentId", parent, "status", status);
  }

  private static class ChannelDatabase implements AutoCloseable {
    private final JdbcDataSource dataSource = new JdbcDataSource();
    private final SqlSession session;
    private final VideoPlatformChannelMapper mapper;
    private final ChannelSyncHandler handler;

    ChannelDatabase(String mode) throws Exception {
      dataSource.setURL("jdbc:h2:mem:channel_sync_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=" + mode);
      var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
      config.setMapUnderscoreToCamelCase(true);
      var helper = new MapperHelper();
      helper.registerMapper(BaseMapper.class);
      config.addMapper(VideoPlatformChannelMapper.class);
      helper.processConfiguration(config);
      String columns = EntityHelper.getColumns(VideoPlatformChannel.class).stream()
          .map(column -> column.getColumn() + " " + columnType(column) + (column.isId() ? " PRIMARY KEY" : ""))
          .collect(Collectors.joining(", "));
      try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
        statement.execute("CREATE TABLE video_platform_channel (" + columns + ")");
        statement.execute("CREATE UNIQUE INDEX uk_device_channel ON video_platform_channel(device_id, channel_id)");
        statement.execute("CREATE UNIQUE INDEX uk_instance_channel ON video_platform_channel(instance_key, channel_id, parent_id)");
        // PostgreSQL DDL is also accepted by H2; exercise the migration before the sync.
        statement.execute(Files.readString(Path.of(
            "../cn-universal-db-migrator/src/main/resources/db/migration/postgresql/V2026101001__scope_video_channels_to_device.sql")));
      }
      session = new SqlSessionFactoryBuilder().build(config).openSession(true);
      mapper = session.getMapper(VideoPlatformChannelMapper.class);
      handler = new ChannelSyncHandler(mapper);
    }

    private static String columnType(EntityColumn column) {
      if (column.getJavaType() == Long.class) return "BIGINT";
      if (column.getJavaType() == Integer.class) return "INTEGER";
      if (column.getJavaType() == Date.class) return "TIMESTAMP";
      return "VARCHAR(4000)";
    }

    List<VideoPlatformChannel> channels(Long deviceId) {
      var example = new Example(VideoPlatformChannel.class);
      example.createCriteria().andEqualTo("deviceId", deviceId);
      return mapper.selectByExample(example);
    }

    @Override
    public void close() throws Exception {
      session.close();
      try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
        statement.execute("SHUTDOWN");
      }
    }
  }

  private static class ChannelSyncHandler extends WvpDeviceSyncHandler {
    ChannelSyncHandler(VideoPlatformChannelMapper mapper) {
      super(null);
      channelMapper = mapper;
    }

    @Override
    protected String resolveInstanceTenantId(String instanceKey) {
      return "test-tenant";
    }

    int sync(Long deviceId, String platformDeviceId, List<Map<String, Object>> channels) {
      return batchInsertChannels(deviceId, INSTANCE, platformDeviceId, channels);
    }
  }
}
