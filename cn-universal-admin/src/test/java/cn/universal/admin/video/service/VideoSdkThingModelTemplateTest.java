package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.core.video.CameraThingModel;
import cn.universal.dm.video.VideoSdkEventCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class VideoSdkThingModelTemplateTest {
  @Test
  void gbProductsGainSelectableEventsAndFieldsWithoutOverwritingUserMetadata() {
    var catalog = mock(VideoSdkEventCatalog.class);
    when(catalog.modelEvents("wvp")).thenReturn(List.of(
        new VideoSdkEventCatalog.Event("intrusion", "regionIntrusion", "区域入侵", "wvp", "5:6"),
        new VideoSdkEventCatalog.Event("vendor_raw", "wvpGb28181Event", "国标未解析事件", "GB28181", "")));
    var template = new VideoSdkThingModelTemplate(new ObjectMapper(), catalog);
    String original = """
        {"events":[{"id":"cameraAlarm","name":"自定义国标报警","valueType":{"type":"object","properties":[
          {"id":"alarmMethod","name":"自定义报警方式","valueType":{"type":"string"}},
          {"id":"custom","name":"自定义","valueType":{"type":"string"}}]}}],"functions":[]}
        """;
    for (boolean creating : List.of(true, false)) {
      String merged = template.merge(original, "wvp", creating);
      var events = JSONUtil.parseObj(merged).getJSONArray("events");
      assertEquals(creating ? 4 : 5, events.size());
      assertEquals(!creating, events.toList(cn.hutool.json.JSONObject.class).stream()
          .anyMatch(event -> "cameraAlarm".equals(event.getStr("id"))));
      if (!creating) assertEquals("自定义国标报警", events.getJSONObject(0).getStr("name"));
      for (var event : events.jsonIter()) {
        if (!List.of("regionIntrusion", "wvpGb28181Event").contains(event.getStr("id"))) continue;
        var fields = event.getJSONObject("valueType").getJSONArray("properties");
        var ids = fields.toList(cn.hutool.json.JSONObject.class).stream().map(p -> p.getStr("id")).toList();
        assertTrue(ids.containsAll(List.of("alarmMethod", "gbAlarmType", "cameraEventType", "images", "vendorEventCode")));
        assertTrue(ids.stream().noneMatch(CameraThingModel.COMPACT_VIDEO_FIELDS::contains));
        assertEquals(ids.size(), new java.util.HashSet<>(ids).size());
      }
      assertEquals(merged, template.merge(merged, "wvp", false));
    }
  }

  @Test
  void existingSdkEventsGainFieldsWhileUserNamesAndCustomFieldsRemain() {
    var catalog = mock(VideoSdkEventCatalog.class);
    when(catalog.modelEvents("hik_sdk")).thenReturn(List.of(
        new VideoSdkEventCatalog.Event("motion", "motionDetection", "移动侦测", "hik_sdk", "0x4000")));
    var template = new VideoSdkThingModelTemplate(new ObjectMapper(), catalog);
    String original = """
        {"events":[{"id":"motionDetection","name":"我的移动侦测","valueType":{"type":"object","properties":[
          {"id":"custom","name":"自定义","valueType":{"type":"string"}},
          {"id":"rawData","valueType":{"type":"object","properties":[]}},
          {"id":"alarmType","valueType":{"type":"string"}},
          {"id":"outputData","valueType":{"type":"object","properties":[
            {"id":"alarmType","valueType":{"type":"string"}},
            {"id":"confidence","valueType":{"type":"double"}}]}}]}}],"functions":[]}
        """;
    String merged = template.merge(original, "hik_sdk", false);
    var event = JSONUtil.parseObj(merged).getJSONArray("events").getJSONObject(0);
    assertEquals("我的移动侦测", event.getStr("name"));
    var fields = event.getJSONObject("valueType").getJSONArray("properties");
    assertTrue(fields.jsonIter().iterator().hasNext());
    var ids = fields.toList(cn.hutool.json.JSONObject.class).stream().map(p -> p.getStr("id")).toList();
    assertTrue(ids.containsAll(List.of("custom", "channelId", "vendorEventCode", "cameraEventType", "images", "outputData", "evidence")));
    assertTrue(ids.stream().noneMatch(CameraThingModel.COMPACT_VIDEO_FIELDS::contains));
    var output = fields.toList(cn.hutool.json.JSONObject.class).stream()
        .filter(field -> "outputData".equals(field.getStr("id"))).findFirst().orElseThrow();
    assertEquals(List.of("alarmType", "confidence"), output.getJSONObject("valueType").getJSONArray("properties")
        .toList(cn.hutool.json.JSONObject.class).stream().map(field -> field.getStr("id")).toList());
    assertEquals(merged, template.merge(merged, "hik_sdk", false));
  }

  @Test
  void newSdkModelsDeclareCompactImageFieldsForEveryDirectPlatform() {
    var catalog = mock(VideoSdkEventCatalog.class);
    for (String platform : List.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "hik_isup")) {
      when(catalog.modelEvents(platform)).thenReturn(List.of(
          new VideoSdkEventCatalog.Event("motion", "motionDetection", "移动侦测", platform, "0")));
      var template = new VideoSdkThingModelTemplate(new ObjectMapper(), catalog);
      String merged = template.merge("{}", platform, true);
      var fields = JSONUtil.parseObj(merged).getJSONArray("events").getJSONObject(0)
          .getJSONObject("valueType").getJSONArray("properties").toList(cn.hutool.json.JSONObject.class);
      assertTrue(fields.stream().map(field -> field.getStr("id"))
          .noneMatch(CameraThingModel.COMPACT_VIDEO_FIELDS::contains), platform);
      assertFalse(fields.stream().anyMatch(field -> List.of("data", "outputData").contains(field.getStr("id"))), platform);
      var images = fields.stream().filter(field -> "images".equals(field.getStr("id"))).findFirst().orElseThrow();
      assertEquals("array", images.getJSONObject("valueType").getStr("type"));
      assertEquals(List.of("url", "status", "imageSource", "capturedAt", "contentType", "size", "width",
          "height", "sha256", "ossId", "objectKey"), images.getJSONObject("valueType").getJSONObject("elementType")
          .getJSONArray("properties").toList(cn.hutool.json.JSONObject.class).stream()
          .map(field -> field.getStr("id")).toList());
      assertEquals(merged, template.merge(merged, platform, false), platform);
    }
  }
}
