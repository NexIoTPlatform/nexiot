/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.ossm.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import io.lettuce.core.dynamic.annotation.Key;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.KeySql;

/** OSS云存储对象 @Author Lion Li */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@Table(name = "sys_oss")
public class SysOss implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 云存储主键 */
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long ossId;

  /** 文件名 */
  @Column(name = "file_name")
  private String fileName;

  /** 原名 */
  @Column(name = "original_name")
  private String originalName;

  /** 文件后缀名 */
  @Column(name = "file_suffix")
  private String fileSuffix;

  /** URL地址 */
  @Column(name = "url")
  private String url;

  /** 租户ID */
  @Column(name = "tenant_id")
  private String tenantId;

  /** 创建时间 */
  //  @TableField(fill = FieldFill.INSERT)
  @Column(name = "create_time")
  private Date createTime;

  /** 上传人 */
  //  @TableField(fill = FieldFill.INSERT)
  @Column(name = "create_by")
  private String createBy;

  /** 更新时间 */
  //  @TableField(fill = FieldFill.INSERT_UPDATE)
  @Column(name = "update_time")
  private Date updateTime;

  /** 更新人 */
  //  @TableField(fill = FieldFill.INSERT_UPDATE)
  @Column(name = "update_by")
  private String updateBy;

  /** 服务商 */
  @Column(name = "service")
  private String service;
}
