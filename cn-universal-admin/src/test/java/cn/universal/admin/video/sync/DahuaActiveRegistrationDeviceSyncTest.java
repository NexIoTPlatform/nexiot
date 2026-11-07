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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class DahuaActiveRegistrationDeviceSyncTest {
  private final DahuaSdkDeviceSyncHandler handler = new DahuaSdkDeviceSyncHandler(mock(VideoPlatformAdapterRegistry.class));
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformDeviceExtMapper extensions = mock(VideoPlatformDeviceExtMapper.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoDeviceImportService imports = mock(VideoDeviceImportService.class);
  private final VideoPlatformInstance instance = VideoPlatformInstance.builder().instanceKey("dh-a")
      .creatorId("trusted-creator").tenantId("trusted-tenant").platformType("dahua_sdk").build();
  @BeforeEach void setup() {
    EntityHelper.initEntityNameMap(VideoPlatformDevice.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformDeviceExt.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformChannel.class, new Config());
    handler.deviceMapper = devices; handler.channelMapper = channels; handler.instanceMapper = instances;
    handler.deviceExtMapper = extensions; handler.transactionExecutor = new DeviceSyncTransactionExecutor();
    handler.objectMapper = new ObjectMapper();
    handler.alarmContextCache = mock(cn.universal.admin.video.alarm.SdkVideoAlarmContextCache.class);
    ReflectionTestUtils.setField(handler, "importService", imports);
    when(instances.selectOne(any())).thenReturn(instance); when(extensions.selectByExample(any())).thenReturn(List.of());
    when(channels.selectByExample(any())).thenReturn(List.of());
  }
  @Test void deletedOfflineRegistrationNeverRecreatesCatalogOrIot() throws Exception {
    when(devices.selectByExample(any())).thenReturn(List.of());
    handler.syncRegisteredDevice(instance, Map.of("deviceId", "register-a", "status", "offline", "online", false));
    verify(devices, never()).insertSelective(any()); verifyNoInteractions(extensions, channels, imports);
    var query = org.mockito.ArgumentCaptor.forClass(Example.class); verify(devices).selectByExample(query.capture());
    assertTrue(query.getValue().isForUpdate()); assertTrue(handler.preserveLocalConfiguration());
  }
  @Test void registeredRefreshPreservesDisabledDeviceAndFusionWithoutImportingIt() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("register-a").instanceKey("dh-a")
        .enabled(0).createId("original-creator").configuration("{\"fusion\":{\"enabled\":true}}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved)); when(devices.selectOne(any())).thenReturn(saved);
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncRegisteredDevice(instance, Map.of("deviceId", "register-a", "status", "offline", "online", false));
    }
    assertEquals(0, saved.getEnabled()); assertEquals("OFF", saved.getDeviceStatus());
    assertTrue(handler.objectMapper.readTree(saved.getConfiguration()).path("fusion").path("enabled").asBoolean());
    verifyNoInteractions(imports); assertTrue(handler.preserveLocalConfiguration());
    var ext = org.mockito.ArgumentCaptor.forClass(VideoPlatformDeviceExt.class); verify(extensions).insertSelective(ext.capture());
    var metadata = handler.objectMapper.readTree(ext.getValue().getExtField3());
    assertEquals("active_registration", metadata.path("loginMode").asText()); assertEquals("sdk_gateway", metadata.path("streamMode").asText());
  }
  @Test void directRefreshPreservesDisabledDeviceAndFusionConfiguration() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("direct-a").instanceKey("dh-a")
        .enabled(0).configuration("{\"fusion\":{\"enabled\":true}}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncSingleDevice("dh-a", "direct-a", Map.of("deviceId", "direct-a", "deviceStatus", "online"));
    }
    assertEquals(0, saved.getEnabled());
    assertTrue(handler.objectMapper.readTree(saved.getConfiguration()).path("fusion").path("enabled").asBoolean());
    verifyNoInteractions(imports);
    assertTrue(handler.preserveLocalConfiguration());
  }

  @Test void offlineSnapshotDoesNotRecreateADeletedChannelOrResetLocalChannelConfiguration() throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("register-a").instanceKey("dh-a")
        .enabled(0).configuration("{\"fusion\":{\"enabled\":true}}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(devices.selectOne(any())).thenReturn(saved);
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncRegisteredDevice(instance, Map.of("deviceId", "register-a", "status", "offline",
          "online", false, "channelList", List.of(Map.of("channelId", "deleted-channel", "channelStatus", "offline"))));
    }
    verify(channels, never()).insertSelective(any());
    verify(channels, never()).updateByPrimaryKeySelective(any());
    var update = org.mockito.ArgumentCaptor.forClass(VideoPlatformChannel.class);
    var scope = org.mockito.ArgumentCaptor.forClass(Example.class);
    verify(channels).updateByExampleSelective(update.capture(), scope.capture());
    assertEquals("offline", update.getValue().getChannelStatus());
    assertNull(update.getValue().getEnabled()); assertNull(update.getValue().getStreamConfig());
    assertTrue(scope.getValue().getOredCriteria().getFirst().getAllCriteria().stream()
        .anyMatch(criterion -> criterion.getCondition().startsWith("device_id") && Long.valueOf(11).equals(criterion.getValue())));
    assertTrue(handler.objectMapper.readTree(saved.getConfiguration()).path("fusion").path("enabled").asBoolean());
    assertEquals(0, saved.getEnabled()); verifyNoInteractions(imports);
  }
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void directAndRegisteredRefreshUseSavedAliasesForIot(boolean registration) {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("camera").instanceKey("dh-a")
        .enabled(1).configuration("{\"deviceNameOverride\":\"大华球机\",\"channelNameOverrides\":{\"1\":\"平台名称\"}}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(devices.selectOne(any())).thenReturn(saved);
    when(imports.updateDeviceStatusAndConfig(any(), any(), any())).thenReturn(true);
    var incoming = new java.util.LinkedHashMap<String, Object>(Map.of("deviceId", "camera", "deviceName", "camera",
        "status", "online", "channelList", List.of(Map.of("channelId", "1", "channelName", "设备原名"),
            Map.of("channelId", "1_sub", "channelName", "设备原名 Sub"))));
    if (registration) incoming.put("loginMode", "active_registration");
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncSingleDevice("dh-a", "camera", incoming);
    }
    var payload = org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(imports).updateDeviceStatusAndConfig(eq("dh-a"), payload.capture(), eq("dahua_sdk"));
    assertEquals("大华球机", payload.getValue().get("deviceName"));
    var snapshot = (List<?>) payload.getValue().get("channelList");
    assertEquals("平台名称", ((Map<?, ?>) snapshot.getFirst()).get("channelName"));
    assertEquals("平台名称 Sub", ((Map<?, ?>) snapshot.getLast()).get("channelName"));
    assertEquals("设备原名", ((Map<?, ?>) payload.getValue().get("sourceChannelNames")).get("1"));
    assertEquals("设备原名", ((Map<?, ?>) ((List<?>) incoming.get("channelList")).getFirst()).get("channelName"));
    if (registration) verify(imports).importDevice(eq("dh-a"), any(), isNull());
    else verify(imports, never()).importDevice(any(), any(), any());
  }

  @Test void firstRefreshPreservesHistoricalManualDeviceNameAndThenTracksSources() {
    var legacy = VideoPlatformDevice.builder().deviceName("大华球机").configuration("{}").build();
    var result = handler.applyDeviceNames(legacy, Map.of("deviceName", "192.168.31.101"));
    assertEquals("大华球机", result.get("deviceName"));
    assertEquals("大华球机", result.get("deviceNameOverride"));
    assertEquals("192.168.31.101", result.get("sourceDeviceName"));
    legacy.setConfiguration("{\"sourceDeviceName\":\"previous-source\"}");
    result = handler.applyDeviceNames(legacy, Map.of("deviceName", "new-source"));
    assertEquals("new-source", result.get("deviceName"));
    assertFalse(result.containsKey("deviceNameOverride"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void authoritativeEmptyCatalogRemovesOldSlotsForDirectAndRegisteredDevices(boolean registration) throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("camera").instanceKey("dh-a")
        .enabled(0).configuration("{}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(channels.selectByExample(any())).thenReturn(List.of(
        VideoPlatformChannel.builder().id(41L).deviceId(11L).channelId("4").build(),
        VideoPlatformChannel.builder().id(42L).deviceId(11L).channelId("4_sub").build()));
    var incoming = new java.util.LinkedHashMap<String, Object>(Map.of("deviceId", "camera",
        "status", "online", "channelCatalogAuthoritative", true, "channelList", List.of()));
    if (registration) incoming.put("loginMode", "active_registration");
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncSingleDevice("dh-a", "camera", incoming);
    }
    verify(channels).deleteByPrimaryKey(41L);
    verify(channels).deleteByPrimaryKey(42L);
    verify(channels, never()).insertSelective(any());
    assertTrue(handler.objectMapper.valueToTree(
        handler.objectMapper.readValue(saved.getConfiguration() == null ? "{}" : saved.getConfiguration(), Map.class))
        .path("channelList").isEmpty());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void authoritativeCatalogRemovesOnlyMissingSlotsAndKeepsLocalSettings(boolean registration) {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("camera").instanceKey("dh-a")
        .enabled(0).configuration("{}").build();
    var bound = VideoPlatformChannel.builder().id(21L).deviceId(11L).channelId("2")
        .enabled(0).streamConfig("{\"local\":true}").build();
    var empty = VideoPlatformChannel.builder().id(41L).deviceId(11L).channelId("4").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(channels.selectByExample(any())).thenReturn(List.of(bound, empty), List.of(bound));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      var incoming = new java.util.LinkedHashMap<String, Object>(Map.of("deviceId", "camera",
          "status", "online", "channelCatalogAuthoritative", true,
          "channelList", List.of(Map.of("channelId", "2", "channelNo", 2, "channelStatus", "OFF"))));
      if (registration) incoming.put("loginMode", "active_registration");
      handler.syncSingleDevice("dh-a", "camera", incoming);
    }
    verify(channels).deleteByPrimaryKey(41L);
    verify(channels, never()).deleteByPrimaryKey(21L);
    var update = org.mockito.ArgumentCaptor.forClass(VideoPlatformChannel.class);
    verify(channels).updateByPrimaryKeySelective(update.capture());
    assertEquals("2", update.getValue().getChannelId());
    assertEquals("OFF", update.getValue().getChannelStatus());
    assertEquals(0, update.getValue().getEnabled());
    assertTrue(update.getValue().getStreamConfig().contains("\"local\":true"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void uncertainOrOfflineCatalogCannotClearRegisteredChannels(boolean offline) {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("camera").instanceKey("dh-a")
        .enabled(0).configuration("{}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncSingleDevice("dh-a", "camera", Map.of("deviceId", "camera",
          "status", offline ? "offline" : "online", "loginMode", "active_registration",
          "channelCatalogAuthoritative", offline, "channelList", List.of()));
    }
    verify(channels, never()).deleteByPrimaryKey(any());
  }

  @Test void failedDirectQueryCannotDeletePreviouslyDiscoveredSlots() {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("camera").instanceKey("dh-a")
        .enabled(0).configuration("{}").build();
    var previous = VideoPlatformChannel.builder().id(51L).deviceId(11L).channelId("5").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(channels.selectByExample(any())).thenAnswer(invocation -> {
      Example query = invocation.getArgument(0);
      return query.getOredCriteria().getFirst().getAllCriteria().stream()
          .anyMatch(criterion -> criterion.getCondition().startsWith("channel_id"))
              ? List.of() : List.of(previous);
    });
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncSingleDevice("dh-a", "camera", Map.of("deviceId", "camera", "status", "online",
          "channelCatalogAuthoritative", false,
          "channelList", List.of(Map.of("channelId", "1", "channelNo", 1, "channelStatus", "UNKNOWN"))));
    }
    verify(channels, never()).deleteByPrimaryKey(any());
    verify(channels).insertSelective(any());
    assertTrue(handler.preserveChannelDirectory());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void nonauthoritativeCatalogKeepsKnownDirectoryWithoutImportingCapacitySlots(boolean registration) throws Exception {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("camera").instanceKey("dh-a")
        .enabled(0).configuration("{\"channelList\":[{\"channelId\":\"5\",\"channelName\":\"已配置通道\"}]}").build();
    var previous = VideoPlatformChannel.builder().id(51L).deviceId(11L).channelId("5").enabled(0).build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    when(channels.selectByExample(any())).thenReturn(List.of(previous));
    var incoming = new java.util.LinkedHashMap<String, Object>(Map.of("deviceId", "camera", "status", "online",
        "channelCatalogAuthoritative", false,
        "channelList", List.of(Map.of("channelId", "1", "channelName", "CH1", "channelStatus", "UNKNOWN"))));
    if (registration) incoming.put("loginMode", "active_registration");
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncSingleDevice("dh-a", "camera", incoming);
    }
    verify(channels, never()).deleteByPrimaryKey(any());
    verify(channels, never()).insertSelective(any());
    var storedChannels = handler.objectMapper.readTree(saved.getConfiguration()).path("channelList");
    assertEquals(1, storedChannels.size());
    assertEquals("5", storedChannels.get(0).path("channelId").asText());
    assertEquals("UNKNOWN", storedChannels.get(0).path("channelStatus").asText());
  }

  @Test void ipcSignalEvidenceUpdatesKnownStatusWithoutPruningOrReplacingTheSavedDirectory() {
    var saved = VideoPlatformDevice.builder().configuration("""
        {"channelNameOverrides":{"1":"平台别名"},"channelList":[
          {"channelId":"1","channelName":"设备原名","channelStatus":"UNKNOWN"},
          {"channelId":"1_sub","channelName":"设备原名 Sub","channelStatus":"UNKNOWN"},
          {"channelId":"5","channelName":"历史通道","channelStatus":"ON"}
        ]}
        """).build();
    var incoming = Map.<String, Object>of("deviceStatus", "ON", "channelCatalogAuthoritative", false,
        "channelList", List.of(
            Map.of("channelId", "1", "channelName", "设备原名", "channelStatus", "ON",
                "onlineState", "online", "channelStateSource", "NET_DEVSTATE_VIDEOLOST", "videoLost", false),
            Map.of("channelId", "1_sub", "channelName", "设备原名 Sub", "channelStatus", "ON",
                "onlineState", "online", "channelStateSource", "NET_DEVSTATE_VIDEOLOST", "videoLost", false)));

    var merged = handler.applyDeviceNames(saved, incoming);
    var result = handler.objectMapper.valueToTree(merged).path("channelList");

    assertEquals(false, merged.get("channelCatalogAuthoritative"));
    assertEquals(3, result.size());
    assertEquals("平台别名", result.get(0).path("channelName").asText());
    assertEquals("平台别名 Sub", result.get(1).path("channelName").asText());
    for (int index : new int[] {0, 1}) {
      assertEquals("ON", result.get(index).path("channelStatus").asText());
      assertEquals("online", result.get(index).path("onlineState").asText());
      assertEquals("NET_DEVSTATE_VIDEOLOST", result.get(index).path("channelStateSource").asText());
      assertFalse(result.get(index).path("videoLost").asBoolean());
    }
    assertEquals("5", result.get(2).path("channelId").asText());
    assertEquals("UNKNOWN", result.get(2).path("channelStatus").asText());
    verifyNoInteractions(channels);
  }

  @Test void offlineDirectSnapshotCannotRecreateChannelsFromOldConfiguration() {
    var saved = VideoPlatformDevice.builder().id(11L).deviceId("camera").instanceKey("dh-a")
        .enabled(0).configuration("{\"channelList\":[{\"channelId\":\"5\"}]}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncSingleDevice("dh-a", "camera", Map.of("deviceId", "camera", "status", "offline", "online", false,
          "channelCatalogAuthoritative", false, "channelList", List.of(Map.of("channelId", "6"))));
    }
    verify(channels, never()).deleteByPrimaryKey(any());
    verify(channels, never()).insertSelective(any());
    verify(channels, never()).updateByPrimaryKeySelective(any());
  }

}
