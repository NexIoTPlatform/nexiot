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
import lombok.Data;

/**
 * 产品 ProductKey 修改请求
 *
 * @since 2026/4/3
 */
@Data
@Schema(description = "产品 ProductKey 修改请求")
public class IoTProductKeyUpdateBO {

  @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(description = "新的 ProductKey", requiredMode = Schema.RequiredMode.REQUIRED)
  private String productKey;
}
