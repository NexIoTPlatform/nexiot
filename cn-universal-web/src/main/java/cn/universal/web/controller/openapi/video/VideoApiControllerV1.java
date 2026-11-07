package cn.universal.web.controller.openapi.video;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.video.service.OpenApiVideoScope;
import cn.universal.admin.video.service.VideoOpenApiSessionService;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.admin.video.service.VideoPlaybackTicketService;
import cn.universal.common.domain.R;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.video.VideoMediaUrlResolver;
import cn.universal.dm.video.VideoSdkPlatforms;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.query.IoTAPIQuery;
import cn.universal.web.context.TtlAuthContextHolder;
import cn.universal.web.controller.common.BaseApiController;
import cn.universal.web.dto.openapi.video.VideoApiRequests;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Business-facing API over existing video services. No native SDK or media credentials are accepted. */
@RestController
@RequestMapping("/api/openapi/v1/video/devices/{productKey}/{deviceId}")
@Tag(name = "视频开放API", description = "统一预览、录像查询/回放、抓图和临时iframe播放，时间统一为Unix毫秒")
@SecurityRequirement(name = "Authorization")
public class VideoApiControllerV1 extends BaseApiController {
  private static final String CHANNEL = "/channels/{channelId}";
  private static final Set<String> URL_FIELDS = Set.of("flv", "http_flv", "https_flv", "ws_flv", "wss_flv",
      "hls", "http_hls", "https_hls", "ws_hls", "wss_hls", "fmp4", "https_fmp4", "ws_fmp4", "wss_fmp4",
      "ts", "https_ts", "ws_ts", "wss_ts", "rtc", "rtcs", "webrtc", "preferredUrl");
  private static final Set<String> PUBLIC_FIELDS = Set.of("status", "playerType", "audioSupported",
      "playbackControlSupported", "startTime", "endTime", "streamType", "source", "snapshotUrl", "imageUrl",
      "url", "requestId", "jobId", "accepted", "pending", "supported", "capability", "width", "height",
      "expiresAt", "capturedAt", "provider", "stopped", "skipped", "progress", "playbackRate", "seekMode",
      "imageSource", "fallback", "mimeType", "size", "releaseMode", "upstreamStopped");

  @Resource private VideoPlatformInstanceService videos;
  @Resource private IoTProductDeviceService products;
  @Resource private VideoPlatformDeviceMapper videoDevices;
  @Resource private VideoPlatformChannelMapper channels;
  @Resource private VideoOpenApiSessionService sessions;
  @Resource private VideoPlaybackTicketService tickets;
  @Resource private VideoMediaUrlResolver mediaUrls;

