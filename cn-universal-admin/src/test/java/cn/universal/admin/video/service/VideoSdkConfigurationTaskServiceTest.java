package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.*;
import cn.universal.persistence.entity.*;
import cn.universal.persistence.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;

class VideoSdkConfigurationTaskServiceTest {
  final VideoSdkConfigTaskMapper tasks = mock(VideoSdkConfigTaskMapper.class);
  final IoTTaskItemMapper items = mock(IoTTaskItemMapper.class);
  final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  final VideoPlatformAdapterRegistry adapters = mock(VideoPlatformAdapterRegistry.class);
  final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  final VideoSdkConfigurationTaskService service =
      new VideoSdkConfigurationTaskService(
          tasks, items, instances, devices, channels, adapters, new ObjectMapper());
  VideoPlatformInstance instance =
      VideoPlatformInstance.builder()
          .instanceKey("isup")
          .tenantId("t")
          .creatorId("u")
          .platformType("hik_isup")
          .enabled(1)
          .build();
  IoTTaskCenter batch =
      IoTTaskCenter.builder()
          .id(5L)
          .tenantId("t")
          .bizKey("isup")
          .taskType("ISUP_CONFIG_BATCH")
          .status("RUNNING")
          .requestSnapshot("{\"resource\":\"encoding\",\"changes\":{\"bitrate\":1024}}")
          .build();
  IoTTaskItem item;

  @BeforeEach
  void setup() {
    item = new IoTTaskItem();
    item.setId(8L);
    item.setTaskId(5L);
    item.setTenantId("t");
    item.setStatus("PENDING");
    item.setItemNo(UUID.randomUUID().toString());
    item.setRawData("{\"deviceId\":\"nvr\",\"channelId\":\"7\"}");
    when(tasks.claim(eq(5L), anyString(), any(), any())).thenReturn(1);
    when(tasks.renew(eq(5L), anyString(), any())).thenReturn(1);
    when(tasks.updateItemOwned(any(), anyString())).thenReturn(1);
    when(tasks.selectByPrimaryKey(5L)).thenReturn(batch);
    when(tasks.configurationItems(5L, "t", "isup")).thenReturn(List.of(item));
    when(instances.selectOne(any())).thenReturn(instance);
    when(devices.selectOne(any()))
        .thenReturn(VideoPlatformDevice.builder().id(9L).tenantId("t").enabled(1).build());
    when(channels.selectCount(any())).thenReturn(1);
    when(adapters.getAdapter("hik_isup")).thenReturn(adapter);
    when(adapter.executeOperation(any(), eq("capabilities"), any())).thenReturn(Map.of());
    when(adapter.executeOperation(any(), eq("deviceConfigRead"), any()))
        .thenReturn(Map.of("observedRevision", "rev1"));
    when(adapter.executeOperation(any(), eq("deviceConfigApply"), any()))
        .thenReturn(Map.of("status", "success", "verified", true));
  }

  @AfterEach
  void close() {
    service.stop();
  }

  @Test
  void snapshotsBeforeRevisionAndStableRequestBeforeVerifiedWrite() {
    service.process(5L);
    assertEquals("SUCCESS", item.getStatus());
    verify(adapter)
        .executeOperation(
            eq(instance),
            eq("deviceConfigApply"),
            argThat(
                p ->
                    "rev1".equals(p.get("expectedRevision"))
                        && item.getItemNo().equals(p.get("requestId"))
                        && "7".equals(p.get("channelId"))));
    var order = inOrder(adapter, tasks);
    order.verify(adapter).executeOperation(any(), eq("capabilities"), any());
    order.verify(adapter).executeOperation(any(), eq("deviceConfigRead"), any());
    order.verify(tasks).updateItemOwned(any(), anyString());
    order.verify(adapter).executeOperation(any(), eq("deviceConfigApply"), any());
  }

  @Test
  void recoveryOfDispatchedItemReadsWithoutReplayingSet() {
    item.setStatus("DISPATCHED");
    service.process(5L);
    assertEquals("UNKNOWN", item.getStatus());
    verify(adapter, never()).executeOperation(any(), eq("deviceConfigApply"), any());
    verify(adapter).executeOperation(any(), eq("deviceConfigRead"), any());
  }

  @Test
  void uncertainWriteIsUnknownAndNeverAutomaticallyReplayed() {
    when(adapter.executeOperation(any(), eq("deviceConfigApply"), any()))
        .thenThrow(new IllegalStateException("timeout"));
    service.process(5L);
    assertEquals("UNKNOWN", item.getStatus());
    service.process(5L);
    verify(adapter, times(1)).executeOperation(any(), eq("deviceConfigApply"), any());
  }

