package cn.universal.web.websocket;

import cn.hutool.json.JSONUtil;
import cn.universal.admin.system.service.UserWebSocketPushSettingService;
import cn.universal.persistence.entity.bo.WebSocketPushSettingsBO;
import jakarta.annotation.Resource;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class DeviceEventWebSocketHandler extends TextWebSocketHandler {

  @Resource private WebSocketPushSessionManager webSocketPushSessionManager;

  @Resource private UserWebSocketPushSettingService userWebSocketPushSettingService;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    Map<String, Object> attributes = session.getAttributes();
    String unionId = String.valueOf(attributes.getOrDefault("unionId", ""));
    boolean admin = Boolean.TRUE.equals(attributes.get("admin"));
    String tenantId = (String) attributes.get("tenantId");
    boolean tenantAdmin = Boolean.TRUE.equals(attributes.get("tenantAdmin"));
    String parentUnionId = (String) attributes.get("parentUnionId");
    WebSocketPushSettingsBO settings =
        (WebSocketPushSettingsBO) attributes.getOrDefault("settings", WebSocketPushSettingsBO.defaults());
    settings = settings == null ? WebSocketPushSettingsBO.defaults() : settings.normalized();
    webSocketPushSessionManager.register(
        session, unionId, admin, tenantId, tenantAdmin, parentUnionId, settings);
    webSocketPushSessionManager.sendToSession(
        session,
        "connected",
        Map.of(
            "globalEnabled", userWebSocketPushSettingService.isGlobalEnabled(),
            "settings", settings,
            "serverTime", System.currentTimeMillis()));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    try {
      Map<String, Object> payload = JSONUtil.toBean(message.getPayload(), Map.class);
      String type = String.valueOf(payload.getOrDefault("type", ""));
      if ("heartbeat".equalsIgnoreCase(type) || "ping".equalsIgnoreCase(type)) {
        webSocketPushSessionManager.sendToSession(
            session, "heartbeat_response", Map.of("serverTime", System.currentTimeMillis()));
        return;
      }
      if ("subscribe".equalsIgnoreCase(type)) {
        Object rawSettings = payload.containsKey("settings") ? payload.get("settings") : payload;
        WebSocketPushSettingsBO settings =
            JSONUtil.toBean(JSONUtil.toJsonStr(rawSettings), WebSocketPushSettingsBO.class);
        WebSocketPushSettingsBO latestSettings =
            webSocketPushSessionManager.updateSessionSettings(session.getId(), settings);
        webSocketPushSessionManager.sendToSession(
            session,
            "subscription_ack",
            Map.of(
                "globalEnabled", userWebSocketPushSettingService.isGlobalEnabled(),
                "settings", latestSettings,
                "serverTime", System.currentTimeMillis()));
      }
    } catch (Exception e) {
      log.warn("WebSocket消息处理失败: sessionId={}, payload={}", session.getId(), message.getPayload(), e);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    webSocketPushSessionManager.unregister(session.getId());
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.warn("WebSocket连接异常: sessionId={}", session.getId(), exception);
    webSocketPushSessionManager.unregister(session.getId());
  }
}
