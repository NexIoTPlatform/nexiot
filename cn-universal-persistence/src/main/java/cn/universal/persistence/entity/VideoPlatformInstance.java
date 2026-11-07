/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台实例实体
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
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

@Table(name = "video_platform_instance")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "视频平台实例实体")
public class VideoPlatformInstance implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 平台类型：wvp/isc/icc */
  @Column(name = "platform_type")
  @Schema(description = "平台类型")
  private String platformType;

  /** 实例唯一标识 */
  @Column(name = "instance_key")
  @Schema(description = "实例标识")
  private String instanceKey;

  /** 实例名称 */
  @Column(name = "name")
  @Schema(description = "实例名称")
  private String name;

  /** 平台API地址或域 */
  @Column(name = "endpoint")
  @Schema(description = "平台端点")
  private String endpoint;

  /** 鉴权配置（JSON） */
  @Column(name = "auth")
  @Schema(description = "鉴权配置")
  private String auth;

  /** 平台版本 */
  @Column(name = "version")
  @Schema(description = "平台版本")
  private String version;

  /** 其他选项配置（JSON） */
  @Column(name = "options")
  @Schema(description = "其他选项")
  private String options;

  /** WVP是否自动创建GB/级联产品 */
  @Column(name = "auto_create_products")
  @Schema(description = "自动创建产品")
  private Integer autoCreateProducts;

  /** 是否启用 */
  @Column(name = "enabled")
  @Schema(description = "启用状态")
  private Integer enabled;

  /** 创建者ID */
  @Column(name = "creator_id")
  @Schema(description = "创建者ID")
  private String creatorId;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

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
