package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.alarm.SdkVideoAlarmContextCache;
import cn.universal.admin.video.service.VideoSdkThingModelTemplate;
import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.SysDictType;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.persistence.mapper.admin.SysDictTypeMapper;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

/** 验证页面默认导入路径，使用真实产品服务、事件目录和 WVP 模板。 */
class WvpProductInitializationTest {
  private static final String INSTANCE_KEY = "wvp-import-test";
  private static final String DICT_TYPE = "video_sdk_event_wvp";
  private static final String DEVICE_ID = "41010500002000000001";
  private final ObjectMapper json = new ObjectMapper();
  private final VideoPlatformInstanceMapper instances = mock(VideoPlatformInstanceMapper.class);
  private final IoTProductMapper products = mock(IoTProductMapper.class);
  private final IoTDeviceMapper devices = mock(IoTDeviceMapper.class);
  private final SysDictDataMapper dictionaries = mock(SysDictDataMapper.class);
  private final IoTCacheInvalidationPublisher invalidation = mock(IoTCacheInvalidationPublisher.class);
  private final AtomicReference<IoTProduct> storedProduct = new AtomicReference<>();
  private VideoSdkEventCatalog catalog;
  private VideoDeviceImportServiceImpl importer;
  private List<SysDictData> seed;

  @BeforeEach
  void setup() throws Exception {
    for (Class<?> entity : List.of(
        IoTProduct.class, IoTDevice.class, VideoPlatformInstance.class, VideoPlatformDevice.class)) {
      EntityHelper.initEntityNameMap(entity, new Config());
    }
    var instance = VideoPlatformInstance.builder()
        .instanceKey(INSTANCE_KEY).name("测试国标平台").platformType("wvp")
        .tenantId("trusted-tenant").creatorId("trusted-creator").build();
    when(instances.selectByExample(any())).thenReturn(List.of(instance));
    when(products.getProductByProductKey(INSTANCE_KEY)).thenAnswer(call -> storedProduct.get());
    when(products.insertSelective(any(IoTProduct.class))).thenAnswer(call -> {
      IoTProduct product = call.getArgument(0);
      product.setId(1L);
      storedProduct.set(product);
      return 1;
    });
    when(products.updateByExampleSelective(any(IoTProduct.class), any())).thenReturn(1);
    when(devices.selectByExample(any())).thenReturn(List.of());
    var video = mock(VideoPlatformDeviceMapper.class);
    when(video.selectByExample(any())).thenReturn(List.of());
    var types = mock(SysDictTypeMapper.class);
    var type = new SysDictType();
    type.setStatus("0");
    when(types.selectOne(any())).thenReturn(type);
    seed = seededDictionary();
    when(dictionaries.selectDictDataByType(DICT_TYPE)).thenReturn(seed);
    catalog = new VideoSdkEventCatalog(dictionaries, types, json);
    var productService = new VideoPlatformProductServiceImpl(
        products, instances, json, new VideoSdkThingModelTemplate(json, catalog), invalidation);
    importer = new VideoDeviceImportServiceImpl(
        instances, productService, devices, products, json, video,
        mock(VideoPlatformChannelMapper.class), mock(SdkVideoAlarmContextCache.class));
  }

