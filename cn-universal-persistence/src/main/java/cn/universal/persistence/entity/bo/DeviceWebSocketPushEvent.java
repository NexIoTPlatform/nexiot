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
public class DeviceWebSocketPushEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;

  private String referenceId;

  private String createdBy;

  private String action;

  private String category;

  private String title;

  private String summary;

  private String content;

  private String level;

  private Long timestamp;

  private String routePath;

  private String productKey;

  private String tenantId;

  private String deviceId;

  private String deviceName;

  private String ownerUnionId;

  private String commandId;

  private String messageType;

  private String event;

  private String eventName;

  private Object payload;
}
