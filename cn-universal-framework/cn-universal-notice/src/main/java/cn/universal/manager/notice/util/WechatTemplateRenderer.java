package cn.universal.manager.notice.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.model.NoticeTemplate;
import cn.universal.manager.notice.model.WechatTemplateConfig;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/** 微信原文只负责声明字段；映射表达式与其他通知、规则使用相同的 NexIoT 参数。 */
public final class WechatTemplateRenderer {
  private static final Pattern FIELD = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\.DATA\\s*}}");
  private static final Pattern PARAM = Pattern.compile("#\\{([a-zA-Z0-9_.]+)}");
  private WechatTemplateRenderer() {}

  public static boolean isWechat(String type) {
    return "wechat".equalsIgnoreCase(type) || "wechat_template".equalsIgnoreCase(type);
  }

  public static void validate(NoticeTemplate template) {
    WechatTemplateConfig config = template.getWechat();
    if (config == null || !StringUtils.hasText(config.getTemplateId()))
      throw new IllegalArgumentException("请配置微信模板 ID 和字段映射");
    if (config.getTemplateId().contains("#{") || config.getTemplateId().length() > 128)
      throw new IllegalArgumentException("微信模板 ID 必须是公众号申请的固定 ID");
    Set<String> declared = new LinkedHashSet<>();
    FIELD.matcher(template.getContent() == null ? "" : template.getContent()).results()
        .forEach(m -> declared.add(m.group(1)));
    if (declared.isEmpty()) throw new IllegalArgumentException("请粘贴包含 {{字段.DATA}} 的微信模板原文");
    Set<String> mapped = new LinkedHashSet<>();
    if (config.getFields() == null) throw new IllegalArgumentException("微信字段映射不能为空");
    for (WechatTemplateConfig.Field field : config.getFields()) {
      if (field == null || field.getKey() == null || !declared.contains(field.getKey()))
        throw new IllegalArgumentException("映射字段必须与微信原文完全一致");
      if (!mapped.add(field.getKey())) throw new IllegalArgumentException("微信字段重复: " + field.getKey());
      if (!StringUtils.hasText(field.getValue())) throw new IllegalArgumentException("请配置字段 " + field.getKey() + " 的取值");
      if (PARAM.matcher(field.getValue()).replaceAll("").contains("#{"))
        throw new IllegalArgumentException("字段 " + field.getKey() + " 的参数表达式无效");
    }
    declared.removeAll(mapped);
    if (!declared.isEmpty()) throw new IllegalArgumentException("微信字段缺少映射: " + String.join(", ", declared));
    if (StringUtils.hasText(config.getMiniProgramAppId()) != StringUtils.hasText(config.getMiniProgramPage()))
      throw new IllegalArgumentException("小程序 AppID 和页面路径必须同时配置");
    if (StringUtils.hasText(config.getUrl())) validateUrl(config.getUrl().replaceAll("#\\{[a-zA-Z0-9_.]+}", "value"));
  }

  public static JSONObject render(NoticeTemplate template, NoticeChannel channel, Map<String, Object> params) {
    WechatTemplateConfig config = template.getWechat();
    // 旧版开放接口只兼容显式 data，绝不把整个业务上下文误当成微信字段。
    if (config == null) {
      JSONObject legacy = JSONUtil.parseObj(channel.getConfig());
      Object raw = params == null ? null : params.get("data");
      if (!StringUtils.hasText(legacy.getStr("templateId")) || !(raw instanceof Map<?, ?> data) || data.isEmpty())
        throw new IllegalArgumentException("旧微信模板缺少字段映射，请编辑模板并识别微信字段");
      JSONObject body = JSONUtil.createObj().set("template_id", legacy.getStr("templateId"));
      JSONObject values = JSONUtil.createObj();
      data.forEach((key, value) -> {
        if (!String.valueOf(key).matches("[a-zA-Z][a-zA-Z0-9_]*")) throw new IllegalArgumentException("微信字段名无效");
        Object scalar = value instanceof Map<?, ?> map ? map.get("value") : value;
        values.set(String.valueOf(key), Map.of("value", scalar(scalar, String.valueOf(key))));
      });
      body.set("data", values);
      addJump(body, legacy.getStr("url"), legacy.getStr("miniProgramAppId"), legacy.getStr("miniProgramPage"), params);
      return body;
    }
    validate(template);
    JSONObject body = JSONUtil.createObj().set("template_id", config.getTemplateId().trim());
    JSONObject values = JSONUtil.createObj();
    for (WechatTemplateConfig.Field field : config.getFields()) {
      String value = resolve(field.getValue(), params, field.getKey());
      values.set(field.getKey(), Map.of("value", scalar(value, field.getKey())));
    }
    body.set("data", values);
    addJump(body, config.getUrl(), config.getMiniProgramAppId(), config.getMiniProgramPage(), params);
    return body;
  }

  private static void addJump(JSONObject body, String url, String appId, String page, Map<String, Object> params) {
    if (StringUtils.hasText(url)) {
      String resolved = resolve(url, params, "跳转链接");
      validateUrl(resolved);
      body.set("url", resolved);
    }
    if (StringUtils.hasText(appId) != StringUtils.hasText(page))
      throw new IllegalArgumentException("小程序 AppID 和页面路径必须同时配置");
    if (StringUtils.hasText(appId)) body.set("miniprogram", Map.of("appid", appId.trim(), "pagepath", resolve(page, params, "小程序页面")));
  }

  private static void validateUrl(String url) {
    try {
      URI uri = URI.create(url);
      if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null)
        throw new IllegalArgumentException();
    } catch (IllegalArgumentException e) { throw new IllegalArgumentException("跳转链接必须是完整的 HTTP(S) 地址"); }
  }

  public static String resolve(String expression, Map<String, Object> params, String field) {
    return PARAM.matcher(expression).replaceAll(match -> {
      String path = match.group(1);
      Object value = params;
      if (params != null && params.containsKey(path)) value = params.get(path);
      else for (String part : path.split("\\.")) value = value instanceof Map<?, ?> map ? map.get(part) : null;
      return java.util.regex.Matcher.quoteReplacement(scalar(value, field + "（参数 " + path + "）"));
    });
  }

  private static String scalar(Object value, String field) {
    if (value == null || value instanceof Map || value instanceof Iterable || value.getClass().isArray() || !StringUtils.hasText(String.valueOf(value)))
      throw new IllegalArgumentException("微信字段 " + field + " 缺少有效值，请填写标量参数");
    return String.valueOf(value);
  }
}
