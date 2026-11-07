package cn.universal.web.websocket;

import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.VideoPlatformInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Single-use relay tickets are shared between Web nodes; device credentials stay server-side. */
@Service
public class VideoSdkTalkTickets {
  private static final String PREFIX = "video:sdk:talk:";
  private final VideoPlatformInstanceService instances;
  private final StringRedisTemplate redis;
  private final ObjectMapper mapper;
  public VideoSdkTalkTickets(VideoPlatformInstanceService instances, StringRedisTemplate redis, ObjectMapper mapper) {
    this.instances = instances; this.redis = redis; this.mapper = mapper;
  }

  public Map<String, Object> create(String instanceKey, Map<String, Object> body) {
    VideoPlatformInstance instance = instances.getByInstanceKey(instanceKey);
    // Only allow device/channel selectors, never client-supplied hosts, passwords or options.
    Map<String, Object> params = new LinkedHashMap<>();
    for (String key : List.of("deviceId", "channelId")) {
      String value = Objects.toString(body.get(key), "").trim();
      if (value.isEmpty() || value.length() > 128) throw new BaseException("请选择对讲设备和通道");
      params.put(key, value);
    }
    if (body.containsKey("voiceChannel")) {
      int channel;
      try { channel = Integer.parseInt(String.valueOf(body.get("voiceChannel"))); }
      catch (RuntimeException ex) { throw new BaseException("语音通道无效"); }
      if (channel < 1 || channel > 512) throw new BaseException("语音通道必须在 1 到 512 之间");
      params.put("voiceChannel", channel);
    }
    String mode=Objects.toString(body.get("mode"),"duplex");
    if(!Set.of("listen","speak","duplex").contains(mode))throw new BaseException("对讲模式无效");
    if(body.containsKey("codec")) {
      String codec=Objects.toString(body.get("codec"),"").toUpperCase(Locale.ROOT);
      if(!Set.of("G711U","G711A").contains(codec))throw new BaseException("对讲编码仅支持G711U或G711A");
      params.put("codec",codec);
    }
    if(instance!=null&&"hik_isup".equals(instance.getPlatformType()))params.put("mode",mode);
    else params.put("transfer", Boolean.TRUE.equals(body.get("transfer")));
    // The service enforces instance ownership/tenant access before resolving device credentials.
    Map<String, Object> result = instances.executePlatformOperation(instanceKey, "talkStart", params);
    URI base = endpoint(instance);
    String gatewayToken = Objects.toString(result.get("token"), "");
    if (!gatewayToken.matches("[a-fA-F0-9-]{36}")) throw new BaseException("网关返回的对讲凭据无效");
    try {
      URI upstream = new URI(base.getScheme().equals("https") ? "wss" : "ws", null, base.getHost(), base.getPort(),
          base.getPath().replaceAll("/+$", "") + "/ws/sdk-talk", "token=" + gatewayToken, null);
      String token = UUID.randomUUID().toString();
      redis.opsForValue().set(PREFIX + token, mapper.writeValueAsString(Map.of("upstream",upstream.toString(),"instanceKey",instanceKey,
          "deviceId",params.get("deviceId"),"channelId",params.get("channelId"))), Duration.ofSeconds(20));
      return Map.of("token", token, "sampleRate", result.get("sampleRate"), "frameSamples", result.get("frameSamples"),
          "encoding", "pcm_s16le", "channels", 1, "expiresIn", 20,"mode",mode);
    } catch (Exception ex) { throw new BaseException("创建对讲连接失败，请稍后重试"); }
  }

  public void cancel(String instanceKey,String token) {
    if (token == null || !token.matches("[a-fA-F0-9-]{36}")) return;
    String saved=redis.opsForValue().get(PREFIX+token);
    if(saved==null)return;
    try {
      var ticket=mapper.readTree(saved);
      if(!instanceKey.equals(ticket.path("instanceKey").asText()))throw new BaseException("对讲票据与实例不一致");
      instances.getDeviceDetail(instanceKey,ticket.path("deviceId").asText());
    } catch(BaseException denied){throw denied;}catch(Exception invalid){throw new BaseException("对讲票据无效");}
    String target = redis.opsForValue().getAndDelete(PREFIX + token);
    if (target == null) return; // Claimed connections are closed by the WebSocket relay.
    URI uri = target(target);
    try {
      String gatewayToken = org.springframework.web.util.UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
      URI cancel = new URI(uri.getScheme().equals("wss") ? "https" : "http", null, uri.getHost(), uri.getPort(),
          uri.getPath().replaceFirst("/ws/sdk-talk$", "/sdk/talk/cancel"), null, null);
      try (var response = cn.hutool.http.HttpRequest.post(cancel.toString()).timeout(5000)
          .contentType("application/json").body(mapper.writeValueAsString(Map.of("token", gatewayToken))).execute()) {
        if (!response.isOk()) throw new IllegalStateException();
      }
    } catch (Exception ex) { throw new BaseException("对讲取消请求未送达，网关将在超时后自动释放"); }
  }

  URI consume(String token) {
    if (token == null || !token.matches("[a-fA-F0-9-]{36}")) throw new BaseException("对讲凭据无效");
    String target = redis.opsForValue().getAndDelete(PREFIX + token);
    if (target == null) throw new BaseException("对讲凭据已过期或已使用");
    return target(target);
  }
  private URI target(String value) {
    try{return URI.create(value.startsWith("{")?mapper.readTree(value).path("upstream").asText():value);}
    catch(Exception invalid){throw new BaseException("对讲票据无效");}
  }

  private URI endpoint(VideoPlatformInstance instance) {
    if (instance == null || !Set.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup").contains(instance.getPlatformType()))
      throw new BaseException("此视频接入方式尚不支持SDK对讲");
    String endpoint = instance.getEndpoint();
    try {
      if (instance.getOptions() != null) {
        var options = mapper.readTree(instance.getOptions());
        for (String key : List.of("gatewayBaseUrl", "gatewayEndpoint", "baseUrl")) {
          if (!options.path(key).asText().isBlank()) { endpoint = options.path(key).asText(); break; }
        }
      }
      URI uri = URI.create(endpoint);
      if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
          || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) throw new IllegalArgumentException();
      return uri;
    } catch (Exception ex) { throw new BaseException("SDK 网关地址无效"); }
  }
}
