package cn.universal.admin.platform.support;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.utils.JsonDesensitizationUtil;
import cn.universal.persistence.entity.IoTProduct;

/** Keeps the stored SIP service credential when an editor sends a masked or empty value. */
public final class SipProductConfigSupport {
  private SipProductConfigSupport() {}

  public static void preserveCredential(IoTProduct product, IoTProduct previous) {
    if (product == null || previous == null || !"sip".equals(previous.getThirdPlatform())) return;
    String merged =
        JsonDesensitizationUtil.mergeAuthConfig(
            product.getConfiguration(), previous.getConfiguration());
    if (merged == null || merged.isBlank()) return;
    JSONObject config = JSONUtil.parseObj(merged);
    String password = config.getStr("sipEslPassword");
    if (password == null || password.isBlank() || JsonDesensitizationUtil.isMaskedValue(password)) {
      JSONObject stored = JSONUtil.parseObj(previous.getConfiguration());
      if (stored.containsKey("sipEslPassword")) {
        config.set("sipEslPassword", stored.get("sipEslPassword"));
      }
    }
    product.setConfiguration(config.toString());
  }
}
