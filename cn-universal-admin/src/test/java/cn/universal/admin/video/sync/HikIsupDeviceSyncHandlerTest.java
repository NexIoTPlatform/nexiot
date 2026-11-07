package cn.universal.admin.video.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.VideoDeviceImportService;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceExtMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class HikIsupDeviceSyncHandlerTest {
  private final HikIsupDeviceSyncHandler handler = new HikIsupDeviceSyncHandler(mock(VideoPlatformAdapterRegistry.class));
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformDeviceExtMapper extensions = mock(VideoPlatformDeviceExtMapper.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoDeviceImportService imports = mock(VideoDeviceImportService.class);
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder()
      .instanceKey("isup-a").creatorId("trusted-creator").tenantId("trusted-tenant").build();

  @BeforeEach void setup() {
    EntityHelper.initEntityNameMap(VideoPlatformDevice.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformDeviceExt.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformChannel.class, new Config());
    handler.deviceMapper = devices; handler.channelMapper = channels; handler.instanceMapper = instances;
    handler.deviceExtMapper = extensions;
    handler.transactionExecutor = new DeviceSyncTransactionExecutor();
    handler.objectMapper = new ObjectMapper();
    handler.alarmContextCache = mock(cn.universal.admin.video.alarm.SdkVideoAlarmContextCache.class);
    ReflectionTestUtils.setField(handler, "importService", imports);
    when(instances.selectOne(any())).thenReturn(instance);
    when(extensions.selectByExample(any())).thenReturn(List.of());
    when(channels.selectByExample(any())).thenReturn(List.of());
    when(imports.updateDeviceStatusAndConfig(eq("isup-a"), anyMap(), eq("hik_isup"))).thenReturn(true);
  }

  @Test void missingOfflineDirectoryEntriesNeverCreateDeviceOrAssociatedResources() throws Exception {
    List<Map<String, Object>> offlineSignals = List.of(
        Map.of("deviceStatus", "OFF"), Map.of("deviceStatus", "offline"),
        Map.of("deviceStatus", "0"), Map.of("deviceStatus", "false"),
        Map.of("status", "OFF"), Map.of("status", "offline"),
        Map.of("status", "0"), Map.of("status", "false"),
        Map.of("online", false), Map.of("online", "false"),
        Map.of("deviceStatus", "ON", "online", false));
    when(devices.selectByExample(any())).thenReturn(List.of());

    for (Map<String, Object> signal : offlineSignals) {
      Map<String, Object> directoryEntry = new LinkedHashMap<>(signal);
      directoryEntry.put("deviceId", "nvr-a");
      directoryEntry.put("channelList", List.of(Map.of("channelId", "1")));
      handler.syncRegisteredDevice(instance, directoryEntry);
    }

    verify(devices, never()).insertSelective(any());
    verify(devices, never()).updateByPrimaryKeySelective(any());
    verify(devices, never()).selectOne(any());
    verifyNoInteractions(extensions, channels, imports, handler.alarmContextCache);
    var queries = org.mockito.ArgumentCaptor.forClass(Example.class);
    verify(devices, times(offlineSignals.size())).selectByExample(queries.capture());
    queries.getAllValues().forEach(HikIsupDeviceSyncHandlerTest::assertLockedDeviceQuery);
  }

  @Test void existingEnabledDeviceStillUpdatesOfflineStateAndIotStatus() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("isup-a")
        .enabled(1).deviceStatus("ON").tenantId("old-tenant").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(devices.selectOne(any())).thenReturn(saved);

    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncRegisteredDevice(instance, Map.of("deviceId", "nvr-a", "online", false));
    }

    var updated = org.mockito.ArgumentCaptor.forClass(VideoPlatformDevice.class);
    verify(devices).updateByPrimaryKeySelective(updated.capture());
    assertEquals(11L, updated.getValue().getId());
    assertEquals(1, updated.getValue().getEnabled());
    assertEquals("OFF", updated.getValue().getDeviceStatus());
    assertEquals("trusted-tenant", updated.getValue().getTenantId());
    verify(devices, never()).insertSelective(any());
    var channelUpdate = org.mockito.ArgumentCaptor.forClass(VideoPlatformChannel.class);
    verify(channels).updateByExampleSelective(channelUpdate.capture(), any(Example.class));
    assertEquals("offline", channelUpdate.getValue().getChannelStatus());
    var imported = org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(imports).importDevice(eq("isup-a"), imported.capture(), isNull());
    assertEquals("OFF", imported.getValue().get("deviceStatus"));
    assertEquals("OFF", imported.getValue().get("status"));
    verify(imports).updateDeviceStatusAndConfig(eq("isup-a"), eq(imported.getValue()), eq("hik_isup"));
    var extension = org.mockito.ArgumentCaptor.forClass(VideoPlatformDeviceExt.class);
    verify(extensions).insertSelective(extension.capture());
    assertEquals("0", extension.getValue().getIsOnline());
  }

  @Test void existingDisabledOfflineDevicePreservesDisableAndNeverImportsIot() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("isup-a")
        .enabled(0).createId("original-creator").deviceStatus("ON")
        .configuration("{\"fusion\":{\"enabled\":true}}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(devices.selectOne(any())).thenReturn(saved);

    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncRegisteredDevice(instance, Map.of("deviceId", "nvr-a", "status", "offline"));
    }

    verify(devices).updateByPrimaryKeySelective(saved);
    assertEquals(0, saved.getEnabled());
    assertEquals("OFF", saved.getDeviceStatus());
    assertEquals("original-creator", saved.getCreateId());
    assertTrue(handler.objectMapper.readTree(saved.getConfiguration()).path("fusion").path("enabled").asBoolean());
    verify(devices, never()).insertSelective(any());
    verify(channels).updateByExampleSelective(any(VideoPlatformChannel.class), any(Example.class));
    verifyNoInteractions(imports);
  }

  @Test void newOnlineRegistrationCreatesCatalogAndImportsIotUnderTrustedInstanceTenant() throws Exception {
    AtomicReference<VideoPlatformDevice> stored = new AtomicReference<>();
    when(devices.selectByExample(any())).thenAnswer(call ->
        stored.get() == null ? List.of() : List.of(stored.get()));
    when(devices.selectOne(any())).thenAnswer(call -> stored.get());
    doAnswer(call -> {
      VideoPlatformDevice inserted = call.getArgument(0);
      inserted.setId(11L);
      stored.set(inserted);
      return 1;
    }).when(devices).insertSelective(any(VideoPlatformDevice.class));

    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncRegisteredDevice(instance, Map.of("deviceId", "nvr-a", "status", "ON",
          "online", true, "tenantId", "untrusted-device-tenant", "creatorId", "untrusted-device-creator",
          "channelList", List.of(Map.of("channelId", "1", "status", "online"))));
    }

    assertNotNull(stored.get());
    assertEquals("nvr-a", stored.get().getDeviceId());
    assertEquals("ON", stored.get().getDeviceStatus());
    assertEquals(1, stored.get().getEnabled());
    assertEquals("trusted-tenant", stored.get().getTenantId());
    assertEquals("trusted-creator", stored.get().getCreateId());
    verify(devices).insertSelective(stored.get());
    verify(devices, never()).updateByPrimaryKeySelective(any());
    var extension = org.mockito.ArgumentCaptor.forClass(VideoPlatformDeviceExt.class);
    verify(extensions).insertSelective(extension.capture());
    assertEquals("trusted-tenant", extension.getValue().getTenantId());
    var channel = org.mockito.ArgumentCaptor.forClass(VideoPlatformChannel.class);
    verify(channels).insertSelective(channel.capture());
    assertEquals(11L, channel.getValue().getDeviceId());
    assertEquals("trusted-tenant", channel.getValue().getTenantId());
    assertEquals("trusted-creator", channel.getValue().getCreateId());
    verify(imports).importDevice(eq("isup-a"), anyMap(), isNull());
    verify(imports).updateDeviceStatusAndConfig(eq("isup-a"), anyMap(), eq("hik_isup"));
  }

  @Test void authoritativeEmptyCatalogDeletesStaleRowsAndPreservesLocalDeviceSettings() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("isup-a")
        .enabled(1).deviceName("平台名称").configuration("{\"fusion\":{\"enabled\":true},\"channelList\":[{\"channelId\":\"33\"},{\"channelId\":\"37\"}]}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(devices.selectOne(any())).thenReturn(saved);
    when(channels.selectByExample(any())).thenReturn(List.of(
        VideoPlatformChannel.builder().id(101L).deviceId(11L).channelId("33").build(),
        VideoPlatformChannel.builder().id(102L).deviceId(11L).channelId("37").build()));

    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncRegisteredDevice(instance, Map.of("deviceId", "nvr-a", "online", true,
          "channelCatalogAuthoritative", true, "channelList", List.of()));
    }

    verify(channels).deleteByPrimaryKey(101L);
    verify(channels).deleteByPrimaryKey(102L);
    verify(channels, never()).insertSelective(any());
    assertEquals(1, saved.getEnabled());
    var config = handler.objectMapper.readTree(saved.getConfiguration());
    assertTrue(config.path("fusion").path("enabled").asBoolean());
    assertTrue(config.path("channelList").isEmpty());
    var imported = org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(imports).importDevice(eq("isup-a"), imported.capture(), isNull());
    assertEquals(List.of(), imported.getValue().get("channelList"));
  }

  @Test void failedCatalogPreservesSavedDirectoryAndPassesEffectiveCatalogToIot() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("isup-a")
        .enabled(1).configuration("{\"channelList\":[{\"channelId\":\"33\",\"channelNo\":\"33\",\"channelName\":\"入口\"},{\"channelId\":\"37\",\"channelNo\":\"37\",\"channelName\":\"出口\"}]}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(devices.selectOne(any())).thenReturn(saved);

    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncRegisteredDevice(instance, Map.of("deviceId", "nvr-a", "online", true,
          "channelCatalogAuthoritative", false, "channelList", List.of()));
    }

    verify(channels, never()).deleteByPrimaryKey(any());
    var imported = org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(imports).importDevice(eq("isup-a"), imported.capture(), isNull());
    var effective = handler.objectMapper.valueToTree(imported.getValue().get("channelList"));
    assertEquals(2, effective.size());
    assertEquals("33", effective.get(0).path("channelId").asText());
    assertEquals("37", effective.get(1).path("channelId").asText());
    assertEquals("unknown", effective.get(0).path("onlineState").asText());
    assertEquals(handler.objectMapper.readTree(saved.getConfiguration()).path("channelList"), effective);
  }

  @Test void partialDirectoryCanAddRealIsapiChannelsButCannotRecreateCapacitySlots() {
    var saved = VideoPlatformDevice.builder().configuration("{\"channelList\":[{\"channelId\":\"33\",\"channelNo\":\"33\"}]}").build();
    var result = handler.applyDeviceNames(saved, Map.of("online", true, "channelCatalogAuthoritative", false,
        "channelList", List.of(Map.of("channelId", "34", "channelNo", "34"),
            Map.of("channelId", "37", "channelNo", "37", "directorySource", "isapi"))));
    var channels = (List<?>)result.get("channelList");
    assertEquals(List.of("33", "37"), channels.stream().map(row -> ((Map<?, ?>)row).get("channelId")).toList());
    assertEquals(false, result.get("channelCatalogAuthoritative"));
  }

  @Test void offlineCachedCatalogNeverDeletesOrCreatesChannelRowsAndIotRetainsExistingDirectory() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("isup-a")
        .enabled(1).configuration("{\"channelList\":[{\"channelId\":\"33\",\"channelNo\":\"33\"}]}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(devices.selectOne(any())).thenReturn(saved);
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncRegisteredDevice(instance, Map.of("deviceId", "nvr-a", "online", false,
          "channelCatalogAuthoritative", true, "channelList", List.of(Map.of("channelId", "37"))));
    }
    verify(channels, never()).deleteByPrimaryKey(any());
    verify(channels, never()).insertSelective(any());
    var imported = org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(imports).importDevice(eq("isup-a"), imported.capture(), isNull());
    var effective = handler.objectMapper.valueToTree(imported.getValue().get("channelList"));
    assertEquals(1, effective.size());
    assertEquals("33", effective.get(0).path("channelId").asText());
    assertEquals("offline", effective.get(0).path("onlineState").asText());
    assertEquals(false, imported.getValue().get("channelCatalogAuthoritative"));
  }

  @Test void offlineExistenceCheckRunsWithRowLockInsideDeviceTransactionAfterDeletion() throws Exception {
    AtomicReference<VideoPlatformDevice> stored = new AtomicReference<>(VideoPlatformDevice.builder()
        .id(11L).instanceKey("isup-a").deviceId("nvr-a").enabled(1).build());
    var transactions = new AbstractPlatformTransactionManager() {
      @Override protected Object doGetTransaction() { return new Object(); }
      @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
        // The delete has committed before the sync transaction acquires its row lock.
        stored.set(null);
      }
      @Override protected void doCommit(DefaultTransactionStatus status) {}
      @Override protected void doRollback(DefaultTransactionStatus status) {}
    };
    ProxyFactory proxy = new ProxyFactory(new DeviceSyncTransactionExecutor());
    proxy.setProxyTargetClass(true);
    proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
    handler.transactionExecutor = (DeviceSyncTransactionExecutor) proxy.getProxy();
    when(devices.selectByExample(any())).thenAnswer(call -> {
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      assertLockedDeviceQuery(call.getArgument(0));
      assertNull(stored.get());
      return List.of();
    });

    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    handler.syncRegisteredDevice(instance, Map.of("deviceId", "nvr-a", "deviceStatus", "OFF"));
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    verify(devices, times(1)).selectByExample(any(Example.class));
    verify(devices, never()).insertSelective(any());
    verify(devices, never()).updateByPrimaryKeySelective(any());
    verifyNoInteractions(extensions, channels, imports);
  }

  private static void assertLockedDeviceQuery(Example query) {
    assertTrue(query.isForUpdate());
    var criteria = query.getOredCriteria().getFirst().getAllCriteria();
    assertTrue(criteria.stream().anyMatch(criterion ->
        criterion.getCondition().startsWith("instance_key") && "isup-a".equals(criterion.getValue())));
    assertTrue(criteria.stream().anyMatch(criterion ->
        criterion.getCondition().startsWith("device_id") && "nvr-a".equals(criterion.getValue())));
  }

  @Test void backgroundRefreshPreservesDeviceIdentityDisableAndLocalConfig() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("isup-a")
        .enabled(0).createId("original-creator").configuration("{\"fusion\":{\"enabled\":true}}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      var actual = handler.upsertDevice("isup-a", "nvr-a", Map.of("status", "OFF", "channelList", List.of()));
      assertEquals(11L, actual.getId()); assertEquals(0, actual.getEnabled());
      assertEquals("trusted-creator", actual.getUpdateId()); assertEquals("trusted-tenant", actual.getTenantId());
      assertEquals("original-creator", actual.getCreateId());
      assertTrue(handler.objectMapper.readTree(actual.getConfiguration()).path("fusion").path("enabled").asBoolean());
    }
  }

  @Test void sameChannelOnTwoNvrsKeepsSeparateIdsAndDoesNotDeleteAbsentChannels() {
    var a = VideoPlatformChannel.builder().id(101L).deviceId(11L).channelId("1").enabled(0)
        .streamConfig("{\"fusion\":{\"enabled\":true}}").createId("original").build();
    var b = VideoPlatformChannel.builder().id(201L).deviceId(22L).channelId("1").enabled(1).build();
    when(channels.selectByExample(any())).thenReturn(List.of(a), List.of(b));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.batchInsertChannels(11L, "isup-a", "nvr-a", List.of(Map.of("channelId", "1", "status", "online")));
      handler.batchInsertChannels(22L, "isup-a", "nvr-b", List.of(Map.of("channelId", "1", "status", "offline")));
    }
    var captured = org.mockito.ArgumentCaptor.forClass(VideoPlatformChannel.class);
    verify(channels, times(2)).updateByPrimaryKeySelective(captured.capture());
    assertEquals(101L, captured.getAllValues().get(0).getId());
    assertEquals(201L, captured.getAllValues().get(1).getId());
    assertEquals(0, captured.getAllValues().get(0).getEnabled());
    assertTrue(captured.getAllValues().get(0).getStreamConfig().contains("fusion"));
    verify(channels, never()).deleteByPrimaryKey(any());
    var queries = org.mockito.ArgumentCaptor.forClass(Example.class);
    verify(channels, times(2)).selectByExample(queries.capture());
    assertTrue(queries.getAllValues().stream().allMatch(query -> query.getOredCriteria().getFirst()
        .getAllCriteria().stream().anyMatch(criterion -> criterion.getCondition().startsWith("device_id"))));
  }

  @Test void unchangedPollDoesNotUpdateOrInvalidateButIpChangeDoes() {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("isup-a")
        .enabled(1).createId("original-creator").tenantId("trusted-tenant").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.upsertDevice("isup-a", "nvr-a", Map.of("status", "ON", "ip", "192.0.2.1"));
      clearInvocations(devices, handler.alarmContextCache);
      handler.upsertDevice("isup-a", "nvr-a", Map.of("ip", "192.0.2.1", "status", "ON"));
      verify(devices, never()).updateByPrimaryKeySelective(any());
      verifyNoInteractions(handler.alarmContextCache);
      handler.upsertDevice("isup-a", "nvr-a", Map.of("status", "ON", "ip", "192.0.2.2"));
      verify(devices).updateByPrimaryKeySelective(saved);
      verify(handler.alarmContextCache).publishPlatformChange("isup-a");
    }
  }

  @Test void omittedNullableFieldsDoNotInvalidateValuesThatSelectiveUpdateWouldPreserve() {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("isup-a")
        .deviceName("nvr-a").deviceStatus("ON").deviceIp("192.0.2.1")
        .deviceModel("retained-model").gpsX("120.1")
        .enabled(1).tenantId("trusted-tenant").configuration("{}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.upsertDevice("isup-a", "nvr-a", Map.of("status", "ON", "ip", "192.0.2.1"));
      verify(devices, never()).updateByPrimaryKeySelective(any());
      verifyNoInteractions(handler.alarmContextCache);
    }
  }

  @Test void capabilityProbeTimePersistsWithoutInvalidationButActualCapabilityChangeRefreshes() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("isup-a")
        .deviceName("nvr-a").deviceStatus("ON").enabled(1).tenantId("trusted-tenant")
        .configuration(handler.objectMapper.writeValueAsString(Map.of("channelList", List.of(
            Map.of("channelId", "1", "ptzSupport", false, "capabilitiesCheckedAt", 1L,
                "attributes", Map.of("capabilitiesCheckedAt", 1L)))))).build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.upsertDevice("isup-a", "nvr-a", Map.of("status", "ON", "channelList", List.of(
          Map.of("channelId", "1", "ptzSupport", false, "capabilitiesCheckedAt", 2L,
              "attributes", Map.of("capabilitiesCheckedAt", 2L)))));
      verify(devices).updateByPrimaryKeySelective(saved);
      assertEquals(2L, handler.objectMapper.readTree(saved.getConfiguration())
          .path("channelList").get(0).path("capabilitiesCheckedAt").asLong());
      verifyNoInteractions(handler.alarmContextCache);
      clearInvocations(devices);
      handler.upsertDevice("isup-a", "nvr-a", Map.of("status", "ON", "channelList", List.of(
          Map.of("channelId", "1", "ptzSupport", true, "capabilitiesCheckedAt", 3L,
              "attributes", Map.of("capabilitiesCheckedAt", 3L)))));
      verify(devices).updateByPrimaryKeySelective(saved);
      verify(handler.alarmContextCache).publishPlatformChange("isup-a");
    }
  }

  @Test void registrationNeverUsesIpAsDeviceIdentityOrResetsCatalog() {
    assertNull(handler.resolveDirectSdkDeviceId(Map.of("ip", "192.0.2.1")));
    assertFalse(handler.shouldResetBeforeSync(Map.of("__resetBeforeSync", true)));
    assertEquals(0, handler.batchInsertChannels(11L, "isup-a", "nvr-a", List.of()));
    verifyNoInteractions(channels);
  }
}
