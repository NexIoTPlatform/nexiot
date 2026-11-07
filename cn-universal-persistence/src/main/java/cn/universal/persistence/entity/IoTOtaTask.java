/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA升级任务实体
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * OTA升级任务实体 iot_ota_task
 *
 * @since 2026-03-21
 */
@Table(name = "iot_ota_task")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "OTA升级任务实体")
public class IoTOtaTask implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final int FLAG_YES = 1;
  private static final int FLAG_NO = 0;

  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "task_no")
  @Schema(description = "任务编号")
  private String taskNo;

  @Column(name = "product_id")
  @Schema(description = "产品ID")
  private Long productId;

  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  @Column(name = "package_id")
  @Schema(description = "升级包ID")
  private Long packageId;

  @Column(name = "task_name")
  @Schema(description = "任务名称")
  private String taskName;

  @Column(name = "target_version")
  @Schema(description = "目标版本")
  private String targetVersion;

  @Column(name = "package_type")
  @Schema(description = "升级包类型")
  private String packageType;

  @Column(name = "custom_type")
  @Schema(description = "自定义类型")
  private String customType;

  @Schema(description = "升级模块")
  private String module;

  @Column(name = "rollout_mode")
  @Schema(description = "发布模式")
  private String rolloutMode;

  @Schema(description = "任务描述")
  private String description;

  @Schema(description = "任务状态")
  private String status;

  @Column(name = "device_count")
  @Schema(description = "设备总数")
  private Integer deviceCount;

  @Column(name = "waiting_count")
  @Schema(description = "等待数")
  private Integer waitingCount;

  @Column(name = "in_progress_count")
  @Schema(description = "进行中数量")
  private Integer inProgressCount;

  @Column(name = "success_count")
  @Schema(description = "成功数量")
  private Integer successCount;

  @Column(name = "failed_count")
  @Schema(description = "失败数量")
  private Integer failedCount;

  @Column(name = "canceled_count")
  @Schema(description = "取消数量")
  private Integer canceledCount;

  @Column(name = "skipped_count")
  @Schema(description = "跳过数量")
  private Integer skippedCount;

  @Column(name = "creator_id")
  @Schema(description = "创建人")
  private String creatorId;

  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  @Column(name = "strategy_json")
  @Schema(description = "策略配置")
  private String strategyJson;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  @Column(name = "start_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "开始时间")
  private Date startTime;

  @Column(name = "finish_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "完成时间")
  private Date finishTime;

  @Column(name = "cancel_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "取消时间")
  private Date cancelTime;

  @Transient
  @Schema(description = "产品名称")
  private String productName;

  @Transient
  @Schema(description = "升级包名称")
  private String packageName;

  @Transient
  @Schema(description = "升级包版本")
  private String packageVersion;

  @Transient
  @Schema(description = "设备ID列表")
  private List<String> deviceIds;

  @Transient
  @Schema(description = "最大重试次数")
  private Integer maxRetryCount;

  @Transient
  @Schema(description = "超时时间(秒)")
  private Integer timeoutSeconds;

  @Transient
  @Schema(description = "是否自动下发")
  private Integer autoDispatch;

  @Transient
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "计划开始时间")
  private Date plannedStartTime;

  @Transient
  @Schema(description = "单批下发设备数")
  private Integer batchSize;

  @Transient
  @Schema(description = "批次间隔秒数")
  private Integer batchIntervalSeconds;

  @Transient
  @Schema(description = "灰度比例(%)")
  private Integer grayPercent;

  @Transient
  @Schema(description = "灰度设备上限")
  private Integer grayDeviceLimit;

  @Transient
  @Schema(description = "失败阈值(%)")
  private Integer failureThresholdPercent;

  @Transient
  @Schema(description = "失败自动回滚")
  private Integer rollbackOnFailure;

  @Transient
  @Schema(description = "回滚升级包ID")
  private Long rollbackPackageId;

  @Transient
  @Schema(description = "是否校验目标版本更新")
  private Boolean requireVersionCheck;

  @JsonSetter("rollbackOnFailure")
  public void setRollbackOnFailureValue(Object rollbackOnFailure) {
    this.rollbackOnFailure = toFlagValue(rollbackOnFailure);
  }

  @JsonSetter("autoDispatch")
  public void setAutoDispatchValue(Object autoDispatch) {
    this.autoDispatch = toFlagValue(autoDispatch);
  }

  public static Integer toFlagValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean booleanValue) {
      return booleanValue ? FLAG_YES : FLAG_NO;
    }
    if (value instanceof Number numberValue) {
      return numberValue.intValue() == FLAG_YES ? FLAG_YES : FLAG_NO;
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) {
      return null;
    }
    if ("true".equalsIgnoreCase(text)) {
      return FLAG_YES;
    }
    if ("false".equalsIgnoreCase(text)) {
      return FLAG_NO;
    }
    try {
      return Integer.parseInt(text) == FLAG_YES ? FLAG_YES : FLAG_NO;
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  public boolean isRollbackOnFailureFlag() {
    return rollbackOnFailure != null && rollbackOnFailure == FLAG_YES;
  }

  public boolean isAutoDispatchFlag() {
    return autoDispatch != null && autoDispatch == FLAG_YES;
  }

  public static class IoTOtaTaskBuilder {
    public IoTOtaTaskBuilder rollbackOnFailure(Boolean rollbackOnFailure) {
      this.rollbackOnFailure = IoTOtaTask.toFlagValue(rollbackOnFailure);
      return this;
    }

    public IoTOtaTaskBuilder autoDispatch(Boolean autoDispatch) {
      this.autoDispatch = IoTOtaTask.toFlagValue(autoDispatch);
      return this;
    }
  }
}
