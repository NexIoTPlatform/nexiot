package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.core.video.CameraThingModel;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.admin.SysDictType;
import cn.universal.persistence.mapper.admin.SysDictDataMapper;
import cn.universal.persistence.mapper.admin.SysDictTypeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class WvpThingModelTemplateTest {
  private final ObjectMapper json = new ObjectMapper();
  // GB/T 28181-2022 A.2.5.3：与直连摄像机共享的稳定业务 event ID。
  private static final List<String> STANDARD_EVENTS = List.of(
      "1|phone_alarm|phoneAlarm", "2|device_alarm|deviceAlarm", "3|sms_alarm|smsAlarm",
      "4|gps_alarm|gpsAlarm", "7|other_alarm|otherAlarm", "2:1|video_lost|videoLoss",
      "2:2|device_tamper|deviceTamper", "2:3|disk_full|diskFull",
      "2:4|device_over_temperature|deviceOverTemperature", "2:5|device_under_temperature|deviceUnderTemperature",
      "5:1|manual_video_alarm|manualVideoAlarm", "5:2|motion|motionDetection",
      "5:3|object_left|objectLeft", "5:4|object_removed|objectRemoved",
      "5:5|line_crossing|lineCrossing", "5:6|intrusion|regionIntrusion",
      "5:7|reverse_direction|reverseDirection", "5:8|loitering|loitering",
      "5:9|traffic_statistics|trafficStatistics", "5:10|density_detection|densityDetection",
      "5:11|video_exception|videoException", "5:12|fast_movement|fastMovement",
      "5:13|shelter|videoTamper", "6:1|disk_error|diskError", "6:2|fan_error|fanError");

  @Test
  void standaloneModelAndPublishedCatalogHaveOneSelectableEventForEachStandardAlarm() throws Exception {
    JsonNode standalone;
    try (var input = new ClassPathResource("video/wvp-thing-model.json").getInputStream()) {
      standalone = json.readTree(input);
    }
    var definitions = byId(standalone.path("events"));
    var rows = new ArrayList<SysDictData>();
    for (String entry : STANDARD_EVENTS) {
      String[] parts = entry.split("\\|");
      assertTrue(definitions.containsKey(parts[2]), entry);
      rows.add(row(rows.size() + 1, parts[0], parts[1], parts[2], definitions.get(parts[2]).path("name").asText()));
    }
    var catalog = catalog(rows);
    var template = new VideoSdkThingModelTemplate(json, catalog);
    String merged = template.merge("{}", "wvp", true);
    var model = json.readTree(merged);
    var events = byId(model.path("events"));
    assertEquals(28, events.size());
    assertEquals(definitions.keySet(), events.keySet());
    assertFalse(events.containsKey("cameraAlarm"));
    assertFalse(events.containsKey("hikSdkEvent"));
    assertFalse(events.containsKey("sdkDeviceHealth"));
    assertEquals("移动侦测", events.get("motionDetection").path("name").asText());
    assertEquals("区域入侵", events.get("regionIntrusion").path("name").asText());
    assertEquals(standalone.path("functions"), model.path("functions"));
    assertEquals(standalone.path("properties"), model.path("properties"));
    assertEquals(10, model.path("functions").size());
    for (String entry : STANDARD_EVENTS) {
      String[] parts = entry.split("\\|");
      String[] code = parts[0].split(":");
      Map<String, Object> attributes = new LinkedHashMap<>();
      attributes.put("alarmMethod", code[0]);
      if (code.length == 2) attributes.put("gbAlarmType", code[1]);
      var event = catalog.resolve("wvp", "vendor_raw", attributes);
      assertEquals(parts[2], event.id());
      var definition = events.get(event.id());
      assertEquals("object", definition.path("valueType").path("type").asText());
      var fields = byId(definition.path("valueType").path("properties"));
      assertTrue(fields.keySet().containsAll(List.of("instanceKey", "platformType", "deviceId",
          "channelId", "eventId", "cameraEventType", "alarmTime", "alarmState", "images",
          "sdkEventFamily", "vendorEventCode", "alarmMethod", "gbAlarmType", "alarmLevel")), event.id());
      assertTrue(fields.keySet().stream().noneMatch(CameraThingModel.COMPACT_VIDEO_FIELDS::contains), event.id());
      assertFalse(fields.containsKey("data"), event.id());
      assertFalse(fields.containsKey("outputData"), event.id());
      assertEquals(List.of("url", "status", "imageSource", "capturedAt", "contentType", "size", "width",
          "height", "sha256", "ossId", "objectKey"), new ArrayList<>(byId(fields.get("images").path("valueType")
          .path("elementType").path("properties")).keySet()));
      assertEquals("long", fields.get("alarmTime").path("valueType").path("type").asText());
    }
    assertEquals(merged, template.merge(merged, "wvp", false));
  }

  @Test
  void existingProductsRetainCustomFieldsFunctionsAndHistoricalEvents() throws Exception {
    var template = new VideoSdkThingModelTemplate(json,
        catalog(List.of(row(1, "5:2", "motion", "motionDetection", "移动侦测"))));
    String metadata = """
        {"tags":["custom"],"properties":[{"id":"customProperty"},{"id":"alarmType"}],
        "functions":[{"id":"customFunction"},{"id":"rawData"}],
        "events":[{"id":"cameraAlarm","name":"旧国标告警"},
        {"id":"motionDetection","name":"自定义移动侦测","valueType":{"type":"object","properties":[
          {"id":"alarmMethod","name":"我的报警方式","valueType":{"type":"string"}},
          {"id":"customField","valueType":{"type":"string"}},
          {"id":"cameraEventName","valueType":{"type":"string"}},
          {"id":"rawData","valueType":{"type":"object","properties":[]}},
          {"id":"data","valueType":{"type":"object","properties":[
            {"id":"alarmType","valueType":{"type":"string"}},
            {"id":"count","valueType":{"type":"int"}}]}}]}}]}
        """;
    String merged = template.merge(metadata, "wvp", false);
    var model = json.readTree(merged);
    var events = byId(model.path("events"));
    assertEquals("旧国标告警", events.get("cameraAlarm").path("name").asText());
    var motion = events.get("motionDetection");
    assertEquals("自定义移动侦测", motion.path("name").asText());
    var fields = byId(motion.path("valueType").path("properties"));
    assertEquals("我的报警方式", fields.get("alarmMethod").path("name").asText());
    assertTrue(fields.containsKey("customField"));
    assertTrue(fields.keySet().stream().noneMatch(CameraThingModel.COMPACT_VIDEO_FIELDS::contains));
    assertEquals(List.of("alarmType", "count"), new ArrayList<>(byId(fields.get("data").path("valueType")
        .path("properties")).keySet()));
    assertTrue(byId(model.path("functions")).containsKey("customFunction"));
    assertTrue(byId(model.path("functions")).containsKey("rawData"));
    assertTrue(byId(model.path("properties")).containsKey("customProperty"));
    assertTrue(byId(model.path("properties")).containsKey("alarmType"));
    assertEquals("custom", model.path("tags").get(0).asText());
    assertEquals(merged, template.merge(merged, "wvp", false));
  }

  @Test
  void dictionaryChangeRecompilesCachedDeclarations() throws Exception {
    var catalog = mock(VideoSdkEventCatalog.class);
    var motion = new VideoSdkEventCatalog.Event("motion", "motionDetection", "移动侦测", "wvp", "5:2");
    var intrusion = new VideoSdkEventCatalog.Event("intrusion", "regionIntrusion", "入侵新名称", "wvp", "5:6");
    when(catalog.modelEvents("wvp")).thenReturn(List.of(motion));
    var template = new VideoSdkThingModelTemplate(json, catalog);
    assertTrue(byId(json.readTree(template.merge("{}", "wvp", true)).path("events")).containsKey("motionDetection"));
    when(catalog.modelEvents("wvp")).thenReturn(List.of(intrusion));
    var events = byId(json.readTree(template.merge("{}", "wvp", true)).path("events"));
    assertEquals("入侵新名称", events.get("regionIntrusion").path("name").asText());
    assertFalse(events.containsKey("motionDetection"));
  }

  @Test
  void dictionaryControlsNewModelsAndCustomMappingsCreateIndependentEvents() throws Exception {
    var template = new VideoSdkThingModelTemplate(json, catalog(List.of(
        row(1, "5:2", "motion", "motionDetection", "移动侦测").setStatus("1"),
        row(2, "5:6", "intrusion", "customIntrusion", "自定义入侵"))));
    var events = byId(json.readTree(template.merge("{}", "wvp", true)).path("events"));
    assertEquals(4, events.size());
    assertTrue(events.containsKey("customIntrusion"));
    assertTrue(byId(events.get("customIntrusion").path("valueType").path("properties")).containsKey("gbAlarmType"));
    assertFalse(events.containsKey("motionDetection"));
    assertFalse(events.containsKey("regionIntrusion"));
    assertTrue(events.containsKey("wvpGb28181Event"));
    var noDictionary = new VideoSdkThingModelTemplate(json, catalog(List.of()));
    assertEquals(3, json.readTree(noDictionary.merge("{}", "wvp", true)).path("events").size());
  }

  private VideoSdkEventCatalog catalog(List<SysDictData> rows) {
    var data = mock(SysDictDataMapper.class);
    var types = mock(SysDictTypeMapper.class);
    var type = new SysDictType();
    type.setStatus("0");
    when(types.selectOne(any())).thenReturn(type);
    when(data.selectDictDataByType("video_sdk_event_wvp")).thenReturn(rows);
    return new VideoSdkEventCatalog(data, types, json);
  }

  private SysDictData row(long id, String code, String type, String event, String name) throws Exception {
    return new SysDictData().setDictCode(id).setDictSort((int) id).setDictValue(code).setDictLabel(name).setStatus("0")
        .setRemark(json.writeValueAsString(Map.of("type", type, "eventId", event)));
  }

  private Map<String, JsonNode> byId(JsonNode array) {
    var result = new LinkedHashMap<String, JsonNode>();
    array.forEach(item -> assertNull(result.put(item.path("id").asText(), item), "重复物模型标识"));
    return result;
  }
}
