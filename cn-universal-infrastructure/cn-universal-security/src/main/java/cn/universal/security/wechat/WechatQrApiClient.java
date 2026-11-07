package cn.universal.security.wechat;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.WechatLoginConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** 公众号临时二维码；凭据按 AppID 和配置版本摘要缓存于共享 Redis。 */
@Component
public class WechatQrApiClient {
  private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
      "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) end return 0", Long.class);
  private final StringRedisTemplate redis;

  public WechatQrApiClient(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public String createQr(WechatLoginConfig config, String scene, int expiresIn) {
    String token = token(config);
    JSONObject body = JSONUtil.createObj().set("expire_seconds", expiresIn)
        .set("action_name", "QR_STR_SCENE")
        .set("action_info", JSONUtil.createObj().set("scene", JSONUtil.createObj().set("scene_str", scene)));
    JSONObject result = post("https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=" + token, body);
    int code = result.getInt("errcode", 0);
    if (code == 40001 || code == 40014 || code == 42001) {
      redis.delete(tokenKey(config));
      result = post("https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=" + token(config), body);
      code = result.getInt("errcode", 0);
    }
    if (code == 48001 || code == 50001) throw new CapabilityUnavailableException();
    if (code != 0 || result.getStr("ticket", "").isBlank()) {
      throw new IoTException("微信二维码创建失败，错误码 " + code + "，请核对公众号权限与配置");
    }
    return "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket="
        + java.net.URLEncoder.encode(result.getStr("ticket"), StandardCharsets.UTF_8);
  }

  private String token(WechatLoginConfig config) {
    if (config.getAppSecret() == null || config.getAppSecret().isBlank()) {
      throw new CapabilityUnavailableException();
    }
    String key = tokenKey(config);
    String token = redis.opsForValue().get(key);
    if (token != null) return token;
    String lock = UUID.randomUUID().toString();
    if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key + ":lock", lock, Duration.ofSeconds(15)))) {
      throw new IoTException("公众号凭据正在刷新，请稍后重试");
    }
    try {
      token = redis.opsForValue().get(key);
      if (token != null) return token;
      JSONObject result = post("https://api.weixin.qq.com/cgi-bin/stable_token",
          JSONUtil.createObj().set("grant_type", "client_credential").set("appid", config.getAppId())
              .set("secret", config.getAppSecret()).set("force_refresh", false));
      int code = result.getInt("errcode", 0);
      if (code == 48001 || code == 50001) throw new CapabilityUnavailableException();
      token = result.getStr("access_token");
      if (code != 0 || token == null || token.isBlank()) {
        throw new IoTException("公众号接口凭据获取失败，错误码 " + code + "，请核对 AppSecret 与 IP 白名单");
      }
      redis.opsForValue().set(key, token, Duration.ofSeconds(Math.max(60, result.getInt("expires_in", 7200) - 300)));
      return token;
    } finally {
      redis.execute(UNLOCK, List.of(key + ":lock"), lock);
    }
  }

  private String tokenKey(WechatLoginConfig config) {
    try {
      String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(config.getAppSecret().getBytes(StandardCharsets.UTF_8))).substring(0, 16);
      return "auth:wechat:access-token:" + config.getAppId() + ":" + fingerprint;
    } catch (Exception e) {
      throw new IoTException("公众号凭据配置无效");
    }
  }

  protected JSONObject post(String url, JSONObject body) {
    try (HttpResponse response = HttpRequest.post(url).timeout(4500).contentType("application/json")
        .body(body.toString()).execute()) {
      if (!response.isOk()) throw new IoTException("微信接口暂不可用，请稍后重试");
      return JSONUtil.parseObj(response.body());
    } catch (cn.hutool.http.HttpException e) {
      // 不记录请求 URL（access_token）与请求体（AppSecret）。
      throw new IoTException("微信接口连接超时，请稍后重试");
    }
  }

  public static class CapabilityUnavailableException extends IoTException {
    public CapabilityUnavailableException() {
      super("当前公众号不具备带参数二维码能力，请使用消息验证码方式");
    }
  }
}
