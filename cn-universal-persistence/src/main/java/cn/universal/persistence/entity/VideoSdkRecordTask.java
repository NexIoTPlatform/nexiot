package cn.universal.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

/** Durable recording task metadata; camera and cloud credentials are never stored here. */
@Data
@Table(name = "video_sdk_record_task")
public class VideoSdkRecordTask implements java.io.Serializable {
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
  @Column(name = "task_name")
  private String taskName;
  @Column(name = "mode")
  private String mode;
  @Column(name = "start_time")
  private Long startTime;
  @Column(name = "end_time")
  private Long endTime;
  @Column(name = "status")
  private String status;
  @Column(name = "progress")
  private Integer progress;
  @Column(name = "message")
  private String message;
  @Column(name = "gateway_job_id")
  private String gatewayJobId;
  @Column(name = "result_json")
  private String resultJson;
  @Column(name = "created_by")
  private String createdBy;
  @Column(name = "created_at")
  private Long createdAt;
  @Column(name = "updated_at")
  private Long updatedAt;
  @Column(name = "next_poll_at")
  private Long nextPollAt;
  @Column(name = "lease_until")
  private Long leaseUntil;
  @Column(name = "lease_owner")
  private String leaseOwner;
  @Column(name = "cancel_requested")
  private Integer cancelRequested;
}
