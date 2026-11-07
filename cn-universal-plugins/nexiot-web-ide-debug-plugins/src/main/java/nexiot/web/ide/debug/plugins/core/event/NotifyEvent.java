package nexiot.web.ide.debug.plugins.core.event;

import nexiot.web.ide.debug.plugins.core.config.Constants;
import nexiot.web.ide.debug.plugins.core.model.MagicNotify;

public class NotifyEvent extends MagicEvent {

  private String id;

  public NotifyEvent(MagicNotify notify) {
    super(notify.getType(), notify.getAction(), Constants.EVENT_SOURCE_NOTIFY);
    this.id = notify.getId();
  }

  public String getId() {
    return id;
  }
}
