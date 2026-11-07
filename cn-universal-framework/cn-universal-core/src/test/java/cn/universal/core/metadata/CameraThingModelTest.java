package cn.universal.core.metadata;

import cn.hutool.json.JSONUtil;
import cn.universal.core.video.CameraThingModel;
import cn.universal.core.metadata.types.ObjectType;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class CameraThingModelTest {
  @Test
  public void schemaSignatureIgnoresValuesButTracksNestedFieldsAndTypes() {
    String first = CameraThingModel.observationSignature(
        Map.of("rawData", Map.of("count", 1), "alarmTime", 100L, "token", "first-private"), null);
    String next = CameraThingModel.observationSignature(
        Map.of("rawData", Map.of("count", 99), "alarmTime", 900L, "token", "second-private"), null);
    Assert.assertEquals(first, next);
    Assert.assertFalse(first.contains("private"));
    Assert.assertFalse(first.contains("100"));
    Assert.assertNotEquals(first, CameraThingModel.observationSignature(
        Map.of("rawData", Map.of("count", 1, "score", 0.9), "alarmTime", 100L), null));
    Assert.assertNotEquals(first, CameraThingModel.observationSignature(
        Map.of("rawData", Map.of("count", "one"), "alarmTime", 100L), null));
    Assert.assertNotEquals(first, CameraThingModel.observationSignature(
        Map.of("rawData", Map.of("count", 1), "alarmTime", 100L), Map.of("Battery", 10)));
  }

  @Test
  public void legacyEventsBecomeStructuredAndUnknownEventsExposeRealVendorData() {
    String existing = "{\"events\":[{\"id\":\"human\",\"name\":\"自定义人形名称\",\"valueType\":{\"type\":\"string\"}}],\"properties\":[]}";
    String enriched = CameraThingModel.enrichEvents(existing);
    var root = JSONUtil.parseObj(enriched);
    Assert.assertEquals("自定义人形名称", root.getJSONArray("events").getJSONObject(0).getStr("name"));
    Assert.assertEquals("object", root.getJSONArray("events").getJSONObject(0).getJSONObject("valueType").getStr("type"));
    String observed = CameraThingModel.observe(enriched, "vendor_336500", "厂商事件 336500",
        Map.of("outputData", Map.of("confidence", 0.91, "targetCount", 2),
            "evidence", List.of(Map.of("kind", "IMAGE")), "token", "private"),
        Map.of("Battery", 88));
    DeviceMetadata model = new DeviceMetadata(JSONUtil.parseObj(observed));
    Assert.assertNotNull(model.getEventOrNull("vendor_336500"));
    var type = (ObjectType) model.getEventOrNull("vendor_336500").getValueType();
    Assert.assertFalse(type.getProperties().stream().anyMatch(p -> "token".equals(p.getId())));
    var output = (ObjectType) type.getProperties().stream().filter(p -> "outputData".equals(p.getId()))
        .findFirst().orElseThrow().getValueType();
    Assert.assertTrue(output.getProperties().stream().anyMatch(p -> "confidence".equals(p.getId())));
    Assert.assertNotNull(model.getPropertyOrNull("Battery"));
    Assert.assertEquals(observed, CameraThingModel.observe(observed, "vendor_336500", "厂商事件 336500",
        Map.of("outputData", Map.of("confidence", 0.2, "targetCount", 3)), Map.of("Battery", 10)));
    Assert.assertFalse(observed.contains("private"));
  }

  @Test
  public void mergingRetainsUserFieldsAndDoesNotRewriteUnchangedMetadata() {
    String current = "{\"properties\":[{\"id\":\"custom\",\"name\":\"自定义\",\"valueType\":{\"type\":\"string\"}}],\"functions\":[],\"events\":[]}";
    String defaults = "{\"properties\":[],\"functions\":[],\"events\":[" + CameraThingModel.event("human", "人形告警") + "]}";
    String merged = CameraThingModel.mergeMetadata(current, defaults);
    Assert.assertNotNull(new DeviceMetadata(JSONUtil.parseObj(merged)).getPropertyOrNull("custom"));
    Assert.assertEquals(merged, CameraThingModel.mergeMetadata(merged, defaults));
  }

  @Test
  public void compactDeclarationsMatchStoredFieldsAndKeepBusinessSchemas() {
    String metadata = CameraThingModel.observe("{}", "motionDetection", "自定义移动侦测",
        Map.of("alarmType", "motion", "images", List.of(Map.of("url", "image.jpg",
            "snapshotUrl", "image.jpg", "deviceId", "camera", "sha256", "digest", "customCaption", "entrance")),
            "analysis", Map.of("targetCount", 2), "data", Map.of("customScore", 0.9)), null);
    String compact = CameraThingModel.compactVideoMetadata(metadata);
    var event = JSONUtil.parseObj(compact).getJSONArray("events").getJSONObject(0);
    var fields = event.getJSONObject("valueType").getJSONArray("properties");
    Assert.assertEquals("自定义移动侦测", event.getStr("name"));
    for (var field : fields.jsonIter()) {
      Assert.assertFalse(CameraThingModel.COMPACT_VIDEO_FIELDS.contains(field.getStr("id")));
      if ("images".equals(field.getStr("id"))) {
        var images = field.getJSONObject("valueType").getJSONObject("elementType").getJSONArray("properties");
        Assert.assertEquals(3, images.size());
      }
    }
    Assert.assertTrue(compact.contains("targetCount"));
    Assert.assertTrue(compact.contains("customScore"));
    Assert.assertTrue(compact.contains("customCaption"));
    Assert.assertFalse(fields.toString().contains("\"id\":\"outputData\""));
    Assert.assertEquals(compact, CameraThingModel.compactVideoMetadata(compact));
  }

  @Test
  public void officialNumericAndLiteralDotFieldsAreLearnedWithoutChangingEventIds() {
    String observed = CameraThingModel.observe("{}", "vendor_336500", "厂家事件（336500）",
        Map.of("outputData", Map.of("336501", 1, "336502.score", 92, "password", "private")),
        Map.of("111100", 1, "14600", Map.of("14603", 1)));
    DeviceMetadata model = new DeviceMetadata(JSONUtil.parseObj(observed));
    Assert.assertNotNull(model.getPropertyOrNull("111100"));
    var state = (ObjectType) model.getPropertyOrNull("14600").getValueType();
    Assert.assertTrue(state.getProperties().stream().anyMatch(p -> "14603".equals(p.getId())));
    var event = (ObjectType) model.getEventOrNull("vendor_336500").getValueType();
    var output = (ObjectType) event.getProperties().stream().filter(p -> "outputData".equals(p.getId()))
        .findFirst().orElseThrow().getValueType();
    Assert.assertTrue(output.getProperties().stream().anyMatch(p -> "336501".equals(p.getId())));
    Assert.assertTrue(output.getProperties().stream().anyMatch(p -> "336502.score".equals(p.getId())));
    Assert.assertFalse(observed.contains("private"));
    Assert.assertFalse(output.getProperties().stream().anyMatch(p -> "password".equals(p.getId())));
    String numericEvent = CameraThingModel.observe("{}", "336500", "unknown", Map.of("score", 92), null);
    Assert.assertNull(new DeviceMetadata(JSONUtil.parseObj(numericEvent)).getEventOrNull("336500"));
  }
}
