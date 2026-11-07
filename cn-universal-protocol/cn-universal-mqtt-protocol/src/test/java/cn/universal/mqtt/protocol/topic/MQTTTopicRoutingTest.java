package cn.universal.mqtt.protocol.topic;

import static cn.universal.mqtt.protocol.config.MqttConstant.TopicCategory.*;
import static org.junit.jupiter.api.Assertions.*;

import cn.universal.mqtt.protocol.entity.MQTTProductConfig.MqttTopicConfig;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MQTTTopicRoutingTest {
  private final MQTTTopicManager manager = new MQTTTopicManager();

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "hongfa/FF8A050101005543/upload/|hongfa/+/upload/|true",
        "a/|a/+|true",
        "a|a/#|true",
        "a/b/c|a/#|true",
        "a|a/+|false",
        "a/b/|a/b|false",
        "a.b/x|a.b/+|true",
        "axb/x|a.b/+|false",
        "a/(b)/x|a/(b)/+|true",
        "$SYS/a|#|false",
        "$SYS/a|+/a|false",
        "$SYS/a|$SYS/#|true",
        "a/x|a/#/x|false",
        "a/x|a/x+|false",
        "a/x|$share/group/a/+|true",
        "a/x|$share//a/+|false"
      })
  void matchesMqttLevelsLiterally(String topic, String filter, boolean expected) {
    assertEquals(expected, manager.matchesTopic(topic, filter));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "$thing/up/property/prod1/device1|$thing/up/#|prod1|THING_MODEL",
        "$thing/up/property/prod1/device1|$thing/up/+/+/+|prod1|THING_MODEL",
        "$thing/up/prod1/device1|$thing/up/+/+|prod1|PASSTHROUGH",
        "$ota/report/prod1/device1|$ota/#|prod1|SYSTEM_LEVEL",
        "$vendor/up/event/prod1/device1|$vendor/#|prod1|THING_MODEL"
      })
  void automaticProductUsesStandardPosition(
      String topic, String filter, String product, String category) {
    var route = manager.resolveRoute(topic, List.of(mapping(filter, null, null)), key -> false);
    assertNotNull(route);
    assertEquals(product, route.productKey());
    assertEquals(category, route.topicCategory().name());
  }

  @Test
  void customAutoMatchRequiresOneBoundKeyAndNeverGuessesDeviceId() {
    var mappings = List.of(mapping("vendor/#", null, PASSTHROUGH));
    Set<String> bound = Set.of("prod1", "prod2");
    assertEquals(
        "prod1",
        manager
            .resolveRoute("vendor/device123/prod1/data", mappings, bound::contains)
            .productKey());
    assertEquals(
        "prod1",
        manager.resolveRoute("vendor/prod1/prod1/data", mappings, bound::contains).productKey());
    assertNull(
        manager.resolveRoute("vendor/prod1/prod2/data", mappings, bound::contains).productKey());
    assertNull(
        manager.resolveRoute("vendor/device123/upload/", mappings, bound::contains).productKey());
  }

  @Test
  void firstEnabledRuleSuppliesBothProductAndCategory() {
    var disabled = mapping("vendor/#", "disabled", SYSTEM_LEVEL);
    disabled.setEnabled(false);
    var first = mapping("vendor/#", "prod1", PASSTHROUGH);
    var second = mapping("vendor/data", "prod2", THING_MODEL);
    var route = manager.resolveRoute("vendor/data", List.of(disabled, first, second), key -> true);
    assertEquals("prod1", route.productKey());
    assertEquals(PASSTHROUGH, route.topicCategory());
    // 不能在第一条无法识别时跳到后面的规则，却仍沿用第一条的数据类型。
    first.setProductKey(null);
    route = manager.resolveRoute("vendor/data", List.of(first, second), key -> false);
    assertNull(route.productKey());
    assertEquals(PASSTHROUGH, route.topicCategory());
  }

  @Test
  void sameProductCanUseDifferentCategoriesAndNetworksAreIndependent() {
    var mappings =
        List.of(mapping("raw/+", "prod1", PASSTHROUGH), mapping("json/+", "prod1", THING_MODEL));
    assertEquals(PASSTHROUGH, manager.resolveRoute("raw/dev", mappings, null).topicCategory());
    assertEquals(THING_MODEL, manager.resolveRoute("json/dev", mappings, null).topicCategory());
    assertEquals(
        THING_MODEL,
        manager
            .resolveRoute("raw/dev", List.of(mapping("raw/+", "prod2", THING_MODEL)), null)
            .topicCategory());
  }

  @Test
  void unmatchedOrDisabledStandardTopicCannotBypassSubscriptionRules() {
    var mapping = mapping("$thing/up/#", null, null);
    mapping.setEnabled(false);
    assertNull(manager.resolveRoute("$thing/up/prod1/dev", List.of(mapping), key -> true));
    assertNull(
        manager.resolveRoute(
            "$thing/up/prod1/dev", List.of(mapping("vendor/#", null, null)), key -> true));
    assertEquals(
        UNKNOWN,
        manager.getTopicCategoryFromConfig(
            "vendor/dev", List.of(mapping("vendor/#", "prod1", null))));
  }

  private MqttTopicConfig mapping(
      String topic,
      String product,
      cn.universal.mqtt.protocol.config.MqttConstant.TopicCategory category) {
    return MqttTopicConfig.builder()
        .topic(topic)
        .productKey(product)
        .topicCategory(category)
        .enabled(true)
        .qos(1)
        .build();
  }
}
