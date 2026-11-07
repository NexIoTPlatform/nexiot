package cn.universal.rule.scene.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.entity.bo.TriggerBO.ConditionFilter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SceneConditionEvaluatorTest {
  @Test
  void eventTypeAliasesStayAtTheDataRootAndNeverOverrideRealValues() {
    var compact = new LinkedHashMap<String, Object>(Map.of("cameraEventType", "motion",
        "rawData", Map.of("cameraEventType", "vendor-motion")));
    assertEquals("motion", SceneConditionEvaluator.valueAt(compact, "alarmType"));
    assertEquals("motion", SceneConditionEvaluator.valueAt(compact, "data.alarmType"));
    assertNull(SceneConditionEvaluator.valueAt(compact, "rawData.alarmType"));
    assertFalse(compact.containsKey("alarmType"));
    assertEquals("motion", SceneConditionEvaluator.valueAt(Map.of("alarmType", "motion"), "cameraEventType"));
    compact.put("alarmType", "");
    assertEquals("", SceneConditionEvaluator.valueAt(compact, "data.alarmType"));
    compact.put("alarmType", null);
    assertNull(SceneConditionEvaluator.valueAt(compact, "alarmType"));
    compact.put("data.alarmType", "literal");
    assertEquals("literal", SceneConditionEvaluator.valueAt(compact, "data.alarmType"));
  }

  @Test
  void propertyConditionsDoNotUseVideoEventAliases() {
    var filter = new ConditionFilter();
    filter.setKey("alarmType");
    filter.setOperator("eq");
    filter.setValue("motion");
    var data = Map.<String, Object>of("cameraEventType", "motion");
    assertTrue(SceneConditionEvaluator.event(List.of(filter), data));
    assertFalse(SceneConditionEvaluator.properties(List.of(filter), data, true));
  }

  @Test
  void imoulifeOutputAliasesAreEventOnlyAndHonorExplicitRootValues() {
    var data = new LinkedHashMap<String, Object>(Map.of("platformType", "IMOULIFE",
        "outputData", Map.of("score.value", 92, "objects", List.of(Map.of("type", "human")))));
    assertEquals(92, SceneConditionEvaluator.valueAt(data, "data.score.value"));
    assertEquals("human", SceneConditionEvaluator.valueAt(data, "objects.0.type"));
    var filter = new ConditionFilter(); filter.setKey("score.value"); filter.setOperator("gt"); filter.setValue("90");
    assertTrue(SceneConditionEvaluator.event(List.of(filter), data));
    assertFalse(SceneConditionEvaluator.properties(List.of(filter), data, true));
    data.put("score.value", null);
    assertNull(SceneConditionEvaluator.valueAt(data, "score.value"));
    assertFalse(data.containsKey("objects"));
  }
}
