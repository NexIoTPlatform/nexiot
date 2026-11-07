package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.alarm.SdkVideoAlarmContextCache;
import cn.universal.admin.video.service.VideoPlatformProductService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class VideoDeviceImportCacheChangeTest {
  @Test void failedHikCatalogRetainsMissingIotSlotsAndNativeMappingUntilACompleteSnapshot() throws Exception {
    EntityHelper.initEntityNameMap(IoTDevice.class, new Config());
    var instances = mock(VideoPlatformInstanceMapper.class);
    var devices = mock(IoTDeviceMapper.class);
    var json = new ObjectMapper();
    var service = new VideoDeviceImportServiceImpl(instances, mock(VideoPlatformProductService.class),
        devices, mock(IoTProductMapper.class), json, mock(VideoPlatformDeviceMapper.class),
        mock(VideoPlatformChannelMapper.class), mock(SdkVideoAlarmContextCache.class));
    when(instances.selectOne(any())).thenReturn(VideoPlatformInstance.builder()
        .instanceKey("one").tenantId("tenant").build());
    var saved = IoTDevice.builder().productKey("custom").state(true)
        .configuration("{\"instanceKey\":\"one\",\"channelList\":[{\"channelId\":\"1\",\"local\":true},{\"channelId\":\"5\",\"channelNo\":37}]}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    var channel = Map.of("channelId", "1", "channelNo", 33,
        "attributes", Map.of("sdkChannel", 33, "rtspChannel", 1, "channelSeq", 1, "onlineState", "unknown"));
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "nvr", "status", "ON",
        "channelCatalogAuthoritative", false, "loginChannelCount", 8,
        "channelList", List.of(channel)), "hik_sdk"));
    var config = json.readTree(saved.getConfiguration());
    assertEquals(2, config.path("channelList").size());
    var first = config.path("channelList").path(0);
    assertEquals(33, first.path("channelNo").asInt());
    assertEquals(33, first.path("sdkChannel").asInt());
    assertEquals(1, first.path("rtspChannel").asInt());
    assertEquals("unknown", first.path("channelStatus").asText());
    assertTrue(first.path("local").asBoolean());
    assertEquals(8, config.path("loginChannelCount").asInt());
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "nvr", "status", "ON",
        "channelCatalogAuthoritative", true, "channelCount", 1,
        "channelList", List.of(channel)), "hik_sdk"));
    assertEquals(1, json.readTree(saved.getConfiguration()).path("channelList").size());
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "nvr", "status", "ON",
        "channelCatalogAuthoritative", true, "channelCount", 0,
        "channelList", List.of()), "hik_sdk"));
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "nvr", "status", "ON",
        "channelCatalogAuthoritative", false, "channelCount", 0,
        "channelList", List.of()), "hik_sdk"));
    assertEquals(0, json.readTree(saved.getConfiguration()).path("channelList").size());
  }
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"dahua_sdk", "hik_sdk", "tiandy_sdk"})
  void authoritativeEmptySdkCatalogClearsIotSlotsWithoutInventingDefaultChannel(String platformType) throws Exception {
    EntityHelper.initEntityNameMap(IoTDevice.class, new Config());
    var instances = mock(VideoPlatformInstanceMapper.class);
    var devices = mock(IoTDeviceMapper.class);
    var json = new ObjectMapper();
    var service = new VideoDeviceImportServiceImpl(instances, mock(VideoPlatformProductService.class),
        devices, mock(IoTProductMapper.class), json, mock(VideoPlatformDeviceMapper.class),
        mock(VideoPlatformChannelMapper.class), mock(SdkVideoAlarmContextCache.class));
    when(instances.selectOne(any())).thenReturn(VideoPlatformInstance.builder()
        .instanceKey("one").tenantId("tenant").build());
    var saved = IoTDevice.builder().productKey("one").state(true)
        .configuration("{\"local\":true,\"channelList\":[{\"channelId\":\"4\"}]}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "camera", "status", "ON",
        "channelCatalogAuthoritative", true, "channelCount", 0, "channelList", List.of()), platformType));
    var config = json.readTree(saved.getConfiguration());
    assertEquals(0, config.path("channelList").size());
    assertEquals(0, config.path("channelCount").asInt());
    assertTrue(config.path("channelCatalogAuthoritative").asBoolean());
    assertTrue(config.path("local").asBoolean());
  }

  @Test void failedTiandyCatalogRetainsKnownIotSlotsAndDoesNotRecreateAnEmptyDirectory() throws Exception {
    EntityHelper.initEntityNameMap(IoTDevice.class, new Config());
    var instances = mock(VideoPlatformInstanceMapper.class);
    var devices = mock(IoTDeviceMapper.class);
    var json = new ObjectMapper();
    var service = new VideoDeviceImportServiceImpl(instances, mock(VideoPlatformProductService.class),
        devices, mock(IoTProductMapper.class), json, mock(VideoPlatformDeviceMapper.class),
        mock(VideoPlatformChannelMapper.class), mock(SdkVideoAlarmContextCache.class));
    when(instances.selectOne(any())).thenReturn(VideoPlatformInstance.builder()
        .instanceKey("one").tenantId("tenant").build());
    var saved = IoTDevice.builder().productKey("custom").state(true)
        .configuration("{\"instanceKey\":\"one\",\"channelList\":[{\"channelId\":\"1\",\"local\":true},{\"channelId\":\"4\",\"nativeChannelNo\":3}]}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    var channel = Map.of("channelId", "1", "channelNo", 1,
        "attributes", Map.of("nativeChannelNo", 0, "channelSeq", 1, "onlineState", "unknown"));
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "nvr", "status", "ON",
        "channelCatalogAuthoritative", false, "loginChannelCount", 5,
        "channelList", List.of(channel)), "tiandy_sdk"));
    var config = json.readTree(saved.getConfiguration());
    assertEquals(2, config.path("channelList").size());
    assertTrue(config.path("channelList").path(0).path("local").asBoolean());
    assertEquals(0, config.path("channelList").path(0).path("nativeChannelNo").asInt(-1));
    assertEquals("unknown", config.path("channelList").path(0).path("channelStatus").asText());
    assertEquals(5, config.path("loginChannelCount").asInt());
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "nvr", "status", "ON",
        "channelCatalogAuthoritative", true, "channelCount", 0,
        "channelList", List.of()), "tiandy_sdk"));
    assertEquals(0, json.readTree(saved.getConfiguration()).path("channelList").size());
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "nvr", "status", "ON",
        "channelCatalogAuthoritative", false, "channelCount", 0,
        "channelList", List.of()), "tiandy_sdk"));
    assertEquals(0, json.readTree(saved.getConfiguration()).path("channelList").size());
  }

  @Test void repeatedSyncKeepsCacheButStateAndChannelChangesRefreshIt() throws Exception {
    EntityHelper.initEntityNameMap(IoTDevice.class, new Config());
    var instances = mock(VideoPlatformInstanceMapper.class);
    var devices = mock(IoTDeviceMapper.class);
    var cache = mock(SdkVideoAlarmContextCache.class);
    var json = new ObjectMapper();
    var service = new VideoDeviceImportServiceImpl(instances, mock(VideoPlatformProductService.class),
        devices, mock(IoTProductMapper.class), json, mock(VideoPlatformDeviceMapper.class),
        mock(VideoPlatformChannelMapper.class), cache);
    when(instances.selectOne(any())).thenReturn(VideoPlatformInstance.builder()
        .instanceKey("one").tenantId("tenant").build());
    var saved = new IoTDevice();
    saved.setState(1);
    saved.setProductKey("one");
    saved.setConfiguration("{\"local\":{\"keep\":true}}");
    when(devices.selectByExample(any())).thenReturn(List.of(saved));
    Map<String,Object> poll = Map.of("deviceId", "camera", "status", "ON",
        "channelList", List.of(Map.of("channelId", "1", "status", "online")));
    assertTrue(service.updateDeviceStatusAndConfig("one", poll, "dahua_sdk"));
    assertTrue(json.readTree(saved.getConfiguration()).path("local").path("keep").asBoolean());
    clearInvocations(devices, cache);
    // Reordered object fields must not create a false cache change.
    saved.setConfiguration(json.writerWithDefaultPrettyPrinter().writeValueAsString(
        json.readTree(saved.getConfiguration())));
    assertTrue(service.updateDeviceStatusAndConfig("one", poll, "dahua_sdk"));
    verify(devices, never()).updateByPrimaryKeySelective(any());
    verifyNoInteractions(cache);
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "camera", "status", "OFF",
        "channelList", List.of(Map.of("channelId", "1", "status", "online"))), "dahua_sdk"));
    assertEquals(0, saved.getState());
    verify(cache).publishPlatformChange("one");
    clearInvocations(cache);
    assertTrue(service.updateDeviceStatusAndConfig("one", Map.of("deviceId", "camera", "status", "OFF",
        "channelList", List.of(Map.of("channelId", "2", "status", "offline"))), "dahua_sdk"));
    verify(cache).publishPlatformChange("one");
    assertTrue(json.readTree(saved.getConfiguration()).path("local").path("keep").asBoolean());
  }
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"dahua_sdk", "hik_sdk", "tiandy_sdk"})
  void explicitDeviceAliasAndCanonicalChannelsRefreshCustomProductWithoutTouchingAnotherInstance(String platformType) throws Exception {
    EntityHelper.initEntityNameMap(IoTDevice.class, new Config());
    var instances = mock(VideoPlatformInstanceMapper.class);
    var devices = mock(IoTDeviceMapper.class);
    var cache = mock(SdkVideoAlarmContextCache.class);
    var json = new ObjectMapper();
    var service = new VideoDeviceImportServiceImpl(instances, mock(VideoPlatformProductService.class),
        devices, mock(IoTProductMapper.class), json, mock(VideoPlatformDeviceMapper.class),
        mock(VideoPlatformChannelMapper.class), cache);
    when(instances.selectOne(any())).thenReturn(VideoPlatformInstance.builder()
        .instanceKey("one").tenantId("tenant").build());
    var linked = IoTDevice.builder().id(1L).productKey("custom").deviceName("old").state(true)
        .configuration("{\"instanceKey\":\"one\",\"channelList\":[{\"channelId\":\"1\",\"channelName\":\"old\",\"custom\":true}]}").build();
    var other = IoTDevice.builder().id(2L).productKey("one")
        .configuration("{\"instanceKey\":\"different\"}").build();
    when(devices.selectByExample(any())).thenReturn(List.of(linked, other));
    var poll = Map.<String, Object>of("deviceId", "camera", "status", "ON", "deviceNameOverride", "大华球机",
        "channelList", List.of(Map.of("channelId", "1", "channelName", "露台", "channelNo", 1,
            "attributes", Map.of("nativeChannelNo", 0, "onlineState", "unknown"))),
        "channels", List.of(Map.of("channelId", "1", "channelName", "stale")));
    assertTrue(service.updateDeviceStatusAndConfig("one", poll, platformType));
    assertEquals("大华球机", linked.getDeviceName());
    var channel = json.readTree(linked.getConfiguration()).path("channelList").path(0);
    assertEquals("露台", channel.path("channelName").asText());
    assertEquals(1, channel.path("channelNo").asInt());
    assertEquals(0, channel.path("nativeChannelNo").asInt(-1));
    assertEquals("unknown", channel.path("channelStatus").asText());
    assertTrue(channel.path("custom").asBoolean());
    var scope = org.mockito.ArgumentCaptor.forClass(Example.class);
    verify(devices).selectByExample(scope.capture());
    assertTrue(scope.getValue().isForUpdate());
    var criteria = scope.getValue().getOredCriteria().getFirst().getAllCriteria();
    assertTrue(criteria.stream().anyMatch(c -> c.getCondition().startsWith("device_id")
        && "camera".equals(c.getValue())));
    assertTrue(criteria.stream().anyMatch(c -> c.getCondition().startsWith("tenant_id")
        && "tenant".equals(c.getValue())));
    assertTrue(criteria.stream().noneMatch(c -> c.getCondition().startsWith("product_key")));
    verify(devices).updateByPrimaryKeySelective(linked);
    verify(devices, never()).updateByPrimaryKeySelective(other);
    clearInvocations(devices, cache);
    assertTrue(service.updateDeviceStatusAndConfig("one", poll, platformType));
    verify(devices, never()).updateByPrimaryKeySelective(any());
    verifyNoInteractions(cache);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"dahua_sdk", "hik_sdk", "tiandy_sdk"})
  void importUsesSavedAliasAndReimportRefreshesExistingCustomProductWithoutDuplicating(String platformType) throws Exception {
    for (Class<?> entity : List.of(IoTDevice.class, cn.universal.persistence.entity.VideoPlatformDevice.class, VideoPlatformInstance.class))
      EntityHelper.initEntityNameMap(entity, new Config());
    var instances = mock(VideoPlatformInstanceMapper.class);
    var products = mock(IoTProductMapper.class);
    var devices = mock(IoTDeviceMapper.class);
    var video = mock(VideoPlatformDeviceMapper.class);
    var service = new VideoDeviceImportServiceImpl(instances, mock(VideoPlatformProductService.class),
        devices, products, new ObjectMapper(), video, mock(VideoPlatformChannelMapper.class), mock(SdkVideoAlarmContextCache.class));
    var instance = VideoPlatformInstance.builder().instanceKey("one").tenantId("tenant")
        .creatorId("creator").platformType(platformType).build();
    when(instances.selectByExample(any())).thenReturn(List.of(instance));
    when(instances.selectOne(any())).thenReturn(instance);
    when(products.getProductByProductKey("custom")).thenReturn(cn.universal.persistence.entity.IoTProduct.builder()
        .productKey("custom").name("自定义产品").build());
    when(video.selectOne(any())).thenReturn(cn.universal.persistence.entity.VideoPlatformDevice.builder()
        .deviceName("大华球机").configuration("""
          {"deviceNameOverride":"大华球机","channelList":[{"channelId":"1","channelName":"平台名称","channelStatus":"online"}]}
          """).build());
    var poll = Map.<String, Object>of("deviceId", "camera", "deviceName", "stale", "status", "ON",
        "channels", List.of(Map.of("channelId", "1", "channelName", "CH1")));
    when(devices.selectByExample(any())).thenReturn(List.of());
    try (var security = mockStatic(cn.universal.security.utils.SecurityUtils.class)) {
      security.when(cn.universal.security.utils.SecurityUtils::getUnionIdQuietly).thenReturn(null);
      assertEquals("customcamera", service.importDevice("one", poll, "custom"));
    }
    var inserted = org.mockito.ArgumentCaptor.forClass(IoTDevice.class);
    verify(devices).insertSelective(inserted.capture());
    assertEquals("大华球机", inserted.getValue().getDeviceName());
    assertTrue(inserted.getValue().getConfiguration().contains("平台名称"));
    assertFalse(inserted.getValue().getConfiguration().contains("CH1"));
    var existing = inserted.getValue();
    existing.setId(99L);
    existing.setDeviceName("old name");
    existing.setConfiguration("{\"instanceKey\":\"one\",\"local\":true,\"channelList\":[{\"channelId\":\"1\",\"channelName\":\"old\"}]}");
    when(devices.selectByExample(any())).thenReturn(List.of(existing));
    clearInvocations(devices);
    assertEquals("EXISTS:customcamera", service.importDevice("one", poll, "custom"));
    assertEquals("大华球机", existing.getDeviceName());
    assertTrue(existing.getConfiguration().contains("平台名称"));
    assertTrue(existing.getConfiguration().contains("\"local\":true"));
    verify(devices, never()).insertSelective(any());
    verify(devices).updateByPrimaryKeySelective(existing);
  }
}
