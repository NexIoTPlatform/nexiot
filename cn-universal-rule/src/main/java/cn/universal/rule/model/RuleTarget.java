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

package cn.universal.rule.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 推送第三方配置 @Author gitee.com/NexIoT
 *
 * @since 2023/1/14 16:25
 */
@Schema
@Data
public class RuleTarget {

  @Schema(description = "推送id")
  private String id;

  @Schema(description = "推送类型")
  private String type;

  @Schema(description = "请求地址")
  private String url;
}
