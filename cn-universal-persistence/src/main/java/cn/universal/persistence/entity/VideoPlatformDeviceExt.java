/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台设备扩展表实体
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
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
 * 视频平台设备扩展表
 *
 * <p>存储各视频平台设备的特有字段信息
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/11/08
 */
@Table(name = "video_platform_device_ext")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlatformDeviceExt implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @KeySql(genId = SQenGenId.class)
  @Id private Long id;

  /** 关联主表设备ID */
  @Column(name = "device_id")
  private Long deviceId;

  /** 平台实例唯一标识 */
  @Column(name = "instance_key")
  private String instanceKey;

  /** 平台侧设备ID */
  @Column(name = "platform_device_id")
  private String platformDeviceId;

  // ==================== WVP GB28181 特有字段 ====================

  /** WVP字符集: GB2312/UTF-8 */
  @Column(name = "charset")
  private String charset;

  /** WVP传输协议: UDP/TCP */
  @Column(name = "transport")
  private String transport;

  /** WVP流模式: TCP-ACTIVE/TCP-PASSIVE/UDP */
  @Column(name = "stream_mode")
  private String streamMode;

  /** WVP主机地址 */
  @Column(name = "host_address")
  private String hostAddress;

  /** WVP注册有效期(秒) */
  @Column(name = "expires")
  private Integer expires;

  /** WVP最后心跳时间 */
  @Column(name = "keepalive_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date keepaliveTime;

  /** WVP注册时间 */
  @Column(name = "register_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date registerTime;

  /** WVP流媒体服务器ID */
  @Column(name = "media_server_id")
  private String mediaServerId;

  // ==================== 海康ISC 设备级特有字段 ====================

  /** 海康编码设备索引码 */
  @Column(name = "encode_dev_index_code")
  private String encodeDevIndexCode;

  /** 海康设备能力集编码 */
  @Column(name = "device_capability_set")
  private String deviceCapabilitySet;

  // ==================== 大华ICC特有字段 ====================

  /** 大华设备序列号 */
  @Column(name = "device_sn")
  private String deviceSn;

  /** 大华设备类别 */
  @Column(name = "device_category")
  private Integer deviceCategory;

  /** 大华设备类型 */
  @Column(name = "device_type")
  private String deviceType;

  /** 大华所属者编码 */
  @Column(name = "owner_code")
  private String ownerCode;

  /** 大华在线状态: 0/1 */
  @Column(name = "is_online")
  private String isOnline;

  /** 大华休眠状态: 0-非休眠/1-休眠 */
  @Column(name = "sleep_stat")
  private Integer sleepStat;

  /** 大华第三方代理端口 */
  @Column(name = "third_proxy_port")
  private Integer thirdProxyPort;

  /** 大华第三方代理服务器编码 */
  @Column(name = "third_proxy_server_code")
  private String thirdProxyServerCode;

  /** 大华license限制 */
  @Column(name = "license_limit")
  private Integer licenseLimit;

  /** 大华离线原因 */
  @Column(name = "offline_reason")
  private String offlineReason;

  /** 大华子系统标识 */
  @Column(name = "sub_system")
  private String subSystem;

  /** 大华单元信息（JSON）包含unitType/channels等 */
  @Column(name = "units_info")
  private String unitsInfo;

  // ==================== 扩展字段（预留） ====================

  /** 扩展字段1 */
  @Column(name = "ext_field1")
  private String extField1;

  /** 扩展字段2 */
  @Column(name = "ext_field2")
  private String extField2;

  /** 扩展字段3（JSON） */
  @Column(name = "ext_field3")
  private String extField3;

  /** 创建者ID */
  @Column(name = "create_id")
  private String createId;

  /** 租户ID */
  @Column(name = "tenant_id")
  private String tenantId;

  /** 更新者ID */
  @Column(name = "update_id")
  private String updateId;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  /** 更新时间 */
  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date updateTime;
}
