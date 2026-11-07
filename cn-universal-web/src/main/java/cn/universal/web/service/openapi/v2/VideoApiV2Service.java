package cn.universal.web.service.openapi.v2;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.video.service.OpenApiVideoScope;
import cn.universal.admin.video.service.VideoOpenApiSessionService;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.video.VideoSdkPlatforms;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.web.context.TtlAuthContextHolder;
import cn.universal.web.dto.openapi.v2.VideoApiV2Models;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Adapts the existing video/thing-model service without bypassing its resource policy.
 *
 * <p>The access service establishes the current OAuth application and IoT grant first. Only after
 * checking every video resource do we install a short-lived, exact-channel OpenApiVideoScope. Async
 * native request IDs are stored under application-owned opaque handles; they are never accepted
 * directly from callers or returned in public payloads.
 */
@Service
@RequiredArgsConstructor
public class VideoApiV2Service {
  private static final Set<String> CAPABILITIES =
      Set.of(
          "snapshot",
          "preview",
          "ptz",
          "recordingSearch",
          "playback",
          "playbackStreamStart",
          "playbackControl",
          "audio",
          "talk");
  private final OpenApiV2Access access;
  private final IoTProductDeviceService products;
  private final VideoPlatformInstanceService videos;
  private final VideoPlatformDeviceMapper videoDevices;
  private final VideoPlatformChannelMapper videoChannels;
  private final VideoOpenApiSessionService sessions;

  public List<VideoApiV2Models.Channel> channels(String productKey, String deviceId) {
    Authorized resource = authorize(productKey, deviceId, null, Set.of(), true);
    return videoChannels
        .select(
            VideoPlatformChannel.builder()
                .instanceKey(resource.key())
                .deviceId(resource.videoDeviceId())
                .build())
        .stream()
        .filter(channel -> Objects.equals(resource.instance().getTenantId(), channel.getTenantId()))
        .map(
            channel ->
                new VideoApiV2Models.Channel(
                    channel.getChannelId(),
                    channel.getChannelName(),
                    channel.getChannelStatus(),
                    channel.getEnabled(),
                    capabilities(channel.getCapabilities())))
        .toList();
  }

  public VideoApiV2Models.Snapshot snapshot(
      String productKey, String deviceId, String channelId, VideoApiV2Models.SnapshotRequest body) {
    var request = body == null ? new VideoApiV2Models.SnapshotRequest(null, null) : body;
    if (!Set.of("device", "platform_frame").contains(request.sourceOrDefault())
        || !Set.of("main", "sub").contains(request.streamOrDefault()))
      throw status(HttpStatus.BAD_REQUEST, "抓图来源或码流类型无效");
    Authorized resource =
        authorize(productKey, deviceId, channelId, Set.of("snapshot", "preview"), false);
    Map<String, Object> result =
        OpenApiVideoScope.call(
            resource.scope(),
            () ->
                videos.captureSnapshot(
                    resource.key(),
                    deviceId,
                    channelId,
                    Map.of(
                        "source",
                        request.sourceOrDefault(),
                        "streamType",
                        request.streamOrDefault(),
                        "failOnOssUpload",
                        true)));
    if (result == null || result.isEmpty()) throw status(HttpStatus.BAD_GATEWAY, "视频服务未返回图片或抓图任务");
    String nativeId = text(result.get("requestId"));
    if (nativeId == null) return project(result, null);
    if (!"hik_isup".equals(resource.instance().getPlatformType()))
      throw status(HttpStatus.BAD_GATEWAY, "当前视频平台未提供可查询的异步抓图能力");
    String handle = null;
    try {
      Map<String, Object> stored = new LinkedHashMap<>();
      stored.put("nativeRequestId", nativeId);
      for (String key : List.of("provider", "imageSource"))
        if (result.get(key) instanceof String value) stored.put(key, value);
      handle =
          sessions.create(
              resource.owner(), resource.instance(), deviceId, channelId, "snapshot", stored);
      return project(result, handle);
    } catch (RuntimeException failure) {
      // The native job already exists. Cancel it if publishing its handle failed.
      try {
        OpenApiVideoScope.call(
            resource.operation("snapshotCancel"),
            () ->
                videos.executePlatformOperation(
                    resource.key(),
                    "snapshotCancel",
                    Map.of("deviceId", deviceId, "channelId", channelId, "requestId", nativeId)));
      } catch (RuntimeException cleanup) {
        if (cleanup != failure) failure.addSuppressed(cleanup);
      }
      if (handle != null) {
        try {
          sessions.remove(handle, resource.owner());
        } catch (RuntimeException cleanup) {
          if (cleanup != failure) failure.addSuppressed(cleanup);
        }
      }
      throw failure;
    }
  }

