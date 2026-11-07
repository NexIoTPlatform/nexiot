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

package cn.universal.rule.model.bo;

import cn.universal.rule.model.RuleConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/**
 * 测试sql请求 @Author gitee.com/NexIoT
 *
 * @since 2023/1/14 14:16
 */
@Data
@Schema
public class RuleBo {

  @Schema(description = "规则id")
  private Long id;

  private String creatorId;

  @Schema(description = "租户ID")
  private String tenantId;

  @Schema(description = "规则名称")
  private String ruleName;

  @Schema(description = "产品key")
  private String productKey;

  @Schema(description = "数据级别")
  private String dataLevel;

  @Schema(description = "设备分组id")
  private String groupId;

  @Schema(description = "关联设备id")
  private List<String> relationIds;

  @Schema(description = "模拟数据")
  private String payload;

  @Schema(description = "规则配置")
  private RuleConfig config;

  /** 描述 */
  @Schema(description = "描述")
  private String description;
}
