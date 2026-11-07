package cn.universal.admin.platform.service.customfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.universal.persistence.entity.IoTProduct;
import org.junit.jupiter.api.Test;

class VideoCloudSystemFieldInjectorTest {

  private final VideoCloudSystemFieldInjector injector = new VideoCloudSystemFieldInjector();

  @Test
  void shouldInjectRequiredEzvizValidateCode() {
    JSONArray fields = new JSONArray();

    injector.inject(product("ezviz"), fields);

    JSONObject field = fields.getJSONObject(0);
    assertEquals("validateCode", field.getStr("id"));
    assertEquals("password", field.getStr("type"));
    assertTrue(field.getBool("required"));
    assertTrue(field.getBool("system"));
  }

  @Test
  void shouldNormalizeExistingImoulifeSecurityCode() {
    JSONArray fields = new JSONArray();
    fields.add(
        new JSONObject()
            .set("id", "appKey")
            .set("name", "安全码")
            .set("type", "string")
            .set("required", false)
            .set("system", false));

    injector.inject(product("imoulife"), fields);

    assertEquals(1, fields.size());
    JSONObject field = fields.getJSONObject(0);
    assertEquals("password", field.getStr("type"));
    assertTrue(field.getBool("required"));
    assertTrue(field.getBool("system"));
  }

  @Test
  void shouldSupportOnlyVideoCloudProducts() {
    assertTrue(injector.supports(product("ezviz")));
    assertTrue(injector.supports(product("IMOULIFE")));
    assertFalse(injector.supports(product("mqtt")));
  }

  private IoTProduct product(String platform) {
    return IoTProduct.builder().thirdPlatform(platform).build();
  }
}