  public VideoApiV2Models.Snapshot snapshotStatus(
      String productKey, String deviceId, String channelId, String requestId, boolean cancel) {
    if (requestId == null || !requestId.matches("[a-f0-9]{32}"))
      throw status(HttpStatus.BAD_REQUEST, "请使用 V2 抓图接口返回的 requestId");
    String operation = cancel ? "snapshotCancel" : "snapshotStatus";
    Authorized resource = authorize(productKey, deviceId, channelId, Set.of(operation), true);
    Map<String, Object> record = sessions.require(requestId, resource.owner());
    if (!"snapshot".equals(record.get("kind"))
        || !Objects.equals(resource.key(), record.get("instanceKey"))
        || !Objects.equals(resource.instance().getTenantId(), record.get("tenantId"))
        || !Objects.equals(deviceId, record.get("deviceId"))
        || !Objects.equals(channelId, record.get("channelId")))
      throw new AccessDeniedException("抓图任务不属于当前应用、设备或通道");
    Map<String, Object> stored = VideoOpenApiSessionService.stream(record);
    String nativeId = text(stored.get("nativeRequestId"));
    if (nativeId == null) throw status(HttpStatus.CONFLICT, "抓图任务已不可用，请重新发起");
    Map<String, Object> result =
        OpenApiVideoScope.call(
            resource.scope(),
            () ->
                videos.executePlatformOperation(
                    resource.key(),
                    operation,
                    Map.of("deviceId", deviceId, "channelId", channelId, "requestId", nativeId)));
    if (result == null || result.isEmpty()) throw status(HttpStatus.BAD_GATEWAY, "视频服务未返回抓图状态");
    Map<String, Object> merged = new LinkedHashMap<>(result);
    for (String key : List.of("provider", "imageSource"))
      if (stored.get(key) != null) merged.putIfAbsent(key, stored.get(key));
    return project(merged, requestId);
  }

