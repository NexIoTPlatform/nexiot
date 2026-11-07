package cn.universal.rule.utils;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

class JsonParserUtilTest {
  private static final String MOTION = """
      {"messageType":"EVENT","event":"motionDetection","eventName":"移动侦测",
       "data":{"channelId":"1","alarmTime":1792131225132,"nativeState":1,
               "images":[{"url":"https://camera.example/snapshot.jpg","width":1280}],
               "rawData":{"vendorEventCode":0}}}
      """;

  @Test
  void readsStandardVideoEnvelopeIncludingEvidenceArraysAndTypedFields() {
    var event = JSONUtil.parseObj(MOTION);
    assertEquals("motionDetection", JsonParserUtil.getValue(event, "event"));
    assertEquals("1", JsonParserUtil.getValue(event, "data.channelId"));
    assertEquals("https://camera.example/snapshot.jpg",
        JsonParserUtil.getValue(event, "data.images.0.url"));
    assertEquals("https://camera.example/snapshot.jpg",
        JsonParserUtil.getValue(event, "data.images[0].url"));
    assertEquals(1280, JsonParserUtil.getValue(event, "data.images.0.width", Integer.class));
    assertEquals(1792131225132L, JsonParserUtil.getValue(MOTION, "data.alarmTime", Long.class));
    assertEquals(0, JsonParserUtil.getValue(event, "data.rawData.vendorEventCode"));
    assertEquals("1", JsonParserUtil.getValue(event, "\"data\".\"channelId\""));
  }

  @Test
  void literalVendorDotKeysTakePriorityAtEveryObjectLevel() {
    var event = JSONUtil.parseObj("""
        {"data.channelId":"outer literal","data":{"channelId":"1",
         "outputData":{"score.value":92,"score":{"value":10}},
         "images":[{"image.url":"literal image","image":{"url":"nested image"}}]}}
        """);
    assertEquals("outer literal", JsonParserUtil.getValue(event, "data.channelId"));
    assertEquals(92, JsonParserUtil.getValue(event, "data.outputData.score.value"));
    assertEquals("literal image", JsonParserUtil.getValue(event, "data.images.0.image.url"));
  }

  @Test
  void missingMalformedAndOutOfRangePathsReturnNullWithoutExecutingFieldNames() {
    var event = JSONUtil.parseObj(MOTION);
    for (String path : new String[] {"data.images.1.url", "data.images.-1.url",
        "data.images[1].url", "data.images[-1].url", "data.images[99999999999999999999].url",
        "data.images[name].url", "data.images[0).url",
        "data.images.99999999999999999999.url", "data.images.name", "data..channelId",
        "data.channelId.class", "T(java.lang.System).currentTimeMillis()", "", null}) {
      assertNull(JsonParserUtil.getValue(event, path));
    }
  }

  @Test
  void videoTypeAliasesOnlyApplyToTheEventDataRootAndRealValuesWin() {
    var compact = JSONUtil.parseObj("""
        {"event":"motionDetection","data":{"cameraEventType":"motion",
         "rawData":{"cameraEventType":"vendor-motion"}}}
        """);
    assertEquals("motion", JsonParserUtil.getValue(compact, "data.alarmType"));
    assertEquals("motion", JsonParserUtil.getValue(compact, "data.cameraEventType"));
    assertNull(JsonParserUtil.getValue(compact, "data.rawData.alarmType"));
    assertFalse(compact.getJSONObject("data").containsKey("alarmType"));
    var legacy = JSONUtil.parseObj("""
        {"event":"motionDetection","data":{"alarmType":"motion"}}
        """);
    assertEquals("motion", JsonParserUtil.getValue(legacy, "data.cameraEventType"));
    var explicit = JSONUtil.parseObj("""
        {"event":"motionDetection","data":{"alarmType":"","cameraEventType":"motion"}}
        """);
    assertEquals("", JsonParserUtil.getValue(explicit, "data.alarmType"));
    explicit.getJSONObject("data").set("alarmType", cn.hutool.json.JSONNull.NULL);
    assertNull(JsonParserUtil.getValue(explicit, "data.alarmType"));
    assertNull(JsonParserUtil.getValue(JSONUtil.parseObj("""
        {"data":{"cameraEventType":"property-value"}}
        """), "data.alarmType"));
  }

  @Test
  void imoulifeOfficialOutputsSupportLegacyRootReadsWithoutCopyingValues() {
    var event = JSONUtil.parseObj("""
        {"event":"vendor_336700","data":{"platformType":"IMOULIFE","outputData":{
          "score.value":92,"objects":[{"type":"human"}]}}}
        """);
    assertEquals(92, JsonParserUtil.getValue(event, "data.score.value"));
    assertEquals(92, JsonParserUtil.getValue(event, "data.outputData.score.value"));
    assertEquals("human", JsonParserUtil.getValue(event, "data.objects[0].type"));
    assertFalse(event.getJSONObject("data").containsKey("objects"));
    event.getJSONObject("data").set("objects", cn.hutool.json.JSONNull.NULL);
    assertNull(JsonParserUtil.getValue(event, "data.objects[0].type"));
    event.getJSONObject("data").set("platformType", "wvp");
    assertNull(JsonParserUtil.getValue(event, "data.score.value"));
  }
}
