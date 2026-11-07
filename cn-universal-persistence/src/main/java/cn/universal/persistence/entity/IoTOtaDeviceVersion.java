/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA设备版本快照实体
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
 * OTA设备版本快照实体 iot_ota_device_version
 *
 * @since 2026-03-21
 */
@Table(name = "iot_ota_device_version")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "OTA设备版本快照实体")
public class IoTOtaDeviceVersion implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  @Column(name = "iot_id")
  @Schema(description = "IoT ID")
  private String iotId;

  @Column(name = "package_type")
  @Schema(description = "升级包类型")
  private String packageType;

  @Column(name = "custom_type")
  @Schema(description = "自定义类型")
  private String customType;

  @Schema(description = "升级模块")
  private String module;

  @Column(name = "current_version")
  @Schema(description = "当前版本")
  private String currentVersion;

  @Column(name = "target_version")
  @Schema(description = "目标版本")
  private String targetVersion;

  @Column(name = "hardware_version")
  @Schema(description = "硬件版本")
  private String hardwareVersion;

  @Column(name = "last_task_id")
  @Schema(description = "最近任务ID")
  private Long lastTaskId;

  @Column(name = "last_status")
  @Schema(description = "最近状态")
  private String lastStatus;

  @Schema(description = "协议类型")
  private String protocol;

  @Column(name = "retry_count")
  @Schema(description = "重试次数")
  private Integer retryCount;

  @Column(name = "status_detail")
  @Schema(description = "状态详情")
  private String statusDetail;

  @Column(name = "ext_info")
  @Schema(description = "扩展信息")
  private String extInfo;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  @Column(name = "last_report_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "最后上报时间")
  private Date lastReportTime;

  @Column(name = "last_success_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "最后成功时间")
  private Date lastSuccessTime;

  @Transient
  @Schema(description = "设备名称")
  private String deviceName;
}
