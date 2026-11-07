package nexiot.web.ide.debug.plugins.core.handler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import nexiot.web.ide.debug.plugins.core.annotation.Message;
import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.config.MessageType;
import nexiot.web.ide.debug.plugins.core.config.WebSocketSessionManager;
import nexiot.web.ide.debug.plugins.core.context.MagicConsoleSession;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.interceptor.AuthorizationInterceptor;
import nexiot.web.ide.debug.plugins.utils.IpUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * UI上其它操作处理
 *
 * @author mxd
 */
public class MagicWorkbenchHandler {

  private static final MagicUser guest = new MagicUser("guest", "游客", "unauthorization");
  private static final Logger logger = LoggerFactory.getLogger(MagicWorkbenchHandler.class);
  private final AuthorizationInterceptor authorizationInterceptor;

  public MagicWorkbenchHandler(AuthorizationInterceptor authorizationInterceptor) {
    this.authorizationInterceptor = authorizationInterceptor;
  }

  @Message(MessageType.LOGIN)
  public void onLogin(MagicConsoleSession session, String token, String clientId) {
    session.setClientId(clientId);
    MagicUser user = null;
    try {
      user = authorizationInterceptor.getUserByToken(token);
    } catch (Exception e) {
      if (!authorizationInterceptor.requireLogin()) {
        user = guest;
      }
    }
    if (user != null) {
      String ip =
          Optional.ofNullable(session.getWebSocketSession().getRemoteAddress())
              .map(it -> it.getAddress().getHostAddress())
              .orElse("unknown");
      HttpHeaders headers = session.getWebSocketSession().getHandshakeHeaders();
      ip = IpUtils.getRealIP(ip, headers::getFirst, null);
      session.setUser(user);
      if (user.getTimeout() > 0) {
        session.setTimeout(user.getTimeout() * 1000 + System.currentTimeMillis());
      }
      session.setAttribute(Constants.WEBSOCKET_ATTRIBUTE_USER_ID, user.getId());
      session.setAttribute(
          Constants.WEBSOCKET_ATTRIBUTE_USER_IP, StringUtils.defaultIfBlank(ip, "unknown"));
      session.setAttribute(Constants.WEBSOCKET_ATTRIBUTE_USER_NAME, user.getUsername());
      session.setActivateTime(System.currentTimeMillis());
      synchronized (MagicWorkbenchHandler.class) {
        if (WebSocketSessionManager.getConsoleSession(clientId) != null) {
          WebSocketSessionManager.sendBySession(
              session, WebSocketSessionManager.buildMessage(MessageType.LOGIN_RESPONSE, "-1"));
          return;
        }
        WebSocketSessionManager.add(session);
      }
      WebSocketSessionManager.sendBySession(
          session,
          WebSocketSessionManager.buildMessage(
              MessageType.LOGIN_RESPONSE, "1", session.getAttributes()));
      List<Map<String, Object>> messages = getOnlineUsers();
      if (!messages.isEmpty()) {
        WebSocketSessionManager.sendByClientId(
            session.getClientId(),
            WebSocketSessionManager.buildMessage(MessageType.ONLINE_USERS, messages));
      }
      WebSocketSessionManager.sendToMachine(MessageType.SEND_ONLINE, session.getClientId());
      WebSocketSessionManager.sendToOther(
          session.getClientId(), MessageType.USER_LOGIN, session.getAttributes());
    } else {
      WebSocketSessionManager.sendBySession(
          session, WebSocketSessionManager.buildMessage(MessageType.LOGIN_RESPONSE, "0"));
    }
  }

  @Message(MessageType.SEND_ONLINE)
  public void sendOnline(String clientId) {
    List<Map<String, Object>> messages = getOnlineUsers();
    if (!messages.isEmpty()) {
      WebSocketSessionManager.sendToMachineByClientId(
          clientId, WebSocketSessionManager.buildMessage(MessageType.ONLINE_USERS, messages));
    }
  }

  @Message(MessageType.PONG)
  public String pong(MagicConsoleSession session) {
    session.setActivateTime(System.currentTimeMillis());
    MagicUser user = session.getUser();
    if (user != null && session.getTimeout() - System.currentTimeMillis() < 60 * 1000) {
      String oldToken = user.getToken();
      try {
        authorizationInterceptor.refreshToken(user);
      } catch (RuntimeException exception) {
        session.close();
        return null;
      }
      String newToken = user.getToken();
      session.setTimeout(System.currentTimeMillis() + user.getTimeout() * 1000);
      if (!Objects.equals(newToken, oldToken)) {
        WebSocketSessionManager.sendBySession(
            session, WebSocketSessionManager.buildMessage(MessageType.REFRESH_TOKEN, newToken));
      }
    }
    return null;
  }

  private List<Map<String, Object>> getOnlineUsers() {
    return WebSocketSessionManager.getSessions().stream()
        .map(MagicConsoleSession::getAttributes)
        .collect(Collectors.toList());
  }
}