  @Test
  void unsupportedCapabilityAndMissingRevisionNeverWrite() {
    when(adapter.executeOperation(any(), eq("capabilities"), any()))
        .thenReturn(
            Map.of(
                "operations",
                List.of(Map.of("id", "deviceConfigApply", "deviceSupport", "unsupported"))));
    service.process(5L);
    assertEquals("SKIPPED", item.getStatus());
    verify(adapter, never()).executeOperation(any(), eq("deviceConfigApply"), any());
  }

  @Test
  void cancellationAndForeignTenantNeverWrite() {
    batch.setStatus("CANCEL_REQUESTED");
    service.process(5L);
    assertEquals("CANCELLED", item.getStatus());
    verifyNoInteractions(adapter);
    item.setStatus("PENDING");
    batch.setStatus("RUNNING");
    instance.setTenantId("other");
    service.process(5L);
    assertEquals("FAILED", item.getStatus());
    verifyNoInteractions(adapter);
  }

  @Test
  void leaseLossBeforeSetNeverWrites() {
    when(tasks.updateItemOwned(any(), anyString())).thenReturn(0);
    service.process(5L);
    verify(adapter, never()).executeOperation(any(), eq("deviceConfigApply"), any());
    verify(tasks, never())
        .finish(anyLong(), anyString(), anyString(), anyInt(), anyInt(), anyString(), any());
  }

