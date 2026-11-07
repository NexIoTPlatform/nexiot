/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA任务设备明细实体
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * OTA任务设备明细实体 iot_ota_task_device
 *
 * @since 2026-03-21
 */
@Table(name = "iot_ota_task_device")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "OTA任务设备明细实体")
public class IoTOtaTaskDevice implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "task_id")
  @Schema(description = "任务ID")
  private Long taskId;

  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  @Column(name = "package_id")
  @Schema(description = "升级包ID")
  private Long packageId;

  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  @Column(name = "iot_id")
  @Schema(description = "IoT ID")
  private String iotId;

  @Column(name = "current_version")
  @Schema(description = "当前版本")
  private String currentVersion;

  @Column(name = "target_version")
  @Schema(description = "目标版本")
  private String targetVersion;

  @Schema(description = "升级状态")
  private String status;

  @Schema(description = "升级进度")
  private Integer progress;

  @Column(name = "retry_count")
  @Schema(description = "重试次数")
  private Integer retryCount;

  @Column(name = "max_retry_count")
  @Schema(description = "最大重试次数")
  private Integer maxRetryCount;

  @Column(name = "status_detail")
  @Schema(description = "状态详情")
  private String statusDetail;

  @Column(name = "error_code")
  @Schema(description = "错误编码")
  private String errorCode;

  @Column(name = "error_message")
  @Schema(description = "错误信息")
  private String errorMessage;

  @Schema(description = "协议类型")
  private String protocol;

  @Column(name = "command_payload")
  @Schema(description = "下发命令快照")
  private String commandPayload;

  @Column(name = "raw_report")
  @Schema(description = "原始上报")
  private String rawReport;

  @Column(name = "transfer_state")
  @Schema(description = "OTA传输会话状态（JSON）")
  private String transferState;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  @Column(name = "pushed_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "下发时间")
  private Date pushedTime;

  @Column(name = "last_report_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "最后上报时间")
  private Date lastReportTime;

  @Column(name = "success_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "成功时间")
  private Date successTime;

  @Transient
  @Schema(description = "任务编号")
  private String taskNo;

  @Transient
  @Schema(description = "任务名称")
  private String taskName;

  @Transient
  @Schema(description = "设备名称")
  private String deviceName;

  @Transient
  @Schema(description = "升级包名称")
  private String packageName;

  @Transient
  @Schema(description = "升级包版本")
  private String packageVersion;

  @Transient
  @Schema(description = "任务模块")
  private String module;

  @Transient
  @Schema(description = "任务升级包类型")
  private String packageType;

  @Transient
  @Schema(description = "任务自定义类型")
  private String customType;
}
