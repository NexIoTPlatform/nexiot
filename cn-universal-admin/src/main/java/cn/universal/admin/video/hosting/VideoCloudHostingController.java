package cn.universal.admin.video.hosting;

import cn.universal.common.domain.R;
import cn.universal.core.video.hosting.VideoCloudHostingProvider;
import cn.universal.core.video.hosting.VideoCloudHostingConfigProvider;
import cn.universal.security.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端托管会话及厂商回调入口。回调不携带登录态，依靠一次性 traceability 绑定会话。 */
@RestController
@Tag(name = "视频设备托管")
public class VideoCloudHostingController extends BaseController {
  private final VideoCloudHostingService service;
  private final List<VideoCloudHostingConfigProvider> configProviders;
  public VideoCloudHostingController(VideoCloudHostingService service,
      List<VideoCloudHostingConfigProvider> configProviders) {
    this.service = service;
    this.configProviders = configProviders == null ? List.of() : configProviders;
  }

  @GetMapping("/admin/v1/video/hosting/providers")
  @Operation(summary = "查询视频托管协议配置元数据")
  public List<VideoCloudHostingConfigProvider> providers() {
    return configProviders;
  }

  @PostMapping("/admin/v1/video/hosting/session")
  @Operation(summary = "创建视频设备托管会话")
  public R<Map<String,Object>> create(@RequestBody SessionRequest request) {
    if ("dahua_cloud".equals(request.platform())) throw new IllegalArgumentException("请先创建并选择大华云产品");
    return R.ok(service.create(request.platform(), request.authorizationUrl(), request.productKey()));
  }
  @GetMapping("/admin/v1/video/hosting/session/{sessionId}")
  public R<Map<String,Object>> get(@PathVariable String sessionId) { return R.ok(service.get(sessionId)); }
  @PostMapping("/admin/v1/video/hosting/session/{sessionId}/cancel")
  public R<Map<String,Object>> cancel(@PathVariable String sessionId) { return R.ok(service.cancel(sessionId)); }

  @GetMapping("/admin/v1/video/hosting/devices")
  @Operation(summary = "查询托管设备", description = "查询乐橙/萤石在平台侧的待接受、已接受和已拒绝托管设备")
  public R<List<Map<String, Object>>> devices(
      @RequestParam String platform,
      @RequestParam(required = false) String productKey,
      @RequestParam(defaultValue = "init") String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "100") int pageSize) {
    if ("dahua_cloud".equals(platform)) throw new IllegalArgumentException("请使用大华云产品托管页面");
    return R.ok(service.listHosted(platform, productKey, status, page, pageSize));
  }

  @PostMapping("/admin/v1/video/hosting/devices/accept")
  @Operation(summary = "接受托管设备", description = "接受厂商待确认托管设备并写入当前用户设备库")
  public R<Map<String, Object>> accept(@RequestBody AcceptRequest request) {
    if ("dahua_cloud".equals(request.platform())) throw new IllegalArgumentException("请使用大华云产品托管页面");
    return R.ok(service.acceptHosted(request.platform(), request.productKey(), request.devices()));
  }

  @PostMapping("/admin/v1/video/hosting/ezviz/cancel")
  public R<Map<String, Object>> cancelEzviz(@RequestParam String productKey, @RequestParam String deviceId) {
    return R.ok(service.cancelEzvizDevice(productKey, deviceId));
  }

  @PostMapping("/admin/v1/video/hosting/ezviz/sync")
  public R<Map<String, Object>> syncEzviz(@RequestParam(required = false) String productKey) {
    return R.ok(service.syncAuthorized("ezviz", productKey));
  }

  @GetMapping({"/hosting/ezviz/callback", "/video/hosting/ezviz/callback"})
  public Map<String,String> ezvizCallback(@RequestParam Map<String,String> query) {
    service.handleQuery("ezviz", query);
    return Map.of("code", "200");
  }
  @PostMapping({"/hosting/ezviz/callback", "/video/hosting/ezviz/callback"})
  public Map<String,String> ezvizCallbackPost(@RequestParam Map<String,String> query) {
    return ezvizCallback(query);
  }
  @PostMapping("/hosting/imoulife/callback")
  public Map<String,String> imouCallback(@RequestBody(required = false) String body, @RequestParam Map<String,String> query) {
    VideoCloudHostingProvider.HostingCallback callback = service.parseCallback("imoulife", query, body);
    service.callback(requireTrace(callback), "imoulife", callback); return Map.of("code", "200");
  }
  @GetMapping("/hosting/imoulife/callback")
  public Map<String,String> imouCallbackGet(@RequestParam Map<String,String> query) {
    VideoCloudHostingProvider.HostingCallback callback = service.parseCallback("imoulife", query, null);
    service.callback(requireTrace(callback), "imoulife", callback); return Map.of("code", "200");
  }
  private String requireTrace(VideoCloudHostingProvider.HostingCallback callback) {
    if (callback == null || callback.traceability() == null || callback.traceability().isBlank()) {
      throw new IllegalArgumentException("托管回调缺少会话标识");
    }
    return callback.traceability();
  }
  // productKey 指向用户已授权的本地产品；authorizationUrl 只是旧页面的可选地址兜底。
  public record SessionRequest(String platform, String authorizationUrl, String productKey) {
    public SessionRequest(String platform, String authorizationUrl) { this(platform, authorizationUrl, null); }
  }
  // devices 是厂商设备/通道引用，实际租户和产品权限在服务端重新校验。
  public record AcceptRequest(String platform, String productKey, List<VideoCloudHostingProvider.DeviceRef> devices) {
    public AcceptRequest(String platform, List<VideoCloudHostingProvider.DeviceRef> devices) { this(platform, null, devices); }
  }
}
