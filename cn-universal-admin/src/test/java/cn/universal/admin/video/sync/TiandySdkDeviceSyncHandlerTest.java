package cn.universal.admin.video.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.alarm.SdkVideoAlarmContextCache;
import cn.universal.admin.video.service.VideoDeviceImportService;
import cn.universal.admin.video.service.VideoDeviceNames;
import cn.universal.admin.video.service.VideoPlatformDeviceCleanService;
import cn.universal.dm.video.VideoPlatformAdapter;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class TiandySdkDeviceSyncHandlerTest {
  private final VideoPlatformAdapterRegistry registry = mock(VideoPlatformAdapterRegistry.class);
  private final TiandySdkDeviceSyncHandler handler =
      new TiandySdkDeviceSyncHandler(registry);
  private final VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
  private final VideoPlatformDeviceExtMapper extensions = mock(VideoPlatformDeviceExtMapper.class);
  private final VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final VideoDeviceImportService imports = mock(VideoDeviceImportService.class);
  private final Map<String, VideoPlatformChannel> rows = new LinkedHashMap<>();
  private VideoPlatformDevice saved;

  @BeforeEach
  void setup() {
    EntityHelper.initEntityNameMap(VideoPlatformDevice.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformDeviceExt.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformChannel.class, new Config());
    handler.deviceMapper = devices;
    handler.channelMapper = channels;
    handler.instanceMapper = instances;
    handler.deviceExtMapper = extensions;
    handler.transactionExecutor = new DeviceSyncTransactionExecutor();
    handler.objectMapper = new ObjectMapper();
    handler.alarmContextCache = mock(SdkVideoAlarmContextCache.class);
    ReflectionTestUtils.setField(handler, "importService", imports);
    when(instances.selectOne(any())).thenReturn(VideoPlatformInstance.builder()
        .instanceKey("tiandy-a").creatorId("trusted-creator")
        .tenantId("trusted-tenant").platformType("tiandy_sdk").build());
    when(extensions.selectByExample(any())).thenReturn(List.of());
    saved = VideoPlatformDevice.builder().id(11L).deviceId("nvr-a").instanceKey("tiandy-a")
        .deviceName("平台 NVR").enabled(0).configuration("{}").build();
    when(devices.selectByExample(any())).thenAnswer(invocation -> List.of(saved));
    when(devices.selectOne(any())).thenAnswer(invocation -> saved);
    when(channels.selectByExample(any())).thenAnswer(invocation -> {
      Example query = invocation.getArgument(0);
      Object channelId = query.getOredCriteria().getFirst().getAllCriteria().stream()
          .filter(criterion -> criterion.getCondition().startsWith("channel_id"))
          .map(Example.Criterion::getValue).findFirst().orElse(null);
      if (channelId == null) return new ArrayList<>(rows.values());
      VideoPlatformChannel row = rows.get(String.valueOf(channelId));
      return row == null ? List.of() : List.of(row);
    });
    when(channels.insertSelective(any())).thenAnswer(invocation -> {
      VideoPlatformChannel row = invocation.getArgument(0);
      row.setId(100L + rows.size());
      rows.put(row.getChannelId(), row);
      return 1;
    });
    when(channels.updateByPrimaryKeySelective(any())).thenAnswer(invocation -> {
      VideoPlatformChannel row = invocation.getArgument(0);
      rows.put(row.getChannelId(), row);
      return 1;
    });
    when(channels.deleteByPrimaryKey(any())).thenAnswer(invocation -> {
      Object id = invocation.getArgument(0);
      rows.values().removeIf(row -> id.equals(row.getId()));
      return 1;
    });
    when(channels.updateByExampleSelective(any(), any())).thenAnswer(invocation -> {
      VideoPlatformChannel update = invocation.getArgument(0);
      rows.values().forEach(row -> row.setChannelStatus(update.getChannelStatus()));
      return rows.size();
    });
  }

  @Test
  void authoritativeEmptyCatalogDeletesOldSlotsAndPreservesLocalDeviceSettings() throws Exception {
    rememberKnownDirectory();

    sync(snapshot(true, List.of()));

    assertTrue(rows.isEmpty());
    verify(channels, times(4)).deleteByPrimaryKey(any());
    verify(channels, never()).insertSelective(any());
    assertEquals(0, saved.getEnabled());
    JsonNode config = configuration();
    assertTrue(config.path("channelList").isEmpty());
    assertEquals(0, config.path("channelCount").asInt());
    assertEquals(8, config.path("loginChannelCount").asInt());
    assertTrue(config.path("channelCatalogAuthoritative").asBoolean());
    assertTrue(config.path("fusion").path("enabled").asBoolean());
  }

  @Test
  void authoritativeLogicalCatalogPreservesNativeMappingsAliasesAndManualSettings() throws Exception {
    rememberKnownDirectory();
    rememberRow("3", 41L);
    rememberRow("3_sub", 42L);

    sync(snapshot(true, sourceCatalog()));

    assertEquals(List.of("1", "1_sub", "2", "2_sub"), new ArrayList<>(rows.keySet()));
    verify(channels).deleteByPrimaryKey(41L);
    verify(channels).deleteByPrimaryKey(42L);
    verify(channels, never()).insertSelective(any());
    assertEquals("平台 NVR", saved.getDeviceName());
    assertEquals(0, saved.getEnabled());
    for (String id : rows.keySet()) {
      VideoPlatformChannel row = rows.get(id);
      int logical = Integer.parseInt(VideoDeviceNames.physicalId(id));
      assertEquals(String.valueOf(logical), row.getChannelNo());
      assertEquals(0, row.getEnabled());
      assertEquals(logical == 1 ? "东门平台名" + suffix(id) : "设备西门" + suffix(id),
          row.getChannelName());
      JsonNode stream = handler.objectMapper.readTree(row.getStreamConfig());
      assertTrue(stream.path("manual").asBoolean());
      assertEquals("tcp", stream.path("transport").asText());
      JsonNode metadata = handler.objectMapper.readTree(row.getExtField3());
      assertEquals(logical - 1, metadata.path("sdkChannel").asInt());
      assertEquals(logical, metadata.path("rtspChannel").asInt());
      assertTrue(metadata.path("localAlarmPolicy").asBoolean());
    }
    JsonNode config = configuration();
    assertEquals(2, config.path("channelCount").asInt());
    assertEquals("设备东门", config.path("sourceChannelNames").path("1").asText());
    assertTrue(config.path("fusion").path("enabled").asBoolean());
    Map<String, Object> effective = importedSnapshot();
    assertEquals("平台 NVR", effective.get("deviceName"));
    assertEquals(config.path("channelList"), handler.objectMapper.valueToTree(effective.get("channelList")));
    assertEquals(2, effective.get("channelCount"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void uncertainCapacityCatalogKeepsKnownDirectorySourcesAndAliases(boolean missingMarker) throws Exception {
    rememberKnownDirectory();
    Map<String, Object> incoming = snapshot(false, fallbackCatalog(8));
    incoming.put("channelCount", 8);
    if (missingMarker) incoming.remove("channelCatalogAuthoritative");

    sync(incoming);

    assertEquals(List.of("1", "1_sub", "2", "2_sub"), new ArrayList<>(rows.keySet()));
    verify(channels, never()).deleteByPrimaryKey(any());
    verify(channels, never()).insertSelective(any());
    assertEquals("东门平台名", rows.get("1").getChannelName());
    assertEquals("东门平台名 Sub", rows.get("1_sub").getChannelName());
    assertEquals("设备西门", rows.get("2").getChannelName());
    assertEquals("设备西门 Sub", rows.get("2_sub").getChannelName());
    assertTrue(rows.values().stream().allMatch(row -> "UNKNOWN".equals(row.getChannelStatus())));
    JsonNode config = configuration();
    assertEquals("设备东门", config.path("sourceChannelNames").path("1").asText());
    assertEquals("设备西门 Sub", config.path("sourceChannelNames").path("2_sub").asText());
    assertEquals(4, config.path("channelList").size());
    assertEquals(2, config.path("channelCount").asInt());
    assertFalse(config.path("channelCatalogAuthoritative").asBoolean());
    Map<String, Object> effective = importedSnapshot();
    assertEquals(16, ((List<?>) incoming.get("channelList")).size());
    assertEquals(4, ((List<?>) effective.get("channelList")).size());
    assertEquals(2, effective.get("channelCount"));
    assertEquals(8, effective.get("loginChannelCount"));
    assertEquals(config.path("sourceChannelNames"), handler.objectMapper.valueToTree(effective.get("sourceChannelNames")));
    JsonNode first = handler.objectMapper.valueToTree(((List<?>) effective.get("channelList")).getFirst());
    assertEquals("东门平台名", first.path("channelName").asText());
    assertEquals(0, first.path("sdkChannel").asInt());
    assertEquals(1, first.path("rtspChannel").asInt());
  }

  @Test
  void missingKnownChannelsBecomeUnknownDuringUncertainOnlineRefresh() throws Exception {
    rememberKnownDirectory();
    sync(snapshot(false, sourceCatalog().subList(0, 2)));

    assertEquals("ON", saved.getDeviceStatus());
    assertEquals("ON", rows.get("1").getChannelStatus());
    assertEquals("UNKNOWN", rows.get("2").getChannelStatus());
    assertEquals("UNKNOWN", rows.get("2_sub").getChannelStatus());
    assertEquals("unknown", handler.objectMapper.readTree(rows.get("2").getExtField3())
        .path("onlineState").asText());
    assertEquals(4, configuration().path("channelList").size());
    verify(channels, never()).deleteByPrimaryKey(any());
    verify(channels, never()).insertSelective(any());
  }

  @Test
  void repeatedFallbackCannotRepopulateKnownEmptyCatalog() throws Exception {
    saved.setConfiguration(VideoDeviceNames.encode(Map.of("channelList", List.of(),
        "channelCatalogAuthoritative", true, "fusion", Map.of("enabled", true))));

    sync(snapshot(false, fallbackCatalog(8)));
    sync(snapshot(false, fallbackCatalog(8)));

    assertTrue(rows.isEmpty());
    assertTrue(configuration().path("channelList").isEmpty());
    assertEquals(0, configuration().path("channelCount").asInt());
    assertFalse(configuration().path("channelCatalogAuthoritative").asBoolean());
    verify(channels, never()).insertSelective(any());
    verify(channels, never()).deleteByPrimaryKey(any());
    verify(channels, never()).updateByPrimaryKeySelective(any());
  }

  @Test
  void offlineCatalogOnlyMarksExistingChannelsOfflineAndKeepsManualConfiguration() throws Exception {
    rememberKnownDirectory();
    Map<String, Object> incoming = snapshot(true, fallbackCatalog(8));
    incoming.put("deviceStatus", "OFF");
    incoming.put("online", false);

    sync(incoming);

    assertEquals(4, rows.size());
    assertEquals("OFF", saved.getDeviceStatus());
    assertEquals(0, saved.getEnabled());
    verify(channels, never()).deleteByPrimaryKey(any());
    verify(channels, never()).insertSelective(any());
    verify(channels, never()).updateByPrimaryKeySelective(any());
    ArgumentCaptor<VideoPlatformChannel> update = ArgumentCaptor.forClass(VideoPlatformChannel.class);
    ArgumentCaptor<Example> scope = ArgumentCaptor.forClass(Example.class);
    verify(channels).updateByExampleSelective(update.capture(), scope.capture());
    assertEquals("offline", update.getValue().getChannelStatus());
    assertNull(update.getValue().getEnabled());
    assertNull(update.getValue().getStreamConfig());
    assertTrue(scope.getValue().getOredCriteria().getFirst().getAllCriteria().stream()
        .anyMatch(criterion -> criterion.getCondition().startsWith("device_id")
            && Long.valueOf(11).equals(criterion.getValue())));
    for (VideoPlatformChannel row : rows.values()) {
      assertEquals("offline", row.getChannelStatus());
      assertEquals(0, row.getEnabled());
      assertTrue(handler.objectMapper.readTree(row.getStreamConfig()).path("manual").asBoolean());
    }
    JsonNode config = configuration();
    assertEquals(4, config.path("channelList").size());
    assertEquals(2, config.path("channelCount").asInt());
    assertFalse(config.path("channelCatalogAuthoritative").asBoolean());
    assertTrue(config.path("fusion").path("enabled").asBoolean());
    config.path("channelList").forEach(channel -> {
      assertEquals("OFF", channel.path("channelStatus").asText());
      assertEquals("offline", channel.path("onlineState").asText());
    });
  }

  @Test
  void authoritativeUnknownChannelsDoNotInheritOnlineDeviceStatus() throws Exception {
    rememberKnownDirectory();
    List<Map<String, Object>> unknown = sourceCatalog();
    unknown.forEach(channel -> {
      channel.put("channelStatus", "UNKNOWN");
      channel.put("onlineState", "unknown");
    });

    sync(snapshot(true, unknown));

    assertEquals("ON", saved.getDeviceStatus());
    assertTrue(rows.values().stream().allMatch(row -> "UNKNOWN".equals(row.getChannelStatus())));
    for (VideoPlatformChannel row : rows.values())
      assertEquals("unknown", handler.objectMapper.readTree(row.getExtField3()).path("onlineState").asText());
  }

  @Test
  void directScansNeverResetManualEnablementBeforeSync() {
    assertFalse(handler.shouldResetBeforeSync(Map.of()));
    assertFalse(handler.shouldResetBeforeSync(Map.of("ip", "192.168.31.101")));
    assertTrue(handler.preserveLocalConfiguration());
  }

  @Test
  void failedDeviceReadDoesNotDisableDevicesOrModifyTheirChannels() throws Exception {
    VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
    handler.cleanService = mock(VideoPlatformDeviceCleanService.class);
    when(registry.getAdapter("tiandy_sdk")).thenReturn(adapter);
    when(adapter.listDevices(any(), any())).thenThrow(new IllegalStateException("device unavailable"));

    DeviceSyncHandler.DeviceSyncResult result = handler.syncDevices(VideoPlatformInstance.builder()
        .instanceKey("tiandy-a").platformType("tiandy_sdk").build(), Map.of("ip", "192.168.31.9"));

    assertEquals(0, result.getTotalDevices());
    assertEquals(1, result.getErrors().size());
    verifyNoInteractions(handler.cleanService, devices, channels, extensions, imports);
  }

  private void rememberKnownDirectory() {
    List<Map<String, Object>> canonical = sourceCatalog();
    canonical.get(0).put("channelName", "东门平台名");
    canonical.get(1).put("channelName", "东门平台名 Sub");
    saved.setConfiguration(VideoDeviceNames.encode(Map.of(
        "channelList", canonical, "channelCatalogAuthoritative", true,
        "deviceNameOverride", "平台 NVR", "sourceDeviceName", "设备 NVR",
        "channelNameOverrides", Map.of("1", "东门平台名"),
        "sourceChannelNames", Map.of("1", "设备东门", "1_sub", "设备东门 Sub",
            "2", "设备西门", "2_sub", "设备西门 Sub"),
        "fusion", Map.of("enabled", true))));
    rememberRow("1", 21L);
    rememberRow("1_sub", 22L);
    rememberRow("2", 23L);
    rememberRow("2_sub", 24L);
  }

  private void rememberRow(String id, long primaryKey) {
    rows.put(id, VideoPlatformChannel.builder().id(primaryKey).deviceId(11L)
        .instanceKey("tiandy-a").platformDeviceId("nvr-a").channelId(id).enabled(0)
        .channelStatus("ON").streamConfig("{\"manual\":true,\"transport\":\"udp\"}")
        .extField3("{\"localAlarmPolicy\":true}").build());
  }

  private List<Map<String, Object>> sourceCatalog() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (int physical = 1; physical <= 2; physical++) {
      for (boolean sub : List.of(false, true)) {
        String id = physical + (sub ? "_sub" : "");
        Map<String, Object> channel = new LinkedHashMap<>();
        channel.put("channelId", id);
        channel.put("channelName", (physical == 1 ? "设备东门" : "设备西门") + suffix(id));
        channel.put("channelNo", physical);
        channel.put("sdkChannel", physical - 1);
        channel.put("rtspChannel", physical);
        channel.put("attributes", Map.of("nativeChannelNo", physical - 1));
        channel.put("streamType", sub ? "sub" : "main");
        channel.put("channelStatus", "ON");
        channel.put("onlineState", "online");
        channel.put("streamConfig", Map.of("transport", "tcp"));
        result.add(channel);
      }
    }
    return result;
  }

  private List<Map<String, Object>> fallbackCatalog(int capacity) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (int physical = 1; physical <= capacity; physical++) {
      for (boolean sub : List.of(false, true)) {
        String id = physical + (sub ? "_sub" : "");
        result.add(new LinkedHashMap<>(Map.of(
            "channelId", id, "channelName", "CH" + physical + suffix(id),
            "channelNo", physical, "sdkChannel", physical - 1,
            "rtspChannel", physical, "channelStatus", "UNKNOWN", "onlineState", "unknown",
            "attributes", Map.of("channelNameSource", "fallback", "nativeChannelNo", physical - 1))));
      }
    }
    return result;
  }

  private Map<String, Object> snapshot(boolean authoritative, List<Map<String, Object>> catalog) {
    return new LinkedHashMap<>(Map.of("deviceId", "nvr-a", "deviceName", "设备 NVR",
        "deviceStatus", "ON", "channelCatalogAuthoritative", authoritative,
        "loginChannelCount", 8, "channelList", catalog));
  }

  private void sync(Map<String, Object> incoming) {
    try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      handler.syncSingleDevice("tiandy-a", "nvr-a", incoming);
    }
  }

  private JsonNode configuration() throws Exception {
    return handler.objectMapper.readTree(saved.getConfiguration());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Map<String, Object> importedSnapshot() {
    ArgumentCaptor<Map> payload = ArgumentCaptor.forClass(Map.class);
    verify(imports).updateDeviceStatusAndConfig(eq("tiandy-a"), payload.capture(), eq("tiandy_sdk"));
    return payload.getValue();
  }

  private static String suffix(String id) {
    return id.endsWith("_sub") ? " Sub" : "";
  }
}