  @Test
  void importingNewDeviceInitializesProductFromCurrentWvpTemplateAndSeedDictionary() throws Exception {
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      assertEquals(Map.of("success", 1, "failed", 0, "exists", 0), importDevice());
    }
    JsonNode template;
    try (var input = new ClassPathResource("video/wvp-thing-model.json").getInputStream()) {
      template = json.readTree(input);
    }
    IoTProduct product = storedProduct.get();
    var model = json.readTree(product.getMetadata());
    var events = byId(model.path("events"));
    assertEquals(byId(template.path("events")).keySet(), events.keySet());
    assertEquals(byId(template.path("functions")).keySet(), byId(model.path("functions")).keySet());
    assertFalse(events.containsKey("cameraAlarm"));
    assertEquals("移动侦测", events.get("motionDetection").path("name").asText());
    assertEquals("区域入侵", events.get("regionIntrusion").path("name").asText());
    assertTrue(byId(events.get("regionIntrusion").path("valueType").path("properties"))
        .keySet().containsAll(Set.of("alarmMethod", "gbAlarmType", "alarmPriority", "rawData")));
    assertEquals("wvp", product.getThirdPlatform());
    var savedDevice = ArgumentCaptor.forClass(IoTDevice.class);
    verify(devices).insertSelective(savedDevice.capture());
    assertEquals(product.getProductKey(), savedDevice.getValue().getProductKey());
    assertEquals("trusted-tenant", savedDevice.getValue().getTenantId());
    verify(products, times(1)).insertSelective(any(IoTProduct.class));
    verify(products, never()).updateByExampleSelective(any(IoTProduct.class), any());
  }

  @Test
  void reimportUpgradesOldProductBeforeSkippingExistingDeviceAndPreservesCustomMetadata() throws Exception {
    var oldProduct = IoTProduct.builder().id(1L).productKey(INSTANCE_KEY).thirdPlatform("wvp")
        .metadata("""
            {"properties":[{"id":"customProperty"}],"functions":[{"id":"customFunction"}],
            "events":[{"id":"cameraAlarm","name":"历史告警"},
            {"id":"motionDetection","name":"自定义移动事件",
              "valueType":{"type":"object","properties":[{"id":"customField"}]}}]}
            """).build();
    storedProduct.set(oldProduct);
    existingDevice();
    assertEquals(Map.of("success", 0, "failed", 0, "exists", 1), importDevice());
    var ordered = inOrder(products, devices);
    ordered.verify(products).updateByExampleSelective(any(IoTProduct.class), any());
    ordered.verify(devices).selectByExample(any());
    var model = json.readTree(oldProduct.getMetadata());
    var events = byId(model.path("events"));
    assertTrue(events.keySet().containsAll(Set.of("cameraAlarm", "regionIntrusion", "motionDetection")));
    assertEquals("自定义移动事件", events.get("motionDetection").path("name").asText());
    assertTrue(byId(events.get("motionDetection").path("valueType").path("properties"))
        .containsKey("customField"));
    assertTrue(byId(model.path("properties")).containsKey("customProperty"));
    assertTrue(byId(model.path("functions")).containsKey("customFunction"));
    String upgraded = oldProduct.getMetadata();
    assertEquals(Map.of("success", 0, "failed", 0, "exists", 1), importDevice());
    assertEquals(upgraded, oldProduct.getMetadata());
    verify(products, times(1)).updateByExampleSelective(any(IoTProduct.class), any());
    verify(invalidation).publishProductChange(INSTANCE_KEY, "video-sdk-metadata-update");
    verify(products, never()).insertSelective(any(IoTProduct.class));
    verify(devices, never()).insertSelective(any(IoTDevice.class));
  }

  @Test
  void dictionaryChangeIsUsedOnNextImportAfterInvalidationWithoutRestartingTemplate() throws Exception {
    try (var security = mockStatic(SecurityUtils.class)) {
      security.when(SecurityUtils::getUnionIdQuietly).thenReturn(null);
      importDevice();
    }
    existingDevice();
    var updated = new ArrayList<>(seed);
    updated.add(new SysDictData().setDictCode(100L).setDictSort(100).setDictValue("5:99")
        .setDictLabel("扩展国标事件").setStatus("0")
        .setRemark("{\"type\":\"custom_gb_alarm\",\"eventId\":\"customGbAlarm\"}"));
    when(dictionaries.selectDictDataByType(DICT_TYPE)).thenReturn(updated);
    catalog.onCacheInvalidation(new IoTCacheInvalidationEvent(
        "dict-update", null, null, Set.of(), Map.of("selectDictDataByType", Set.of(DICT_TYPE)), Set.of()));
    assertEquals(Map.of("success", 0, "failed", 0, "exists", 1), importDevice());
    var events = byId(json.readTree(storedProduct.get().getMetadata()).path("events"));
    assertEquals("扩展国标事件", events.get("customGbAlarm").path("name").asText());
    assertTrue(byId(events.get("customGbAlarm").path("valueType").path("properties"))
        .containsKey("gbAlarmType"));
    assertEquals("customGbAlarm", catalog.resolve(
        "wvp", "vendor_raw", Map.of("alarmMethod", "5", "gbAlarmType", "99")).id());
    verify(dictionaries, times(2)).selectDictDataByType(DICT_TYPE);
    verify(products, times(1)).updateByExampleSelective(any(IoTProduct.class), any());
    verify(devices, times(1)).insertSelective(any(IoTDevice.class));
  }

  private Map<String, Integer> importDevice() {
    return importer.importDevices(INSTANCE_KEY,
        List.of(Map.of("deviceId", DEVICE_ID, "deviceName", "测试国标设备")), null);
  }

  private void existingDevice() {
    when(devices.selectByExample(any())).thenReturn(List.of(IoTDevice.builder()
        .iotId(INSTANCE_KEY + DEVICE_ID).productKey(INSTANCE_KEY).deviceId(DEVICE_ID).build()));
  }

  private List<SysDictData> seededDictionary() throws Exception {
    // 直接使用已发布迁移中的字典，避免测试自造一份与实际部署不同的事件目录。
    String sql = Files.readString(Path.of("..", "cn-universal-db-migrator", "src", "main", "resources",
        "db", "migration", "postgresql", "V2026101601__wvp_gb28181_event_dictionary.sql"));
    var matcher = Pattern.compile("\\((\\d+),'([^']+)','([^']+)','([^']+)'\\)").matcher(sql);
    var result = new ArrayList<SysDictData>();
    while (matcher.find()) {
      result.add(new SysDictData().setDictCode(Long.parseLong(matcher.group(1)))
          .setDictSort(Integer.parseInt(matcher.group(1))).setDictLabel(matcher.group(2))
          .setDictValue(matcher.group(3)).setRemark(matcher.group(4)).setStatus("0"));
    }
    assertEquals(25, result.size(), "应加载完整已发布国标事件字典");
    return result;
  }

  private Map<String, JsonNode> byId(JsonNode array) {
    var result = new LinkedHashMap<String, JsonNode>();
    array.forEach(item -> assertNull(result.put(item.path("id").asText(), item), "物模型标识不能重复"));
    return result;
  }
}
