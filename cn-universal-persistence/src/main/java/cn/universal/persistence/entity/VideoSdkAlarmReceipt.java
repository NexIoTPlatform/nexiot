package cn.universal.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

/** Durable callback identity and retry state; never stores gateway credentials. */
@Data
@Table(name = "video_sdk_alarm_receipt")
public class VideoSdkAlarmReceipt implements java.io.Serializable {
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
  @Column(name = "event_id")
  private String eventId;
  @Column(name = "event_kind")
  private String eventKind;
  @Column(name = "image_fingerprint")
  private String imageFingerprint;
  @Column(name = "status")
  private String status;
  @Column(name = "payload_json")
  private String payloadJson;
  @Column(name = "result_json")
  private String resultJson;
  @Column(name = "log_persisted")
  private Integer logPersisted;
  @Column(name = "flow_dispatched")
  private Integer flowDispatched;
  @Column(name = "attempts")
  private Integer attempts;
  @Column(name = "created_at")
  private Long createdAt;
  @Column(name = "updated_at")
  private Long updatedAt;
  @Column(name = "next_attempt_at")
  private Long nextAttemptAt;
  @Column(name = "lease_until")
  private Long leaseUntil;
  @Column(name = "lease_owner")
  private String leaseOwner;
}
