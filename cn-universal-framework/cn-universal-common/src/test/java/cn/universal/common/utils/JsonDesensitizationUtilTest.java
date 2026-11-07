package cn.universal.common.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class JsonDesensitizationUtilTest {

  @Test
  public void shouldRedactSipCredentialsInsideConfigurationStringsWithoutMutatingInput() {
    JSONObject input = new JSONObject();
    input.set("configuration", "{\"sipUsername\":\"1001\",\"sipPassword\":\"private-account\"}");
    input.set("productConfiguration", "{\"sipEslPassword\":\"private-control\"}");
    JSONObject result = JsonDesensitizationUtil.desensitizeJsonObject(input);
    Assert.assertFalse(result.toString().contains("private-account"));
    Assert.assertFalse(result.toString().contains("private-control"));
    Assert.assertEquals("1001", JSONUtil.parseObj(result.getStr("configuration")).getStr("sipUsername"));
    Assert.assertTrue(input.toString().contains("private-account"));
  }

  @Test
  public void shouldReplaceScopedKeysAndRemoveMissingEntriesWithinScope() {
    String oldConfig =
        "{\"downTopic\":\"$thing/down/tp/{{deviceId}}\","
            + "\"subDeviceDownTopic\":\"$thing/down/${gatewayPk}/${gatewayDk}/sub/${subPk}/${subDk}\","
            + "\"upTopic\":\"$thing/up/tp/{{deviceId}}\"}";
    String newConfig =
        "{\"subDeviceDownTopic\":\"$thing/down/${gatewayPk}/${gatewayDk}/sub/${subPk}/${subDk}\"}";

    String merged =
        JsonDesensitizationUtil.replaceScopedAuthConfig(
            newConfig, oldConfig, Arrays.asList("downTopic", "subDeviceDownTopic"));

    JSONObject result = JSONUtil.parseObj(merged);
    Assert.assertFalse(result.containsKey("downTopic"));
    Assert.assertEquals(
        "$thing/down/${gatewayPk}/${gatewayDk}/sub/${subPk}/${subDk}",
        result.getStr("subDeviceDownTopic"));
    Assert.assertEquals("$thing/up/tp/{{deviceId}}", result.getStr("upTopic"));
  }

  @Test
  public void shouldKeepOriginalValueForMaskedScopedField() {
    String oldConfig = "{\"password\":\"real-secret\",\"upTopic\":\"$thing/up/tp/{{deviceId}}\"}";
    String newConfig = "{\"password\":\"***\"}";

    String merged =
        JsonDesensitizationUtil.replaceScopedAuthConfig(
            newConfig, oldConfig, Arrays.asList("password"));

    JSONObject result = JSONUtil.parseObj(merged);
    Assert.assertEquals("real-secret", result.getStr("password"));
    Assert.assertEquals("$thing/up/tp/{{deviceId}}", result.getStr("upTopic"));
  }
}
