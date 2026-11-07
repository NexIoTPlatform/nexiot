/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台通道表实体
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
 * 视频平台通道表
 *
 * <p>存储视频平台设备的通道信息，一个设备可包含多个通道
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/11/08
 */
@Table(name = "video_platform_channel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "视频平台通道实体")
public class VideoPlatformChannel implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 关联设备主表ID */
  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private Long deviceId;

  /** 平台实例唯一标识 */
  @Column(name = "instance_key")
  @Schema(description = "实例标识")
  private String instanceKey;

  /** 平台侧设备ID */
  @Column(name = "platform_device_id")
  @Schema(description = "平台设备ID")
  private String platformDeviceId;

  /** 通道ID（平台侧唯一标识） */
  @Column(name = "channel_id")
  @Schema(description = "通道ID")
  private String channelId;

  // ==================== 通道公共字段 ====================

  /** 通道名称 */
  @Column(name = "channel_name")
  @Schema(description = "通道名称")
  private String channelName;

  /** 通道状态: online/offline */
  @Column(name = "channel_status")
  @Schema(description = "通道状态")
  private String channelStatus;

  /** 通道类型: analog/digital/virtual */
  @Column(name = "channel_type")
  @Schema(description = "通道类型")
  private String channelType;

  /** 父通道ID（级联场景） */
  @Column(name = "parent_id")
  @Schema(description = "父通道ID")
  private String parentId;

  /** 通道厂商 */
  @Column(name = "manufacturer")
  @Schema(description = "通道厂商")
  private String manufacturer;

  /** 通道型号 */
  @Column(name = "model")
  @Schema(description = "通道型号")
  private String model;

  /** 通道所有者 */
  @Column(name = "owner")
  @Schema(description = "通道所有者")
  private String owner;

  /** 行政区划 */
  @Column(name = "civil_code")
  @Schema(description = "行政区划码")
  private String civilCode;

  /** 安装地址 */
  @Column(name = "address")
  @Schema(description = "安装地址")
  private String address;

  /** 是否有子设备: 0-否 1-是 */
  @Column(name = "parental")
  @Schema(description = "是否有子设备")
  private Integer parental;

  /** 信令安全模式 */
  @Column(name = "safety_way")
  @Schema(description = "信令安全模式")
  private Integer safetyWay;

  /** 注册方式 */
  @Column(name = "register_way")
  @Schema(description = "注册方式")
  private Integer registerWay;

  /** 保密属性 */
  @Column(name = "secrecy")
  @Schema(description = "保密属性")
  private Integer secrecy;

  /** 通道IP地址 */
  @Column(name = "ip_address")
  @Schema(description = "通道IP地址")
  private String ipAddress;

  /** 通道端口 */
  @Column(name = "port")
  @Schema(description = "通道端口")
  private Integer port;

  /** 经度 */
  @Column(name = "longitude")
  @Schema(description = "经度")
  private String longitude;

  /** 纬度 */
  @Column(name = "latitude")
  @Schema(description = "纬度")
  private String latitude;

  /** 云台类型: 0-不支持 1-球机 2-半球 3-固定枪机 4-遥控枪机 */
  @Column(name = "ptz_type")
  @Schema(description = "云台类型")
  private Integer ptzType;

  /** 位置类型 */
  @Column(name = "position_type")
  @Schema(description = "位置类型")
  private Integer positionType;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  // ==================== WVP GB28181 通道级特有字段 ====================

  /** WVP流ID */
  @Column(name = "stream_id")
  @Schema(description = "WVP流ID")
  private String streamId;

  /** WVP国标流ID */
  @Column(name = "gb_stream_id")
  @Schema(description = "WVP国标流ID")
  private String gbStreamId;

  /** WVP是否有音频 */
  @Column(name = "has_audio")
  @Schema(description = "是否有音频")
  private Integer hasAudio;

  // ==================== 海康ISC 通道级特有字段 ====================

  /** 海康摄像机唯一标识码(用于抓图/预览) */
  @Column(name = "camera_index_code")
  @Schema(description = "海康摄像机索引码")
  private String cameraIndexCode;

  /** 海康通道号 */
  @Column(name = "channel_no")
  @Schema(description = "海康通道号")
  private String channelNo;

  /** 海康摄像机类型: 0-枪机/1-球机/2-半球 */
  @Column(name = "camera_type")
  @Schema(description = "海康摄像机类型")
  private Integer cameraType;

  /** 海康是否支持云台: 0-否/1-是 */
  @Column(name = "ptz")
  @Schema(description = "海康是否支持云台")
  private Integer ptz;

  /** 海康通道能力集编码 */
  @Column(name = "capability_set")
  @Schema(description = "海康通道能力集")
  private String capabilitySet;

  /** 海康安装位置 */
  @Column(name = "install_location")
  @Schema(description = "海康安装位置")
  private String installLocation;

  // ==================== 大华ICC 通道级特有字段 ====================

  /** 大华通道编码 */
  @Column(name = "channel_code")
  @Schema(description = "大华通道编码")
  private String channelCode;

  /** 大华通道序号 */
  @Column(name = "channel_seq")
  @Schema(description = "大华通道序号")
  private Integer channelSeq;

  /** 大华编码格式 */
  @Column(name = "encode_format")
  @Schema(description = "大华编码格式")
  private String encodeFormat;

  /** 大华分辨率 */
  @Column(name = "resolution")
  @Schema(description = "大华分辨率")
  private String resolution;

  // ==================== 通道能力与配置 ====================

  /** 通道能力集（JSON）: 录像/抓图/对讲/报警等 */
  @Column(name = "capabilities")
  @Schema(description = "通道能力集")
  private String capabilities;

  /** 流配置（JSON）: 主码流/子码流参数 */
  @Column(name = "stream_config")
  @Schema(description = "流配置")
  private String streamConfig;

  // ==================== 扩展字段 ====================

  /** 扩展字段1 */
  @Column(name = "ext_field1")
  @Schema(description = "扩展字段1")
  private String extField1;

  /** 扩展字段2 */
  @Column(name = "ext_field2")
  @Schema(description = "扩展字段2")
  private String extField2;

  /** 扩展字段3（JSON） */
  @Column(name = "ext_field3")
  @Schema(description = "扩展字段3")
  private String extField3;

  /** 是否启用：0-禁用，1-启用 */
  @Column(name = "enabled")
  @Schema(description = "启用状态")
  private Integer enabled;

  /** 创建者ID */
  @Column(name = "create_id")
  @Schema(description = "创建者ID")
  private String createId;

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
