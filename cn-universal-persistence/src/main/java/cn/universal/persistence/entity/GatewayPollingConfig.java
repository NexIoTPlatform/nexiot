/*
 *
 * Copyright (c) 2025, IoT-Universal. All Rights Reserved.
 *
 * @Description: 本文件由 Aleo 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: Aleo
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
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
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 网关轮询配置实体
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
@Data
@Table(name = "iot_gateway_polling_config")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "网关轮询配置实体")
public class GatewayPollingConfig implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final int ENABLED_NO = 0;
  public static final int ENABLED_YES = 1;
  public static final String TASK_TYPE_CLOUD_POLLING = "CLOUD_POLLING";
  public static final String TASK_TYPE_MODBUS_COLLECT = "MODBUS_COLLECT";

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 网关设备ID */
  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  /** 产品KEY */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** IoT ID */
  @Column(name = "iot_id")
  @Schema(description = "IoT ID")
  private String iotId;

  /** 任务类型: CLOUD_POLLING-云端轮询透传, MODBUS_COLLECT-Modbus采集 */
  @Column(name = "task_type")
  @Schema(description = "任务类型")
  private String taskType;

  /** 是否启用轮询 */
  @Column(name = "enabled")
  @Schema(description = "是否启用轮询")
  private Integer enabled;

  /** 轮询间隔(秒) */
  @Column(name = "interval_seconds")
  @Schema(description = "轮询间隔(秒)")
  private Integer intervalSeconds;

  /** 超时时间(秒) */
  @Column(name = "timeout_seconds")
  @Schema(description = "超时时间(秒)")
  private Integer timeoutSeconds;

  /** 失败重试次数 */
  @Column(name = "retry_times")
  @Schema(description = "失败重试次数")
  private Integer retryTimes;

  /** 指令间隔(毫秒) */
  @Column(name = "command_interval_ms")
  @Schema(description = "指令间隔(毫秒)")
  private Integer commandIntervalMs;

  /** 下次轮询时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "next_poll_time")
  @Schema(description = "下次轮询时间")
  private Date nextPollTime;

  /** 最后轮询时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "last_poll_time")
  @Schema(description = "最后轮询时间")
  private Date lastPollTime;

  /** 最后成功时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "last_success_time")
  @Schema(description = "最后成功时间")
  private Date lastSuccessTime;

  /** 连续失败次数 */
  @Column(name = "continuous_fail_count")
  @Schema(description = "连续失败次数")
  private Integer continuousFailCount;

  /** 轮询状态: NORMAL-正常, PAUSED-暂停, FAILED-失败 */
  @Column(name = "polling_status")
  @Schema(description = "轮询状态")
  private String pollingStatus;

  /** 总轮询次数 */
  @Column(name = "total_poll_count")
  @Schema(description = "总轮询次数")
  private Long totalPollCount;

  /** 成功次数 */
  @Column(name = "success_count")
  @Schema(description = "成功次数")
  private Long successCount;

  /** 失败次数 */
  @Column(name = "fail_count")
  @Schema(description = "失败次数")
  private Long failCount;

  /** 创建时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 更新时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "update_time")
  @Schema(description = "更新时间")
  private Date updateTime;

  /** 创建人ID */
  @Column(name = "creator_id")
  @Schema(description = "创建人ID")
  private String creatorId;

  @JsonSetter("enabled")
  public void setEnabledFromBoolean(Boolean enabled) {
    this.enabled = toEnabledValue(enabled);
  }

  public static Integer toEnabledValue(Boolean enabled) {
    if (enabled == null) {
      return null;
    }
    return enabled ? ENABLED_YES : ENABLED_NO;
  }

  public boolean isEnabledFlag() {
    return enabled != null && enabled == ENABLED_YES;
  }

  public static class GatewayPollingConfigBuilder {
    public GatewayPollingConfigBuilder enabled(Integer enabled) {
      this.enabled = enabled;
      return this;
    }

    public GatewayPollingConfigBuilder enabled(Boolean enabled) {
      this.enabled = GatewayPollingConfig.toEnabledValue(enabled);
      return this;
    }
  }
}
