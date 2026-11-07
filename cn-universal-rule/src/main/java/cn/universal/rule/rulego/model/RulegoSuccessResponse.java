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

package cn.universal.rule.rulego.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * rulego简单成功响应
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RulegoSuccessResponse {

  /** 是否成功 */
  private boolean success = true;

  /** 响应消息 */
  private String message = "操作成功";

  /** 响应代码 */
  private String code = "200";
}