  private Authorized authorize(
      String productKey,
      String deviceId,
      String channelId,
      Set<String> operations,
      boolean allowDisabled) {
    OpenApiV2Access.identifier(productKey, "productKey");
    OpenApiV2Access.identifier(deviceId, "deviceId");
    if (channelId != null) OpenApiV2Access.identifier(channelId, "channelId");
    IoTUserApplication application = access.application();
    access.device(productKey, deviceId);
    String tenant = access.tenantId(application);
    IoTProduct product = products.getProduct(productKey);
    if (product == null || !Objects.equals(tenant, product.getTenantId()))
      throw new AccessDeniedException("视频产品租户不匹配");
    String instanceKey = productKey;
    if (StrUtil.isNotBlank(product.getThirdConfiguration())) {
      try {
        instanceKey =
            JSONUtil.parseObj(product.getThirdConfiguration()).getStr("instanceKey", productKey);
      } catch (RuntimeException invalid) {
        throw status(HttpStatus.CONFLICT, "视频产品实例配置无效");
      }
    }
    VideoPlatformInstance instance = videos.getByInstanceKey(instanceKey);
    if (instance == null
        || !Objects.equals(tenant, instance.getTenantId())
        || !Objects.equals(product.getThirdPlatform(), instance.getPlatformType())
        || !(VideoSdkPlatforms.managed(instance.getPlatformType())
            || "wvp".equals(instance.getPlatformType())))
      throw new AccessDeniedException("设备未关联可访问的视频实例");
    VideoPlatformDevice video =
        videoDevices.selectOne(
            VideoPlatformDevice.builder().instanceKey(instanceKey).deviceId(deviceId).build());
    if (video == null || !Objects.equals(tenant, video.getTenantId()))
      throw new AccessDeniedException("视频设备不存在或无权访问");
    if (!allowDisabled
        && (!Integer.valueOf(1).equals(instance.getEnabled())
            || Integer.valueOf(0).equals(video.getEnabled())))
      throw status(HttpStatus.CONFLICT, "视频实例或设备已停用");
    if (channelId != null) {
      VideoPlatformChannel channel =
          videoChannels.selectOne(
              VideoPlatformChannel.builder()
                  .instanceKey(instanceKey)
                  .deviceId(video.getId())
                  .channelId(channelId)
                  .build());
      if (channel == null || !Objects.equals(tenant, channel.getTenantId()))
        throw new AccessDeniedException("视频通道不存在或无权访问");
      if (!allowDisabled && Integer.valueOf(0).equals(channel.getEnabled()))
        throw status(HttpStatus.CONFLICT, "视频通道已停用");
    }
    String owner =
        DigestUtil.sha256Hex(
            JSONUtil.toJsonStr(
                List.of(
                    tenant,
                    application.getAppUniqueId(),
                    productKey,
                    deviceId,
                    Objects.toString(channelId, ""),
                    "snapshot:v2")));
    String subject = TtlAuthContextHolder.getInstance().getContext();
    OpenApiVideoScope.Scope scope =
        operations.isEmpty()
            ? null
            : new OpenApiVideoScope.Scope(
                subject,
                tenant,
                application.getAppUniqueId(),
                instanceKey,
                deviceId,
                channelId,
                operations);
    return new Authorized(instance, video.getId(), deviceId, channelId, owner, scope);
  }

  /** Strict projection: only a completed image can expose a URL; never forward raw SDK fields. */
  static VideoApiV2Models.Snapshot project(Map<String, Object> source, String handle) {
    Map<String, Object> fields = new LinkedHashMap<>(source);
    if (source.get("image") instanceof Map<?, ?> image) {
      for (String key :
          List.of(
              "snapshotUrl",
              "imageUrl",
              "url",
              "mimeType",
              "contentType",
              "size",
              "capturedAt",
              "imageSource",
              "width",
              "height")) if (image.get(key) != null) fields.putIfAbsent(key, image.get(key));
    }
    String url = null;
    for (String key : List.of("snapshotUrl", "imageUrl", "url", "snapshotOssUrl", "ossUrl")) {
      String candidate = text(fields.get(key));
      if (httpUrl(candidate)) {
        url = candidate;
        break;
      }
    }
    String upstream = Objects.toString(source.get("status"), "").toLowerCase(Locale.ROOT);
    String state =
        switch (upstream) {
          case "completed", "ready", "success", "succeeded" -> "completed";
          case "pending", "waiting_upload", "accepted", "queued", "processing", "running" ->
              "pending";
          case "cancelled", "canceled" -> "cancelled";
          case "expired", "timeout", "timed_out" -> "expired";
          case "failed", "error" -> "failed";
          case "" -> url != null ? "completed" : handle != null ? "pending" : "failed";
          default -> "failed";
        };
    boolean accepted = !Boolean.FALSE.equals(source.get("accepted"));
    String error = safeCode(source.get("errorCode"));
    if ("completed".equals(state) && url == null) {
      state = "failed";
      error = "SNAPSHOT_IMAGE_UNAVAILABLE";
    }
    if ("pending".equals(state) && handle == null) {
      state = "failed";
      error = "SNAPSHOT_TASK_UNAVAILABLE";
    }
    if (!accepted) {
      state = "failed";
      error = "SNAPSHOT_REJECTED";
    }
    if ("failed".equals(state) && error == null) error = "SNAPSHOT_FAILED";
    if ("expired".equals(state) && error == null) error = "SNAPSHOT_EXPIRED";
    boolean completed = "completed".equals(state);
    boolean pending = "pending".equals(state);
    String mimeType = text(fields.get("mimeType"));
    if (mimeType == null) mimeType = text(fields.get("contentType"));
    String message =
        switch (state) {
          case "completed" -> "抓图已完成";
          case "pending" -> "抓图已受理，请使用 requestId 查询结果";
          case "cancelled" -> "抓图已取消";
          case "expired" -> "等待图片超时，请重新发起或选择平台画面抓图";
          default -> "抓图未完成，请核对设备状态或选择平台画面抓图";
        };
    return new VideoApiV2Models.Snapshot(
        state,
        accepted,
        pending,
        handle,
        completed ? url : null,
        text(fields.get("imageSource")),
        text(fields.get("provider")),
        booleanValue(fields.get("fallback")),
        completed ? mimeType : null,
        completed ? positiveInt(fields.get("width")) : null,
        completed ? positiveInt(fields.get("height")) : null,
        completed ? nonnegativeLong(fields.get("size")) : null,
        completed ? timestamp(fields.get("capturedAt")) : null,
        error,
        message);
  }

