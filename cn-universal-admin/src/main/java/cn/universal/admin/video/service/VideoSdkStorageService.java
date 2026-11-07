package cn.universal.admin.video.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import cn.universal.common.exception.BaseException;
import cn.universal.ossm.service.ISysOssConfigService;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.persistence.entity.VideoPlatformInstance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Secret-bearing requests are server-to-server only and deliberately excluded from request audit logs. */
@Service
@RequiredArgsConstructor
public class VideoSdkStorageService {
  private final ISysOssConfigService oss;
  private final VideoPlatformInstanceService instances;
  private final ObjectMapper mapper;
  @Value("${nexiot.video.sdk.storage-management-token:${NEXIOT_SDK_STORAGE_MANAGEMENT_TOKEN:}}")
  private String managementToken;
  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.beans.factory.ObjectProvider<cn.universal.dm.video.VideoPlatformAdapterRegistry> adapters;

  public Map<String, Object> configure(VideoPlatformInstance instance, Map<String, Object> input) {
    if (cn.universal.dm.video.VideoSdkPlatforms.isup(instance.getPlatformType()))
      throw new BaseException("ISUP 存储为共享网关级配置，请由网关运维配置；平台实例仅可查看状态");
    String tenant = instance.getTenantId();
    if (tenant == null || tenant.isBlank()) throw new BaseException("请先为 SDK 实例设置租户归属");
    String endpoint = endpoint(instance);
    for (VideoPlatformInstance other : instances.listAll()) {
      if (cn.universal.dm.video.VideoSdkPlatforms.managed(other.getPlatformType()) && endpoint.equals(endpoint(other))
          && !Objects.equals(tenant, other.getTenantId()))
        throw new BaseException("该 SDK 网关被多个租户共用，请为租户配置独立网关后再下发存储配置");
    }
    Map<String, Object> settings = read(instance);
    boolean reset = Boolean.TRUE.equals(input.get("resetToStartup"));
    if (reset) settings = call(instance, Method.GET, null, "/sdk/storage/defaults");
    settings.remove("s3CredentialsConfigured"); settings.remove("remoteConfigEnabled");
    settings.remove("configSource"); settings.remove("configWarning");
    settings.remove("platformCloud");
    settings.put("ownerTenantId", tenant);
    for (String key : List.of("mode", "localDirectory", "localBaseUrl", "alarmSnapshotEnabled", "alarmSnapshotIntervalSeconds", "alarmRecordingEnabled", "alarmPreSeconds", "alarmPostSeconds",
        "alarmRecordSettleSeconds", "alarmRecordIntervalSeconds", "healthEnabled", "healthIntervalSeconds", "recordingExpected", "recordingGapSeconds"))
      if (!reset && input.containsKey(key)) settings.put(key, input.get(key));
    String mode = Objects.toString(settings.get("mode"), "");
    if (!Set.of("local", "s3", "both").contains(mode)) throw new BaseException("存储模式无效");
    if (!mode.equals("s3")) {
      settings.put("localBaseUrl", httpUrl(settings.get("localBaseUrl"), "网关文件访问地址"));
      String directory = Objects.toString(settings.get("localDirectory"), "").trim();
      if (directory.isBlank()) throw new BaseException("本地保存目录不能为空");
      settings.put("localDirectory", directory);
    }
    if (!mode.equals("local")) {
      if ("manual".equals(input.get("source"))) {
        throw new BaseException("请在平台云存储页面统一配置，SDK 网关只支持复用平台云存储");
      }
      settings.putAll(platformSettings(tenant));
    }
    return withPlatformSummary(instance, call(instance, Method.PUT, settings));
  }

