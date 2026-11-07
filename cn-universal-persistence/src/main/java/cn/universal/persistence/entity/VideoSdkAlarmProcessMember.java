package cn.universal.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "video_sdk_alarm_process_member")
public class VideoSdkAlarmProcessMember implements java.io.Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "receipt_id")
  private String receiptId;

  @Column(name = "process_id")
  private String processId;

  @Column(name = "tenant_id")
  private String tenantId;

  @Column(name = "instance_key")
  private String instanceKey;

  @Column(name = "device_id")
  private String deviceId;

  @Column(name = "channel_id")
  private String channelId;

  @Column(name = "event_id")
  private String eventId;

  @Column(name = "source_event_id")
  private String sourceEventId;

  @Column(name = "evidence_id")
  private String evidenceId;

  @Column(name = "alarm_state")
  private String alarmState;

  @Column(name = "alarm_time")
  private Long alarmTime;

  @Column(name = "received_at")
  private Long receivedAt;

  @Column(name = "active_post_count")
  private Long activePostCount;
}
