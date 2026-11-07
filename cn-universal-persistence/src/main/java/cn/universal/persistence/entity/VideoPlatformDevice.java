/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台设备主表实体
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
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 视频平台设备主表
 *
 * <p>存储各视频平台设备的公共字段信息
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/11/08
 */
@Table(name = "video_platform_device")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频平台设备实体")
public class VideoPlatformDevice implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 平台实例唯一标识 */
  @Column(name = "instance_key")
  @Schema(description = "实例标识")
  private String instanceKey;

  /** 设备ID（平台侧唯一标识） */
  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  /** 设备名称 */
  @Column(name = "device_name")
  @Schema(description = "设备名称")
  private String deviceName;

  /** 设备状态: online/offline/1/0等 */
  @Column(name = "device_status")
  @Schema(description = "设备状态")
  private String deviceStatus;

  /** 设备型号 */
  @Column(name = "device_model")
  @Schema(description = "设备型号")
  private String deviceModel;

  /** 设备IP地址 */
  @Column(name = "device_ip")
  @Schema(description = "设备IP")
  private String deviceIp;

  /** 设备端口 */
  @Column(name = "device_port")
  @Schema(description = "设备端口")
  private Integer devicePort;

  /** 设备厂商 */
  @Column(name = "manufacturer")
  @Schema(description = "设备厂商")
  private String manufacturer;

  /** 所属组织ID */
  @Column(name = "org_id")
  @Schema(description = "组织ID")
  private String orgId;

  /** 所属组织名称 */
  @Column(name = "org_name")
  @Schema(description = "组织名称")
  private String orgName;

  /** 经度（longitude） */
  @Column(name = "gps_x")
  @Schema(description = "经度")
  private String gpsX;

  /** 纬度（latitude） */
  @Column(name = "gps_y")
  @Schema(description = "纬度")
  private String gpsY;

  /** Z轴高度（altitude） */
  @Column(name = "gps_z")
  @Schema(description = "海拔高度")
  private String gpsZ;

  /** 设备配置（JSON）包含channelList、能力集等公共扩展信息 */
  @Column(name = "configuration")
  @Schema(description = "设备配置")
  private String configuration;

  /** 备注说明 */
  @Column(name = "remark")
  @Schema(description = "备注")
  private String remark;

  /** 是否启用：0-禁用，1-启用 */
  @Column(name = "enabled")
  @Schema(description = "启用状态")
  private Integer enabled;

  /** 创建者ID */
  @Column(name = "create_id")
  @Schema(description = "创建者ID")
  private String createId;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 更新者ID */
  @Column(name = "update_id")
  @Schema(description = "更新者ID")
  private String updateId;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 更新时间 */
  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;
}