  public Map<String, Object> read(VideoPlatformInstance instance) {
    if (cn.universal.dm.video.VideoSdkPlatforms.isup(instance.getPlatformType())) return readIsupStatus(instance);
    Map<String, Object> settings = call(instance, Method.GET, null);
    String owner = Objects.toString(settings.get("ownerTenantId"), "");
    if (!owner.isBlank() && !owner.equals(instance.getTenantId())) throw new BaseException("此网关存储已绑定其他租户");
    return withPlatformSummary(instance, settings);
  }

  private Map<String,Object> readIsupStatus(VideoPlatformInstance instance) {
    if(instance.getTenantId()==null||instance.getTenantId().isBlank())throw new BaseException("请先为SDK实例设置租户归属");
    Map<String,Object> runtime=adapters.getObject().getAdapter(instance.getPlatformType()).executeOperation(instance,"instanceStatus",Map.of());
    if(!Objects.equals(instance.getTenantId(),runtime.get("tenantId")))throw new BaseException("ISUP网关存储状态租户归属不匹配");
    Map<?,?> services=runtime.get("services") instanceof Map<?,?> map?map:Map.of();
    Map<?,?> storage=services.get("storage") instanceof Map<?,?> map?map:Map.of();
    boolean listening=Boolean.TRUE.equals(storage.get("listening"));
    // Runtime diagnostics use the authenticated instance contract, never the gateway's write credential.
    Map<String,Object> status=new LinkedHashMap<>();
    status.put("configSource","gateway_runtime_status");status.put("status",listening?"ready":"unavailable");status.put("readonly",true);
    status.put("storageService",Map.of("listening",listening,"lastError",Objects.toString(storage.get("lastError"),"").substring(0,Math.min(500,Objects.toString(storage.get("lastError"),"").length()))));
    status.put("message","ISUP图片服务运行状态；保存方式和容量由共享网关节点运维配置");
    return withPlatformSummary(instance,status);
  }

  private Map<String, Object> withPlatformSummary(VideoPlatformInstance instance, Map<String, Object> settings) {
    Map<String, Object> cloud = new LinkedHashMap<>();
    try {
      var platform = platformSettings(instance.getTenantId());
      cloud.put("configured", true);
      cloud.put("provider", platform.get("provider"));
      cloud.put("bucket", platform.get("s3Bucket"));
      cloud.put("baseUrl", platform.get("s3PublicBaseUrl"));
    } catch (Exception ex) {
      cloud.put("configured", false);
      cloud.put("message", ex instanceof BaseException ? ex.getMessage() : "平台云存储配置不可用，请前往云存储页面检查");
    }
    settings.put("platformCloud", cloud);
    settings.put("readOnly", cn.universal.dm.video.VideoSdkPlatforms.isup(instance.getPlatformType()));
    settings.put("scope", "gateway");
    return settings;
  }

  private Map<String, Object> platformSettings(String tenant) {
    if (tenant == null || tenant.isBlank()) throw new BaseException("请先为 SDK 实例设置租户归属");
    String provider = oss.getActiveProvider(tenant);
    if (!Set.of("minio", "qiniu", "aliyun").contains(Objects.toString(provider, "")))
      throw new BaseException("当前平台启用的云存储服务商暂不支持 SDK 网关，请选择 MinIO、七牛或阿里云 OSS");
    var config = oss.getRuntimeConfig(provider, tenant);
    if (config == null || blank(config.getBucketName()) || blank(config.getAccessKey()) || blank(config.getSecretKey()))
      throw new BaseException("请先在平台云存储页面配置存储桶和访问凭证");
    String endpoint = provider.equals("qiniu") ? "" : httpUrl(config.getEndpoint(), "平台云存储服务端地址");
    // Use the platform provider's URL rules, including MinIO bucket paths and OSS CNAMEs.
    String base = OssTenantContext.run(tenant, () -> OssFactory.instance(provider).getEndpointLink());
    if (!blank(base) && !base.contains("://")) base = (Boolean.TRUE.equals(config.getIsHttps()) ? "https://" : "http://") + base;
    if (provider.equals("qiniu") && Boolean.TRUE.equals(config.getIsHttps()) && base != null) base = base.replaceFirst("^http://", "https://");
    base = httpUrl(base, "平台云存储文件访问地址");
    String prefix = Objects.toString(config.getPrefix(), "").trim().replaceAll("^/+|/+$", "");
    if (!prefix.matches("[a-zA-Z0-9_/-]*") || prefix.contains(".."))
      throw new BaseException("平台云存储对象前缀无效");
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put("provider", provider);
    settings.put("s3Endpoint", endpoint);
    settings.put("s3Bucket", config.getBucketName().trim());
    settings.put("s3AccessKey", config.getAccessKey());
    settings.put("s3SecretKey", config.getSecretKey());
    settings.put("s3Region", blank(config.getRegion()) ? "us-east-1" : config.getRegion());
    settings.put("s3Prefix", prefix.isBlank() ? "video-sdk" : prefix + "/video-sdk");
    settings.put("s3PublicBaseUrl", base);
    settings.put("qiniuDomain", provider.equals("qiniu") ? base : "");
    return settings;
  }

