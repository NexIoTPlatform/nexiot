package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.zlm.VideoZlmStreamProxyService;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import jakarta.annotation.PreDestroy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** 大华主动注册原生预览与租户 ZLM 接收端共同拥有、续期和释放。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoDahuaPreviewService {
  private final VideoPlatformAdapterRegistry adapters;
  private final ObjectProvider<VideoZlmStreamProxyService> media;
  private final VideoPlatformInstanceMapper instances;
  private final VideoPlatformDeviceMapper devices;
  private final Map<String, Session> sessions = new java.util.concurrent.ConcurrentHashMap<>();
  @org.springframework.beans.factory.annotation.Value("${nexiot.video.backend-node-id:}") private String backendNode;
  private final String processNode = UUID.randomUUID().toString();
  private volatile boolean closing;
  private static final Set<String> TERMINAL = Set.of("failed", "stopped", "ended", "expired");
  private static class Session {
    final VideoPlatformInstance instance;
    final String id, device, channel, type, scope, node, stream;
    final Map<String, Object> request;
    Map<String, Object> state = Map.of("status", "starting");
    long touched = System.currentTimeMillis();
    boolean stopping, sdkClosed, mediaClosed;
    Session(VideoPlatformInstance instance, String id, String device, String channel, String type,
        String scope, Map<String, Object> request) {
      this.instance = instance; this.id = id; this.device = device; this.channel = channel;
      this.type = type; this.scope = scope; this.request = request;
      node = Objects.toString(request.get("mediaServerPrimaryId"), "");
      stream = Objects.toString(request.get("zlmStream"), "");
    }
  }

  public void assertOwner(Object node) {
    if (node != null && !ownerNode().equals(node))
      throw new BaseException("SESSION_OWNER_MISMATCH: 请将预览控制路由到创建会话的后端节点");
  }
  private String ownerNode() { return backendNode == null || backendNode.isBlank() ? processNode : backendNode; }

  public synchronized Map<String, String> start(VideoPlatformInstance instance, String device, String channel,
      String type, Map<String, Object> options, String scope, String preferredNode) {
    if (closing || !Integer.valueOf(1).equals(instance.getEnabled()) || !DahuaActiveRegistrationConfiguration.enabled(instance))
      throw new BaseException("大华主动注册实例已停用");
    VideoPlatformDevice saved = devices.selectOne(VideoPlatformDevice.builder()
        .instanceKey(instance.getInstanceKey()).deviceId(device).build());
    if (saved == null || !Integer.valueOf(1).equals(saved.getEnabled())
        || !Objects.equals(saved.getTenantId(), instance.getTenantId()))
      throw new BaseException("大华设备不存在、已停用或无权访问");
    String streamType = type == null || type.isBlank() ? "main" : type;
    if (!Set.of("main", "sub").contains(streamType)) throw new BaseException("码流仅支持主码流或辅码流");
    if (sessions.size() >= 128) throw new BaseException("大华预览并发已满，请关闭其他窗口");
    String id = UUID.randomUUID().toString();
    Map<String, Object> target = mediaService().openRtpPlayback(scope, preferredNode, "dahua-live-" + id);
    Map<String, Object> request = new LinkedHashMap<>(options);
    request.putAll(target); request.put("previewId", id);
    request.put("deviceId", device); request.put("channelId", channel); request.put("streamType", streamType);
    Session session = new Session(instance, id, device, channel, streamType, scope, request);
    sessions.put(id, session);
    try {
      Map<String, Object> nativeState = adapters.getAdapter("dahua_sdk")
          .executeOperation(instance, "previewStreamStart", request);
      if (TERMINAL.contains(Objects.toString(nativeState.get("status"), "").toLowerCase(java.util.Locale.ROOT)))
        throw new BaseException("大华原生预览启动失败");
      session.state = safeState(nativeState);
      Map<String, String> result = new LinkedHashMap<>();
      target.forEach((key, value) -> { if (value != null) result.put(key, String.valueOf(value)); });
      session.state.forEach((key, value) -> { if (value != null) result.put(key, String.valueOf(value)); });
      result.keySet().removeAll(Set.of("zlmHost", "zlmRtpPort", "zlmTransport", "ssrc", "psPayloadType", "h264PayloadType", "h265PayloadType"));
      result.put("previewId", id); result.put("ownerNode", ownerNode());
      result.put("streamSource", "gateway_dahua_registration"); result.put("streamMode", "sdk_zlm_rtp");
      result.put("requestedMediaServerId", session.node); result.put("mediaServerId", session.node);
      result.put("app", "rtp"); result.put("stream", session.stream); result.put("streamType", streamType);
      return result;
    } catch (RuntimeException e) { session.stopping = true; release(session); throw e; }
  }

  public synchronized Map<String, Object> stop(VideoPlatformInstance instance, String device, String channel, Map<String, String> options) {
    assertOwner(options == null ? null : options.get("ownerNode"));
    String stream = options == null ? null : options.getOrDefault("zlmStream", options.getOrDefault("stream", options.get("streamKey")));
    String previewId = options == null ? null : options.get("previewId");
    String type = options == null ? null : options.get("streamType");
    int count = 0;
    for (Session session : List.copyOf(sessions.values())) {
      if (!matches(session, instance, device, channel)
          || stream != null && !stream.isBlank() && !stream.equals(session.stream)
          || previewId != null && !previewId.isBlank() && !previewId.equals(session.id)
          || type != null && !type.isBlank() && !type.equals(session.type)) continue;
      session.stopping = true; release(session); count++;
    }
    return Map.of("stopped", true, "releasedSessions", count, "deviceId", device, "channelId", channel);
  }

  public synchronized Map<String, Object> heartbeat(VideoPlatformInstance instance, String device, String channel, String stream, Object previewId) {
    if (stream == null || stream.isBlank()) throw new BaseException("缺少大华预览流标识");
    for (Session session : sessions.values()) {
      if (!matches(session, instance, device, channel) || !stream.equals(session.stream)
          || previewId != null && !session.id.equals(previewId)) continue;
      if (session.stopping) return Map.of("active", false, "status", "stopped", "stream", stream);
      session.touched = System.currentTimeMillis();
      Map<String, Object> result = new LinkedHashMap<>(session.state);
      result.put("active", true); result.put("stream", stream); result.put("previewId", session.id); result.put("ownerNode", ownerNode());
      return result;
    }
    return Map.of("active", false, "status", "expired", "stream", stream);
  }

  private boolean matches(Session session, VideoPlatformInstance instance, String device, String channel) {
    return Objects.equals(session.instance.getInstanceKey(), instance.getInstanceKey())
        && Objects.equals(session.instance.getTenantId(), instance.getTenantId())
        && Objects.equals(session.device, device) && Objects.equals(session.channel, channel);
  }
  private Map<String, Object> safeState(Map<String, Object> nativeState) {
    Map<String, Object> state = new LinkedHashMap<>();
    for (String key : List.of("status", "message", "receivedBytes", "bytes", "lastDataAt"))
      if (nativeState.containsKey(key)) state.put(key, nativeState.get(key));
    state.putIfAbsent("status", "starting"); return state;
  }
  private VideoZlmStreamProxyService mediaService() {
    VideoZlmStreamProxyService service = media.getIfAvailable();
    if (service == null) throw new BaseException("尚未接入可用 ZLM 媒体服务"); return service;
  }
  private void release(Session session) {
    if (!session.sdkClosed) try {
      adapters.getAdapter("dahua_sdk").executeOperation(session.instance, "previewStreamStop", session.request);
      session.sdkClosed = true;
    } catch (RuntimeException e) { log.warn("[大华预览] 原生释放待重试 previewId={}", session.id); }
    if (!session.mediaClosed) try {
      mediaService().closeRtpPlayback(session.scope, session.node, session.stream); session.mediaClosed = true;
    } catch (RuntimeException e) { log.warn("[大华预览] 媒体释放待重试 previewId={}", session.id); }
    if (session.sdkClosed && session.mediaClosed) sessions.remove(session.id, session);
  }

  @Scheduled(fixedDelay = 15000)
  public synchronized void renewAndCleanup() {
    if (closing) return;
    for (Session session : List.copyOf(sessions.values())) {
      try {
        VideoPlatformInstance latest = instances.selectByPrimaryKey(session.instance.getId());
        VideoPlatformDevice device = devices.selectOne(VideoPlatformDevice.builder()
            .instanceKey(session.instance.getInstanceKey()).deviceId(session.device).build());
        if (latest == null || !Integer.valueOf(1).equals(latest.getEnabled()) || device == null
            || !Integer.valueOf(1).equals(device.getEnabled()) || !Objects.equals(latest.getTenantId(), session.instance.getTenantId())
            || !Objects.equals(latest.getAuth(), session.instance.getAuth())
            || !DahuaActiveRegistrationConfiguration.enabled(latest) || System.currentTimeMillis() - session.touched > 60000)
          session.stopping = true;
        if (!session.stopping) {
          Map<String, Object> nativeState = adapters.getAdapter("dahua_sdk").executeOperation(latest, "previewStreamStatus", session.request);
          session.state = safeState(nativeState);
          if (TERMINAL.contains(Objects.toString(nativeState.get("status"), "").toLowerCase(java.util.Locale.ROOT))) session.stopping = true;
          else {
            mediaService().renewRtpPlayback(session.scope, session.node, session.stream);
            session.state.put("mediaReady", mediaService().isRtpPlaybackReady(session.scope, session.node, session.stream));
          }
        }
      } catch (RuntimeException e) { session.stopping = true; }
      if (session.stopping) release(session);
    }
  }
  @PreDestroy public synchronized void shutdown() {
    closing = true; List.copyOf(sessions.values()).forEach(session -> { session.stopping = true; release(session); });
  }
}
