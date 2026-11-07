package cn.universal.admin.platform.service.customfield;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.universal.persistence.entity.IoTProduct;
import org.springframework.stereotype.Component;

/** 为需要设备侧验证码的视频云产品注入设备扩展字段。 */
@Component
public class VideoCloudSystemFieldInjector implements SystemFieldInjector {

  private static final String PLATFORM_EZVIZ = "ezviz";
  private static final String PLATFORM_IMOULIFE = "imoulife";

  @Override
  public boolean supports(IoTProduct product) {
    return product != null
        && (PLATFORM_EZVIZ.equalsIgnoreCase(product.getThirdPlatform())
            || PLATFORM_IMOULIFE.equalsIgnoreCase(product.getThirdPlatform()));
  }

  @Override
  public void inject(IoTProduct product, JSONArray customFields) {
    if (PLATFORM_EZVIZ.equalsIgnoreCase(product.getThirdPlatform())) {
      upsert(
          customFields,
          passwordField(
              "validateCode", "设备验证码", "萤石设备标签上的验证码，首次绑定设备时使用", "请输入设备验证码"));
      return;
    }
    upsert(
        customFields,
        passwordField("appKey", "设备安全码", "乐橙设备标签上的安全码，首次绑定设备时使用", "请输入设备安全码"));
  }

  @Override
  public int getOrder() {
    return 30;
  }

  private JSONObject passwordField(
      String id, String name, String description, String placeholder) {
    JSONObject field = new JSONObject();
    field.set("id", id);
    field.set("name", name);
    field.set("description", description);
    field.set("type", "password");
    field.set("required", true);
    field.set("placeholder", placeholder);
    field.set("options", new JSONArray());
    field.set("disabled", false);
    field.set("hidden", false);
    field.set("system", true);
    field.set("autoGenerate", false);
    return field;
  }

  private void upsert(JSONArray fields, JSONObject replacement) {
    String id = replacement.getStr("id");
    for (int i = 0; i < fields.size(); i++) {
      JSONObject current = fields.getJSONObject(i);
      if (current != null && id.equals(current.getStr("id"))) {
        fields.set(i, replacement);
        return;
      }
    }
    fields.add(replacement);
  }
}
