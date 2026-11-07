package cn.universal.web.websocket;

import cn.hutool.json.JSONUtil;
import cn.universal.security.wechat.WechatAuthService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** 独立于设备推送设置的登录前连接；每个浏览器会话仅保留一条本机连接。 */
@Component
public class WechatAuthWebSocketHandler extends TextWebSocketHandler implements MessageListener {
  private final WechatAuthService auth;
  private final RedisMessageListenerContainer listeners;
  private final ConcurrentHashMap<String, WebSocketSession> sockets = new ConcurrentHashMap<>();

  public WechatAuthWebSocketHandler(WechatAuthService auth, RedisMessageListenerContainer listeners) {
    this.auth = auth;
    this.listeners = listeners;
  }

  @PostConstruct
  public void subscribe() {
    listeners.addMessageListener(this, new ChannelTopic(WechatAuthService.STATE_TOPIC));
  }

  @PreDestroy
  public void destroy() {
    listeners.removeMessageListener(this);
    sockets.values().forEach(this::close);
    sockets.clear();
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession original) {
    String id = (String) original.getAttributes().get("wechatSessionId");
    if (id == null || !id.matches("[a-f0-9]{32}")) { close(original); return; }
    WebSocketSession session = new ConcurrentWebSocketSessionDecorator(original, 5000, 16384);
    WebSocketSession previous = sockets.put(id, session);
    if (previous != null) close(previous);
    push(id, session);
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String id = new String(message.getBody(), StandardCharsets.UTF_8);
    if (!id.matches("[a-f0-9]{32}")) return;
    WebSocketSession session = sockets.get(id);
    if (session != null) push(id, session);
  }

  @Scheduled(fixedDelay = 15000)
  public void expireConnections() {
    sockets.forEach(this::push);
  }

  private void push(String id, WebSocketSession session) {
    if (!session.isOpen()) { sockets.remove(id, session); return; }
    try {
      Map<String, Object> state = auth.socketState(id);
      session.sendMessage(new TextMessage(JSONUtil.toJsonStr(state)));
      if (List.of("CONSUMED", "BOUND", "UNBOUND", "FAILED", "CANCELLED", "EXPIRED").contains(state.get("status"))) {
        sockets.remove(id, session);
        close(session);
      }
    } catch (cn.universal.common.exception.IoTException e) {
      try { session.sendMessage(new TextMessage(JSONUtil.toJsonStr(Map.of(
          "sessionId", id, "status", "EXPIRED", "message", "二维码已过期，请刷新")))); }
      catch (Exception ignored) { }
      sockets.remove(id, session);
      close(session);
    } catch (Exception e) {
      // 网络/Redis 暂时不可用时关闭连接，页面用受保护的 HTTP 查询补偿。
      sockets.remove(id, session);
      close(session);
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    // 客户端不能通过 WS 设置会话状态或指定目标账号。
    if (message.getPayloadLength() > 1024) close(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    remove(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    remove(session);
    close(session);
  }

  private void remove(WebSocketSession session) {
    String id = (String) session.getAttributes().get("wechatSessionId");
    if (id != null) sockets.computeIfPresent(id, (key, current) ->
        current.getId().equals(session.getId()) ? null : current);
  }

  private void close(WebSocketSession session) {
    try { if (session.isOpen()) session.close(CloseStatus.NORMAL); }
    catch (Exception ignored) { }
  }
}
