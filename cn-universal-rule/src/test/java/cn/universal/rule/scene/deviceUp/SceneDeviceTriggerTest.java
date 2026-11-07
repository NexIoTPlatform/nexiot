package cn.universal.rule.scene.deviceUp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.message.UPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.bo.TriggerBO;
import cn.universal.persistence.mapper.IoTDeviceRuleLogMapper;
import cn.universal.rule.enums.RunStatus;
import cn.universal.rule.scene.deviceDown.SenceIoTDeviceDownService;
import cn.universal.rule.scene.service.SceneConditionEvaluator;
import cn.universal.rule.scene.service.SceneRuleCache;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class SceneDeviceTriggerTest {
  static final String TRIGGER =
      """
      {"trigger":"device","type":"properties","deviceId":"d","productKey":"p",
       "filters":[{"key":"temperature","value":"10","operator":"gt"},
                  {"key":"humidity","value":"20","operator":"lte"}]}
      """;

  static SceneLinkage scene(String touch, String conditions) {
    return SceneLinkage.builder()
        .id(1L)
        .status(0)
        .sleepCycle(30)
        .touch(touch)
        .triggerCondition(conditions)
        .build();
  }

  static UPRequest up(Map<String, Object> values) {
    var up = new UPRequest();
    up.setProductKey("p");
    up.setDeviceId("d");
    up.setMessageType(MessageType.PROPERTIES);
    up.setProperties(values);
    return up;
  }

  @Test
  void allRequiresEveryPropertyInTheSameReport() {
    var engine = new DevicePropertiesUp();
    var all = scene("all", "[" + TRIGGER + "]");
    assertFalse(engine.testDeviceTrigger(all, up(Map.of("temperature", 11))));
    assertTrue(engine.testDeviceTrigger(all, up(Map.of("temperature", 11, "humidity", 20))));
    assertFalse(engine.testDeviceTrigger(all, up(Map.of("temperature", 11, "humidity", 21))));
    all.setTouch("one");
    assertTrue(engine.testDeviceTrigger(all, up(Map.of("temperature", 11))));
  }

  @Test
  void neverEvaluatesAnotherDeviceOrProductAgainstCurrentValues() {
    var engine = new DevicePropertiesUp();
    var other = TRIGGER.replace("\"d\"", "\"other\"");
    assertFalse(
        engine.testDeviceTrigger(scene("one", "[" + other + "]"), up(Map.of("temperature", 100))));
    var split = scene("one", "[" + other + "," + TRIGGER.replace("\"p\"", "\"another\"") + "]");
    assertFalse(engine.testDeviceTrigger(split, up(Map.of("temperature", 100))));
    assertFalse(
        engine.testDeviceTrigger(
            scene("all", "[" + TRIGGER + "," + other + "]"),
            up(Map.of("temperature", 100, "humidity", 10))));
  }

  @Test
  void valuesAreComparedWithoutExpressionInterpolation() {
    assertTrue(SceneConditionEvaluator.compare(1.0, "1", "eq"));
    assertTrue(SceneConditionEvaluator.compare("a'b", "a'b", "eq"));
    assertTrue(SceneConditionEvaluator.compare("pump-alarm", "alarm", "like"));
    assertFalse(SceneConditionEvaluator.compare(null, "1", "not"));
    assertFalse(SceneConditionEvaluator.compare("oops", "1", "gt"));
    assertTrue(SceneConditionEvaluator.compare("9007199254740993", "9007199254740992", "gt"));
  }

  @Test
  void membershipUsesTrimmedCandidateValuesAndNeverMatchesMissingData() {
    assertTrue(SceneConditionEvaluator.compare("ACTIVE", " RECOVERED , ACTIVE ", "in"));
    assertFalse(SceneConditionEvaluator.compare("ACTIVE", "RECOVERED,UNKNOWN", "in"));
    assertTrue(SceneConditionEvaluator.compare("ACTIVE", "RECOVERED,UNKNOWN", "nin"));
    assertFalse(SceneConditionEvaluator.compare("ACTIVE", "RECOVERED,ACTIVE", "nin"));
    assertTrue(SceneConditionEvaluator.compare(2, "1, 2,3", "in"));
    assertFalse(SceneConditionEvaluator.compare(null, "1,2", "nin"));
    assertFalse(SceneConditionEvaluator.compare("ACT", "ACTIVE,RECOVERED", "in"));
  }

  @Test
  void propertyConditionsSupportNestedMapsAndArraysWithoutChangingLiteralKeyPriority() {
    var configured = scene("all", """
        [{"trigger":"device","type":"properties","deviceId":"d","productKey":"p",
          "filters":[{"key":"properties.nestedOutputData.status","value":"active,warning","operator":"in"},
                     {"key":"sensors.0.temperature","value":"20","operator":"gt"},
                     {"key":"outputData.status","value":"literal","operator":"eq"}]}]
        """);
    var request = up(Map.of("nestedOutputData", Map.of("status", "active"),
        "outputData.status", "literal", "sensors", List.of(Map.of("temperature", 25))));
    assertTrue(new DevicePropertiesUp().testDeviceTrigger(configured, request));
    request.setProperties(Map.of("nestedOutputData", Map.of("status", "active"),
        "outputData.status", "literal", "sensors", List.of(Map.of("temperature", 15))));
    assertFalse(new DevicePropertiesUp().testDeviceTrigger(configured, request));
    assertEquals("literal", SceneConditionEvaluator.valueAt(Map.of(
        "data.channelId", "literal", "channelId", "nested"), "data.channelId"));
  }

  @Test
  void eventRequiresMatchingDeviceAndEventIdentifier() {
    var engine = new DeviceEventUp();
    var scene =
        scene(
            "one",
            "[{\"trigger\":\"device\",\"type\":\"event\",\"deviceId\":\"d\",\"productKey\":\"p\",\"modelId\":\"alarm\"}]");
    var up = up(Map.of());
    up.setMessageType(MessageType.EVENT);
    up.setEvent("alarm");
    assertTrue(engine.testDeviceTrigger(scene, up));
    up.setDeviceId("other");
    assertFalse(engine.testDeviceTrigger(scene, up));
  }

  @Test
  void officialNumericFieldsAndPrefixedLiteralDotKeysKeepExactMatchPriority() {
    var properties = scene("all", """
        [{"trigger":"device","type":"properties","deviceId":"d","productKey":"p",
          "filters":[{"key":"properties.111100","value":"1","operator":"eq"},
                     {"key":"properties.14600.14603","value":"literal","operator":"eq"}]}]
        """);
    var values = Map.<String, Object>of("111100", 1, "14600.14603", "literal",
        "14600", Map.of("14603", "nested"));
    assertTrue(new DevicePropertiesUp().testDeviceTrigger(properties, up(values)));
    assertFalse(new DevicePropertiesUp().testDeviceTrigger(properties, up(Map.of("111100", 1,
        "14600", Map.of("14603", "nested")))));
    var request = up(Map.of()); request.setMessageType(MessageType.EVENT); request.setEvent("vendor_336500");
    request.setData(Map.of("outputData", Map.of("336501", 1, "336502.score", 92,
        "336502", Map.of("score", 10))));
    var event = scene("one", """
        [{"trigger":"device","type":"event","deviceId":"d","productKey":"p","modelId":"vendor_336500",
          "filters":[{"key":"data.outputData.336501","value":"1","operator":"eq"},
                     {"key":"data.outputData.336502.score","value":"90","operator":"gt"}]}]
        """);
    assertTrue(new DeviceEventUp().testDeviceTrigger(event, request));
    request.setData(Map.of("outputData", Map.of("336501", 1, "336502", Map.of("score", 10))));
    assertFalse(new DeviceEventUp().testDeviceTrigger(event, request));
  }

  @Test
  void eventRequiresEveryConfiguredDataFieldAndSupportsVendorArrays() {
    var engine = new DeviceEventUp();
    var configured = scene("one", """
        [{"trigger":"device","type":"event","deviceId":"d","productKey":"p","modelId":"vendor_336500",
          "filters":[{"key":"data.channelId","value":"0","operator":"eq"},
                     {"key":"outputData.score","value":"0.8","operator":"gte"},
                     {"key":"data.evidence.0.kind","value":"IMAGE","operator":"eq"}]}]
        """);
    var request = up(Map.of()); request.setMessageType(MessageType.EVENT); request.setEvent("vendor_336500");
    request.setData(Map.of("channelId", "0", "outputData", Map.of("score", 0.95),
        "evidence", List.of(Map.of("kind", "IMAGE"))));
    assertTrue(engine.testDeviceTrigger(configured, request));
    request.setData(Map.of("channelId", "0", "outputData", Map.of("score", 0.2),
        "evidence", List.of(Map.of("kind", "IMAGE"))));
    assertFalse(engine.testDeviceTrigger(configured, request));
    request.setData(Map.of("channelId", "0", "outputData", Map.of("score", 0.95)));
    assertFalse(engine.testDeviceTrigger(configured, request));
    request.setData(Map.of("channelId", "1", "outputData", Map.of("score", 0.95),
        "evidence", List.of(Map.of("kind", "IMAGE"))));
    assertFalse(engine.testDeviceTrigger(configured, request));
  }

  @Test
  void wvpAndTiandyMotionEventsMatchTheSameModelIdAndTheirDataValues() {
    var configured = scene("one", """
        [{"trigger":"device","type":"event","deviceId":"d","productKey":"p","modelId":"motionDetection",
          "filters":[{"key":"data.channelId","value":"1","operator":"eq"},
                     {"key":"data.alarmType","value":"motion","operator":"eq"},
                     {"key":"data.alarmTime","value":"1792131225000","operator":"gte"},
                     {"key":"data.images.0.width","value":"1280","operator":"eq"}]}]
        """);
    for (String platform : List.of("wvp", "tiandy_sdk")) {
      var request = up(Map.of());
      request.setMessageType(MessageType.EVENT);
      request.setEvent("motionDetection");
      request.setData(Map.of("platformType", platform, "channelId", "1",
          "cameraEventType", "motion", "alarmTime", 1792131225132L, "images", List.of(Map.of("width", 1280))));
      assertTrue(new DeviceEventUp().testDeviceTrigger(configured, request), platform);
      request.setEvent("regionIntrusion");
      assertFalse(new DeviceEventUp().testDeviceTrigger(configured, request));
      request.setEvent("motionDetection");
      request.setData(Map.of("platformType", platform, "channelId", "2",
          "cameraEventType", "motion", "alarmTime", 1792131225132L, "images", List.of(Map.of("width", 1280))));
      assertFalse(new DeviceEventUp().testDeviceTrigger(configured, request));
    }
  }

  @Test
  void twoNodesShareAtomicCooldownAndNonMatchesNeverTouchRedis() throws Exception {
    var scene = scene("one", "[" + TRIGGER + "]");
    var rules = mock(SceneRuleCache.class);
    when(rules.find("p", "d", "t", "u"))
        .thenReturn(
            List.of(
                new SceneRuleCache.Rule(
                    scene, JSONUtil.toList(scene.getTriggerCondition(), TriggerBO.class))));
    var redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    AtomicBoolean claimed = new AtomicBoolean();
    when(values.setIfAbsent(anyString(), eq("1"), eq(30L), eq(TimeUnit.SECONDS)))
        .thenAnswer(i -> claimed.compareAndSet(false, true));
    var down = mock(SenceIoTDeviceDownService.class);
    when(down.doIoTDeviceFunction(any(), any())).thenReturn(List.of());
    when(down.matchSuccess(any())).thenReturn(RunStatus.success);
    var logs = mock(IoTDeviceRuleLogMapper.class);
    var nodes = List.of(new DevicePropertiesUp(), new DevicePropertiesUp());
    nodes.forEach(
        engine -> {
          ReflectionTestUtils.setField(engine, "sceneRuleCache", rules);
          ReflectionTestUtils.setField(engine, "stringRedisTemplate", redis);
          ReflectionTestUtils.setField(engine, "senceIoTDeviceDownService", down);
          ReflectionTestUtils.setField(engine, "ioTDeviceRuleLogMapper", logs);
        });
    var device = new IoTDeviceDTO();
    device.setDeviceId("d");
    device.setProductKey("p");
    device.setTenantId("t");
    device.setUserUnionId("u");
    nodes.getFirst().doTestTrigger(up(Map.of("temperature", 0)), device);
    verifyNoInteractions(redis, down, logs);
    try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          java.util.stream.IntStream.range(0, 100)
              .mapToObj(
                  i ->
                      threads.submit(
                          () ->
                              nodes
                                  .get(i % 2)
                                  .doTestTrigger(up(Map.of("temperature", 100)), device)))
              .toList();
      for (var future : futures) future.get(5, TimeUnit.SECONDS);
    }
    verify(down, times(1)).doIoTDeviceFunction(any(), any());
    verify(logs, times(1)).insertList(anyList());
    verify(values, never()).get(anyString());
  }
}
