package nexiot.web.ide.debug.plugins.core.handler;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import nexiot.web.ide.debug.plugins.auth.MagicSsoSessionService;
import nexiot.web.ide.debug.plugins.core.context.MagicConsoleSession;
import nexiot.web.ide.debug.plugins.core.context.MagicUser;
import nexiot.web.ide.debug.plugins.core.interceptor.AuthorizationInterceptor;
import nexiot.web.ide.debug.plugins.core.service.MagicNotifyService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MagicSsoWebSocketTest {
  @Test
  void expiredOrRevokedTokenClosesTheSocketBeforeDispatch() throws Exception {
    MagicSsoSessionService sessions = new MagicSsoSessionService();
    MagicUser user =
        sessions.issue("a", "tenant-a", false, Instant.now().plusSeconds(900), () -> {});
    AuthorizationInterceptor auth =
        new AuthorizationInterceptor() {
          public boolean requireLogin() {
            return true;
          }

          public MagicUser getUserByToken(String token)
              throws nexiot.web.ide.debug.plugins.core.exception.MagicLoginException {
            return sessions.getUser(token);
          }
        };
    MagicWebSocketDispatcher dispatcher =
        new MagicWebSocketDispatcher("test", mock(MagicNotifyService.class), List.of(), auth);
    WebSocketSession socket = mock(WebSocketSession.class);
    when(socket.getId()).thenReturn("revoked-socket");
    MagicConsoleSession.from(socket).setUser(user);
    sessions.revoke(user.getToken());
    dispatcher.handleTextMessage(socket, new TextMessage("pong"));
    verify(socket).close(any());
  }

  @Test
  void unauthenticatedSocketCannotSendCommands() throws Exception {
    AuthorizationInterceptor auth = mock(AuthorizationInterceptor.class);
    when(auth.requireLogin()).thenReturn(true);
    MagicWebSocketDispatcher dispatcher =
        new MagicWebSocketDispatcher("test", mock(MagicNotifyService.class), List.of(), auth);
    WebSocketSession socket = mock(WebSocketSession.class);
    when(socket.getId()).thenReturn("anonymous-socket");
    dispatcher.handleTextMessage(socket, new TextMessage("set_file_id,foreign-file"));
    verify(socket).close(any());
    verify(auth, never()).getUserByToken(any());
  }
}
