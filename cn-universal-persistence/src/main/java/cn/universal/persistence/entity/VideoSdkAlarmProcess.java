package cn.universal.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "video_sdk_alarm_process")
public class VideoSdkAlarmProcess implements java.io.Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "tenant_id")
  private String tenantId;

  @Column(name = "instance_key")
  private String instanceKey;

  @Column(name = "device_id")
  private String deviceId;

  @Column(name = "channel_id")
  private String channelId;

  @Column(name = "event_kind")
  private String eventKind;

  @Column(name = "row_kind")
  private String rowKind;

  @Column(name = "state")
  private String state;

  @Column(name = "current_process_id")
  private String currentProcessId;

  @Column(name = "first_time")
  private Long firstTime;

  @Column(name = "last_time")
  private Long lastTime;

  @Column(name = "first_received_at")
  private Long firstReceivedAt;

  @Column(name = "last_received_at")
  private Long lastReceivedAt;

  @Column(name = "event_count")
  private Long eventCount;

  @Column(name = "first_event_id")
  private String firstEventId;

  @Column(name = "last_event_id")
  private String lastEventId;

  @Column(name = "start_observed")
  private Integer startObserved;

  @Column(name = "stop_observed")
  private Integer stopObserved;

  @Column(name = "closed_at")
  private Long closedAt;

  @Column(name = "close_reason")
  private String closeReason;

  @Column(name = "created_at")
  private Long createdAt;

  @Column(name = "updated_at")
  private Long updatedAt;
}
