/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
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

@Table(name = "video_platform_org_cache")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoPlatformOrgCache implements Serializable {
  private static final long serialVersionUID = 1L;
  @KeySql(genId = SQenGenId.class)
  @Id private Long id;

  @Column(name = "instance_key")
  private String instanceKey;

  @Column(name = "org_id")
  private String orgId;

  @Column(name = "parent_org_id")
  private String parentOrgId;

  @Column(name = "org_name")
  private String orgName;

  /** 组织路径（可选） */
  @Column(name = "path")
  private String path;

  /** 创建者ID */
  @Column(name = "create_id")
  private String createId;

  @Column(name = "tenant_id")
  private String tenantId;

  /** 更新者ID */
  @Column(name = "update_id")
  private String updateId;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date updateTime;
}