  @Operation(summary = "查询设备视频通道", description = "限定在当前应用已绑定的IoT设备，返回可用通道和能力")
  @GetMapping("/channels")
  public R<List<Map<String, Object>>> channels(@PathVariable String productKey, @PathVariable String deviceId) {
    Authorized resource = authorize(productKey, deviceId, null, Set.of(), true);
    List<VideoPlatformChannel> rows = channels.select(VideoPlatformChannel.builder()
        .instanceKey(resource.instance().getInstanceKey()).deviceId(resource.videoDeviceId()).build());
    return R.ok(rows.stream().filter(row -> Objects.equals(row.getTenantId(), resource.instance().getTenantId()))
        .map(row -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("channelId", row.getChannelId()); item.put("channelName", row.getChannelName());
          item.put("status", row.getChannelStatus()); item.put("enabled", row.getEnabled());
          item.put("capabilities", JSONUtil.isTypeJSON(row.getCapabilities()) ? JSONUtil.parse(row.getCapabilities()) : Map.of());
          return item;
        }).toList());
  }

  @Operation(summary = "创建实时预览", description = "返回播放流、sessionId和临时playerUrl/embedUrl；播放器每15秒维持会话")
  @PostMapping(CHANNEL + "/preview")
  public R<Map<String, Object>> preview(@PathVariable String productKey, @PathVariable String deviceId,
      @PathVariable String channelId, @Valid @RequestBody(required = false) VideoApiRequests.Preview body) {
    var request = body == null ? new VideoApiRequests.Preview(null, null) : body;
    Authorized resource = authorize(productKey, deviceId, channelId, Set.of("preview"), false);
    Map<String, Object> stream = OpenApiVideoScope.call(resource.scope(), () -> new LinkedHashMap<>(
        videos.getStreamUrl(resource.key(), deviceId, channelId, request.stream())));
    stream.put("streamType", request.stream());
    return R.ok(open(resource, "preview", stream, request.ttl()));
  }

  @Operation(summary = "查询设备录像", description = "一次最多查询24小时；录像能力和存储来源由设备决定")
  @PostMapping(CHANNEL + "/recordings/search")
  public R<List<Map<String, Object>>> recordings(@PathVariable String productKey, @PathVariable String deviceId,
      @PathVariable String channelId, @Valid @RequestBody VideoApiRequests.RecordingRange body) {
    range(body, 86_400_000L);
    Authorized resource = authorize(productKey, deviceId, channelId, Set.of("recordingSearch"), false);
    return R.ok(OpenApiVideoScope.call(resource.scope(), () -> videos.queryRecords(resource.key(), deviceId,
        channelId, body.startTime(), body.endTime(), Map.of("streamType", body.stream()))));
  }

  @Operation(summary = "创建录像回放", description = "一次回放1秒至2小时，统一通过已有媒体服务；返回可嵌入的临时播放器")
  @PostMapping(CHANNEL + "/playback")
  public R<Map<String, Object>> playback(@PathVariable String productKey, @PathVariable String deviceId,
      @PathVariable String channelId, @Valid @RequestBody VideoApiRequests.RecordingRange body) {
    range(body, 7_200_000L);
    Authorized resource = authorize(productKey, deviceId, channelId, Set.of("playbackStreamStart"), false);
    Map<String, Object> stream = OpenApiVideoScope.call(resource.scope(), () -> {
      if (VideoSdkPlatforms.managed(resource.instance().getPlatformType())) {
        return videos.executePlatformOperation(resource.key(), "playbackStreamStart", Map.of(
            "deviceId", deviceId, "channelId", channelId, "startTime", body.startTime(),
            "endTime", body.endTime(), "streamType", body.stream()));
      }
      return videos.getPlaybackUrl(resource.key(), deviceId, channelId, body.startTime(), body.endTime(),
          Map.of("streamType", body.stream()));
    });
    return R.ok(open(resource, "playback", stream, body.ttl()));
  }

  @Operation(summary = "控制已创建的播放会话", description = "status维持预览/回放；stop释放；pause/resume/seek/speed按设备能力执行。只接受本应用本通道sessionId")
  @PostMapping(CHANNEL + "/sessions/{sessionId}/{action:status|stop|pause|resume|seek|speed}")
  public R<Map<String, Object>> control(@PathVariable String productKey, @PathVariable String deviceId,
      @PathVariable String channelId, @PathVariable String sessionId, @PathVariable String action,
      @Valid @RequestBody(required = false) VideoApiRequests.Control body) {
    String operation = switch (action) {
      case "stop" -> "playbackStreamStop"; case "pause" -> "playbackStreamPause";
      case "resume" -> "playbackStreamResume"; case "seek" -> "playbackStreamSeek";
      case "speed" -> "playbackStreamSpeed"; default -> "playbackStreamStatus";
    };
    Authorized resource = authorize(productKey, deviceId, channelId,
        Set.copyOf(List.of(operation, "stopPreview", "playbackStreamStatus")), true);
    Map<String, Object> record = sessions.require(sessionId, resource.owner());
    Map<String, Object> stream = VideoOpenApiSessionService.stream(record);
    Map<String, Object> result = OpenApiVideoScope.call(resource.scope(), () -> {
      if ("preview".equals(record.get("kind"))) {
        if ("status".equals(action)) { sessions.heartbeat(sessionId); return Map.of("status", "active"); }
        if (!"stop".equals(action)) throw badRequest("预览仅支持status和stop");
        if ("wvp".equals(resource.instance().getPlatformType()))
          return Map.of("stopped", true, "upstreamStopped", false, "releaseMode", "viewer_only");
        return videos.stopPreview(resource.key(), deviceId, channelId, stringOptions(stream));
      }
      Map<String, Object> input = new LinkedHashMap<>();
      input.put("deviceId", deviceId); input.put("channelId", channelId);
      if (stream.get("ownerNode") != null) input.put("ownerNode", stream.get("ownerNode"));
      if ("seek".equals(action)) {
        if (body == null || body.seekTime() == null) throw badRequest("seek缺少seekTime");
        input.put("seekTime", body.seekTime());
      }
      if ("speed".equals(action)) {
        if (body == null || body.speed() == null) throw badRequest("speed缺少speed");
        input.put("playbackRate", body.speed());
      }
      if ("pause".equals(action)) input.put("paused", true);
      if (stream.get("playbackId") != null) {
        input.put("playbackId", stream.get("playbackId"));
        return videos.executePlatformOperation(resource.key(), operation, input);
      }
      if ("status".equals(action)) return Map.of("status", "active");
      if ("seek".equals(action)) throw badRequest("当前国标回放请重新创建指定时段回放");
      videos.controlPlayback(resource.key(), deviceId, channelId, Objects.toString(stream.get("stream"), ""),
          action, body == null ? null : body.speed());
      return Map.of("status", "stop".equals(action) ? "stopped" : action);
    });
    if ("stop".equals(action)) {
      if (Boolean.FALSE.equals(result.get("stopped")) || Boolean.TRUE.equals(result.get("skipped")))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "媒体会话尚未释放，可使用当前sessionId重试");
      sessions.remove(sessionId, resource.owner());
      Object ticket = stream.get("openApiPlayerToken");
      if (ticket != null) tickets.revoke(ticket.toString(), resource.owner());
    } else if (result.get("previousPlaybackId") != null) {
      result = new LinkedHashMap<>(result);
      result.put("openApiSessionId", sessionId);
      result.put("playback", true);
      Object previousTicket = stream.get("openApiPlayerToken");
      Object nextTicket = null;
      try {
        if (previousTicket != null) tickets.revoke(previousTicket.toString(), resource.owner());
        Map<String, Object> ticketResult = tickets.create(result, resource.owner(), 300);
        nextTicket = ticketResult.get("token");
        result.put("openApiPlayerToken", nextTicket);
        sessions.update(sessionId, resource.owner(), result);
        Map<String, Object> response = publicStream(result);
        response.putAll(ticketResult); response.put("sessionId", sessionId);
        return R.ok(response);
      } catch (RuntimeException failure) {
        // Seek already replaced the old native playback. Only the new identity may be released.
        releaseAcquiredStream(resource, "playback", result, failure);
        discardPublication(resource, sessionId, nextTicket, failure);
        throw failure;
      }
    }
    Map<String, Object> response = publicStream(result);
    response.put("sessionId", sessionId);
    return R.ok(response);
  }

  @Operation(summary = "抓图", description = "优先设备原生抓图；异步设备返回requestId，可继续查询任务状态")
  @PostMapping(CHANNEL + "/snapshots")
  public R<Map<String, Object>> snapshot(@PathVariable String productKey, @PathVariable String deviceId,
      @PathVariable String channelId, @Valid @RequestBody(required = false) VideoApiRequests.Snapshot body) {
    Authorized resource = authorize(productKey, deviceId, channelId, Set.of("snapshot", "preview"), false);
    String source = body == null || body.source() == null ? "device" : body.source();
    return R.ok(publicStream(OpenApiVideoScope.call(resource.scope(), () -> videos.captureSnapshot(
        resource.key(), deviceId, channelId, Map.of("source", source)))));
  }

  @Operation(summary = "查询异步抓图状态")
  @GetMapping(CHANNEL + "/snapshots/{requestId}")
  public R<Map<String, Object>> snapshotStatus(@PathVariable String productKey, @PathVariable String deviceId,
      @PathVariable String channelId, @PathVariable String requestId) {
    Authorized resource = authorize(productKey, deviceId, channelId, Set.of("snapshotStatus"), true);
    return R.ok(publicStream(OpenApiVideoScope.call(resource.scope(), () -> videos.executePlatformOperation(
        resource.key(), "snapshotStatus", Map.of("deviceId", deviceId, "channelId", channelId, "requestId", requestId)))));
  }

  @Operation(summary = "云台控制")
  @PostMapping(CHANNEL + "/ptz")
  public R<Void> ptz(@PathVariable String productKey, @PathVariable String deviceId,
      @PathVariable String channelId, @Valid @RequestBody VideoApiRequests.Ptz body) {
    Authorized resource = authorize(productKey, deviceId, channelId, Set.of("ptz"), false);
    OpenApiVideoScope.call(resource.scope(), () -> { videos.controlPTZ(resource.key(), deviceId, channelId,
        body.command(), body.speed() == null ? 50 : body.speed()); return null; });
    return R.ok();
  }

  @Operation(summary = "撤销播放器票据", description = "立即禁止该票据继续获取播放上下文；释放媒体会话请调用session的stop")
  @DeleteMapping(CHANNEL + "/player-tickets/{token}")
  public R<Void> revoke(@PathVariable String productKey, @PathVariable String deviceId,
      @PathVariable String channelId, @PathVariable String token) {
    Authorized resource = authorize(productKey, deviceId, channelId, Set.of(), true);
    tickets.revoke(token, resource.owner());
    return R.ok();
  }

  protected Authorized authorize(String productKey, String deviceId, String channelId, Set<String> operations,
      boolean allowDisabled) {
    if (StrUtil.hasBlank(productKey, deviceId) || productKey.length() > 100 || deviceId.length() > 100
        || channelId != null && (channelId.isBlank() || channelId.length() > 100)) throw badRequest("设备或通道标识无效");
    String owner = iotUnionId(), tenant = currentTenantId(), app = iotApplicationId();
    if (StrUtil.hasBlank(owner, tenant)) throw new AccessDeniedException("无法确认调用方租户");
    IoTUserApplication application = currentIotApplication();
    if (application == null) throw new AccessDeniedException("请使用已授权应用的client_credentials令牌调用视频开放接口");
    if (StrUtil.isBlank(app) || Integer.valueOf(1).equals(application.getAppStatus())
        || Integer.valueOf(1).equals(application.getDeleted())
        || application.getValidEndDate() != null && application.getValidEndDate().getTime() <= System.currentTimeMillis())
      throw new AccessDeniedException("应用已停用或过期");
    if (!iotDeviceService.selectDevCount(IoTAPIQuery.builder().productKey(productKey).deviceId(deviceId)
        .iotUnionId(owner).applicationId(app).build())) throw new AccessDeniedException("此应用没有设备权限");
    IoTDeviceDTO iot = iotDeviceService.selectDevInstanceBO(productKey, deviceId);
    IoTProduct product = products.getProduct(productKey);
    if (iot == null || product == null || !Objects.equals(tenant, iot.getTenantId())
        || !Objects.equals(tenant, product.getTenantId())) throw new AccessDeniedException("设备租户不匹配");
    String key = productKey;
    if (StrUtil.isNotBlank(product.getThirdConfiguration())) {
      key = JSONUtil.parseObj(product.getThirdConfiguration()).getStr("instanceKey", productKey);
    }
    VideoPlatformInstance instance = videos.getByInstanceKey(key);
    if (instance == null || !Objects.equals(tenant, instance.getTenantId())
        || !Objects.equals(product.getThirdPlatform(), instance.getPlatformType())
        || !(VideoSdkPlatforms.managed(instance.getPlatformType()) || "wvp".equals(instance.getPlatformType())))
      throw new AccessDeniedException("设备未关联可访问的视频实例");
    VideoPlatformDevice video = videoDevices.selectOne(VideoPlatformDevice.builder().instanceKey(key).deviceId(deviceId).build());
    if (video == null || !Objects.equals(tenant, video.getTenantId())) throw new AccessDeniedException("视频设备不存在或无权访问");
    if (!allowDisabled && (!Integer.valueOf(1).equals(instance.getEnabled()) || Integer.valueOf(0).equals(video.getEnabled())))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "视频实例或设备已停用");
    if (channelId != null) {
      VideoPlatformChannel channel = channels.selectOne(VideoPlatformChannel.builder().instanceKey(key)
          .deviceId(video.getId()).channelId(channelId).build());
      if (channel == null || !Objects.equals(tenant, channel.getTenantId())) throw new AccessDeniedException("通道不存在或无权访问");
      if (!allowDisabled && Integer.valueOf(0).equals(channel.getEnabled()))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "视频通道已停用");
    }
    String scopeOwner = DigestUtil.sha256Hex(JSONUtil.toJsonStr(List.of(tenant,
        app == null ? "user:" + owner : "app:" + app, productKey, deviceId, Objects.toString(channelId, ""))));
    String subject = TtlAuthContextHolder.getInstance().getContext();
    var scope = channelId == null || operations.isEmpty() ? null
        : new OpenApiVideoScope.Scope(subject, tenant, app, key, deviceId, channelId, operations);
    return new Authorized(instance, video.getId(), deviceId, channelId, scopeOwner, scope);
  }

  private Map<String, Object> open(Authorized resource, String kind, Map<String, Object> input, int ttl) {
    Map<String, Object> stream = new LinkedHashMap<>(input);
    String id = null;
    Object token = null;
    try {
      Map<String, Object> response = publicStream(stream);
      if (((Map<?, ?>) response.get("urls")).isEmpty() || Boolean.FALSE.equals(stream.get("available"))
          || Set.of("failed", "stopped", "expired", "ended").contains(Objects.toString(stream.get("status"), "")))
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "设备暂未返回浏览器可播放的视频流");
      id = sessions.create(resource.owner(), resource.instance(), resource.deviceId(), resource.channelId(), kind, stream);
      stream.put("openApiSessionId", id);
      stream.put("playback", "playback".equals(kind));
      Map<String, Object> ticket = tickets.create(stream, resource.owner(), ttl);
      token = ticket.get("token");
      stream.put("openApiPlayerToken", token);
      sessions.update(id, resource.owner(), stream);
      response.putAll(ticket); response.put("sessionId", id);
      response.put("deviceId", resource.deviceId()); response.put("channelId", resource.channelId());
      return response;
    } catch (RuntimeException failure) {
      releaseAcquiredStream(resource, kind, stream, failure);
      discardPublication(resource, id, token, failure);
      throw failure;
    }
  }

  private void releaseAcquiredStream(Authorized resource, String kind, Map<String, Object> stream,
      RuntimeException failure) {
    if ("preview".equals(kind) && "wvp".equals(resource.instance().getPlatformType())) return;
    try {
      String operation = "preview".equals(kind) ? "stopPreview" : "playbackStreamStop";
      var original = resource.scope();
      var cleanupScope = new OpenApiVideoScope.Scope(original.subject(), original.tenantId(),
          original.applicationId(), original.instanceKey(), original.deviceId(), original.channelId(),
          "preview".equals(kind) ? Set.of(operation) : Set.of(operation, "playbackStreamStatus"));
      OpenApiVideoScope.call(cleanupScope, () -> {
        if ("preview".equals(kind)) {
          Map<String, String> options = stringOptions(stream);
          // An empty stop filter could close another viewer's stream on the same channel.
          if (List.of("zlmStream", "stream", "streamKey").stream()
              .noneMatch(key -> StrUtil.isNotBlank(options.get(key)))) return null;
          videos.stopPreview(resource.key(), resource.deviceId(), resource.channelId(), options);
        } else if (StrUtil.isNotBlank(Objects.toString(stream.get("playbackId"), ""))) {
          Map<String, Object> input = new LinkedHashMap<>();
          input.put("deviceId", resource.deviceId()); input.put("channelId", resource.channelId());
          input.put("playbackId", stream.get("playbackId"));
          if (stream.get("ownerNode") != null) input.put("ownerNode", stream.get("ownerNode"));
          videos.executePlatformOperation(resource.key(), operation, input);
        } else if (StrUtil.isNotBlank(Objects.toString(stream.get("stream"), ""))) {
          videos.controlPlayback(resource.key(), resource.deviceId(), resource.channelId(),
              stream.get("stream").toString(), "stop", null);
        }
        return null;
      });
    } catch (RuntimeException cleanupFailure) {
      if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
    }
  }

  private void discardPublication(Authorized resource, String id, Object token, RuntimeException failure) {
    if (token != null) {
      try { tickets.revoke(token.toString(), resource.owner()); }
      catch (RuntimeException cleanupFailure) {
        if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
      }
    }
    if (id != null) {
      try { sessions.remove(id, resource.owner()); }
      catch (RuntimeException cleanupFailure) {
        if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
      }
    }
  }

  private Map<String, Object> publicStream(Map<String, Object> source) {
    source = new LinkedHashMap<>(source);
    if (source.get("image") instanceof Map<?, ?> image) {
      for (String key : List.of("snapshotUrl", "imageUrl", "url", "mimeType", "size", "capturedAt", "imageSource", "width", "height"))
        if (image.get(key) != null) source.putIfAbsent(key, image.get(key));
    }
    for (String nested : List.of("urls", "streamUrls")) {
      if (source.get(nested) instanceof Map<?, ?> values)
        for (String key : URL_FIELDS) if (values.get(key) != null) source.putIfAbsent(key, values.get(key));
    }
    Map<String, String> original = new LinkedHashMap<>();
    source.forEach((key, value) -> { if (URL_FIELDS.contains(key) && value instanceof String url && browserUrl(url)) original.put(key, url); });
    Map<String, String> urls = mediaUrls.publicUrls(original);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("urls", urls);
    source.forEach((key, value) -> {
      if (PUBLIC_FIELDS.contains(key) && value != null
          && (!Set.of("snapshotUrl", "imageUrl", "url").contains(key) || value instanceof String url && browserUrl(url)))
        result.put(key, value);
    });
    if (urls.get("preferredUrl") != null) result.put("preferredUrl", urls.get("preferredUrl"));
    return result;
  }

  private static Map<String, String> stringOptions(Map<String, Object> stream) {
    Map<String, String> options = new LinkedHashMap<>();
    for (String key : List.of("ownerNode", "previewId", "streamType", "mediaServerPrimaryId", "mediaServerId",
        "requestedMediaServerId", "zlmMediaServerId", "app", "zlmApp", "stream", "zlmStream", "streamKey"))
      if (stream.get(key) != null) options.put(key, stream.get(key).toString());
    return options;
  }

  private static boolean browserUrl(String url) {
    try { var uri = java.net.URI.create(url); return uri.getHost() != null && uri.getUserInfo() == null
        && Set.of("https", "http", "wss", "ws").contains(uri.getScheme()); }
    catch (IllegalArgumentException e) { return false; }
  }

  private static void range(VideoApiRequests.RecordingRange body, long max) {
    if (body.startTime() == null || body.endTime() == null || body.startTime() <= 0
        || body.endTime() <= body.startTime() || body.endTime() - body.startTime() < 1000
        || body.endTime() - body.startTime() > max) throw badRequest("录像时间范围无效或超过单次上限");
  }

  private static ResponseStatusException badRequest(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
  }

  protected record Authorized(VideoPlatformInstance instance, Long videoDeviceId, String deviceId, String channelId,
      String owner, OpenApiVideoScope.Scope scope) {
    String key() { return instance.getInstanceKey(); }
  }
}
