package nexiot.web.ide.debug.plugins.core.event;

import java.util.Collections;
import java.util.List;
import nexiot.web.ide.debug.plugins.core.model.Group;
import nexiot.web.ide.debug.plugins.core.model.MagicEntity;

public class GroupEvent extends MagicEvent {

  /** 分组信息 */
  private Group group;

  /** 子分组 */
  private List<MagicEntity> entities;

  public GroupEvent(String type, EventAction action, Group group) {
    this(type, action, group, Collections.emptyList());
  }

  public GroupEvent(String type, EventAction action, Group group, List<MagicEntity> entities) {
    super(type, action);
    this.group = group;
    this.entities = entities;
  }

  public Group getGroup() {
    return group;
  }

  public void setGroup(Group group) {
    this.group = group;
  }

  public List<MagicEntity> getEntities() {
    return entities;
  }

  public void setEntities(List<MagicEntity> entities) {
    this.entities = entities;
  }
}
