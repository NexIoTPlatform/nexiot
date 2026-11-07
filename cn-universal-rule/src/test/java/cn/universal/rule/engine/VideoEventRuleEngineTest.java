package cn.universal.rule.engine;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONUtil;
import cn.universal.rule.express.ExpressTemplate;
import cn.universal.rule.function.RuleFunctionTemplate;
import cn.universal.rule.function.inner.ToUpperCaseFunction;
import cn.universal.rule.parser.DefaultRuleParser;
import cn.universal.rule.parser.SqlModelParserStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VideoEventRuleEngineTest {
  private static final RuleFunctionTemplate FUNCTIONS = new RuleFunctionTemplate(List.of(new ToUpperCaseFunction()));
  private static final ExpressTemplate EXPRESSIONS = new ExpressTemplate();
  static {
    ReflectionTestUtils.setField(EXPRESSIONS, "ruleFunctionTemplate", FUNCTIONS);
    EXPRESSIONS.initFunction();
  }

  private static DefaultRuleEngine engine() {
    var engine = new DefaultRuleEngine();
    ReflectionTestUtils.setField(engine, "ruleParser",
        new DefaultRuleParser(List.of(new SqlModelParserStrategy())));
    ReflectionTestUtils.setField(engine, "expressTemplate", EXPRESSIONS);
    ReflectionTestUtils.setField(engine, "ruleFunctionTemplate", FUNCTIONS);
    return engine;
  }

  @Test
  void matchesSharedWvpSdkMotionIdAndSelectsEventDataAndEvidence() {
    var engine = engine();
    String sql = """
        SELECT event, data.channelId AS channel, data.alarmTime AS alarmTime, data.alarmType AS alarmType,
               data.images."0".url AS snapshot
        FROM app WHERE event = 'motionDetection' AND data.channelId = '1' AND data.alarmType = 'motion'
        """;
    for (String platform : List.of("wvp", "tiandy_sdk")) {
      var event = JSONUtil.parseObj("""
          {"messageType":"EVENT","event":"motionDetection","eventName":"移动侦测",
           "data":{"channelId":"1","alarmTime":1792131225132,"cameraEventType":"motion",
                   "images":[{"url":"https://camera.example/snapshot.jpg"}]}}
          """);
      event.getJSONObject("data").set("platformType", platform);
      var result = engine.executeRule(event, sql, "app");
      assertNotNull(result, platform);
      assertEquals("motionDetection", result.getStr("event"));
      assertEquals("1", result.getStr("channel"));
      assertEquals("motion", result.getStr("alarmType"));
      assertEquals(1792131225132L, result.getLong("alarmTime"));
      assertEquals("https://camera.example/snapshot.jpg", result.getStr("snapshot"));
      assertSame(event, engine.executeRule(event,
          "SELECT * FROM app WHERE data.alarmType = 'motion'", "app"));
      assertFalse(event.getJSONObject("data").containsKey("alarmType"));
      event.set("event", "regionIntrusion");
      assertNull(engine.executeRule(event, sql, "app"));
      event.set("event", "motionDetection");
      event.getJSONObject("data").set("channelId", "2");
      assertNull(engine.executeRule(event, sql, "app"));
    }
  }

  @Test
  void whereAliasesRespectRealValuesAndOnlyApplyToEventData() {
    var engine = engine();
    var legacy = JSONUtil.parseObj("""
        {"event":"motionDetection","data":{"alarmType":"motion",
         "rawData":{"cameraEventType":"vendor-motion"}}}
        """);
    assertEquals("motion", engine.executeRule(legacy,
        "SELECT data.cameraEventType AS type FROM app WHERE data.cameraEventType = 'motion'", "app").getStr("type"));
    assertFalse(legacy.getJSONObject("data").containsKey("cameraEventType"));
    assertNull(engine.executeRule(legacy,
        "SELECT * FROM app WHERE data.rawData.alarmType = 'vendor-motion'", "app"));
    legacy.getJSONObject("data").set("cameraEventType", "");
    assertNull(engine.executeRule(legacy,
        "SELECT * FROM app WHERE data.cameraEventType = 'motion'", "app"));
    legacy.getJSONObject("data").set("cameraEventType", cn.hutool.json.JSONNull.NULL);
    assertNull(engine.executeRule(legacy,
        "SELECT * FROM app WHERE data.cameraEventType = 'motion'", "app"));
    legacy.getJSONObject("data").remove("cameraEventType");
    legacy.set("event", cn.hutool.json.JSONNull.NULL);
    assertNull(engine.executeRule(legacy,
        "SELECT * FROM app WHERE data.cameraEventType = 'motion'", "app"));
  }

  @Test
  void whereAndSelectUseTheSameNestedArrayAndLiteralDotPaths() {
    var engine = engine();
    var event = JSONUtil.parseObj("""
        {"event":"motionDetection","data":{"channelId":"camera","images":[
          {"url":"https://camera.example/a.jpg","width":1280,"image.url":"literal-image"}],
          "outputData":{"score.value":92,"score":{"value":10}}}}
        """);
    for (String index : List.of(".\"0\"", "[0]")) {
      var sql = "SELECT data.images" + index + ".url AS snapshot, data.outputData.\"score.value\" AS score FROM app WHERE "
          + "data.images" + index + ".width = 1280 AND data.outputData.\"score.value\" > 90 "
          + "AND data.outputData.score.value = 92 AND data.images" + index + ".\"image.url\" = 'literal-image'";
      var result = engine.executeRule(event, sql, "app");
      assertNotNull(result, index);
      assertEquals("https://camera.example/a.jpg", result.getStr("snapshot"));
      assertEquals(92, result.getInt("score"));
      assertNull(engine.executeRule(event, sql.replace("= 1280", "= 640"), "app"));
    }
    event.getJSONObject("data").set("images[0].url", "literal-bracket-image");
    assertEquals("literal-bracket-image", engine.executeRule(event,
        "SELECT data.images[0].url AS snapshot FROM app WHERE data.images[0].url = 'literal-bracket-image'",
        "app").getStr("snapshot"));
    assertNotNull(engine.executeRule(event,
        "SELECT * FROM app WHERE data.images[0].width + 10 >= 1290", "app"));
    assertNotNull(engine.executeRule(event,
        "SELECT * FROM app WHERE data.images[99].url IS NULL", "app"));
  }

  @Test
  void wherePreservesOrdinaryPropertiesFunctionsMembershipAndNullConditions() {
    var engine = engine();
    var event = JSONUtil.parseObj("""
        {"event":"motionDetection","data":{"channelId":"camera","score":92,
         "optional":null,"images":[{"url":"https://camera.example/a.jpg"}]}}
        """);
    var sql = """
        SELECT toUpperCase(data.channelId) AS channel FROM app
        WHERE event = 'motionDetection' AND toUpperCase(data.channelId) = 'CAMERA'
          AND data.score IN (90,92) AND data.channelId NOT IN ('other','none')
          AND data.optional IS NULL AND data.missing IS NULL AND data.images[0].url IS NOT NULL
        """;
    assertEquals("CAMERA", engine.executeRule(event, sql, "app").getStr("channel"));
    assertNull(engine.executeRule(event, sql.replace("IN (90,92)", "IN (1,2)"), "app"));
    assertNull(engine.executeRule(event, sql.replace("IS NOT NULL", "IS NULL"), "app"));
    assertNotNull(engine.executeRule(event,
        "SELECT * FROM app WHERE toUpperCase(data.images[0].url) = 'HTTPS://CAMERA.EXAMPLE/A.JPG'", "app"));
  }

  @Test
  void imoulifeOutputValuesAreSelectableAtOfficialAndLegacyPaths() {
    var event = JSONUtil.parseObj("""
        {"event":"vendor_336700","data":{"platformType":"IMOULIFE","outputData":{"confidence":92}}}
        """);
    var result = engine().executeRule(event,
        "SELECT data.confidence AS legacy, data.outputData.confidence AS official FROM app "
            + "WHERE event = 'vendor_336700' AND data.confidence > 90 AND data.outputData.confidence = 92", "app");
    assertNotNull(result);
    assertEquals(92, result.getInt("legacy"));
    assertEquals(92, result.getInt("official"));
    assertFalse(event.getJSONObject("data").containsKey("confidence"));
  }
}
