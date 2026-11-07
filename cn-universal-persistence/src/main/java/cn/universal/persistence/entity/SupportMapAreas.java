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

package cn.universal.persistence.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "support_map_areas")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "支持的地图区域实体")
public class SupportMapAreas implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Schema(description = "主键ID")
  private String id;

  /** 父id */
  @Schema(description = "父级ID")
  private String pid;

  /** 地址 */
  @Schema(description = "地址名称")
  private String name;

  /** 详细地址 */
  @Schema(description = "完整地址名称")
  private String fullName;

  /** 深度 */
  @Schema(description = "深度")
  private String deep;

  /** 核心点坐标 */
  @Schema(description = "核心点坐标")
  private String location;

  /** 范围坐标 */
  @Schema(description = "范围坐标")
  private String polygon;
}
