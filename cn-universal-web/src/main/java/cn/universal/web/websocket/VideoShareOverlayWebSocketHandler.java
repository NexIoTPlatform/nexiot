package cn.universal.web.websocket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 公开视频分享页的前端 OSD WebSocket 推送。
 *
 * <p>这里推送的是 service 生成的 OSD 快照，不负责视频取流，也不会启动后端融合任务。分享页拿到首屏播放地址后，
 * 后续只通过该通道刷新 overlayText/ts；WebSocket 不可用时前端会退回 HTTP 轮询。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoShareOverlayWebSocketHandler extends TextWebSocketHandler {

  private static final String PATH_PREFIX = "/ws/video-share/";
  private static final CloseStatus TOO_MANY_SESSIONS =
      new CloseStatus(1013, "too many share sessions");

  private final VideoPlatformInstanceService instanceService;

  // session 级上下文用于单连接发送，subscription 按 token 聚合，避免同一个分享链接开多个定时任务。
  private final Map<String, ShareSessionContext> sessions = new ConcurrentHashMap<>();
  private final Map<String, TokenSubscription> subscriptions = new ConcurrentHashMap<>();

  @Value("${video.share.overlay-event-interval-ms:1000}")
  private long overlayEventIntervalMs;

  @Value("${video.share.websocket.worker-pool-size:4}")
  private int workerPoolSize;

  @Value("${video.share.websocket.max-sessions-per-token:200}")
  private int maxSessionsPerToken;

  @Value("${video.share.websocket.heartbeat-interval-ms:15000}")
  private long heartbeatIntervalMs;

  private ScheduledExecutorService overlayPushExecutor;

  @PostConstruct
  public void init() {
    AtomicInteger index = new AtomicInteger(1);
    ThreadFactory threadFactory =
        runnable -> {
          Thread thread =
              new Thread(runnable, "video-share-osd-ws-" + index.getAndIncrement());
          thread.setDaemon(true);
          return thread;
        };
    overlayPushExecutor =
        Executors.newScheduledThreadPool(Math.max(2, workerPoolSize), threadFactory);
  }

  @PreDestroy
  public void shutdown() {
    if (overlayPushExecutor != null) {
      overlayPushExecutor.shutdownNow();
    }
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    String token = resolveToken(session.getUri());
    if (StrUtil.isBlank(token)) {
      closeWithError(session, CloseStatus.POLICY_VIOLATION, "分享链接无效");
      return;
    }

    try {
      // 首次读取快照同时校验 token/过期时间；失败时直接拒绝连接。
      Map<String, Object> overlay = instanceService.getFrontendOsdShareOverlay(token);
      TokenSubscription subscription =
          subscriptions.computeIfAbsent(token, TokenSubscription::new);
      if (subscription.sessionIds.size() >= Math.max(1, maxSessionsPerToken)) {
        closeWithError(session, TOO_MANY_SESSIONS, "当前分享链接连接数过多");
        return;
      }

      sessions.put(session.getId(), new ShareSessionContext(session, token));
      subscription.sessionIds.add(session.getId());
      ensureScheduled(subscription);

      // 建连后立即推送一次快照，避免前端等到下一个周期才看到最新 SYNC 时间。
      sendToSession(
          session,
          "connected",
          Map.of(
              "serverTime",
              System.currentTimeMillis(),
              "overlayEventIntervalMs",
              resolveOverlayEventIntervalMs()));
      sendToSession(session, "overlay", overlay);
    } catch (Exception e) {
      log.warn("[视频分享] WebSocket握手校验失败 token={}, msg={}", token, e.getMessage());
      closeWithError(session, CloseStatus.POLICY_VIOLATION, e.getMessage());
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    ShareSessionContext context = sessions.get(session.getId());
    if (context == null) {
      closeWithError(session, CloseStatus.POLICY_VIOLATION, "分享会话无效");
      return;
    }

    try {
      Map<String, Object> payload = JSONUtil.toBean(message.getPayload(), Map.class);
      String type = String.valueOf(payload.getOrDefault("type", ""));
      if ("heartbeat".equalsIgnoreCase(type) || "ping".equalsIgnoreCase(type)) {
        sendToSession(
            session, "heartbeat_response", Map.of("serverTime", System.currentTimeMillis()));
        return;
      }
      if ("refresh".equalsIgnoreCase(type)) {
        // 前端 watchdog 可主动请求一次快照，用于网络抖动后的快速补偿。
        sendToSession(session, "overlay", instanceService.getFrontendOsdShareOverlay(context.token));
      }
    } catch (Exception e) {
      log.debug(
          "[视频分享] WebSocket消息处理失败 sessionId={}, payload={}, msg={}",
          session.getId(),
          message.getPayload(),
          e.getMessage());
      sendToSession(session, "error", Map.of("message", StrUtil.blankToDefault(e.getMessage(), "消息处理失败")));
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    unregister(session.getId());
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.debug("[视频分享] WebSocket连接异常 sessionId={}, msg={}", session.getId(), exception.getMessage());
    unregister(session.getId());
  }

  private void ensureScheduled(TokenSubscription subscription) {
    if (subscription.future != null && !subscription.future.isCancelled()) {
      return;
    }
    synchronized (subscription) {
      if (subscription.future != null && !subscription.future.isCancelled()) {
        return;
      }
      // 每个 token 只保留一个定时推送任务，再广播给该 token 下所有浏览器连接。
      subscription.future =
          overlayPushExecutor.scheduleWithFixedDelay(
              () -> pushOverlay(subscription),
              resolveOverlayEventIntervalMs(),
              resolveOverlayEventIntervalMs(),
              TimeUnit.MILLISECONDS);
    }
  }

  private void pushOverlay(TokenSubscription subscription) {
    if (subscription.sessionIds.isEmpty()) {
      cancelSubscription(subscription);
      return;
    }

    try {
      Map<String, Object> overlay = instanceService.getFrontendOsdShareOverlay(subscription.token);
      String signature =
          String.valueOf(overlay.get("overlayText")) + ":" + String.valueOf(overlay.get("ts"));
      long now = System.currentTimeMillis();
      if (!signature.equals(subscription.lastSignature.getAndSet(signature))) {
        // 文本或时间戳变化才广播 overlay，降低大量分享页同时打开时的无效流量。
        broadcast(subscription, "overlay", overlay);
        subscription.lastHeartbeatAt.set(now);
        return;
      }
      if (now - subscription.lastHeartbeatAt.get() > resolveHeartbeatIntervalMs()) {
        // 心跳只维持连接活性，前端不会把它当作 OSD 内容更新。
        broadcast(subscription, "heartbeat", Map.of("serverTime", now));
        subscription.lastHeartbeatAt.set(now);
      }
    } catch (Exception e) {
      log.warn("[视频分享] WebSocket OSD推送结束 token={}, msg={}", subscription.token, e.getMessage());
      broadcast(
          subscription,
          "error",
          Map.of("message", StrUtil.blankToDefault(e.getMessage(), "分享链接不可用")));
      closeSubscription(subscription, CloseStatus.POLICY_VIOLATION);
    }
  }

  private void broadcast(TokenSubscription subscription, String type, Object data) {
    for (String sessionId : new ArrayList<>(subscription.sessionIds)) {
      ShareSessionContext context = sessions.get(sessionId);
      if (context == null || context.session == null || !context.session.isOpen()) {
        unregister(sessionId);
        continue;
      }
      sendToSession(context.session, type, data);
    }
  }

  private void closeSubscription(TokenSubscription subscription, CloseStatus status) {
    for (String sessionId : new ArrayList<>(subscription.sessionIds)) {
      ShareSessionContext context = sessions.get(sessionId);
      if (context != null && context.session != null && context.session.isOpen()) {
        try {
          context.session.close(status);
        } catch (IOException ignore) {
        }
      }
      unregister(sessionId);
    }
    cancelSubscription(subscription);
  }

  private void unregister(String sessionId) {
    ShareSessionContext context = sessions.remove(sessionId);
    if (context == null) {
      return;
    }
    TokenSubscription subscription = subscriptions.get(context.token);
    if (subscription == null) {
      return;
    }
    subscription.sessionIds.remove(sessionId);
    if (subscription.sessionIds.isEmpty()) {
      cancelSubscription(subscription);
    }
  }

  private void cancelSubscription(TokenSubscription subscription) {
    if (subscription.future != null) {
      subscription.future.cancel(false);
    }
    subscriptions.remove(subscription.token, subscription);
  }

  private void sendToSession(WebSocketSession session, String type, Object data) {
    if (session == null || !session.isOpen()) {
      return;
    }
    JSONObject message = new JSONObject();
    message.set("type", type);
    message.set("data", data);
    try {
      // Spring WebSocketSession 发送不是天然并发安全，按 session 串行化防止帧交错。
      synchronized (session) {
        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(message)));
      }
    } catch (IOException e) {
      log.debug("[视频分享] WebSocket消息发送失败 sessionId={}, type={}", session.getId(), type, e);
      unregister(session.getId());
    }
  }

  private void closeWithError(WebSocketSession session, CloseStatus status, String message) {
    sendToSession(
        session,
        "error",
        Map.of("message", StrUtil.blankToDefault(message, "分享链接不可用")));
    try {
      if (session != null && session.isOpen()) {
        session.close(status);
      }
    } catch (IOException ignore) {
    }
  }

  private String resolveToken(URI uri) {
    if (uri == null) {
      return "";
    }
    String path = StrUtil.blankToDefault(uri.getPath(), "");
    if (path.startsWith(PATH_PREFIX)) {
      String token = path.substring(PATH_PREFIX.length());
      int slashIndex = token.indexOf('/');
      if (slashIndex >= 0) {
        token = token.substring(0, slashIndex);
      }
      return URLDecoder.decode(token, StandardCharsets.UTF_8);
    }
    return StrUtil.blankToDefault(
        UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token"), "");
  }

  private long resolveOverlayEventIntervalMs() {
    return Math.max(300L, overlayEventIntervalMs);
  }

  private long resolveHeartbeatIntervalMs() {
    return Math.max(5000L, heartbeatIntervalMs);
  }

  private record ShareSessionContext(WebSocketSession session, String token) {}

  private static class TokenSubscription {
    private final String token;
    private final Set<String> sessionIds = ConcurrentHashMap.newKeySet();
    private final AtomicReference<String> lastSignature = new AtomicReference<>("");
    private final AtomicLong lastHeartbeatAt = new AtomicLong(0L);
    private volatile ScheduledFuture<?> future;

    private TokenSubscription(String token) {
      this.token = token;
    }
  }
}
