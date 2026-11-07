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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** ISUP 原生预览与租户媒体接收资源共同释放；平台服务已完成设备/通道授权。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoIsupPreviewService {
  private final VideoPlatformAdapterRegistry adapters;
  private final ObjectProvider<VideoZlmStreamProxyService> media;
  private final VideoPlatformInstanceMapper instances;
  private final VideoPlatformDeviceMapper devices;
  private final Map<Identity, Session> sessions = new java.util.concurrent.ConcurrentHashMap<>();
  private volatile boolean closing;
  @org.springframework.beans.factory.annotation.Value("${nexiot.video.backend-node-id:}") private String backendNode;
  private final String processNode = UUID.randomUUID().toString();
  public void assertOwner(Object node) {
    if (node != null && !ownerNode().equals(node)) throw new BaseException("SESSION_OWNER_MISMATCH: 请将预览控制路由到创建会话的后端节点");
  }
  private String ownerNode() { return backendNode == null || backendNode.isBlank() ? processNode : backendNode; }
  private record Identity(String instance, String tenant, String device, String channel, String type, String node, String session) {}
  private static class Session {
    final VideoPlatformInstance instance;
    final String scope, node, stream;
    final Map<String, Object> target;
    Map<String, String> view = Map.of();
    Map<String, Object> nativeState = Map.of("status", "connecting");
    long touched = System.currentTimeMillis();
    boolean stopping, sdkClosed, mediaClosed;
    Session(VideoPlatformInstance instance, String scope, Map<String, Object> target) {
      this.instance = instance; this.scope = scope; this.target = target;
      node = Objects.toString(target.get("mediaServerPrimaryId"), "");
      stream = Objects.toString(target.get("zlmStream"), "");
    }
  }

  public synchronized Map<String, String> start(VideoPlatformInstance instance, String device, String channel,
      String type, Map<String, Object> options, String scope, String preferredNode) {
    if (closing || !Integer.valueOf(1).equals(instance.getEnabled())) throw new BaseException("ISUP 实例已停用");
    VideoPlatformDevice registered = devices.selectOne(VideoPlatformDevice.builder()
        .instanceKey(instance.getInstanceKey()).deviceId(device).build());
    if (registered == null || !Integer.valueOf(1).equals(registered.getEnabled()))
      throw new BaseException("ISUP 设备不存在或已停用");
    String streamType = type == null || type.isBlank() ? "main" : type;
    if (!Set.of("main", "sub").contains(streamType)) throw new BaseException("码流仅支持主码流或辅码流");
    String id = UUID.randomUUID().toString();
    // 每个播放器独立资源，关闭一个窗口不会停止另一个窗口的同通道预览。
    Identity identity = new Identity(instance.getInstanceKey(), instance.getTenantId(), device, channel, streamType, preferredNode, id);
    if (sessions.size() >= 128) throw new BaseException("ISUP 预览并发已满，请关闭其他窗口");
    Map<String, Object> target = mediaService().openRtpPlayback(scope, preferredNode, "isup-live-" + id);
    Session session = new Session(instance, scope, target);
    sessions.put(identity, session);
    try {
      Map<String, Object> request = new LinkedHashMap<>(options);
      request.putAll(target);
      request.put("app", target.getOrDefault("zlmApp", "rtp"));
      Map<String, String> nativeResult = adapters.getAdapter("hik_isup").getStreamUrl(instance, device, channel, streamType, request);
      if (nativeResult == null || nativeResult.isEmpty()) throw new BaseException("ISUP 原生预览未受理");
      Map<String, String> result = new LinkedHashMap<>();
      for (String key : java.util.List.of("status", "message", "sessionId", "streamId"))
        if (nativeResult.containsKey(key)) result.put(key, nativeResult.get(key));
      target.forEach((key, value) -> { if (value != null) result.put(key, String.valueOf(value)); });
      result.keySet().removeAll(Set.of("zlmHost", "zlmRtpPort", "zlmTransport", "ssrc", "psPayloadType", "h264PayloadType", "h265PayloadType"));
      result.put("streamSource", "gateway_isup"); result.put("streamMode", "isup_zlm_rtp");
      result.put("requestedMediaServerId", session.node); result.put("mediaServerId", session.node);
      result.put("app", "rtp"); result.put("stream", session.stream); result.put("streamType", streamType);
      result.put("ownerNode", ownerNode());
      session.view = result;
      return new LinkedHashMap<>(result);
    } catch (RuntimeException e) {
      session.stopping = true;
      release(identity, session);
      throw e;
    }
  }

  public synchronized Map<String, Object> stop(VideoPlatformInstance instance, String device, String channel, Map<String, String> options) {
    assertOwner(options == null ? null : options.get("ownerNode"));
    String stream = options == null ? null : options.getOrDefault("zlmStream", options.getOrDefault("stream", options.get("streamKey")));
    String type = options == null ? null : options.get("streamType");
    int count = 0;
    for (var entry : sessions.entrySet()) {
      Identity id = entry.getKey(); Session session = entry.getValue();
      if (!Objects.equals(id.instance(), instance.getInstanceKey()) || !Objects.equals(id.tenant(), instance.getTenantId())
          || !Objects.equals(id.device(), device) || !Objects.equals(id.channel(), channel)
          || (stream != null && !stream.isBlank() && !stream.equals(session.stream))
          || (type != null && !type.isBlank() && !type.equals(id.type()))) continue;
      session.stopping = true; release(id, session); count++;
    }
    return Map.of("stopped", true, "deviceId", device, "channelId", channel, "releasedSessions", count);
  }

  public synchronized Map<String, Object> heartbeat(VideoPlatformInstance instance, String device, String channel, String stream) {
    if (stream == null || stream.isBlank()) throw new BaseException("缺少 ISUP 预览流标识");
    for (var entry : sessions.entrySet()) {
      Identity id = entry.getKey(); Session session = entry.getValue();
      if (!Objects.equals(id.instance(), instance.getInstanceKey()) || !Objects.equals(id.tenant(), instance.getTenantId())
          || !Objects.equals(id.device(), device) || !Objects.equals(id.channel(), channel) || !stream.equals(session.stream)) continue;
      if (session.stopping) return Map.of("active", false, "status", "stopped", "stream", stream);
      session.touched = System.currentTimeMillis();
      Map<String, Object> result = new LinkedHashMap<>(session.nativeState);
      result.put("active", true); result.put("stream", stream);
      result.put("ownerNode", ownerNode());
      return result;
    }
    return Map.of("active", false, "status", "expired", "stream", stream);
  }

  private VideoZlmStreamProxyService mediaService() {
    VideoZlmStreamProxyService service = media.getIfAvailable();
    if (service == null) throw new BaseException("尚未接入可用 ZLM 媒体服务");
    return service;
  }

  private void release(Identity identity, Session session) {
    if (!session.sdkClosed) try {
      adapters.getAdapter("hik_isup").executeOperation(session.instance, "stopPreview",
          Map.of("deviceId", identity.device(), "channelId", identity.channel(), "streamKey", session.stream));
      session.sdkClosed = true;
    } catch (RuntimeException e) { log.warn("[ISUP预览] 原生释放待重试 instanceKey={}, stream={}", identity.instance(), session.stream); }
    if (!session.mediaClosed) try {
      mediaService().closeRtpPlayback(session.scope, session.node, session.stream); session.mediaClosed = true;
    } catch (RuntimeException e) { log.warn("[ISUP预览] 媒体释放待重试 instanceKey={}, stream={}", identity.instance(), session.stream); }
    if (session.sdkClosed && session.mediaClosed) sessions.remove(identity, session);
  }

  @Scheduled(fixedDelay = 15000)
  public synchronized void renewAndCleanup() {
    if (closing) return;
    for (var entry : sessions.entrySet()) {
      Identity identity = entry.getKey(); Session session = entry.getValue();
      try {
        VideoPlatformInstance latest = instances.selectByPrimaryKey(session.instance.getId());
        VideoPlatformDevice device = devices.selectOne(VideoPlatformDevice.builder()
            .instanceKey(identity.instance()).deviceId(identity.device()).build());
        if (latest == null || !Integer.valueOf(1).equals(latest.getEnabled()) || device == null
            || !Integer.valueOf(1).equals(device.getEnabled()) || !Objects.equals(latest.getTenantId(), identity.tenant())
            || !Objects.equals(latest.getAuth(), session.instance.getAuth())
            || System.currentTimeMillis() - session.touched > 60000) session.stopping = true;
        if (!session.stopping) {
          Map<String, Object> nativeState = adapters.getAdapter("hik_isup").executeOperation(latest, "renewStream",
              Map.of("deviceId", identity.device(), "channelId", identity.channel(), "streamKey", session.stream));
          if (Set.of("failed", "stopped", "ended", "expired").contains(Objects.toString(nativeState.get("status"))))
            session.stopping = true;
          else {
            session.nativeState = new LinkedHashMap<>();
            for (String key : java.util.List.of("status", "receivedBytes", "lastDataAt"))
              if (nativeState.containsKey(key)) session.nativeState.put(key, nativeState.get(key));
            mediaService().renewRtpPlayback(session.scope, session.node, session.stream);
          }
        }
      } catch (RuntimeException e) { session.stopping = true; }
      if (session.stopping) release(identity, session);
    }
  }

  @PreDestroy public synchronized void shutdown() {
    closing = true;
    sessions.forEach((id, session) -> { session.stopping = true; release(id, session); });
  }
}
