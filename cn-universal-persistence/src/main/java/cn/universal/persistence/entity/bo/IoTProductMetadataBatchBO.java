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

package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物模型批量更新请求对象
 * 支持从公共物模型初始化、批量导入等场景
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "物模型批量更新请求对象")
public class IoTProductMetadataBatchBO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 目标产品Key */
  @Schema(description = "目标产品Key", required = true)
  private String productKey;

  /** 
   * 更新模式
   * replace: 完全替换（清空原有物模型后导入）
   * merge: 合并模式（保留现有，ID相同的会被覆盖）
   */
  @Schema(description = "更新模式：replace=完全替换, merge=合并模式")
  private String mode;

  /** 原始物模型JSON，仅支持replace完整替换，保留文档及字段扩展配置。 */
  private String metadata;

  /** 
   * 公共物模型产品ID（优先使用）
   * 如果提供此参数，将直接从公共物模型表获取物模型数据
   */
  @Schema(description = "公共物模型产品ID（优先从此ID获取物模型）")
  private Long standardProductId;

  /** 属性列表 */
  @Schema(description = "属性列表")
  private List<IoTProductMetadataBO> properties;

  /** 事件列表 */
  @Schema(description = "事件列表")
  private List<IoTProductMetadataBO> events;

  /** 功能列表 */
  @Schema(description = "功能列表")
  private List<IoTProductMetadataBO> functions;
}
