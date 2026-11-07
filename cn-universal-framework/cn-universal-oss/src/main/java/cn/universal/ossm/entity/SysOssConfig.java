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

/** OSS存储配置 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@Table(name = "sys_oss_config")
public class SysOssConfig implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  @Column(name = "config_id")
  private Long configId;

  /** 服务商 qiniu/aliyun/minio/qcloud */
  @Column(name = "provider")
  private String provider;

  /** 租户ID */
  @Column(name = "tenant_id")
  private String tenantId;

  /** 服务端连接地址 */
  @Column(name = "endpoint")
  private String endpoint;

  /** 浏览器访问地址 */
  @Column(name = "public_endpoint")
  private String publicEndpoint;

  /** 七牛等绑定域名 */
  @Column(name = "domain")
  private String domain;

  /** 对象前缀 */
  @Column(name = "prefix")
  private String prefix;

  /** AccessKey 或 AccessKeyId */
  @Column(name = "access_key")
  private String accessKey;

  /** 加密后的 SecretKey */
  @Column(name = "secret_key")
  private String secretKey;

  /** Bucket */
  @Column(name = "bucket_name")
  private String bucketName;

  /** 区域 */
  @Column(name = "region")
  private String region;

  /** 是否 HTTPS */
  @Column(name = "is_https")
  private Boolean isHttps;

  /** 是否当前启用 */
  @Column(name = "active")
  private Boolean active;

  @Column(name = "create_by")
  private String createBy;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "update_by")
  private String updateBy;

  @Column(name = "update_time")
  private Date updateTime;

  @Column(name = "remark")
  private String remark;
}