  private static boolean blank(String value) { return value == null || value.isBlank(); }

  private static String httpUrl(Object value, String label) {
    String text = Objects.toString(value, "").trim();
    try {
      URI uri = URI.create(text);
      if (!Set.of("http", "https").contains(Objects.toString(uri.getScheme(), "")) || uri.getHost() == null
          || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
          || uri.getPort() == 0 || uri.getPort() > 65535) throw new IllegalArgumentException();
      return text.replaceAll("/+$", "");
    } catch (IllegalArgumentException ex) { throw new BaseException(label + "必须是有效的 HTTP(S) 地址，且不能包含凭证、查询参数或片段"); }
  }

  private Map<String, Object> call(VideoPlatformInstance instance, Method method, Map<String, Object> body) {
    return call(instance, method, body, "/sdk/storage/config");
  }

  private Map<String, Object> call(VideoPlatformInstance instance, Method method, Map<String, Object> body, String path) {
    if (managementToken == null || managementToken.isBlank()) throw new BaseException("主后端尚未配置 SDK 存储管理凭证");
    String url = endpoint(instance) + path;
    try {
      HttpRequest request = HttpRequest.of(url).method(method).timeout(15000)
          .header("X-NexIoT-Storage-Token", managementToken);
      if (body != null) request.body(mapper.writeValueAsString(body));
      try (var response = request.execute()) {
        var json = mapper.readTree(response.body());
        if (!response.isOk() || !json.path("success").asBoolean()) {
          if ("INVALID_REQUEST".equals(json.path("code").asText()))
            throw new BaseException(json.path("message").asText("网关存储配置参数无效"));
          throw new BaseException("SDK 网关存储配置失败，请检查管理凭证和配置参数");
        }
        Map<String, Object> data = mapper.convertValue(json.path("data"), new TypeReference<>() {});
        data.put("s3AccessKey", ""); data.put("s3SecretKey", "");
        return data;
      }
    } catch (BaseException ex) { throw ex; }
    catch (Exception ex) { throw new BaseException("无法访问 SDK 网关存储配置接口"); }
  }

  private String endpoint(VideoPlatformInstance instance) {
    if (!cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType())) throw new BaseException("此实例不支持 SDK 网关存储");
    String endpoint = instance.getEndpoint();
    try {
      if (instance.getOptions() != null) {
        var options = mapper.readTree(instance.getOptions());
        for (String key : List.of("gatewayBaseUrl", "gatewayEndpoint", "baseUrl")) {
          if (!options.path(key).asText().isBlank()) { endpoint = options.path(key).asText(); break; }
        }
      }
      URI uri = URI.create(endpoint);
      if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
        throw new IllegalArgumentException();
      return endpoint.replaceAll("/+$", "");
    } catch (Exception ex) { throw new BaseException("SDK 网关地址无效"); }
  }
}
