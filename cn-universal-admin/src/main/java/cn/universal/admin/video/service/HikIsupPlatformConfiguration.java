package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import cn.universal.common.utils.JsonDesensitizationUtil;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 每个平台的 CMS 监听与认证凭据；租户归属只取已保存实例。 */
@Component
@RequiredArgsConstructor
public class HikIsupPlatformConfiguration {
  private final VideoPlatformInstanceMapper instances;
  private final ObjectMapper json;

  public void validate(VideoPlatformInstance instance) {
    if (!"hik_isup".equals(instance.getPlatformType())) return;
    Map<String, Object> auth = read(instance.getAuth());
    Map<String, Object> options = read(instance.getOptions());
    String key = registrationKey(auth, options);
    if (key.isBlank() || JsonDesensitizationUtil.isMaskedValue(key))
      throw new BaseException("请填写 ISUP 平台密钥");
    if (key.getBytes(StandardCharsets.UTF_8).length > 32 || key.indexOf(0) >= 0)
      throw new BaseException("ISUP 平台密钥不能超过 32 个字节或包含空字符");
    VideoPlatformInstance previous = instance.getId() == null ? null : instances.selectByPrimaryKey(instance.getId());
    boolean keyUnchanged = previous != null && "hik_isup".equals(previous.getPlatformType())
        && Objects.equals(key, registrationKey(read(previous.getAuth()), read(previous.getOptions())));
    // 保留已部署的旧密钥；新建或显式更换时采用 IPC/NVR 均可填写的长度。
    if (!keyUnchanged && !key.matches("[A-Za-z0-9]{8,12}"))
      throw new BaseException("ISUP 注册密钥须为 8–12 位英文字母或数字");
    int port = cmsPort(options);
    String endpoint = gatewayEndpoint(instance, options);
    if (endpoint.isBlank()) throw new BaseException("请填写 ISUP SDK 网关地址");
    if (instance.getTenantId() == null || instance.getTenantId().isBlank())
      throw new BaseException("ISUP 平台必须属于一个租户");
    for (VideoPlatformInstance other : instances.select(VideoPlatformInstance.builder().platformType("hik_isup").build())) {
      if (Objects.equals(instance.getId(), other.getId()) && instance.getId() != null) continue;
      Map<String, Object> otherOptions = read(other.getOptions());
      if (Objects.equals(endpoint, gatewayEndpoint(other, otherOptions))) {
        if (port == cmsPort(otherOptions))
          throw new BaseException("此 SDK 网关上的 CMS 端口已被其他 ISUP 平台使用，请选择独立端口");
        String otherKey = registrationKey(read(other.getAuth()), otherOptions);
        if (java.security.MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), otherKey.getBytes(StandardCharsets.UTF_8)))
          throw new BaseException("此 SDK 网关上的平台密钥已被其他 ISUP 平台使用，请配置独立密钥");
        if (listenPort(options) == listenPort(otherOptions))
          throw new BaseException("此 SDK 网关上的 CMS 监听端口已被其他 ISUP 平台使用");
      }
    }
    auth.put("isupKey", key);
    options.remove("isupKey"); // 旧 options 凭据迁入 auth，配置页和状态不直接暴露密钥。
    options.put("cmsPort", port);
    options.putIfAbsent("cmsIp", "0.0.0.0");
    Object previousRevision = previous == null ? null : read(previous.getOptions()).get("savedRevision");
    long revision = previousRevision instanceof Number number ? number.longValue() : 0;
    options.put("savedRevision", Math.addExact(revision, 1));
    options.remove("applicationStatus");
    options.remove("applicationState");
    options.remove("appliedRevision");
    options.remove("applyError");
    instance.setAuth(write(auth));
    instance.setOptions(write(options));
  }

  private static String registrationKey(Map<String, Object> auth, Map<String, Object> options) {
    String key = Objects.toString(auth.get("isupKey"), "");
    return key.isBlank() ? Objects.toString(options.get("isupKey"), "") : key;
  }

  static int cmsPort(Map<String, Object> options) {
    Object value = options.getOrDefault("cmsPort", 7660);
    try {
      int port = Integer.parseInt(Objects.toString(value));
      if (port >= 1024 && port <= 65535) return port;
    } catch (NumberFormatException ignored) { }
    throw new BaseException("CMS 端口需在 1024 到 65535 之间");
  }

  public static long savedRevision(VideoPlatformInstance instance) {
    try {
      if (instance.getOptions() == null) return 0;
      return new ObjectMapper().readTree(instance.getOptions()).path("savedRevision").asLong(0);
    } catch (Exception ignored) { return 0; }
  }

  static int listenPort(Map<String, Object> options) {
    return cmsPort(Map.of("cmsPort", options.getOrDefault("cmsListenPort", cmsPort(options))));
  }

  static String gatewayEndpoint(VideoPlatformInstance instance, Map<String, Object> options) {
    String endpoint = Objects.toString(options.getOrDefault("gatewayBaseUrl", options.getOrDefault("gatewayEndpoint", instance.getEndpoint())), "").trim();
    try {
      URI uri = URI.create(endpoint);
      if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getUserInfo() != null)
        throw new IllegalArgumentException();
      int port = uri.getPort() < 0 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort();
      return uri.getScheme().toLowerCase(java.util.Locale.ROOT) + "://" + uri.getHost().toLowerCase(java.util.Locale.ROOT) + ":" + port;
    } catch (IllegalArgumentException e) {
      throw new BaseException("请填写有效的 HTTP 或 HTTPS SDK 网关地址");
    }
  }

  private Map<String, Object> read(String text) {
    if (text == null || text.isBlank()) return new LinkedHashMap<>();
    try { return json.readValue(text, new TypeReference<LinkedHashMap<String, Object>>() {}); }
    catch (Exception e) { throw new BaseException("ISUP 平台配置不是有效 JSON"); }
  }

  private String write(Map<String, Object> values) {
    try { return json.writeValueAsString(values); }
    catch (Exception e) { throw new BaseException("无法保存 ISUP 平台配置"); }
  }
}
