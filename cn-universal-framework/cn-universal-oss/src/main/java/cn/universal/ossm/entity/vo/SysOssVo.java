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

package cn.universal.ossm.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/** OSS云存储视图对象 sys_oss @Author Lion Li */
@Data
@Schema(description = "OSS云存储视图对象")
public class SysOssVo {

  private static final long serialVersionUID = 1L;

  /** 云存储主键 */
  @Schema(description = "云存储主键")
  private Long ossId;

  /** 文件名 */
  @Schema(description = "文件名")
  private String fileName;

  /** 原名 */
  @Schema(description = "原名")
  private String originalName;

  /** 文件后缀名 */
  @Schema(description = "文件后缀名")
  private String fileSuffix;

  /** URL地址 */
  @Schema(description = "URL地址")
  private String url;

  /** 创建时间 */
  @Schema(description = "创建时间")
  private Date createTime;

  /** 上传人 */
  @Schema(description = "上传人")
  private String createBy;

  /** 服务商 */
  @Schema(description = "服务商")
  private String service;
}
