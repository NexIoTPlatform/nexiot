package cn.universal.web.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.bo.DeviceWebSocketPushEvent;
import cn.universal.persistence.entity.bo.WebSocketPushSettingsBO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class WebSocketPushSessionManagerTest {

  @Test
  void genericDevicePushRespectsOwnerAndSubscription() throws Exception {
    var manager =
        new WebSocketPushSessionManager(mock(ClusterWebSocketSubscriptionRegistry.class));
    var owner = register(manager, "owner-session", "owner", true);
    var unrelated = register(manager, "unrelated-session", "other", true);
    var disabled = register(manager, "disabled-session", "owner", false);
    var event = DeviceWebSocketPushEvent.builder()
        .id("event-1")
        .category(WebSocketPushSettingsBO.DEVICE_ONLINE)
        .tenantId("tenant-a")
        .ownerUnionId("owner")
        .build();

    manager.broadcast(event);

    var message = ArgumentCaptor.forClass(TextMessage.class);
    verify(owner).sendMessage(message.capture());
    var payload = JSONUtil.parseObj(message.getValue().getPayload());
    assertEquals("device_push", payload.getStr("type"));
    assertEquals("event-1", payload.getJSONObject("data").getStr("id"));
    verify(unrelated, never()).sendMessage(any());
    verify(disabled, never()).sendMessage(any());
  }

  @Test
  void unregisterUpdatesLocalSubscribers() {
    var manager =
        new WebSocketPushSessionManager(mock(ClusterWebSocketSubscriptionRegistry.class));
    register(manager, "session-1", "owner", true);
    assertTrue(manager.hasLocalSubscribers(WebSocketPushSettingsBO.DEVICE_ONLINE));

    manager.unregister("session-1");

    assertFalse(manager.hasLocalSubscribers(WebSocketPushSettingsBO.DEVICE_ONLINE));
  }

  @Test
  void heartbeatContinuesThroughGenericSessionManager() throws Exception {
    var manager = mock(WebSocketPushSessionManager.class);
    var handler = new DeviceEventWebSocketHandler();
    ReflectionTestUtils.setField(handler, "webSocketPushSessionManager", manager);
    var session = mock(WebSocketSession.class);

    handler.handleTextMessage(session, new TextMessage("{\"type\":\"heartbeat\"}"));

    verify(manager).sendToSession(
        org.mockito.ArgumentMatchers.eq(session),
        org.mockito.ArgumentMatchers.eq("heartbeat_response"), any());
  }

  private WebSocketSession register(
      WebSocketPushSessionManager manager, String id, String owner, boolean enabled) {
    var session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn(id);
    when(session.isOpen()).thenReturn(true);
    var settings = WebSocketPushSettingsBO.defaults();
    settings.setEnabled(enabled);
    manager.register(session, owner, false, "tenant-a", false, "", settings);
    return session;
  }
}
