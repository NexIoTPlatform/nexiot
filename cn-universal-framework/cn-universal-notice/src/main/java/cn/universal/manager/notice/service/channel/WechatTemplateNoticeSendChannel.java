package cn.universal.manager.notice.service.channel;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.service.WechatOpenIdService;
import cn.universal.manager.notice.util.WechatTemplateRenderer;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class WechatTemplateNoticeSendChannel extends AbstractNoticeSendChannel {
  private final WechatOpenIdService openIds;
  private final WechatApiClient client;
  public WechatTemplateNoticeSendChannel(WechatOpenIdService openIds, WechatApiClient client) {
    this.openIds = openIds;
    this.client = client;
  }
  @Override public boolean support(String type) { return WechatTemplateRenderer.isWechat(type); }

  @Override public NoticeSendResult send(String content, String receivers, NoticeChannel config, Map<String, Object> params) {
    List<String> ids = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    try {
      JSONObject c = parseConfig(config);
      if (!StringUtils.hasText(c.getStr("appId")) || !StringUtils.hasText(c.getStr("appSecret")))
        throw new IllegalArgumentException("公众号 AppID、AppSecret 配置不完整");
      if (!StringUtils.hasText(receivers)) throw new IllegalArgumentException("用户 OpenID 不能为空");
      JSONObject payload = JSONUtil.parseObj(content);
      if (!StringUtils.hasText(payload.getStr("template_id")) || payload.getJSONObject("data") == null || payload.getJSONObject("data").isEmpty())
        throw new IllegalArgumentException("微信模板字段未解析，请检查模板映射");
      Set<String> targets = new LinkedHashSet<>();
      for (String receiver : receivers.split(",")) {
        String openid = receiver.trim();
        if (openid.startsWith("user:")) {
          String key = openid.substring(5);
          openid = openIds.resolve(config.getId(), key);
          if (!StringUtils.hasText(openid)) throw new IllegalArgumentException("用户尚未绑定该公众号: " + key);
        }
        if (!openid.matches("[a-zA-Z0-9_-]{10,128}")) throw new IllegalArgumentException("OpenID 格式无效");
        targets.add(openid);
      }
      String token = client.token(c.getStr("appId"), c.getStr("appSecret"));
      for (String openid : targets) {
        try {
          JSONObject body = JSONUtil.parseObj(payload.toString()).set("touser", openid);
          JSONObject response = client.send(token, body);
          // 仅在微信明确拒绝 token 时刷新重试一次；超时或业务错误不重试。
          if (Set.of(40001, 40014, 42001).contains(response.getInt("errcode", -1))) {
            client.invalidate(c.getStr("appId"), token);
            token = client.token(c.getStr("appId"), c.getStr("appSecret"));
            response = client.send(token, body);
          }
          if (response.getInt("errcode", -1) == 0 && response.get("msgid") != null) ids.add(response.getStr("msgid"));
          else errors.add(response.getInt("errcode", -1) + ": " + response.getStr("errmsg", "未返回消息 ID"));
        } catch (Exception e) { errors.add(e.getMessage()); }
      }
    } catch (Exception e) { errors.add(e.getMessage()); }
    boolean success = errors.isEmpty() && !ids.isEmpty();
    NoticeSendResult result = new NoticeSendResult(success, receivers, content,
        errors.isEmpty() ? null : "微信已受理 " + ids.size() + " 条；失败: " + String.join("; ", errors));
    result.setMessageIds(ids);
    log.info("[WeChat] 接口受理={}，失败={}，msgids={}", ids.size(), errors.size(), ids);
    return result;
  }
}