  @Test
  void ownedMapperUpdatesAreLeaseGuardedOnRealTaskCenterTables() throws Exception {
    var db = new org.h2.jdbcx.JdbcDataSource();
    db.setURL("jdbc:h2:mem:config_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    String seed =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("../docker/database/postgres/001-schema.sql"));
    try (var connection = db.getConnection();
        var statement = connection.createStatement()) {
      for (String table : List.of("iot_task_center", "iot_task_item")) {
        int start = seed.indexOf("CREATE TABLE unix_tenant_202605." + table + " (");
        int end = seed.indexOf(";", start);
        statement.execute(seed.substring(start, end + 1).replace("unix_tenant_202605.", ""));
      }
    }
    var configuration =
        new org.apache.ibatis.session.Configuration(
            new org.apache.ibatis.mapping.Environment(
                "test", new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(), db));
    configuration.setMapUnderscoreToCamelCase(true);
    var helper = new tk.mybatis.mapper.mapperhelper.MapperHelper();
    helper.registerMapper(cn.universal.persistence.common.BaseMapper.class);
    configuration.addMapper(VideoSdkConfigTaskMapper.class);
    configuration.addMapper(IoTTaskItemMapper.class);
    helper.processConfiguration(configuration);
    try (var session =
        new org.apache.ibatis.session.SqlSessionFactoryBuilder()
            .build(configuration)
            .openSession(true)) {
      var mapper = session.getMapper(VideoSdkConfigTaskMapper.class);
      var itemMapper = session.getMapper(IoTTaskItemMapper.class);
      batch.setTaskNo("number");
      batch.setTaskName("batch");
      batch.setCreatorId("u");
      batch.setStatus("PENDING");
      batch.setCreateTime(new Date());
      batch.setUpdateTime(new Date());
      mapper.insert(batch);
      item.setCreateTime(new Date());
      item.setUpdateTime(new Date());
      itemMapper.insert(item);
      assertEquals(1, mapper.claim(5, "owner", new Date(), new Date(0)));
      assertEquals(0, mapper.claim(5, "other", new Date(), new Date(0)));
      item.setStatus("DISPATCHED");
      assertEquals(0, mapper.updateItemOwned(item, "other"));
      assertEquals(1, mapper.updateItemOwned(item, "owner"));
      assertEquals("DISPATCHED", itemMapper.selectByPrimaryKey(8L).getStatus());
      assertEquals(0, mapper.cancel(5, "foreign", "isup"));
      assertEquals(1, mapper.cancel(5, "t", "isup"));
      assertEquals(1, mapper.renew(5, "owner", new Date()));
      assertEquals(1, mapper.finish(5, "owner", "CANCELLED", 0, 1, "{}", new Date()));
      assertEquals(0, mapper.updateItemOwned(item, "owner"));
    }
  }

  @Test
  void templateVersionIsOpaqueAndRejectsSecretsAndForeignReads() {
    var created =
        service.createTemplate(
            instance,
            Map.of("name", "码率", "resource", "encoding", "changes", Map.of("bitrate", 1024)),
            "u");
    assertInstanceOf(String.class, created.get("version"));
    assertThrows(
        BaseException.class,
        () ->
            service.createTemplate(
                instance,
                Map.of(
                    "name", "x", "resource", "encoding", "changes", Map.of(), "password", "hidden"),
                "u"));
    batch.setTaskType("ISUP_CONFIG_TEMPLATE");
    batch.setTenantId("other");
    assertThrows(
        BaseException.class,
        () ->
            service.createBatch(
                instance,
                Map.of(
                    "templateId",
                    "5",
                    "targets",
                    List.of(Map.of("deviceId", "nvr", "channelId", "7"))),
                "u"));
  }

  private record DatabaseFixture(
      org.apache.ibatis.session.SqlSession session,
      VideoSdkConfigTaskMapper tasks,
      IoTTaskItemMapper items)
      implements AutoCloseable {
    @Override
    public void close() {
      session.close();
    }
  }

  private DatabaseFixture databaseFixture() throws Exception {
    var db = new org.h2.jdbcx.JdbcDataSource();
    db.setURL(
        "jdbc:h2:mem:config_regression_"
            + UUID.randomUUID()
            + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    String seed =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("../docker/database/postgres/001-schema.sql"));
    try (var connection = db.getConnection();
        var statement = connection.createStatement()) {
      for (String table : List.of("iot_task_center", "iot_task_item")) {
        int start = seed.indexOf("CREATE TABLE unix_tenant_202605." + table + " (");
        int end = seed.indexOf(";", start);
        statement.execute(seed.substring(start, end + 1).replace("unix_tenant_202605.", ""));
      }
    }
    var configuration =
        new org.apache.ibatis.session.Configuration(
            new org.apache.ibatis.mapping.Environment(
                "test", new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(), db));
    configuration.setMapUnderscoreToCamelCase(true);
    var helper = new tk.mybatis.mapper.mapperhelper.MapperHelper();
    helper.registerMapper(cn.universal.persistence.common.BaseMapper.class);
    configuration.addMapper(VideoSdkConfigTaskMapper.class);
    configuration.addMapper(IoTTaskItemMapper.class);
    helper.processConfiguration(configuration);
    var session =
        new org.apache.ibatis.session.SqlSessionFactoryBuilder()
            .build(configuration)
            .openSession(true);
    return new DatabaseFixture(
        session,
        session.getMapper(VideoSdkConfigTaskMapper.class),
        session.getMapper(IoTTaskItemMapper.class));
  }

  private IoTTaskCenter persistedBatch(long id, String status) {
    Date now = new Date();
    return IoTTaskCenter.builder()
        .id(id)
        .taskNo("fixture-" + id)
        .taskName("配置验收")
        .tenantId("t")
        .bizKey("isup")
        .taskType("ISUP_CONFIG_BATCH")
        .status(status)
        .definitionId(11L)
        .creatorId("u")
        .requestSnapshot(batch.getRequestSnapshot())
        .createTime(now)
        .updateTime(now)
        .totalCount(1)
        .successCount(0)
        .failCount(0)
        .build();
  }

  private IoTTaskItem persistedItem(long id, long taskId, int row, String status, String tenant) {
    var result = new IoTTaskItem();
    result.setId(id);
    result.setTaskId(taskId);
    result.setTenantId(tenant);
    result.setItemNo(UUID.randomUUID().toString());
    result.setRowNo(row);
    result.setStatus(status);
    result.setRawData("{\"deviceId\":\"nvr\",\"channelId\":\"7\"}");
    result.setResultData("{\"status\":\"success\",\"verified\":true,\"marker\":" + row + "}");
    result.setCreateTime(new Date());
    result.setUpdateTime(new Date());
    return result;
  }

  @SuppressWarnings("unchecked")
  @Test
  void completedAndInterruptedItemsRemainVisibleInScopedBatchAndListViews() throws Exception {
    try (var f = databaseFixture()) {
      var task = persistedBatch(90, "SUCCESS");
      f.tasks.insert(task);
      var states =
          List.of(
              "SUCCESS",
              "FAILED",
              "PARTIAL",
              "UNKNOWN",
              "SKIPPED",
              "CANCELLED",
              "PENDING",
              "DISPATCHED");
      for (int n = states.size() - 1; n >= 0; n--)
        f.items.insert(persistedItem(100 + n, 90, n, states.get(n), "t"));
      // Neither a foreign item tenant nor a task of another family can leak through the query.
      f.items.insert(persistedItem(200, 90, 0, "SUCCESS", "foreign"));
      var unrelated = persistedBatch(91, "SUCCESS");
      unrelated.setTaskType("OTHER_TASK");
      f.tasks.insert(unrelated);
      f.items.insert(persistedItem(201, 91, 0, "SUCCESS", "t"));
      assertEquals(0, f.tasks.configurationItems(90, "foreign", "isup").size());
      assertEquals(0, f.tasks.configurationItems(90, "t", "other-instance").size());
      assertEquals(0, f.tasks.configurationItems(91, "t", "isup").size());
      var queried = f.tasks.configurationItems(90, "t", "isup");
      assertEquals(states, queried.stream().map(IoTTaskItem::getStatus).toList());
      var durable =
          new VideoSdkConfigurationTaskService(
              f.tasks, f.items, instances, devices, channels, adapters, new ObjectMapper());
      try {
        var view = durable.batch(instance, "90");
        assertEquals(8, view.get("total"));
        assertEquals(6L, view.get("completed"));
        var results = (List<Map<String, Object>>) view.get("results");
        assertEquals(
            states.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList(),
            results.stream().map(r -> r.get("status")).toList());
        assertEquals(
            true, ((Map<String, Object>) results.getFirst().get("result")).get("verified"));
        var listed = (List<Map<String, Object>>) durable.batches(instance).get("records");
        assertEquals(1, listed.size());
        assertEquals(view.get("results"), listed.getFirst().get("results"));
        assertThrows(
            BaseException.class,
            () ->
                durable.batch(
                    VideoPlatformInstance.builder()
                        .platformType("hik_isup")
                        .tenantId("foreign")
                        .instanceKey("isup")
                        .build(),
                    "90"));
      } finally {
        durable.stop();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  void persistedDispatchedRecoveryReadsBackUnknownOnceWithoutReplayingTheWrite() throws Exception {
    try (var f = databaseFixture()) {
      var task = persistedBatch(92, "RUNNING");
      task.setUpdateTime(new Date(System.currentTimeMillis() - 180_000));
      task.setExtJson("interrupted-owner");
      f.tasks.insert(task);
      var pending = persistedItem(202, 92, 0, "DISPATCHED", "t");
      pending.setResultData("{\"beforeRevision\":\"rev0\",\"requestId\":\"original-request\"}");
      f.items.insert(pending);
      var durable =
          new VideoSdkConfigurationTaskService(
              f.tasks, f.items, instances, devices, channels, adapters, new ObjectMapper());
      try {
        durable.process(92);
        var recovered = f.items.selectByPrimaryKey(202L);
        assertEquals("UNKNOWN", recovered.getStatus());
        assertTrue(recovered.getMessage().contains("停止自动重放"));
        assertEquals(
            "rev1",
            new ObjectMapper()
                .readTree(recovered.getResultData())
                .get("observedRevision")
                .asText());
        assertEquals("PARTIAL_SUCCESS", f.tasks.selectByPrimaryKey(92L).getStatus());
        assertEquals(1, f.tasks.selectByPrimaryKey(92L).getFailCount());
        var view = durable.batch(instance, "92");
        assertEquals(1L, view.get("completed"));
        assertEquals(
            "unknown", ((List<Map<String, Object>>) view.get("results")).getFirst().get("status"));
        durable.process(92);
        verify(adapter, times(1))
            .executeOperation(
                eq(instance),
                eq("deviceConfigRead"),
                argThat(
                    p ->
                        "encoding".equals(p.get("resource"))
                            && "7".equals(p.get("channelId"))
                            && !p.containsKey("changes")));
        verify(adapter, never()).executeOperation(any(), eq("deviceConfigApply"), any());
        verify(adapter, never()).executeOperation(any(), eq("capabilities"), any());
      } finally {
        durable.stop();
      }
    }
  }

  @Test
  void emptyMissingAndNonObjectTemplateChangesAreRejectedBeforePersistence() {
    var input = new LinkedHashMap<String, Object>();
    input.put("name", "空配置");
    input.put("resource", "encoding");
    assertThrows(BaseException.class, () -> service.createTemplate(instance, input, "u"));
    for (Object invalid : Arrays.asList(Map.of(), null, List.of())) {
      input.put("changes", invalid);
      assertThrows(BaseException.class, () -> service.createTemplate(instance, input, "u"));
    }
    verify(tasks, never()).insert(any());
    verifyNoInteractions(adapter);
  }
}
