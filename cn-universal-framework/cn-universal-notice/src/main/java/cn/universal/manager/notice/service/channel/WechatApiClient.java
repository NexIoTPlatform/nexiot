package cn.universal.manager.notice.service.channel;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** stable_token 的普通刷新不会使其他节点的有效 token 失效。 */
@Component
public class WechatApiClient {
  private final ConcurrentHashMap<String, TokenEntry> tokens = new ConcurrentHashMap<>();
  private static final class TokenEntry { String value; long expiresAt; }

  public String token(String appId, String secret) {
    TokenEntry entry = tokens.computeIfAbsent(appId, key -> new TokenEntry());
    synchronized (entry) {
      long now = System.currentTimeMillis();
      if (entry.value != null && entry.expiresAt > now) return entry.value;
      JSONObject result = post("https://api.weixin.qq.com/cgi-bin/stable_token", JSONUtil.createObj()
          .set("grant_type", "client_credential").set("appid", appId).set("secret", secret).set("force_refresh", false));
      String value = result.getStr("access_token");
      if (value == null || value.isBlank()) throw new IllegalStateException("微信 stable_token 获取失败: " + result.getInt("errcode") + " " + result.getStr("errmsg"));
      entry.value = value;
      entry.expiresAt = now + Math.max(1, result.getInt("expires_in", 7200) - 300) * 1000L;
      return value;
    }
  }

  public void invalidate(String appId, String rejectedToken) {
    TokenEntry entry = tokens.get(appId);
    if (entry == null) return;
    synchronized (entry) {
      if (java.util.Objects.equals(entry.value, rejectedToken)) entry.expiresAt = 0;
    }
  }

  public JSONObject send(String token, JSONObject body) {
    return post("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + token, body);
  }

  protected JSONObject post(String url, JSONObject body) {
    try (HttpResponse response = HttpRequest.post(url).timeout(15000).contentType("application/json").body(body.toString()).execute()) {
      if (!response.isOk()) throw new IllegalStateException("微信接口 HTTP " + response.getStatus());
      return JSONUtil.parseObj(response.body());
    } catch (cn.hutool.http.HttpException e) {
      // 不自动重试发送超时，避免实际已受理时产生重复通知。
      throw new IllegalStateException("微信接口连接失败或超时，请核实微信端结果后再重试");
    }
  }
}
