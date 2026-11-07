package nexiot.web.ide.debug.plugins.core.event;

import nexiot.web.ide.debug.plugins.core.model.MagicEntity;

public class FileEvent extends MagicEvent {

  private final MagicEntity entity;

  public FileEvent(String type, EventAction action, MagicEntity entity) {
    super(type, action);
    this.entity = entity;
  }

  public FileEvent(String type, EventAction action, MagicEntity entity, String source) {
    super(type, action, source);
    this.entity = entity;
  }

  public MagicEntity getEntity() {
    return entity;
  }
}
