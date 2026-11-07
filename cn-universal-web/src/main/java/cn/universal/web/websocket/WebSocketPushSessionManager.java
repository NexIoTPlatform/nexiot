package cn.universal.web.websocket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.bo.DeviceWebSocketPushEvent;
import cn.universal.persistence.entity.bo.WebSocketPushSettingsBO;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class WebSocketPushSessionManager {

  private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

  private final Map<String, Integer> localCategoryCounters = new ConcurrentHashMap<>();

  private final ClusterWebSocketSubscriptionRegistry clusterWebSocketSubscriptionRegistry;

  public WebSocketPushSessionManager(
      ClusterWebSocketSubscriptionRegistry clusterWebSocketSubscriptionRegistry) {
    this.clusterWebSocketSubscriptionRegistry = clusterWebSocketSubscriptionRegistry;
    refreshLocalCategoryCounters();
  }

  public void register(
      WebSocketSession session,
      String unionId,
      boolean admin,
      String tenantId,
      boolean tenantAdmin,
      String parentUnionId,
      WebSocketPushSettingsBO settings) {
    WebSocketPushSettingsBO latestSettings =
        settings == null ? WebSocketPushSettingsBO.defaults() : settings.normalized();
    sessions.put(
        session.getId(),
        new SessionContext(
            session, unionId, admin, tenantId, tenantAdmin, parentUnionId, latestSettings));
    refreshLocalCategoryCounters();
  }

  public void unregister(String sessionId) {
    sessions.remove(sessionId);
    refreshLocalCategoryCounters();
  }

  public WebSocketPushSettingsBO updateSessionSettings(
      String sessionId, WebSocketPushSettingsBO settings) {
    SessionContext context = sessions.get(sessionId);
    if (context == null) {
      return settings == null ? WebSocketPushSettingsBO.defaults() : settings.normalized();
    }
    context.setSettings(
        settings == null ? WebSocketPushSettingsBO.defaults() : settings.normalized());
    refreshLocalCategoryCounters();
    return context.getSettings();
  }

  public boolean hasLocalSubscribers(String category) {
    return localCategoryCounters.getOrDefault(category, 0) > 0;
  }

  public void broadcast(DeviceWebSocketPushEvent event) {
    if (event == null) {
      return;
    }
    sessions.values().stream()
        .filter(context -> context.matches(event))
        .forEach(context -> sendToSession(context.getSession(), "device_push", event));
  }

  public void sendToSession(WebSocketSession session, String type, Object data) {
    if (session == null || !session.isOpen()) {
      return;
    }
    JSONObject message = new JSONObject();
    message.set("type", type);
    message.set("data", data);
    try {
      synchronized (session) {
        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(message)));
      }
    } catch (IOException e) {
      log.warn("WebSocket消息发送失败: sessionId={}, type={}", session.getId(), type, e);
      unregister(session.getId());
    }
  }

  private synchronized void refreshLocalCategoryCounters() {
    Map<String, Integer> nextCounters = new LinkedHashMap<>();
    WebSocketPushSettingsBO.categories().forEach(category -> nextCounters.put(category, 0));

    sessions
        .values()
        .forEach(
            context ->
                context
                    .getSettings()
                    .toCategoryMap()
                    .forEach(
                        (category, enabled) -> {
                          if (Boolean.TRUE.equals(enabled)
                              && Boolean.TRUE.equals(context.getSettings().getEnabled())) {
                            nextCounters.computeIfPresent(category, (key, value) -> value + 1);
                          }
                        }));

    localCategoryCounters.clear();
    localCategoryCounters.putAll(nextCounters);
    clusterWebSocketSubscriptionRegistry.syncLocalCounts(nextCounters);
  }

  @Data
  @AllArgsConstructor
  private static class SessionContext {
    private WebSocketSession session;

    private String unionId;

    private boolean admin;

    private String tenantId;

    private boolean tenantAdmin;

    private String parentUnionId;

    private WebSocketPushSettingsBO settings;

    public boolean matches(DeviceWebSocketPushEvent event) {
      if (session == null || !session.isOpen() || event == null) {
        return false;
      }
      if (settings == null || !settings.supportsCategory(event.getCategory())) {
        return false;
      }
      if (admin) {
        return true;
      }
      if (tenantAdmin) {
        String eventTenantId = event.getTenantId();
        if (StrUtil.isNotBlank(eventTenantId)) {
          return StrUtil.equals(tenantId, eventTenantId);
        }
      }
      String ownerUnionId = event.getOwnerUnionId();
      if (StrUtil.isBlank(ownerUnionId)) {
        return false;
      }
      return StrUtil.equals(ownerUnionId, unionId) || StrUtil.equals(ownerUnionId, parentUnionId);
    }
  }
}
