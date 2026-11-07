package cn.universal.dm.video;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.cache.IoTCacheInvalidationEvent;
import cn.universal.core.video.CameraThingModel;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class VideoThingModelSynchronizerTest {
  @BeforeAll static void mapper() { EntityHelper.initEntityNameMap(IoTProduct.class, new Config()); }

  private IoTProduct product(String metadata, String tenant) {
    var p = new IoTProduct(); p.setId(1L); p.setProductKey("cloud"); p.setThirdPlatform("imoulife");
    p.setTenantId(tenant); p.setMetadata(metadata); return p;
  }

  @Test
  void observingUnknownEventsKeepsConcurrentAdministratorDefinitionsAndCachesSchema() {
    var mapper = mock(IoTProductMapper.class);
    var invalidation = mock(IoTCacheInvalidationPublisher.class);
    var synchronizer = new VideoThingModelSynchronizer(mapper, invalidation);
    var stale = product("{}", "t");
    var latest = product("{\"functions\":[],\"events\":[],\"properties\":[{\"id\":\"custom\",\"valueType\":{\"type\":\"string\"}}]}", "t");
    when(mapper.selectByPrimaryKey(1L)).thenReturn(latest);
    when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
    synchronizer.observe(stale, "vendor_336500", "厂商事件 336500", Map.of("outputData", Map.of("count", 2)), null);
    var patch = ArgumentCaptor.forClass(IoTProduct.class);
    verify(mapper).updateByExampleSelective(patch.capture(), any());
    assertEquals("custom", JSONUtil.parseObj(patch.getValue().getMetadata()).getJSONArray("properties").getJSONObject(0).getStr("id"));
    assertEquals("vendor_336500", JSONUtil.parseObj(stale.getMetadata()).getJSONArray("events").getJSONObject(0).getStr("id"));
    synchronizer.observe(stale, "vendor_336500", "厂商事件 336500", Map.of("outputData", Map.of("count", 3)), null);
    verify(mapper, times(1)).updateByExampleSelective(any(), any());
    verify(invalidation).publishProductChange("cloud", "video-event-metadata-update");
  }

  @Test
  void warmSchemaStillLearnsNewFieldsAndHonorsAdministratorMetadataChanges() {
    var mapper = mock(IoTProductMapper.class);
    var invalidation = mock(IoTCacheInvalidationPublisher.class);
    var synchronizer = new VideoThingModelSynchronizer(mapper, invalidation);
    String initial = CameraThingModel.compactVideoMetadata(CameraThingModel.observe("{}", "regionIntrusion", "区域入侵",
        Map.of("analysis", Map.of("count", 1)), null));
    var current = product(initial, "t");
    current.setThirdPlatform("wvp");
    for (int i = 0; i < 10; i++) synchronizer.observe(current, "regionIntrusion", "区域入侵",
        Map.of("analysis", Map.of("count", i)), null);
    verifyNoInteractions(mapper);
    assertEquals(initial, current.getMetadata());

    // 管理员修改名称后，即使报文字段形状相同，也不能复用旧的物模型结果。
    var edited = JSONUtil.parseObj(initial);
    edited.getJSONArray("events").getJSONObject(0).set("name", "管理员设置的入侵名称");
    current.setMetadata(edited.toString());
    synchronizer.invalidate(new IoTCacheInvalidationEvent(
        "product-update", "cloud", null, null, Set.of(), Map.of(), Set.of()));
    synchronizer.observe(current, "regionIntrusion", "区域入侵", Map.of("analysis", Map.of("count", 42)), null);
    assertTrue(current.getMetadata().contains("管理员设置的入侵名称"));
    verifyNoInteractions(mapper);

    when(mapper.selectByPrimaryKey(1L)).thenReturn(current);
    when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
    synchronizer.observe(current, "regionIntrusion", "区域入侵",
        Map.of("analysis", Map.of("count", 42, "confidence", 0.9)), null);
    assertTrue(current.getMetadata().contains("confidence"));
    assertTrue(current.getMetadata().contains("管理员设置的入侵名称"));
    assertFalse(current.getMetadata().contains("0.9"));
    synchronizer.observe(current, "regionIntrusion", "区域入侵",
        Map.of("analysis", Map.of("count", 43, "confidence", 0.8)), null);
    verify(mapper, times(1)).updateByExampleSelective(any(), any());
  }

  @Test
  void crossTenantProductNeverGetsPatched() {
    var mapper = mock(IoTProductMapper.class); var invalidation = mock(IoTCacheInvalidationPublisher.class);
    when(mapper.selectByPrimaryKey(1L)).thenReturn(product("{}", "another"));
    new VideoThingModelSynchronizer(mapper, invalidation).observe(product("{}", "t"), "human", "人形告警", Map.of("channelId", "0"), null);
    verify(mapper, never()).updateByExampleSelective(any(), any()); verifyNoInteractions(invalidation);
  }

  @Test
  void sdkAndWvpDefinitionsArePrunedAndObservationDoesNotRestoreAliases() {
    for (String platform : Set.of("wvp", "hik_sdk", "dahua_sdk", "tiandy_sdk", "hik_isup")) {
      var mapper = mock(IoTProductMapper.class);
      var invalidation = mock(IoTCacheInvalidationPublisher.class);
      var synchronizer = new VideoThingModelSynchronizer(mapper, invalidation);
      String legacy = CameraThingModel.observe("{}", "motionDetection", "管理员名称",
          Map.of("alarmType", "motion", "analysis", Map.of("count", 1)), null);
      var current = product(legacy, "t"); current.setThirdPlatform(platform);
      when(mapper.selectByPrimaryKey(1L)).thenReturn(current);
      when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
      synchronizer.ensureMetadata(current, legacy);
      assertFalse(current.getMetadata().contains("\"rawData\""), platform);
      assertFalse(current.getMetadata().contains("\"alarmType\""), platform);
      assertTrue(current.getMetadata().contains("管理员名称"), platform);
      assertTrue(current.getMetadata().contains("\"analysis\""), platform);
      synchronizer.observe(current, "motionDetection", "移动侦测",
          Map.of("analysis", Map.of("count", 2)), null);
      synchronizer.ensureMetadata(current, legacy);
      verify(mapper, times(1)).updateByExampleSelective(any(), any());
    }
  }

  @Test
  void observationCacheKeepsCloudSchemaSeparateFromCompactPlatforms() {
    var mapper = mock(IoTProductMapper.class);
    var synchronizer = new VideoThingModelSynchronizer(mapper, mock(IoTCacheInvalidationPublisher.class));
    var sdk = product("{}", "t"); sdk.setThirdPlatform("tiandy_sdk");
    var cloud = product("{}", "t"); cloud.setThirdPlatform("ezviz"); cloud.setProductKey("cloud-other"); cloud.setId(2L);
    when(mapper.selectByPrimaryKey(1L)).thenReturn(sdk);
    when(mapper.selectByPrimaryKey(2L)).thenReturn(cloud);
    when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
    synchronizer.observe(sdk, "motionDetection", "移动侦测", Map.of("channelId", "1"), null);
    synchronizer.observe(cloud, "motionDetection", "移动侦测", Map.of("channelId", "1"), null);
    assertFalse(sdk.getMetadata().contains("\"rawData\""));
    assertTrue(cloud.getMetadata().contains("\"rawData\""));
  }

  @Test
  void imoulifePrunesCallbackCopiesButKeepsDeclaredOutputFieldsAndUnknownEvents() {
    var mapper = mock(IoTProductMapper.class);
    var synchronizer = new VideoThingModelSynchronizer(mapper, mock(IoTCacheInvalidationPublisher.class));
    String legacy = CameraThingModel.observe("{}", "vendor_336700", "自定义人入侵",
        Map.of("outputData", Map.of("336701", 1), "content", Map.of("sessionId", "example"),
            "monitor", Map.of("vendor", java.util.List.of("COS"), "cname", "镜头")), null);
    var current = product(legacy, "t");
    when(mapper.selectByPrimaryKey(1L)).thenReturn(current);
    when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
    synchronizer.ensureMetadata(current, legacy);
    assertFalse(current.getMetadata().contains("\"rawData\""));
    assertFalse(current.getMetadata().contains("\"content\""));
    assertFalse(current.getMetadata().contains("\"vendor\""));
    assertTrue(current.getMetadata().contains("\"outputData\""));
    assertTrue(current.getMetadata().contains("\"336701\""));
    assertTrue(current.getMetadata().contains("自定义人入侵"));
    assertTrue(current.getMetadata().contains("\"images\""));
  }
}
