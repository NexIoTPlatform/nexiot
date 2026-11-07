package cn.universal.persistence.entity.bo;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketPushProfileBO implements Serializable {

  private static final long serialVersionUID = 1L;

  private Boolean globalEnabled;

  private Boolean adminEditable;

  private String websocketPath;

  private WebSocketPushSettingsBO settings;
}
