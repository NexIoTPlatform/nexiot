package cn.universal.admin.video.service;

import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.zlm.VideoZlmStreamProxyService;
import cn.universal.persistence.entity.VideoPlatformInstance;
import jakarta.annotation.PreDestroy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Owns both the native playback and the tenant-scoped ZLM receiver lease. */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoSdkPlaybackService {
  private static final Set<String> TERMINAL = Set.of("ended", "failed", "stopped", "expired");
  private static final AtomicInteger CLEANUP_THREAD_SEQUENCE = new AtomicInteger();
  private final VideoPlatformAdapterRegistry adapters;
  private final ObjectProvider<VideoZlmStreamProxyService> media;
  private final Map<String, Playback> sessions = new ConcurrentHashMap<>();
  @org.springframework.beans.factory.annotation.Value("${nexiot.video.backend-node-id:}") private String backendNode;
  private final String processNode = UUID.randomUUID().toString();
  private final ThreadPoolExecutor cleanupExecutor = new ThreadPoolExecutor(4, 4, 0L,
      TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(24), task -> {
        Thread thread = new Thread(task, "sdk-playback-cleanup-" + CLEANUP_THREAD_SEQUENCE.incrementAndGet());
        thread.setDaemon(true);
        return thread;
      }, new ThreadPoolExecutor.AbortPolicy());
  private volatile boolean shuttingDown;

  private static final class Playback {
    final VideoPlatformInstance instance;
    final String id, scope, node, stream, ownerNode;
    final Map<String, Object> request, urls;
    long originalStartTime, originalEndTime;
    final AtomicBoolean cleanupPending = new AtomicBoolean();
    volatile long touched = System.currentTimeMillis();
    long drainUntil, pauseStarted, lastPauseRenewal, mediaEndedAt;
    boolean mediaEverReady;
    volatile Map<String, Object> state = Map.of("status", "starting", "message", "正在连接设备录像");
    volatile boolean sdkClosed, mediaClosed;
    Playback(VideoPlatformInstance instance, String id, String scope, Map<String, Object> request, Map<String, Object> urls, String ownerNode) {
      this.instance = instance; this.id = id; this.scope = scope; this.request = request; this.urls = urls;
      node = Objects.toString(urls.get("mediaServerPrimaryId")); stream = Objects.toString(urls.get("zlmStream"));
      this.ownerNode = ownerNode;
      originalStartTime = timestamp(request.get("startTime")); originalEndTime = timestamp(request.get("endTime"));
    }
    Map<String, Object> view() {
      Map<String, Object> result = new LinkedHashMap<>(urls);
      result.putAll(state); result.put("playbackId", id);
      result.put("ownerNode", ownerNode);
      result.put("deviceId", request.get("deviceId")); result.put("channelId", request.get("channelId"));
      result.put("startTime", request.get("startTime")); result.put("endTime", request.get("endTime"));
      result.put("originalStartTime",originalStartTime);result.put("originalEndTime",originalEndTime);
      result.putIfAbsent("audioSupported", true);
      result.keySet().removeAll(Set.of("zlmHost", "zlmRtpPort", "zlmTransport", "ssrc", "psPayloadType", "h264PayloadType", "h265PayloadType"));
      return result;
    }
  }

  public synchronized Map<String, Object> start(VideoPlatformInstance instance, Map<String, Object> input,
      String scope, String preferredNode) {
    if (shuttingDown) throw new BaseException("回放服务正在停止");
    if (!cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType()))
      throw new BaseException("此实例不支持SDK直接回放");
    if (!Integer.valueOf(1).equals(instance.getEnabled())) throw new BaseException("实例已停用");
    long from = timestamp(input.get("startTime")), to = timestamp(input.get("endTime"));
    double rate = playbackRate(input.getOrDefault("playbackRate", 1));
    if (cn.universal.dm.video.VideoSdkPlatforms.isup(instance.getPlatformType()) && Double.compare(rate, 1d) != 0)
      throw new BaseException("当前 ISUP 回放仅支持原速播放");
    String streamType = input.get("streamType") == null ? null : Objects.toString(input.get("streamType"));
    if (streamType != null && !Set.of("main", "sub").contains(streamType)) throw new BaseException("录像码流仅支持主码流或辅码流");
    if (to - from < 1000 || to - from > 7200000) throw new BaseException("回放时段需在1秒到2小时之间");
    if (sessions.values().stream().filter(item -> !TERMINAL.contains(Objects.toString(item.state.get("status")))
        || !item.sdkClosed || !item.mediaClosed).count() >= 12)
      throw new BaseException("回放并发已满，请关闭其他回放窗口");
    VideoZlmStreamProxyService service = media.getIfAvailable();
    if (service == null) throw new BaseException("尚未接入可用ZLM媒体服务");
    String id = UUID.randomUUID().toString();
    Map<String, Object> urls = service.openRtpPlayback(scope, preferredNode, "sdk-playback-" + id);
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("deviceId", Objects.toString(input.get("deviceId"), ""));
    request.put("channelId", Objects.toString(input.get("channelId"), ""));
    if (streamType != null) request.put("streamType", streamType);
    request.putAll(urls); request.put("playbackId", id); request.put("startTime", from); request.put("endTime", to);
    // Gateway session identity is the owned RTP stream; playbackId is the platform UI identity.
    if(cn.universal.dm.video.VideoSdkPlatforms.isup(instance.getPlatformType()))
      request.put("streamKey", Objects.toString(urls.get("zlmStream"), ""));
    request.put("playbackRate", rate);
    Playback playback = new Playback(instance, id, scope, request, urls, ownerNode());
    sessions.put(id, playback);
    synchronized (playback) {
      try {
        updateState(playback, adapters.getAdapter(instance.getPlatformType()).executeOperation(instance, "playbackStreamStart", request));
        if (TERMINAL.contains(Objects.toString(playback.state.get("status")))) close(playback);
        return playback.view();
      } catch (RuntimeException ex) {
        playback.state = Map.of("status", "failed", "message", Objects.toString(ex.getMessage(), "回放启动失败"));
        close(playback);
        throw ex;
      }
    }
  }

  public Map<String, Object> control(VideoPlatformInstance instance, String operation, Map<String, Object> input) {
    if (input.get("ownerNode") != null && !ownerNode().equals(input.get("ownerNode")))
      throw new BaseException("SESSION_OWNER_MISMATCH: 请将会话控制路由到创建该会话的后端节点");
    if ("playbackStreamResume".equals(operation)) {
      operation = "playbackStreamPause"; input = new LinkedHashMap<>(input); input.put("paused", false);
    }
    String id = Objects.toString(input.get("playbackId"), "");
    Playback playback = sessions.get(id);
    if (playback == null && "playbackStreamStop".equals(operation))
      return Map.of("playbackId", id, "status", "stopped", "message", "回放已停止");
    if (playback == null || !Objects.equals(instance.getInstanceKey(), playback.instance.getInstanceKey())
        || !Objects.equals(instance.getTenantId(), playback.instance.getTenantId())) throw new BaseException("回放会话已结束或不存在");
    for (String field : List.of("deviceId", "channelId")) {
      if (input.get(field) != null && !Objects.equals(input.get(field), playback.request.get(field)))
        throw new BaseException("回放会话与操作设备或通道不一致");
    }
    synchronized (playback) {
      if ("playbackStreamSeek".equals(operation)) {
        long position = timestamp(input.getOrDefault("seekTime", input.get("position")));
        long from = playback.originalStartTime, to = playback.originalEndTime;
        if (position < from || position > to - 1000) throw new BaseException("定位时间必须在当前录像时段内，且距结束至少1秒");
        Map<String, Object> nextRequest = new LinkedHashMap<>(playback.request);
        nextRequest.put("startTime", position);
        Map<String, Object> result = new LinkedHashMap<>(start(instance, nextRequest, playback.scope, playback.node));
        Playback next = sessions.get(Objects.toString(result.get("playbackId")));
        if (next != null) { next.originalStartTime=from;next.originalEndTime=to;result.put("originalStartTime",from);result.put("originalEndTime",to); }
        playback.state = Map.of("status", "stopped", "message", "已创建定位后的回放会话"); close(playback);
        result.put("previousPlaybackId", id); result.put("seekMode", "reopen");
        return result;
      }
      if ("playbackStreamStop".equals(operation)) {
        if (Boolean.TRUE.equals(input.get("completed"))) {
          if ("draining".equals(playback.state.get("status"))) finishDrain(playback);
        } else {
          playback.state = Map.of("status", "stopped", "message", "回放已停止"); close(playback);
        }
      } else if ("draining".equals(playback.state.get("status"))) {
        continueDrain(playback);
      } else if (!TERMINAL.contains(Objects.toString(playback.state.get("status")))) {
        Map<String, Object> request = new LinkedHashMap<>(playback.request);
        if ("playbackStreamSpeed".equals(operation)) {
          if ("paused".equals(playback.state.get("status"))) throw new BaseException("请恢复播放后调整倍率");
          request.put("playbackRate", playbackRate(input.get("playbackRate")));
        }
        if ("playbackStreamPause".equals(operation)) {
          boolean paused = Boolean.TRUE.equals(input.get("paused"));
          request.put("paused", paused);
          VideoZlmStreamProxyService service = media.getIfAvailable();
          if (service == null) throw new BaseException("回放媒体服务不可用");
          if (paused) {
            if (!"paused".equals(playback.state.get("status"))) clearPauseLease(playback);
            pauseReceiver(playback, service, true);
          }
          boolean resumed = false;
          try {
            Map<String, Object> next = adapters.getAdapter(instance.getPlatformType()).executeOperation(instance, operation, request);
            if (!paused && !TERMINAL.contains(Objects.toString(next.get("status")))) {
              resumed = true;
              pauseReceiver(playback, service, false);
            }
            updateState(playback, next);
          } catch (RuntimeException ex) {
            if (paused && !TERMINAL.contains(Objects.toString(playback.state.get("status")))) {
              try { pauseReceiver(playback, service, false); }
              catch (RuntimeException cleanup) { ex.addSuppressed(cleanup); }
              if (!"paused".equals(playback.state.get("status"))) clearPauseLease(playback);
            } else if (resumed) {
              playback.state = Map.of("status", "failed", "message", "回放恢复失败，请重新开始回放");
              clearPauseLease(playback);
              close(playback);
            }
            throw ex;
          }
        } else {
          updateState(playback, adapters.getAdapter(instance.getPlatformType()).executeOperation(instance, operation, request));
          if ("paused".equals(playback.state.get("status")) && System.currentTimeMillis() - playback.lastPauseRenewal >= 15000) {
            VideoZlmStreamProxyService service = media.getIfAvailable();
            if (service == null) throw new BaseException("回放媒体服务不可用");
            pauseReceiver(playback, service, true);
          }
        }
        if (TERMINAL.contains(Objects.toString(playback.state.get("status")))) close(playback);
      }
      if (!TERMINAL.contains(Objects.toString(playback.state.get("status")))) {
        try {
          VideoZlmStreamProxyService service = media.getIfAvailable();
          if (service == null) throw new BaseException("回放媒体服务不可用");
          service.renewRtpPlayback(playback.scope, playback.node, playback.stream);
        } catch (RuntimeException ex) {
          playback.state = Map.of("status", "failed", "message", "回放媒体容量租约失效，请重新开始回放");
          close(playback);
          throw ex;
        }
      }
      playback.touched = System.currentTimeMillis();
      return playback.view();
    }
  }

  private String ownerNode() { return backendNode == null || backendNode.isBlank() ? processNode : backendNode; }

  private void updateState(Playback playback, Map<String, Object> next) {
    if (cn.universal.dm.video.VideoSdkPlatforms.isup(playback.instance.getPlatformType())) {
      Map<String, Object> normalized = new LinkedHashMap<>();
      for (String key : List.of("message", "nativeDataReceived", "receivedBytes", "lastDataAt", "positionMs", "positionTime", "playbackRate", "audioSupported"))
        if (next.containsKey(key)) normalized.put(key, next.get(key));
      String status = Objects.toString(next.get("status"), "").toLowerCase(java.util.Locale.ROOT);
      if (next.containsKey("paused")) status = Boolean.TRUE.equals(next.get("paused")) ? "paused" : "streaming";
      if ("connecting".equals(status) || status.isBlank()) status = "starting";
      if (!Set.of("starting", "streaming", "paused", "ended", "failed", "stopped", "expired").contains(status))
        throw new BaseException("ISUP 回放返回了未知状态");
      // Native bytes only confirm delivery. The owned ZLM receiver must expose a ready video track.
      boolean ready = false;
      boolean nativeEnded = "ended".equals(status);
      if (!TERMINAL.contains(status) || nativeEnded) {
        VideoZlmStreamProxyService service = media.getIfAvailable();
        ready = service != null && service.isRtpPlaybackReady(playback.scope, playback.node, playback.stream);
        playback.mediaEverReady |= ready;
        if (nativeEnded && !playback.mediaEverReady) {
          if (playback.mediaEndedAt == 0) playback.mediaEndedAt = System.currentTimeMillis();
          boolean timedOut = System.currentTimeMillis() - playback.mediaEndedAt >= 15000;
          status = timedOut ? "failed" : "starting";
          normalized.put("message", timedOut ? "设备录像已结束，但流媒体未生成可播放的视频" : "正在等待流媒体处理录像");
        } else if (!nativeEnded && !"paused".equals(status)) status = ready ? "streaming" : "starting";
      }
      if (nativeEnded) normalized.put("nativeEnded", true);
      normalized.put("mediaReady", ready);
      normalized.put("mediaReadinessOwner", "platform");
      normalized.put("status", status);
      normalized.putIfAbsent("playbackRate", playback.request.get("playbackRate"));
      // 媒体地址仅来自已授权节点，不能被 SDK 默认节点地址覆盖。
      next = normalized;
    }
    if (("ended".equals(next.get("status")) || Boolean.TRUE.equals(next.get("nativeEnded"))) && playback.pauseStarted > 0) {
      try {
        VideoZlmStreamProxyService service = media.getIfAvailable();
        if (service == null) throw new BaseException("回放媒体服务不可用");
        pauseReceiver(playback, service, false);
      } catch (RuntimeException failure) {
        playback.state = Map.of("status", "failed", "message", "回放结束时恢复媒体接收失败");
        clearPauseLease(playback);
        close(playback);
        throw failure;
      }
    }
    Map<String, Object> state = new LinkedHashMap<>(next);
    if (!"paused".equals(next.get("status"))) clearPauseLease(playback);
    if ("ended".equals(next.get("status"))) {
      // Native completion only ends delivery; the browser acknowledges the rendered tail.
      state.put("status", "draining");
      state.put("message", "正在播放剩余画面");
      playback.drainUntil = System.currentTimeMillis() + 60000;
    }
    playback.state = state;
    if (requiresProducerEof(playback) && "draining".equals(state.get("status"))) {
      // Closing the completed raw-RTP producer makes ZLM flush its final access unit.
      // Keep the receiver lease and URLs until the browser drains that real FLV EOF.
      closeSdk(playback);
    }
  }

  private void pauseReceiver(Playback playback, VideoZlmStreamProxyService service, boolean paused) {
    if (!paused) {
      service.pauseRtpPlayback(playback.scope, playback.node, playback.stream, false, 0);
      return;
    }
    long now = System.currentTimeMillis();
    if (playback.pauseStarted == 0) playback.pauseStarted = now;
    // ZLM keeps its original timeout-check clock while paused; API renewals do not reset it.
    int pauseSeconds = (int) Math.min(Integer.MAX_VALUE, Math.max(0, now - playback.pauseStarted) / 1000 + 90);
    service.pauseRtpPlayback(playback.scope, playback.node, playback.stream, true, pauseSeconds);
    playback.lastPauseRenewal = now;
  }

  private void clearPauseLease(Playback playback) {
    playback.pauseStarted = 0;
    playback.lastPauseRenewal = 0;
  }

  private void finishDrain(Playback playback) {
    if (requiresProducerEof(playback) && !playback.sdkClosed) {
      closeSdk(playback);
      if (!playback.sdkClosed) return;
    }
    Map<String, Object> state = new LinkedHashMap<>(playback.state);
    state.put("status", "ended");
    state.put("message", "回放已结束");
    playback.state = state;
    close(playback);
  }

  private boolean requiresProducerEof(Playback playback) {
    return "uniview_sdk".equals(playback.instance.getPlatformType());
  }

  private void continueDrain(Playback playback) {
    if (requiresProducerEof(playback)) closeSdk(playback);
    if (System.currentTimeMillis() < playback.drainUntil) return;
    if (!requiresProducerEof(playback)) {
      finishDrain(playback);
      return;
    }
    Map<String, Object> state = new LinkedHashMap<>(playback.state);
    state.put("status", "failed");
    state.put("message", playback.sdkClosed
        ? "回放画面和声音未能确认播放完成，请重新开始回放"
        : "回放源连接释放失败，请重新开始回放");
    playback.state = state;
    close(playback);
  }

  private void closeSdk(Playback playback) {
    if (!playback.sdkClosed) {
      try {
        adapters.getAdapter(playback.instance.getPlatformType()).executeOperation(playback.instance,
            "playbackStreamStop", playback.request);
        playback.sdkClosed = true;
      } catch (RuntimeException ex) { log.warn("[SDK回放] 原生会话释放待重试 id={}", playback.id); }
    }
  }

  private void close(Playback playback) {
    closeSdk(playback);
    if (!playback.mediaClosed) {
      try {
        VideoZlmStreamProxyService service = media.getIfAvailable();
        if (service != null) {
          service.closeRtpPlayback(playback.scope, playback.node, playback.stream);
          playback.mediaClosed = true;
        }
      } catch (RuntimeException ex) { log.warn("[SDK回放] 媒体端口释放待重试 id={}", playback.id); }
    }
  }

  @Scheduled(fixedDelay = 10000)
  public void reap() {
    if (shuttingDown) return;
    sessions.forEach((id, playback) -> {
      if (TERMINAL.contains(Objects.toString(playback.state.get("status")))
          && playback.sdkClosed && playback.mediaClosed && System.currentTimeMillis() - playback.touched <= 60000)
        return;
      if (!playback.cleanupPending.compareAndSet(false, true)) return;
      try {
        cleanupExecutor.execute(() -> {
          try {
            synchronized (playback) {
              if (shuttingDown) return;
              long now = System.currentTimeMillis();
              if ("draining".equals(playback.state.get("status"))) continueDrain(playback);
              if (now - playback.touched > 60000 && !TERMINAL.contains(Objects.toString(playback.state.get("status"))))
                playback.state = Map.of("status", "expired", "message", "回放窗口已离线");
              if (TERMINAL.contains(Objects.toString(playback.state.get("status")))) {
                close(playback);
                if (playback.sdkClosed && playback.mediaClosed && now - playback.touched > 60000)
                  sessions.remove(id, playback);
              }
            }
          } finally {
            playback.cleanupPending.set(false);
          }
        });
      } catch (RejectedExecutionException ex) {
        playback.cleanupPending.set(false);
      }
    });
  }

  @PreDestroy public synchronized void shutdown() {
    shuttingDown = true;
    cleanupExecutor.shutdownNow();
    sessions.values().forEach(playback -> {
      synchronized (playback) {
        playback.cleanupPending.set(false);
        playback.state = Map.of("status", "stopped", "message", "回放服务正在停止");
        close(playback);
      }
    });
  }
  private static long timestamp(Object value) {
    try { return Long.parseLong(Objects.toString(value)); }
    catch (RuntimeException ex) { throw new BaseException("回放起止时间无效"); }
  }
  private static double playbackRate(Object value) {
    if (!(value instanceof Number number)) throw new BaseException("回放倍率仅支持0.5、1、2、4倍");
    double rate = number.doubleValue();
    if (rate != 0.5 && rate != 1 && rate != 2 && rate != 4) throw new BaseException("回放倍率仅支持0.5、1、2、4倍");
    return rate;
  }
}
