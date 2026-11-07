package cn.universal.common.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 事件消息 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage {

  /** 事件类型 */
  private String eventType;

  /** 事件数据 */
  private Object data;

  /** 节点ID */
  private String nodeId;

  /** 时间戳 */
  private Long timestamp;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 延迟秒数 */
  private Long delaySeconds;

  /** 事件ID */
  private String eventId;
}
