package nexiot.web.ide.debug.plugins.core.handler;

import nexiot.web.ide.debug.plugins.core.annotation.Message;
import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.config.MessageType;
import nexiot.web.ide.debug.plugins.core.config.WebSocketSessionManager;
import nexiot.web.ide.debug.plugins.core.context.MagicConsoleSession;

public class MagicCoordinationHandler {

  @Message(MessageType.SET_FILE_ID)
  public void setFileId(MagicConsoleSession session, String fileId) {
    session.setAttribute(Constants.WEBSOCKET_ATTRIBUTE_FILE_ID, fileId);
    WebSocketSessionManager.sendToOther(
        session.getClientId(),
        MessageType.INTO_FILE_ID,
        session.getAttribute(Constants.WEBSOCKET_ATTRIBUTE_CLIENT_ID),
        fileId);
  }
}
