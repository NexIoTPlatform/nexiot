package cn.universal.manager.notice.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.manager.notice.model.NoticeChannel;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** OAuth state 一次性消费；结果只允许创建者查询，支持多节点回调。 */
@Service
public class WechatBindingSessionService {
  private static final String STATE = "notice:wechat:oauth:state:";
  private static final String RESULT = "notice:wechat:oauth:result:";
  private static final Duration TTL = Duration.ofMinutes(5);
  @org.springframework.beans.factory.annotation.Autowired private NoticeAccess access;
  private final StringRedisTemplate redis;
  private final WechatOpenIdService openIds;
  private final NoticeChannelService channels;
  public WechatBindingSessionService(StringRedisTemplate redis, WechatOpenIdService openIds, NoticeChannelService channels) {
    this.redis = redis; this.openIds = openIds; this.channels = channels;
  }
  public Map<String, Object> create(NoticeChannel channel, String userKey, String owner) {
    if (userKey == null || userKey.isBlank()) userKey = "scan_" + UUID.randomUUID().toString().replace("-", "");
    if (!userKey.matches("[a-zA-Z0-9_@.:-]{1,100}")) throw new IllegalArgumentException("绑定标识限 1-100 位字母、数字及 _ @ . : -");
    String callback = JSONUtil.parseObj(channel.getConfig()).getStr("oauthCallbackUrl");
    try {
      URI uri = URI.create(callback == null ? "" : callback);
      if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null
          || !uri.getPath().endsWith("/open-api/v1/notice/wechat/callback") || uri.getQuery() != null || uri.getFragment() != null) throw new IllegalArgumentException();
    } catch (IllegalArgumentException e) { throw new IllegalArgumentException("请在渠道配置中填写公网微信回调地址，以 /open-api/v1/notice/wechat/callback 结尾，并在公众号配置网页授权域名"); }
    String id = UUID.randomUUID().toString().replace("-", "");
    JSONObject session = JSONUtil.createObj().set("channelId", channel.getId()).set("userKey", userKey).set("owner", owner).set("status", "WAITING");
    redis.opsForValue().set(STATE + id, session.toString(), TTL);
    redis.opsForValue().set(RESULT + id, session.toString(), TTL);
    return Map.of("sessionId", id, "url", openIds.authorizationUrl(channel, callback, id), "expiresIn", TTL.toSeconds());
  }
  public Map<String, Object> status(String id, String owner) {
    JSONObject session = result(id);
    if (!owner.equals(session.getStr("owner"))) throw new IllegalArgumentException("无权限读取该绑定会话");
    return Map.of("status", session.getStr("status"), "channelId", session.getLong("channelId"), "userKey", session.getStr("userKey"), "openid", session.getStr("openid", ""), "message", session.getStr("message", ""));
  }
  private JSONObject result(String id) {
    if (id == null || !id.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("绑定会话无效");
    String raw = redis.opsForValue().get(RESULT + id);
    if (raw == null) throw new IllegalArgumentException("二维码已过期，请重新生成");
    return JSONUtil.parseObj(raw);
  }
  public void complete(String id, String code) {
    result(id);
    String raw = redis.opsForValue().getAndDelete(STATE + id);
    if (raw == null) throw new IllegalArgumentException("二维码已使用或过期");
    JSONObject session = JSONUtil.parseObj(raw);
    try {
      NoticeChannel channel = channels.getById(session.getLong("channelId"));
      if (channel == null || "1".equals(channel.getStatus())) throw new IllegalArgumentException("通知渠道不可用");
      access.checkCreator(session.getStr("owner"), channel, channel.getCreator());
      access.runAs(session.getStr("owner"), () -> {
        String openid = openIds.exchange(channel, code);
        String userKey = openIds.bind(channel.getId(), session.getStr("userKey"), openid);
        session.set("userKey", userKey);
        session.set("status", "BOUND").set("openid", openid);
        return null;
      });
    } catch (Exception e) {
      session.set("status", "FAILED").set("message", e.getMessage());
      throw e;
    } finally { redis.opsForValue().set(RESULT + id, session.toString(), TTL); }
  }
}
