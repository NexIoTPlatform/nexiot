/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 小程序应用实体
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Table(name = "mp_app")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpApp implements Serializable {

  private static final long serialVersionUID = 1L;
  @KeySql(genId = SQenGenId.class)
  @Id private Long id;

  /** 小程序APPID */
  @Column(name = "app_id")
  private String appId;

  /** 小程序密钥（加密存储） */
  @Column(name = "app_secret")
  private String appSecret;

  /** 应用名称 */
  private String name;

  /** 应用Logo图片URL */
  private String logo;

  /** 小程序访问二维码 */
  private String qrcodeImage;

  /** 小程序访问URL（用于二维码链接） */
  private String accessUrl;

  /** 应用描述 */
  private String description;

  /** 数据源类型：1-关联已有应用 2-AK/SK自定义 */
  @Column(name = "data_source_type")
  private Integer dataSourceType;

  /** 关联的IoT应用ID */
  @Column(name = "linked_app_id")
  private String linkedAppId;

  /** Access Key */
  private String ak;

  /** Secret Key（加密存储） */
  private String sk;

  /** 主题色 */
  @Column(name = "primary_color")
  private String primaryColor;

  /** 欢迎语 */
  @Column(name = "welcome_text")
  private String welcomeText;

  /** 客服电话 */
  @Column(name = "contact_phone")
  private String contactPhone;

  /** 客服邮箱 */
  @Column(name = "contact_email")
  private String contactEmail;

  /** 用户数量（冗余） */
  @Column(name = "user_count")
  private Integer userCount;

  /** 设备数量（冗余） */
  @Column(name = "device_count")
  private Integer deviceCount;

  /** 在线设备数（冗余） */
  @Column(name = "online_device_count")
  private Integer onlineDeviceCount;

  /** 状态：0-禁用 1-启用 */
  private Integer status;

  /** 创建者ID */
  @Column(name = "creator_id")
  private String creatorId;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  /** 更新时间 */
  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date updateTime;

  /** 删除标记：0-正常 1-已删除 */
  private Integer deleted;

  /** 关联的IoT应用名称（非数据库字段） */
  @Transient private String appName;

  /** 租户ID */
  @Column(name = "tenant_id")
  private String tenantId;

  /** 数据权限部门范围（非数据库字段） */
  @Builder.Default
  @Transient
  private List<Long> scopeDeptIds = new ArrayList<>();

  /** 数据权限用户范围（非数据库字段） */
  @Builder.Default
  @Transient
  private List<String> scopeUnionIds = new ArrayList<>();

  /** 数据权限当前用户（非数据库字段） */
  @Transient private String scopeUnionId;

  /** 是否无权限访问（非数据库字段） */
  @Builder.Default
  @Transient
  private boolean scopeDenied = false;
}
