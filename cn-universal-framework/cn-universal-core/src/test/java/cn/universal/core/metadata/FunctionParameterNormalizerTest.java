package cn.universal.core.metadata;

import static org.junit.Assert.*;

import cn.hutool.json.JSONUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class FunctionParameterNormalizerTest {

  private static final String METADATA = """
      {"functions":[{"id":"controlLight","inputs":[
        {"id":"slaveAddress","valueType":{"type":"int"}},
        {"id":"lightMode","valueType":{"type":"string"}},
        {"id":"enabled","valueType":{"type":"boolean"}},
        {"id":"level","valueType":{"type":"enum","elements":[{"value":2,"text":"高"}]}},
        {"id":"textEnum","valueType":{"type":"enum","elements":[{"value":"06","text":"闪烁"}]}},
        {"id":"settings","valueType":{"type":"object","properties":[
          {"id":"volume","valueType":{"type":"double"}}]}},
        {"id":"channels","valueType":{"type":"array","elementType":{"type":"int"}}}
      ]}]}
      """;

  @Test
  public void restoresHistoricalStringsWithoutChangingStringCodesOrUnknownInputs() {
    Map<String, Object> values = FunctionParameterNormalizer.normalize(METADATA, "controlLight",
        Map.of("slaveAddress", "1", "lightMode", "06", "enabled", "false", "level", "2",
            "textEnum", "06", "untyped", "007"));
    assertEquals(1, values.get("slaveAddress"));
    assertEquals("06", values.get("lightMode"));
    assertEquals(false, values.get("enabled"));
    assertEquals(2, values.get("level"));
    assertEquals("06", values.get("textEnum"));
    assertEquals("007", values.get("untyped"));
    assertTrue(JSONUtil.toJsonStr(values).contains("\"slaveAddress\":1"));
  }

  @Test
  public void normalizesNestedStructuresAndSupportsSchemaStrings() {
    Map<String, Object> values = FunctionParameterNormalizer.normalize(METADATA, "controlLight",
        Map.of("settings", "{\"volume\":\"1.25\",\"code\":\"06\"}", "channels", "[\"1\",2]"));
    Map<?, ?> settings = (Map<?, ?>) values.get("settings");
    assertEquals(new BigDecimal("1.25"), settings.get("volume"));
    assertEquals("06", settings.get("code"));
    assertEquals(List.of(1, 2), values.get("channels"));
    assertEquals(6, FunctionParameterNormalizer.normalizeValue("int", "06"));
    assertEquals(6L, FunctionParameterNormalizer.normalizeValue("{\"type\":\"long\"}", "06"));
    assertEquals((short) 6, FunctionParameterNormalizer.normalizeValue("short", "6"));
    assertEquals((byte) 6, FunctionParameterNormalizer.normalizeValue("byte", "6"));
    assertEquals(new BigDecimal("0.6"), FunctionParameterNormalizer.normalizeValue("decimal", "0.6"));
  }

  @Test
  public void honorsExplicitEnumValueTypeAndPreservesNativeValues() {
    assertEquals(6, FunctionParameterNormalizer.normalizeValue(
        JSONUtil.parseObj("{\"type\":\"enum\",\"valueType\":\"int\"}"), "06"));
    assertEquals(Map.of("native", 1), FunctionParameterNormalizer.normalize(null, "missing", Map.of("native", 1)));
    assertEquals("06", FunctionParameterNormalizer.normalizeValue("custom", "06"));
  }

  @Test
  public void rejectsInvalidAndFractionalIntegerInsteadOfRoundingOrSendingAString() {
    for (String value : List.of("oops", "1.5", "2147483648")) {
      try {
        FunctionParameterNormalizer.normalize(METADATA, "controlLight", Map.of("slaveAddress", value));
        fail("invalid int accepted: " + value);
      } catch (IllegalArgumentException ex) {
        assertTrue(ex.getMessage().contains("slaveAddress"));
      }
    }
    try {
      FunctionParameterNormalizer.normalizeValue("boolean", "oops");
      fail("invalid boolean accepted");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("boolean"));
    }
  }
}
