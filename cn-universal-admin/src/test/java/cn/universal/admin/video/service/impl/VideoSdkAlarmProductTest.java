package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.VideoSdkThingModelTemplate;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.SysDictType;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.persistence.mapper.admin.SysDictTypeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VideoSdkAlarmProductTest {
  @org.junit.jupiter.api.BeforeAll
  static void initMapper() {
    tk.mybatis.mapper.mapperhelper.EntityHelper.initEntityNameMap(
        IoTProduct.class, new tk.mybatis.mapper.entity.Config());
  }

  private VideoSdkEventCatalog catalog() {
    var data = mock(SysDictDataMapper.class);
    var types = mock(SysDictTypeMapper.class);
    var type = new SysDictType();
    type.setStatus("0");
    when(types.selectOne(any())).thenReturn(type);
    when(data.selectDictDataByType(anyString()))
        .thenReturn(
            List.of(
                entry("motion", "motionDetection"),
                entry("video_lost", "videoLoss"),
                entry("intrusion", "regionIntrusion")));
    return new VideoSdkEventCatalog(data, types, new ObjectMapper());
  }

  private SysDictData entry(String type, String id) {
    return new SysDictData()
        .setDictValue(type)
        .setDictLabel(type)
        .setStatus("0")
        .setRemark("{\"eventId\":\"" + id + "\"}");
  }

  @Test
  void existingProductGetsSpecificEventsWithoutOverwritingCustomModel() throws Exception {
    var mapper = mock(IoTProductMapper.class);
    var json = new ObjectMapper();
    var service =
        new VideoPlatformProductServiceImpl(
            mapper,
            mock(VideoPlatformInstanceMapper.class),
            json,
            new VideoSdkThingModelTemplate(json, catalog()),
            mock(IoTCacheInvalidationPublisher.class));
    var instance = new VideoPlatformInstance();
    instance.setInstanceKey("test-dahua");
    instance.setPlatformType("dahua_sdk");
    var product = new IoTProduct();
    product.setId(1L);
    product.setProductKey("test-dahua");
    product.setMetadata(
        "{\"properties\":[{\"id\":\"custom\"}],\"functions\":[{\"id\":\"play\"}],\"events\":[{\"id\":\"cameraAlarm\"},{\"id\":\"motionDetection\",\"name\":\"自定义移动事件\"}]}");
    when(mapper.getProductByProductKey("test-dahua")).thenReturn(product);
    when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
    service.ensurePlatformProduct(instance, "VIDEO_DEVICE");
    var model = json.readTree(product.getMetadata());
    assertEquals("custom", model.path("properties").get(0).path("id").asText());
    assertEquals("play", model.path("functions").get(0).path("id").asText());
    assertEquals("自定义移动事件", model.path("events").get(1).path("name").asText());
    assertTrue(product.getMetadata().contains("videoLoss"));
    assertTrue(product.getMetadata().contains("dahuaSdkEvent"));
    assertFalse(product.getMetadata().contains("diskUnformatted"));
    service.ensurePlatformProduct(instance, "VIDEO_DEVICE");
    verify(mapper, times(1)).updateByExampleSelective(any(IoTProduct.class), any());
  }

  @Test
  void concurrentMetadataEditIsNotOverwrittenAndMissingProductIsNotCreated() {
    var mapper = mock(IoTProductMapper.class);
    var json = new ObjectMapper();
    var publisher = mock(IoTCacheInvalidationPublisher.class);
    var service =
        new VideoPlatformProductServiceImpl(
            mapper,
            mock(VideoPlatformInstanceMapper.class),
            json,
            new VideoSdkThingModelTemplate(json, catalog()),
            publisher);
    var instance = new VideoPlatformInstance();
    instance.setInstanceKey("test");
    instance.setPlatformType("dahua_sdk");
    assertThrows(RuntimeException.class, () -> service.prepareAlarmProduct(instance));
    verify(mapper, never()).insertSelective(any());
    var product = new IoTProduct();
    product.setId(1L);
    product.setProductKey("test");
    product.setThirdPlatform("dahua_sdk");
    product.setMetadata("{}");
    when(mapper.getProductByProductKey("test")).thenReturn(product);
    when(mapper.updateByExampleSelective(any(), any())).thenReturn(0);
    assertThrows(RuntimeException.class, () -> service.prepareAlarmProduct(instance));
    assertEquals("{}", product.getMetadata());
    verifyNoInteractions(publisher);
  }

  @Test
  void vendorCatalogMapsKnownTypesAndKeepsUnknownVendorIdentity() {
    assertEquals("motionDetection", catalog().resolve("hik_sdk", "motion").id());
    assertEquals("regionIntrusion", catalog().resolve("dahua_sdk", "intrusion").id());
    assertEquals("hikSdkEvent", catalog().resolve("hik_sdk", "hik_command_99").id());
    assertEquals("dahuaSdkEvent", catalog().resolve("dahua_sdk", "dahua_command_99").id());
  }

  @Test
  void tiandyAddsItsOwnEventsAndUsesVerifiedConnectionDefaults() throws Exception {
    var template = new VideoSdkThingModelTemplate(new ObjectMapper(), catalog());
    var metadata = new ObjectMapper().readTree(template.merge("{}", "tiandy_sdk", true));
    var eventIds = new java.util.HashSet<String>();
    metadata.path("events").forEach(event -> eventIds.add(event.path("id").asText()));
    assertTrue(eventIds.containsAll(List.of("motionDetection", "tiandySdkEvent", "sdkDeviceHealth")));
    assertFalse(eventIds.contains("hikSdkEvent"));
    assertFalse(eventIds.contains("dahuaSdkEvent"));
    var service = mock(VideoPlatformInstanceServiceImpl.class, CALLS_REAL_METHODS);
    var instance = new VideoPlatformInstance();
    instance.setPlatformType("tiandy_sdk");
    assertEquals(Integer.valueOf(3000), ReflectionTestUtils.invokeMethod(service, "defaultSdkPort", instance));
    assertEquals("tiandy", ReflectionTestUtils.invokeMethod(service, "sdkVendor", instance));
    Map<String, Object> candidate = ReflectionTestUtils.invokeMethod(service,
        "normalizeSdkCandidateForDisplay", instance, Map.of("deviceId", "camera", "host", "192.0.2.121"));
    assertEquals("Tiandy", candidate.get("manufacturer"));
    assertEquals(3000, candidate.get("sdkPort"));
    assertEquals(List.of(3000, 3001, 3002, 554, 80),
        ReflectionTestUtils.invokeMethod(service, "defaultSdkDiscoveryPorts", instance));
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    instance.setAuth("{\"sdkPortDefault\":3300,\"rtspPortDefault\":8554,\"streamMode\":\"sdk_only\"}");
    assertEquals(Integer.valueOf(3300), ReflectionTestUtils.invokeMethod(service, "defaultSdkPort", instance));
    instance.setOptions("{\"sdkPortDefault\":3400}");
    assertEquals(Integer.valueOf(3400), ReflectionTestUtils.invokeMethod(service, "defaultSdkPort", instance));
    Map<String, Object> configured = ReflectionTestUtils.invokeMethod(service,
        "normalizeSdkCandidateForSync", instance, Map.of("deviceId", "camera", "host", "192.0.2.121"), Map.of());
    assertEquals(8554, configured.get("rtspPort"));
    assertEquals("sdk_only", configured.get("streamMode"));
  }

  @Test
  void individualNestedCredentialWinsOverBatchAndInstanceDefaults() {
    var service = mock(VideoPlatformInstanceServiceImpl.class, CALLS_REAL_METHODS);
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    var instance = new VideoPlatformInstance();
    instance.setPlatformType("dahua_sdk");
    instance.setAuth(
        "{\"defaultUsername\":\"default-user\",\"defaultPassword\":\"default-password\"}");
    Map<String, Object> actual =
        ReflectionTestUtils.invokeMethod(
            service,
            "normalizeSdkCandidateForSync",
            instance,
            Map.of(
                "deviceId",
                "192.0.2.1",
                "host",
                "192.0.2.1",
                "auth",
                Map.of("username", "individual", "password", "individual-password")),
            Map.of("username", "batch", "password", "batch-password"));
    assertEquals("individual", actual.get("username"));
    assertEquals("individual-password", actual.get("password"));
  }
}
