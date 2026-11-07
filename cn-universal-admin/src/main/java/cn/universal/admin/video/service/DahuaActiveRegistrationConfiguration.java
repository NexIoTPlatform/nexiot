package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 大华主动注册监听配置与实例租户绑定，直连设备继续使用原配置。 */
@Component
@RequiredArgsConstructor
public class DahuaActiveRegistrationConfiguration {
  private final VideoPlatformInstanceMapper instances;
  private final ObjectMapper json;

  public void validate(VideoPlatformInstance instance) {
    if (!"dahua_sdk".equals(instance.getPlatformType())) return;
    Map<String, Object> options = read(instance.getOptions());
    Object raw = options.get("activeRegistration");
    if (raw == null) return;
    if (!(raw instanceof Map<?, ?> map)) throw new BaseException("大华主动注册配置必须是 JSON 对象");
    Map<String, Object> auth = read(instance.getAuth());
    Object password = auth.get("activeRegistrationPassword");
    if (password != null && String.valueOf(password).isBlank() && instance.getId() != null) {
      VideoPlatformInstance previous = instances.selectByPrimaryKey(instance.getId());
      Object savedPassword = previous == null ? null : read(previous.getAuth()).get("activeRegistrationPassword");
      if (savedPassword != null) {
        auth.put("activeRegistrationPassword", savedPassword);
        try { instance.setAuth(json.writeValueAsString(auth)); }
        catch (Exception e) { throw new BaseException("无法保存大华主动注册凭据"); }
      }
    }
    Map<String, Object> config = new LinkedHashMap<>();
    map.forEach((key, value) -> config.put(String.valueOf(key), value));
    if (!(config.get("enabled") instanceof Boolean)) throw new BaseException("主动注册启用状态必须是布尔值");
    if (!Boolean.TRUE.equals(config.get("enabled"))) return;
    if (instance.getTenantId() == null || instance.getTenantId().isBlank())
      throw new BaseException("大华主动注册平台必须属于一个租户");
    int port = port(config.getOrDefault("listenPort", 9500));
    String endpoint = gatewayEndpoint(instance, options);
    String bind = Objects.toString(config.getOrDefault("listenIp", "0.0.0.0"), "").trim();
    if (!bind.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")
        || java.util.Arrays.stream(bind.split("\\.")).anyMatch(part -> Integer.parseInt(part) > 255))
      throw new BaseException("主动注册监听地址须为服务器本机 IPv4 地址，监听所有网卡请填 0.0.0.0");
    String advertised = Objects.toString(config.get("advertisedHost"), "").trim();
    if (advertised.isBlank() || advertised.equals("0.0.0.0") || advertised.length() > 253
        || advertised.contains(":") || advertised.contains("/") || advertised.matches(".*\\s.*"))
      throw new BaseException("请填写设备能访问的主动注册服务器 IP 或域名，不包含端口和协议");
    for (VideoPlatformInstance other : instances.select(VideoPlatformInstance.builder().platformType("dahua_sdk").build())) {
      if (instance.getId() != null && Objects.equals(instance.getId(), other.getId())) continue;
      Map<String, Object> otherOptions = read(other.getOptions());
      if (!enabled(other) || !Objects.equals(endpoint, gatewayEndpoint(other, otherOptions))) continue;
      Map<?, ?> otherConfig = (Map<?, ?>) otherOptions.get("activeRegistration");
      if (port == port(otherConfig.containsKey("listenPort") ? otherConfig.get("listenPort") : 9500))
        throw new BaseException("此 SDK 网关上的主动注册端口已被其他大华实例使用，请选择独立端口");
    }
    config.put("listenIp", bind); config.put("listenPort", port); config.put("advertisedHost", advertised);
    options.put("activeRegistration", config);
    try { instance.setOptions(json.writeValueAsString(options)); }
    catch (Exception e) { throw new BaseException("无法保存大华主动注册配置"); }
  }

  public static boolean enabled(VideoPlatformInstance instance) {
    if (instance == null || !"dahua_sdk".equals(instance.getPlatformType())) return false;
    try { return new ObjectMapper().readTree(instance.getOptions() == null ? "{}" : instance.getOptions())
        .path("activeRegistration").path("enabled").asBoolean(false); }
    catch (Exception e) { return false; }
  }

  public static boolean configured(VideoPlatformInstance instance) {
    if (instance == null || !"dahua_sdk".equals(instance.getPlatformType())) return false;
    try { return new ObjectMapper().readTree(instance.getOptions() == null ? "{}" : instance.getOptions())
        .path("activeRegistration").isObject(); }
    catch (Exception e) { return false; }
  }

  static String gatewayEndpoint(VideoPlatformInstance instance, Map<String, Object> options) {
    String raw = Objects.toString(instance.getEndpoint(), "");
    // 与 SDK 客户端的网关别名和非空优先级保持一致。
    for (String alias : List.of("gatewayBaseUrl", "gatewayEndpoint", "baseUrl")) {
      Object value = options.get(alias);
      if (value != null && !String.valueOf(value).isBlank()) {
        raw = String.valueOf(value); break;
      }
    }
    raw = raw.trim();
    String origin = HikIsupPlatformConfiguration.gatewayEndpoint(instance, Map.of("gatewayBaseUrl", raw));
    // 反向代理可在同一域名下通过路径连接不同网关，路径也是有效地址的一部分。
    String path = URI.create(raw).getRawPath();
    return origin + (path == null ? "" : path.replaceAll("/+$", ""));
  }

  private int port(Object value) {
    try {
      int port = Integer.parseInt(Objects.toString(value));
      if (port >= 1024 && port <= 65535) return port;
    } catch (NumberFormatException ignored) { }
    throw new BaseException("主动注册监听端口须在 1024 到 65535 之间");
  }

  private Map<String, Object> read(String value) {
    if (value == null || value.isBlank()) return new LinkedHashMap<>();
    try { return json.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() {}); }
    catch (Exception e) { throw new BaseException("大华平台配置不是有效 JSON"); }
  }
}