  private static Map<String, Object> capabilities(String json) {
    Map<String, Object> safe = new LinkedHashMap<>();
    if (StrUtil.isBlank(json)) return safe;
    try {
      var object = JSONUtil.parseObj(json);
      for (String key : CAPABILITIES) {
        Object value = object.get(key);
        if (value instanceof Boolean
            || value instanceof String text
                && Set.of("supported", "unsupported", "unknown").contains(text))
          safe.put(key, value);
        else if (value instanceof Map<?, ?> map) {
          Map<String, Object> observation = new LinkedHashMap<>();
          for (String field : List.of("support", "supported", "source", "checkedAt"))
            if (map.get(field) instanceof String
                || map.get(field) instanceof Boolean
                || map.get(field) instanceof Number) observation.put(field, map.get(field));
          if (!observation.isEmpty()) safe.put(key, observation);
        }
      }
    } catch (RuntimeException ignored) {
      /* Corrupt optional metadata cannot widen access. */
    }
    return safe;
  }

  private static boolean httpUrl(String value) {
    if (value == null) return false;
    try {
      URI uri = URI.create(value);
      return uri.getHost() != null
          && uri.getUserInfo() == null
          && Set.of("https", "http").contains(uri.getScheme());
    } catch (IllegalArgumentException invalid) {
      return false;
    }
  }

  private static String text(Object value) {
    return value instanceof String string && !string.isBlank() ? string : null;
  }

  private static String safeCode(Object value) {
    String code = text(value);
    return code != null && code.matches("[A-Za-z0-9_-]{1,100}") ? code : null;
  }

  private static Boolean booleanValue(Object value) {
    return value instanceof Boolean bool ? bool : null;
  }

  private static Long nonnegativeLong(Object value) {
    if (value instanceof Number number && number.longValue() >= 0) return number.longValue();
    return null;
  }

  private static Integer positiveInt(Object value) {
    Long number = nonnegativeLong(value);
    return number != null && number > 0 && number <= Integer.MAX_VALUE ? number.intValue() : null;
  }

  private static Long timestamp(Object value) {
    if (value instanceof Number) return nonnegativeLong(value);
    if (value instanceof String string) {
      try {
        return Instant.parse(string).toEpochMilli();
      } catch (RuntimeException invalid) {
        return null;
      }
    }
    return null;
  }

  private static ResponseStatusException status(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }

  private record Authorized(
      VideoPlatformInstance instance,
      Long videoDeviceId,
      String deviceId,
      String channelId,
      String owner,
      OpenApiVideoScope.Scope scope) {
    String key() {
      return instance.getInstanceKey();
    }

    OpenApiVideoScope.Scope operation(String operation) {
      return new OpenApiVideoScope.Scope(
          scope.subject(),
          scope.tenantId(),
          scope.applicationId(),
          key(),
          deviceId,
          channelId,
          Set.of(operation));
    }
  }
}
