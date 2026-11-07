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

package cn.universal.ossm.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** OSS云存储分页查询对象 sys_oss @Author Lion Li */
@Data
@Schema(description = "OSS云存储分页查询对象")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SysOssBo {

  /** 分页大小 */
  @Schema(description = "分页大小")
  private Integer pageSize;

  /** 当前页数 */
  @Schema(description = "当前页数")
  private Integer pageNum;

  /** 排序列 */
  @Schema(description = "排序列")
  private String orderByColumn;

  /** 排序的方向desc或者asc */
  private String isAsc;

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

  /** 租户ID */
  @Schema(description = "租户ID")
  private String tenantId;

  /** 服务商 */
  @Schema(description = "服务商")
  private String service;

  /** 创建者 */
  @Schema(description = "创建者")
  private String createBy;
}
